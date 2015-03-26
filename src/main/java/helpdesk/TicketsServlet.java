package helpdesk;

import static java.net.HttpURLConnection.HTTP_OK;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.sap.core.connectivity.api.DestinationException;
import com.sap.core.connectivity.api.http.HttpDestination;

/**
 * This servlet serves as ticket provider (GET) and allows helpDesk users to send their ticket responses (POST).
 * 
 * on GET: a hard coded array of three tickets is returned in JSON format
 * 
 * on POST: the servlet looks up the ticket and its relevance (critical or not), then creates and sends an
 * event for the gamification service
 * 
 */
public class TicketsServlet extends HttpServlet {
   private static final long serialVersionUID = 1L;
   protected static final Logger logger = LoggerFactory.getLogger(TicketsServlet.class);

   private Map<Integer, Ticket> ticketMap;
   // default appname = HelpDesk, when not set in VM args
   private static final String GAMIFICATION_SERVICE_APP = System.getProperty("gamification.demoapp.appname", "HelpDesk");

   // This destination needs to be configured in your HCP runtime.
   // As you saw in the documentation, if you run this on your local HCP configure the following destination (double
   // click server > Connectivity > New Destination), name "gsdest", URL
   // "http://localhost:8080/gamification/api/tech/JsonRPC", Authentication "BasicAuthentication", User and Password of
   // a user with "AppAdmin" and "AppStandard" roles defined in the Users tab
   private static final String GAMIFICATION_SERVICE_DESTINATION = "gsdest";

   /**
    * @see HttpServlet#HttpServlet()
    */
   public TicketsServlet() {
      super();
      // example data containing 3 help desk tickets. one is marked as
      // critical.
      ticketMap = new HashMap<Integer, Ticket>();

      Ticket ticket = new Ticket(10056, "", "Hi! I just received my new computer and the button on the CD tray doesn't work properly.", "Jack",
            183, new Date(), "");
      ticketMap.put(ticket.getTicketid(), ticket);

      ticket = new Ticket(10057, "", "My computer mouse is broken. When I move the mouse right, the pointer goes left. ", "Max", 223,
            new Date(), "");
      ticketMap.put(ticket.getTicketid(), ticket);

      ticket = new Ticket(10058, "critical", "I need the password to my account. Please send it to me ASAP!!!", "Lisa", 123, new Date(), "");
      ticketMap.put(ticket.getTicketid(), ticket);
   }

   /**
    * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
    */
   protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

      response.setContentType("application/json");
      PrintWriter out = response.getWriter();

      // iterate the hash map and create a complex JSON response from it
      Gson gson = new Gson();
      String tickets_json = "[";
      int ticketCount = 0;

      for (Object ticket : ticketMap.values()) {
         ticketCount++;
         tickets_json += gson.toJson(ticket, Ticket.class);
         if (ticketCount < ticketMap.size()) {
            tickets_json += ",";
         }
      }
      tickets_json += "]";

      out.print(tickets_json);
      out.flush();
   }

   /**
    * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
    */
   protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

      StringBuffer jb = new StringBuffer();
      String line = null;

      BufferedReader reader;
      try {
         reader = request.getReader();
         while ((line = reader.readLine()) != null)
            jb.append(line);
      }
      catch (IOException e) {
         response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error serializing request");
      }

      Gson gson = new Gson();
      // create a ticket object from the request
      Ticket request_ticket = gson.fromJson(jb.toString(), Ticket.class);

      // look up the relevance of the ticket
      String relevance = ticketMap.get(request_ticket.getTicketid()).getRelevance();

      String gamificationResponse = "";
      try {

         // notify the gamification service
         // productive code additionally would need some plausibility checks here, e.g. to prevent 
         // answering the same ticket thousands of times, causing DoS attacks etc.
         gamificationResponse = tellGamificationServiceAboutSolvedProblem(request.getRemoteUser(), relevance);

      }
      catch (Exception e) {
         response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error sending requests to gamification service. Nested error: "
               + e.getMessage());
         return;
      }

      // create a doPost Servlet Response; include response from
      // gamification service
      String doPostResponse = "{\"gamificationservice\": " + gamificationResponse + "}";

      response.getWriter().println(doPostResponse);
   }

   /**
    * Tells the gamification service that a user has solved a problem
    * 
    * @param userId
    *           user identification to be used in the gamification service event
    * @param relevance
    *           ticket relevance, either <code>"critical"</code> or <code>""</code>
    * @return String containing the gamification service response
    * @throws NamingException
    * @throws DestinationException
    * @throws ClientProtocolException
    * @throws IOException
    */
   private String tellGamificationServiceAboutSolvedProblem(String userId, String relevance)
         throws ClientProtocolException,
         IOException,
         DestinationException,
         NamingException {

      // gamification event data
      String jsonRPCrequest = "{" + "\"method\":\"receiveEvents\"," + "\"id\":1, " + "\"params\":[" + "[{" + "\"siteId\":\""
            + GAMIFICATION_SERVICE_APP + "\"," + "\"type\":\"solvedProblem\"," + "\"playerid\":\"" + userId + "\"," + "\"data\":{"
            + "\"relevance\":\"" + relevance + "\"," + "\"processTime\":20" + "}" + "}]" + "]" + "}";

      String gamificationServiceResponse = sendGamificationEvent(jsonRPCrequest);

      String frontendResponse = "{ \"event\": \"" + jsonRPCrequest + "&app=" + GAMIFICATION_SERVICE_APP + "\", \"response\":"
            + gamificationServiceResponse + "}";

      return frontendResponse;
   }

   /**
    * Creates and fires an HTTP Post request for the provided JSON event string.
    * 
    * @param jsonString
    *           gamification service event formatted as JSON string
    * @param response
    *           original doPost HttpServletResponse for exception handling
    * @return String serialized gamification service response msg
    * @throws ClientProtocolException
    * @throws NamingException
    * @throws IOException
    * @throws DestinationException
    */
   private String sendGamificationEvent(String jsonString)
         throws ClientProtocolException,
         IOException,
         DestinationException,
         NamingException {

      HttpClient httpClient = null;
      BufferedReader reader = null;
      HttpEntity httpEntity = null;
      try {
         Context ctx = new InitialContext();

         // The default request to the Servlet will use
         // outbound-internet-destination
         HttpDestination destination = (HttpDestination) ctx.lookup("java:comp/env/" + GAMIFICATION_SERVICE_DESTINATION);

         // Create HTTP client
         httpClient = destination.createHttpClient();
         HttpPost post = new HttpPost();

         // add event data and app name as url parameters

         List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
         urlParameters.add(new BasicNameValuePair("json", jsonString));
         urlParameters.add(new BasicNameValuePair("app", GAMIFICATION_SERVICE_APP));
         post.setEntity(new UrlEncodedFormEntity(urlParameters));

         // execute request, send POST to gamification service
         HttpResponse gamificationServiceResponse = httpClient.execute(post);
         logger.debug("[Helpdesk TicketServlet] sending event to destination " + GAMIFICATION_SERVICE_DESTINATION + " (App: "
               + GAMIFICATION_SERVICE_APP + ") --> " + jsonString);

        
         // serialize gamification service response

         StringBuffer buffer = new StringBuffer();
         httpEntity = gamificationServiceResponse.getEntity();
         reader = new BufferedReader(new InputStreamReader(httpEntity.getContent(), Charset.forName("UTF-8")));
         String l;
         while ((l = reader.readLine()) != null) {
            buffer.append(l);
         }

         // Check response status code
         int statusCode = gamificationServiceResponse.getStatusLine().getStatusCode();
         if (statusCode != HTTP_OK) {
            logger.error(gamificationServiceResponse.getStatusLine().toString());
            throw new IllegalStateException(gamificationServiceResponse.getStatusLine().toString());
         }
         
         
         return buffer.toString();
      }
      finally {
         // When HttpClient instance is no longer needed, shut down the
         // connection manager to ensure immediate
         // deallocation of system resources
         if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
         }
         if (null != httpEntity) {
            EntityUtils.consume(httpEntity);
         }
         if (reader != null) {
            try {
               reader.close();
            }
            catch (IOException e) {
               logger.error("Failed to release resources after exception", e.getMessage());
            }
         }
      }
   }
}
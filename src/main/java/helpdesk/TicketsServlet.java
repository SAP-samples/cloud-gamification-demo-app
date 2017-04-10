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
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sap.core.connectivity.api.DestinationException;
import com.sap.core.connectivity.api.http.HttpDestination;
import com.sap.security.auth.login.LoginContextFactory;

/**
 * This servlet serves as ticket provider (GET) and allows helpDesk users to send their ticket responses (POST).
 * 
 * on GET: a hard coded array of three tickets is returned in JSON format
 * 
 * on POST: the servlet looks up the ticket and its relevance (critical or not), then creates and sends an event for the
 * gamification service
 * 
 */
public class TicketsServlet extends HttpServlet {
   private static final long serialVersionUID = 1L;
   protected static final Logger logger = LoggerFactory.getLogger(TicketsServlet.class);

   private Map<Integer, Ticket> ticketMap;
   // default appname = HelpDesk, when not set in VM args
   private static final String GAMIFICATION_SERVICE_APP = System.getProperty("gamification.demoapp.appname", "HelpDesk");

   // This destination needs to be configured in your SAP Cloud Platform runtime.
   // As you saw in the documentation, if you run this on your local SAP Cloud Platform configure the following
   // destination (double click server > Connectivity > New Destination), name "gsdest", URL
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

      Ticket ticket = new Ticket(10056, "", "Hi! I just received my new computer and the button on the CD tray doesn't work properly.",
            "Jack", 183, new Date(), "");
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
   @Override
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
   @Override
   protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

      // check if the user context exists, if not login
      String userId = request.getRemoteUser();
      if (userId == null) {
         LoginContext loginContext;
         try {
            loginContext = LoginContextFactory.createLoginContext();
            loginContext.login();
            userId = request.getRemoteUser();
         }
         catch (LoginException e) {
            logger.debug("Login failed.", e);
         }
      }

      StringBuffer jb = new StringBuffer();
      int intC;
      BufferedReader reader;
      try {
         reader = request.getReader();
         while ((intC = reader.read()) != -1)
            jb.append((char) intC);
         // limit upload to 50MB
         if (jb.length() > 52428800) {
            throw new IllegalArgumentException("input too long");
         }
      }
      catch (IOException e) {
         response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
         response.getWriter().write("Error while serializing request");
         return;
      }

      String gamificationServiceResponse = "";

      // Create and prepare player.
      if (jb.toString().equals("initPlayer")) {
         try {
            // Check if player already exists in the gamification service
            // if not, create player and then trigger the init rule to automatically apply initial missions to the
            // player
            if (!this.checkPlayerExists(userId) && this.createPlayer(userId)) {
               this.initPlayerForHelpDesk(userId);
            }

         }
         catch (Exception e) {

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error while creating / initializing player: " + e.getMessage());
            return;
         }
      }
      else {

         // check the request data, pick up the related ticket, compare its relevance and send the event to the GS
         // service

         Gson gson = new Gson();
         // create a ticket object from the request
         Ticket request_ticket = gson.fromJson(jb.toString(), Ticket.class);

         // look up the relevance of the ticket
         String relevance = ticketMap.get(request_ticket.getTicketid()).getRelevance();

         try {

            // notify the gamification service
            // productive code additionally would need some plausibility checks here, e.g. to prevent
            // answering the same ticket thousands of times, causing DoS attacks etc.
            gamificationServiceResponse = tellGamificationServiceAboutSolvedProblem(userId, relevance);

         }
         catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (e.getMessage().contains(GAMIFICATION_SERVICE_APP)) {
               // shorten the original message
               // "App HelpDesk does not exist. Please provide an existing App or none for defaultApp."
               response.getWriter().write("Make sure '" + GAMIFICATION_SERVICE_APP + "' app has been created.");
            }
            else {
               response.getWriter().write("Gamification service responded: " + e.getMessage());
            }
            return;
         }

      }

      // include response from gamification service
      String doPostResponse = "{\"gamificationservice\": " + gamificationServiceResponse + "}";
      response.getWriter().println(doPostResponse);
   }

   /**
    * Check if player exists in gamification service
    * 
    * @param playerId
    * @return
    * @throws ClientProtocolException
    * @throws IOException
    * @throws DestinationException
    * @throws NamingException
    */
   private boolean checkPlayerExists(String playerId) throws ClientProtocolException, IOException, DestinationException, NamingException {

      String jsonRPCrequest = "{\"method\": \"getPlayer\", \"params\": [ \"" + playerId + "\" ]}";

      String gamificationServiceResponse = requestGamificationService(jsonRPCrequest);

      JsonParser parser = new JsonParser();
      JsonObject result = (JsonObject) parser.parse(gamificationServiceResponse);

      if (result.get("result").isJsonNull()) {
         return false;
      }
      else {
         return true;
      }
   }

   /**
    * Create a new player with given id
    * 
    * @param playerId
    * @return
    * @throws ClientProtocolException
    * @throws IOException
    * @throws DestinationException
    * @throws NamingException
    */
   private boolean createPlayer(String playerId) throws ClientProtocolException, IOException, DestinationException, NamingException {

      String jsonRPCrequest = "{\"method\":\"createPlayer\",\"params\":[\"" + playerId + "\"]}";

      String gamificationServiceResponse = requestGamificationService(jsonRPCrequest);

      JsonParser parser = new JsonParser();
      JsonObject result = (JsonObject) parser.parse(gamificationServiceResponse);

      if (result.get("result").toString().equals("true")) {
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * at the moment players are initiated implicitly, as soon as there are events sent for their playerID
    * 
    * @param playerId
    * gamification service player id
    * @return
    * @throws ClientProtocolException
    * @throws IOException
    * @throws DestinationException
    * @throws NamingException
    */
   private String initPlayerForHelpDesk(String playerId) throws ClientProtocolException, IOException, DestinationException, NamingException {

      // create json event string to initialize players
      String jsonRPCrequest = getEventStringFor(playerId, "initPlayerForApp", null);

      String gamificationServiceResponse = requestGamificationService(jsonRPCrequest);

      // create a request response that contains the original request and forwards the response from the gamification
      // service
      return "{ \"event\": \"" + jsonRPCrequest + "&app=" + GAMIFICATION_SERVICE_APP + "\", \"response\":" + gamificationServiceResponse
            + "}";
   }

   /**
    * Tells the gamification service that a user has solved a problem
    * 
    * @param playerId
    * gamification service player id
    * @param relevance
    * ticket relevance, either <code>"critical"</code> or <code>""</code>
    * @return String containing the gamification service response
    * @throws NamingException
    * @throws DestinationException
    * @throws ClientProtocolException
    * @throws IOException
    */
   private String tellGamificationServiceAboutSolvedProblem(String playerId, String relevance) throws ClientProtocolException, IOException,
         DestinationException, NamingException {

      // gamification event data
      String jsonRPCrequest = getEventStringFor(playerId, "solvedProblem", relevance);

      String gamificationServiceResponse = requestGamificationService(jsonRPCrequest);

      // create a request response that contains the original request and forwards the response from the gamification
      // service
      return "{ \"event\": " + jsonRPCrequest + "&app=" + GAMIFICATION_SERVICE_APP + "\", \"response\":" + gamificationServiceResponse
            + "}";

   }

   /**
    * creates a json string which can be used to send certain events for users
    * 
    * @param playerId
    * @param eventName
    * @param relevance
    * @return
    */
   private String getEventStringFor(String playerId, String eventName, String relevance) {

      String response = "{\"method\":\"handleEvent\", \"params\":[{\"type\":\"" + eventName + "\",\"playerid\":\"" + playerId
            + "\",\"data\":{";

      if (relevance != null) {
         response += "\"relevance\":\"" + relevance + "\",\"processTime\":20";
      }
      response += "}}]}";
      return response;
   }

   /**
    * Creates and fires an HTTP Post request for the provided JSON event string.
    * 
    * @param jsonString
    * gamification service event formatted as JSON string
    * @param response
    * original doPost HttpServletResponse for exception handling
    * @return String serialized gamification service response msg
    * @throws ClientProtocolException
    * @throws NamingException
    * @throws IOException
    * @throws DestinationException
    */
   private String requestGamificationService(String jsonString) throws ClientProtocolException, IOException, DestinationException,
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
         logger.debug("Sending request to destination " + GAMIFICATION_SERVICE_DESTINATION + " (App: " + GAMIFICATION_SERVICE_APP
               + ") --> " + jsonString);
         HttpResponse gamificationServiceResponse = httpClient.execute(post);

         // serialize gamification service response

         StringBuffer buffer = new StringBuffer();
         httpEntity = gamificationServiceResponse.getEntity();
         reader = new BufferedReader(new InputStreamReader(httpEntity.getContent(), Charset.forName("UTF-8")));
         String l;
         while ((l = reader.readLine()) != null) {
            buffer.append(l);
         }
         String response = buffer.toString();

         // Check response status code
         int statusCode = gamificationServiceResponse.getStatusLine().getStatusCode();
         if (statusCode != HTTP_OK) {
            String errorMessage = null;
            try {
               errorMessage = new JsonParser().parse(response).getAsJsonObject().get("error").getAsString();
            }
            catch (Exception e) {
               errorMessage = gamificationServiceResponse.getStatusLine().toString() + ": " + response;
            }
            throw new IllegalStateException(errorMessage);
         }

         return response;
      }
      finally {
         // When HttpClient instance is no longer needed, shut down the
         // connection manager to ensure immediate
         // deallocation of system resources
         if (httpClient != null) {
            httpClient.getConnectionManager().shutdown();
         }
         if (null != httpEntity) {
            try {
               EntityUtils.consume(httpEntity);
            }
            catch (IOException e) {
               logger.debug("Failed to consume HttpEntity", e.getMessage());
            }
         }
         if (reader != null) {
            try {
               reader.close();
            }
            catch (IOException e) {
               logger.debug("Failed to release resources after exception", e.getMessage());
            }
         }
      }
   }
}
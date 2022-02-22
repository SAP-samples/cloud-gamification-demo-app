package com.sap.gamification.widgets;

import static java.net.HttpURLConnection.HTTP_OK;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.sap.core.connectivity.api.authentication.AuthenticationHeader;
import com.sap.core.connectivity.api.authentication.AuthenticationHeaderProvider;
import com.sap.core.connectivity.api.configuration.ConnectivityConfiguration;
import com.sap.core.connectivity.api.configuration.DestinationConfiguration;

/**
 * This servlet acts as a proxy to consume Gamification Service widgets.
 * Depending on the deployment reported by VM arguments, it communicates
 * directly (same origin) or via destination (foreign destination).
 * Available Widgets
 * - Notifications Widget. Currently there is one widget integrated to HelpDesk,
 * namely notifications. The resources (js, css, images etc.) of the
 * Notifications widget have been copied to src/main/webapp/widgets. Calls to
 * the gamification service, i.e. retrieving the server time and polling new
 * notifications, go through this proxy servlet.
 */
public class ProxyServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyServlet.class);

    private static final String GAMIFICATION_SERVICE_WIDGET_DESTINATION = "gswidgetdest";
    private static final String GAMIFICATION_SERVICE_APPNAME = System.getProperty("gamification.demoapp.appname",
            "HelpDesk");

    private final ThreadLocal<String> csrfToken = new ThreadLocal<>();
    private static final String TOKEN_NAME = "X-CSRF-Token";

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String user = request.getRemoteUser();
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please login before using this service.");
            return;
        }

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        ConnectionInfo connectionInfo = new ConnectionInfo();
        connectionInfo.setAppName(GAMIFICATION_SERVICE_APPNAME);

        Gson gson = new Gson();

        try {
            // Get HTTP destination
            Context ctx = new InitialContext();
            // look up the connectivity configuration API "connectivityConfiguration"
            ConnectivityConfiguration configuration = (ConnectivityConfiguration) ctx
                    .lookup("java:comp/env/connectivityConfiguration");

            // get destination configuration for GAMIFICATION_SERVICE_WIDGET_DESTINATION
            DestinationConfiguration destConfiguration = configuration
                    .getConfiguration(GAMIFICATION_SERVICE_WIDGET_DESTINATION);

            URL url = new URL(destConfiguration.getProperty("URL"));
            connectionInfo.setOrigin(url.getProtocol() + "://" + url.getAuthority());
            out.print(gson.toJson(connectionInfo));
            out.flush();
        } catch (NamingException e) {
            String errorMessage = "Lookup of destination failed with reason: " + e.getMessage() + ". See "
                    + "logs for details. Hint: Make sure to have the destination "
                    + GAMIFICATION_SERVICE_WIDGET_DESTINATION + " configured.";
            LOGGER.debug(errorMessage, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, errorMessage);
        } catch (Exception e) {
            LOGGER.debug(e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Invalid service origin: " + e.getMessage());
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String user = request.getRemoteUser();
            if (user == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Pleas login before using this service.");
                return;
            }

            // POST message to Gamification Service via Destination gswidgetdest
            String json = request.getParameter("json");
            String app = request.getParameter("app");

            if (json == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid Request. Expects parameters 'json' and 'app' as url parameters.");
            } else {
                // proxy gamification service call
                String gamificationResponse = this.callGamificationService(json, app);
                response.getWriter().println(gamificationResponse);
            }
        } catch (Exception e) {
            LOGGER.debug(
                    "Forwarding data to Gamification Service failed. Hint: Make sure to have an HTTP proxy configured in your "
                            + "local Eclipse environment in case your environment uses an HTTP proxy for the outbound Internet communication.",
                    e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Creates and fires an HTTP Post request for the provided json API request
     * string. Connects via gswidgetdest destination.
     *
     * @param jsonString
     *                   gamification service event formatted as json string
     * @param app
     *                   optional: provide name of app
     * @param response
     *                   original doPost HttpServletResponse for exception handling
     *
     * @return String serialized gamification service response msg
     *
     * @throws ServletException
     * @throws IOException
     */
    private String callGamificationService(String jsonString, String app) throws ServletException, IOException {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            // Get HTTP destination
            Context ctx = new InitialContext();

            // look up the connectivity configuration API "connectivityConfiguration"
            ConnectivityConfiguration configuration = (ConnectivityConfiguration) ctx
                    .lookup("java:comp/env/connectivityConfiguration");

            AuthenticationHeaderProvider authHeaderProvider = (AuthenticationHeaderProvider) ctx
                    .lookup("java:comp/env/authenticationHeaderProvider");

            // get destination configuration for GAMIFICATION_SERVICE_WIDGET_DESTINATION
            DestinationConfiguration destConfiguration = configuration
                    .getConfiguration(GAMIFICATION_SERVICE_WIDGET_DESTINATION);

            String url = destConfiguration.getProperty("URL");
            HttpPost post = new HttpPost(url);
            post.addHeader(TOKEN_NAME, csrfToken.get());

            // add JSON request and app name as url parameters
            List<NameValuePair> urlParameters = new ArrayList<>();
            urlParameters.add(new BasicNameValuePair("json", jsonString));

            if (app != null) {
                urlParameters.add(new BasicNameValuePair("app", app));
            }
            post.setEntity(new UrlEncodedFormEntity(urlParameters));

            // execute request, send POST to gamification service
            HttpResponse gamificationServiceResponse = null;

            String authType = destConfiguration.getProperty("Authentication");
            switch (authType) {
                case "AppToAppSSO":
                    // retrieve the authorization header for application-to-application SSO
                    AuthenticationHeader appToAppSSOHeader = authHeaderProvider.getAppToAppSSOHeader(url);
                    post.addHeader(appToAppSSOHeader.getName(), appToAppSSOHeader.getValue());
                    break;
                case "BasicAuthentication":
                    // only used for local
                    if (url.contains("localhost")) {
                        post.addHeader(HttpHeaders.AUTHORIZATION,
                                "Basic " + Base64.getEncoder()
                                        .encodeToString((destConfiguration.getProperty("User") + ":"
                                                + destConfiguration.getProperty("Password"))
                                                        .getBytes(StandardCharsets.UTF_8)));
                    }
                    break;
                default:
                    break;
            }
            gamificationServiceResponse = httpClient.execute(post);
            LOGGER.debug("[Helpdesk TicketServlet] sending event to destination {} (App: {}) --> {}",
                    GAMIFICATION_SERVICE_WIDGET_DESTINATION, app, jsonString);

            String response = serializeHTTPResponse(gamificationServiceResponse);

            // Check response status code
            int statusCode = gamificationServiceResponse.getStatusLine().getStatusCode();
            if (statusCode != HTTP_OK) {

                // CSRF Token missing
                if (gamificationServiceResponse.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN
                        && gamificationServiceResponse.getFirstHeader(TOKEN_NAME).getValue().equals("Required")) {

                    // get token
                    // create http context with cookie store
                    HttpContext httpContext = new BasicHttpContext();
                    httpContext.setAttribute(HttpClientContext.COOKIE_STORE, new BasicCookieStore());

                    // get token
                    csrfToken.remove();
                    csrfToken.set(fetchToken(httpClient, httpContext, url));

                    post.setHeader(TOKEN_NAME, csrfToken.get());

                    if (authType.equals("AppToAppSSO")) {
                        // retrieve the authorization header for
                        // application-to-application SSO
                        AuthenticationHeader appToAppSSOHeader = authHeaderProvider.getAppToAppSSOHeader(url);
                        post.setHeader(appToAppSSOHeader.getName(), appToAppSSOHeader.getValue());
                    }

                    gamificationServiceResponse = httpClient.execute(post, httpContext);
                    LOGGER.debug("[Helpdesk TicketServlet] sending event to destination {} (App: {}) --> {}",
                            GAMIFICATION_SERVICE_WIDGET_DESTINATION, app, jsonString);

                    statusCode = gamificationServiceResponse.getStatusLine().getStatusCode();
                    response = serializeHTTPResponse(gamificationServiceResponse);

                    if (statusCode != HTTP_OK) {
                        String errorMessage = "Expected response status code is 200 but it is " + statusCode
                                + " . Server Response: " + response;
                        LOGGER.error(errorMessage);
                        throw new ServletException(errorMessage);
                    }

                } else {
                    String errorMessage = "Expected response status code is 200 but it is " + statusCode
                            + " . Server Response: " + response;
                    LOGGER.error(errorMessage);
                    throw new ServletException(errorMessage);
                }
            }

            return response;
        } catch (NamingException e) {
            String errorMessage = "Lookup of destination failed with reason: " + e.getMessage()
                    + ". Hint: Make sure to have the destination " + GAMIFICATION_SERVICE_WIDGET_DESTINATION
                    + " configured.";
            throw new ServletException(errorMessage, e);
        }

    }

    private String serializeHTTPResponse(HttpResponse gamificationServiceResponse) throws ServletException {
        // serialize gamification service response
        StringBuilder sb = new StringBuilder();
        HttpEntity httpEntity = gamificationServiceResponse.getEntity();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(httpEntity.getContent(), StandardCharsets.UTF_8))) {
            String l;
            while ((l = reader.readLine()) != null) {
                sb.append(l);
            }
        } catch (Exception e) {
            String errorMessage = "error with POST data. could not be serialized:";
            throw new ServletException(errorMessage, e);
        } finally {

            try {
                EntityUtils.consume(httpEntity);
            } catch (IOException e) {
                LOGGER.error("Failed to release resources in finally block", e);
            }
        }
        return sb.toString();
    }

    private final String fetchToken(HttpClient httpClient, HttpContext httpContext, String url) throws IOException {
        HttpGet request = new HttpGet(url);

        request.setHeader(TOKEN_NAME, "Fetch");

        HttpResponse response = httpClient.execute(request, httpContext);
        String token = response.getFirstHeader(TOKEN_NAME).getValue();

        EntityUtils.consume(response.getEntity());

        if (token.length() < 10) {
            throw new SecurityException("Gamification server returned error while fetching CSRF token: " + response);
        }
        return token;
    }

}

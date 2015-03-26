package helpdesk;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.sap.security.um.service.UserManagementAccessor;
import com.sap.security.um.user.User;
import com.sap.security.um.user.UserProvider;

/**
 * Servlet implementation class userdata
 */
public class UserDataServlet extends HttpServlet {
   private static final long serialVersionUID = 1L;

   /**
    * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
    */
   protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      try {
         response.setContentType("application/json");
         PrintWriter out = response.getWriter();

         // retrieve user from SAML identity provider, e.g. SAP ID service
         UserProvider users = UserManagementAccessor.getUserProvider();
         User user = users.getUser(request.getUserPrincipal().getName());

         UserDataObject userdataObject = new UserDataObject();
         userdataObject.setEmail(user.getAttribute("email"));
         userdataObject.setFirstname(user.getAttribute("firstname"));
         userdataObject.setLastname(user.getAttribute("lastname"));
         userdataObject.setId(request.getUserPrincipal().getName());

         Gson gson = new Gson();

         out.print(gson.toJson(userdataObject));
         out.flush();
      }
      catch (Exception e) {
         response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving user data: " + e.getMessage());
      }
   }
}

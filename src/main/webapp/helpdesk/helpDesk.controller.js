sap.ui.controller("helpdesk.helpDesk", {

    topbarView: null,
    loginView: null,
    openTicketsView: null,
    profileView: null,
    notificationsView: null,

    onInit: function() {
        sap.ui.getCore().setModel(new sap.ui.model.json.JSONModel(), "userdata");
        sap.ui.getCore().setModel(new sap.ui.model.json.JSONModel(),
      "servicedata");

        this.loadAndConfigure();
        this.initializeData(this.initApp, this);
    },
    /**
     * loads session and user data from servlets 
     * and, when available, configures the notification widget
     */
    loadAndConfigure: function() {
        var that = this;

        $.when(
        this.getUser(),
        this.getServiceData()
      )
      .done(function(userArgs, serviceArgs) {
          that.initializeWidget(userArgs[0], serviceArgs[0]);
      })
      .fail(function() {
          console.log("could not load user/service data from servlet");
      });

    },
    /**
     * calls widget servlet for gamification service information. stores 
     * origin in the global user model
     */
    getServiceData: function() {
        return $.getJSON("ProxyServlet/JsonRPC", function(data) {
            sap.ui.getCore().getModel("servicedata").setData(data);
        });
    },

    /**
     * calls user data servlet for user information. stores user data (name,
     * email, app name) in the global user model
     *
     * when available, it also configures the notification widget (the
     * notification widget has more complex public api, allowing more
     * customization.)
     */
    getUser: function() {
        return $.getJSON("UserDataServlet", function(data) {
            sap.ui.getCore().getModel("userdata").setData(data);
        });
    },

    /**
     * Initialization of the gamification service notification widget
     *
     * at some point the notification configuration will be moved into the
     * notification widget at some point. however, in-app configuration can
     * still be used to configure the widget at runtime (see widget API)
     * when widget is available, build up its config and initialize it
     */
    initializeWidget: function(userdata, servicedata) {

        if (GSNotifications !== undefined) {
      // configure notifications with users name and style class
            var config = {
                appName: servicedata.appName,
                userName: userdata.id,
                classList: ["sap-blue"]
            };
            GSNotifications.init(config);
        }
    },

    /**
     * loads helpdesk tickets from server resets local notifications. this method
     * is called: - on application start - on logout (remark: no real logout.
     * only jumps back to welcome screen) - to reset tickets after all available
     * tickets have been sent.
     *
     * requests a list of tickets from the ticket servlet. all retrieved tickets
     * are stored in the global model will also reset any retrieved
     * notifications.
     */
    initializeData: function(callback, scope) {

        var model = new sap.ui.model.json.JSONModel();
        var modelData = {};
        // reset the data every time this method is called
        $.getJSON("TicketsServlet", function(data) {

            modelData.open_tickets = data;
            modelData.response_text = "Dear Customer,";
            modelData.active_ticket = data[0];
            model.setData(modelData);
            sap.ui.getCore().setModel(model);

            if (callback && scope) {
                callback.call(scope);
            }
        });

        // reset notifications model
        if (!sap.ui.getCore().getModel("notifications")) {
            sap.ui.getCore().setModel(new sap.ui.model.json.JSONModel(),
        "notifications");
        }

    },
    /**
     * initializes all views
     */
    initApp: function() {

        this.topbarView = sap.ui.view({
            id: "HeaderView",
            viewName: "helpdesk.header",
            type: sap.ui.core.mvc.ViewType.JS
        });
        this.loginView = sap.ui.view({
            id: "LoginView",
            viewName: "helpdesk.login",
            type: sap.ui.core.mvc.ViewType.JS
        });
        this.openTicketsView = sap.ui.view({
            id: "openTicketsView",
            viewName: "helpdesk.openTickets",
            type: sap.ui.core.mvc.ViewType.JS
        });
        this.profileView = sap.ui.view({
            id: "profileView",
            viewName: "helpdesk.profile",
            type: sap.ui.core.mvc.ViewType.JS
        });
        this.notificationsView = sap.ui.view({
            id: "notificationsView",
            viewName: "helpdesk.notifications",
            type: sap.ui.core.mvc.ViewType.JS
        });

        // start with login view
        sap.ui.getCore().byId("mainView").getController().show("login");
    },
    /**
     * generic method that takes any view name as input and navigates the demo
     * app to this view
     */
    show: function(sViewName) {
        var cont = sap.ui.getCore().byId("mainContainer");

        cont.removeAllContent();
        cont.addContent(this.topbarView);
        cont.addContent(sap.ui.getCore().byId(sViewName + "View"));

    }
});
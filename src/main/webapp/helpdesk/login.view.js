/**
 * simple welcome page.
 */
sap.ui.jsview("helpdesk.login", {

    getControllerName: function() {
        return "helpdesk.login";
    },

    createContent: function(controller) {
        var appController = sap.ui.getCore().byId("mainView").getController();
        var container = new sap.ui.commons.layout.VerticalLayout("loginView", {});
        var name = new sap.ui.commons.Label("currentUser", {
            text: {
                path: "userdata>/firstname",
                formatter: function(sValue) {
                    if (sValue) {
                        return "Hi " + sValue + ", welcome back!";
                    }
                    return "Welcome to our HelpDesk Demo.";
                }
            }
        });

        var continueLink = new sap.ui.commons.Link({
            text: "continue...",
            press: function() {
                appController.show("openTickets");
                controller.login();
            }
        });

        var loginPanel = new sap.ui.commons.layout.VerticalLayout("loginPanel");
        loginPanel.addContent(name);
        loginPanel.addContent(continueLink);

        container.addContent(loginPanel);
        return container;

    }
});
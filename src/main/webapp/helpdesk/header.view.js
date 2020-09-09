sap.ui.jsview("helpdesk.header", {

    getControllerName: function() {
        return "helpdesk.header";
    },

    createContent: function() {

        var appController = sap.ui.getCore().byId("mainView").getController();

        // custom header
        var container = new sap.ui.commons.layout.HorizontalLayout("header");
        var logoContainer = new sap.ui.commons.layout.HorizontalLayout("logo-header");
        var userContainer = new sap.ui.commons.layout.HorizontalLayout("user-header").setVisible(false);
        var menuContainer = new sap.ui.commons.layout.HorizontalLayout("menu-header").setVisible(false);
        container.addContent(logoContainer);
        container.addContent(menuContainer);
        container.addContent(userContainer);

        // sap logo in header (top left)
        logoContainer.addContent(new sap.ui.commons.Image("logo", {
            src: "resources/images/sap_logo.png"
        }));

        // add menu panel (top center)
        menuContainer.addContent(new sap.ui.commons.Link({
            text: "Open Tickets",
            press: function() {
                appController.show("openTickets");
            }
        }).addStyleClass("nav-item"));
        menuContainer.addContent(new sap.ui.commons.Link({
            text: "Notifications",
            press: function() {
                appController.show("notifications");
            }
        }).addStyleClass("nav-item"));

        // add user panel (top right)
        userContainer.addContent(new sap.ui.commons.Link("profileLink", {
            text: "{userdata>/firstname}",
            press: function() {
                appController.show("profile");
            }
        }));
        userContainer.addContent(new sap.ui.commons.Link({
            text: "Log Out",
            press: function() {
                sap.ui.getCore().byId("LoginView").getController().logoff();
            }
        }));

        return container;
    }
});

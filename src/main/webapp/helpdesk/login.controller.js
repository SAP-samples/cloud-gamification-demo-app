/**
 * welcome page controller. does no login, since logon is realized by the IP
 * however the welcome page uses a different header, which has to be be adjusted
 */
sap.ui.controller("helpdesk.login", {

    login : function() {
        sap.ui.getCore().byId("mainView").getController().show("openTickets");
        sap.ui.getCore().byId("menu-header").setVisible(true);
        sap.ui.getCore().byId("user-header").setVisible(true);

		// initialize the player by sending an empty event. currently players
		// are created implicitly with any event.
        $.ajax({
            type : "POST",
            url : "TicketsServlet",
            contentType : "application/json",
            data : "initPlayer",
            success : function(data) {
                console.log("servlet response: " + data);
            }
        });
    },
    logoff : function() {
        var mainView = sap.ui.getCore().byId("mainView");
        mainView.getController().initializeData();
        sap.ui.getCore().byId("menu-header").setVisible(false);
        sap.ui.getCore().byId("user-header").setVisible(false);
        sap.ui.getCore().byId("mainView").getController().show("login");

    }
});
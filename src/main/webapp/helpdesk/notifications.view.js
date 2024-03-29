/**
 * notifications view<br>
 * this view is used to display notifications, which are retrieved by the notifications widget. when the notifications
 * widget is available, it will poll notifications from the gamification service. for the purpose of this demo
 * application it will keep polling in short intervals, allowing to not only display notifications related to sent
 * tickets, but also to display any other notification from other gamification scenarios the user takes part in.
 */
sap.ui.jsview("helpdesk.notifications", {

    getControllerName: function () {
        return "helpdesk.notifications";
    },

    createContent: function (oController) {

        var newLbl = new sap.ui.commons.Label({
            text: "Recent"
        });
        var newLyt = new sap.ui.commons.layout.VerticalLayout("notificationsListNew", {
            width: "100%"
        });
        newLyt.bindAggregation("content", "/", oController.oNotificationsTmpl);

        return new sap.ui.commons.layout.VerticalLayout("notificationsVL", {
            /*scrollable: true,*/
            content: [newLbl, newLyt]
        });
    }
});

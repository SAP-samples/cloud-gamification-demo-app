/**
 * notifications view
 *
 * this view is used to display notifications, which are retrieved
 * by the notifications widget.
 *
 * when the notifications widget is available, it will poll notifications
 * from the gamification service. for the purpose of this demo application
 * it will keep polling in short intervals, allowing to not only dispay
 * notifications related to sent tickets, but also to display any other
 * notification from other gamification scenarios the user takes part in.
 */
sap.ui.jsview("helpdesk.notifications", {

    getControllerName: function() {
        return "helpdesk.notifications";
    },

    createContent: function(oController) {

        var newLbl = new sap.ui.commons.Label({
            text: "Recent"
        });
        var newLyt = new sap.ui.commons.layout.VerticalLayout("notificationsListNew", {
            width: "100%",
            height: "100%",
        });
        newLyt.bindAggregation("content", "/", oController.oNotificationsTmpl);

        var vL = new sap.ui.commons.layout.VerticalLayout("notificationsVL", {
            scrollable: true,
            content: [newLbl, newLyt]
        });

        return vL;
    }
});
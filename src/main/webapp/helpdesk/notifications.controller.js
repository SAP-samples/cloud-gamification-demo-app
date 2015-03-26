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
sap.ui.controller("helpdesk.notifications", {

    onInit: function() {

        if (window.GSNotifications !== undefined) {
            var oModel = this.getView().getModel();

            if (!oModel) {
                oModel = sap.ui.getCore().getModel("notifications");
                this.getView().setModel(oModel);
            }
            // when the notifications widget is available, add an event listener to the 
            // notification event
            document.addEventListener("gs-notification-new", this.getNotifications);
            document.addEventListener("gs-notification-history", this.getNotifications);
        }
    },
    getNotifications: function() {
        if (window.GSNotifications !== undefined) {
            console.log("[helpdesk] Received new notifications!");
            var notifications = GSNotifications.getNotifications();
            notifications.newNotifications = notifications.newNotifications.slice(0, 10);
            notifications.history = notifications.history.slice(0, 6);
            sap.ui.getCore().getModel("notifications").setData(notifications);
        }
    },
    oNotificationsTmpl: function(sId, oContext) {
        var hLayout = new sap.ui.commons.layout.HorizontalLayout({
                width: "100%",
                content: [
                    new sap.ui.commons.Label({
                        text: "{message}"
                    }),
                ]
            })
            .addStyleClass("notification-panel");
        return hLayout;
    }

});
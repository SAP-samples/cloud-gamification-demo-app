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
        }
    },
    getNotifications: function() {
        if (window.GSNotifications !== undefined) {
            console.log("[helpdesk] Received new notifications!");
            sap.ui.getCore().getModel("notifications").setData(GSNotifications.getNotifications());
        }
    },
    /** 
     * create meaningful messages for the user for every notification data set and add it  to the layout
     **/
    oNotificationsTmpl: function(sId, oContext) {
        var n = oContext.getObject();
        var message = "";
        switch(n.category) {

        case "POINT" :
            if (n.type !== "CUSTOM"){
                message = "+" + parseInt(n.detail, 10) + " " + n.subject;
            } else {
                message = n.message;
            }
            break;
        case "MISSION" :
            if (n.type === "ADD") {
                message = "New Mission: " + n.subject;
            } else if (n.type === "COMPLETE") {
                message = "Completed: " + n.subject;
            }
            break;
        case "BADGE":
            message += "Earned: " + n.subject;
            break;
        default :
            break;
        }

        if (n.message) {
            message += " (" + n.message + ")";
        }

        var hLayout = new sap.ui.commons.layout.VerticalLayout({
            width: "100%",
            content: [
                new sap.ui.commons.Label({
                    text: message
                }).addStyleClass("notification-message"),
                new sap.ui.commons.Label({
                    text: new Date(n.dateCreated).toLocaleString()
                }).addStyleClass("notification-date"),
            ]
        })
            .addStyleClass("notification-panel");
        return hLayout;
    }

});
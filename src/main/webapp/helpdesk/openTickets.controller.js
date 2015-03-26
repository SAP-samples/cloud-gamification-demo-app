sap.ui.controller("helpdesk.openTickets", {

    onInit: function() {
        $(".ticketPanel").hover(function() {
            $(this).toggleClass("respondBtn-active");
        });

        $(".ticketPanel").hover(function() {
            $(this).toggleClass("ticket-hover");
        });
        $(".ticketPanel").click(function() {
            $(".ticketPanel").removeClass("ticket-active");
            $(this).toggleClass("ticket-active");
        });
    },

    /** 
     * sending the response to the backend
     */
    sendTicketResponse: function(ticketid, response) {
        var data = {
            ticketid: ticketid,
            response: response
        };
        $.ajax({
            type: "POST",
            url: "TicketsServlet",
            contentType: "application/json",
            data: JSON.stringify(data),
            success: function(data) {
                console.log("servlet response: " + data);
                if (data && data.indexOf("exception") !== -1) {
                    alert("There are conflicts in your rules. "+
                        "Consider updating your Rule Engine in the GS Workbench. \n\n" + data.substring(data.indexOf("error") + 8));
                }
            }
        });
    },

    /**
     *  called when the response has been send
     */
    onTicketSendBtnPress: function(oControlEvent) {
        var dataModel = sap.ui.getCore().getModel().getData();
        var response = dataModel.response_text;
        var ticketid = dataModel.active_ticket.ticketid;

        // send a ticket response to the servlet
        this.sendTicketResponse(ticketid, response);

        // remove the sent ticket delayed to wait for css transitions
        setTimeout(this.delayedCleanUp, 850);

        // show send notification
        // slowly fade out the sent ticket
        var model = sap.ui.getCore().getModel();
        var data = model.getData();
        for (var i = 0; data.open_tickets.length; i++) {
            if (data.open_tickets[i] && data.active_ticket.ticketid === data.open_tickets[i].ticketid) {
                // fade out ticket
                $("#ticketPanel_openTicketsLayout-" + i).addClass("fadeTicket");
                break;
            }
        }
        // display "ticket sent" message
        $("#notifyInnerDiv").removeClass("hidden");
        $("div#currentTicket").removeClass("padding-bottom");

        // hide textarea and send buttons (until a new ticket is selected)
        sap.ui.getCore().byId("responseHeader").setVisible(false);
        sap.ui.getCore().byId("responseInput").setVisible(false);
        sap.ui.getCore().byId("responseBtn").setVisible(false);

    },
    /**
     * called after a ticket has been send
     * - removes ticket from local model (no backend service available for demo app)
     * - triggers ticket reload after last ticket has been send
     */
    delayedCleanUp: function() {
        var model = sap.ui.getCore().getModel();
        var data = model.getData();

        for (var i = 0; data.open_tickets.length; i++) {

            if (data.active_ticket.ticketid === data.open_tickets[i].ticketid) {

                // remove the closed ticket from the open_tickets array
                data.open_tickets.splice(i, 1);
                sap.ui.getCore().getModel().setData(data);
                sap.ui.getCore().getModel().updateBindings();

                // when last ticket has been resolved, load new tickets from back end                    
                if (sap.ui.getCore().getModel().getData().open_tickets.length < 1) {
                    sap.ui.getCore().byId("mainView").getController().initializeData();
                }
                return;
            }
        }
    },
    openTicketsTmpl: function(sId, oContext) {
        var controller = sap.ui.getCore().byId("openTicketsView").getController();
        var vLayout = new sap.ui.commons.layout.VerticalLayout("ticketPanel_" + sId, {
                width: "100%"
            })
            .data("ticketid", oContext.getProperty("ticketid"))
            .addStyleClass("ticketPanel")
            .attachBrowserEvent("click", controller.ticketSelected);

        var ticket = new sap.ui.commons.Label({
                text: "{issue}",
            })
            .addStyleClass("ticket-issue");

        var ticketDetails = new sap.ui.commons.layout.HorizontalLayout({
            content: [
                new sap.ui.commons.Label({
                    text: "#" + oContext.getProperty("ticketid")
                }).addStyleClass("ticket-id"),
                new sap.ui.commons.Label({
                    text: "{customer_name}"
                }).addStyleClass("ticket-customer"),
                new sap.ui.commons.Label({
                    text: {
                        path: "date",
                        formatter: function(val) {
                            return val;
                        }
                    }
                }).addStyleClass("ticket-date")
            ]
        }).addStyleClass("ticketDetails");

        vLayout.addContent(ticket);
        vLayout.addContent(ticketDetails);

        if (oContext.getProperty("relevance") === "critical") vLayout.addStyleClass("criticalTicket");
        return vLayout;
    },
    // called whenever the user selects a ticket
    ticketSelected: function(oControlEvent) {
        var ticket = sap.ui.getCore().byId(oControlEvent.currentTarget.getAttribute("id"));
        var ticketid = ticket.data("ticketid");
        var modelData = sap.ui.getCore().getModel().getData();

        for (var i = 0; i < modelData.open_tickets.length; i++) {
            if (modelData.open_tickets[i].ticketid === ticketid) {
                modelData.active_ticket = modelData.open_tickets[i];
            }
        }

        sap.ui.getCore().getModel().setData(modelData);

        $("#notifyInnerDiv").addClass("hidden");
        $("div#currentTicket").addClass("padding-bottom");
        sap.ui.getCore().byId("responseHeader").setVisible(true);
        sap.ui.getCore().byId("responseInput").setVisible(true);
        sap.ui.getCore().byId("responseBtn").setVisible(true);
        sap.ui.getCore().byId("responseInput").setValue("Dear " +
            modelData.active_ticket.customer_name + ",");

    }
});
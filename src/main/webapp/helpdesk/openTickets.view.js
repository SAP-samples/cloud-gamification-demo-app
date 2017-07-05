/**
 * this is the main view of our helpdesk demo app
 * integration details for the communication with the gamification service
 * can be found in the openTickets controller
 */
sap.ui.jsview("helpdesk.openTickets", {

    getControllerName: function() {
        return "helpdesk.openTickets";
    },

    createContent: function(controller) {

        var oOpenTickets = new sap.ui.commons.layout.VerticalLayout("openTicketsLayout", {
            width: "100%",
            height: "100%",
        });

        oOpenTickets.bindAggregation("content", "/open_tickets", controller.openTicketsTmpl);

        // response View
        var ticketResponseLayout = new sap.ui.commons.layout.VerticalLayout("currentTicket", {
            width: "100%"
        }).addStyleClass("padding-bottom");

        var ticketHeader = new sap.ui.commons.layout.HorizontalLayout("currentTicketHeader", {
            width: "100%",
            content: [
                new sap.ui.commons.Label("currentTicketId", {
                    text: {
                        path: "/active_ticket/ticketid",
                        formatter: function(val) {
                            return "#" + val;
                        }
                    }
                }),
                new sap.ui.commons.Label("currentTicketRelevance", {
                    text: {
                        path: "/active_ticket/relevance",
                        formatter: function(val) {
                            return (val === "critical") ? val : "";
                        }
                    }
                })
            ]
        });

        var ticketIssue = new sap.ui.commons.Label("currentTicketIssue", {
            text: "{/active_ticket/issue}"
        });

        var ticketDetails = new sap.ui.commons.layout.HorizontalLayout("currentTicketDetails", {
            height: "300px",
            width: "100%",
            allowWrapping: true,
            content: [
                new sap.ui.commons.Label("current-ticket-customer-name", {
                    text: {
                        path: "/active_ticket/customer_name",
                        formatter: function(value) {
                            return "From " + value;
                        }
                    }
                }),
                new sap.ui.commons.Label("current-ticket-date", {
                    text: "{/active_ticket/date}"
                })
            ]
        });

        var divider = new sap.ui.commons.HorizontalDivider("divider");

        var notification = new sap.ui.core.HTML("notify", {
            width: "100%",
            height: "80px",
            content: "<div id='notifyInnerDiv' class='hidden'>Your response has been sent. You may continue.</div>",
            isVisible: false
        }).addStyleClass("hidden");


        var responseHeader = new sap.ui.commons.Label("responseHeader", {
            text: "Your Response"
        });
        var responseTextArea = new sap.ui.commons.TextArea("responseInput", {
            value: {
                path: "/response_text"
            },
            width: "80%",
            height: "140px"
        });

        ticketResponseLayout.addContent(ticketHeader);
        ticketResponseLayout.addContent(ticketIssue);
        ticketResponseLayout.addContent(ticketDetails);
        ticketResponseLayout.addContent(divider);
        ticketResponseLayout.addContent(notification);
        ticketResponseLayout.addContent(responseHeader);
        ticketResponseLayout.addContent(responseTextArea);

        var sendBtn = new sap.ui.commons.Button("responseBtn", {
            text: "Send Answer",
            press: [controller.onTicketSendBtnPress, controller]
        });

        var oResponse = new sap.ui.commons.layout.VerticalLayout("ticketResponse", {
            width: "100%",
            height: "100%",
            content: [ticketResponseLayout, sendBtn]
        });

        var horizontalLayout = new sap.ui.commons.layout.HorizontalLayout("ticketsWorkbench", {
            content: [oOpenTickets, oResponse]
        });

        return horizontalLayout;


    }
});
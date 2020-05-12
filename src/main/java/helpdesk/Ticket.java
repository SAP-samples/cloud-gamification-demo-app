package helpdesk;

import java.util.Date;

public class Ticket {
    private int ticketid;
    private String relevance;
    private String issue;
    private String customerName;
    private int customerId;
    private Date date;
    private String response;

    public Ticket(int ticketid, String relevance, String issue, String customerName, int customerId, Date date,
            String response) {
        super();
        this.ticketid = ticketid;
        this.relevance = relevance;
        this.issue = issue;
        this.customerName = customerName;
        this.customerId = customerId;
        this.date = date;
        this.response = response;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public int getTicketid() {
        return ticketid;
    }

    public void setTicketid(int ticketid) {
        this.ticketid = ticketid;
    }

    public String getRelevance() {
        return relevance;
    }

    public void setRelevance(String relevance) {
        this.relevance = relevance;
    }
}

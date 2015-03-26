package helpdesk;

import java.util.Date;

public class Ticket {
   public int ticketid;
   public String relevance;
   public String issue;
   public String customer_name;
   public int customer_id;
   public Date date;
   public String response;

   public Ticket(int ticketid, String relevance, String issue, String customer_name, int customer_id, Date date, String response) {
      super();
      this.ticketid = ticketid;
      this.relevance = relevance;
      this.issue = issue;
      this.customer_name = customer_name;
      this.customer_id = customer_id;
      this.date = date;
      this.response = response;
   }

   public String getCustomer_name() {
      return customer_name;
   }

   public void setCustomer_name(String customer_name) {
      this.customer_name = customer_name;
   }

   public int getCustomer_id() {
      return customer_id;
   }

   public void setCustomer_id(int customer_id) {
      this.customer_id = customer_id;
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

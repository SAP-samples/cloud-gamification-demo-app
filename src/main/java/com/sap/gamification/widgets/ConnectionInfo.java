package com.sap.gamification.widgets;

public class ConnectionInfo {
   private String origin;
   private String appName;
   
   public String getOrigin() {
      return origin;
   }

   public void setOrigin(String endpoint) {
      this.origin = endpoint;
   }

   public String getAppName() {
      return appName;
   }

   public void setAppName(String appName) {
      this.appName = appName;
   }
   
}

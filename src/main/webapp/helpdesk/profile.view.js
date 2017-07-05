sap.ui.jsview("helpdesk.profile", {

    getControllerName : function() {
        return "helpdesk.profile";
    },
    
    createContent : function(oController) {
        var layout = new sap.ui.commons.layout.VerticalLayout("profileViewLayout", {width: "100%"});
        var rendered = false;
        
        this.iFrameUP = new sap.ui.core.HTML("iframeProfile", {
            width: "100%",
            content: ""
        }).addDelegate({

            onAfterRendering: function (e) {
                var dataModelUser = sap.ui.getCore().getModel("userdata").getData();
                var userid = dataModelUser.id;
                                
                var dataModelService = sap.ui.getCore().getModel("servicedata").getData();                
                var origin = dataModelService.origin;
                var app = dataModelService.app;
                
                if (!app) {
                    console.warn("did you forget the appname in the VM args for your server?");
                    app = "HelpDesk";
                }
            
                if (!rendered) {
                    e.srcControl.setContent("" +
                      "<iframe src='" + origin + "/gamification/userprofile.html?name=" + userid + "&app=" + app + "'" +
                      " width='100%' height='100%' frameBorder='0'>" + "Alternate text if the iframe cannot be rendered" + 
                      "</iframe>");

                    rendered = true;
                }

                // whenever the app navigates to the profile view, recalc its height
                var headerHeight = $("#mainContainer>div:first-child").height();
                var profileHeight = $(document).height() - headerHeight;
                $("div#profileViewLayout > div:first-child").height(profileHeight);

            }
        });

        layout.addContent(this.iFrameUP);
        return layout;
    }
});

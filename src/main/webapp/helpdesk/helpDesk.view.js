sap.ui.jsview("helpdesk.helpDesk", {

    getControllerName : function() {
        return "helpdesk.helpDesk";
    },
	
    createContent : function(oController) {
		
        var container = new sap.ui.commons.layout.VerticalLayout("mainContainer");
		// see  onInit() method in controller

        return container;
    },
});

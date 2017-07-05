jQuery.sap.log.setLevel(jQuery.sap.log.Level.NONE);

/**
 * Helper functions in order to access url parameters using JQuery
 */
$.extend({

    getUrlVars: function() {
        var vars = [],
            hash;
        var hashes = window.location.href.slice(
            window.location.href.indexOf("?") + 1).split("&");
        for (var i = 0; i < hashes.length; i++) {
            hash = hashes[i].split("=");
            vars.push(hash[0]);
            vars[hash[0]] = hash[1];
        }
        return vars;
    },
    getUrlVar: function(name) {
        return $.getUrlVars()[name];
    }

});
sap.ui.localResources("helpdesk");

var mainView = sap.ui.view({
    id: "mainView",
    viewName: "helpdesk.helpDesk",
    type: sap.ui.core.mvc.ViewType.JS,
    height: "100%"
});
mainView.placeAt("content");

// dealing with userprofile-iframe height on init and after resize
$(document).ready(function() {
    var headerHeight = $("#mainContainer>div:first-child").height();
    var profileHeight = $(window).height() - headerHeight;
    $("div#profileViewLayout > div:first-child").height(profileHeight);
});

$(window).resize(function() {
    var headerHeight = $("#mainContainer>div:first-child").height();
    var profileHeight = $(window).height() - headerHeight;
    $("div#profileViewLayout > div:first-child").height(profileHeight);
});
var locale = null;

//event doesn't work
var messagesLoaded = false;

let lang = getCookie("panel_lang");
if (lang == null) {
    lang = "en";
}

$.ajax({
    type: "GET",
    url: "/api/getPanelLocale?v=2lang=" + lang,
    success: function (response) {
        locale = response;
        messagesLoaded = true;
    }
});

function getMessage(key, replacers) {
    let message = locale[key];

    if (replacers && replacers.length > 0) {
        replacers.forEach((replacer, index) => {
            message = message.replace(`{${index}}`, replacer);
        });
    }

    return message;
}

function getCookie(name) {
    let cookieArr = document.cookie.split(";");

    for(let i = 0; i < cookieArr.length; i++) {
        let cookiePair = cookieArr[i].split("=");

        if(name === cookiePair[0].trim()) {
            return decodeURIComponent(cookiePair[1]);
        }
    }

    return null;
}
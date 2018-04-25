if (location.protocol === "https:") {
    var socketSource = new WebSocket("wss://" + location.host + webDavSettings.WebSocketPath);
} else {
    var socketSource = new WebSocket("ws://" + location.host + webDavSettings.WebSocketPath);
}


socketSource.addEventListener('message', function (e) {
    var notifyObject = JSON.parse(e.data);

    // Removing domain and trailing slash.
    var regExp = new RegExp("^\/" + webDavSettings.WebSocketPath + "|\/$", "g");
    var currentLocation = location.pathname.replace(regExp, '');
    // Checking message type after receiving.
    if (notifyObject.eventType === "refresh") {
        // Refresh folder structure if any item in this folder is updated or new item is created.
        if (currentLocation.toUpperCase() === notifyObject.folderPath.toUpperCase()) {
            WebDAVController.Reload();
        }
    } else if (notifyObject.eventType === "delete") {
        if (notifyObject.folderPath.substring(0, notifyObject.folderPath.lastIndexOf('/')).toUpperCase() === currentLocation.toUpperCase()) {
            // Refresh folder structure if any item in this folder is deleted.
            WebDAVController.Reload();
        } else if (currentLocation.toUpperCase().indexOf(notifyObject.folderPath.toUpperCase()) === 0) {
            // Redirect client to the root folder if current path is being deleted.
            var originPath = location.origin + webDavSettings.ApplicationPath;
            history.pushState({ Url: originPath }, '', originPath);
            WebDAVController.NavigateFolder(originPath);
        }
    }
}, false);
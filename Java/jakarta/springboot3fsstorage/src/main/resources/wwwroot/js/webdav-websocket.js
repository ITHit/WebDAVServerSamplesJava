function WebSocketConnect() {
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
        if (notifyObject.EventType === "updated" || notifyObject.EventType === "created" || notifyObject.EventType === "locked" ||
            notifyObject.EventType === "unlocked") {
            // Refresh folder structure if any item in this folder is updated or new item is created.
            if (notifyObject.ItemPath.substring(0, notifyObject.ItemPath.lastIndexOf('/')).toUpperCase() === currentLocation.toUpperCase()) {
                WebDAVController.Reload();
            }
        } else if (notifyObject.EventType === "moved") {
            // Refresh folder structure if file or folder is moved.
            if (notifyObject.ItemPath.substring(0, notifyObject.ItemPath.lastIndexOf('/')).toUpperCase() === currentLocation.toUpperCase() ||
                notifyObject.TargetPath.substring(0, notifyObject.TargetPath.lastIndexOf('/')).toUpperCase() === currentLocation.toUpperCase()) {
                WebDAVController.Reload();
            }

        } else if (notifyObject.EventType === "deleted") {
            if (notifyObject.ItemPath.substring(0, notifyObject.ItemPath.lastIndexOf('/')).toUpperCase() === currentLocation.toUpperCase()) {
                // Refresh folder structure if any item in this folder is deleted.
                WebDAVController.Reload();
            } else if (currentLocation.toUpperCase().indexOf(notifyObject.ItemPath.toUpperCase()) === 0) {
                // Redirect client to the root folder if current path is being deleted.
                var originPath = location.origin + "/";
                history.pushState({ Url: originPath }, '', originPath);
                WebDAVController.NavigateFolder(originPath);
            }
        }
    }, false);

    socketSource.addEventListener('error', function (err) {
        console.error('Socket encountered error: ', err.message, 'Closing socket');
        socketSource.close();
    });

    socketSource.addEventListener('close', function (e) {
        console.log('Socket is closed. Reconnect will be attempted in 5 seconds.', e.reason);
        setTimeout(function () {
            WebSocketConnect();
        }, 5000);
    });
}
WebSocketConnect();
package com.ithit.webdav.samples.fsstorageservlet.websocket

import javax.websocket.EncodeException
import javax.websocket.Encoder
import javax.websocket.EndpointConfig

/**
 * Encodes notification object to the JSON
 */
class NotificationEncoder : Encoder.Text<WebSocketServer.Notification> {

    override fun init(config: EndpointConfig) {}

    override fun destroy() {}

    @Throws(EncodeException::class)
    override fun encode(notification: WebSocketServer.Notification): String {
        var target = ""
        if (notification is WebSocketServer.MovedNotification) {
            target = "\"TargetPath\" : \"" + notification.targetPath + "\" ,"
        }
        return "{" + target +
                "\"ItemPath\" : \"" + notification.itemPath + "\" ," +
                "\"EventType\" : \"" + notification.operation + "\"" +
                "}"
    }
}

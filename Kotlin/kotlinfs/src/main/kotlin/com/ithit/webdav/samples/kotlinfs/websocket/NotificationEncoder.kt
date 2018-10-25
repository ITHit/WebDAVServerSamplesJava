package com.ithit.webdav.samples.kotlinfs.websocket

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
        return "{" +
                "\"folderPath\" : \"" + notification.folderPath + "\" ," +
                "\"eventType\" : \"" + notification.eventType + "\"" +
                "}"
    }
}

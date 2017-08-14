package com.ithit.webdav.samples.deltavservlet.websocket;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

/**
 * Encodes notification object to the JSON
 */
public class NotificationEncoder implements Encoder.Text<WebSocketServer.Notification> {

    @Override
    public void init(EndpointConfig config) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public String encode(WebSocketServer.Notification notification) throws EncodeException {
        return "{" +
                "\"folderPath\" : \"" + notification.getFolderPath() + "\" ," +
                "\"eventType\" : \"" + notification.getEventType() + "\"" +
                "}";
    }
}

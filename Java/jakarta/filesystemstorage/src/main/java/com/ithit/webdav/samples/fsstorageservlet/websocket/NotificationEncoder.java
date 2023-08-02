package com.ithit.webdav.samples.fsstorageservlet.websocket;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

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
        String target = "";
        if (notification instanceof WebSocketServer.MovedNotification) {
            target = "\"TargetPath\" : \"" + ((WebSocketServer.MovedNotification) notification).getTargetPath() + "\" ,";
        }
        return "{" + target +
                "\"ItemPath\" : \"" + notification.getItemPath() + "\" ," +
                "\"EventType\" : \"" + notification.getOperation() + "\"" +
                "}";
    }
}

package com.ithit.webdav.samples.deltavservlet.websocket;

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
    public String encode(WebSocketServer.Notification notification) {
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

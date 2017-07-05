package com.ithit.webdav.samples.fsstorageservlet;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

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

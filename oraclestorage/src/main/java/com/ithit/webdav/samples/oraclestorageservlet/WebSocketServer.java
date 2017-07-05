package com.ithit.webdav.samples.oraclestorageservlet;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.HashSet;
import java.util.Set;

@ServerEndpoint(value = "/",
        configurator = GetHttpSessionConfigurator.class, encoders = {NotificationEncoder.class})
public class WebSocketServer {

    private static final Set<Session> sessions = new HashSet<>();
    private HttpSession httpSession;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        sessions.add(session);
        httpSession = (HttpSession) config.getUserProperties()
                .get(HttpSession.class.getName());
        ((WebDavEngine)httpSession.getAttribute("engine")).setWebSocketServer(this);
    }

    @OnMessage
    public String onMessage(String message, Session session) {
        return message;
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        sessions.remove(session);
        ((WebDavEngine)httpSession.getAttribute("engine")).setWebSocketServer(this);
    }

    void send(String type, String folder) {
        for (Session s: sessions) {
            if (s.isOpen()) {
                s.getAsyncRemote().sendObject(new Notification(folder, type));
            }
        }
    }

    class Notification {
        private String folderPath;
        private String eventType;

        Notification(String folderPath, String eventType) {
            this.folderPath = folderPath;
            this.eventType = eventType;
        }

        public String getFolderPath() {
            return folderPath;
        }

        public void setFolderPath(String folderPath) {
            this.folderPath = folderPath;
        }

        public String getEventType() {
            return eventType;
        }

        public void setEventType(String eventType) {
            this.eventType = eventType;
        }
    }
}

package com.ithit.webdav.samples.deltavservlet.websocket;

import com.ithit.webdav.samples.deltavservlet.WebDavEngine;
import com.ithit.webdav.server.util.StringUtil;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.HashSet;
import java.util.Set;

/**
 * WebSocket server, creates web socket endpoint, handles client's sessions
 */
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
        ((WebDavEngine) httpSession.getAttribute("engine")).setWebSocketServer(this);
    }

    @OnMessage
    public String onMessage(String message) {
        return message;
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        ((WebDavEngine) httpSession.getAttribute("engine")).setWebSocketServer(this);
    }

    /**
     * Send notification to the client
     *
     * @param type   of the notification
     * @param folder to notify
     */
    private void send(String type, String folder) {
        for (Session s : sessions) {
            if (s.isOpen()) {
                s.getAsyncRemote().sendObject(new Notification(folder, type));
            }
        }
    }

    /**
     * Sends refresh notification to the web socket client
     *
     * @param folder to refresh
     */
    public void notifyRefresh(String folder) {
        folder = StringUtil.trimEnd(StringUtil.trimStart(folder, "/"), "/");
        send("refresh", folder);
    }

    /**
     * Sends delete notification to the web socket client
     *
     * @param folder to delete
     */
    public void notifyDelete(String folder) {
        folder = StringUtil.trimEnd(StringUtil.trimStart(folder, "/"), "/");
        send("delete", folder);
    }

    /**
     * Represents VO to exchange between client and server
     */
    class Notification {
        private String folderPath;
        private String eventType;

        Notification(String folderPath, String eventType) {
            this.folderPath = folderPath;
            this.eventType = eventType;
        }

        String getFolderPath() {
            return folderPath;
        }

        String getEventType() {
            return eventType;
        }
    }
}

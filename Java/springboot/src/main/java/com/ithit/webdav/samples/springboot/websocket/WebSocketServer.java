package com.ithit.webdav.samples.springboot.websocket;

import com.ithit.webdav.server.util.StringUtil;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;

/**
 * WebSocket server, creates web socket endpoint, handles client's sessions
 */
public class WebSocketServer {

    private List<WebSocketSession> sessions;

    public WebSocketServer(List<WebSocketSession> sessions) {
        this.sessions = sessions;
    }

    /**
     * Send notification to the client
     *
     * @param type   of the notification
     * @param folder to notify
     */
    private void send(String type, String folder) {
        for (WebSocketSession session: sessions) {
            try {
                session.sendMessage(new TextMessage(new Notification(folder, type).toString()));
            } catch (IOException e) {
                e.printStackTrace();
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
    static class Notification {
        private String folderPath;
        private String eventType;

        Notification(String folderPath, String eventType) {
            this.folderPath = folderPath;
            this.eventType = eventType;
        }

        @Override
        public String toString() {
            return "{" +
                    "\"folderPath\" : \"" + folderPath + "\" ," +
                    "\"eventType\" : \"" + eventType + "\"" +
                    "}";
        }
    }
}

package com.ithit.webdav.samples.oraclestorageservlet.websocket;

import com.ithit.webdav.samples.oraclestorageservlet.WebDavEngine;
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

    private static final Set<Session> SESSIONS = new HashSet<>();
    private HttpSession httpSession;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        SESSIONS.add(session);
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
        SESSIONS.remove(session);
        ((WebDavEngine) httpSession.getAttribute("engine")).setWebSocketServer(this);
    }

    /**
     * Send notification to the client
     *
     * @param itemPath   File/Folder path.
     * @param operation  Operation name: created/updated/deleted/moved
     */
    private void send(String itemPath, String operation) {
        itemPath = StringUtil.trimEnd(StringUtil.trimStart(itemPath, "/"), "/");
        final Notification notification = new Notification(itemPath, operation);
        for (Session s : SESSIONS) {
            if (s.isOpen()) {
                s.getAsyncRemote().sendObject(notification);
            }
        }
    }

    /**
     * Notifies client that file/folder was created.
     *
     * @param itemPath file/folder.
     */
    public void notifyCreated(String itemPath) {
        send(itemPath, "created");
    }

    /**
     * Notifies client that file/folder was updated.
     *
     * @param itemPath file/folder.
     */
    public void notifyUpdated(String itemPath) {
        send(itemPath, "updated");
    }

    /**
     * Notifies client that file/folder was deleted.
     *
     * @param itemPath file/folder.
     */
    public void notifyDeleted(String itemPath) {
        send(itemPath, "deleted");
    }

    /**
     * Notifies client that file/folder was locked.
     *
     * @param itemPath file/folder.
     */
    public void notifyLocked(String itemPath) {
        send(itemPath, "locked");
    }

    /**
     * Notifies client that file/folder was unlocked.
     *
     * @param itemPath file/folder.
     */
    public void notifyUnlocked(String itemPath) {
        send(itemPath, "unlocked");
    }

    /**
     * Notifies client that file/folder was moved.
     *
     * @param itemPath file/folder.
     */
    public void notifyMoved(String itemPath, String targetPath) {
        itemPath = StringUtil.trimEnd(StringUtil.trimStart(itemPath, "/"), "/");
        targetPath = StringUtil.trimEnd(StringUtil.trimStart(targetPath, "/"), "/");
        final MovedNotification movedNotification = new MovedNotification(itemPath, "moved", targetPath);
        for (Session s : SESSIONS) {
            if (s.isOpen()) {
                s.getAsyncRemote().sendObject(movedNotification);
            }
        }
    }

    /**
     * Represents VO to exchange between client and server
     */
    static class Notification {
        private final String itemPath;
        private final String operation;

        Notification(String itemPath, String operation) {
            this.itemPath = itemPath;
            this.operation = operation;
        }

        String getItemPath() {
            return itemPath;
        }

        String getOperation() {
            return operation;
        }
    }

    /**
     * Represents VO to exchange between client and server for move type
     */
    static class MovedNotification extends Notification {
        private final String targetPath;

        MovedNotification(String itemPath, String operation, String targetPath) {
            super(itemPath, operation);
            this.targetPath = targetPath;
        }

        String getTargetPath() {
            return targetPath;
        }

    }
}

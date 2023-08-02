package com.ithit.webdav.samples.fsstorageservlet.websocket;

import com.ithit.webdav.server.util.StringUtil;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WebSocket server, creates web socket endpoint, handles client's sessions
 */
@ServerEndpoint(value = "/",
        encoders = {NotificationEncoder.class}, configurator = GetHttpSessionConfigurator.class)
public class WebSocketServer {

    public static final String INSTANCE_HEADER_NAME = "InstanceId";
    private static final Map<String, WebSocketClient> SESSIONS = new HashMap<>();
    private static WebSocketServer instance;

    public static WebSocketServer getInstance() {
        return instance;
    }

    public static void setInstance(WebSocketServer instance) {
        WebSocketServer.instance = instance;
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        SESSIONS.put(session.getId(), new WebSocketClient((String) config.getUserProperties().get(INSTANCE_HEADER_NAME), session));
        setInstance(this);
    }

    @OnMessage
    public String onMessage(String message) {
        return message;
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.remove(session.getId());
        setInstance(this);
    }

    /**
     * Send notification to the client
     *
     * @param itemPath   File/Folder path.
     * @param operation  Operation name: created/updated/deleted/moved
     * @param clientId Current clientId.
     */
    private void send(String itemPath, String operation, String clientId) {
        itemPath = StringUtil.trimEnd(StringUtil.trimStart(itemPath, "/"), "/");
        final Notification notification = new Notification(itemPath, operation);
        for (WebSocketClient s :
                StringUtil.isNullOrEmpty(clientId)
                        ? SESSIONS.values()
                        : SESSIONS.values().stream().filter(webSocketClient -> !webSocketClient.instanceId.equals(clientId)).collect(Collectors.toSet())) {
            if (s.session.isOpen()) {
                s.session.getAsyncRemote().sendObject(notification);
            }
        }
    }

    /**
     * Notifies client that file/folder was created.
     *
     * @param itemPath file/folder.
     * @param clientId Current clientId.
     */
    public void notifyCreated(String itemPath, String clientId) {
        send(itemPath, "created", clientId);
    }

    /**
     * Notifies client that file/folder was updated.
     *
     * @param itemPath file/folder.
     * @param clientId Current clientId.
     */
    public void notifyUpdated(String itemPath, String clientId) {
        send(itemPath, "updated", clientId);
    }

    /**
     * Notifies client that file/folder was deleted.
     *
     * @param itemPath file/folder.
     * @param clientId Current clientId.
     */
    public void notifyDeleted(String itemPath, String clientId) {
        send(itemPath, "deleted", clientId);
    }

    /**
     * Notifies client that file/folder was locked.
     *
     * @param itemPath file/folder.
     * @param clientId Current clientId.
     */
    public void notifyLocked(String itemPath, String clientId) {
        send(itemPath, "locked", clientId);
    }

    /**
     * Notifies client that file/folder was unlocked.
     *
     * @param itemPath file/folder.
     * @param clientId Current clientId.
     */
    public void notifyUnlocked(String itemPath, String clientId) {
        send(itemPath, "unlocked", clientId);
    }

    /**
     * Notifies client that file/folder was moved.
     *
     * @param itemPath file/folder.
     * @param clientId Current clientId.
     */
    public void notifyMoved(String itemPath, String targetPath, String clientId) {
        itemPath = StringUtil.trimEnd(StringUtil.trimStart(itemPath, "/"), "/");
        targetPath = StringUtil.trimEnd(StringUtil.trimStart(targetPath, "/"), "/");
        final MovedNotification movedNotification = new MovedNotification(itemPath, "moved", targetPath);
        for (WebSocketClient s :
                clientId != null ?
                        SESSIONS.values().stream().filter(webSocketClient -> !webSocketClient.instanceId.equals(clientId)).collect(Collectors.toSet()) :
                        SESSIONS.values()) {
            if (s.session.isOpen()) {
                s.session.getAsyncRemote().sendObject(movedNotification);
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

    private static class WebSocketClient {
        private final String instanceId;
        private final Session session;

        public WebSocketClient(String instanceId, Session session) {
            this.instanceId = instanceId;
            this.session = session;
        }
    }
}

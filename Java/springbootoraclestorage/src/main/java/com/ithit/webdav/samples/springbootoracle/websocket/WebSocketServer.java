package com.ithit.webdav.samples.springbootoracle.websocket;

import com.ithit.webdav.server.util.StringUtil;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WebSocket server, creates web socket endpoint, handles client's sessions
 */
public class WebSocketServer {

    public static final String INSTANCE_HEADER_NAME = "InstanceId";
    private final List<WebSocketSession> sessions;

    public WebSocketServer(List<WebSocketSession> sessions) {
        this.sessions = sessions;
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
        final TextMessage textMessage = new TextMessage(new Notification(itemPath, operation).toString());
        send(clientId, textMessage);
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
     * @param targetPath file/folder.
     * @param clientId Current clientId.
     */
    public void notifyMoved(String itemPath, String targetPath, String clientId) {
        itemPath = StringUtil.trimEnd(StringUtil.trimStart(itemPath, "/"), "/");
        targetPath = StringUtil.trimEnd(StringUtil.trimStart(targetPath, "/"), "/");
        final TextMessage textMessage = new TextMessage(new MovedNotification(itemPath, "moved", targetPath).toString());
        send(clientId, textMessage);
    }

    /**
     * Send TextMessage to all sessions but initiator
     * @param clientId      Id of the initiator
     * @param textMessage   Message
     */
    private void send(String clientId, TextMessage textMessage) {
        for (WebSocketSession session: StringUtil.isNullOrEmpty(clientId)
                ? sessions
                : sessions.stream().filter(x -> !x.getAttributes().get(INSTANCE_HEADER_NAME).equals(clientId)).collect(Collectors.toSet())) {
            try {
                session.sendMessage(textMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Represents VO to exchange between client and server
     */
    static class Notification {
        protected final String itemPath;
        protected final String operation;

        Notification(String itemPath, String operation) {
            this.itemPath = itemPath;
            this.operation = operation;
        }

        @Override
        public String toString() {
            return "{" +
                    "\"ItemPath\" : \"" + itemPath + "\" ," +
                    "\"EventType\" : \"" + operation + "\"" +
                    "}";
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

        @Override
        public String toString() {
            return "{" +
                    "\"ItemPath\" : \"" + itemPath + "\" ," +
                    "\"TargetPath\" : \"" + targetPath + "\" ," +
                    "\"EventType\" : \"" + operation + "\"" +
                    "}";
        }

    }
}
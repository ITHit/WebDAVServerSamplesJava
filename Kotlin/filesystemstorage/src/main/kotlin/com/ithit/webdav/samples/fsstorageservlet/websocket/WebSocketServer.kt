package com.ithit.webdav.samples.fsstorageservlet.websocket

import com.ithit.webdav.server.util.StringUtil
import javax.websocket.*
import javax.websocket.server.ServerEndpoint

const val INSTANCE_HEADER_NAME = "InstanceId"

/**
 * WebSocket server, creates web socket endpoint, handles client's sessions
 */
@ServerEndpoint(value = "/", encoders = [NotificationEncoder::class], configurator = GetHttpSessionConfigurator::class)
class WebSocketServer {

    @OnOpen
    fun onOpen(session: Session, config: EndpointConfig) {
        sessions[session.id] = WebSocketClient(config.userProperties[INSTANCE_HEADER_NAME].toString(), session)
        setInstance(this)
    }

    @OnMessage
    fun onMessage(message: String): String {
        return message
    }

    @OnClose
    fun onClose(session: Session) {
        sessions.remove(session.id)
        setInstance(this)
    }

    /**
     * Send notification to the client
     *
     * @param itemPath   File/Folder path.
     * @param operation  Operation name: created/updated/deleted/moved
     * @param clientId Current clientId.
     */
    private fun send(itemPath: String, operation: String, clientId: String?) {
        var itemPath: String? = itemPath
        itemPath = StringUtil.trimEnd(StringUtil.trimStart(itemPath, "/"), "/")
        val notification = Notification(itemPath, operation)
        for (s in if (StringUtil.isNullOrEmpty(clientId))
                sessions.values else
                sessions.values.filter { x -> !x.instanceId.equals(clientId, ignoreCase = true) }) {
            if (s.session.isOpen) {
                s.session.asyncRemote.sendObject(notification)
            }
        }
    }

    /**
     * Notifies client that file/folder was created.
     *
     * @param itemPath file/folder.
     * @param clientId Current clientId.
     */
    fun notifyCreated(itemPath: String, clientId: String?) {
        send(itemPath, "created", clientId)
    }

    /**
     * Notifies client that file/folder was updated.
     *
     * @param itemPath file/folder.
     * @param clientId Current clientId.
     */
    fun notifyUpdated(itemPath: String, clientId: String?) {
        send(itemPath, "updated", clientId)
    }

    /**
     * Notifies client that file/folder was deleted.
     *
     * @param itemPath file/folder.
     * @param clientId Current clientId.
     */
    fun notifyDeleted(itemPath: String, clientId: String?) {
        send(itemPath, "deleted", clientId)
    }

    /**
     * Notifies client that file/folder was locked.
     *
     * @param itemPath file/folder.
     * @param clientId Current clientId.
     */
    fun notifyLocked(itemPath: String, clientId: String?) {
        send(itemPath, "locked", clientId)
    }

    /**
     * Notifies client that file/folder was unlocked.
     *
     * @param itemPath file/folder.
     * @param clientId Current clientId.
     */
    fun notifyUnlocked(itemPath: String, clientId: String?) {
        send(itemPath, "unlocked", clientId)
    }

    /**
     * Notifies client that file/folder was moved.
     *
     * @param itemPath file/folder.
     * @param targetPath file/folder.
     * @param clientId Current clientId.
     */
    fun notifyMoved(itemPath: String?, targetPath: String?, clientId: String?) {
        var itemPath = itemPath
        var targetPath = targetPath
        itemPath = StringUtil.trimEnd(StringUtil.trimStart(itemPath, "/"), "/")
        targetPath = StringUtil.trimEnd(StringUtil.trimStart(targetPath, "/"), "/")
        val movedNotification = MovedNotification(itemPath, "moved", targetPath)
        for (s in if (StringUtil.isNullOrEmpty(clientId))
            sessions.values else
            sessions.values.filter { x -> !x.instanceId.equals(clientId, ignoreCase = true) }) {
            if (s.session.isOpen) {
                s.session.asyncRemote.sendObject(movedNotification)
            }
        }
    }

    /**
     * Represents VO to exchange between client and server
     */
    open inner class Notification(val itemPath: String?, val operation: String)

    /**
     * Represents VO to exchange between client and server for move type
     */
    inner class MovedNotification(itemPath: String?, operation: String, val targetPath: String?) :
        Notification(itemPath, operation)

    inner class WebSocketClient(val instanceId: String, val session: Session)

    companion object {
        private var sessions: MutableMap<String, WebSocketClient> = HashMap()
        private var instance: WebSocketServer? = null

        fun getInstance(): WebSocketServer? {
            return instance
        }

        fun setInstance(instance: WebSocketServer) {
            this.instance = instance
        }
    }
}

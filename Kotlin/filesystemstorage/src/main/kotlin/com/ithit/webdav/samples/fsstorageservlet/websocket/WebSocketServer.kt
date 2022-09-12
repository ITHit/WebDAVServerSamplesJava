package com.ithit.webdav.samples.fsstorageservlet.websocket

import com.ithit.webdav.samples.fsstorageservlet.WebDavEngine
import com.ithit.webdav.server.util.StringUtil
import java.util.*
import javax.servlet.http.HttpSession
import javax.websocket.*
import javax.websocket.server.ServerEndpoint

/**
 * WebSocket server, creates web socket endpoint, handles client's sessions
 */
@ServerEndpoint(value = "/", configurator = GetHttpSessionConfigurator::class, encoders = [NotificationEncoder::class])
class WebSocketServer {
    private var httpSession: HttpSession? = null

    @OnOpen
    fun onOpen(session: Session, config: EndpointConfig) {
        sessions.add(session)
        httpSession = config.userProperties[HttpSession::class.java.name] as HttpSession
        (httpSession!!.getAttribute("engine") as WebDavEngine).webSocketServer = this
    }

    @OnMessage
    fun onMessage(message: String): String {
        return message
    }

    @OnClose
    fun onClose(session: Session) {
        sessions.remove(session)
        (httpSession!!.getAttribute("engine") as WebDavEngine).webSocketServer = this
    }

    /**
     * Send notification to the client
     *
     * @param itemPath   File/Folder path.
     * @param operation  Operation name: created/updated/deleted/moved
     */
    private fun send(itemPath: String, operation: String) {
        var itemPath: String? = itemPath
        itemPath = StringUtil.trimEnd(StringUtil.trimStart(itemPath, "/"), "/")
        var notification = Notification(itemPath, operation)
        for (s in sessions) {
            if (s.isOpen) {
                s.asyncRemote.sendObject(notification)
            }
        }
    }

    /**
     * Notifies client that file/folder was created.
     *
     * @param itemPath file/folder.
     */
    fun notifyCreated(itemPath: String) {
        send(itemPath, "created")
    }

    /**
     * Notifies client that file/folder was updated.
     *
     * @param itemPath file/folder.
     */
    fun notifyUpdated(itemPath: String) {
        send(itemPath, "updated")
    }

    /**
     * Notifies client that file/folder was deleted.
     *
     * @param itemPath file/folder.
     */
    fun notifyDeleted(itemPath: String) {
        send(itemPath, "deleted")
    }

    /**
     * Notifies client that file/folder was locked.
     *
     * @param itemPath file/folder.
     */
    fun notifyLocked(itemPath: String) {
        send(itemPath, "locked")
    }

    /**
     * Notifies client that file/folder was unlocked.
     *
     * @param itemPath file/folder.
     */
    fun notifyUnlocked(itemPath: String) {
        send(itemPath, "unlocked")
    }

    /**
     * Notifies client that file/folder was moved.
     *
     * @param itemPath file/folder.
     */
    fun notifyMoved(itemPath: String?, targetPath: String?) {
        var itemPath = itemPath
        var targetPath = targetPath
        itemPath = StringUtil.trimEnd(StringUtil.trimStart(itemPath, "/"), "/")
        targetPath = StringUtil.trimEnd(StringUtil.trimStart(targetPath, "/"), "/")
        var movedNotification = MovedNotification(itemPath, "moved", targetPath)
        for (s in sessions) {
            if (s.isOpen) {
                s.asyncRemote.sendObject(movedNotification)
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

    companion object {
        private val sessions: MutableSet<Session> = HashSet()
    }
}

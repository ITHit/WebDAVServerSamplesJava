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
     * @param type   of the notification
     * @param folder to notify
     */
    private fun send(type: String, folder: String) {
        for (s in sessions) {
            if (s.isOpen) {
                s.asyncRemote.sendObject(Notification(folder, type))
            }
        }
    }

    /**
     * Sends refresh notification to the web socket client
     *
     * @param folder to refresh
     */
    fun notifyRefresh(folder: String) {
        var localFolder = folder
        localFolder = StringUtil.trimEnd(StringUtil.trimStart(localFolder, "/"), "/")
        send("refresh", localFolder)
    }

    /**
     * Sends delete notification to the web socket client
     *
     * @param folder to delete
     */
    fun notifyDelete(folder: String) {
        var localFolder = folder
        localFolder = StringUtil.trimEnd(StringUtil.trimStart(localFolder, "/"), "/")
        send("delete", localFolder)
    }

    /**
     * Represents VO to exchange between client and server
     */
    inner class Notification(val folderPath: String, val eventType: String)

    companion object {

        private val sessions = HashSet<Session>()
    }
}

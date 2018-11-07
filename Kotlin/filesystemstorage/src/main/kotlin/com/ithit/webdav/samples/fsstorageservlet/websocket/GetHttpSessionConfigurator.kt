package com.ithit.webdav.samples.fsstorageservlet.websocket

import javax.servlet.http.HttpSession
import javax.websocket.HandshakeResponse
import javax.websocket.server.HandshakeRequest
import javax.websocket.server.ServerEndpointConfig

/**
 * Configurer, that adds http session to the socket server
 */
class GetHttpSessionConfigurator : ServerEndpointConfig.Configurator() {
    override fun modifyHandshake(config: ServerEndpointConfig?,
                                 request: HandshakeRequest?,
                                 response: HandshakeResponse?) {
        val httpSession = request!!.httpSession as HttpSession
        config!!.userProperties[HttpSession::class.java.name] = httpSession
    }
}

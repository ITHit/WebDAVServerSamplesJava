package com.ithit.webdav.samples.fsstorageservlet.websocket

import javax.websocket.HandshakeResponse
import javax.websocket.server.HandshakeRequest
import javax.websocket.server.ServerEndpointConfig

class GetHttpSessionConfigurator : ServerEndpointConfig.Configurator() {
    override fun modifyHandshake(
        config: ServerEndpointConfig,
        request: HandshakeRequest,
        response: HandshakeResponse
    ) {
        config.userProperties[INSTANCE_HEADER_NAME] = request.headers
            .entries
            .stream()
            .filter { x -> x.key.equals(INSTANCE_HEADER_NAME, ignoreCase = true) }
            .findFirst().map { x ->
                if (x.value.isNotEmpty()) {
                    return@map x.value[0]
                }
                ""
            }
            .orElse("")
    }
}
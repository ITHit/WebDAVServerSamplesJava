package com.ithit.webdav.samples.springbootfs.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

import static com.ithit.webdav.samples.springbootfs.websocket.WebSocketServer.INSTANCE_HEADER_NAME;

public class HandshakeHeadersInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Map<String, Object> map) throws Exception {
        map.put(INSTANCE_HEADER_NAME, serverHttpRequest.getHeaders()
                .entrySet()
                .stream()
                .filter(x -> x.getKey().equalsIgnoreCase(INSTANCE_HEADER_NAME))
                .findFirst().map(x -> {
                    if (!x.getValue().isEmpty()) {
                        return x.getValue().get(0);
                    }
                    return "";
                })
                .orElse(""));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Exception e) {

    }
}

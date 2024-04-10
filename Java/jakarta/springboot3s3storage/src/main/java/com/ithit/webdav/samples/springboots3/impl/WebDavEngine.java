package com.ithit.webdav.samples.springboots3.impl;

import com.ithit.webdav.integration.spring.SpringBootLogger;
import com.ithit.webdav.integration.spring.websocket.WebSocketServer;
import com.ithit.webdav.samples.springboots3.s3.DataClient;
import com.ithit.webdav.server.Engine;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.exceptions.ServerException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * Implementation if {@link Engine}.
 * Resolves hierarchy items by paths.
 */
@Slf4j
public class WebDavEngine extends Engine {

    private final Logger logger;
    private final String license;
    @Getter
    private final boolean showExceptions;
    @Getter
    private final DataClient dataClient;
    @Setter
    private WebSocketServer webSocketServer;

    /**
     * Initializes a new instance of the WebDavEngine class.
     * @param license License string.
     * @param showExceptions True if you want to print exceptions in the response.
     * @param dataClient S3 dataClient
     */
    public WebDavEngine(String license, boolean showExceptions, DataClient dataClient) {
        this.showExceptions = showExceptions;
        this.dataClient = dataClient;
        this.logger = new SpringBootLogger(log);
        this.license = license;
    }

    /**
     * Creates {@link HierarchyItem} instance by path.
     *
     * @param contextPath Item relative path including query string.
     * @return Instance of corresponding {@link HierarchyItem} or null if item is not found.
     * @throws ServerException in case if cannot read file attributes.
     */
    @Override
    public HierarchyItem getHierarchyItem(String contextPath) throws ServerException {
        int i = contextPath.indexOf('?');
        if (i >= 0) {
            contextPath = contextPath.substring(0, i);
        }

        HierarchyItem item = dataClient.locateObject(HierarchyItemImpl.decode(contextPath), this);
        if (item != null) {
            return item;
        }
        getLogger().logDebug("Could not find item that corresponds to path: " + contextPath);
        return null; // no hierarchy item that corresponds to path parameter was found in the repository
    }

    /**
     * Returns logger that will be used by engine.
     *
     * @return Instance of {@link Logger}.
     */
    @Override
    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns license string.
     *
     * @return license string.
     */
    @Override
    public String getLicense() {
        return license;
    }

    /**
     * Returns web socket server instance
     *
     * @return web socket server instance
     */
    WebSocketServer getWebSocketServer() {
        return webSocketServer;
    }
}

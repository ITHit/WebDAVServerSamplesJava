package com.ithit.webdav.samples.springboots3.impl;

import com.ithit.webdav.samples.springboots3.s3.DataClient;
import com.ithit.webdav.samples.springboots3.websocket.WebSocketServer;
import com.ithit.webdav.server.Engine;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.exceptions.ServerException;
import lombok.extern.slf4j.Slf4j;


/**
 * Implementation if {@link Engine}.
 * Resolves hierarchy items by paths.
 */
@Slf4j
public class WebDavEngine extends Engine {

    private final Logger logger;
    private final String license;
    private final boolean showExceptions;
    private final DataClient dataClient;
    private WebSocketServer webSocketServer;

    /**
     * Initializes a new instance of the WebDavEngine class.
     * @param license License string.
     * @param showExceptions True if you want to print exceptions in the response.
     * @param dataClient
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
        int i = contextPath.indexOf("?");
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

    public DataClient getDataClient() {
        return dataClient;
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
     * Returns flag if exception should be printed to response.
     * @return true  if exception should be printed to response.
     */
    public boolean isShowExceptions() {
        return showExceptions;
    }

    /**
     * Sets web socket server instance
     *
     * @param webSocketServer web socket server instance
     */
    public void setWebSocketServer(WebSocketServer webSocketServer) {
        this.webSocketServer = webSocketServer;
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

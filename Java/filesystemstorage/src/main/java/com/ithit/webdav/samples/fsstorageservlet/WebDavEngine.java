package com.ithit.webdav.samples.fsstorageservlet;

import com.ithit.webdav.samples.fsstorageservlet.websocket.WebSocketServer;
import com.ithit.webdav.server.Engine;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.exceptions.ServerException;

import java.util.Set;

/**
 * Implementation if {@link Engine}.
 * Resolves hierarchy items by paths.
 */
public class WebDavEngine extends Engine {

    private WebSocketServer webSocketServer;
    private final WebSocketServer _FAKED_WEB_SOCKET = new WebSocketServer();
    private final Logger logger;
    private final String license;
    private final Set<String> maskRequestHeaders;
    private SearchFacade searchFacade;

    /**
     * Initializes a new instance of the WebDavEngine class.
     *
     * @param logger  Where to log messages.
     * @param license License string.
     */
    WebDavEngine(Logger logger, String license, Set<String> maskRequestHeaders) {
        this.logger = logger;
        this.license = license;
        this.maskRequestHeaders = maskRequestHeaders;
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
        HierarchyItemImpl item;
        item = FolderImpl.getFolder(contextPath, this);
        if (item != null) {
            return item;
        }
        item = FileImpl.getFile(contextPath, this);
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

    @Override
    public Set<String> getMaskRequestHeaders() {
        return maskRequestHeaders;
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
        return webSocketServer == null ? _FAKED_WEB_SOCKET : webSocketServer;
    }

    /**
     * Returns SearchFacade instance
     *
     * @return SearchFacade instance
     */
    SearchFacade getSearchFacade() {
        return searchFacade;
    }

    /**
     * Sets SearchFacade instance
     *
     * @param searchFacade SearchFacade instance
     */
    void setSearchFacade(SearchFacade searchFacade) {
        this.searchFacade = searchFacade;
    }
}

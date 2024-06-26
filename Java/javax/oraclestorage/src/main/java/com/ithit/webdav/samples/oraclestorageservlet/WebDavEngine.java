package com.ithit.webdav.samples.oraclestorageservlet;

import com.ithit.webdav.integration.servlet.websocket.DavWebSocketEndpoint;
import com.ithit.webdav.server.Engine;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.exceptions.ServerException;

/**
 * Implementation if {@link Engine}.
 * Resolves hierarchy items by paths.
 */
public class WebDavEngine extends Engine {

    private final Logger logger;
    private final String license;
    private DataAccess dataAccess;
    private SearchFacade searchFacade;

    /**
     * Initializes a new instance of the WebDavEngine class.
     *
     * @param logger  Where to log messages.
     * @param license License string.
     */
    WebDavEngine(Logger logger, String license) {
        this.logger = logger;
        this.license = license;
    }

    /**
     * Creates {@link HierarchyItem} instance by path.
     *
     * @param contextPath Item relative path including query string.
     * @return Instance of corresponding {@link HierarchyItem} or null if item is not found.
     * @throws ServerException in case if engine cannot read file attributes.
     */
    @Override
    public HierarchyItem getHierarchyItem(String contextPath) throws ServerException {
        int i = contextPath.indexOf("?");
        if (i >= 0) {
            contextPath = contextPath.substring(0, i);
        }
        return dataAccess.getHierarchyItem(contextPath);
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
     * Returns {@link DataAccess} helper for DB operations.
     *
     * @return DataAccess.
     */
    DataAccess getDataAccess() {
        return dataAccess;
    }

    /**
     * Sets the {@link DataAccess}.
     *
     * @param dataAccess DataAccess to set.
     */
    void setDataAccess(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    /**
     * Returns web socket server instance
     *
     * @return web socket server instance
     */
    DavWebSocketEndpoint getWebSocketServer() {
        return DavWebSocketEndpoint.getInstance();
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

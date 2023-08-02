package com.ithit.webdav.samples.springbootfs.impl;

import com.ithit.webdav.samples.springbootfs.websocket.WebSocketServer;
import com.ithit.webdav.server.Engine;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.exceptions.ServerException;
import com.ithit.webdav.server.util.StringUtil;
import lombok.extern.slf4j.Slf4j;


/**
 * Implementation if {@link Engine}.
 * Resolves hierarchy items by paths.
 */
@Slf4j
public class WebDavEngine extends Engine {

    private final Logger logger;
    private final String license;
    private final String dataFolder;
    private final boolean showExceptions;
    private final String rootContext;
    private SearchFacade searchFacade;
    private WebSocketServer webSocketServer;

    /**
     * Initializes a new instance of the WebDavEngine class.
     *
     * @param license        License string.
     * @param dataFolder     Path to the root folder to map to DAV.
     * @param showExceptions True if you want to print exceptions in the response.
     * @param rootContext    Context path where webdav service is working.
     */
    public WebDavEngine(String license, String dataFolder, boolean showExceptions, String rootContext) {
        this.dataFolder = dataFolder;
        this.showExceptions = showExceptions;
        this.rootContext = StringUtil.trimEnd(rootContext, "/");
        this.logger = new SpringBootLogger(log);
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
        int i = contextPath.indexOf('?');
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
        return null; // no hierarchy item corresponds to path parameter was found in the repository
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
     * Returns folder where data will be sourced for WebDAV
     * @return data folder.
     */
    public String getDataFolder() {
        return dataFolder;
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
    public void setSearchFacade(SearchFacade searchFacade) {
        this.searchFacade = searchFacade;
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

    /**
     * Returns real path considering context which may vary.
     * @param path context aware path.
     * @return real path.
     */
    public String getContextAware(String path) {
        if (path != null && path.startsWith(rootContext)) {
            String contextAware = path.replaceFirst(rootContext, "/");
            return contextAware.replace("//", "/");
        }
        return path;
    }
}

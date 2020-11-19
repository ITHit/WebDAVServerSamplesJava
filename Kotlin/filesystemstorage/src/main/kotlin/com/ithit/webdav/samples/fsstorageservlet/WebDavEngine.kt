package com.ithit.webdav.samples.fsstorageservlet

import com.ithit.webdav.samples.fsstorageservlet.websocket.WebSocketServer
import com.ithit.webdav.server.Engine
import com.ithit.webdav.server.HierarchyItem
import com.ithit.webdav.server.Logger
import com.ithit.webdav.server.exceptions.ServerException

/**
 * Implementation if [Engine].
 * Resolves hierarchy items by paths.
 */
class WebDavEngine
/**
 * Initializes a new instance of the WebDavEngine class.
 *
 * @param logger  Where to log messages.
 * @param license License string.
 */
internal constructor(private val logger: Logger?, private val license: String?) : Engine() {

    internal var webSocketServer: WebSocketServer? = null
        get() = if (field == null) _FAKED_WEB_SOCKET else field
    private val _FAKED_WEB_SOCKET = WebSocketServer()
    /**
     * Returns SearchFacade instance
     *
     * @return SearchFacade instance
     */
    /**
     * Sets SearchFacade instance
     *
     * @param searchFacade SearchFacade instance
     */
    internal var searchFacade: SearchFacade? = null

    /**
     * Creates [HierarchyItem] instance by path.
     *
     * @param contextPath Item relative path including query string.
     * @return Instance of corresponding [HierarchyItem] or null if item is not found.
     * @throws ServerException in case if cannot read file attributes.
     */
    // <<<< getHierarchyItemEngine
    @Throws(ServerException::class)
    override fun getHierarchyItem(contextPath: String): HierarchyItem? {
        var localContextPath = contextPath
        val i = localContextPath.indexOf("?")
        if (i >= 0) {
            localContextPath = localContextPath.substring(0, i)
        }
        var item: HierarchyItemImpl?
        item = FolderImpl.getFolder(localContextPath, this)
        if (item != null) {
            return item
        }
        item = FileImpl.getFile(localContextPath, this)
        if (item != null) {
            return item
        }
        getLogger()?.logDebug("Could not find item that corresponds to path: $localContextPath")
        return null // no hierarchy item that corresponds to path parameter was found in the repository
    }
    // getHierarchyItemEngine >>>>

    /**
     * Returns logger that will be used by engine.
     *
     * @return Instance of [Logger].
     */
    override fun getLogger(): Logger? {
        return logger
    }

    /**
     * Returns license string.
     *
     * @return license string.
     */
    override fun getLicense(): String? {
        return license
    }
}

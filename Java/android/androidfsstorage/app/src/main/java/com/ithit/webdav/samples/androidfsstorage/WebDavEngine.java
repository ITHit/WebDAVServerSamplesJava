package com.ithit.webdav.samples.androidfsstorage;

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
}

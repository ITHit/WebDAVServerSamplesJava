package com.ithit.webdav.samples.deltavservlet;

import com.ithit.webdav.integration.servlet.websocket.DavWebSocketEndpoint;
import com.ithit.webdav.server.Engine;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.deltav.AutoVersion;
import com.ithit.webdav.server.exceptions.ServerException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation if {@link Engine}.
 * Resolves hierarchy items by paths.
 */
public class WebDavEngine extends Engine {

    private final Logger logger;
    private final String license;
    private DataAccess dataAccess;
    private SearchFacade searchFacade;
    private AutoVersion autoVersionMode;
    private boolean autoputUnderVersionControl;

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
     * @param pathAndQuery Item relative path including query string.
     * @return Instance of corresponding {@link HierarchyItem} or null if item is not found.
     * @throws ServerException in case if engine cannot read file attributes.
     */
    @Override
    public HierarchyItem getHierarchyItem(String pathAndQuery) throws ServerException {
        if (pathAndQuery.contains("?version=")) // Version
        {
            String versionNum = getQueryMap(pathAndQuery).get("version");
            String itemPath = pathAndQuery.substring(0, pathAndQuery.indexOf('?'));

            HierarchyItemImpl item = dataAccess.getHierarchyItem(itemPath);
            if (item == null)
                return null;

            String commandText = "SELECT Id, ItemId, VersionNumber, Name, Created"
                    + " FROM Version"
                    + " WHERE ItemId = ?"
                    + " AND VersionNumber = ?";

            List<VersionImpl> versions = dataAccess.readVersions(commandText, itemPath, item.getId(), versionNum);
            if (!versions.isEmpty())
                return versions.get(0);
        } else if (pathAndQuery.contains("?history")) // History
        {
            String itemPath = pathAndQuery.substring(0, pathAndQuery.indexOf('?'));
            HierarchyItemImpl item = dataAccess.getHierarchyItem(itemPath);

            if (item instanceof FileImpl) {
                FileImpl file = (FileImpl) item;
                return file.getVersionHistory();
            }
        } else {
            int ind = pathAndQuery.indexOf('?');
            if (ind > 0)
                pathAndQuery = pathAndQuery.substring(0, ind);

            return dataAccess.getHierarchyItem(pathAndQuery);
        }

        return null;
    }

    /**
     * Creates map from the URL parameters.
     *
     * @param query URL with all the parameters.
     * @return Name value map with URL parameters.
     */
    private Map<String, String> getQueryMap(String query) {
        query = query.split("\\?")[1];
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
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
     * @return License string.
     */
    @Override
    public String getLicense() {
        return license;
    }

    /**
     * Indicates whether to auto put item under version control.
     *
     * @return True if yes, false otherwise.
     */
    @Override
    public boolean getAutoPutUnderVersionControl() {
        return autoputUnderVersionControl;
    }

    /**
     * Set whether to auto put item under version control.
     *
     * @param autoputUnderVersionControl True if yes, false otherwise.
     */
    void setAutoPutUnderVersionControl(boolean autoputUnderVersionControl) {
        this.autoputUnderVersionControl = autoputUnderVersionControl;
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
     * Returns {@link AutoVersion} rule for version mode.
     *
     * @return {@link AutoVersion} rule for version mode.
     */
    AutoVersion getAutoVersionMode() {
        return autoVersionMode;
    }

    /**
     * Sets {@link AutoVersion} rule for version mode.
     *
     * @param autoVersionMode {@link AutoVersion} rule for version mode.
     */
    void setAutoVersionMode(AutoVersion autoVersionMode) {
        this.autoVersionMode = autoVersionMode;
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

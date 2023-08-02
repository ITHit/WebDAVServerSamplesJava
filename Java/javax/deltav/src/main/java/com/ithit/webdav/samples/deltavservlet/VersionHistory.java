package com.ithit.webdav.samples.deltavservlet;

import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.Property;
import com.ithit.webdav.server.deltav.History;
import com.ithit.webdav.server.deltav.VersionableItem;
import com.ithit.webdav.server.exceptions.LockedException;
import com.ithit.webdav.server.exceptions.MultistatusException;
import com.ithit.webdav.server.exceptions.ServerException;
import com.ithit.webdav.server.exceptions.WebDavStatus;

import java.util.List;

/**
 * Contains all versions of a particular version-controlled item.
 * <p>
 * The important property of this interface is <i>Path</i> property inherited from HierarchyItem.
 * The url returned by this property is used by client applications to remove item from version control.
 * The client application submits DELETE WebDAV request to this url and the engine calls {@link VersionableItem#putUnderVersionControl}
 * passing <b>false</b> as a parameter. In your {@link VersionableItem#putUnderVersionControl} you will usually delete all versions.
 * </p>
 */
class VersionHistory implements History {
    private final WebDavEngine engine;
    private final int itemId;
    private final String itemPath;

    /**
     * Initializes new instance of the {@link VersionHistory}.
     *
     * @param engine   Instance of current {@link WebDavEngine}.
     * @param itemId   Of the item in DB.
     * @param itemPath Relative to WebDAV root folder path..
     */
    VersionHistory(WebDavEngine engine, int itemId, String itemPath) {
        this.engine = engine;
        this.itemId = itemId;
        this.itemPath = itemPath;
    }

    /**
     * Unique item path in the repository relative to storage root.
     * <p/>
     * <p>
     * The URL returned by this method is relative to storage root. If your server root is located at http://webdavserver.com/myserver/ and the item URL is http://webdavserver.com/myserver/myfolder/myitem.doc this property implementation must return myfolder/myitem.doc. To calculate the entire item URL the engine will call {@link javax.servlet.http.HttpServletRequest#getContextPath} property and attach it to url returned by this property.
     * </p>
     * <p>Examples:
     * <ul>
     * <li>File: myfolder/mydoc.docx</li>
     * <li>Folder: myfolder/folder/</li>
     * <li>History item: myfolder/mydoc.docx?history</li>
     * <li>Version: myfolder/mydoc.docx?version=5</li>
     * </ul>
     * </p>
     *
     * @return Item path relative to storage root.
     */
    public String getPath() {
        return itemPath + "?history";
    }

    /**
     * Returns current item version.
     *
     * @return current {@link VersionImpl}.
     * @throws ServerException in case of DB errors.
     */
    public VersionImpl getCurrentVersion() throws ServerException {
        String commandText = "SELECT Id, ItemId, VersionNumber, Created"
                + " FROM Version WHERE ItemId = ?"
                + " AND VersionNumber ="
                + " (SELECT MAX(VersionNumber)"
                + " FROM Version"
                + " WHERE ItemId = ?)";

        return engine.getDataAccess().readVersions(commandText, itemPath, itemId, itemId).get(0);
    }

    /**
     * All versions of current item.
     *
     * @return List with all versions of current item.
     * @throws ServerException in case of an error.
     */
    public List<VersionImpl> getVersionSet() throws ServerException {
        return engine.getDataAccess().readVersions("SELECT Id, ItemId, VersionNumber, Created"
                + " FROM Version WHERE ItemId = ? ORDER BY VersionNumber DESC", itemPath, itemId);
    }

    /**
     * Item's root version.
     *
     * @return Root version for the item.
     * @throws ServerException in case of an error.
     */
    public VersionImpl getRootVersion() throws ServerException {
        String commandText = "SELECT Id, ItemId, VersionNumber, Created"
                + " FROM Version WHERE ItemId = ?"
                + " AND VersionNumber ="
                + " (SELECT MIN(VersionNumber)"
                + " FROM Version"
                + " WHERE ItemId = ?)";

        return engine.getDataAccess().readVersions(commandText, itemPath, itemId, itemId).get(0);
    }

    /**
     * Gets the name of the item in repository.
     *
     * @return Name of this item.
     * @throws ServerException In case of an error.
     */
    public String getName() throws ServerException {
        throw new ServerException(WebDavStatus.NOT_IMPLEMENTED);
    }

    /**
     * Gets the creation date of the item in repository expressed as the coordinated universal time (UTC).
     *
     * @return Creation date of the item.
     * @throws ServerException In case of an error.
     */
    public long getCreated() throws ServerException {
        throw new ServerException(WebDavStatus.NOT_IMPLEMENTED);
    }

    /**
     * Gets the last modification date of the item in repository expressed as the coordinated universal time (UTC).
     *
     * @return Modification date of the item.
     * @throws ServerException In case of an error.
     */
    public long getModified() throws ServerException {
        throw new ServerException(WebDavStatus.NOT_IMPLEMENTED);
    }

    /**
     * Deletes this item.
     *
     * @throws ServerException - in case of another error.
     */
    public void delete() throws ServerException {
        throw new ServerException(WebDavStatus.NOT_ALLOWED);
    }

    /**
     * Gets values of all properties or selected properties for this item.
     *
     * @param props <ul>
     *              <li>
     *              Array of properties which values are requested.
     *              </li>
     *              <li>
     *              {@code null} to get all properties.
     *              </li>
     *              </ul>
     * @return List of properties with values set. If property cannot be found it shall be omitted from the result.
     * @throws ServerException In case of an error.
     */
    public List<Property> getProperties(Property[] props) throws ServerException {
        throw new ServerException(WebDavStatus.NOT_ALLOWED);
    }

    /**
     * Gets names of all properties for this item.
     * <p>
     * Most WebDAV clients never request list of property names, so your implementation can just throw {@link ServerException}
     * with {@link WebDavStatus#NOT_ALLOWED} status.
     * </p>
     *
     * @return List of all property names for this item.
     * @throws ServerException In case of an error.
     */
    public List<Property> getPropertyNames() throws ServerException {
        throw new ServerException(WebDavStatus.NOT_ALLOWED);
    }

    /**
     * Modifies and removes properties for this item.
     *
     * @param setProps Array of properties to be set.
     * @param delProps Array of properties to be removed. {@link Property#value} field is ignored.
     *                 Specifying the removal of a property that does not exist is not an error.
     * @throws LockedException      this item was locked and client did not provide lock token.
     * @throws MultistatusException If update fails for a property, this exception shall be thrown and contain
     *                              result of the operation for each property.
     *                              Status for each property can be one of following:
     *                              <ul>
     *                              <li>
     *                              {@link WebDavStatus#OK} - the property was successfully updated or deleted.
     *                              </li>
     *                              <li>
     *                              {@link WebDavStatus#CONFLICT} - the client has provided a value whose semantics are not appropriate for the property,
     *                              this includes trying to set read-only properties.
     *                              </li>
     *                              <li>
     *                              {@link WebDavStatus#FAILED_DEPENDENCY} - indicates this action would have succeeded if it were not for the conflict
     *                              with updating/removing some other property.
     *                              </li>
     *                              </ul>
     * @throws ServerException      In case of other error.
     */
    public void updateProperties(Property[] setProps, Property[] delProps) throws
            LockedException, MultistatusException, ServerException {
        throw new ServerException(WebDavStatus.NOT_ALLOWED);
    }

    /**
     * Creates a copy of this item with a new name in the destination folder.
     * <p>
     * If error occurred while copying items located in a subtree, the server
     * should try to continue copy operation and copy all other items. In this case
     * you must throw {@link MultistatusException} that contain separate response for
     * every item that was successfully copied or failed to copy.
     * </p>
     * <p>
     * A CopyTo method invocation must not copy any locks active on the source item.
     * However, if this method copies the item into a folder that has a deep lock,
     * then the destination item must be added to the lock.
     * </p>
     *
     * @param folder   Destination folder.
     * @param destName Name of the destination item.
     * @param deep     Indicates whether to copy entire subtree.
     * @throws ServerException - In case of other error.
     */
    public void copyTo(Folder folder, String destName, boolean deep) throws ServerException {
        throw new ServerException(WebDavStatus.NOT_ALLOWED);
    }

    /**
     * Moves this item to the destination folder under a new name.
     *
     * @param folder   Destination folder.
     * @param destName Name of the destination item.
     * @throws ServerException - in case of another error.
     */
    public void moveTo(Folder folder, String destName) throws ServerException {
        throw new ServerException(WebDavStatus.NOT_ALLOWED);
    }
}
package com.ithit.webdav.samples.oraclestorageservlet;

import com.ithit.webdav.server.*;
import com.ithit.webdav.server.exceptions.*;
import com.ithit.webdav.server.util.DavContext;
import com.ithit.webdav.server.util.StringUtil;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

/**
 * Represents file or folder in the Oracle DB repository.
 * <p>
 * Defines methods common to all WebDAV folders and files.
 * {@link #getCreated} and {@link #getModified} methods must return Universal Coordinated Time (UTC).
 * {@link #getProperties} and {@link #updateProperties} are called when WebDAV client is reading, adding,
 * updating or deleting custom properties.
 * This interface also provides methods for managing hierarchy: moving, copying and deleting WebDAV items.
 * See {@link #copyTo}, {@link #moveTo} and {@link #delete} methods.
 * Your file items must implement {@link com.ithit.webdav.server.File} interface, folder items - {@link Folder} interface.
 * </p>
 */
public abstract class HierarchyItemImpl implements HierarchyItem, Lock {

    static final String SNIPPET = "snippet";
    final int id;
    private final String path;
    private final long created;
    private final long modified;
    private final int parentId;
    private final WebDavEngine engine;
    private String name;

    /**
     * Initializes a new instance of the {@link HierarchyItemImpl} class.
     *
     * @param id       Id of the item in DB.
     * @param parentId Id of the parent item in DB.
     * @param name     Name of hierarchy item.
     * @param path     Relative to WebDAV root folder path.
     * @param created  Creation time of the hierarchy item.
     * @param modified Modification time of the hierarchy item.
     * @param engine   Instance of current {@link WebDavEngine}.
     */
    HierarchyItemImpl(int id, int parentId, String name, String path,
                      long created, long modified, WebDavEngine engine) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.path = path;
        this.created = created;
        this.modified = modified;
        this.engine = engine;
    }

    /**
     * Gets the name of the item in repository.
     *
     * @return Name of this item.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns Oracle DB engine.
     *
     * @return Oracle DB engine.
     */
    public WebDavEngine getEngine() {
        return engine;
    }

    /**
     * Returns {@link DataAccess} helper for DB operations.
     *
     * @return DataAccess.
     */
    DataAccess getDataAccess() {
        return engine.getDataAccess();
    }

    /**
     * Indicates whether we need to delete item tree before replace.
     *
     * @return True if yes, false otherwise.
     */
    boolean delWhenReplace() {
        return true;
    }

    /**
     * Returns parent folder of {@link HierarchyItemImpl}.
     *
     * @return Returns parent folder of {@link HierarchyItemImpl}.
     * @throws ServerException in case of DB errors.
     */
    FolderImpl getParent() throws ServerException {

        String parentPath = path.substring(0, path.lastIndexOf('/'));

        FolderImpl parent = (FolderImpl) getDataAccess().readItem("SELECT ID, Parent, Name, Created, Modified, ItemType, LastChunkSaved, TotalContentLength"
                + " FROM Repository"
                + " WHERE ID = ?", parentPath, false, parentId);

        if (parent == null)
            throw new ServerException(WebDavStatus.CONFLICT);

        return parent;
    }

    /**
     * Returns id associated with this {@link HierarchyItemImpl}.
     *
     * @return Returns id associated with this {@link HierarchyItemImpl}.
     */
    int getId() {
        return id;
    }

    /**
     * Gets the creation date of the item in repository expressed as the coordinated universal time (UTC).
     *
     * @return Creation date of the item.
     */
    public long getCreated() {
        return created;
    }

    /**
     * Gets the last modification date of the item in repository expressed as the coordinated universal time (UTC).
     *
     * @return Modification date of the item.
     */
    public long getModified() {
        return modified;
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
        return path;
    }

    /**
     * Check whether client is the lock owner.
     *
     * @throws LockedException in case if not owner.
     * @throws ServerException other errors.
     */
    void ensureHasToken() throws LockedException, ServerException {
        if (!clientHasToken())
            throw new LockedException();
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

        List<Property> l = getDataAccess().readProperties("SELECT Name, Namespace, PropVal"
                + " FROM Properties"
                + " WHERE ItemID = ?", getId());


        if (props == null) {
            if (l == null) {
                return Collections.emptyList();
            }
            return l;
        }

        List<Property> result = new ArrayList<>();

        for (Property lookForProp : props) {
            if (SNIPPET.equalsIgnoreCase(lookForProp.getName()) && this instanceof FileImpl) {
                result.add(Property.create(lookForProp.getNamespace(), lookForProp.getName(), ((FileImpl) this).getSnippet()));
                continue;
            }
            for (Property foundProp : l) {
                if (StringUtil.stringEquals(lookForProp.getName(), foundProp.getName()) &&
                        StringUtil.stringEquals(lookForProp.getNamespace(), foundProp.getNamespace())) {

                    result.add(foundProp);
                    break;
                }
            }
        }
        return result;
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
        return getDataAccess().readProperties("SELECT Name, Namespace, '' as PropVal"
                + " FROM Properties"
                + " WHERE ItemID = ?", getId());
    }

    /**
     * Modifies and removes properties for this item.
     *
     * @param setProps Array of properties to be set.
     * @param delProps Array of properties to be removed. {@link Property#getXmlValueRaw()} field is ignored.
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

        ensureHasToken();

        if (setProps != null) {
            for (Property p : setProps) {
                setProperty(p); // create or update property
            }
        }

        if (delProps != null)
            for (Property p : delProps) {
                removeProperty(p.getName(), p.getNamespace());
            }

        updateModified();
        getEngine().getWebSocketServer().notifyRefresh(getParent(getPath()));
    }

    /**
     * Set property on the {@link HierarchyItemImpl} and stores its value in Properties table.
     *
     * @param prop {@link Property} to set.
     * @throws ServerException in case od DB error.
     */
    private void setProperty(Property prop) throws ServerException {
        int count = getDataAccess().executeInt("SELECT Count(*) FROM Properties"
                + " WHERE ItemID = ?"
                + " AND Name = ?"
                + " AND Namespace = ?", getId(), prop.getName(), prop.getNamespace());

        if (count == 0) // insert
        {
            getDataAccess().executeUpdate("INSERT INTO Properties"
                            + " (ItemID, Name, Namespace, PropVal)"
                            + " VALUES(?, ?, ?, ?)",
                    getId(), prop.getName(), prop.getNamespace(), prop.getXmlValueRaw());
        } else // update
        {
            getDataAccess().executeUpdate("UPDATE Properties"
                    + " SET PropVal = ?"
                    + " WHERE ItemID = ?"
                    + " AND Name = ?"
                    + " AND Namespace = ?", prop.getXmlValueRaw(), getId(), prop.getName(), prop.getNamespace());
        }
    }

    /**
     * Removes property from the {@link HierarchyItemImpl} and removes it from Properties table.
     *
     * @param propertyName Property name to remove.
     * @param ns           Property name space to remove.
     * @throws ServerException in case od DB error.
     */
    private void removeProperty(String propertyName, String ns) throws ServerException {
        getDataAccess().executeUpdate("DELETE FROM Properties"
                + " WHERE ItemID = ?"
                + " AND Name = ?"
                + " AND Namespace = ?", getId(), propertyName, ns);
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
     * @throws LockedException      - the destination item was locked and client did not provide lock token.
     * @throws ConflictException    - destination folder does not exist.
     * @throws MultistatusException - errors has occured during processing of the subtree.
     *                              Every item that has been eithre successfully copied or failed to copy must be present in exception with corresponding status.
     * @throws ServerException      - In case of other error.
     */
    public abstract void copyTo(Folder folder, String destName, boolean deep)
            throws LockedException, MultistatusException, ServerException, ConflictException;

    /**
     * Moves this item to the destination folder under a new name.
     *
     * @param folder   Destination folder.
     * @param destName Name of the destination item.
     * @throws LockedException      - the source or the destination item was locked and client did not provide lock token.
     * @throws ConflictException    - destination folder does not exist.
     * @throws MultistatusException - errors has occured during processing of the subtree. Every processed item must have corresponding response added
     *                              with corresponding status.
     * @throws ServerException      - in case of another error.
     */
    public abstract void moveTo(Folder folder, String destName)
            throws LockedException, ConflictException, MultistatusException, ServerException;

    /**
     * Deletes this item.
     *
     * @throws LockedException      - this item or its parent was locked and client did not provide lock token.
     * @throws MultistatusException - errors has occurred during processing of the subtree. Every processed item must have corresponding response added
     *                              to the exception with corresponding status.
     * @throws ServerException      - in case of another error.
     */
    public abstract void delete() throws LockedException, MultistatusException, ServerException;

    /**
     * Check whether client is the lock owner.
     *
     * @return True if owner, false otherwise.
     * @throws ServerException in case of errors.
     */
    boolean clientHasToken() throws ServerException {

        List<LockInfo> itemLocks = getActiveLocks();
        if (itemLocks.size() == 0)
            return true;
        List<String> clientLockTokens = DavContext.currentRequest().getClientLockTokens();
        for (String clientLockToken : clientLockTokens)
            for (LockInfo itemLock : itemLocks)
                if (clientLockToken.equals(itemLock.getToken()))
                    return true;
        return false;
    }


    /**
     * Removes {@link HierarchyItemImpl} information from the Oracle DB.
     *
     * @throws ServerException in case of DB error.
     */
    void deleteThisItem() throws ServerException {
        getDataAccess().executeUpdate("DELETE FROM Locks WHERE ItemID = ?", id);
        getDataAccess().executeUpdate("DELETE FROM Properties WHERE ItemID = ?", id);
        getDataAccess().executeUpdate("DELETE FROM Repository WHERE ID = ?", id);
    }

    /**
     * Gets the array of all locks for this item.
     * <p>
     * This method must return all locks for the item including deep locks on any of the parent folders.
     * All fields of each {@link LockInfo} structure in the array must be set.
     * </p>
     *
     * @return Array of locks.
     * @throws ServerException In case of an error.
     */
    public List<LockInfo> getActiveLocks() throws ServerException {
        int itemId = getId();
        ArrayList<LockInfo> l = new ArrayList<>();

        l.addAll(getLocks(getId(), false)); // get all locks
        while (true) {
            Integer res = getDataAccess().executeInt("SELECT Parent FROM Repository WHERE ID = ?", itemId);
            if (res == null)
                break;
            itemId = res;
            if (itemId <= 0)
                break;
            l.addAll(getLocks(itemId, true));  // get only deep locks
        }

        return l;
    }

    /**
     * Loads locks from Oracle DB.
     *
     * @param itemId   Unique item id.
     * @param onlyDeep Determines whether to load deep locks.
     * @return List of {@link LockInfo}.
     * @throws ServerException in case of DB errors.
     */
    private List<LockInfo> getLocks(int itemId, boolean onlyDeep) throws ServerException {
        if (onlyDeep)
            return getDataAccess().readLocks("SELECT Token, Shared, Deep, Expires, Owner"
                    + " FROM Locks"
                    + " WHERE ItemID = ?"
                    + " AND Deep = ?", itemId, true);
        else
            return getDataAccess().readLocks("SELECT Token, Shared, Deep, Expires, Owner FROM Locks WHERE ItemID = ?", itemId);
    }

    /**
     * Checks if there is items on the lock.
     *
     * @param skipShared Indicates whether to skip shared locks.
     * @return True if there are locks on the item.
     * @throws ServerException in case of DB errors.
     */
    private boolean itemHasLock(boolean skipShared) throws ServerException {
        List<LockInfo> locks = getActiveLocks();
        if (locks.size() == 0)
            return false;
        return !(skipShared && locks.get(0).isShared());
    }

    /**
     * Walks the {@link HierarchyItemImpl} tree to check for locked items.
     *
     * @param root       Root of the tree to walk.
     * @param skipShared Indicates whether to skip shared {@link HierarchyItemImpl}.
     * @throws ServerException in case of errors.
     */
    private void checkNoItemsLocked(HierarchyItemImpl root, boolean skipShared)
            throws ServerException, MultistatusException {

        MultistatusException mr = new MultistatusException();
        checkNoItemsLocked(mr, root, skipShared);
        if (mr.getResponses().length > 0)
            throw mr;
    }

    /**
     * Walks the {@link HierarchyItemImpl} tree to check for locked items.
     *
     * @param mr         Aggregate {@link MultistatusException} response.
     * @param root       Root of the tree to walk.
     * @param skipShared Indicates whether to skip shared {@link HierarchyItemImpl}.
     * @throws ServerException in case of errors.
     */
    private void checkNoItemsLocked(MultistatusException mr, HierarchyItemImpl root, boolean skipShared)
            throws ServerException {

        FolderImpl folder = root instanceof FolderImpl ? (FolderImpl) root : null;
        if (folder != null)
            for (HierarchyItem child : folder.getChildren(Collections.<Property>emptyList(), null, null, null).getPage()) {
                if (((HierarchyItemImpl)child).itemHasLock(skipShared))
                    mr.addResponse(child.getPath(), WebDavStatus.LOCKED);
                checkNoItemsLocked(mr, ((HierarchyItemImpl)child), skipShared);
            }

    }

    /**
     * Locks this item.
     * <p>
     * In your {@code Lock} implementation you must generate lock token and create {@link LockResult} class instance.
     * You must also associate generated token with the hierarchy item in your repository during this call.
     * The token is sent to the WebDAV client.
     * </p>
     *
     * @param shared  Indicates whether a lock is shared or exclusive.
     * @param deep    Indicates whether a lock is enforceable on the subtree.
     * @param timeout Lock expiration time in seconds. Negative value means never.
     * @param owner   Provides information about the principal taking out a lock.
     * @return Actually applied lock (Server may modify timeout).
     * @throws LockedException      The item is locked, so the method has been rejected.
     * @throws MultistatusException Errors have occured during processing of the subtree.
     * @throws ServerException      In case of an error.
     */
    public LockResult lock(boolean shared, boolean deep, long timeout, String owner)
            throws LockedException, MultistatusException, ServerException {
        if (itemHasLock(shared))
            throw new LockedException();

        if (deep) { // check if no items are locked in this subtree
            checkNoItemsLocked(this, shared);
        }

        String token = UUID.randomUUID().toString();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.SECOND, (int) timeout);
        Timestamp expires = timeout >= 0 ? new Timestamp(calendar.getTimeInMillis()) : null;

        getDataAccess().executeUpdate("INSERT INTO Locks (ItemID,Token,Shared,Deep,Expires,Owner)"
                        + " VALUES(?, ?, ?, ?, ?, ?)",
                getId(), token, shared, deep, expires, owner);
        getEngine().getWebSocketServer().notifyRefresh(getParent(getPath()));
        return new LockResult(token, timeout);
    }

    /**
     * Updates lock timeout information on this item.
     *
     * @param token   The lock token associated with a lock.
     * @param timeout Lock expiration time in seconds. Negative value means never.
     * @return Actually applied lock (Server may modify timeout).
     * @throws PreconditionFailedException Included lock token was not enforceable on this item.
     * @throws ServerException             In case of an error.
     */
    public RefreshLockResult refreshLock(String token, long timeout) throws PreconditionFailedException, ServerException {
        List<LockInfo> locks = getActiveLocks();

        LockInfo lockInfo = null;
        for (LockInfo lock : locks)
            if (token.equals(lock.getToken()))
                lockInfo = lock;

        if (lockInfo == null)
            throw new PreconditionFailedException();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.SECOND, (int) timeout);
        Timestamp expires = timeout >= 0 ? new java.sql.Timestamp(calendar.getTime().getTime()) : null;

        getDataAccess().executeUpdate("UPDATE Locks SET Expires = ? WHERE Token = ?",
                expires, token);
        getEngine().getWebSocketServer().notifyRefresh(getParent(getPath()));
        return new RefreshLockResult(lockInfo.isShared(), lockInfo.isDeep(),
                timeout, lockInfo.getOwner());
    }

    /**
     * Removes lock with the specified token from this item.
     * <p>
     * If this lock included more than one hierarchy item, the lock is removed from all items included in the lock.
     * </p>
     *
     * @param lockToken Lock with this token should be removed from the item.
     * @throws PreconditionFailedException Included lock token was not enforceable on this item.
     * @throws ServerException             In case of an error.
     */
    public void unlock(String lockToken) throws PreconditionFailedException, ServerException {
        List<LockInfo> locks = getActiveLocks();

        boolean found = false;
        for (LockInfo lock : locks)
            if (lock.getToken().equals(lockToken)) {
                found = true;
                break;
            }

        if (!found)
            throw new PreconditionFailedException();


        getDataAccess().executeUpdate("DELETE FROM Locks WHERE Token = ?", lockToken);
        getEngine().getWebSocketServer().notifyRefresh(getParent(getPath()));
    }

    /**
     * Updates modified date of the {@link HierarchyItemImpl}.
     *
     * @throws ServerException in case of DB errors.
     */
    void updateModified() throws ServerException {
        getDataAccess().executeUpdate("UPDATE Repository SET Modified = ? WHERE ID = ?", new java.sql.Timestamp(new Date().getTime()),
                getId());
    }

    /**
     * Create copy of the {@link File} in OracleDB.
     *
     * @param id       New item id.
     * @param parentId New item parent id.
     * @param name     New item name.
     * @param path     New item path.
     * @param created  Time of creation.
     * @param modified Time of creation.
     * @param engine   {@link WebDavEngine}.
     * @return Newly created {@link HierarchyItemImpl}.
     */
    protected abstract HierarchyItemImpl createItemCopy(int id, int parentId, String name, String path,
                                                        long created, long modified, WebDavEngine engine);

    /**
     * Copies {@link HierarchyItemImpl} to the destination {@link FolderImpl} with the new name.
     *
     * @param destFolder Destination folder.
     * @param destItem   Item to copy.
     * @param destName   New file name.
     * @return Newly created {@link HierarchyItemImpl}.
     * @throws ServerException in case of DB errors.
     */
    HierarchyItemImpl copyThisItem(FolderImpl destFolder, HierarchyItemImpl destItem, String destName)
            throws ServerException {
        // returns created item, if any, otherwise null

        HierarchyItemImpl createdItem = null;

        int destId;
        if (destItem == null) // insert new item
        {
            // copy item
            destId = genItemId();
            getDataAccess().executeUpdate(
                    "INSERT INTO Repository"
                            + " (ID, Name, Created, Modified, Parent, ItemType, ContentType, Content, TotalContentLength, LastChunkSaved, SerialNumber)"
                            + " SELECT ?, ?, SYSDATE, SYSDATE, ?, ItemType, ContentType, Content, TotalContentLength, LastChunkSaved, 1"
                            + " FROM Repository"
                            + " WHERE ID = ?", destId, destName, destFolder.getId(), getId());

            destFolder.updateModified();
            String encodedDestName = getDataAccess().encode(destName);
            createdItem = createItemCopy(destId, destFolder.getId(), destName, destFolder.getPath() + encodedDestName + "/",
                    new Date().getTime(), new Date().getTime(), getEngine());
        } else // update existing destination
        {
            destId = destItem.getId();
            Integer destSN = destItem.getSerialNumber().intValueExact();

            getDataAccess().executeUpdate("UPDATE Repository SET " +
                            "    (Modified, ItemType, ContentType, Content, LastChunkSaved, TotalContentLength, SerialNumber) = (" +
                            "     SELECT Modified,ItemType, ContentType, Content, LastChunkSaved, TotalContentLength, ?" +
                            "        FROM" +
                            "          Repository" +
                            "        WHERE ID = ?" +
                            " )" +
                            "WHERE" +
                            "    ID=?"
                    , destSN + 1, getId(), destId);

            // remove old properties from the destination
            getDataAccess().executeUpdate("DELETE FROM Properties WHERE ItemID = ?", destId);
        }

        // copy properties
        getDataAccess().executeUpdate("INSERT INTO Properties"
                + " (ItemID, Name, Namespace, PropVal)"
                + " SELECT ?, Name, Namespace, PropVal"
                + " FROM Properties"
                + " WHERE ItemID = ?", destId, getId());

        return createdItem;
    }

    /**
     * Generates new unique id for the item.
     *
     * @return New unique int id.
     * @throws ServerException in case of DB errors.
     */
    int genItemId() throws ServerException {
        return getDataAccess().executeInt("select \"REPOSITORY_SEQ\".nextval from dual");
    }

    /**
     * Copies {@link HierarchyItemImpl} to the destination {@link FolderImpl} with the new name.
     *
     * @param destFolder Destination folder.
     * @param destName   New file name.
     * @param parent     Original folder to move from.
     * @throws ServerException in case of DB errors.
     */
    void moveThisItem(FolderImpl destFolder, String destName, FolderImpl parent) throws ServerException {

        getDataAccess().executeUpdate("UPDATE Repository SET"
                + " Name = ?"
                + ", Parent = ?"
                + " WHERE ID = ?", destName, destFolder.getId(), getId());
        getDataAccess().executeUpdate("DELETE FROM Locks WHERE ItemID = ?", getId());

        parent.updateModified();
        destFolder.updateModified();

        name = destName;
    }

    BigDecimal getSerialNumber() throws ServerException {
        return getDataAccess().executeScalar("SELECT SerialNumber FROM Repository WHERE ID = ?", id);
    }

    String getParent(String path) {
        String parentPath = StringUtil.trimEnd(StringUtil.trimStart(path, "/"), "/");
        int index = parentPath.lastIndexOf("/");
        if (index > -1) {
            parentPath = parentPath.substring(0, index);
        } else {
            parentPath = "";
        }
        return parentPath;
    }
}

package com.ithit.webdav.samples.oraclestorageservlet;

import com.ithit.webdav.server.*;
import com.ithit.webdav.server.exceptions.*;
import com.ithit.webdav.server.paging.OrderProperty;
import com.ithit.webdav.server.paging.PageResults;
import com.ithit.webdav.server.quota.Quota;
import com.ithit.webdav.server.resumableupload.ResumableUploadBase;
import com.ithit.webdav.server.search.Search;
import com.ithit.webdav.server.search.SearchOptions;

import java.math.BigDecimal;
import java.util.*;

/**
 * Represents a folder in the Oracle DB repository.
 */
public class FolderImpl extends HierarchyItemImpl implements Folder, Search, Quota, ResumableUploadBase {

    private long usedBytes;

    /**
     * Initializes a new instance of the {@link FolderImpl} class.
     *
     * @param id       Id of the item in DB.
     * @param parentId Id of the parent item in DB.
     * @param name     Name of hierarchy item.
     * @param path     Relative to WebDAV root folder path.
     * @param created  Creation time of the hierarchy item.
     * @param modified Modification time of the hierarchy item.
     * @param engine   Instance of current {@link WebDavEngine}.
     */
    FolderImpl(int id, int parentId, String name, String path, long created, long modified, WebDavEngine engine) {
        super(id, parentId, name, path, created, modified, engine);
    }

    /**
     * Gets the array of this folder's children.
     *
     * @param propNames List of properties to retrieve with the children. They will be queried by the engine later.
     * @param offset The number of items to skip before returning the remaining items.
     * @param nResults The number of items to return.
     * @param orderProps List of order properties requested by the client.
     * @return Instance of {@link PageResults} class that contains items on a requested page and total number of items in a folder.
     * @throws ServerException In case of an error.
     */
    public PageResults getChildren(List<Property> propNames, Long offset, Long nResults, List<OrderProperty> orderProps) throws ServerException {
        offset = offset == null ? 0 : offset;
        nResults = nResults == null || nResults < 0 ? 10 : nResults;

        String sqlBeforeOrder = "SELECT ID, Parent, ItemType, Name, Created, Modified, LastChunkSaved, " +
                "TotalContentLength, row_number() over (ORDER BY ";

        StringBuilder order = new StringBuilder();
        if (orderProps != null && !orderProps.isEmpty()) {
            for (OrderProperty orderProperty : orderProps) {
                String sortPropertyName = orderProperty.getProperty().getName();
                String sortPropertyVal = orderProperty.isAscending() ? " ASC " : " DESC ";
                if ("is-directory".equals(sortPropertyName))
                    order.append("ItemType").append(sortPropertyVal).append(", ");

                else if ("quota-used-bytes".equals(sortPropertyName))
                    order.append("TotalContentLength").append(sortPropertyVal).append(", ");

                else if ("getlastmodified".equals(sortPropertyName))
                    order.append("Modified").append(sortPropertyVal).append(", ");

                else if ("displayname".equals(sortPropertyName))
                    order.append("Name").append(sortPropertyVal).append(", ");

                else if ("getcontenttype".equals(sortPropertyName))
                    order.append("substr(Name, nullif(instr(Name,'.', -1) + 1, 1))")
                            .append(sortPropertyVal).append(", ");
            }

            if (order.lastIndexOf(", ") == order.length() - 2) {
                order = new StringBuilder(order.substring(0, order.length() - 2));
            }
        } else {
            order.append("ID ASC");
        }

        String sqlAfterOrder = ") line_number FROM Repository ";

        String whereQuery = " WHERE Parent = ? AND ID != 0 ";

        List<HierarchyItemImpl> hierarchyItems = getDataAccess().readItems(
                "SELECT * FROM (" + sqlBeforeOrder + order + sqlAfterOrder + whereQuery +
                        ") WHERE line_number BETWEEN ? AND ? ORDER BY line_number",
                getPath(), true, id, offset + 1, nResults + offset);

        long hierarchyItemsSize = getDataAccess()
                .executeInt("SELECT COUNT(ID) FROM Repository " + whereQuery, id);

        return new PageResults(hierarchyItems, hierarchyItemsSize);
    }

    /**
     * Creates new {@link FileImpl} file with the specified name in this folder.
     *
     * @param name Name of the file to create.
     * @return Reference to created {@link File}.
     * @throws LockedException This folder was locked. Client did not provide the lock token.
     * @throws ServerException In case of an error.
     */
    public FileImpl createFile(String name) throws LockedException, ServerException {
        ensureHasToken();
        return (FileImpl) createChild(name, ItemType.FILE);
    }

    /**
     * Creates new {@link FolderImpl} folder with the specified name in this folder.
     *
     * @param name Name of the folder to create.
     * @throws LockedException This folder was locked. Client did not provide the lock token.
     * @throws ServerException In case of an error.
     */
    public void createFolder(String name) throws LockedException, ServerException {
        ensureHasToken();
        createChild(name, ItemType.FOLDER);
    }

    /**
     * Creates child {@link HierarchyItemImpl} of the specified type.
     *
     * @param name     Name of the {@link HierarchyItemImpl} to create.
     * @param itemType Item type.
     * @return Newly created {@link FileImpl} or null if {@link FolderImpl}.
     * @throws ServerException in case of DB errors.
     */
    private HierarchyItemImpl createChild(String name, byte itemType) throws ServerException {

        int newId = genItemId();
        getDataAccess().executeUpdate("INSERT INTO Repository"
                        + " (ID, Name, Created, Modified, Parent, ItemType, Content, LastChunkSaved, TotalContentLength, SerialNumber)"
                        + " VALUES(?, ?, ?, ?, ?, ?, EMPTY_BLOB(), CURRENT_TIMESTAMP, 0, 1)",
                newId,
                name,
                new java.sql.Timestamp(new Date().getTime()),
                new java.sql.Timestamp(new Date().getTime()),
                getId(),
                itemType);


        updateModified();

        HierarchyItemImpl item = null;
        if (itemType == ItemType.FILE) {
            long now = new Date().getTime();
            item = new FileImpl(newId, getId(), name, getPath() + name, now, now, now, 0, getEngine());
        }
        getEngine().getWebSocketServer().notifyCreated(getPath() + name);
        return item;

    }

    /**
     * Check whether client is the lock owner.
     *
     * @throws LockedException in case if not owner.
     * @throws ServerException other errors.
     */
    void ensureHasTokenForTree() throws LockedException, ServerException {
        if (!clientHasTokenForTree())
            throw new LockedException();
    }

    /**
     * Searches for the child {@link HierarchyItemImpl} by the child name.
     *
     * @param childName Name to search.
     * @return HierarchyItemImpl.
     * @throws ServerException in case of DB errors.
     */
    HierarchyItemImpl findChild(String childName) throws ServerException {
        return getDataAccess().readItem("SELECT ID, Parent, ItemType, Name, Created, Modified, LastChunkSaved, TotalContentLength"
                + " FROM Repository"
                + " WHERE Parent = ?"
                + " AND Name = ?", getPath(), true, getId(), childName);
    }


    @Override
    public void moveTo(Folder folder, String destName)
            throws LockedException, ConflictException, MultistatusException, ServerException {

        FolderImpl destFolder = getDataAccess().getFolderImpl(folder);

        FolderImpl parent = getParent();

        ensureHasToken();
        destFolder.ensureHasToken();
        parent.ensureHasToken();

        HierarchyItemImpl destItem = destFolder.findChild(destName);
        FolderImpl newDestFolder;

        // copy this folder
        if (destItem != null) {
            if (destItem instanceof File) {
                destItem.delete();
                newDestFolder = (FolderImpl) copyThisItem(destFolder, null, destName);
            } else {
                newDestFolder = getDataAccess().getFolderImpl(destItem);
            }
        } else {
            newDestFolder = (FolderImpl) copyThisItem(destFolder, null, destName);
        }
        // move children
        MultistatusException mr = new MultistatusException();
        for (HierarchyItem child : getChildren(Collections.emptyList(), null, null, null).getPage()) {
            try {
                child.moveTo(newDestFolder, child.getName());
            } catch (MultistatusException e) {
                mr.addResponses(e.getResponses());
            } catch (DavException e) {
                mr.addResponse(child.getPath(), e.getStatus());
            }
        }
        if (mr.getResponses().length > 0)
            throw mr;
        try {
            getEngine().getSearchFacade().getIndexer().deleteIndex(this);
            getEngine().getSearchFacade().getIndexer().indexFile(newDestFolder.getName(), newDestFolder.getId(), null, this);
        } catch (Exception ex) {
            getEngine().getLogger().logError("Errors during indexing.", ex);
        }
        // delete this folder
        deleteThisItem();
        getEngine().getWebSocketServer().notifyMoved(getPath(), folder.getPath() + destName);
    }

    @Override
    public void copyTo(Folder folder, String destName, boolean deep)
            throws LockedException, MultistatusException, ServerException {

        FolderImpl destFolder = getDataAccess().getFolderImpl(folder);

        destFolder.ensureHasToken();

        HierarchyItemImpl destItem = destFolder.findChild(destName);
        if (isRecursive(destFolder)) {
            throw new ServerException("Cannot copy to subfolder", WebDavStatus.FORBIDDEN);
        }
        FolderImpl newDestFolder;

        if (destItem != null) {
            if (destItem instanceof File) {
                destItem.delete();
                newDestFolder = (FolderImpl) copyThisItem(destFolder, null, destName);
            } else {
                newDestFolder = getDataAccess().getFolderImpl(destItem);
            }
        } else {
            newDestFolder = (FolderImpl) copyThisItem(destFolder, null, destName);
        }

        try {
            getEngine().getSearchFacade().getIndexer().indexFile(newDestFolder.getName(), newDestFolder.getId(), null, this);
        } catch (Exception ex) {
            getEngine().getLogger().logError("Errors during indexing.", ex);
        }

        // copy children
        MultistatusException mr = new MultistatusException();
        if (deep) {
            for (HierarchyItem child : getChildren(Collections.emptyList(), null, null, null).getPage()) {
                try {
                    child.copyTo(newDestFolder, child.getName(), deep);
                } catch (MultistatusException ex) {
                    mr.addResponses(ex.getResponses());
                } catch (DavException ex) {
                    mr.addResponse(child.getPath(), ex.getStatus());
                }
            }
        }
        getEngine().getWebSocketServer().notifyCreated(folder.getPath() + destName);
        if (mr.getResponses().length > 0)
            throw mr;

    }

    /**
     * Check whether current folder is the parent to the destination.
     *
     * @param destFolder Path to the destination folder.
     * @return True if current folder is parent for the destination, false otherwise.
     * @throws ServerException in case of any server exception.
     */
    private boolean isRecursive(Folder destFolder) throws ServerException {
        return destFolder.getPath().startsWith(getPath());
    }

    @Override
    public void delete() throws ServerException, LockedException, MultistatusException {

        getParent().ensureHasToken();
        ensureHasToken();

        MultistatusException mx = new MultistatusException();

        for (HierarchyItem child : getChildren(Collections.emptyList(), null, null, null).getPage()) {
            try {
                child.delete();
            } catch (MultistatusException ex) {
                mx.addResponses(ex.getResponses());
            }
        }
        if (mx.getResponses().length > 0)
            throw mx;
        else {
            try {
                getEngine().getSearchFacade().getIndexer().deleteIndex(this);
            } catch (Exception ex) {
                getEngine().getLogger().logError("Errors during indexing.", ex);
            }
            deleteThisItem();
        }
        getEngine().getWebSocketServer().notifyDeleted(getPath());
    }

    /**
     * Removes {@link HierarchyItemImpl} tree which is equivalent of the folder remove.
     *
     * @throws ServerException in case of DB errors.
     */
    void removeTree() throws ServerException {
        for (HierarchyItem child : getChildren(Collections.emptyList(), null, null, null).getPage()) {
            FolderImpl childFolder = child instanceof FolderImpl ? (FolderImpl) child : null;
            if (childFolder != null)
                childFolder.removeTree();
            else
                ((HierarchyItemImpl)child).deleteThisItem();
        }
        deleteThisItem();
    }

    /**
     * Check whether client is the lock owner.
     *
     * @throws ServerException other errors.
     */
    private boolean clientHasTokenForTree() throws ServerException {

        if (!clientHasToken())
            return false;
        for (HierarchyItem child : getChildren(Collections.emptyList(), null, null, null).getPage()) {
            FolderImpl childFolder = child instanceof FolderImpl ? (FolderImpl) child : null;
            if (childFolder != null) {
                if (!childFolder.clientHasTokenForTree())
                    return false;
            } else {
                if (!((HierarchyItemImpl)child).clientHasToken())
                    return false;
            }
        }
        return true;
    }

    @Override
    protected HierarchyItemImpl createItemCopy(int id, int parentId, String name, String path, long created, long modified,
                                               WebDavEngine engine) {
        return new FolderImpl(id, parentId, name, path, created, modified, engine);
    }

    /**
     * Returns list of items that correspond to search request.
     * <p>
     * <p>
     * This method is called by {@link Engine} when client application is sending search request.
     * In your implementation you must return the list of items that correspond to the requested search phrase and options.
     * </p>
     * <p>The search phrase may contain wildcards:</p>
     * <p>To indicate one or more characters the '%' is passed in search string.</p>
     * <p>To indicate exactly one character the '_' is passed in search string.</p>
     * <p>To include '%', '_' and '\' characters in the search string thay are escaped with '\' character.</p>
     * <p>Note that IT Hit Ajax File Browser is using '*' and '?' as wildcard characters. In case included in search they are replaced with '%' and '_'.</p>
     *
     * @param searchString A phrase to search.
     * @param options      Search parameters.
     * @param propNames    List of properties to retrieve with the children. They will be queried by the engine later.
     * @param offset The number of items to skip before returning the remaining items.
     * @param nResults The number of items to return.
     * @return Instance of {@link PageResults} class that contains items on a requested page and total number of items in search results.
     */
    @Override
    public PageResults search(String searchString, SearchOptions options, List<Property> propNames, Long offset, Long nResults) {
        List<HierarchyItem> results = new LinkedList<>();
        SearchFacade.Searcher searcher = getEngine().getSearchFacade().getSearcher();
        if (searcher == null) {
            return new PageResults(results, (long) 0);
        }
        boolean snippet = false;
        for (Property pr : propNames) {
            if (SNIPPET.equalsIgnoreCase(pr.getName())) {
                snippet = true;
                break;
            }
        }
        Map<String, String> searchResult;
        searchResult = searcher.search(searchString, options, snippet);
        for (Map.Entry<String, String> entry : searchResult.entrySet()) {
            try {
                String path = getDataAccess().executeScalar("select path from " +
                        "  (SELECT id, SYS_CONNECT_BY_PATH(name, '/') path " +
                        "   FROM REPOSITORY where id = (select min(id) from REPOSITORY) " +
                        "   START WITH id = ? " +
                        "   CONNECT BY id = PRIOR parent and parent!= prior id)", entry.getKey());
                String[] pathParts = path.split("/");
                pathParts = Arrays.copyOf(pathParts, pathParts.length - 1);
                StringBuilder pathBuilder = new StringBuilder();
                for (int i = pathParts.length - 1; i >= 0; i--) {
                    if (Objects.equals(pathParts[i], "")) {
                        continue;
                    }
                    pathBuilder.append('/');
                    pathBuilder.append(pathParts[i]);
                }
                String itemPath = pathBuilder.toString();
                String decodedPath = getDataAccess().decode(getPath());
                if (itemPath.startsWith(decodedPath)) {
                    HierarchyItem item = getDataAccess().getFile(Integer.parseInt(entry.getKey()), itemPath);
                    if (item != null) {
                        if (snippet && item instanceof FileImpl) {
                            ((FileImpl) item).setSnippet(entry.getValue());
                        }
                        results.add(item);
                    }
                }
            } catch (Exception ex) {
                getEngine().getLogger().logError("Error during search.", ex);
            }
        }
        return new PageResults(results, (long) results.size());
    }

    /**
     * Value in bytes representing the amount of additional disk space beyond the current
     * allocation that can be allocated to the folder(or other item) before further
     * allocations will be refused.  It is understood that this space may be
     * consumed by allocations to other files/folders.
     *
     * @return Bytes that can be additionally allocated in folder/file.
     */
    @Override
    public long getAvailableBytes() {
        try {
            if (usedBytes == 0) {
                usedBytes = getUsedBytes();
            }
            return getDataAccess().getTotalBytes() - usedBytes;
        } catch (Exception e) {
            getEngine().getLogger().logError(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Value in bytes representing the amount of space used by this folder/file
     * and possibly a number of other similar folders/files, where the set of "similar" meets at least
     * the criterion that allocating space to any folder/file in the set will
     * count against the {@link #getAvailableBytes()}.  It MUST include the
     * total count including usage derived from sub-items if
     * appropriate.  It SHOULD include metadata storage size if metadata
     * storage is counted against the {@link #getAvailableBytes()}
     *
     * @return Bytes occupied by folder/file.
     */
    @Override
    public long getUsedBytes() {
        BigDecimal bytes = BigDecimal.valueOf(0);
        try {
            if (usedBytes == 0 && getDataAccess().getDefaultTableSpace() != null) {
                bytes = getDataAccess().executeScalar("SELECT sum(bytes) FROM dba_segments " +
                        "where tablespace_name=? " +
                        "GROUP BY tablespace_name", getDataAccess().getDefaultTableSpace());
            } else {
                return usedBytes;
            }

        } catch (Exception e) {
            getEngine().getLogger().logError(e.getMessage(), e);
        }
        return bytes.longValue();
    }
}
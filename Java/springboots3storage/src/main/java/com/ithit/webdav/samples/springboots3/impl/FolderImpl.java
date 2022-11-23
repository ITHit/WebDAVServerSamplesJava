package com.ithit.webdav.samples.springboots3.impl;

import com.ithit.webdav.server.File;
import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Property;
import com.ithit.webdav.server.exceptions.*;
import com.ithit.webdav.server.paging.OrderProperty;
import com.ithit.webdav.server.paging.PageResults;
import com.ithit.webdav.server.resumableupload.ResumableUploadBase;
import software.amazon.awssdk.core.exception.SdkException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a folder in the File system repository.
 */
public final class FolderImpl extends HierarchyItemImpl implements Folder, ResumableUploadBase {


    /**
     * Initializes a new instance of the {@link FolderImpl} class.
     *
     * @param name     Name of hierarchy item.
     * @param path     Relative to WebDAV root folder path.
     * @param created  Creation time of the hierarchy item.
     * @param modified Modification time of the hierarchy item.
     * @param engine   Instance of current {@link WebDavEngine}
     */
    private FolderImpl(String name, String path, long created, long modified,
                       WebDavEngine engine) {
        super(name, path, created, modified, engine);
    }

    /**
     * Returns folder that corresponds to path.
     *
     * @param path   Encoded path relative to WebDAV root.
     * @param engine Instance of {@link WebDavEngine}
     * @return Folder instance or null if physical folder not found in file system.
     */
    public static FolderImpl getFolder(String path, String name, long created, long modified, WebDavEngine engine) {
        return new FolderImpl(name, fixPath(path), created, modified, engine);
    }

    private static String fixPath(String path) {
        if (!Objects.equals(path.substring(path.length() - 1), "/")) {
            path += "/";
        }
        return path;
    }

    /**
     * Creates new {@link FileImpl} file with the specified name in this folder.
     *
     * @param name Name of the file to create.
     * @return Reference to created {@link File}.
     * @throws LockedException This folder was locked. Client did not provide the lock token.
     * @throws ServerException In case of an error.
     */
    @Override
    public FileImpl createFile(String name) throws LockedException, ServerException {
        ensureHasToken();
        final String decodedName = decode(name);
        final String originalPath = getPath() + decodedName;
        final HierarchyItem hierarchyItem = getEngine().getDataClient().locateObject(originalPath, getEngine());
        if (hierarchyItem == null) {
            try {
                getEngine().getDataClient().storeObject(originalPath, null, null, 0);
                getEngine().getWebSocketServer().notifyCreated(getPath() + getEngine().getDataClient().encode(name), getWebSocketID());
                final long created = System.currentTimeMillis();
                return FileImpl.getFile(originalPath, decodedName, created, created, 0, getEngine());
            } catch (Exception e) {
                throw new ServerException(e);
            }
        }
        return null;
    }

    /**
     * Creates new {@link FolderImpl} folder with the specified name in this folder.
     *
     * @param name Name of the folder to create.
     * @throws LockedException This folder was locked. Client did not provide the lock token.
     * @throws ServerException In case of an error.
     */
    @Override
    public void createFolder(String name) throws LockedException,
            ServerException {
        ensureHasToken();

        final String originalPath = getPath() + decode(name) + "/";
        final HierarchyItem hierarchyItem = getEngine().getDataClient().locateObject(originalPath, getEngine());
        if (hierarchyItem == null) {
            try {
                getEngine().getDataClient().createFolder(originalPath);
            } catch (Exception e) {
                throw new ServerException(e);
            }
            getEngine().getWebSocketServer().notifyCreated(getPath() + getEngine().getDataClient().encode(name), getWebSocketID());
        }
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
    @Override
    public PageResults getChildren(List<Property> propNames, Long offset, Long nResults, List<OrderProperty> orderProps) throws ServerException {
        List<HierarchyItem> children = getEngine().getDataClient().getChildren(getPath(), getEngine());
        children = sortChildren(children, orderProps);
        Long total = (long) children.size();
        if (offset != null && nResults != null)
        {
            children = children.stream().skip(offset).limit(nResults).collect(Collectors.toList());
        }
        return new PageResults(children, total);
    }

    @Override
    public void delete() throws LockedException,
            ServerException {
        deleteInternal(0);
    }

    @Override
    public void deleteInternal(int recursionDepth) throws LockedException, ServerException {
        ensureHasToken();
        try {
            for (HierarchyItem hierarchyItem : getChildren(null, null, null, null).getPage()) {
                try {
                    ((HierarchyItemImpl)hierarchyItem).deleteInternal(recursionDepth + 1);
                } catch (Exception e) {
                    throw new ServerException();
                }
            }
            getEngine().getDataClient().delete(getPath());
            if (recursionDepth == 0) {
                getEngine().getWebSocketServer().notifyDeleted(getPath(), getWebSocketID());
            }
        } catch (SdkException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public void copyTo(Folder folder, String destName, boolean deep)
            throws LockedException, ServerException {
        copyToInternal(folder, destName, deep, 0);
    }

    @Override
    public void copyToInternal(Folder folder, String destName, boolean deep, int recursionDepth) throws LockedException, ServerException {
        ((FolderImpl) folder).ensureHasToken();

        String relUrl = decodeAndConvertToPath(folder.getPath());
        if (isRecursive(relUrl)) {
            throw new ServerException("Cannot copy to subfolder", WebDavStatus.FORBIDDEN);
        }
        final Folder destFolder = getDestinationFolder(folder, destName);
        try {
            for (HierarchyItem hierarchyItem : getChildren(null, null, null, null).getPage()) {
                try {
                    ((HierarchyItemImpl)hierarchyItem).copyToInternal(destFolder, hierarchyItem.getName(), deep, recursionDepth + 1);
                } catch (Exception e) {
                    throw new ServerException();
                }
            }
        } catch (SdkException e) {
            throw new ServerException(e);
        }
        if (recursionDepth == 0) {
            getEngine().getWebSocketServer().notifyCreated(folder.getPath() + getEngine().getDataClient().encode(destName), getWebSocketID());
        }
    }

    @Override
    public void moveTo(Folder folder, String destName) throws LockedException,
            ConflictException, ServerException {
        moveToInternal(folder, destName, 0);
    }

    @Override
    public void moveToInternal(Folder folder, String destName, int recursionDepth) throws LockedException, ConflictException, ServerException {
        ensureHasToken();
        ((FolderImpl) folder).ensureHasToken();
        String relUrl = decodeAndConvertToPath(folder.getPath());
        if (isRecursive(relUrl)) {
            throw new ServerException("Cannot move to subfolder", WebDavStatus.FORBIDDEN);
        }
        final Folder destFolder = getDestinationFolder(folder, destName);

        try {
            for (HierarchyItem hierarchyItem : getChildren(null, null, null, null).getPage()) {
                try {
                    ((HierarchyItemImpl)hierarchyItem).moveToInternal(destFolder, hierarchyItem.getName(), recursionDepth + 1);
                    hierarchyItem.delete();
                } catch (Exception e) {
                    throw new ServerException();
                }
            }
            getEngine().getDataClient().delete(getPath());
        } catch (SdkException e) {
            throw new ServerException(e);
        }
        if (recursionDepth == 0) {
            getEngine().getWebSocketServer().notifyMoved(getPath(), folder.getPath() + getEngine().getDataClient().encode(destName), getWebSocketID());
        }
    }

    /**
     * Checks if destination parent folder exists and creates destination folder.
     */
    private Folder getDestinationFolder(Folder folder, String destName) throws ServerException, LockedException {
        final HierarchyItem dFolder = getEngine().getDataClient().locateObject(folder.getPath(), getEngine());
        if (!(dFolder instanceof Folder))
            throw new ServerException();
        ((Folder) dFolder).createFolder(destName);
        final HierarchyItem destFolder = getEngine().getDataClient().locateObject(folder.getPath() + destName + "/", getEngine());
        if (!(destFolder instanceof Folder))
            throw new ServerException();
        return (Folder) destFolder;
    }

    /**
     * Check whether current folder is the parent to the destination.
     *
     * @param destFolder Path to the destination folder.
     * @return True if current folder is parent for the destination, false otherwise.
     * @throws ServerException in case of any server exception.
     */
    private boolean isRecursive(String destFolder) throws ServerException {
        return destFolder.startsWith(getPath().replace("/", java.io.File.separator));
    }

    /**
     * Sorts array of FileSystemInfo according to the specified order.
     * @param paths Array of files and folders to sort.
     * @param orderProps Sorting order.
     * @return Sorted list of files and folders.
     */
    private List<HierarchyItem> sortChildren(List<HierarchyItem> paths, List<OrderProperty> orderProps) {
        if (orderProps != null && !orderProps.isEmpty()) {
            int index = 0;
            Comparator<HierarchyItem> comparator = null;
            for (OrderProperty orderProperty :
                    orderProps) {
                Comparator<HierarchyItem> tempComp = null;
                if ("is-directory".equals(orderProperty.getProperty().getName())) {
                    Function<HierarchyItem, Boolean> sortFunc = Folder.class::isInstance;
                    tempComp = Comparator.comparing(sortFunc);
                }
                if ("quota-used-bytes".equals(orderProperty.getProperty().getName())) {
                    Function<HierarchyItem, Long> sortFunc = item -> {
                        try {
                            return (item instanceof File ? ((File) item).getContentLength() : 0);
                        } catch (ServerException e) {
                            return 0L;
                        }
                    };
                    tempComp = Comparator.comparing(sortFunc);
                }
                if ("getlastmodified".equals(orderProperty.getProperty().getName())) {
                    Function<HierarchyItem, Long> sortFunc = item -> {
                        try {
                            return item.getModified();
                        } catch (ServerException e) {
                            return 0L;
                        }
                    };
                    tempComp = Comparator.comparing(sortFunc);
                }
                if ("displayname".equals(orderProperty.getProperty().getName())) {
                    Function<HierarchyItem, String> sortFunc = item -> {
                        try {
                            return item.getName();
                        } catch (ServerException e) {
                            return "";
                        }
                    };
                    tempComp = Comparator.comparing(sortFunc);
                }
                if ("getcontenttype".equals(orderProperty.getProperty().getName())) {
                    Function<HierarchyItem, String> sortFunc = item -> {
                        try {
                            return getExtension(item.getName());
                        } catch (ServerException e) {
                            return "";
                        }
                    };
                    tempComp = Comparator.comparing(sortFunc);
                }
                if (tempComp != null) {
                    if (index++ == 0) {
                        if (orderProperty.isAscending()) {
                            comparator = tempComp;
                        } else {
                            comparator = tempComp.reversed();
                        }
                    } else {
                        if (orderProperty.isAscending()) {
                            comparator = comparator != null ? comparator.thenComparing(tempComp) : tempComp;
                        } else {
                            comparator = comparator != null ? comparator.thenComparing(tempComp.reversed()) : tempComp.reversed();
                        }
                    }
                }
            }
            if (comparator != null) {
                paths = paths.stream().sorted(comparator).collect(Collectors.toList());
            }
        }
        return paths;
    }

    private String getExtension(String name) {
        int periodIndex = name.lastIndexOf('.');
        return periodIndex == -1 ? "" : name.substring(periodIndex + 1);

    }
}

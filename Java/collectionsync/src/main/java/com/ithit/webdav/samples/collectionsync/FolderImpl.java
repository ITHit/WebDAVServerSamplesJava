package com.ithit.webdav.samples.collectionsync;

import com.ithit.webdav.samples.collectionsync.filesystem.FileSystemExtension;
import com.ithit.webdav.server.File;
import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Property;
import com.ithit.webdav.server.exceptions.*;
import com.ithit.webdav.server.paging.OrderProperty;
import com.ithit.webdav.server.paging.PageResults;
import com.ithit.webdav.server.quota.Quota;
import com.ithit.webdav.server.resumableupload.ResumableUploadBase;
import com.ithit.webdav.server.search.Search;
import com.ithit.webdav.server.search.SearchOptions;
import com.ithit.webdav.server.synchronization.Change;
import com.ithit.webdav.server.synchronization.Changes;
import com.ithit.webdav.server.synchronization.SynchronizationCollection;
import com.ithit.webdav.server.util.StringUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents a folder in the File system repository.
 */
final class FolderImpl extends HierarchyItemImpl implements Folder, Search, Quota, ResumableUploadBase, SynchronizationCollection {


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
     * @throws ServerException in case of exception
     */
    static FolderImpl getFolder(String path, WebDavEngine engine) throws ServerException {
        BasicFileAttributes view = null;
        Path fullPath;
        String name = null;
        try {
            boolean root = path.equals("/");
            String pathFragment = decodeAndConvertToPath(path);
            String rootFolder = getRootFolder();
            fullPath = root ? Paths.get(rootFolder) : Paths.get(rootFolder, pathFragment);
            if (Files.exists(fullPath)) {
                name = root ? "ROOT" : Paths.get(pathFragment).getFileName().toString();
                view = Files.getFileAttributeView(fullPath, BasicFileAttributeView.class).readAttributes();
            }
            if (view == null || !view.isDirectory()) {
                return null;
            }
        } catch (IOException e) {
            throw new ServerException();
        }

        long created = view.creationTime().toMillis();
        long modified = view.lastModifiedTime().toMillis();
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
    // <<<< createFileImpl
    @Override
    public FileImpl createFile(String name) throws LockedException, ServerException {
        ensureHasToken();

        Path fullPath = deleteIfHidden(name);
        if (!Files.exists(fullPath)) {
            try {
                Files.createFile(fullPath);
            } catch (IOException e) {
                throw new ServerException(e);
            }
            final String itemPath = getPath() + encode(name);
            getEngine().getWebSocketServer().notifyCreated(itemPath, getWebSocketID());
            return FileImpl.getFile(itemPath, getEngine());
        }
        return null;
    }
    // createFileImpl >>>>

    /**
     * Creates new {@link FolderImpl} folder with the specified name in this folder.
     *
     * @param name Name of the folder to create.
     * @throws LockedException This folder was locked. Client did not provide the lock token.
     * @throws ServerException In case of an error.
     */
    // <<<< createFolderImpl
    @Override
    public void createFolder(String name) throws LockedException,
            ServerException {
        createFolderInternal(name);

        getEngine().getWebSocketServer().notifyCreated(getPath() + encode(name), getWebSocketID());
    }

    private void createFolderInternal(String name) throws LockedException,
            ServerException {
        ensureHasToken();

        Path fullPath = deleteIfHidden(name);
        if (!Files.exists(fullPath)) {
            try {
                Files.createDirectory(fullPath);
            } catch (IOException e) {
                throw new ServerException(e);
            }
        }
    }
    // createFolderImpl >>>>

    private Path deleteIfHidden(String name) throws ServerException {
        Path fullPath = Paths.get(this.getFullPath().toString(), name);
        if (Files.exists(fullPath) && isHidden(fullPath)) {
            try {
                Files.delete(fullPath);
            } catch (IOException e) {
                throw new ServerException(e);
            }
        }
        return fullPath;
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
    // <<<< getChildren
    @Override
    public PageResults getChildren(List<Property> propNames, Long offset, Long nResults, List<OrderProperty> orderProps) throws ServerException {
        String decodedPath = HierarchyItemImpl.decodeAndConvertToPath(getPath());
        Path fullFolderPath = Paths.get(getRootFolder() + decodedPath);
        List<HierarchyItemImpl> children = new ArrayList<>();
        Long total = null;
        DirectoryStream.Filter<Path> notHiddenFilter = entry -> !isHidden(entry);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(fullFolderPath, notHiddenFilter)) {
            List<Path> paths = StreamSupport.stream(ds.spliterator(), false).collect(Collectors.toList());
            paths = sortChildren(paths, orderProps);
            for (Path p : paths) {
                String childPath = getPath() + encode(p.getFileName().toString());
                HierarchyItemImpl item = (HierarchyItemImpl) getEngine().getHierarchyItem(childPath);
                children.add(item);
            }
            total = (long) paths.size();
            if (offset != null && nResults != null)
            {
                children = children.stream().skip(offset).limit(nResults).collect(Collectors.toList());
            }
        } catch (IOException e) {
            getEngine().getLogger().logError(e.getMessage(), e);
        }
        return new PageResults(children, total);
    }
    // getChildren >>>>

    // <<<< deleteFolderImpl
    @Override
    public void delete() throws LockedException, MultistatusException,
            ServerException {
        ensureHasToken();
        try {
            removeIndex(getFullPath(), this);
            FileUtils.cleanDirectory(getFullPath().toFile());
            if (isHidden(getFullPath())) {
                // hide folder, it is needed for sync-collection report.
                Files.setAttribute(getFullPath(), "dos:hidden", true, LinkOption.NOFOLLOW_LINKS);
            } else {
                FileUtils.deleteDirectory(getFullPath().toFile());
            }
        } catch (IOException e) {
            throw new ServerException(e);
        }
        getEngine().getWebSocketServer().notifyDeleted(getPath(), getWebSocketID());
    }
    // deleteFolderImpl >>>>

    // <<<< copyToFolderImpl
    @Override
    public void copyTo(Folder folder, String destName, boolean deep)
            throws LockedException, MultistatusException, ServerException {
        ((FolderImpl) folder).ensureHasToken();

        String relUrl = HierarchyItemImpl.decodeAndConvertToPath(folder.getPath());
        String destinationFolder = Paths.get(getRootFolder(), relUrl).toString();
        if (isRecursive(relUrl)) {
            throw new ServerException("Cannot copy to subfolder", WebDavStatus.FORBIDDEN);
        }
        final Path destinationFolderPath = Paths.get(destinationFolder);
        if (!Files.exists(destinationFolderPath) || isHidden(destinationFolderPath))
            throw new ServerException();
        try {
            Path sourcePath = this.getFullPath();
            Path destinationFullPath = Paths.get(destinationFolder, destName);
            FileUtils.copyDirectory(sourcePath.toFile(), destinationFullPath.toFile());
            addIndex(destinationFullPath, folder.getPath() + destName, destName);
        } catch (IOException e) {
            throw new ServerException(e);
        }
        setName(destName);
        getEngine().getWebSocketServer().notifyCreated(folder.getPath() + encode(destName), getWebSocketID());
    }
    // copyToFolderImpl >>>>

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
    private List<Path> sortChildren(List<Path> paths, List<OrderProperty> orderProps) {
        if (orderProps != null && !orderProps.isEmpty()) {
            int index = 0;
            Comparator<Path> comparator = null;
            for (OrderProperty orderProperty :
                    orderProps) {
                Comparator<Path> tempComp = null;
                if ("is-directory".equals(orderProperty.getProperty().getName())) {
                    Function<Path, Boolean> sortFunc = item -> item.toFile().isDirectory();
                    tempComp = Comparator.comparing(sortFunc);
                }
                if ("quota-used-bytes".equals(orderProperty.getProperty().getName())) {
                    Function<Path, Long> sortFunc = item -> item.toFile().length();
                    tempComp = Comparator.comparing(sortFunc);
                }
                if ("getlastmodified".equals(orderProperty.getProperty().getName())) {
                    Function<Path, Long> sortFunc = item -> item.toFile().lastModified();
                    tempComp = Comparator.comparing(sortFunc);
                }
                if ("displayname".equals(orderProperty.getProperty().getName())) {
                    Function<Path, String> sortFunc = item -> item.getFileName().toString();
                    tempComp = Comparator.comparing(sortFunc);
                }
                if ("getcontenttype".equals(orderProperty.getProperty().getName())) {
                    Function<Path, String> sortFunc = item -> getExtension(item.getFileName().toString());
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

    // <<<< moveToFolderImpl
    @Override
    public void moveTo(Folder folder, String destName) throws LockedException,
            ConflictException, MultistatusException, ServerException {
        ensureHasToken();
        ((FolderImpl) folder).ensureHasToken();
        String destinationFolder = Paths.get(getRootFolder(), HierarchyItemImpl.decodeAndConvertToPath(folder.getPath())).toString();
        if (!Files.exists(Paths.get(destinationFolder)))
            throw new ConflictException();
        Path sourcePath = this.getFullPath();
        Path destinationFullPath = Paths.get(destinationFolder, destName);
        try {
            removeIndex(getFullPath(), this);
            Files.move(sourcePath, destinationFullPath, StandardCopyOption.REPLACE_EXISTING);
            addIndex(destinationFullPath, folder.getPath() + destName, destName);
        } catch (IOException e) {
            throw new ServerException(e);
        }
        setName(destName);
        getEngine().getWebSocketServer().notifyMoved(getPath(), folder.getPath() + encode(destName), getWebSocketID());
    }
    // moveToFolderImpl >>>>

    /**
     * Returns list of items that correspond to search request.
     *
     * @param searchString A phrase to search.
     * @param options      Search parameters.
     * @param propNames    List of properties to retrieve with the children. They will be queried by the engine later.
     * @param offset The number of items to skip before returning the remaining items.
     * @param nResults The number of items to return.
     * @return Instance of {@link PageResults} class that contains items on a requested page and total number of items in search results.
     */
    // <<<< searchImpl
    @Override
    public PageResults search(String searchString, SearchOptions options, List<Property> propNames, Long offset, Long nResults) {
        List<HierarchyItem> results = new LinkedList<>();
        SearchFacade.Searcher searcher = getEngine().getSearchFacade().getSearcher();
        if (searcher == null) {
            return new PageResults(results, (long) 0);
        }
        boolean snippet = propNames.stream().anyMatch(x -> SNIPPET.equalsIgnoreCase(x.getName()));
        Map<String, String> searchResult;
        try {
            String decodedPath = decode(getPath());
            searchResult = searcher.search(searchString, options, decodedPath, snippet);
            for (Map.Entry<String, String> entry : searchResult.entrySet()) {
                try {
                    HierarchyItem item = getEngine().getHierarchyItem(entry.getKey());
                    if (item != null) {
                        if (snippet && item instanceof FileImpl) {
                            ((FileImpl) item).setSnippet(entry.getValue());
                        }
                        results.add(item);
                    }
                } catch (Exception ex) {
                    getEngine().getLogger().logError("Error during search.", ex);
                }
            }
        } catch (ServerException e) {
            getEngine().getLogger().logError("Error during search.", e);
        }
        return new PageResults((offset != null && nResults != null) ? results.stream().skip(offset).limit(nResults).collect(Collectors.toList()) : results, (long) results.size());
    }
    // searchImpl >>>>

    /**
     * Returns free bytes available to current user.
     *
     * @return Returns free bytes available to current user.
     */
    @Override
    public long getAvailableBytes() {
        return getFullPath().toFile().getFreeSpace();
    }

    /**
     * Returns used bytes by current user.
     *
     * @return Returns used bytes by current user.
     */
    @Override
    public long getUsedBytes() {
        long total = getFullPath().toFile().getTotalSpace();
        return total - getAvailableBytes();
    }

    /**
     * Returns a list of changes that correspond to a synchronization request.
     * @param propNames List of properties to retrieve with the children. They will be queried by the engine later.
     * @param syncToken The synchronization token provided by the server and  returned by the client.
     * @param deep Indicates the "scope" of the synchronization report request, false - immediate children and true - all children at any depth.
     * @param limit Limits the number of member URLs in a response.
     * @return Changes details for the folder.
     */
    @Override
    public Changes getChanges(List<Property> propNames, String syncToken, boolean deep, Long limit) throws ServerException {
        DavChanges changes = new DavChanges();
        Long syncUsn = null;
        Long maxUsn = 0L;

        if (!StringUtil.isNullOrEmpty(syncToken)) {
            syncUsn = Long.parseLong(syncToken);
        }
        List<Pair<HierarchyItemImpl, Long>> children = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(Paths.get(getFullPath().toString()), deep ? Integer.MAX_VALUE : 1)) {
            for (String path : stream.map(Path::toString).collect(Collectors.toSet())) {
                String childPath = StringUtil.trimEnd(getPath(), "/") + "/" + StringUtil.trimStart(path.substring(getFullPath().toString().length()).replace(java.io.File.separator, "/"), "/");
                HierarchyItemImpl child = FolderImpl.getFolder(childPath, getEngine());

                if (child == null) {
                    child = FileImpl.getFile(childPath, getEngine());
                }

                if (child != null) {
                    children.add(new ImmutablePair<>(child, FileSystemExtension.getUsn(path)));
                }
            }
        } catch (IOException ex) {
            throw new ServerException(ex);
        }

        for (Pair<HierarchyItemImpl, Long> item : children.stream().sorted(Comparator.comparingLong(Pair::getRight)).collect(Collectors.toCollection(LinkedHashSet::new))) {
            // Don't include deleted files/folders when syncToken is empty, because this is full sync.
            if (!(item.getLeft().getChangeType() == Change.DELETED && StringUtil.isNullOrEmpty(syncToken))) {
                maxUsn = item.getValue();
                if (syncUsn == null || item.getRight() > syncUsn) {
                    changes.add(item.getLeft());
                }

                if (limit != null && limit == changes.size()) {
                    changes.setMoreResults(true);
                    break;
                }
            }
        }
        changes.setNewSyncToken(maxUsn.toString());

        return changes;
    }

    private void removeIndex(Path sourcePath, FolderImpl itSelf) {
        List<HierarchyItem> filesToDelete = new ArrayList<>();
        getEngine().getSearchFacade().getFilesToIndex(sourcePath.toFile().listFiles(), filesToDelete, WebDavServlet.getRootLocalPath());
        filesToDelete.add(itSelf);
        for (HierarchyItem hi : filesToDelete) {
            try {
                getEngine().getSearchFacade().getIndexer().deleteIndex(hi);
            } catch (Exception e) {
                getEngine().getLogger().logError("Cannot delete index.", e);
            }
        }
    }

    private void addIndex(Path sourcePath, String path, String name) {
        List<HierarchyItem> filesToIndex = new ArrayList<>();
        getEngine().getSearchFacade().getFilesToIndex(sourcePath.toFile().listFiles(), filesToIndex, WebDavServlet.getRootLocalPath());
        getEngine().getSearchFacade().getIndexer().indexFile(name, decode(path), null, null);
        for (HierarchyItem hi : filesToIndex) {
            try {
                getEngine().getSearchFacade().getIndexer().indexFile(hi.getName(), decode(hi.getPath()), null, hi);
            } catch (Exception e) {
                getEngine().getLogger().logError("Cannot index.", e);
            }
        }
    }
}

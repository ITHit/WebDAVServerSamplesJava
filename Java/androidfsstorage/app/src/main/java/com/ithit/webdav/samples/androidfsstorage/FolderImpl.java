package com.ithit.webdav.samples.androidfsstorage;

import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.Property;
import com.ithit.webdav.server.exceptions.ConflictException;
import com.ithit.webdav.server.exceptions.LockedException;
import com.ithit.webdav.server.exceptions.MultistatusException;
import com.ithit.webdav.server.exceptions.ServerException;
import com.ithit.webdav.server.exceptions.WebDavStatus;
import com.ithit.webdav.server.paging.OrderProperty;
import com.ithit.webdav.server.paging.PageResults;
import com.ithit.webdav.server.quota.Quota;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a folder in the File system repository.
 */
class FolderImpl extends HierarchyItemImpl implements Folder, Quota {


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
        String fullPath;
        String name = null;
        boolean root = path.equals("/");
        String pathFragment = decodeAndConvertToPath(path);
        String rootFolder = getRootFolder();
        fullPath = root ? rootFolder : FileUtils.getFile(rootFolder, pathFragment).getPath();
        File file = new java.io.File(fullPath);
        if (file.exists()) {
            name = root ? "ROOT" : FilenameUtils.getName(fullPath);
        }
        if (!file.isDirectory()) {
            return null;
        }

        long created = file.lastModified();
        long modified = file.lastModified();
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

        File fullPath = FileUtils.getFile(this.getFullPath(), name);
        if (!fullPath.exists()) {
            try {
                fullPath.createNewFile();
            } catch (IOException e) {
                throw new ServerException(e);
            }
            return FileImpl.getFile(getPath() + encode(name), getEngine());
        }
        return null;
    }

    /**
     * Creates new {@link FolderImpl} folder with the specified name in this folder.
     *
     * @param name Name of the folder to create.
     * @return Instance of newly created Folder.
     * @throws LockedException This folder was locked. Client did not provide the lock token.
     * @throws ServerException In case of an error.
     */
    @Override
    public Folder createFolder(String name) throws LockedException,
            ServerException {
        ensureHasToken();

        File fullPath = FileUtils.getFile(this.getFullPath(), name);
        if (!fullPath.exists()) {
            fullPath.mkdir();
        }
        return FolderImpl.getFolder(getPath() + encode(name), getEngine());
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
        String decodedPath = HierarchyItemImpl.decodeAndConvertToPath(getPath());
        File fullFolderPath = FileUtils.getFile(getRootFolder() + decodedPath);
        List<HierarchyItemImpl> children = new ArrayList<>();
        try {
            for (File p : fullFolderPath.listFiles()) {
                String childPath = getPath() + encode(p.getName());
                HierarchyItemImpl item = (HierarchyItemImpl) getEngine().getHierarchyItem(childPath);
                children.add(item);
            }
        } catch (Exception ex) {
            return new PageResults(children, null);
        }
        return new PageResults(children, null);
    }

    @Override
    public void delete() throws LockedException, MultistatusException,
            ServerException {
        ensureHasToken();
        try {
            FileUtils.deleteDirectory(new File(getFullPath()));
        } catch (IOException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public void copyTo(Folder folder, String destName, boolean deep)
            throws LockedException, MultistatusException, ServerException {
        ((FolderImpl) folder).ensureHasToken();

        String relUrl = HierarchyItemImpl.decodeAndConvertToPath(folder.getPath());
        File destinationFolder = FileUtils.getFile(getRootFolder(), relUrl);
        if (isRecursive(relUrl)) {
            throw new ServerException("Cannot copy to subfolder", WebDavStatus.FORBIDDEN);
        }
        if (!destinationFolder.exists() || !destinationFolder.isDirectory())
            throw new ServerException();
        try {
            String sourcePath = this.getFullPath();
            File destinationFullPath = FileUtils.getFile(destinationFolder, destName);
            FileUtils.copyDirectory(FileUtils.getFile(sourcePath), destinationFullPath);
        } catch (IOException e) {
            throw new ServerException(e);
        }
        setName(destName);
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

    @Override
    public void moveTo(Folder folder, String destName) throws LockedException,
            ConflictException, MultistatusException, ServerException {
        ensureHasToken();
        ((FolderImpl) folder).ensureHasToken();
        File destinationFolder = FileUtils.getFile(getRootFolder(), HierarchyItemImpl.decodeAndConvertToPath(folder.getPath()));
        if (!destinationFolder.exists() || !destinationFolder.isDirectory())
            throw new ConflictException();
        String sourcePath = this.getFullPath();
        File destinationFullPath = FileUtils.getFile(destinationFolder, destName);
        try {
            FileUtils.copyDirectory(FileUtils.getFile(sourcePath), destinationFullPath);
            delete();
        } catch (IOException e) {
            throw new ServerException(e);
        }
        setName(destName);
    }

    /**
     * Returns free bytes available to current user.
     *
     * @return Returns free bytes available to current user.
     */
    @Override
    public long getAvailableBytes() {
        return FileUtils.getFile(getFullPath()).getFreeSpace();
    }

    /**
     * Returns used bytes by current user.
     *
     * @return Returns used bytes by current user.
     */
    @Override
    public long getUsedBytes() {
        long total = FileUtils.getFile(getFullPath()).getTotalSpace();
        return total - getAvailableBytes();
    }
}

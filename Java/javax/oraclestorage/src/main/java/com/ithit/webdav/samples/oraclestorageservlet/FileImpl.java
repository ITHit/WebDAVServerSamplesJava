package com.ithit.webdav.samples.oraclestorageservlet;

import com.ithit.webdav.server.File;
import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.Lock;
import com.ithit.webdav.server.MimeType;
import com.ithit.webdav.server.exceptions.ConflictException;
import com.ithit.webdav.server.exceptions.LockedException;
import com.ithit.webdav.server.exceptions.MultistatusException;
import com.ithit.webdav.server.exceptions.ServerException;
import com.ithit.webdav.server.resumableupload.ResumableUpload;
import com.ithit.webdav.server.resumableupload.UploadProgress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Represents file in the Oracle DB repository.
 */
public class FileImpl extends HierarchyItemImpl implements File, Lock, ResumableUpload, UploadProgress {

    private final long lastChunkSaved;
    private final long totalContentLength;
    private String snippet;

    /**
     * Initializes a new instance of the {@link FileImpl} class.
     *
     * @param id                 Id of the item in DB.
     * @param parentId           Id of the parent item in DB.
     * @param name               Name of hierarchy item.
     * @param path               Relative to WebDAV root folder path.
     * @param created            Creation time of the hierarchy item.
     * @param modified           Modification time of the hierarchy item.
     * @param lastChunkSaved     Last byte saved.
     * @param totalContentLength Length of the file.
     * @param engine             Instance of current {@link WebDavEngine}
     */
    FileImpl(int id, int parentId, String name, String path, long created, long modified, long lastChunkSaved, long totalContentLength, WebDavEngine engine) {
        super(id, parentId, name, path, created, modified, engine);

        this.lastChunkSaved = lastChunkSaved;
        this.totalContentLength = totalContentLength;
    }

    /**
     * The date and time when the last chunk of file was saved in your storage.
     * <p>
     * Requested by the Engine during a call to {@link UploadProgress#getUploadProgress}.
     * </p>
     *
     * @return Time when last chunk of file was saved.
     * @throws ServerException in case of an error.
     */
    public long getLastChunkSaved() throws ServerException {
        return lastChunkSaved;
    }

    /**
     * Total file size that is being uploaded.
     * <p>
     * This value is passed to {@link FileImpl#write} method. Usually AJAX/HTML based clients will use value returned by this property to display upload progress.
     * </p>
     * <p>
     * Requested by the Engine during a call to {@link FileImpl#getUploadProgress}.
     * </p>
     *
     * @return Total file size in bytes.
     */
    public long getTotalContentLength() {
        return totalContentLength;
    }

    @Override
    public void delete() throws LockedException, MultistatusException, ServerException {
        deleteInternal(0);
    }

    @Override
    public void deleteInternal(int recursionDepth) throws LockedException, MultistatusException, ServerException {
        getParent().ensureHasToken();
        ensureHasToken();

        deleteThisItem();
        if (recursionDepth == 0) {
            getEngine().getWebSocketServer().notifyDeleted(getPath(), getWebSocketID());
        }
        try {
            getEngine().getSearchFacade().getIndexer().deleteIndex(this);
        } catch (Exception ex) {
            getEngine().getLogger().logError("Errors during indexing.", ex);
        }
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
    @Override
    protected HierarchyItemImpl createItemCopy(int id, int parentId, String name, String path,
                                               long created, long modified, WebDavEngine engine) {
        return new FileImpl(id, parentId, name, path, created, modified, lastChunkSaved, totalContentLength, engine);
    }

    /**
     * Gets the media type of the {@link FileImpl}.
     * <p>
     * Mime-type provided by this method is returned in a Content-Type header with GET request.
     * </p>
     * <p>
     * When deciding which action to perform when downloading a file some WebDAV clients and browsers
     * (such as Internet Explorer) rely on file extension, while others (such as Firefox) rely on Content-Type
     * header returned by server. For identical behavior in all browsers and WebDAV clients your server must
     * return a correct mime-type with a requested file.
     * </p>
     *
     * @return MIME type of the file.
     * @throws ServerException In case of an error.
     */
    public String getContentType() throws ServerException {
        String contentType = getDataAccess().executeScalar("SELECT ContentType FROM Repository WHERE ID = ?", id);

        if (contentType == null || contentType.length() == 0) {
            String name = this.getName();
            int periodIndex = name.lastIndexOf('.');
            String ext = name.substring(periodIndex + 1);
            contentType = MimeType.getInstance().getMimeType(ext);
            if (contentType == null)
                contentType = "application/octet-stream";
        }
        return contentType;
    }

    @Override
    public String getEtag() throws ServerException {
        return String.format("%s-%s", Long.hashCode(getModified()), getSerialNumber().intValueExact());
    }

    /**
     * Gets the size of the file content in bytes.
     *
     * @return Length of the file content in bytes.
     * @throws ServerException In case of an error.
     */
    public long getContentLength() throws ServerException {
        try {
            Blob blob = getDataAccess().executeScalar("SELECT Content FROM Repository WHERE ID = ?", id);
            return blob == null ? 0 : blob.length();
        } catch (SQLException ex) {
            throw new ServerException(ex);
        }
    }

    @Override
    public void copyTo(Folder folder, String destName, boolean deep)
            throws LockedException, MultistatusException, ServerException, ConflictException {
        copyToInternal(folder, destName, deep, 0);
    }

    @Override
    public void copyToInternal(Folder folder, String destName, boolean deep, int recursionDepth) throws LockedException, MultistatusException, ServerException, ConflictException {
        FolderImpl destFolder = getDataAccess().getFolderImpl(folder);

        destFolder.ensureHasToken();
        int newID = getId();
        HierarchyItemImpl copy;
        HierarchyItemImpl destItem = destFolder.findChild(destName);
        if (destItem != null) {
            if (destItem instanceof FolderImpl) {
                FolderImpl destItemF = (FolderImpl) destItem;
                destItemF.ensureHasTokenForTree();
                destItemF.removeTree();
                copy = copyThisItem(destFolder, null, destName);
            } else {
                destItem.ensureHasToken();
                copy = copyThisItem(null, destItem, destName);
            }
        } else {
            copy = copyThisItem(destFolder, null, destName);
        }
        if (recursionDepth == 0) {
            getEngine().getWebSocketServer().notifyCreated(folder.getPath() + getDataAccess().encode(destName), getWebSocketID());
        }
        try {
            if (copy != null) {
                newID = copy.getId();
            }
            getEngine().getSearchFacade().getIndexer().indexFile(destName, newID, null, this);
        } catch (Exception ex) {
            getEngine().getLogger().logError("Errors during indexing.", ex);
        }
    }

    /**
     * Writes the content of the file to the specified stream.
     * <p>
     * Client application can request only a part of a file specifying <b>Range</b> header.
     * Download managers may use this header to download single file using several threads at a time.
     * </p>
     *
     * @param output     Output stream.
     * @param startIndex Zero-bazed byte offset in file content at which to begin copying bytes to the output stream.
     * @param count      Number of bytes to be written to the output stream.
     * @throws ServerException In case of an error.
     */
    public void read(OutputStream output, long startIndex, long count) throws ServerException {
        try {
            Blob blob = getDataAccess().executeScalar("SELECT Content FROM Repository WHERE ID = ?", id);
            if (blob != null) {
                try (InputStream stream = blob.getBinaryStream()) {
                    int bufSize = 1048576; // 1Mb
                    byte[] buf = new byte[bufSize];
                    long retval;
                    stream.skip(startIndex);
                    while ((retval = stream.read(buf)) > 0) {
                        try {
                            if (retval > count) {
                                retval = count;
                            }
                            output.write(buf, 0, (int) retval);
                        } catch (IOException e) {
                            getEngine().getLogger().logDebug("Remote host closed connection");
                            return;
                        }

                        startIndex += retval;
                        count -= retval;
                    }
                }
            }
        } catch (SQLException | IOException e) {
            throw new ServerException(e);
        }
    }

    /**
     * Saves the content of the file from the specified stream to the Oracle repository.
     * <p>
     * If {@code totalContentLength} is -1 then <b>content</b> parameter
     * contains entire file content.  {@code startIndex} parameter
     * is always 0 in this case.
     * </p>
     * <p>
     * The Java WebDAV Server Engine can process two types of upload requests:
     * <ul>
     * <li> <b>PUT upload.</b> Files uploaded via PUT verb is performed by most RFC 4918 and RFC 2518 compliant WebDAV clients and modern Ajax clients.</li>
     * <li> <b>POST upload.</b> Files uploaded via POST verb is performed by legacy Ajax/HTML clients such as Internet Explorer 9.</li>
     * </ul>
     * </p>
     * <p>
     * To provide information about what segment of a file is being uploaded PUT request will contain optional <i>Content-Range: bytes XXX-XXX/XXX</i> header.
     * </p>
     * <p>
     * The following example demonstrates upload to WebDAV server using POST with multipart encoding by legacy web browser. The file will be created in /mydocs/ folder.
     * <pre>
     * &lt;html>
     *    &lt;head>&lt;title>POST Upload to WebDAV Server&lt;/title>&lt;/head>
     *     &lt;body>
     *         &lt;form action="/mydocs/" method="post" enctype="multipart/form-data">
     *             &lt;input type="file" name="dummyname" />&lt;br />
     *             &lt;input type="submit" />
     *         &lt;/form>
     *     &lt;/body>
     * &lt;/html>
     * </pre>
     * </p>
     *
     * @param content       {@link InputStream} to read the content of the file from.
     * @param contentType   Indicates media type of the file.
     * @param startIndex    Index in file to which corresponds first byte in {@code content}.
     * @param totalFileSize Total size of the file being uploaded. -1 if size is unknown.
     * @return Number of bytes written.
     * @throws LockedException File was locked and client did not provide lock token.
     * @throws ServerException In case of an error.
     * @throws IOException     I/O error.
     */
    public long write(InputStream content, String contentType, long startIndex, long totalFileSize)
            throws LockedException, ServerException, IOException {

        ensureHasToken();

        try {
            getDataAccess().executeUpdate("UPDATE Repository SET"
                            + " Modified = ?,"
                            + (startIndex == 0 ? " Content = EMPTY_BLOB()," : "")
                            + " ContentType = ?,"
                            + " LastChunkSaved = CURRENT_TIMESTAMP,"
                            + " TotalContentLength = ?,"
                            + " SerialNumber = SerialNumber + 1"
                            + " WHERE ID = ?",
                    new java.sql.Timestamp(new Date().getTime()),
                    contentType,
                    totalFileSize >= 0 ? totalFileSize : 0,
                    getId());
            OutputStream os = null;
            long totalSaved = 0;
            try {
                int read;
                long lastStartIndex = startIndex;
                long lastUpdateTime = new Date().getTime();
                int bufSize = 1048576; // 1Mb
                final long updateInterval = 1000;
                byte[] buf = new byte[bufSize];
                Blob bb = getDataAccess().executeScalar("select content from Repository where id = ? for update", getId());
                os = bb.setBinaryStream(startIndex + 1);
                while ((read = content.read(buf)) > 0) {
                    os.write(buf, 0, read);
                    startIndex += read;
                    //commit every megabate or every second so upload progress is visible
                    //and we don't lose more than 1MB if something happens.
                    if (startIndex - lastStartIndex > bufSize || (new Date().getTime() - lastUpdateTime) > updateInterval) {
                        os.close();
                        os = null;
                        getDataAccess().executeUpdate("UPDATE Repository SET LastChunkSaved = CURRENT_TIMESTAMP  WHERE ID = ?", getId());
                        getDataAccess().commit();
                        totalSaved += startIndex - lastStartIndex;
                        lastStartIndex = startIndex;
                        lastUpdateTime = new Date().getTime();
                        bb = getDataAccess().executeScalar("select content from Repository where id = ? for update", getId());
                        os = bb.setBinaryStream(startIndex + 1);
                    } else {
                        totalSaved += startIndex - lastStartIndex;
                    }
                }
                os.close();
                os = null;
                getDataAccess().commit();
            } finally {
                if (os != null)
                    os.close();
            }
            getEngine().getWebSocketServer().notifyUpdated(getPath(), getWebSocketID());
            try {
                getEngine().getSearchFacade().getIndexer().indexFile(getName(), getId(), getId(), this);
            } catch (Exception ex) {
                getEngine().getLogger().logError("Errors during indexing.", ex);
            }
            return totalSaved;
        } catch (SQLException ex) {
            throw new ServerException(ex);
        }
    }

    @Override
    public void moveTo(Folder folder, String destName)
            throws LockedException, ConflictException, MultistatusException, ServerException {
        moveToInternal(folder, destName, 0);
    }

    @Override
    public void moveToInternal(Folder folder, String destName, int recursionDepth) throws LockedException, ConflictException, MultistatusException, ServerException {
        FolderImpl destFolder = getDataAccess().getFolderImpl(folder);

        FolderImpl parent = getParent();

        ensureHasToken();
        destFolder.ensureHasToken();
        parent.ensureHasToken();

        HierarchyItemImpl destItem = destFolder.findChild(destName);
        if (destItem != null) {
            if (delWhenReplace() && destItem instanceof FolderImpl) {
                FolderImpl destItemF = (FolderImpl) destItem;
                destItemF.ensureHasTokenForTree();
                destItemF.removeTree();
                moveThisItem(destFolder, destName, parent);
            } else if (destItem instanceof File) {
                destItem.delete();
                moveThisItem(destFolder, destName, parent);
            } else
                throw new ConflictException();
        } else {
            moveThisItem(destFolder, destName, parent);
        }
        if (recursionDepth == 0) {
            getEngine().getWebSocketServer().notifyMoved(getPath(), folder.getPath() + getDataAccess().encode(destName), getWebSocketID());
        }
        try {
            getEngine().getSearchFacade().getIndexer().indexFile(destName, getId(), getId(), this);
        } catch (Exception ex) {
            getEngine().getLogger().logError("Errors during indexing.", ex);
        }
    }

    /**
     * In this method implementation you can delete partially uploaded file.
     * <p>
     * Often during long-continued upload you will keep the old file
     * content to be returned by GET requests and store the new file content in a
     * temporary file (or temporary field in database, etc).  To delete this partially
     * uploaded content client can submit CANCELUPLOAD command, the Engine will call this method in this case.
     * </p>
     * <p>
     * If the item was automatically checked-out by the Engine when upload started it will be automatically checked-in by the Engine after this call.
     * </p>
     * <p>
     * Example
     * Request:
     * <pre>
     * CANCELUPLOAD /LargeFile.doc HTTP/1.1
     * Host: http://server:8580/
     * </pre>
     * Response:
     * <pre>
     * HTTP/1.1 200 OK
     * </pre>
     * Response:
     * </p>
     *
     * @throws LockedException - This item or its parent was locked and client did not provide lock token.
     * @throws ServerException - In case of an error.
     */
    public void cancelUpload() throws LockedException, ServerException {
        ensureHasToken();
    }

    /**
     * Amount of bytes successfully saved to your storage.
     * <p>
     * Client will use value returned by this property to restore broken upload.
     * This value shall always reflect number of bytes already stored to persistent medium.
     * </p>
     * <p>
     * Requested by the Engine during a call to {@link UploadProgress#getUploadProgress}.
     * </p>
     *
     * @return Amount of bytes successfully saved.
     * @throws ServerException in case of an error.
     */
    public long getBytesUploaded() throws ServerException {
        return getContentLength();
    }

    /**
     * Indicates if item will be checked-in by the engine when last chunk of a file is uploaded
     * if item was checked in when upload started.
     *
     * @return true if item will be checked in when upload finishes.
     * @throws ServerException in case of an error.
     */
    public boolean getCheckInOnFileComplete() throws ServerException {
        throw new ServerException("Not implemented");
    }

    /**
     * Shall store value which indicates whether file will be checked in when upload finishes.
     *
     * @param value True if item will be checked in when upload finishes.
     * @throws ServerException in case of an error.
     */
    public void setCheckInOnFileComplete(boolean value) throws ServerException {
        throw new ServerException("Not implemented");
    }

    /**
     * Array of items that are being uploaded to this item subtree.
     * <p>
     * Engine calls {@link HierarchyItemImpl#getPath},
     * {@link ResumableUpload#getLastChunkSaved},
     * {@link ResumableUpload#getBytesUploaded},
     * {@link ResumableUpload#getTotalContentLength} and returns this information to
     * client.
     * </p>
     *
     * @return Return array with a single item if implemented on file items. Return all items that are being uploaded to this subtree if implemented on folder items.
     * @throws ServerException - in case of an error.
     */
    public List<? extends ResumableUpload> getUploadProgress() throws ServerException {
        return Collections.singletonList(this);
    }

    /**
     * Loads file content input stream for indexing.
     *
     * @param id File id.
     * @return InputStream for indexing.
     */
    InputStream getFileContentToIndex(int id) {
        Blob blob;
        try {
            blob = getDataAccess().executeScalar("SELECT Content FROM Repository WHERE ID = ?", id);
            return blob.getBinaryStream();
        } catch (Exception e) {
            getEngine().getLogger().logError("Cannot read content from DB.", e);
            return null;
        }
    }

    public String getSnippet() {
        return snippet;
    }

    void setSnippet(String snippet) {
        this.snippet = snippet;
    }
}

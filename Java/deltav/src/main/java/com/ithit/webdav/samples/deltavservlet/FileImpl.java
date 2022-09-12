package com.ithit.webdav.samples.deltavservlet;

import com.ithit.webdav.server.File;
import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.Lock;
import com.ithit.webdav.server.MimeType;
import com.ithit.webdav.server.deltav.AutoVersion;
import com.ithit.webdav.server.deltav.History;
import com.ithit.webdav.server.deltav.Version;
import com.ithit.webdav.server.deltav.VersionableItem;
import com.ithit.webdav.server.exceptions.*;
import com.ithit.webdav.server.resumableupload.ResumableUpload;
import com.ithit.webdav.server.resumableupload.UploadProgress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Represents file in the Oracle DB repository.
 */
public class FileImpl extends HierarchyItemImpl implements
        File,
        Lock,
        ResumableUpload,
        UploadProgress,
        VersionableItem {

    private long lastChunkSaved;
    private long totalContentLength;
    private AutoVersion autoVersion;
    private boolean versionControlled;
    private boolean checkedOut;
    private boolean checkInDuringUnlock;
    private boolean checkInOnFileComplete;
    private String snippet;

    /**
     * Initializes a new instance of the {@link FileImpl} class.
     *
     * @param id                    Id of the item in DB.
     * @param parentId              Id of the parent item in DB.
     * @param name                  Name of hierarchy item.
     * @param path                  Relative to WebDAV root folder path.
     * @param created               Creation time of the hierarchy item.
     * @param modified              Modification time of the hierarchy item.
     * @param lastChunkSaved        Last byte saved.
     * @param totalContentLength    Length of the file.
     * @param versionControlled     Whether file version controlled.
     * @param checkedOut            Determines whether item is in checked-in or checked-out state.
     * @param checkInDuringUnlock   Whether we need check in file during unlock.
     * @param checkInOnFileComplete Whether to checking on file complete.
     * @param autoVersion           Auto versioning rule.
     * @param engine                Instance of current {@link WebDavEngine}.
     */
    FileImpl(int id, int parentId, String name,
             String path, long created, long modified, long lastChunkSaved,
             long totalContentLength,
             boolean versionControlled, boolean checkedOut,
             boolean checkInDuringUnlock, boolean checkInOnFileComplete,
             AutoVersion autoVersion, WebDavEngine engine) {
        super(id, parentId, name, path, created, modified, engine);

        this.lastChunkSaved = lastChunkSaved;
        this.totalContentLength = totalContentLength;
        this.autoVersion = autoVersion;
        this.versionControlled = versionControlled;
        this.checkedOut = checkedOut;
        this.checkInDuringUnlock = checkInDuringUnlock;
        this.checkInOnFileComplete = checkInOnFileComplete;
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

        getParent().ensureHasToken();
        ensureHasToken();

        deleteThisItem();
        getEngine().getWebSocketServer().notifyDeleted(getPath());
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
     * @return newly created {@link HierarchyItemImpl}.
     */
    @Override
    protected HierarchyItemImpl createItemCopy(int id, int parentId, String name, String path,
                                               long created, long modified, WebDavEngine engine) {

        return new FileImpl(id, parentId, name, path, created, modified, lastChunkSaved,
                totalContentLength, versionControlled, checkedOut, checkInDuringUnlock,
                checkInOnFileComplete, autoVersion, engine);
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
        String contentType = getDbField("ContentType", null);

        if (contentType == null || contentType.length() == 0) {
            String name = this.getName();
            int periodIndex = name.lastIndexOf('.');
            String ext = name.substring(periodIndex + 1, name.length());
            contentType = MimeType.getInstance().getMimeType(ext);
            if (contentType == null)
                contentType = "application/octet-stream";
        }
        return contentType;
    }

    @Override
    public String getEtag() throws ServerException {
        BigDecimal sn = getSerialNumber();
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
            Blob blob = getDbField("Content", null);
            return blob == null ? 0 : blob.length();
        } catch (SQLException ex) {
            throw new ServerException(ex);
        }
    }

    @Override
    public void copyTo(Folder folder, String destName, boolean deep)
            throws LockedException, MultistatusException, ServerException {

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
        getEngine().getWebSocketServer().notifyCreated(folder.getPath() + destName);
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
            DownloadUtil.readBlob(getEngine().getLogger(), output, blob, startIndex, count);
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
                final long UPDATE_INTERVAL = 1000;
                byte[] buf = new byte[bufSize];
                Blob bb = getDataAccess().executeScalar(
                        "select content from Repository where id = ? for update", getId());
                os = bb.setBinaryStream(startIndex + 1);
                while ((read = content.read(buf)) > 0) {
                    os.write(buf, 0, read);
                    startIndex += read;
                    //commit every megabate or every second so upload progress is visible
                    //and we don't lose more than 1MB if something happens.
                    if (startIndex - lastStartIndex > bufSize || (new Date().getTime() - lastUpdateTime) > UPDATE_INTERVAL) {
                        os.close();
                        os = null;
                        getDataAccess().executeUpdate("UPDATE Repository SET LastChunkSaved = CURRENT_TIMESTAMP  WHERE ID = ?", getId());
                        getDataAccess().commit();
                        totalSaved += startIndex - lastStartIndex;
                        lastStartIndex = startIndex;
                        lastUpdateTime = new Date().getTime();
                        bb = getDataAccess().executeScalar(
                                "select content from Repository where id = ? for update", getId());
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
            getEngine().getWebSocketServer().notifyUpdated(getPath());
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
        getEngine().getWebSocketServer().notifyMoved(getPath(), folder.getPath() + destName);
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
     * @throws LockedException - this item or its parent was locked and client did not provide lock token.
     * @throws ServerException - in case of an error.
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
     * @return True if item will be checked in when upload finishes.
     * @throws ServerException in case of an error.
     */
    public boolean getCheckInOnFileComplete() throws ServerException {
        return checkInOnFileComplete;
    }

    /**
     * Shall store value which indicates whether file will be checked in when upload finishes.
     *
     * @param value true if item will be checked in when upload finishes.
     * @throws ServerException in case of an error.
     */
    public void setCheckInOnFileComplete(boolean value) throws ServerException {
        checkInOnFileComplete = value;
        setDbField("CheckinOnFileComplete", value ? "Y" : "N");
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
     * Current item version history. {@code Null}, if item is not under version control.
     * If item is under version control it always has at last one version in its versions list.
     *
     * @return Item implementing {@link History} interface or null if item is not under version control.
     * @throws ServerException In case of an error.
     */
    public VersionHistory getVersionHistory() throws ServerException {
        if (versionControlled)
            return new VersionHistory(getEngine(), getId(), getPath());

        return null;
    }

    /**
     * Creates new version. Copies all properties and content from this item.
     *
     * @return Url of the newly created version.
     * @throws ServerException In case of an error.
     */
    public String checkIn() throws ServerException, LockedException {
        ensureHasToken();

        String newVersionUrl;
        if (this.getVersionHistory() != null) {
            // Create new version. Copy content and properties from this item to new version.

            VersionImpl version = this.getVersionHistory().getCurrentVersion();
            assert (version != null);

            newVersionUrl = createNewVersion(version.getVersionNumber() + 1);

            setDbField("ChangeNotes", "");
        } else {
            newVersionUrl = getPath();
        }

        setResourceCheckedOut(false);

        return newVersionUrl;
    }

    /**
     * Creates new version of the file in the DB.
     *
     * @param newVersionNumber New version number.
     * @return Path to the new version.
     * @throws ServerException in case of DB errors.
     */
    private String createNewVersion(int newVersionNumber) throws ServerException {
        String newVersionPath = VersionImpl.createVersionPath(this.getPath(), newVersionNumber);

        int newId = genVersionId();
        // Create new version.
        String commandText =
                " INSERT INTO Version"
                        + " (Id, ItemId, VersionNumber, Name, ChangeNotes,"
                        + " CreatorDisplayName, Content, ContentType, Created)"
                        + " SELECT ?, ?, ?, Name, ChangeNotes, "
                        + " NVL(?, CreatorDisplayName), Content, ContentType, ?"
                        + " FROM Repository"
                        + " WHERE Id = ?";

        getDataAccess().executeUpdate(commandText, newId, getId(), newVersionNumber,
                getUserName(),
                new Timestamp(new Date().getTime()),
                getId());

        String versionPropertyCommand =
                "INSERT INTO VersionProperty"
                        + " (VersionId, Name, Namespace, PropVal)"
                        + " SELECT ?, Name, Namespace, PropVal"
                        + " FROM Property"
                        + " WHERE ItemID = ?";

        getDataAccess().executeUpdate(versionPropertyCommand, newId, getId());
        return newVersionPath;
    }

    /**
     * Updates CheckedOut filed in the Repository table to the specified value.
     *
     * @param value Value to set.
     * @throws ServerException in case of DB errors.
     */
    private void setResourceCheckedOut(boolean value) throws ServerException {
        checkedOut = value;
        setDbField("CheckedOut", value ? "Y" : "N");
    }

    /**
     * Allow modifications to the content and properties of this version-controlled item.
     *
     * @throws ServerException in case of an error.
     */
    public void checkOut() throws ServerException, LockedException {
        ensureHasToken();

        setResourceCheckedOut(true);
    }

    /**
     * Cancels the checkout and restores the pre-checkout state of the version-controlled item.
     *
     * @throws ServerException in case of an error.
     */
    public void unCheckOut() throws ServerException, LockedException {
        ensureHasToken();

        if (this.getVersionHistory() != null) {
            // Discard changes.
            // Copy content and properties from current version to this item. Mark item as checked in.
            VersionImpl version = this.getVersionHistory().getCurrentVersion();

            // Restore properties.

            getDataAccess().executeUpdate("DELETE FROM Property WHERE ItemID = ?", getId());

            String versionPropertyCommand =
                    "INSERT INTO VersionProperty"
                            + " (VersionId, Name, Namespace, PropVal)"
                            + " SELECT ?, Name, Namespace, PropVal"
                            + " FROM Property"
                            + " WHERE ItemID = ?";

            getDataAccess().executeUpdate(versionPropertyCommand, version.getVersionId(), version.getItemId());

            // Restore content.
            getDataAccess().executeUpdate(
                    "update repository set content = (Select content from version where id = ?) where id = ?",
                    version.getVersionId(), getId());
        }
        // Mark item as checked in.
        setResourceCheckedOut(false);
    }

    /**
     * Updates content and properties of the item to those identified by version parameter.
     *
     * @param version Version that contains reference properties and content.
     * @throws ServerException in case of an error.
     */
    public void updateToVersion(Version version) throws ServerException, LockedException {
        ensureHasToken();

        VersionImpl v = (VersionImpl) version;

        if (v.getItemId() != this.getId())
            throw new ServerException(WebDavStatus.CONFLICT);

        getDataAccess().executeUpdate(
                "UPDATE Repository SET Content = (SELECT Content FROM Version WHERE ID = ?) WHERE ID = ?",
                v.getVersionId(), getId());

        // Copy properties to this item
        getDataAccess().executeUpdate("DELETE FROM Property WHERE ItemID = ?", getId());

        String commandText = "INSERT INTO Property"
                + " (ItemId, Name, Namespace, PropVal)"
                + " SELECT ?, Name, Namespace, PropVal"
                + " FROM VersionProperty"
                + " WHERE VersionId = ?";

        getDataAccess().executeUpdate(commandText, getId(), v.getVersionId());

        VersionImpl currVersion = getVersionHistory().getCurrentVersion();
        createNewVersion(currVersion.getVersionNumber() + 1);
    }

    /**
     * Determines how checked-in item responds to WebDAV client attempts to modify its content or properties.
     *
     * @return One of {@link AutoVersion} enum values.
     * @throws ServerException in case of an error.
     */
    public AutoVersion getAutoVersion() throws ServerException {
        return autoVersion;
    }

    /**
     * Sets auth versioning mode for this item.
     *
     * @param value Auto versioning mode.
     * @throws ServerException in case of an error.
     */
    public void setAutoVersion(AutoVersion value) throws ServerException {
        autoVersion = value;
        setDbField("AutoVersion", value.toString());
    }

    /**
     * Determines whether item is in checked-in or checked-out state.
     *
     * @return Boolean value indicating if item is in checked-out state.
     * @throws ServerException in case of an error.
     */
    public boolean isCheckedOut() throws ServerException {
        return checkedOut;
    }

    /**
     * Puts or removes current item from version control.
     *
     * @param enable True to enable version control, false - to disable.
     * @throws ServerException in case of an error.
     */
    public void putUnderVersionControl(boolean enable) throws ServerException {
        if (enable && this.getVersionHistory() == null) {
            // Create new version. The content and properties of the new version is being copied from this item.
            this.setAutoVersion(getEngine().getAutoVersionMode());

            createNewVersion(1);

            setResourceCheckedOut(false);
        } else if (!enable) {
            getDataAccess().executeUpdate("DELETE FROM VersionProperty WHERE VersionId  IN (SELECT ID FROM Version WHERE ItemId= ?)", getId());
            getDataAccess().executeUpdate("DELETE FROM Version WHERE ItemId = ?", getId());
        }

        versionControlled = enable;
        setDbField("VersionControlled", enable ? "Y" : "N");
    }

    /**
     * Indicates if item will be checked-in by the engine during the unlock request.
     * Before checking-out the engine sets this property. When item is being unlocked engine reads this property and calls {@link VersionableItem#checkIn} if necessary. This property is required for auto-versioning.
     *
     * @return If item will be checked-in by the engine during the unlock request.
     * @throws ServerException in case of an error.
     */
    public boolean getAutoCheckIn() throws ServerException {
        return checkInDuringUnlock;
    }

    /**
     * Sets flag which indicates if item will be checked-in by the engine during the unlock request.
     * Before checking-out the engine sets this property. When item is being unlocked engine reads this property and calls {@link VersionableItem#checkIn} if necessary. This property is required for auto-versioning.
     *
     * @param value Value of the flag.
     * @throws ServerException in case of an error.
     */
    public void setAutoCheckIn(boolean value) throws ServerException {
        checkInDuringUnlock = value;
        setDbField("CheckinDuringUnlock", value ? "Y" : "N");
    }

    /**
     * Retrieves brief comment about a resource that is suitable for presentation to a user.
     *
     * @return Comment string.
     * @throws ServerException in case of failure.
     */
    public String getComment() throws ServerException {
        return getDbField("ChangeNotes", null);
    }

    /**
     * Sets brief comment about a resource that is suitable for presentation to a user.
     * <p>
     * This property can be used to indicate why that version was created.
     * </p>
     *
     * @param comment Comment string.
     * @throws ServerException in case of failure.
     */
    public void setComment(String comment) throws ServerException {
        setDbField("ChangeNotes", comment);
    }

    /**
     * Retrieves display name of the user that created this item.
     *
     * @return User name.
     * @throws ServerException in case of error.
     */
    public String getCreatorDisplayName() throws ServerException {
        return getDbField("CreatorDisplayName", null);
    }

    /**
     * Sets display name of the user that created this item.
     * <p>
     * This should be a description of the creator of the resource that is
     * suitable for presentation to a user. Can be used to indicate who created that version.
     * </p>
     *
     * @param name User name.
     * @throws ServerException in case of error.
     */
    public void setCreatorDisplayName(String name) throws ServerException {
        setDbField("CreatorDisplayName", name);
    }

    /**
     * Loads file content input stream for indexing.
     *
     * @param id File id
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

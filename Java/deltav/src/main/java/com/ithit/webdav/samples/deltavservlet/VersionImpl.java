package com.ithit.webdav.samples.deltavservlet;

import com.ithit.webdav.server.File;
import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.Property;
import com.ithit.webdav.server.deltav.DeltaVItem;
import com.ithit.webdav.server.deltav.Version;
import com.ithit.webdav.server.deltav.VersionableItem;
import com.ithit.webdav.server.exceptions.LockedException;
import com.ithit.webdav.server.exceptions.MultistatusException;
import com.ithit.webdav.server.exceptions.ServerException;
import com.ithit.webdav.server.exceptions.WebDavStatus;
import com.ithit.webdav.server.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents single item version.
 * <p>
 * Defines the properties and methods that item version must implement. In addition to methods and properties provided by
 * {@link DeltaVItem} this interface also provides methods for getting version name, next version and previous version.
 * </p>
 * <p>
 * Usually you will implement <b>Version</b> interface for your resource version objects together with {@link com.ithit.webdav.server.File} interface.
 * While <b>Resource</b> interface is optional for resource versions it may be useful if your DeltaV client application will request
 * content of the resource version. In this case {@link com.ithit.webdav.server.File#read}, {@link com.ithit.webdav.server.File#getContentLength} and {@link com.ithit.webdav.server.File#getContentType} members of the {@link com.ithit.webdav.server.File} interface will be requested by the engine.
 * Copying, moving, updating properties and content are not allowed for a version, your {@link com.ithit.webdav.server.HierarchyItem#copyTo},
 * {@link com.ithit.webdav.server.HierarchyItem#moveTo}, {@link com.ithit.webdav.server.HierarchyItem#updateProperties} and {@link com.ithit.webdav.server.File#write}
 * implementations must throw {@link ServerException} with status {@link com.ithit.webdav.server.exceptions.WebDavStatus#NOT_ALLOWED}
 * </p>
 */
class VersionImpl implements Version, File {
    private final int versionId;
    private final int itemId;
    private final int versionNumber;
    private final WebDavEngine engine;
    private final String itemPath;
    private final String path;
    private final long created;

    /**
     * Initializes new instance of the {@link VersionImpl}.
     *
     * @param engine    Instance of current {@link WebDavEngine}.
     * @param versionId Version id of the item in DB.
     * @param itemId    Id of the item in DB.
     * @param itemPath  Relative to WebDAV root folder path.
     * @param verNumber Number of the version item.
     * @param created   Creation time of the hierarchy item.
     */
    VersionImpl(WebDavEngine engine, int versionId, int itemId, String itemPath,
                int verNumber, long created) {
        this.versionId = versionId;
        this.versionNumber = verNumber;

        this.engine = engine;
        this.itemId = itemId;
        this.path = createVersionPath(itemPath, versionNumber);
        this.itemPath = itemPath;
        this.created = created;
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
     * @param itemPath      Relative to WebDAV root folder path.
     * @param versionNumber Number of the version item.
     * @return Item path relative to storage root.
     */
    static String createVersionPath(String itemPath, int versionNumber) {
        return itemPath + "?version=" + versionNumber;
    }

    /**
     * Next version or null if no next version exists.
     *
     * @return Version item representing next version in the list of versions or null if no next version exists.
     * @throws ServerException in case of an error.
     */
    public Version getSuccessor() throws ServerException {

        String commandText =
                "SELECT Id, VersionNumber, Name, Created"
                        + " FROM Version"
                        + " WHERE (ItemId = ?) AND (VersionNumber ="
                        + " (SELECT MIN(VersionNumber)"
                        + "   FROM Version"
                        + "   WHERE (ItemId = ?)"
                        + "     AND (VersionNumber > ?)))";

        List<VersionImpl> versions = engine.getDataAccess().readVersions(
                commandText, itemPath, itemId, itemId, versionNumber);

        return versions.size() > 0 ? versions.get(0) : null;
    }

    /**
     * Previous version or null if no previous version exists.
     *
     * @return Version item representing previous version in the list of versions or null if no previous version exists.
     * @throws ServerException in case of an error.
     */
    public Version getPredecessor() throws ServerException {
        String commandText =
                "SELECT Id, VersionNumber, Name, Created"
                        + " FROM Version"
                        + " WHERE (ItemId = ?) AND (VersionNumber ="
                        + " (SELECT MAX(VersionNumber)"
                        + "   FROM Version"
                        + "   WHERE (ItemId = ?)"
                        + "     AND (VersionNumber < ?)))";

        List<VersionImpl> versions = engine.getDataAccess().readVersions(
                commandText, itemPath, itemId, itemId, versionNumber);

        return versions.size() > 0 ? versions.get(0) : null;
    }

    /**
     * Hierarchy item for this version.
     *
     * @return Hierarchy item for this version.
     * @throws ServerException in case of an error.
     */
    public VersionableItem getVersionableItem() throws ServerException {
        String itemPath = path.substring(0, path.indexOf('?'));
        return (VersionableItem) engine.getHierarchyItem(itemPath);
    }

    /**
     * Name of the version.
     * <p>
     * Must be unique among version items for a given hierarchy item. This string is intended for display
     * for a user.
     * </p>
     *
     * @return Name of the version.
     */
    public String getVersionName() {
        return "V" + this.versionNumber;
    }

    /**
     * Retrieves brief comment about a resource that is suitable for presentation to a user.
     *
     * @return comment string.
     * @throws ServerException in case of failure.
     */
    public String getComment() throws ServerException {
        return getDbField("ChangeNotes", "");
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
        return getDbField("CreatorDisplayName", "");
    }

    /**
     * Sets display name of the user that created this item.
     * <p>
     * This should be a description of the creator of the resource that is
     * suitable for presentation to a user. Can be used to indicate who created that version.
     * </p>
     *
     * @param value User name.
     * @throws ServerException in case of error.
     */
    public void setCreatorDisplayName(String value) throws ServerException {
        setDbField("CreatorDisplayName", value);
    }

    /**
     * Gets the media type of the file.
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
        return getDbField("ContentType", "");
    }

    @Override
    public String getEtag() throws ServerException {
        return String.format("%s", getModified());
    }

    /**
     * Gets the size of the file content in bytes.
     *
     * @return Length of the file content in bytes.
     * @throws ServerException In case of an error.
     */
    public long getContentLength() throws ServerException {
        try {
            Blob blob = engine.getDataAccess().executeScalar("SELECT Content FROM Version WHERE ID = ?", versionId);
            return blob == null ? 0 : blob.length();
        } catch (SQLException ex) {
            throw new ServerException(ex);
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
            Blob blob = engine.getDataAccess().executeScalar("SELECT Content FROM version WHERE ID = ?", versionId);

            DownloadUtil.readBlob(engine.getLogger(), output, blob, startIndex, count);
        } catch (SQLException | IOException e) {
            throw new ServerException(e);
        }
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
        return this.path;
    }

    /**
     * Returns current version number.
     *
     * @return Current version number.
     */
    int getVersionNumber() {
        return this.versionNumber;
    }

    /**
     * Returns unique item id.
     *
     * @return Unique item id.
     */
    int getItemId() {
        return this.itemId;
    }

    /**
     * Returns current version id.
     *
     * @return Current version id.
     */
    int getVersionId() {
        return versionId;
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
     */
    public long getCreated() {
        return created;
    }

    /**
     * Gets the last modification date of the item in repository expressed as the coordinated universal time (UTC).
     *
     * @return Modification date of the item.
     * @throws ServerException In case of an error.
     */
    public long getModified() throws ServerException {
        return getCreated();
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

        String commandText = "SELECT Name, Namespace, PropVal"
                + " FROM VersionProperty"
                + " WHERE VersionId = ?";

        List<Property> l = engine.getDataAccess().readProperties(commandText, versionId);

        if (props == null)
            return l;

        List<Property> result = new ArrayList<>();

        for (Property lookForProp : props) {
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
        return engine.getDataAccess().readProperties("SELECT Name, Namespace, '' as PropVal"
                + " FROM VersionProperty"
                + " WHERE VersionId = ?", versionId);
    }

    /**
     * Deletes this item.
     *
     * @throws ServerException - in case of another error.
     */
    public void delete() throws ServerException {
        engine.getDataAccess().executeUpdate("DELETE FROM Version WHERE Id = ?", versionId);
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

    /**
     * Modifies and removes properties for this item.
     *
     * @param setProps Array of properties to be set.
     * @param delProps Array of properties to be removed. {@link Property#value} field is ignored.
     *                 Specifying the removal of a property that does not exist is not an error.
     * @throws ServerException In case of other error.
     */
    public void updateProperties(Property[] setProps, Property[] delProps) throws ServerException {
        throw new ServerException(WebDavStatus.NOT_ALLOWED);
    }

    /**
     * Saves the content of the file from the specified stream to the WebDAV repository.
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
        throw new ServerException(WebDavStatus.NOT_ALLOWED);
    }

    /**
     * Get DB field value with specified name.
     *
     * @param columnName   Column name to get data.
     * @param defaultValue Default value if data is null for this field.
     * @return Field value or default value of null.
     * @throws ServerException in case of DB errors.
     */
    private <T> T getDbField(String columnName, T defaultValue) throws ServerException {
        String commandText = String.format("SELECT %1s FROM Version WHERE Id = ?", columnName);
        T res = (T) engine.getDataAccess().executeScalar(commandText, getVersionId());

        return res != null ? res : defaultValue;
    }

    /**
     * Set DB field with specified name with new value.
     *
     * @param columnName Column name to set data.
     * @param value      New value to set.
     * @throws ServerException in case of DB errors.
     */
    private <T> void setDbField(String columnName, T value) throws ServerException {
        String commandText = String.format("UPDATE Version SET %1s = ? WHERE Id = ?", columnName);
        engine.getDataAccess().executeUpdate(commandText, value, getVersionId());
    }
}
package com.ithit.webdav.samples.springboots3.impl;

import com.ithit.webdav.server.*;
import com.ithit.webdav.server.exceptions.ConflictException;
import com.ithit.webdav.server.exceptions.LockedException;
import com.ithit.webdav.server.exceptions.MultistatusException;
import com.ithit.webdav.server.exceptions.ServerException;
import com.ithit.webdav.server.resumableupload.ResumableUpload;
import com.ithit.webdav.server.resumableupload.UploadProgress;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents file in the File System repository.
 */
public final class FileImpl extends HierarchyItemImpl implements File, Lock,
        ResumableUpload, UploadProgress {

    private static final int BUFFER_SIZE = 1048576; // 1 Mb
    private final long contentLength;

    /**
     * Initializes a new instance of the {@link FileImpl} class.
     *
     * @param name              Name of hierarchy item.
     * @param path              Relative to WebDAV root folder path.
     * @param created           Creation time of the hierarchy item.
     * @param modified          Modification time of the hierarchy item.
     * @param engine            Instance of current {@link WebDavEngine}.
     */
    private FileImpl(String name, String path, long created, long modified, long contentLength, WebDavEngine engine) {
        super(name, path, created, modified, engine);
        this.contentLength = contentLength;
    }

    /**
     * Returns file that corresponds to path.
     *
     * @param path   Encoded path relative to WebDAV root.
     * @param engine Instance of {@link WebDavEngine}
     * @return File instance or null if physical file not found in file system.
     */
    public static FileImpl getFile(String path, String name, long created, long modified, long contentLength, WebDavEngine engine) {
        return new FileImpl(name, path, created, modified, contentLength, engine);
    }

    /**
     * Array of items that are being uploaded to this item subtree.
     *
     * @return Return array with a single item if implemented on file items. Return all items that are being uploaded to this subtree if implemented on folder items.
     * @throws ServerException - in case of an error.
     */
    @Override
    public List<? extends ResumableUpload> getUploadProgress()
            throws ServerException {
        return Collections.singletonList(this);
    }

    /**
     * In this method implementation you can delete partially uploaded file.
     * <p>
     * Client do not plan to restore upload. Remove any temporary files / cleanup resources here.
     *
     * @throws LockedException - this item or its parent was locked and client did not provide lock token.
     * @throws ServerException - in case of an error.
     */
    @Override
    public void cancelUpload() throws LockedException, ServerException {
        ensureHasToken();
    }

    /**
     * Amount of bytes successfully saved to your storage.
     *
     * @return Amount of bytes successfully saved.
     * @throws ServerException in case of an error.
     */
    @Override
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
    @Override
    public boolean getCheckInOnFileComplete() throws ServerException {
        return false;
    }

    /**
     * Shall store value which indicates whether file will be checked in when upload finishes.
     *
     * @param value True if item will be checked in when upload finishes.
     * @throws ServerException in case of an error.
     */
    @Override
    public void setCheckInOnFileComplete(boolean value) throws ServerException {
        throw new ServerException("Not implemented");
    }

    /**
     * The date and time when the last chunk of file was saved in your storage.
     *
     * @return Time when last chunk of file was saved.
     * @throws ServerException in case of an error.
     */
    @Override
    public long getLastChunkSaved() throws ServerException {
        return getModified();
    }

    /**
     * Total file size that is being uploaded.
     *
     * @return Total file size in bytes.
     * @throws ServerException in case of an error.
     */
    @Override
    public long getTotalContentLength() throws ServerException {
        return getContentLength();
    }

    /**
     * Gets the size of the file content in bytes.
     *
     * @return Length of the file content in bytes.
     * @throws ServerException In case of an error.
     */
    @Override
    public long getContentLength() throws ServerException {
        return contentLength;
    }

    /**
     * Gets the media type of the {@link FileImpl}.
     *
     * @return MIME type of the file.
     */
    @Override
    public String getContentType() {
        String name = this.getName();
        int periodIndex = name.lastIndexOf('.');
        String ext = name.substring(periodIndex + 1);
        String contentType = MimeType.getInstance().getMimeType(ext);
        if (contentType == null)
            contentType = "application/octet-stream";
        return contentType;
    }

    @Override
    public String getEtag() throws ServerException {
        return String.format("%s-%s", Long.hashCode(getModified()), getSerialNumber());
    }

    /**
     * Writes the content of the file to the specified stream.
     *
     * @param out        Output stream.
     * @param startIndex Zero-based byte offset in file content at which to begin copying bytes to the output stream.
     * @param count      Number of bytes to be written to the output stream.
     * @throws ServerException In case of an error.
     */
    @Override
    public void read(OutputStream out, long startIndex, long count) throws ServerException {
        byte[] buf = new byte[BUFFER_SIZE];
        int retVal;
        try (InputStream in = getEngine().getDataClient().getObject(getPath())) {
            in.skip(startIndex);
            while ((retVal = in.read(buf)) > 0) {
                // Strict servlet API doesn't allow to write more bytes then content length. So we do this trick.
                if (retVal > count) {
                    retVal = (int) count;
                }
                out.write(buf, 0, retVal);
                startIndex += retVal;
                count -= retVal;
            }
        } catch (IOException x) {
            throw new ServerException(x);
        }
    }

    /**
     * Saves the content of the file from the specified stream to the File System repository.
     *
     * @param content         {@link InputStream} to read the content of the file from.
     * @param contentType     Indicates media type of the file.
     * @param startIndex      Index in file to which corresponds first byte in {@code content}.
     * @param totalFileLength Total size of the file being uploaded. -1 if size is unknown.
     * @return Number of bytes written.
     * @throws LockedException File was locked and client did not provide lock token.
     * @throws ServerException In case of an error.
     * @throws IOException     I/O error.
     */
    @Override
    public long write(InputStream content, String contentType, long startIndex, long totalFileLength)
            throws LockedException, ServerException, IOException {
        ensureHasToken();
        incrementSerialNumber();
        getEngine().getDataClient().storeObject(getPath(), content, contentType, totalFileLength);
        getEngine().getWebSocketServer().notifyUpdated(getPath(), getWebSocketID());
        return totalFileLength;
    }

    private void incrementSerialNumber() {
        try {
            Property serialNumber = Property.create("", "SerialNumber", "1");
            String sn = getSerialNumber();
            if (!Objects.equals(sn, "0")) {
                serialNumber.setValue(String.valueOf((Integer.parseInt(sn) + 1)));
            }
            getEngine().getDataClient().setMetadata(getPath(), "SerialNumber", SerializationUtils.serialize(Collections.singletonList(serialNumber)));
        } catch (Exception ex) {
            getEngine().getLogger().logError("Cannot update serial number.", ex);
        }
    }

    private String getSerialNumber() throws ServerException {
        String serialJson = getEngine().getDataClient().getMetadata(getPath(), "SerialNumber");
        List<Property> properties = SerializationUtils.deserializeList(Property.class, serialJson);
        if (properties.size() == 1) {
            return properties.get(0).getXmlValueRaw();
        }
        return "0";
    }


    @Override
    public void delete() throws LockedException, MultistatusException, ServerException {
        deleteInternal(0);
    }

    @Override
    public void deleteInternal(int recursionDepth) throws LockedException, MultistatusException, ServerException {
        ensureHasToken();
        try {
            getEngine().getDataClient().delete(getPath());
        } catch (SdkException e) {
            getEngine().getLogger().logError("Tried to delete file in use.", e);
            throw new ServerException(e);
        }
        if (recursionDepth == 0) {
            getEngine().getWebSocketServer().notifyDeleted(getPath(), getWebSocketID());
        }
    }

    @Override
    public void copyTo(Folder folder, String destName, boolean deep)
            throws LockedException, MultistatusException, ServerException, ConflictException {
        copyToInternal(folder, destName, deep, 0);
    }

    @Override
    public void copyToInternal(Folder folder, String destName, boolean deep, int recursionDepth) throws LockedException, MultistatusException, ServerException, ConflictException {
        ((FolderImpl) folder).ensureHasToken();
        final HierarchyItem item = getEngine().getDataClient().locateObject(folder.getPath(), getEngine());
        if (item == null) {
            throw new ConflictException();
        }
        String destPath = folder.getPath() + getEngine().getDataClient().encode(destName);
        try {
            getEngine().getDataClient().copy(getPath(), destPath);
        } catch (SdkException e) {
            throw new ServerException(e);
        }
        // Locks should not be copied, delete them
        getEngine().getDataClient().setMetadata(destPath, ACTIVE_LOCKS_ATTRIBUTE, null);
        if (recursionDepth == 0) {
            getEngine().getWebSocketServer().notifyCreated(destPath, getWebSocketID());
        }
    }

    @Override
    public void moveTo(Folder folder, String destName) throws LockedException,
            ConflictException, MultistatusException, ServerException {
        moveToInternal(folder, destName, 0);
    }

    @Override
    public void moveToInternal(Folder folder, String destName, int recursionDepth) throws LockedException, ConflictException, MultistatusException, ServerException {
        ensureHasToken();
        ((FolderImpl) folder).ensureHasToken();
        final HierarchyItem item = getEngine().getDataClient().locateObject(folder.getPath(), getEngine());
        if (item == null) {
            throw new ConflictException();
        }
        String destPath = folder.getPath() + destName;
        try {
            getEngine().getDataClient().copy(getPath(), destPath);
            getEngine().getDataClient().delete(getPath());
        } catch (SdkException e) {
            throw new ServerException(e);
        }
        setName(destName);
        // Locks should not be copied, delete them
        getEngine().getDataClient().setMetadata(destPath, ACTIVE_LOCKS_ATTRIBUTE, null);
        if (recursionDepth == 0) {
            getEngine().getWebSocketServer().notifyMoved(getPath(), getEngine().getDataClient().encode(destPath), getWebSocketID());
        }
    }
}

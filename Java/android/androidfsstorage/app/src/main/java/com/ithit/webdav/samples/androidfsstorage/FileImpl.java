package com.ithit.webdav.samples.androidfsstorage;

import com.ithit.webdav.samples.androidfsstorage.extendedattributes.ExtendedAttributesExtension;
import com.ithit.webdav.server.File;
import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.Lock;
import com.ithit.webdav.server.MimeType;
import com.ithit.webdav.server.Property;
import com.ithit.webdav.server.exceptions.ConflictException;
import com.ithit.webdav.server.exceptions.LockedException;
import com.ithit.webdav.server.exceptions.MultistatusException;
import com.ithit.webdav.server.exceptions.ServerException;
import com.ithit.webdav.server.resumableupload.ResumableUpload;
import com.ithit.webdav.server.resumableupload.UploadProgress;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents file in the File System repository.
 */
class FileImpl extends HierarchyItemImpl implements File, Lock,
        ResumableUpload, UploadProgress {

    private int bufferSize = 1048576; // 1 Mb

    /**
     * Initializes a new instance of the {@link FileImpl} class.
     *
     * @param name     Name of hierarchy item.
     * @param path     Relative to WebDAV root folder path.
     * @param created  Creation time of the hierarchy item.
     * @param modified Modification time of the hierarchy item.
     * @param engine   Instance of current {@link WebDavEngine}.
     */
    private FileImpl(String name, String path, long created, long modified, WebDavEngine engine) {
        super(name, path, created, modified, engine);
    }

    /**
     * Returns file that corresponds to path.
     *
     * @param path   Encoded path relative to WebDAV root.
     * @param engine Instance of {@link WebDavEngine}
     * @return File instance or null if physical file not found in file system.
     * @throws ServerException in case of exception
     */
    static FileImpl getFile(String path, WebDavEngine engine) throws ServerException {
        java.io.File fullPath;
        String name;
        String pathFragment = decodeAndConvertToPath(path);
        String rootFolder = getRootFolder();
        fullPath = FileUtils.getFile(rootFolder, pathFragment);
        if (fullPath.exists() && !fullPath.isDirectory()) {
            name = fullPath.getName();
        } else {
            return null;
        }
        long created = fullPath.lastModified();
        long modified = fullPath.lastModified();
        return new FileImpl(name, path, created, modified, engine);
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
     *
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
        java.io.File fullPath = FileUtils.getFile(getFullPath());
        long size = 0;
        if (fullPath.exists()) {
            size = FileUtils.sizeOf(fullPath);
        }
        return size;
    }

    /**
     * Gets the media type of the {@link FileImpl}.
     *
     * @return MIME type of the file.
     * @throws ServerException In case of an error.
     */
    @Override
    public String getContentType() throws ServerException {
        String name = this.getName();
        int periodIndex = name.lastIndexOf('.');
        String ext = name.substring(periodIndex + 1, name.length());
        String contentType = MimeType.getInstance().getMimeType(ext);
        if (contentType == null)
            contentType = "application/octet-stream";
        return contentType;
    }

    @Override
    public String getEtag() throws ServerException {
        return null;//String.format("%s-%s", getModified(), getSerialNumber());
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
        java.io.File fullPath = FileUtils.getFile(this.getFullPath());
        byte[] buf = new byte[bufferSize];
        int retVal;
        InputStream in = null;
        try {
            in = new FileInputStream(fullPath);
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
        } finally {
            IOUtils.closeQuietly(in);
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
        java.io.File fullPath = FileUtils.getFile(getFullPath());
        if (!fullPath.exists()) {
            fullPath.createNewFile();
        } else {
            if (startIndex == 0 && !fullPath.isDirectory()) {
                FileUtils.deleteQuietly(fullPath);
            }
        }
        FileOutputStream fos = new FileOutputStream(fullPath);
        try {
            incrementSerialNumber();
            return IOUtils.copyLarge(content, fos, startIndex, totalFileLength, new byte[bufferSize]);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    private void incrementSerialNumber() {
        try {
            Property serialNumber = new Property("", "SerialNumber", "1");
            String sn = getSerialNumber();
            if (!Objects.equals(sn, "0")) {
                serialNumber.setValue(String.valueOf((Integer.valueOf(sn) + 1)));
            }
            ExtendedAttributesExtension.setExtendedAttribute(getFullPath(), "SerialNumber", SerializationUtils.serialize(Collections.singletonList(serialNumber)));
        } catch (Exception ex) {
            getEngine().getLogger().logError("Cannot update serial number.", ex);
        }
    }

    private String getSerialNumber() throws ServerException {
        String serialJson = ExtendedAttributesExtension.getExtendedAttribute(getFullPath(), "SerialNumber");
        List<Property> properties = SerializationUtils.deserializeList(Property.class, serialJson);
        if (properties.size() == 1) {
            return properties.get(0).getValue();
        }
        return "0";
    }


    @Override
    public void delete() throws LockedException, MultistatusException, ServerException {
        ensureHasToken();
        FileUtils.deleteQuietly(FileUtils.getFile(getFullPath()));
    }

    @Override
    public void copyTo(Folder folder, String destName, boolean deep)
            throws LockedException, MultistatusException, ServerException, ConflictException {
        ((FolderImpl) folder).ensureHasToken();
        java.io.File destinationFolder = FileUtils.getFile(getRootFolder(), decodeAndConvertToPath(folder.getPath()));
        if (!destinationFolder.exists() || !destinationFolder.isDirectory()) {
            throw new ConflictException();
        }
        try {
            FileUtils.copyFile(FileUtils.getFile(getFullPath()), FileUtils.getFile(destinationFolder, destName));
        } catch (IOException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public void moveTo(Folder folder, String destName) throws LockedException,
            ConflictException, MultistatusException, ServerException {
        ensureHasToken();
        ((FolderImpl) folder).ensureHasToken();
        java.io.File destinationFolder = FileUtils.getFile(getRootFolder(), decodeAndConvertToPath(folder.getPath()));
        if (!destinationFolder.exists() || !destinationFolder.isDirectory()) {
            throw new ConflictException();
        }
        try {
            java.io.File destFile = FileUtils.getFile(destinationFolder, destName);
            if (destFile.exists() && !destFile.isDirectory()) {
                FileUtils.deleteQuietly(destFile);
            }
            FileUtils.moveFile(FileUtils.getFile(getFullPath()), destFile);
            if (activeLocks != null && !activeLocks.isEmpty()) {
                ExtendedAttributesExtension.deleteExtendedAttribute(getFullPath(), activeLocksAttribute);
                ExtendedAttributesExtension.setExtendedAttribute(destFile.getPath(), activeLocksAttribute, SerializationUtils.serialize(activeLocks));
            }
        } catch (IOException e) {
            throw new ServerException(e);
        }
        setName(destName);
    }

}

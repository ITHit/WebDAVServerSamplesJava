package com.ithit.webdav.samples.fsstorageservlet;

import com.ithit.webdav.samples.fsstorageservlet.extendedattributes.ExtendedAttributesExtension;
import com.ithit.webdav.server.*;
import com.ithit.webdav.server.exceptions.ConflictException;
import com.ithit.webdav.server.exceptions.LockedException;
import com.ithit.webdav.server.exceptions.MultistatusException;
import com.ithit.webdav.server.exceptions.ServerException;
import com.ithit.webdav.server.resumableupload.ResumableUpload;
import com.ithit.webdav.server.resumableupload.UploadProgress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents file in the File System repository.
 */
final class FileImpl extends HierarchyItemImpl implements File, Lock,
        ResumableUpload, UploadProgress {

    private static final int BUFFER_SIZE = 1048576; // 1 Mb

    private String snippet;
    
    private final OpenOption[] allowedOpenFileOptions;

    /**
     * Initializes a new instance of the {@link FileImpl} class.
     *
     * @param name              Name of hierarchy item.
     * @param path              Relative to WebDAV root folder path.
     * @param created           Creation time of the hierarchy item.
     * @param modified          Modification time of the hierarchy item.
     * @param engine            Instance of current {@link WebDavEngine}.
     */
    private FileImpl(String name, String path, long created, long modified, WebDavEngine engine) {
        super(name, path, created, modified, engine);
        
        /* Mac OS X and Ubuntu doesn't work with ExtendedOpenOption.NOSHARE_DELETE */
        String systemName = System.getProperty("os.name").toLowerCase();
        this.allowedOpenFileOptions = (systemName.contains("mac") || systemName.contains("linux")) ?
                (new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.READ}) :
                (new OpenOption[]{StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.READ,
                        noShareDeleteOption()});
    }

    /**
     * Load ExtendedOpenOption with reflection without direct reference - because most of Linux/MacOS jdks don't have it and not required.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private OpenOption noShareDeleteOption() {
        try {
            Class enumClass = Class.forName("com.sun.nio.file.ExtendedOpenOption");
            return (OpenOption) Enum.valueOf(enumClass, "NOSHARE_DELETE");
        } catch (ClassNotFoundException e) {
            return StandardOpenOption.READ;
        }
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
        BasicFileAttributes view = null;
        Path fullPath;
        String name = null;
        try {
            String pathFragment = decodeAndConvertToPath(path);
            String rootFolder = getRootFolder();
            fullPath = Paths.get(rootFolder, pathFragment);
            if (Files.exists(fullPath)) {
                name = Paths.get(pathFragment).getFileName().toString();
                view = Files.getFileAttributeView(fullPath, BasicFileAttributeView.class).readAttributes();
            }
            if (view == null) {
                return null;
            }
        } catch (IOException e) {
            throw new ServerException();
        }
        long created = view.creationTime().toMillis();
        long modified = view.lastModifiedTime().toMillis();
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
        Path fullPath = getFullPath();
        long size = 0;
        if (Files.exists(fullPath)) {
            try {
                size = Files.size(fullPath);
            } catch (IOException e) {
                throw new ServerException(e);
            }
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
    // <<<< readFileImpl
    @Override
    public void read(OutputStream out, long startIndex, long count) throws ServerException {
        Path fullPath = this.getFullPath();
        byte[] buf = new byte[BUFFER_SIZE];
        int retVal;
        try (InputStream in = Files.newInputStream(fullPath)) {
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
    // readFileImpl >>>>

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
    // <<<< writeFileImpl
    @Override
    public long write(InputStream content, String contentType, long startIndex, long totalFileLength)
            throws LockedException, ServerException, IOException {
        ensureHasToken();
        SeekableByteChannel writer = Files.newByteChannel(getFullPath(), this.allowedOpenFileOptions);
        if (startIndex == 0) {
            // If we override the file we must set position to 0 because writer could be at not 0 position.
            writer = writer.truncate(0);
        } else {
            // We must set to start position in case of resume upload.
            writer.position(startIndex);
        }
        incrementSerialNumber();
        byte[] inputBuffer = new byte[BUFFER_SIZE];
        long totalWrittenBytes = startIndex;
        int readBytes;
        try {
            while ((readBytes = content.read(inputBuffer)) > -1) {
                ByteBuffer byteBuffer;
                byteBuffer = ByteBuffer.wrap(inputBuffer, 0, readBytes);
                writer.write(byteBuffer);
                totalWrittenBytes += readBytes;
            }

            try {
                getEngine().getSearchFacade().getIndexer().indexFile(getName(), decode(getPath()), null, this);
            } catch (Exception ex){
                getEngine().getLogger().logError("Errors during indexing.", ex);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            writer.close();
        }
        getEngine().getWebSocketServer().notifyUpdated(getPath(), getWebSocketID());
        return totalWrittenBytes;
    }
    // writeFileImpl >>>>

    private void incrementSerialNumber() {
        try {
            Property serialNumber = Property.create("", "SerialNumber", "1");
            String sn = getSerialNumber();
            if (!Objects.equals(sn, "0")) {
                serialNumber.setValue(String.valueOf((Integer.parseInt(sn) + 1)));
            }
            ExtendedAttributesExtension.setExtendedAttribute(getFullPath().toString(), "SerialNumber", SerializationUtils.serialize(Collections.singletonList(serialNumber)));
        } catch (Exception ex) {
            getEngine().getLogger().logError("Cannot update serial number.", ex);
        }
    }

    private String getSerialNumber() throws ServerException {
        String serialJson = ExtendedAttributesExtension.getExtendedAttribute(getFullPath().toString(), "SerialNumber");
        List<Property> properties = SerializationUtils.deserializeList(Property.class, serialJson);
        if (properties.size() == 1) {
            return properties.get(0).getXmlValueRaw();
        }
        return "0";
    }


    @Override
    public void delete() throws LockedException, MultistatusException, ServerException {
        ensureHasToken();
        try {
            Files.delete(getFullPath());
        } catch (IOException e) {
            getEngine().getLogger().logError("Tried to delete file in use.", e);
            throw new ServerException(e);
        }
        getEngine().getWebSocketServer().notifyDeleted(getPath(), getWebSocketID());
        try {
            getEngine().getSearchFacade().getIndexer().deleteIndex(this);
        } catch (Exception ex) {
            getEngine().getLogger().logError("Errors during indexing.", ex);
        }
    }

    @Override
    public void copyTo(Folder folder, String destName, boolean deep)
            throws LockedException, MultistatusException, ServerException, ConflictException {
        ((FolderImpl) folder).ensureHasToken();
        String destinationFolder = Paths.get(getRootFolder(), HierarchyItemImpl.decodeAndConvertToPath(folder.getPath())).toString();
        if (!Files.exists(Paths.get(destinationFolder))) {
            throw new ConflictException();
        }
        Path newPath = Paths.get(destinationFolder, destName);
        try {
            Files.copy(getFullPath(), newPath);
        } catch (IOException e) {
            throw new ServerException(e);
        }
        // Locks should not be copied, delete them
        if (ExtendedAttributesExtension.hasExtendedAttribute(newPath.toString(), activeLocksAttribute)) {
            ExtendedAttributesExtension.deleteExtendedAttribute(newPath.toString(), activeLocksAttribute);
        }
        try {
            String currentPath = folder.getPath() + encode(destName);
            getEngine().getWebSocketServer().notifyCreated(currentPath, getWebSocketID());
            getEngine().getSearchFacade().getIndexer().indexFile(decode(destName), decode(currentPath), null, this);
        } catch (Exception ex) {
            getEngine().getLogger().logError("Errors during indexing.", ex);
        }
    }

    @Override
    public void moveTo(Folder folder, String destName) throws LockedException,
            ConflictException, MultistatusException, ServerException {
        ensureHasToken();
        ((FolderImpl) folder).ensureHasToken();
        String destinationFolder = Paths.get(getRootFolder(), HierarchyItemImpl.decodeAndConvertToPath(folder.getPath())).toString();
        if (!Files.exists(Paths.get(destinationFolder))) {
            throw new ConflictException();
        }
        Path newPath = Paths.get(destinationFolder, destName);
        try {
            Files.move(getFullPath(), newPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ServerException(e);
        }
        setName(destName);
        // Locks should not be copied, delete them
        if (ExtendedAttributesExtension.hasExtendedAttribute(newPath.toString(), activeLocksAttribute)) {
            ExtendedAttributesExtension.deleteExtendedAttribute(newPath.toString(), activeLocksAttribute);
        }
        try {
            String currentPath = folder.getPath() + encode(destName);
            getEngine().getWebSocketServer().notifyMoved(getPath(), currentPath, getWebSocketID());
            getEngine().getSearchFacade().getIndexer().indexFile(decode(destName), decode(currentPath), getPath(), this);
        } catch (Exception ex) {
            getEngine().getLogger().logError("Errors during indexing.", ex);
        }
    }

    /**
     * Returns snippet of file content that matches search conditions.
     *
     * @return Snippet of file content that matches search conditions.
     */
    String getSnippet() {
        return snippet;
    }

    /**
     * Sets snippet of file content that matches search conditions.
     *
     * @param snippet Snippet of file content that matches search conditions.
     */
    void setSnippet(String snippet) {
        this.snippet = snippet;
    }
}

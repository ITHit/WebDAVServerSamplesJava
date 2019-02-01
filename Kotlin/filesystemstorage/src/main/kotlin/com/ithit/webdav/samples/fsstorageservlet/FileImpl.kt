package com.ithit.webdav.samples.fsstorageservlet

import com.ithit.webdav.samples.fsstorageservlet.extendedattributes.ExtendedAttributesExtension
import com.ithit.webdav.server.*
import com.ithit.webdav.server.exceptions.ConflictException
import com.ithit.webdav.server.exceptions.LockedException
import com.ithit.webdav.server.exceptions.MultistatusException
import com.ithit.webdav.server.exceptions.ServerException
import com.ithit.webdav.server.resumableupload.ResumableUpload
import com.ithit.webdav.server.resumableupload.UploadProgress
import com.sun.nio.file.ExtendedOpenOption
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes

/**
 * Represents file in the File System repository.
 */
internal class FileImpl
/**
 * Initializes a new instance of the [FileImpl] class.
 *
 * @param name     Name of hierarchy item.
 * @param path     Relative to WebDAV root folder path.
 * @param created  Creation time of the hierarchy item.
 * @param modified Modification time of the hierarchy item.
 * @param engine   Instance of current [WebDavEngine].
 */
private constructor(name: String, path: String, created: Long, modified: Long, engine: WebDavEngine) : HierarchyItemImpl(name, path, created, modified, engine), File, Lock, ResumableUpload, UploadProgress {

    private val bufferSize = 1048576 // 1 Mb

    /**
     * Returns snippet of file content that matches search conditions.
     *
     * @return Snippet of file content that matches search conditions.
     */
    /**
     * Sets snippet of file content that matches search conditions.
     *
     * @param snippet Snippet of file content that matches search conditions.
     */
    var snippet: String? = null

    private val serialNumber: String
        @Throws(ServerException::class)
        get() {
            val serialJson = ExtendedAttributesExtension.getExtendedAttribute(fullPath.toString(), "SerialNumber")
            val properties = SerializationUtils.deserializeList(Property::class.java, serialJson)
            return if (properties.size == 1) {
                properties[0].value
            } else "0"
        }

    /**
     * Array of items that are being uploaded to this item subtree.
     *
     * @return Return array with a single item if implemented on file items. Return all items that are being uploaded to this subtree if implemented on folder items.
     * @throws ServerException - in case of an error.
     */
    @Throws(ServerException::class)
    override fun getUploadProgress(): List<ResumableUpload> {
        return listOf(this)
    }

    /**
     * In this method implementation you can delete partially uploaded file.
     *
     * Client do not plan to restore upload. Remove any temporary files / cleanup resources here.
     *
     * @throws LockedException - this item or its parent was locked and client did not provide lock token.
     * @throws ServerException - in case of an error.
     */
    @Throws(LockedException::class, ServerException::class)
    override fun cancelUpload() {
        ensureHasToken()
    }

    /**
     * Amount of bytes successfully saved to your storage.
     *
     * @return Amount of bytes successfully saved.
     * @throws ServerException in case of an error.
     */
    @Throws(ServerException::class)
    override fun getBytesUploaded(): Long {
        return contentLength
    }

    /**
     * Indicates if item will be checked-in by the engine when last chunk of a file is uploaded
     * if item was checked in when upload started.
     *
     * @return True if item will be checked in when upload finishes.
     * @throws ServerException in case of an error.
     */
    @Throws(ServerException::class)
    override fun getCheckInOnFileComplete(): Boolean {
        return false
    }

    /**
     * Shall store value which indicates whether file will be checked in when upload finishes.
     *
     * @param value True if item will be checked in when upload finishes.
     * @throws ServerException in case of an error.
     */
    @Throws(ServerException::class)
    override fun setCheckInOnFileComplete(value: Boolean) {
        throw ServerException("Not implemented")
    }

    /**
     * The date and time when the last chunk of file was saved in your storage.
     *
     * @return Time when last chunk of file was saved.
     * @throws ServerException in case of an error.
     */
    @Throws(ServerException::class)
    override fun getLastChunkSaved(): Long {
        return modified
    }

    /**
     * Total file size that is being uploaded.
     *
     * @return Total file size in bytes.
     * @throws ServerException in case of an error.
     */
    @Throws(ServerException::class)
    override fun getTotalContentLength(): Long {
        return contentLength
    }

    /**
     * Gets the size of the file content in bytes.
     *
     * @return Length of the file content in bytes.
     * @throws ServerException In case of an error.
     */
    @Throws(ServerException::class)
    override fun getContentLength(): Long {
        val fullPath = fullPath
        var size: Long = 0
        if (Files.exists(fullPath)) {
            try {
                size = Files.size(fullPath)
            } catch (e: IOException) {
                throw ServerException(e)
            }
        }
        return size
    }

    /**
     * Gets the media type of the [FileImpl].
     *
     * @return MIME type of the file.
     * @throws ServerException In case of an error.
     */
    @Throws(ServerException::class)
    override fun getContentType(): String {
        val name = this.name
        val periodIndex = name!!.lastIndexOf('.')
        val ext = name.substring(periodIndex + 1, name.length)
        var contentType = MimeType.getInstance().getMimeType(ext)
        if (contentType == null)
            contentType = "application/octet-stream"
        return contentType
    }

    @Throws(ServerException::class)
    override fun getEtag(): String {
        return String.format("%s-%s", java.lang.Long.hashCode(modified), serialNumber)
    }

    /**
     * Writes the content of the file to the specified stream.
     *
     * @param out        Output stream.
     * @param startIndex Zero-based byte offset in file content at which to begin copying bytes to the output stream.
     * @param count      Number of bytes to be written to the output stream.
     * @throws ServerException In case of an error.
     */
    @Throws(ServerException::class)
    override fun read(out: OutputStream, startIndex: Long, count: Long) {
        var localStartIndex = startIndex
        var localCount = count
        val fullPath = this.fullPath
        val buf = ByteArray(bufferSize)
        try {
            Files.newInputStream(fullPath).use { inn ->
                inn.skip(localStartIndex)
                var retVal: Int = -1
                while ({retVal = inn.read(buf); retVal}() > 0) {
                    // Strict servlet API doesn't allow to write more bytes then content length. So we do this trick.
                    if (retVal > localCount) {
                        retVal = localCount.toInt()
                    }
                    out.write(buf, 0, retVal)
                    localStartIndex += retVal.toLong()
                    localCount -= retVal.toLong()
                }
            }
        } catch (x: IOException) {
            throw ServerException(x)
        }
    }

    /**
     * Saves the content of the file from the specified stream to the File System repository.
     *
     * @param content         [InputStream] to read the content of the file from.
     * @param contentType     Indicates media type of the file.
     * @param startIndex      Index in file to which corresponds first byte in `content`.
     * @param totalFileLength Total size of the file being uploaded. -1 if size is unknown.
     * @return Number of bytes written.
     * @throws LockedException File was locked and client did not provide lock token.
     * @throws ServerException In case of an error.
     * @throws IOException     I/O error.
     */
    @Throws(LockedException::class, ServerException::class, IOException::class)
    override fun write(content: InputStream, contentType: String?, startIndex: Long, totalFileLength: Long): Long {
        ensureHasToken()
        var writer = Files.newByteChannel(fullPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE);
        if (startIndex == 0L) {
            // If we override the file we must set position to 0 because writer could be at not 0 position.
            writer = writer.truncate(0)
        } else {
            // We must set to start position in case of resume upload.
            writer.position(startIndex)
        }
        incrementSerialNumber()
        val inputBuffer = ByteArray(bufferSize)
        var totalWrittenBytes = startIndex
        try {
            var readBytes: Int = -1
            while ({readBytes = content.read(inputBuffer); readBytes} () > -1) {
                val byteBuffer: ByteBuffer = ByteBuffer.wrap(inputBuffer, 0, readBytes)
                writer.write(byteBuffer)
                totalWrittenBytes += readBytes.toLong()
            }
            try {
                engine.searchFacade!!.indexer!!.indexFile(name!!, HierarchyItemImpl.decode(path), null, this)
            } catch (ex: Exception) {
                engine.logger?.logError("Errors during indexing.", ex)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            writer.close()
        }
        engine.webSocketServer?.notifyRefresh(getParent(path))
        return totalWrittenBytes
    }

    private fun incrementSerialNumber() {
        try {
            val seNumber = Property("", "SerialNumber", "1")
            val sn = serialNumber
            if (sn != "0") {
                seNumber.value = (Integer.valueOf(sn) + 1).toString()
            }
            ExtendedAttributesExtension.setExtendedAttribute(fullPath.toString(), "SerialNumber", SerializationUtils.serialize(listOf(seNumber)))
        } catch (ex: Exception) {
            engine.logger?.logError("Cannot update serial number.", ex)
        }
    }


    @Throws(LockedException::class, MultistatusException::class, ServerException::class)
    override fun delete() {
        ensureHasToken()
        try {
            Files.delete(fullPath)
        } catch (e: IOException) {
            engine.logger?.logError("Tried to delete file in use.", e)
            throw ServerException(e)
        }

        engine.webSocketServer?.notifyRefresh(getParent(path))
        try {
            engine.searchFacade!!.indexer!!.deleteIndex(this)
        } catch (ex: Exception) {
            engine.logger?.logError("Errors during indexing.", ex)
        }

    }

    @Throws(LockedException::class, MultistatusException::class, ServerException::class, ConflictException::class)
    override fun copyTo(folder: Folder, destName: String, deep: Boolean) {
        (folder as FolderImpl).ensureHasToken()
        val destinationFolder = Paths.get(rootFolder!!, HierarchyItemImpl.decodeAndConvertToPath(folder.getPath())).toString()
        if (!Files.exists(Paths.get(destinationFolder))) {
            throw ConflictException()
        }
        val newPath = Paths.get(destinationFolder, destName)
        try {
            Files.copy(fullPath, newPath)
        } catch (e: IOException) {
            throw ServerException(e)
        }

        // Locks should not be copied, delete them
        if (ExtendedAttributesExtension.hasExtendedAttribute(newPath.toString(), activeLocksAttribute)) {
            ExtendedAttributesExtension.deleteExtendedAttribute(newPath.toString(), activeLocksAttribute)
        }
        engine.webSocketServer?.notifyRefresh(folder.getPath())
        try {
            val currentPath = folder.getPath() + destName
            engine.searchFacade!!.indexer!!.indexFile(HierarchyItemImpl.decode(destName), HierarchyItemImpl.decode(currentPath), null, this)
        } catch (ex: Exception) {
            engine.logger?.logError("Errors during indexing.", ex)
        }

    }

    @Throws(LockedException::class, ConflictException::class, MultistatusException::class, ServerException::class)
    override fun moveTo(folder: Folder, destName: String) {
        ensureHasToken()
        (folder as FolderImpl).ensureHasToken()
        val destinationFolder = Paths.get(rootFolder!!, HierarchyItemImpl.decodeAndConvertToPath(folder.getPath())).toString()
        if (!Files.exists(Paths.get(destinationFolder))) {
            throw ConflictException()
        }
        val newPath = Paths.get(destinationFolder, destName)
        try {
            Files.move(fullPath, Paths.get(destinationFolder, destName), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            throw ServerException(e)
        }

        setName(destName)
        // Locks should not be copied, delete them
        if (ExtendedAttributesExtension.hasExtendedAttribute(newPath.toString(), activeLocksAttribute)) {
            ExtendedAttributesExtension.deleteExtendedAttribute(newPath.toString(), activeLocksAttribute)
        }
        engine.webSocketServer?.notifyRefresh(getParent(path))
        engine.webSocketServer?.notifyRefresh(folder.path)
        try {
            val currentPath = folder.path + destName
            engine.searchFacade!!.indexer!!.indexFile(HierarchyItemImpl.decode(destName), HierarchyItemImpl.decode(currentPath), path, this)
        } catch (ex: Exception) {
            engine.logger?.logError("Errors during indexing.", ex)
        }

    }

    companion object {

        /**
         * Returns file that corresponds to path.
         *
         * @param path   Encoded path relative to WebDAV root.
         * @param engine Instance of [WebDavEngine]
         * @return File instance or null if physical file not found in file system.
         * @throws ServerException in case of exception
         */
        @Throws(ServerException::class)
        fun getFile(path: String, engine: WebDavEngine): FileImpl? {
            var view: BasicFileAttributes? = null
            val fullPath: Path
            var name: String? = null
            try {
                val pathFragment = HierarchyItemImpl.decodeAndConvertToPath(path)
                val rootFolder = rootFolder
                fullPath = Paths.get(rootFolder!!, pathFragment)
                if (Files.exists(fullPath)) {
                    name = Paths.get(pathFragment).fileName.toString()
                    view = Files.getFileAttributeView<BasicFileAttributeView>(fullPath, BasicFileAttributeView::class.java).readAttributes()
                }
                if (view == null) {
                    return null
                }
            } catch (e: IOException) {
                throw ServerException()
            }

            val created = view.creationTime().toMillis()
            val modified = view.lastModifiedTime().toMillis()
            return FileImpl(name!!, path, created, modified, engine)
        }
    }
}

package com.ithit.webdav.samples.fsstorageservlet

import com.ithit.webdav.integration.extendedattributes.ExtendedAttributesExtension
import com.ithit.webdav.integration.utils.IntegrationUtil.INSTANCE_HEADER_NAME
import com.ithit.webdav.integration.utils.SerializationUtils
import com.ithit.webdav.server.*
import com.ithit.webdav.server.exceptions.*
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors

const val SNIPPET = "snippet"
const val METADATA_ETAG = "metadata-Etag"

/**
 * Base class for WebDAV items (folders, files, etc).
 */
internal abstract class HierarchyItemImpl
/**
 * Initializes a new instance of the [HierarchyItemImpl] class.
 *
 * @param name     name of hierarchy item
 * @param path     Relative to WebDAV root folder path.
 * @param created  creation time of the hierarchy item
 * @param modified modification time of the hierarchy item
 * @param engine   instance of current [WebDavEngine]
 */
(private var name: String?, private val path: String, private val created: Long, private val modified: Long,
 /**
  * Returns File System engine.
  *
  * @return File System engine.
  */
 val engine: WebDavEngine) : HierarchyItem, Lock {
    protected var newPath: Path? = null
    var activeLocksAttribute = "Locks"
    private val propertiesAttribute = "Properties"
    private var properties: MutableList<Property>? = null
    private var activeLocks: MutableList<LockInfo>? = null

    /**
     * Returns full path in the File System to the [HierarchyItemImpl].
     *
     * @return Full path in the File System to the [HierarchyItemImpl].
     */
    val fullPath: Path
        get() {
            var fullPath = ""
            try {
                fullPath = rootFolder!! + decodeAndConvertToPath(getPath())
            } catch (ignored: ServerException) {
            }

            return Paths.get(fullPath)
        }

    /**
     * Encodes string to safe characters.
     *
     * @param `val` String to encode.
     * @return Encoded string.
     */
    fun encode(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8").replace("+", "%20")
        } catch (e: UnsupportedEncodingException) {
            URLEncoder.encode(value).replace("+", "%20")
        }

    }

    /**
     * Creates a copy of this item with a new name in the destination folder.
     *
     * @param folder   Destination folder.
     * @param destName Name of the destination item.
     * @param deep     Indicates whether to copy entire subtree.
     * @throws LockedException      - the destination item was locked and client did not provide lock token.
     * @throws ConflictException    - destination folder does not exist.
     * @throws MultistatusException - errors has occurred during processing of the subtree.
     * Every item that has been either successfully copied or failed to copy must be present in exception with corresponding status.
     * @throws ServerException      - In case of other error.
     */
    @Throws(LockedException::class, MultistatusException::class, ServerException::class, ConflictException::class)
    abstract override fun copyTo(folder: Folder, destName: String, deep: Boolean)

    /**
     * Moves this item to the destination folder under a new name.
     *
     * @param folder   Destination folder.
     * @param destName Name of the destination item.
     * @throws LockedException      - the source or the destination item was locked and client did not provide lock token.
     * @throws ConflictException    - destination folder does not exist.
     * @throws MultistatusException - errors has occurred during processing of the subtree. Every processed item must have corresponding response added
     * with corresponding status.
     * @throws ServerException      - in case of another error.
     */
    @Throws(LockedException::class, ConflictException::class, MultistatusException::class, ServerException::class)
    abstract override fun moveTo(folder: Folder, destName: String)

    /**
     * Deletes this item.
     *
     * @throws LockedException      - this item or its parent was locked and client did not provide lock token.
     * @throws MultistatusException - errors has occurred during processing of the subtree. Every processed item must have corresponding response added
     * to the exception with corresponding status.
     * @throws ServerException      - in case of another error.
     */
    @Throws(LockedException::class, MultistatusException::class, ServerException::class)
    abstract override fun delete()

    /**
     * Gets the creation date of the item in repository expressed as the coordinated universal time (UTC).
     *
     * @return Creation date of the item.
     * @throws ServerException In case of an error.
     */
    @Throws(ServerException::class)
    override fun getCreated(): Long {
        return created
    }

    /**
     * Gets the last modification date of the item in repository expressed as the coordinated universal time (UTC).
     *
     * @return Modification date of the item.
     * @throws ServerException In case of an error.
     */
    @Throws(ServerException::class)
    override fun getModified(): Long {
        return modified
    }

    /**
     * Parses string to Date.
     *
     * @param inputDate Date in string.
     * @return Date instance in UTC.
     * @throws ParseException is cannot parse the Date.
     */
    @Throws(ParseException::class)
    private fun parseDateFrom(inputDate: String): Date {
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z")
        val date: Date
        date = format.parse(inputDate)
        return date
    }

    /**
     * Gets the name of the item in repository.
     *
     * @return Name of this item.
     * @throws ServerException In case of an error.
     */
    @Throws(ServerException::class)
    override fun getName(): String? {
        return name
    }

    /**
     * Set [HierarchyItemImpl] name.
     *
     * @param name [HierarchyItemImpl] name.
     */
    fun setName(name: String) {
        this.name = name
    }

    /**
     * Unique item path in the repository relative to storage root.
     *
     * @return Item path relative to storage root.
     * @throws ServerException In case of an error.
     */
    @Throws(ServerException::class)
    override fun getPath(): String {
        return path
    }

    /**
     * Gets values of all properties or selected properties for this item.
     *
     * @return List of properties with values set. If property cannot be found it shall be omitted from the result.
     * @throws ServerException In case of an error.
     */
    @Throws(ServerException::class)
    override fun getProperties(props: Array<Property>?): List<Property> {
        val l = propertyNames
        val result: MutableList<Property>
        if (props == null) {
            return l
        }
        val propNames = Arrays.stream(props).map { it.name }.collect(Collectors.toSet())
        result = l.stream().filter { x -> propNames.contains(x.name) }.collect(Collectors.toList())
        val snippet = Arrays.stream(props).filter { x: Property? -> SNIPPET == x?.name }.findFirst().orElse(null)
        if (snippet != null && this is FileImpl) {
            result.add(Property.create(snippet.namespace, snippet.name, this.snippet))
        }
        val metadata = Arrays.stream(props).filter { x: Property? -> METADATA_ETAG == x?.name }.findFirst().orElse(null)
        if (metadata != null) {
            result.add(Property.create(metadata.namespace, metadata.name, getMetadataEtag()))
        }
        return result
    }

    @Throws(ServerException::class)
    private fun getProperties(): MutableList<Property> {
        if (properties == null) {
            val propertiesJson = ExtendedAttributesExtension.getExtendedAttribute(fullPath.toString(), propertiesAttribute)
            properties = SerializationUtils.deserializeList(Property::class.java, propertiesJson).toMutableList()
        }
        return properties as MutableList<Property>
    }

    /**
     * Returns Metadata ETag stored in extended attributes.
     * @return Metadata ETag.
     * @throws ServerException in case of reading exception.
     */
    @Throws(ServerException::class)
    private fun getMetadataEtag(): String {
        val serialJson =
            ExtendedAttributesExtension.getExtendedAttribute(fullPath.toString(), METADATA_ETAG)
        val metadataProperties = SerializationUtils.deserializeList(
            Property::class.java, serialJson
        )
        if (metadataProperties.size == 1) {
            return metadataProperties[0].xmlValueRaw
        }
        return "0"
    }

    /**
     * Increments Metadata ETag by 1.
     */
    protected fun incrementMetadataEtag() {
        try {
            val metadataEtag = Property.create("", METADATA_ETAG, "1")
            val sn = getMetadataEtag()
            if (sn != "0") {
                metadataEtag.value = (sn.toInt() + 1).toString()
            }
            ExtendedAttributesExtension.setExtendedAttribute(
                fullPath.toString(), METADATA_ETAG, SerializationUtils.serialize(
                    listOf<Property>(metadataEtag)
                )
            )
        } catch (ex: java.lang.Exception) {
            engine.logger?.logError("Cannot update metadata etag.", ex)
        }
    }

    /**
     * Gets names of all properties for this item.
     *
     * @return List of all property names for this item.
     * @throws ServerException In case of an error.
     */
    @Throws(ServerException::class)
    override fun getPropertyNames(): List<Property> {
        if (ExtendedAttributesExtension.hasExtendedAttribute(fullPath.toString(), propertiesAttribute)) {
            val propJson = ExtendedAttributesExtension.getExtendedAttribute(fullPath.toString(), propertiesAttribute)
            return SerializationUtils.deserializeList(Property::class.java, propJson)
        }
        return LinkedList()
    }

    /**
     * Check whether client is the lock owner.
     *
     * @throws LockedException in case if not owner.
     * @throws ServerException other errors.
     */
    @Throws(LockedException::class, ServerException::class)
    fun ensureHasToken() {
        if (!clientHasToken())
            throw LockedException()
    }

    /**
     * Check whether client is the lock owner.
     *
     * @return True if owner, false otherwise.
     * @throws ServerException in case of errors.
     */
    @Throws(ServerException::class)
    private fun clientHasToken(): Boolean {
        getActiveLocks()
        if (activeLocks!!.size == 0) {
            return true
        }
        val clientLockTokens = DavContext.currentRequest()!!.clientLockTokens
        return activeLocks!!.stream().filter { x -> clientLockTokens.contains(x.token) }.collect(Collectors.toList()).isNotEmpty()
    }

    /**
     * Modifies and removes properties for this item.
     *
     * @param setProps Array of properties to be set.
     * @param delProps Array of properties to be removed. [Property.getValue()] field is ignored.
     * Specifying the removal of a property that does not exist is not an error.
     * @throws LockedException      this item was locked and client did not provide lock token.
     * @throws MultistatusException If update fails for a property, this exception shall be thrown and contain
     * result of the operation for each property.
     * @throws ServerException      In case of other error.
     */
    @Throws(LockedException::class, MultistatusException::class, ServerException::class)
    override fun updateProperties(setProps: Array<Property>, delProps: Array<Property>) {
        ensureHasToken()
        for (prop in setProps) {
            val basicAttributeNS = "urn:schemas-microsoft-com:"
            // Microsoft Mini-redirector may update file creation date, modification date and access time passing properties:
            // <Win32CreationTime xmlns="urn:schemas-microsoft-com:">Thu, 28 Mar 2013 20:15:34 GMT</Win32CreationTime>
            // <Win32LastModifiedTime xmlns="urn:schemas-microsoft-com:">Thu, 28 Mar 2013 20:36:24 GMT</Win32LastModifiedTime>
            // <Win32LastAccessTime xmlns="urn:schemas-microsoft-com:">Thu, 28 Mar 2013 20:36:24 GMT</Win32LastAccessTime>
            // In this case update creation and modified date in your storage or do not save this properties at all, otherwise
            // Windows Explorer will display creation and modification date from this props and it will differ from the values
            // in the Created and Modified fields in your storage
            // String basicAttributeNS = "urn:schemas-microsoft-com:";
            if (prop.namespace == basicAttributeNS) {
                updateBasicProperties(prop.xmlValueRaw, prop.name)
            } else {
                properties = getProperties()
                val existingProp = properties!!.stream().filter { x -> x.name == prop.name }.findFirst().orElse(null)
                if (existingProp != null) {
                    existingProp.xmlValueRaw = prop.xmlValueRaw
                } else {
                    properties!!.add(prop)
                }
            }
        }
        properties = getProperties()
        val propNamesToDel = Arrays.stream(delProps).map { it.name }.collect(Collectors.toSet())
        properties = properties!!.stream()
                .filter { e -> !propNamesToDel.contains(e.name) }
                .collect(Collectors.toList())
        ExtendedAttributesExtension.setExtendedAttribute(fullPath.toString(), propertiesAttribute, SerializationUtils.serialize(properties as List<Property>))
        incrementMetadataEtag()
        engine.webSocketServer?.notifyUpdated(getPath(), getWebSocketID())
    }

    /**
     * Updates basic file times in the following format - Thu, 28 Mar 2013 20:15:34 GMT.
     *
     * @param date  Date to update
     * @param field Field to update
     */
    private fun updateBasicProperties(date: String?, field: String) {
        val attributes = Files.getFileAttributeView(fullPath, BasicFileAttributeView::class.java)
        try {
            val propertyCreatedName = "Win32CreationTime"
            val propertyModifiedName = "Win32LastModifiedTime"
            val propertyAccessedName = "Win32LastAccessTime"
            if (field == propertyCreatedName || field == propertyModifiedName || field == propertyAccessedName) {
                val time = if (date == null) null else FileTime.fromMillis(parseDateFrom(date).time)
                if (field == propertyModifiedName) {
                    attributes.setTimes(time, null, null)
                }
                if (field == propertyAccessedName) {
                    attributes.setTimes(null, time, null)
                }
                if (field == propertyCreatedName) {
                    // For some reason Windows Explorer caches created time so for some period of time it may return wrong creation time
                    attributes.setTimes(null, null, time)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Locks this item.
     *
     * @param shared  Indicates whether a lock is shared or exclusive.
     * @param deep    Indicates whether a lock is enforceable on the subtree.
     * @param timeout Lock expiration time in seconds. Negative value means never.
     * @param owner   Provides information about the principal taking out a lock.
     * @return Actually applied lock (Server may modify timeout).
     * @throws LockedException      The item is locked, so the method has been rejected.
     * @throws MultistatusException Errors have occurred during processing of the subtree.
     * @throws ServerException      In case of an error.
     */
    @Throws(LockedException::class, MultistatusException::class, ServerException::class)
    override fun lock(shared: Boolean, deep: Boolean, timeout: Long, owner: String): LockResult {
        var localTimeout = timeout
        if (hasLock(shared)) {
            throw LockedException()
        }
        val token = UUID.randomUUID().toString()
        if (localTimeout < 0 || localTimeout == java.lang.Long.MAX_VALUE) {
            // If timeout is absent or infinity timeout requested,
            // grant 5 minute lock.
            localTimeout = 300
        }
        val expires = System.currentTimeMillis() + localTimeout * 1000
        val lockInfo = LockInfo(shared, deep, token, expires, owner)
        activeLocks!!.add(lockInfo)
        ExtendedAttributesExtension.setExtendedAttribute(fullPath.toString(), activeLocksAttribute, SerializationUtils.serialize<List<LockInfo>>(activeLocks!!))
        incrementMetadataEtag()
        engine.webSocketServer?.notifyLocked(getPath(), getWebSocketID())
        return LockResult(token, localTimeout)
    }

    /**
     * Checks whether [HierarchyItemImpl] has a lock and whether it is shared.
     *
     * @param skipShared Indicates whether to skip shared.
     * @return True if item has lock and skipShared is true, false otherwise.
     * @throws ServerException in case of errors.
     */
    @Throws(ServerException::class)
    private fun hasLock(skipShared: Boolean): Boolean {
        val locks = getActiveLocks()
        return locks.isNotEmpty() && !(skipShared && locks[0].isShared)
    }

    /**
     * Gets the array of all locks for this item.
     *
     * @return Array of locks.
     * @throws ServerException In case of an error.
     */
    @Throws(ServerException::class)
    override fun getActiveLocks(): List<LockInfo> {
        activeLocks = if (activeLocks == null) {
            val activeLocksJson =
                ExtendedAttributesExtension.getExtendedAttribute(fullPath.toString(), activeLocksAttribute)
            ArrayList(SerializationUtils.deserializeList(LockInfo::class.java, activeLocksJson))
        } else {
            LinkedList()
        }
        return activeLocks!!.stream()
            .filter { x -> System.currentTimeMillis() < x.timeout }
            .map { lock ->
                LockInfo(
                    lock.isShared,
                    lock.isDeep,
                    lock.token,
                    if (lock.timeout < 0 || lock.timeout == Long.MAX_VALUE) lock.timeout else (lock.timeout - System.currentTimeMillis()) / 1000,
                    lock.owner
                )
            }
            .collect(Collectors.toList())
    }

    /**
     * Removes lock with the specified token from this item.
     *
     * @param lockToken Lock with this token should be removed from the item.
     * @throws PreconditionFailedException Included lock token was not enforceable on this item.
     * @throws ServerException             In case of an error.
     */
    @Throws(PreconditionFailedException::class, ServerException::class)
    override fun unlock(lockToken: String) {
        getActiveLocks()
        val lock = activeLocks!!.stream().filter { x -> x.token == lockToken }.findFirst().orElse(null)
        if (lock != null) {
            activeLocks!!.remove(lock)
            if (activeLocks!!.isNotEmpty()) {
                ExtendedAttributesExtension.setExtendedAttribute(fullPath.toString(), activeLocksAttribute, SerializationUtils.serialize<List<LockInfo>>(activeLocks!!))
            } else {
                ExtendedAttributesExtension.deleteExtendedAttribute(fullPath.toString(), activeLocksAttribute)
            }
            incrementMetadataEtag()
            engine.webSocketServer?.notifyUnlocked(getPath(), getWebSocketID())
        } else {
            throw PreconditionFailedException()
        }
    }

    /**
     * Updates lock timeout information on this item.
     *
     * @param token   The lock token associated with a lock.
     * @param timeout Lock expiration time in seconds. Negative value means never.
     * @return Actually applied lock (Server may modify timeout).
     * @throws PreconditionFailedException Included lock token was not enforceable on this item.
     * @throws ServerException             In case of an error.
     */
    @Throws(PreconditionFailedException::class, ServerException::class)
    override fun refreshLock(token: String, timeout: Long): RefreshLockResult {
        var localTimeout = timeout
        getActiveLocks()
        val lockInfo = activeLocks!!.stream().filter { x -> x.token == token }.findFirst().orElse(null)
                ?: throw PreconditionFailedException()
        if (localTimeout < 0 || localTimeout == java.lang.Long.MAX_VALUE) {
            // If timeout is absent or infinity timeout requested,
            // grant 5 minute lock.
            localTimeout = 300
        }
        val expires = System.currentTimeMillis() + localTimeout * 1000
        lockInfo.timeout = expires
        ExtendedAttributesExtension.setExtendedAttribute(fullPath.toString(), activeLocksAttribute, SerializationUtils.serialize<List<LockInfo>>(activeLocks!!))
        incrementMetadataEtag()
        engine.webSocketServer?.notifyLocked(getPath(), getWebSocketID())
        return RefreshLockResult(lockInfo.isShared, lockInfo.isDeep,
                localTimeout, lockInfo.owner)
    }

    /**
     * Returns instance ID from header
     * @return InstanceId
     */
    protected open fun getWebSocketID(): String? {
        return DavContext.currentRequest().getHeader(INSTANCE_HEADER_NAME)
    }

    companion object {
        /**
         * Decodes URL and converts it to proper path string.
         *
         * @param url URL to decode.
         * @return Path.
         */
        fun decodeAndConvertToPath(url: String): String {
            val path = decode(url)
            return path.replace("/", File.separator)
        }

        /**
         * Decodes URL.
         *
         * @param url URL to decode.
         * @return Path.
         */
        fun decode(url: String): String {
            return try {
                URLDecoder.decode(url.replace("+", "%2B"), "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                URLDecoder.decode(url.replace("+", "%2B"))
            }
        }

        /**
         * Returns path to the folder in File System which will be the root for the WebDav storage.
         *
         * @return Path to the folder in File System which will be the root for the WebDav storage.
         */
        val rootFolder: String?
            get() = WebDavServlet.rootLocalPath
    }
}

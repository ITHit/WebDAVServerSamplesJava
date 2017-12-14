package com.ithit.webdav.samples.androidfsstorage;

import com.ithit.webdav.samples.androidfsstorage.extendedattributes.ExtendedAttributesExtension;
import com.ithit.webdav.server.Folder;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Lock;
import com.ithit.webdav.server.LockInfo;
import com.ithit.webdav.server.LockResult;
import com.ithit.webdav.server.Property;
import com.ithit.webdav.server.RefreshLockResult;
import com.ithit.webdav.server.exceptions.ConflictException;
import com.ithit.webdav.server.exceptions.LockedException;
import com.ithit.webdav.server.exceptions.MultistatusException;
import com.ithit.webdav.server.exceptions.PreconditionFailedException;
import com.ithit.webdav.server.exceptions.ServerException;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Base class for WebDAV items (folders, files, etc).
 */
abstract class HierarchyItemImpl implements HierarchyItem, Lock {

    private final String path;
    private final long created;
    private final long modified;
    private final WebDavEngine engine;
    private String name;
    String activeLocksAttribute = "Locks";
    private String propertiesAttribute = "Properties";
    private List<Property> properties;
    List<LockInfo> activeLocks;

    /**
     * Initializes a new instance of the {@link HierarchyItemImpl} class.
     *
     * @param name     name of hierarchy item
     * @param path     Relative to WebDAV root folder path.
     * @param created  creation time of the hierarchy item
     * @param modified modification time of the hierarchy item
     * @param engine   instance of current {@link WebDavEngine}
     */
    HierarchyItemImpl(String name, String path, long created, long modified, WebDavEngine engine) {
        this.name = name;
        this.path = path;
        this.created = created;
        this.modified = modified;
        this.engine = engine;
    }

    /**
     * Decodes URL and converts it to proper path string.
     *
     * @param URL URL to decode.
     * @return Path.
     */
    static String decodeAndConvertToPath(String URL) {
        String path = decode(URL);
        return path.replace("/", File.separator);
    }

    /**
     * Decodes URL.
     *
     * @param URL URL to decode.
     * @return Path.
     */
    static String decode(String URL) {
        String path = "";
        try {
            path = URLDecoder.decode(URL.replaceAll("\\+", "%2B"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("UTF-8 encoding can not be used to decode " + URL);
        }
        return path;
    }

    /**
     * Encodes string to safe characters.
     *
     * @param val String to encode.
     * @return Encoded string.
     */
    String encode(String val) {
        try {
            return URLEncoder.encode(val, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return URLEncoder.encode(val).replace("+", "%20");
        }
    }

    /**
     * Returns path to the folder in File System which will be the root for the WebDav storage.
     *
     * @return Path to the folder in File System which will be the root for the WebDav storage.
     */
    static String getRootFolder() {
        return AndroidWebDavServer.getRootLocalPath();
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
     *                              Every item that has been either successfully copied or failed to copy must be present in exception with corresponding status.
     * @throws ServerException      - In case of other error.
     */
    public abstract void copyTo(Folder folder, String destName, boolean deep)
            throws LockedException, MultistatusException, ServerException, ConflictException;

    /**
     * Moves this item to the destination folder under a new name.
     *
     * @param folder   Destination folder.
     * @param destName Name of the destination item.
     * @throws LockedException      - the source or the destination item was locked and client did not provide lock token.
     * @throws ConflictException    - destination folder does not exist.
     * @throws MultistatusException - errors has occurred during processing of the subtree. Every processed item must have corresponding response added
     *                              with corresponding status.
     * @throws ServerException      - in case of another error.
     */
    public abstract void moveTo(Folder folder, String destName)
            throws LockedException, ConflictException, MultistatusException, ServerException;

    /**
     * Deletes this item.
     *
     * @throws LockedException      - this item or its parent was locked and client did not provide lock token.
     * @throws MultistatusException - errors has occurred during processing of the subtree. Every processed item must have corresponding response added
     *                              to the exception with corresponding status.
     * @throws ServerException      - in case of another error.
     */
    @Override
    public abstract void delete() throws LockedException, MultistatusException,
            ServerException;

    /**
     * Gets the creation date of the item in repository expressed as the coordinated universal time (UTC).
     *
     * @return Creation date of the item.
     * @throws ServerException In case of an error.
     */
    @Override
    public long getCreated() throws ServerException {
        return created;
    }

    /**
     * Gets the last modification date of the item in repository expressed as the coordinated universal time (UTC).
     *
     * @return Modification date of the item.
     * @throws ServerException In case of an error.
     */
    @Override
    public long getModified() throws ServerException {
        return modified;
    }

    /**
     * Gets the name of the item in repository.
     *
     * @return Name of this item.
     * @throws ServerException In case of an error.
     */
    @Override
    public String getName() throws ServerException {
        return name;
    }

    /**
     * Set {@link HierarchyItemImpl} name.
     *
     * @param name {@link HierarchyItemImpl} name.
     */
    void setName(String name) {
        this.name = name;
    }

    /**
     * Unique item path in the repository relative to storage root.
     *
     * @return Item path relative to storage root.
     * @throws ServerException In case of an error.
     */
    @Override
    public String getPath() throws ServerException {
        return path;
    }

    /**
     * Gets values of all properties or selected properties for this item.
     *
     * @return List of properties with values set. If property cannot be found it shall be omitted from the result.
     * @throws ServerException In case of an error.
     */
    // <<<< getPropertiesImpl
    @Override
    public List<Property> getProperties(Property[] props) throws ServerException {
        List<Property> l = getPropertyNames();
        List<Property> result;
        if (props == null) {
            return l;
        }
        result = new ArrayList<>();
        Set<String> propNames = new LinkedHashSet<>();
        for (Property p: props) {
            propNames.add(p.getName());
        }
        for (Property p: l) {
            if (propNames.contains(p.getName())) {
                result.add(p);
            }
        }
        return result;
    }
    // getPropertiesImpl >>>>


    private List<Property> getProperties() throws ServerException {
        if (properties == null) {
            String propertiesJson = ExtendedAttributesExtension.getExtendedAttribute(getFullPath(), propertiesAttribute);
            properties = SerializationUtils.deserializeList(Property.class, propertiesJson);
        }
        return properties;
    }

    /**
     * Gets names of all properties for this item.
     *
     * @return List of all property names for this item.
     * @throws ServerException In case of an error.
     */
    // <<<< getPropertyNamesImpl
    @Override
    public List<Property> getPropertyNames() throws ServerException {
        if (ExtendedAttributesExtension.hasExtendedAttribute(getFullPath(), propertiesAttribute)) {
            String propJson = ExtendedAttributesExtension.getExtendedAttribute(getFullPath(), propertiesAttribute);
            return SerializationUtils.deserializeList(Property.class, propJson);
        }
        return new LinkedList<>();
    }
    // getPropertyNamesImpl >>>>

    /**
     * Check whether client is the lock owner.
     *
     * @throws LockedException in case if not owner.
     * @throws ServerException other errors.
     */
    void ensureHasToken() throws LockedException, ServerException {
        if (!clientHasToken()) {
            throw new LockedException();
        }
    }

    /**
     * Check whether client is the lock owner.
     *
     * @return True if owner, false otherwise.
     * @throws ServerException in case of errors.
     */
    private boolean clientHasToken() throws ServerException {
        getActiveLocks();
        if (activeLocks.size() == 0) {
            return true;
        }
        List<String> clientLockTokens = getEngine().getRequest().getClientLockTokens();
        for (LockInfo lockInfo: activeLocks) {
            if (clientLockTokens.contains(lockInfo.getToken())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Modifies and removes properties for this item.
     *
     * @param setProps Array of properties to be set.
     * @param delProps Array of properties to be removed. {@link Property#getValue()} field is ignored.
     *                 Specifying the removal of a property that does not exist is not an error.
     * @throws LockedException      this item was locked and client did not provide lock token.
     * @throws MultistatusException If update fails for a property, this exception shall be thrown and contain
     *                              result of the operation for each property.
     * @throws ServerException      In case of other error.
     */
    // <<<< updatePropertiesImpl
    @Override
    public void updateProperties(Property[] setProps, Property[] delProps)
            throws LockedException, MultistatusException, ServerException {
        ensureHasToken();
        for (final Property prop : setProps) {
            String basicAttributeNS = "urn:schemas-microsoft-com:";
            // Microsoft Mini-redirector may update file creation date, modification date and access time passing properties:
            // <Win32CreationTime xmlns="urn:schemas-microsoft-com:">Thu, 28 Mar 2013 20:15:34 GMT</Win32CreationTime>
            // <Win32LastModifiedTime xmlns="urn:schemas-microsoft-com:">Thu, 28 Mar 2013 20:36:24 GMT</Win32LastModifiedTime>
            // <Win32LastAccessTime xmlns="urn:schemas-microsoft-com:">Thu, 28 Mar 2013 20:36:24 GMT</Win32LastAccessTime>
            // In this case update creation and modified date in your storage or do not save this properties at all, otherwise
            // Windows Explorer will display creation and modification date from this props and it will differ from the values
            // in the Created and Modified fields in your storage
            // String basicAttributeNS = "urn:schemas-microsoft-com:";
            if (prop.getNamespace().equals(basicAttributeNS)) {
                updateBasicProperties(prop.getValue(), prop.getName());
            } else {
                properties = getProperties();
                Property existingProp = null;
                for (Property p: properties) {
                    if (p.getName().equals(prop.getName())) {
                        existingProp = p;
                    }
                }
                if (existingProp != null) {
                    existingProp.setValue(prop.getValue());
                } else {
                    properties.add(prop);
                }
            }
        }
        properties = getProperties();
        Set<String> propNamesToDel = new LinkedHashSet<>();
        for (Property p: delProps) {
            propNamesToDel.add(p.getName());
        }

        List<Property> propToLeft = new ArrayList<>();
        for (Property p: properties) {
            if (!propNamesToDel.contains(p.getName())) {
                propToLeft.add(p);
            }
        }
        ExtendedAttributesExtension.setExtendedAttribute(getFullPath(), propertiesAttribute, SerializationUtils.serialize(propToLeft));
    }
    // updatePropertiesImpl >>>>

    /**
     * Updates basic file times in the following format - Thu, 28 Mar 2013 20:15:34 GMT.
     *
     * @param date  Date to update
     * @param field Field to update
     */
    private void updateBasicProperties(String date, String field) {
        // Not used for android
    }

    /**
     * Returns File System engine.
     *
     * @return File System engine.
     */
    WebDavEngine getEngine() {
        return engine;
    }

    /**
     * Returns full path in the File System to the {@link HierarchyItemImpl}.
     *
     * @return Full path in the File System to the {@link HierarchyItemImpl}.
     */
    String getFullPath() {
        String fullPath = "";
        try {
            fullPath = getRootFolder() + HierarchyItemImpl.decodeAndConvertToPath(getPath());
        } catch (ServerException ignored) {
        }
        return fullPath;
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
    // <<<< lockImpl
    @Override
    public LockResult lock(boolean shared, boolean deep, long timeout, String owner)
            throws LockedException, MultistatusException, ServerException {
        if (hasLock(shared)) {
            throw new LockedException();
        }
        String token = UUID.randomUUID().toString();
        if (timeout < 0 || timeout == Long.MAX_VALUE) {
            // If timeout is absent or infinity timeout requested,
            // grant 5 minute lock.
            timeout = 300;
        }
        long expires = System.currentTimeMillis() + timeout * 1000;
        LockInfo lockInfo = new LockInfo(shared, deep, token, expires, owner);
        activeLocks.add(lockInfo);
        ExtendedAttributesExtension.setExtendedAttribute(getFullPath(), activeLocksAttribute, SerializationUtils.serialize(activeLocks));
        return new LockResult(token, timeout);
    }
    // lockImpl >>>>

    /**
     * Checks whether {@link HierarchyItemImpl} has a lock and whether it is shared.
     *
     * @param skipShared Indicates whether to skip shared.
     * @return True if item has lock and skipShared is true, false otherwise.
     * @throws ServerException in case of errors.
     */
    private boolean hasLock(boolean skipShared) throws ServerException {
        getActiveLocks();
        return !activeLocks.isEmpty() && !(skipShared && activeLocks.get(0).isShared());
    }

    /**
     * Gets the array of all locks for this item.
     *
     * @return Array of locks.
     * @throws ServerException In case of an error.
     */
    // <<<< getActiveLocksImpl
    @Override
    public List<LockInfo> getActiveLocks() throws ServerException {
        if (activeLocks == null) {
            String activeLocksJson = ExtendedAttributesExtension.getExtendedAttribute(getFullPath(), activeLocksAttribute);
            List<LockInfo> lockInfos = SerializationUtils.deserializeList(LockInfo.class, activeLocksJson);
            activeLocks = new LinkedList<>();
            for (LockInfo lock: lockInfos) {
                if (System.currentTimeMillis() < lock.getTimeout()) {
                    activeLocks.add(lock);
                }
            }
        } else {
            activeLocks = new LinkedList<>();
        }
        return activeLocks;
    }
    // getActiveLocksImpl >>>>

    /**
     * Removes lock with the specified token from this item.
     *
     * @param lockToken Lock with this token should be removed from the item.
     * @throws PreconditionFailedException Included lock token was not enforceable on this item.
     * @throws ServerException             In case of an error.
     */
    // <<<< unlockImpl
    @Override
    public void unlock(String lockToken) throws PreconditionFailedException,
            ServerException {
        getActiveLocks();
        LockInfo lock = null;
        for (LockInfo lockInfo: activeLocks) {
            if (lockInfo.getToken().equals(lockToken)) {
                lock = lockInfo;
                break;
            }
        }
        if (lock != null) {
            activeLocks.remove(lock);
            if (!activeLocks.isEmpty()) {
                ExtendedAttributesExtension.setExtendedAttribute(getFullPath(), activeLocksAttribute, SerializationUtils.serialize(activeLocks));
            } else {
                ExtendedAttributesExtension.deleteExtendedAttribute(getFullPath(), activeLocksAttribute);
            }
        } else {
            throw new PreconditionFailedException();
        }
    }
    // unlockImpl >>>>

    /**
     * Updates lock timeout information on this item.
     *
     * @param token   The lock token associated with a lock.
     * @param timeout Lock expiration time in seconds. Negative value means never.
     * @return Actually applied lock (Server may modify timeout).
     * @throws PreconditionFailedException Included lock token was not enforceable on this item.
     * @throws ServerException             In case of an error.
     */
    // <<<< refreshLockImpl
    @Override
    public RefreshLockResult refreshLock(String token, long timeout)
            throws PreconditionFailedException, ServerException {
        getActiveLocks();
        LockInfo lockInfo = null;
        for (LockInfo lock: activeLocks) {
            if (lock.getToken().equals(token)) {
                lockInfo = lock;
                break;
            }
        }
        if (lockInfo == null) {
            throw new PreconditionFailedException();
        }
        if (timeout < 0 || timeout == Long.MAX_VALUE) {
            // If timeout is absent or infinity timeout requested,
            // grant 5 minute lock.
            timeout = 300;
        }
        long expires = System.currentTimeMillis() + timeout * 1000;
        lockInfo.setTimeout(expires);
        ExtendedAttributesExtension.setExtendedAttribute(getFullPath(), activeLocksAttribute, SerializationUtils.serialize(activeLocks));
        return new RefreshLockResult(lockInfo.isShared(), lockInfo.isDeep(),
                timeout, lockInfo.getOwner());
    }
    // refreshLockImpl >>>>

}

package com.ithit.webdav.samples.springboots3.impl;

import com.ithit.webdav.server.*;
import com.ithit.webdav.server.exceptions.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for WebDAV items (folders, files, etc).
 */
public abstract class HierarchyItemImpl implements HierarchyItem, Lock {

    private final String path;
    private final long created;
    private final long modified;
    private final WebDavEngine engine;
    private String name;
    final String activeLocksAttribute = "Locks";
    private final String propertiesAttribute = "Properties";
    private List<Property> properties;
    private List<LockInfo> activeLocks;

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
     */
    @Override
    public long getCreated() {
        return created;
    }

    /**
     * Gets the last modification date of the item in repository expressed as the coordinated universal time (UTC).
     *
     * @return Modification date of the item.
     */
    @Override
    public long getModified() {
        return modified;
    }

    /**
     * Gets the name of the item in repository.
     *
     * @return Name of this item.
     */
    @Override
    public String getName() {
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
    @Override
    public List<Property> getProperties(Property[] props) throws ServerException {
        List<Property> l = getPropertyNames();
        List<Property> result;
        if (props == null) {
            return l;
        }
        Set<String> propNames = Arrays.stream(props).map(Property::getName).collect(Collectors.toSet());
        result = l.stream().filter(x -> propNames.contains(x.getName())).collect(Collectors.toList());
        return result;
    }


    private List<Property> getProperties() throws ServerException {
        if (properties == null) {
            String propertiesJson = getEngine().getDataClient().getMetadata(getPath(), propertiesAttribute);
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
    @Override
    public List<Property> getPropertyNames() throws ServerException {
        String propJson = getEngine().getDataClient().getMetadata(getPath(), propertiesAttribute);
        return SerializationUtils.deserializeList(Property.class, propJson);
    }

    /**
     * Check whether client is the lock owner.
     *
     * @throws LockedException in case if not owner.
     * @throws ServerException other errors.
     */
    void ensureHasToken() throws LockedException, ServerException {
        if (!clientHasToken())
            throw new LockedException();
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
        List<String> clientLockTokens = DavContext.currentRequest().getClientLockTokens();
        return activeLocks.stream().anyMatch(x -> clientLockTokens.contains(x.getToken()));
    }

    /**
     * Modifies and removes properties for this item.
     *
     * @param setProps Array of properties to be set.
     * @param delProps Array of properties to be removed. {@link Property#getXmlValueRaw()} field is ignored.
     *                 Specifying the removal of a property that does not exist is not an error.
     * @throws LockedException this item was locked and client did not provide lock token.
     * @throws ServerException In case of other error.
     */
    @Override
    public void updateProperties(Property[] setProps, Property[] delProps)
            throws LockedException, ServerException {
        ensureHasToken();
        for (final Property prop : setProps) {
            properties = getProperties();
            Property existingProp = properties.stream().filter(x -> x.getName().equals(prop.getName())).findFirst().orElse(null);
            if (existingProp != null) {
                existingProp.setXmlValueRaw(prop.getXmlValueRaw());
            } else {
                properties.add(prop);
            }
        }
        properties = getProperties();
        Set<String> propNamesToDel = Arrays.stream(delProps).map(Property::getName).collect(Collectors.toSet());
        properties = properties.stream()
                .filter(e -> !propNamesToDel.contains(e.getName()))
                .collect(Collectors.toList());
        getEngine().getDataClient().setMetadata(getPath(), propertiesAttribute, SerializationUtils.serialize(properties));
        getEngine().getWebSocketServer().notifyUpdated(getPath());
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
     * Locks this item.
     *
     * @param shared  Indicates whether a lock is shared or exclusive.
     * @param deep    Indicates whether a lock is enforceable on the subtree.
     * @param timeout Lock expiration time in seconds. Negative value means never.
     * @param owner   Provides information about the principal taking out a lock.
     * @return Actually applied lock (Server may modify timeout).
     * @throws LockedException The item is locked, so the method has been rejected.
     * @throws ServerException In case of an error.
     */
    @Override
    public LockResult lock(boolean shared, boolean deep, long timeout, String owner)
            throws LockedException, ServerException {
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
        getEngine().getDataClient().setMetadata(getPath(), activeLocksAttribute, SerializationUtils.serialize(activeLocks));
        getEngine().getWebSocketServer().notifyLocked(getPath());
        return new LockResult(token, timeout);
    }

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
    @Override
    public List<LockInfo> getActiveLocks() throws ServerException {
        if (activeLocks == null) {
            String activeLocksJson = getEngine().getDataClient().getMetadata(getPath(), activeLocksAttribute);
            activeLocks = SerializationUtils.deserializeList(LockInfo.class, activeLocksJson)
                    .stream()
                    .filter(x -> System.currentTimeMillis() < x.getTimeout())
                    .map(lock -> new LockInfo(
                            lock.isShared(),
                            lock.isDeep(),
                            lock.getToken(),
                            (lock.getTimeout() < 1 || lock.getTimeout() == Long.MAX_VALUE) ? lock.getTimeout() : (lock.getTimeout() - System.currentTimeMillis()) / 1000,
                            lock.getOwner())
                    )
                    .collect(Collectors.toList());
        } else {
            activeLocks = new LinkedList<>();
        }
        return activeLocks;
    }

    /**
     * Removes lock with the specified token from this item.
     *
     * @param lockToken Lock with this token should be removed from the item.
     * @throws PreconditionFailedException Included lock token was not enforceable on this item.
     * @throws ServerException             In case of an error.
     */
    @Override
    public void unlock(String lockToken) throws PreconditionFailedException,
            ServerException {
        getActiveLocks();
        LockInfo lock = activeLocks.stream().filter(x -> x.getToken().equals(lockToken)).findFirst().orElse(null);
        if (lock != null) {
            activeLocks.remove(lock);
            if (!activeLocks.isEmpty()) {
                getEngine().getDataClient().setMetadata(getPath(), activeLocksAttribute, SerializationUtils.serialize(activeLocks));
            } else {
                getEngine().getDataClient().setMetadata(getPath(), activeLocksAttribute, null);
            }
            getEngine().getWebSocketServer().notifyUnlocked(getPath());
        } else {
            throw new PreconditionFailedException();
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
    @Override
    public RefreshLockResult refreshLock(String token, long timeout)
            throws PreconditionFailedException, ServerException {
        getActiveLocks();
        LockInfo lockInfo = activeLocks.stream().filter(x -> x.getToken().equals(token)).findFirst().orElse(null);
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
        getEngine().getDataClient().setMetadata(getPath(), activeLocksAttribute, SerializationUtils.serialize(activeLocks));
        getEngine().getWebSocketServer().notifyLocked(getPath());
        return new RefreshLockResult(lockInfo.isShared(), lockInfo.isDeep(),
                timeout, lockInfo.getOwner());
    }
}

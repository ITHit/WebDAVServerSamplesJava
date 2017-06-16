package com.ithit.webdav.samples.oraclestorageservlet;

import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.LockInfo;
import com.ithit.webdav.server.Property;
import com.ithit.webdav.server.exceptions.ServerException;
import com.ithit.webdav.server.exceptions.WebDavStatus;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Helper class to work with DataBase access.
 */
public class DataAccess {

    private WebDavEngine engine;
    private Connection currentConnection;
    private String defaultTableSpace;
    private long totalBytes;

    /**
     * Initialize {@link DataAccess} with {@link WebDavEngine}.
     *
     * @param engine {@link WebDavEngine}.
     */
    DataAccess(WebDavEngine engine) {
        this.engine = engine;
        try {
            defaultTableSpace = executeScalar("SELECT DEFAULT_TABLESPACE FROM DBA_USERS WHERE USERNAME = (SELECT USER FROM dual)");
            totalBytes = getTotalBytesDB();
        } catch (Exception e) {
            engine.getLogger().logError(e.getMessage(), e);
        }
    }

    private long getTotalBytesDB() {
        try {
            BigDecimal bytes = executeScalar("SELECT sum(bytes)" +
                    "FROM dba_data_files " +
                    "where tablespace_name=? " +
                    "GROUP BY tablespace_name", defaultTableSpace);
            return bytes.longValue();
        } catch (Exception e) {
            engine.getLogger().logError(e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Returns connection to the DB.
     *
     * @return Connection.
     * @throws ServerException in case of errors.
     */
    private Connection getConnection() throws ServerException {
        if (currentConnection == null) {
            try {
                Locale.setDefault(Locale.US);
                Context initContext = new InitialContext();
                Context envContext = (Context) initContext.lookup("java:comp/env");
                DataSource ds = (DataSource) envContext.lookup("jdbc/Oracle");

                currentConnection = ds.getConnection();
                currentConnection.setAutoCommit(false);

            } catch (SQLException | NamingException e) {
                throw new ServerException(e);
            }
        }

        return currentConnection;
    }

    /**
     * Returns default table space in which DB is stored.
     * @return Default table space in which DB is stored.
     */
    String getDefaultTableSpace() {
        return defaultTableSpace;
    }

    /**
     * Returns total bytes used by DB.
     * @return Total bytes used by DB.
     */
    long getTotalBytes() {
        return totalBytes;
    }

    /**
     * Releases connection to the underlying DB.
     */
    void closeConnection() {
        try {
            if (currentConnection != null)
                currentConnection.close();
        } catch (SQLException e) {
            engine.getLogger().logError("Failed to rollback connection", e);
        }
    }

    /**
     * Commits all the changes made to the DB.
     *
     * @throws ServerException in case of DB exception.
     */
    void commit() throws ServerException {
        try {
            if (currentConnection != null)
                currentConnection.commit();
        } catch (SQLException ex) {
            throw new ServerException(ex);
        }
    }

    /**
     * Executes sql against DB and reads response as an {@link HierarchyItemImpl}.
     *
     * @param sql        Query to be executed.
     * @param path       Path to the item.
     * @param parentPath Indicates whether it is path to the item or to the parent folder.
     * @param args       Query arguments.
     * @return HierarchyItemImpl or null if nothing.
     * @throws ServerException in case of DB exception.
     */
    HierarchyItemImpl readItem(String sql, String path, boolean parentPath, Object... args) throws ServerException {
        List<HierarchyItemImpl> items = readItems(sql, path, parentPath, args);
        return items.size() != 0 ? items.get(0) : null;
    }

    /**
     * Executes sql against DB and reads response as an {@link HierarchyItemImpl}.
     *
     * @param sql        Query to be executed.
     * @param path       Path to the item.
     * @param parentPath Indicates whether it is path to the item or to the parent folder.
     * @param args       Query arguments.
     * @return HierarchyItemImpl list.
     * @throws ServerException in case of DB exception.
     */
    List<HierarchyItemImpl> readItems(String sql, final String path, final boolean parentPath, Object... args) throws ServerException {

        ElementReader<HierarchyItemImpl> elementReader = rs -> {
            int itemID = rs.getInt("ID");
            int parentId = rs.getInt("Parent");
            int itemType = rs.getByte("ItemType");
            String itemName = rs.getString("Name");
            long itemCreated = rs.getTimestamp("Created").getTime();
            long itemModified = rs.getTimestamp("Modified").getTime();
            long lastChunkSaved = rs.getTimestamp("LastChunkSaved").getTime();
            long totalContentLength = rs.getLong("TotalContentLength");
            String encodedName = encode(itemName);
            String itemPath = parentPath ? (path.endsWith("/") ? path + encodedName : path + "/" + encodedName) : path;
            switch (itemType) {
                case ItemType.File:
                    return new FileImpl(itemID, parentId, itemName, itemPath, itemCreated, itemModified, lastChunkSaved, totalContentLength, engine);
                case ItemType.Folder:
                    if (!itemPath.endsWith("/"))
                        itemPath = itemPath + "/";
                    return new FolderImpl(itemID, parentId, itemName, itemPath, itemCreated, itemModified, engine);
                default:
                    return null;
            }

        };

        return readObjects(sql, elementReader, args);
    }

    /**
     * Encodes string to safe characters.
     *
     * @param val String to encode.
     * @return Encoded string.
     * @throws ServerException in case of error during encoding.
     */
    String encode(String val) throws ServerException {
        try {
            return URLEncoder.encode(val, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new ServerException(e);
        }
    }

    /**
     * Read DB for {@link LockInfo}.
     *
     * @param sql  query executed against Lock table.
     * @param args query arguments.
     * @return LockInfo.
     * @throws ServerException in case of DB exception.
     */
    List<LockInfo> readLocks(String sql, Object... args) throws ServerException {
        ElementReader<LockInfo> elementReader = rs -> {
            LockInfo li = new LockInfo();
            li.setToken(rs.getString("Token"));
            li.setShared(rs.getBoolean("Shared"));
            li.setDeep(rs.getBoolean("Deep"));
            if (rs.getDate("Expires") == null) {
                li.setTimeout(-1);
            } else {
                java.util.Date expires = new java.util.Date(rs.getTimestamp("Expires").getTime());
                if (expires.getTime() <= new java.util.Date().getTime()) {
                    li.setTimeout(0);
                } else
                    li.setTimeout((expires.getTime() - new java.util.Date().getTime()) / 1000);
            }
            li.setOwner(rs.getString("Owner"));

            if (li.getTimeout() != 0)
                return li;
            return null;
        };

        return readObjects(sql, elementReader, args);
    }

    /**
     * Read DB for {@link Property}.
     *
     * @param sql  Query executed against Lock table.
     * @param args Query arguments.
     * @return Property.
     * @throws ServerException in case of DB exception.
     */
    List<Property> readProperties(String sql, Object... args) throws ServerException {
        ElementReader<Property> elementReader = rs -> new Property(rs.getString("Namespace"),
                rs.getString("Name"),
                rs.getString("PropVal"));

        return readObjects(sql, elementReader, args);
    }

    /**
     * Sets parameters to the DB statement.
     *
     * @param statement Statement to execute.
     * @param args      Parameters.
     * @throws SQLException in case of DB exception.
     */
    private void setParams(PreparedStatement statement, Object... args) throws SQLException {
        int i = 1;
        for (Object o : args) {
            statement.setObject(i++, o);
        }
    }

    /**
     * Creates {@link HierarchyItemImpl} instance by path.
     *
     * @param path Item relative path.
     * @return Instance of corresponding {@link HierarchyItemImpl} or null if item is not found.
     * @throws ServerException in case if cannot read file attributes.
     */
    HierarchyItemImpl getHierarchyItem(String path) throws ServerException {
        if (path.equals("/")) {
            return readItem("SELECT ID, Parent, ItemType, Name, Created, Modified, LastChunkSaved, TotalContentLength"
                    + " FROM Repository"
                    + " WHERE ID = 0", path, false);
        } else {
            int id = 0;
            String[] names = path.split("/");
            //decode parts
            for (int i = 0; i < names.length; i++) {
                names[i] = decode(names[i]);
            }
            int last = names.length - 1;
            while (last > 0 && names[last].equals("")) last--;
            // search for item by path

            for (int i = 0; i < last; i++)
                if (!names[i].equals("")) {
                    Integer res = executeInt("SELECT ID FROM Repository"
                            + " WHERE Name = ?"
                            + " AND Parent = ?", names[i], id);
                    if (res == null)
                        return null;
                    else
                        id = res;
                }
            // get item properties
            return readItem("SELECT ID, Parent, ItemType, Name, Created, Modified, LastChunkSaved, TotalContentLength"
                    + " FROM Repository"
                    + " WHERE Name = ?"
                    + " AND Parent = ?", path, false, names[last], id);

        }
    }

    /**
     * Reads field from DB table.
     *
     * @param sql  Query to execute.
     * @param args Query arguments.
     * @return Field value.
     * @throws ServerException in case of DB errors.
     */
    @SuppressWarnings("unchecked")
    <T> T executeScalar(String sql, Object... args) throws ServerException {
        ElementReader<Object> elementReader = rs -> rs.getObject(1);

        List<Object> res = readObjects(sql, elementReader, args);
        return res.size() == 0 ? null : (T) res.get(0);
    }

    /**
     * Reads integer field from DB table.
     *
     * @param sql  Query to execute.
     * @param args Query arguments.
     * @return Field value.
     * @throws ServerException in case of DB errors.
     */
    Integer executeInt(String sql, Object... args) throws ServerException {
        ElementReader<Integer> elementReader = rs -> rs.getInt(1);

        List<Integer> res = readObjects(sql, elementReader, args);
        return res.size() == 0 ? null : res.get(0);
    }

    /**
     * Reads objects from DB using provided {@link ElementReader}.
     *
     * @param sql      Query to execute.
     * @param elReader {@link ElementReader} to read object.
     * @param args     Query arguments.
     * @return List of object associated with DB row.
     * @throws ServerException in case of DB errors.
     */
    private <T> List<T> readObjects(String sql, ElementReader<T> elReader, Object... args) throws ServerException {

        List<T> res = new ArrayList<>();
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {

            setParams(statement, args);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    T item = elReader.readItem(result);
                    if (item != null)
                        res.add(item);
                }
            }
        } catch (SQLException ex) {
            throw new ServerException(ex);
        }

        return res;

    }

    /**
     * Updates data into DB.
     *
     * @param sql  Query to execute.
     * @param args Query parameters.
     * @throws ServerException in case of DB errors.
     */
    void executeUpdate(String sql, Object... args) throws ServerException {

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            setParams(statement, args);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new ServerException(ex);
        }
    }

    /**
     * Returns {@link HierarchyItemImpl} as {@link FolderImpl} if it is folder throws exception otherwise.
     *
     * @param item Item to cast to {@link FolderImpl}.
     * @return FolderImpl.
     * @throws ServerException if item is not a {@link FolderImpl}.
     */
    FolderImpl getFolderImpl(HierarchyItem item) throws ServerException {
        FolderImpl destFolder = item instanceof FolderImpl ? (FolderImpl) item : null;
        if (destFolder == null)
            throw new ServerException(WebDavStatus.CONFLICT);

        return destFolder;
    }

    /**
     * Interface that helps to read object from DB.
     *
     */
    interface ElementReader<T> {
        /**
         * Reads row from DB to the object.
         *
         * @param rs ResultSet at the position.
         * @return T Object.
         * @throws SQLException    in case od DB exception.
         * @throws ServerException in case of other errors.
         */
        T readItem(ResultSet rs) throws SQLException, ServerException;
    }

    /**
     * Decodes URL.
     *
     * @param URL to decode.
     * @return Path as string.
     */
    String decode(String URL) {
        String path = "";
        try {
            path = URLDecoder.decode(URL.replaceAll("\\+", "%2B"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("UTF-8 encoding can not be used to decode " + URL);
        }
        return path;
    }


    /**
     * Gets the List of all files in the system.
     *
     * @return List of {@link HierarchyItemImpl} objects. Each item is a {@link FileImpl} item.
     */
    List<HierarchyItemImpl> getFiles()  {
        try {
            return readItems("SELECT ID, Parent, ItemType, Name, Created, Modified, LastChunkSaved, TotalContentLength"
                    + " FROM Repository"
                    + " WHERE Name <> 'Root'", "/", false);
        } catch (ServerException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Gets {@link HierarchyItem} from DB by id and specified path.
     * @param id File id.
     * @param path Path of file.
     * @return HierarchyItem.
     */
    HierarchyItem getFile(int id, String path)  {
        HierarchyItem result = null;
        try {
            List<HierarchyItemImpl> hierarchyItems = readItems("SELECT ID, Parent, ItemType, Name, Created, Modified, LastChunkSaved, TotalContentLength"
                    + " FROM Repository"
                    + " WHERE id = ?", path, false, id);
            if (hierarchyItems != null && !hierarchyItems.isEmpty()) {
                result = hierarchyItems.get(0);

            }
        } catch (ServerException e) {
            engine.getLogger().logError(e.getMessage(), e);
        }
        return result;
    }
}

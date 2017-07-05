package com.ithit.webdav.samples.deltavservlet;

import com.ithit.webdav.server.Engine;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.deltav.AutoVersion;
import com.ithit.webdav.server.exceptions.ServerException;
import com.ithit.webdav.server.util.StringUtil;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.Tika;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import static com.ithit.webdav.samples.deltavservlet.Indexer.MAX_CONTENT_LENGTH;

/**
 * Implementation if {@link Engine}.
 * Resolves hierarchy items by paths.
 */
public class WebDavEngine extends Engine {

    private WebSocketServer webSocketServer;
    private HttpServletRequest request;
    private final Logger logger;
    private final String license;
    private DataAccess dataAccess;
    private Indexer indexer;
    private Searcher searcher;
    private AutoVersion autoVersionMode;
    private boolean autoputUnderVersionControl;

    /**
     * Initializes a new instance of the WebDavEngine class.
     *
     * @param logger  Where to log messages.
     * @param license License string.
     */
    WebDavEngine(Logger logger, String license) {
        this.logger = logger;
        this.license = license;
    }

    /**
     * Set original servlet request.
     *
     * @param httpServletRequest Original servlet request.
     */
    void setServletRequest(HttpServletRequest httpServletRequest) {
        this.request = httpServletRequest;
    }

    /**
     * Creates {@link HierarchyItem} instance by path.
     *
     * @param pathAndQuery Item relative path including query string.
     * @return Instance of corresponding {@link HierarchyItem} or null if item is not found.
     * @throws ServerException in case if cannot read file attributes.
     */
    @Override
    public HierarchyItem getHierarchyItem(String pathAndQuery) throws ServerException {
        if (pathAndQuery.contains("?version=")) // Version
        {
            String versionNum = getQueryMap(pathAndQuery).get("version");
            String itemPath = pathAndQuery.substring(0, pathAndQuery.indexOf('?'));

            HierarchyItemImpl item = dataAccess.getHierarchyItem(itemPath);
            if (item == null)
                return null;

            String commandText = "SELECT Id, ItemId, VersionNumber, Name, Created"
                    + " FROM Version"
                    + " WHERE ItemId = ?"
                    + " AND VersionNumber = ?";

            List<VersionImpl> versions = dataAccess.readVersions(commandText, itemPath, item.getId(), versionNum);
            if (versions.size() > 0)
                return versions.get(0);
        } else if (pathAndQuery.contains("?history")) // History
        {
            String itemPath = pathAndQuery.substring(0, pathAndQuery.indexOf('?'));
            HierarchyItemImpl item = dataAccess.getHierarchyItem(itemPath);

            if (item instanceof FileImpl) {
                FileImpl file = (FileImpl) item;
                return file.getVersionHistory();
            }
        } else {
            int ind = pathAndQuery.indexOf('?');
            if (ind > 0)
                pathAndQuery = pathAndQuery.substring(0, ind);

            return dataAccess.getHierarchyItem(pathAndQuery);
        }

        return null;
    }

    /**
     * Creates map from the URL parameters.
     * @param query URL with all the parameters.
     * @return Name value map with URL parameters.
     */
    private Map<String, String> getQueryMap(String query) {
        query = query.split("\\?")[1];
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }

    /**
     * Returns logger that will be used by engine.
     *
     * @return Instance of {@link Logger}.
     */
    @Override
    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns license string.
     *
     * @return License string.
     */
    @Override
    public String getLicense() {
        return license;
    }

    /**
     * Indicates whether to auto put item under version control.
     * @return True if yes, false otherwise.
     */
    @Override
    public boolean getAutoPutUnderVersionControl() {
        return autoputUnderVersionControl;
    }

    /**
     * Set whether to auto put item under version control.
     * @param autoputUnderVersionControl True if yes, false otherwise.
     */
    void setAutoPutUnderVersionControl(boolean autoputUnderVersionControl) {
        this.autoputUnderVersionControl = autoputUnderVersionControl;
    }

    /**
     * Returns {@link DataAccess} helper for DB operations.
     *
     * @return DataAccess.
     */
    DataAccess getDataAccess() {
        return dataAccess;
    }

    /**
     * Sets the {@link DataAccess}.
     *
     * @param dataAccess DataAccess to set.
     */
    void setDataAccess(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    /**
     * Returns original {@link HttpServletRequest}.
     *
     * @return Returns original {@link HttpServletRequest}.
     */
    HttpServletRequest getRequest() {
        return request;
    }

    /**
     * Returns {@link AutoVersion} rule for version mode.
     * @return  {@link AutoVersion} rule for version mode.
     */
    AutoVersion getAutoVersionMode() {
        return autoVersionMode;
    }

    /**
     * Sets {@link AutoVersion} rule for version mode.
     * @param autoVersionMode  {@link AutoVersion} rule for version mode.
     */
    void setAutoVersionMode(AutoVersion autoVersionMode) {
        this.autoVersionMode = autoVersionMode;
    }

    /**
     * Returns {@link Indexer}.
     * @return {@link Indexer}.
     */
    Indexer getIndexer() {
        return indexer;
    }

    /**
     * Returns {@link Searcher}.
     * @return {@link Searcher}.
     */
    Searcher getSearcher() {
        return searcher;
    }

    void setWebSocketServer(WebSocketServer webSocketServer) {
        this.webSocketServer = webSocketServer;
    }

    /**
     * Build initial index of root folder.
     * @param indexFolder Index folder.
     * @param interval Daemon commit interval.
     */
    void indexRootFolder(String indexFolder, Integer interval) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        Directory fsDir;
        try {
            fsDir = FSDirectory.open(Paths.get(indexFolder));
            StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
            IndexWriterConfig conf = new IndexWriterConfig(standardAnalyzer);
            conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            IndexWriter indexWriter = new IndexWriter(fsDir, conf);
            Tika tika = new Tika();
            tika.setMaxStringLength(MAX_CONTENT_LENGTH);
            indexer = new Indexer(indexWriter, getFilesToIndex(), getLogger(), tika);
            forkJoinPool.invoke(indexer);
            indexWriter.commit();
            new Indexer.CommitTask(indexWriter, getLogger()).schedule(interval);
            searcher = new Searcher(indexFolder, standardAnalyzer, getLogger());
        } catch (IOException e) {
            logger.logError("Cannot initialize Lucene", e);
        }
    }

    /**
     * Builds list of the all files stored in DB.
     */
    private List<HierarchyItemImpl> getFilesToIndex() {
        return dataAccess.getFiles();
    }

    void notifyRefresh(String folder) {
        folder = StringUtil.trimEnd(StringUtil.trimStart(folder, "/"), "/");
        notify("refresh", folder);
    }

    void notifyDelete(String folder) {
        folder = StringUtil.trimEnd(StringUtil.trimStart(folder, "/"), "/");
        notify("delete", folder);
    }

    private void notify(String type, String folder) {
        if (webSocketServer != null) {
            webSocketServer.send(type, folder);
        }
    }
}

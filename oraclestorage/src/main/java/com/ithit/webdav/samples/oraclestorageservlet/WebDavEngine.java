package com.ithit.webdav.samples.oraclestorageservlet;

import com.ithit.webdav.server.Engine;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Logger;
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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import static com.ithit.webdav.samples.oraclestorageservlet.Indexer.MAX_CONTENT_LENGTH;

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
     * @param contextPath Item relative path including query string.
     * @return Instance of corresponding {@link HierarchyItem} or null if item is not found.
     * @throws ServerException in case if cannot read file attributes.
     */
    @Override
    public HierarchyItem getHierarchyItem(String contextPath) throws ServerException {
        int i = contextPath.indexOf("?");
        if (i >= 0) {
            contextPath = contextPath.substring(0, i);
        }
        return dataAccess.getHierarchyItem(contextPath);
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
     * @return license string.
     */
    @Override
    public String getLicense() {
        return license;
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

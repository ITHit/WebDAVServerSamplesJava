package com.ithit.webdav.samples.fsstorageservlet;

import com.ithit.webdav.server.Engine;
import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.exceptions.ServerException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.Tika;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

import static com.ithit.webdav.samples.fsstorageservlet.Indexer.MAX_CONTENT_LENGTH;

/**
 * Implementation if {@link Engine}.
 * Resolves hierarchy items by paths.
 */
public class WebDavEngine extends Engine {

    private final Logger logger;
    private final String license;
    private HttpServletRequest request;
    private Indexer indexer;
    private Searcher searcher;

    /**
     * Initializes a new instance of the WebDavEngine class.
     *
     * @param logger                        Where to log messages.
     * @param license                       License string.
     */
    WebDavEngine(Logger logger, String license) {
        this.logger = logger;
        this.license = license;
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
        HierarchyItemImpl item;
        item = FolderImpl.getFolder(contextPath, this);
        if (item != null) {
            return item;
        }
        item = FileImpl.getFile(contextPath, this);
        if (item != null) {
            return item;
        }
        getLogger().logDebug("Could not find item that corresponds to path: " + contextPath);
        return null; // no hierarchy item that corresponds to path parameter was found in the repository
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
     * Returns original servlet request.
     *
     * @return Original servlet request.
     */
    HttpServletRequest getRequest() {
        return request;
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


    /**
     * This task is running in background and indexes all folder mapped for WebDav asynchronously.
     * So there uis a chance that in case of big folder there will be some delay while this task
     * will finish indexation.
     */
    private class IndexTask extends TimerTask {

        private String dataFolder;
        private String indexFolder;
        private Integer interval;

        /**
         * Build initial index of root folder.
         * @param dataFolder Root folder.
         * @param indexFolder Index folder.
         * @param interval Daemon commit interval.
         */
        IndexTask(String dataFolder, String indexFolder, Integer interval) {
            this.dataFolder = dataFolder;
            this.indexFolder = indexFolder;
            this.interval = interval;
        }

        /**
         * The action to be performed by this timer task.
         */
        @Override
        public void run() {
            List<HierarchyItem> filesToIndex = new ArrayList<>();
            File data = new File(dataFolder);
            StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
            searcher = new Searcher(indexFolder, standardAnalyzer, getLogger());
            getFilesToIndex(data.listFiles(), filesToIndex, dataFolder);
            ForkJoinPool forkJoinPool = new ForkJoinPool(4);
            Directory fsDir;
            try {
                fsDir = FSDirectory.open(Paths.get(indexFolder));
                IndexWriterConfig conf = new IndexWriterConfig(standardAnalyzer);
                conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                IndexWriter indexWriter = new IndexWriter(fsDir, conf);
                Tika tika = new Tika();
                tika.setMaxStringLength(MAX_CONTENT_LENGTH);
                indexer = new Indexer(indexWriter, filesToIndex, getLogger(), tika, dataFolder);
                forkJoinPool.execute(indexer);
                indexWriter.commit();
                new Indexer.CommitTask(indexWriter, getLogger()).schedule(interval);
            } catch (Throwable e) {
                logger.logError("Cannot initialize Lucene", e);
            }
        }

        void schedule() {
            Timer timer = new Timer(true);
            timer.schedule(this, 0);
        }
    }

    /**
     * Build initial index of root folder.
     * @param dataFolder Root folder.
     * @param indexFolder Index folder.
     * @param interval Daemon commit interval.
     */
    void indexRootFolder(String dataFolder, String indexFolder, Integer interval) {
        new IndexTask(dataFolder, indexFolder, interval).schedule();
    }

    /**
     * Builds list of the all files (including files in child folders) in the data folder.
     * @param files List of files in root folder.
     * @param result List to be populated with results.
     * @param dataFolder Root folder absolute location.
     */
    void getFilesToIndex(File[] files, List<HierarchyItem> result, String dataFolder) {
        for (File f: files) {
            if (f.isDirectory() && f.canRead() && !f.isHidden()) {
                addFileToTheList(result, dataFolder, f);
                getFilesToIndex(f.listFiles(), result, dataFolder);
            } else {
                if (f.canRead() && !f.isHidden()) {
                    addFileToTheList(result, dataFolder, f);
                }
            }
        }
    }

    private void addFileToTheList(List<HierarchyItem> result, String dataFolder, File f) {
        String quote = Pattern.quote(dataFolder);
        try {
            String context = f.getAbsolutePath().replaceAll("(?i)" + quote, "");
            context = context.replaceAll("\\\\", "/");
            if (f.isDirectory()) {
                context += "/";
            }
            result.add(getHierarchyItem(context));
        } catch (Exception e) {
            getLogger().logDebug("Cannot add file to the list: " + f.getAbsolutePath());
        }
    }
}

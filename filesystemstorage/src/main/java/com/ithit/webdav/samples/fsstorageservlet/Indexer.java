package com.ithit.webdav.samples.fsstorageservlet;

import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.exceptions.ServerException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.RecursiveAction;

/**
 * Indexes files in storage using Apache Lucene engine for indexing and Apache Tika.
 */
class Indexer extends RecursiveAction {
    static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;
    private static final int TASK_INTERVAL = 30 * 1000;
    static final String PATH = "path";
    static final String NAME = "name";
    static final String PARENT_NAME = "parent_name";
    static final String CONTENTS = "contents";
    private IndexWriter indexWriter;
    private List<HierarchyItem> files;
    private Logger logger;
    private Tika tika;
    private String dataRoot;
    private final static int BATCH_SIZE = 100;

    /**
     * Create instance of Indexer file.
     * @param iw {@link IndexWriter} Lucene index writer.
     * @param files List of the file to index.
     * @param logger {@link Logger}.
     * @param tika {@link Tika} to read content.
     * @param dataRoot Files location root folder.
     */
    Indexer(IndexWriter iw, List<HierarchyItem> files, Logger logger, Tika tika, String dataRoot) {
        this.indexWriter = iw;
        this.files = files;
        this.logger = logger;
        this.tika = tika;
        this.dataRoot = dataRoot;
    }

    @Override
    protected void compute() {
        if (files.size() > BATCH_SIZE) {
            List<Indexer> tasks = new ArrayList<>();
            List<List<HierarchyItem>> partitioned = chopped(files, BATCH_SIZE);
            for (List<HierarchyItem> sublist : partitioned) {
                tasks.add(new Indexer(indexWriter, sublist, logger, tika, dataRoot));
            }
            invokeAll(tasks);
        } else {
            for (HierarchyItem f : files) {
                try {
                    indexFile(f.getName(), f.getPath(), null, f);
                } catch (ServerException e) {
                    logger.logDebug("Cannot find path for this file.");
                }
            }
        }
    }

    private static <T> List<List<T>> chopped(List<T> list, final int L) {
        List<List<T>> parts = new ArrayList<>();
        final int N = list.size();
        for (int i = 0; i < N; i += L) {
            parts.add(new ArrayList<>(
                    list.subList(i, Math.min(N, i + L)))
            );
        }
        return parts;
    }

    /**
     * Indexes file.
     * @param fileName File name to add to index.
     * @param currentPath Current relative path of the file.
     * @param oldPath Old relative path of the file if it was moved.
     */
    void indexFile(String fileName, String currentPath, String oldPath, HierarchyItem item) {
        Path fullPath = Paths.get(dataRoot, currentPath);
        try {
            Metadata metadata = new Metadata();
            Document doc = new Document();
            String parentFolder = currentPath.replace(fileName, "").replaceAll("/", "");
            Field pathField = new StringField(PATH, currentPath, Field.Store.YES);
            Field parentField = new TextField(PARENT_NAME, parentFolder, Field.Store.YES);
            Field nameField = new TextField(NAME, fileName, Field.Store.YES);
            doc.add(pathField);
            doc.add(parentField);
            doc.add(nameField);
            if (item != null && item instanceof FileImpl) {
                try (TikaInputStream stream = TikaInputStream.get(fullPath, metadata)) {
                    String content = tika.parseToString(stream, metadata, MAX_CONTENT_LENGTH);
                    doc.add(new TextField(CONTENTS, content, Field.Store.YES));
                } catch (Throwable e) {
                    logger.logError("Error while indexing content: " + fullPath, e);
                }
            }
            if (indexWriter.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                indexWriter.addDocument(doc);
            } else {
                indexWriter.updateDocument(new Term(PATH, oldPath != null ? oldPath : currentPath), doc);
            }
        } catch (Throwable e) {
            logger.logError("Error while indexing file: " + fullPath, e);
        }
    }

    /**
     * Deletes specified file information from the index.
     * @param file {@link FileImpl} to delete from index.
     */
    void deleteIndex(HierarchyItem file) {
        try {
            indexWriter.deleteDocuments(new Term(PATH, file.getPath()));
        } catch (Exception e) {
            logger.logError("Cannot delete index for the file.", e);
        }
    }

    /**
     * Timer task implementation to commit index changes from time to time.
     */
    static class CommitTask extends TimerTask {

        private IndexWriter indexWriter;
        private Logger logger;

        /**
         * Creates instance of {@link CommitTask}.
         * @param indexWriter {@link IndexWriter} Lucene index writer.
         * @param logger {@link Logger}.
         */
        CommitTask(IndexWriter indexWriter, Logger logger) {
            this.indexWriter = indexWriter;
            this.logger = logger;
        }

        /**
         * The action to be performed by this timer task.
         */
        @Override
        public void run() {
            try {
                indexWriter.commit();
            } catch (IOException e) {
                logger.logError("Cannot commit.", e);
            }
        }

        /**
         * Schedule timer executions at the specified Interval.
         * @param interval Timer interval.
         */
        void schedule(Integer interval) {
            Timer timer = new Timer(true);
            timer.scheduleAtFixedRate(this, 0, interval == null ? TASK_INTERVAL : interval * 1000);
        }
    }
}

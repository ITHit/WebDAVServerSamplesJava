package com.ithit.webdav.samples.springbootoracle.impl;

import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.search.SearchOptions;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.Tika;
import org.apache.tika.exception.ZeroByteFileException;
import org.apache.tika.metadata.Metadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Facade that incapsulates all functionality regarding indexing and searching
 */
public class SearchFacade {
    private Indexer indexer;
    private final DataAccess dataAccess;
    private Searcher searcher;
    private final Logger logger;
    private static SearchFacade INSTANCE;
    private volatile boolean indexed = false;

    private SearchFacade(DataAccess dataAccess, Logger logger) {
        this.dataAccess = dataAccess;
        this.logger = logger;
    }

    public synchronized static SearchFacade getInstance(DataAccess dataAccess, Logger logger) {
        if (INSTANCE == null) {
            INSTANCE = new SearchFacade(dataAccess, logger);
        }
        return INSTANCE;
    }

    /**
     * Checks if the searcher already ran overall indexing.
     * @return true if yes.
     */
    public synchronized boolean indexed() {
        return indexed;
    }

    /**
     * Build initial index of root folder.
     *
     * @param indexFolder Index folder.
     * @param interval    Daemon commit interval.
     */
    public void indexRootFolder(String indexFolder, Integer interval) {
        if (!indexed) {
            indexed = true;
            ForkJoinPool forkJoinPool = new ForkJoinPool(4);
            Directory fsDir;
            try {
                fsDir = FSDirectory.open(Paths.get(indexFolder));
                StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
                IndexWriterConfig conf = new IndexWriterConfig(standardAnalyzer);
                conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                IndexWriter indexWriter = new IndexWriter(fsDir, conf);
                Tika tika = new Tika();
                tika.setMaxStringLength(Indexer.MAX_CONTENT_LENGTH);
                indexer = new Indexer(indexWriter, getFilesToIndex(), logger, tika);
                forkJoinPool.invoke(getIndexer());
                indexWriter.commit();
                new Indexer.CommitTask(indexWriter, logger).schedule(interval);
                searcher = new Searcher(indexFolder, standardAnalyzer, logger);
            } catch (IOException e) {
                logger.logError("Cannot initialize Lucene", e);
            }
        }
    }

    /**
     * Builds list of the all files stored in DB.
     */
    private List<HierarchyItemImpl> getFilesToIndex() {
        return dataAccess.getFiles();
    }

    /**
     * Returns Indexer instance
     *
     * @return Indexer instance
     */
    Indexer getIndexer() {
        return indexer;
    }

    /**
     * Returns Searcher instance
     *
     * @return Searcher instance
     */
    Searcher getSearcher() {
        return searcher;
    }

    /**
     * Indexes files in storage using Apache Lucene engine for indexing and Apache Tika.
     */
    static class Indexer extends RecursiveAction {
        static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;
        private static final int TASK_INTERVAL = 30 * 1000;
        static final String ID = "id";
        static final String NAME = "name";
        static final String CONTENTS = "contents";
        private final IndexWriter indexWriter;
        private final List<HierarchyItemImpl> files;
        private final Logger logger;
        private final Tika tika;
        private final static int BATCH_SIZE = 100;

        /**
         * Create instance of Indexer file.
         *
         * @param iw     {@link IndexWriter} Lucene index writer.
         * @param files  List of the file to index.
         * @param logger {@link Logger}.
         * @param tika   {@link Tika} to read content.
         */
        private Indexer(IndexWriter iw, List<HierarchyItemImpl> files, Logger logger, Tika tika) {
            this.indexWriter = iw;
            this.files = files;
            this.logger = logger;
            this.tika = tika;
        }

        @Override
        protected void compute() {
            if (files.size() > BATCH_SIZE) {
                List<Indexer> tasks = new ArrayList<>();
                List<List<HierarchyItemImpl>> partitioned = chopped(files);
                for (List<HierarchyItemImpl> sublist : partitioned) {
                    tasks.add(new Indexer(indexWriter, sublist, logger, tika));
                }
                invokeAll(tasks);
            } else {
                for (HierarchyItemImpl f : files) {
                    indexFile(f.getName(), f.getId(), null, f);
                }
            }
        }

        private static <T> List<List<T>> chopped(List<T> list) {
            List<List<T>> parts = new ArrayList<>();
            final int N = list.size();
            for (int i = 0; i < N; i += Indexer.BATCH_SIZE) {
                parts.add(new ArrayList<>(
                        list.subList(i, Math.min(N, i + Indexer.BATCH_SIZE)))
                );
            }
            return parts;
        }

        /**
         * Indexes file.
         *
         * @param fileName  File name to add to index.
         * @param currentId Current id of the file.
         * @param oldId     Old id of the file if it was moved.
         * @param file      {@link FileImpl} to index.
         */
        void indexFile(String fileName, Integer currentId, Integer oldId, HierarchyItemImpl file) {
            try {
                Field pathField = new StringField(ID, currentId.toString(), Field.Store.YES);
                Field nameField = new TextField(NAME, fileName, Field.Store.YES);
                Document doc = new Document();
                doc.add(pathField);
                doc.add(nameField);
                if (file instanceof FileImpl) {
                    indexContent(currentId, (FileImpl) file, doc);
                }
                if (indexWriter.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                    indexWriter.addDocument(doc);
                } else {
                    indexWriter.updateDocument(new Term(ID, oldId != null ? oldId.toString() : currentId.toString()), doc);
                }
            } catch (Throwable e) {
                logger.logError("Error while indexing file: " + currentId, e);
            }
        }

        /**
         * Indexes content of the file.
         *
         * @param currentId File id.
         * @param file      {@link FileImpl}
         * @param doc       Apache Lucene {@link Document}
         */
        private void indexContent(Integer currentId, FileImpl file, Document doc) {
            InputStream stream = null;
            try {
                stream = file.getFileContentToIndex(currentId);
                if (stream != null) {
                    Metadata metadata = new Metadata();
                    String content = tika.parseToString(stream, metadata, MAX_CONTENT_LENGTH);
                    doc.add(new TextField(CONTENTS, content, Field.Store.YES));
                }
            } catch (Throwable ex) {
                if (!(ex instanceof ZeroByteFileException)) {
                    logger.logError("Error while indexing content: " + currentId, ex);
                }
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Throwable e) {
                        logger.logError("Error while indexing file content: " + currentId, e);
                    }
                }
            }
        }

        /**
         * Close index and release lock
         */
        void stop() {
            try {
                indexWriter.close();
            } catch (IOException e) {
                logger.logError("Cannot release index resources", e);
            }
        }

        /**
         * Deletes specified file information from the index.
         *
         * @param file {@link FileImpl} to delete from index.
         */
        void deleteIndex(HierarchyItemImpl file) {
            try {
                indexWriter.deleteDocuments(new Term(ID, String.valueOf(file.getId())));
            } catch (Throwable e) {
                logger.logDebug("Cannot delete index for the file: " + file.getId());
            }
        }

        /**
         * Timer task implementation to commit index changes from time to time.
         */
        static class CommitTask extends TimerTask {

            private final IndexWriter indexWriter;
            private final Logger logger;

            /**
             * Creates instance of {@link CommitTask}.
             *
             * @param indexWriter {@link IndexWriter} Lucene index writer.
             * @param logger      {@link Logger}.
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
             *
             * @param interval Timer interval.
             */
            void schedule(Integer interval) {
                Timer timer = new Timer(true);
                timer.scheduleAtFixedRate(this, 0, interval == null ? TASK_INTERVAL : interval * 1000);
            }
        }
    }

    /**
     * Search files information in Lucene index
     */
    static class Searcher {

        private final String indexFolder;
        private final QueryParser nameParser;
        private final QueryParser contentParser;
        private final Logger logger;
        private IndexSearcher indexSearcher;

        /**
         * Creates instance of {@link Searcher}.
         *
         * @param indexFolder      Index folder absolute location.
         * @param standardAnalyzer Lucene {@link StandardAnalyzer}.
         * @param logger           {@link Logger}.
         */
        private Searcher(String indexFolder, StandardAnalyzer standardAnalyzer, Logger logger) {
            this.indexFolder = indexFolder;
            nameParser = new QueryParser(Indexer.NAME, standardAnalyzer);
            nameParser.setAllowLeadingWildcard(true);
            contentParser = new QueryParser(Indexer.CONTENTS, standardAnalyzer);
            contentParser.setAllowLeadingWildcard(true);
            this.logger = logger;
        }

        /**
         * Searches files by search line either in file name or in content.
         *
         * Ajax File Browser accepts regular wild cards used in most OS:
         *
         * ‘*’ – to indicate one or more character.
         * ‘?’ – to indicate exactly one character.
         * The ‘*’ and ‘?’ characters are replaced with ‘%’ and ‘_’ characters to comply with DASL standard when submitted to the server.
         *
         * If ‘%’, ‘_’ or ‘\’ characters are used in search phrase they are escaped with ‘\%’, ‘\_’ and ‘\\’.
         *
         * To make the search behave similarly to how file system search functions Ajax File Browser
         * will automatically add ‘%’ character at the end of the search phrase. To search for the exact match wrap the search phrase in double quotes: “my file”.
         *
         * @param searchLine Line to search.
         * @param options    {@link SearchOptions} indicates where to search.
         * @return Map of ids to found items.
         */
        Map<String, String> search(String searchLine, SearchOptions options, boolean snippet) {
            searchLine = StringEscapeUtils.escapeJava(searchLine);
            searchLine = searchLine.replaceAll("%", "*");
            searchLine = searchLine.replaceAll("_", "?");
            Map<String, String> paths = new LinkedHashMap<>();
            try (IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexFolder)))) {
                indexSearcher = new IndexSearcher(reader);
                if (options.isSearchName()) {
                    paths.putAll(searchName(searchLine));
                }
                if (options.isSearchContent()) {
                    paths.putAll(searchContent(searchLine, snippet, reader));
                }
            } catch (Throwable e) {
                logger.logError("Error while doing index search.", e);
            }
            return paths;
        }

        private Map<String, String> searchName(String searchLine) throws Exception {
            Query query = nameParser.parse(searchLine);
            return search(query);
        }


        private Map<String, String> searchContent(String searchLine, boolean withSnippet, IndexReader indexReader) throws Exception {
            Query query = contentParser.parse(searchLine);
            if (withSnippet) {
                return searchWithSnippet(indexReader, query);
            }
            return search(query);
        }

        private Map<String, String> search(Query query) throws IOException {
            TopDocs search = indexSearcher.search(query, 100);
            ScoreDoc[] hits = search.scoreDocs;
            Map<String, String> paths = new LinkedHashMap<>();
            for (ScoreDoc hit : hits) {
                Document doc = indexSearcher.doc(hit.doc);
                String path = doc.get(Indexer.ID);
                paths.put(path, "");
            }
            return paths;
        }

        private Map<String, String> searchWithSnippet(IndexReader indexReader, Query query) throws Exception {
            QueryScorer queryScorer = new QueryScorer(query, Indexer.CONTENTS);
            Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer);
            SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
            Highlighter highlighter = new Highlighter(htmlFormatter, queryScorer);
            highlighter.setMaxDocCharsToAnalyze(Indexer.MAX_CONTENT_LENGTH);
            highlighter.setTextFragmenter(fragmenter);

            ScoreDoc[] scoreDocs = indexSearcher.search(query, 100).scoreDocs;
            Map<String, String> result = new LinkedHashMap<>();
            for (ScoreDoc scoreDoc : scoreDocs) {
                Document document = indexSearcher.doc(scoreDoc.doc);
                String text = document.get(Indexer.CONTENTS);
                String id = document.get(Indexer.ID);
                TokenStream tokenStream = TokenSources.getAnyTokenStream(indexReader,
                        scoreDoc.doc, Indexer.CONTENTS, document, new StandardAnalyzer());
                String fragment = highlighter.getBestFragment(tokenStream, text);
                result.put(id, fragment == null ? "" : fragment);
            }
            return result;
        }
    }
}
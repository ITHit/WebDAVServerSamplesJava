package com.ithit.webdav.samples.fsstorageservlet;

import com.ithit.webdav.server.HierarchyItem;
import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.search.SearchOptions;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.Tika;
import org.apache.tika.exception.ZeroByteFileException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

import static com.ithit.webdav.samples.fsstorageservlet.SearchFacade.Indexer.MAX_CONTENT_LENGTH;

/**
 * Facade that incapsulates all functionality regarding indexing and searching
 */
class SearchFacade {
    protected static final StandardAnalyzer ANALYZER = new StandardAnalyzer();
    private Indexer indexer;
    private Searcher searcher;
    private final WebDavEngine engine;
    private final Logger logger;

    SearchFacade(WebDavEngine webDavEngine, Logger logger) {
        engine = webDavEngine;
        this.logger = logger;
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
     * This task is running in background and indexes all folder mapped for WebDav asynchronously.
     * So there uis a chance that in case of big folder there will be some delay while this task
     * will finish indexation.
     */
    private class IndexTask extends TimerTask {

        private final String dataFolder;
        private final String indexFolder;
        private final Integer interval;

        /**
         * Build initial index of root folder.
         *
         * @param dataFolder  Root folder.
         * @param indexFolder Index folder.
         * @param interval    Daemon commit interval.
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
            ForkJoinPool forkJoinPool = new ForkJoinPool(4);
            Directory fsDir;
            try {
                List<HierarchyItem> filesToIndex = new ArrayList<>();
                File data = new File(dataFolder);
                searcher = new Searcher(indexFolder, ANALYZER, logger);
                getFilesToIndex(data.listFiles(), filesToIndex, dataFolder);
                fsDir = FSDirectory.open(Paths.get(indexFolder));
                IndexWriterConfig conf = new IndexWriterConfig(ANALYZER);
                conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                IndexWriter indexWriter = new IndexWriter(fsDir, conf);
                Tika tika = new Tika();
                tika.setMaxStringLength(MAX_CONTENT_LENGTH);
                indexer = new Indexer(indexWriter, filesToIndex, logger, tika, dataFolder);
                forkJoinPool.execute(indexer);
                indexWriter.commit();
                new Indexer.CommitTask(indexWriter, logger).schedule(interval);
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
     *
     * @param dataFolder  Root folder.
     * @param indexFolder Index folder.
     * @param interval    Daemon commit interval.
     */
    void indexRootFolder(String dataFolder, String indexFolder, Integer interval) {
        new IndexTask(dataFolder, indexFolder, interval).schedule();
    }

    /**
     * Builds list of the all files (including files in child folders) in the data folder.
     *
     * @param files      List of files in root folder.
     * @param result     List to be populated with results.
     * @param dataFolder Root folder absolute location.
     */
    void getFilesToIndex(File[] files, List<HierarchyItem> result, String dataFolder) {
        for (File f : files) {
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
            context = context.replace("\\", "/");
            if (f.isDirectory()) {
                context += "/";
            }
            result.add(engine.getHierarchyItem(context));
        } catch (Throwable e) {
            logger.logDebug("Cannot add file to the list: " + f.getAbsolutePath());
        }
    }


    /**
     * Indexes files in storage using Apache Lucene engine for indexing and Apache Tika.
     */
    static class Indexer extends RecursiveAction {
        static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;
        private static final int TASK_INTERVAL = 30 * 1000;
        static final String PATH = "path";
        static final String NAME = "name";
        static final String PARENT_NAME = "parent_name";
        static final String CONTENTS = "contents";
        private final IndexWriter indexWriter;
        private final List<HierarchyItem> files;
        private final Logger logger;
        private final Tika tika;
        private final String dataRoot;
        private static final int BATCH_SIZE = 100;

        /**
         * Create instance of Indexer file.
         *
         * @param iw       {@link IndexWriter} Lucene index writer.
         * @param files    List of the file to index.
         * @param logger   {@link Logger}.
         * @param tika     {@link Tika} to read content.
         * @param dataRoot Files location root folder.
         */
        private Indexer(IndexWriter iw, List<HierarchyItem> files, Logger logger, Tika tika, String dataRoot) {
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
                    } catch (Throwable e) {
                        logger.logDebug("Cannot find path for this file.");
                    }
                }
            }
        }

        private static <T> List<List<T>> chopped(List<T> list, final int l) {
            List<List<T>> parts = new ArrayList<>();
            final int n = list.size();
            for (int i = 0; i < n; i += l) {
                parts.add(new ArrayList<>(
                        list.subList(i, Math.min(n, i + l)))
                );
            }
            return parts;
        }

        /**
         * Indexes file.
         *
         * @param fileName    File name to add to index.
         * @param currentPath Current relative path of the file.
         * @param oldPath     Old relative path of the file if it was moved.
         */
        void indexFile(String fileName, String currentPath, String oldPath, HierarchyItem item) {
            Path fullPath = Paths.get(dataRoot, currentPath);
            try {
                Metadata metadata = new Metadata();
                Document doc = new Document();
                String parentFolder = currentPath.replace(fileName, "").replace("/", "");
                Field pathField = new StringField(PATH, currentPath, Field.Store.YES);
                Field parentField = new TextField(PARENT_NAME, parentFolder, Field.Store.YES);
                Field nameField = new TextField(NAME, fileName, Field.Store.YES);
                doc.add(pathField);
                doc.add(parentField);
                doc.add(nameField);
                if (item instanceof FileImpl) {
                    try (TikaInputStream stream = TikaInputStream.get(fullPath, metadata)) {
                        String content = tika.parseToString(stream, metadata, MAX_CONTENT_LENGTH);
                        doc.add(new TextField(CONTENTS, content, Field.Store.YES));
                    } catch (Throwable e) {
                        if (!(e instanceof ZeroByteFileException)) {
                            logger.logError("Error while indexing content: " + fullPath, e);
                        }
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
         * Close index and release lock
         */
        void stop() {
            try {
                indexWriter.close();
            } catch (Throwable e) {
                logger.logError("Cannot release index resources", e);
            }
        }

        /**
         * Deletes specified file information from the index.
         *
         * @param file {@link FileImpl} to delete from index.
         */
        void deleteIndex(HierarchyItem file) {
            try {
                indexWriter.deleteDocuments(new Term(PATH, file.getPath()));
            } catch (Throwable e) {
                logger.logError("Cannot delete index for the file.", e);
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
                } catch (Throwable e) {
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
     * Search files information in Lucene index.
     */
    static class Searcher {

        private final String indexFolder;
        private final QueryParser nameParser;
        private final QueryParser contentParser;
        private final QueryParser parentParser;
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
            parentParser = new QueryParser(Indexer.PARENT_NAME, standardAnalyzer);
            parentParser.setAllowLeadingWildcard(true);
            this.logger = logger;
        }

        /**
         * Searches files by search line either in file name or in content.
         * <p>
         * Ajax File Browser accepts regular wild cards used in most OS:
         * <p>
         * ‘*’ – to indicate one or more character.
         * ‘?’ – to indicate exactly one character.
         * The ‘*’ and ‘?’ characters are replaced with ‘%’ and ‘_’ characters to comply with DASL standard when submitted to the server.
         * <p>
         * If ‘%’, ‘_’ or ‘\’ characters are used in search phrase they are escaped with ‘\%’, ‘\_’ and ‘\\’.
         * <p>
         * To make the search behave similarly to how file system search functions Ajax File Browser
         * will automatically add ‘%’ character at the end of the search phrase. To search for the exact match wrap the search phrase in double quotes: “my file”.
         *
         * @param searchLine Line to search.
         * @param options    {@link SearchOptions} indicates where to search.
         * @param parent     Folder location in which to search.
         * @return Map of paths to found items.
         */
        Map<String, String> search(String searchLine, SearchOptions options, String parent, boolean snippet) {
            searchLine = StringEscapeUtils.escapeJava(searchLine);
            searchLine = searchLine.replace("%", "*");
            searchLine = searchLine.replace("_", "?");
            Map<String, String> paths = new LinkedHashMap<>();
            try (IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexFolder)))) {
                indexSearcher = new IndexSearcher(reader);
                if (options.isSearchContent()) {
                    paths.putAll(searchContent(searchLine, parent, snippet, reader));
                }
                if (options.isSearchName()) {
                    paths.putAll(searchName(searchLine, parent));
                }
            } catch (Throwable e) {
                logger.logError("Error while doing index search.", e);
            }
            return paths;
        }

        //  Searches files by search line in file name
        private Map<String, String> searchName(String searchLine, String parent) throws Exception {
            Query query = nameParser.parse(searchLine);
            BooleanQuery.Builder finalQuery = addParentQuery(parent, query);
            return search(finalQuery.build());
        }

        //  Searches files by search line in file content
        private Map<String, String> searchContent(String searchLine, String parent, boolean withSnippet, IndexReader indexReader) throws Exception {
            Query query = contentParser.parse(searchLine);
            BooleanQuery.Builder finalQuery = addParentQuery(parent, query);
            BooleanQuery booleanQuery = finalQuery.build();
            if (withSnippet) {
                return searchWithSnippet(indexReader, booleanQuery);
            }
            return search(booleanQuery);
        }

        //  Searches files by search line in file name and adds highlights for found words
        private Map<String, String> searchWithSnippet(IndexReader indexReader, Query query) throws Exception {
            QueryScorer queryScorer = new QueryScorer(query, Indexer.CONTENTS);
            Fragmenter fragmenter = new SimpleSpanFragmenter(queryScorer);
            SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
            Highlighter highlighter = new Highlighter(htmlFormatter, queryScorer);
            highlighter.setTextFragmenter(fragmenter);

            ScoreDoc[] scoreDocs = indexSearcher.search(query, 100).scoreDocs;
            Map<String, String> result = new LinkedHashMap<>();
            for (ScoreDoc scoreDoc : scoreDocs) {
                Document document = indexSearcher.doc(scoreDoc.doc);
                String text = document.get(Indexer.CONTENTS);
                String path = document.get(Indexer.PATH);
                TokenStream tokenStream = TokenSources.getAnyTokenStream(indexReader,
                        scoreDoc.doc, Indexer.CONTENTS, document, ANALYZER);
                String fragment = highlighter.getBestFragment(tokenStream, text);
                result.put(path, fragment == null ? "" : fragment);
            }
            return result;
        }

        // Adds parent folder to the query to make search only in this folder
        private BooleanQuery.Builder addParentQuery(String parent, Query query) throws ParseException {
            BooleanQuery.Builder finalQuery = new BooleanQuery.Builder();
            finalQuery.add(query, BooleanClause.Occur.MUST); // MUST implies that the keyword must occur.
            String searchString = parent.replace("/", "") + "*";
            if (!Objects.equals(parent, "/")) {
                Query parentQuery = parentParser.parse(searchString);
                finalQuery.add(parentQuery, BooleanClause.Occur.MUST);
            }
            return finalQuery;
        }

        // Searches files by search query either in file name or in content.
        private Map<String, String> search(Query query) throws IOException {
            TopDocs search = indexSearcher.search(query, 100);
            ScoreDoc[] hits = search.scoreDocs;
            Map<String, String> paths = new LinkedHashMap<>();
            for (ScoreDoc hit : hits) {
                Document doc = indexSearcher.doc(hit.doc);
                String path = doc.get(Indexer.PATH);
                paths.put(path, "");
            }
            return paths;
        }
    }
}

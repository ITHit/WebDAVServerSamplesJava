package com.ithit.webdav.samples.kotlinfs

import com.ithit.webdav.samples.kotlinfs.SearchFacade.Indexer.Companion.MAX_CONTENT_LENGTH
import com.ithit.webdav.server.HierarchyItem
import com.ithit.webdav.server.Logger
import com.ithit.webdav.server.exceptions.ServerException
import com.ithit.webdav.server.search.SearchOptions
import org.apache.commons.lang.StringEscapeUtils
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.*
import org.apache.lucene.queryparser.classic.ParseException
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.highlight.*
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.apache.tika.Tika
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.RecursiveAction
import java.util.regex.Pattern

/**
 * Facade that incapsulates all functionality regarding indexing and searching
 */
internal class SearchFacade(private val engine: WebDavEngine, private val logger: Logger) {
    /**
     * Returns Indexer instance
     *
     * @return Indexer instance
     */
    var indexer: Indexer? = null
        private set
    /**
     * Returns Searcher instance
     *
     * @return Searcher instance
     */
    var searcher: Searcher? = null
        private set

    /**
     * This task is running in background and indexes all folder mapped for WebDav asynchronously.
     * So there uis a chance that in case of big folder there will be some delay while this task
     * will finish indexation.
     */
    private inner class IndexTask
    /**
     * Build initial index of root folder.
     *
     * @param dataFolder  Root folder.
     * @param indexFolder Index folder.
     * @param interval    Daemon commit interval.
     */
    internal constructor(private val dataFolder: String, private val indexFolder: String, private val interval: Int?) : TimerTask() {

        /**
         * The action to be performed by this timer task.
         */
        override fun run() {
            val filesToIndex = ArrayList<HierarchyItem>()
            val standardAnalyzer = StandardAnalyzer()
            searcher = Searcher(indexFolder, standardAnalyzer, logger)
            getFilesToIndex(File(dataFolder).listFiles(), filesToIndex, dataFolder)
            val fsDir: Directory
            try {
                fsDir = FSDirectory.open(Paths.get(indexFolder))
                val conf = IndexWriterConfig(standardAnalyzer)
                conf.openMode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND
                val indexWriter = IndexWriter(fsDir, conf)
                val tika = Tika()
                tika.maxStringLength = MAX_CONTENT_LENGTH
                indexer = Indexer(indexWriter, filesToIndex, logger, tika, dataFolder)
                ForkJoinPool(4).execute(indexer!!)
                indexWriter.commit()
                Indexer.CommitTask(indexWriter, logger).schedule(interval)
            } catch (e: Throwable) {
                logger.logError("Cannot initialize Lucene", e)
            }
        }

        internal fun schedule() {
            val timer = Timer(true)
            timer.schedule(this, 0)
        }
    }

    /**
     * Build initial index of root folder.
     *
     * @param dataFolder  Root folder.
     * @param indexFolder Index folder.
     * @param interval    Daemon commit interval.
     */
    fun indexRootFolder(dataFolder: String, indexFolder: String, interval: Int?) {
        IndexTask(dataFolder, indexFolder, interval).schedule()
    }

    /**
     * Builds list of the all files (including files in child folders) in the data folder.
     *
     * @param files      List of files in root folder.
     * @param result     List to be populated with results.
     * @param dataFolder Root folder absolute location.
     */
    fun getFilesToIndex(files: Array<File>?, result: MutableList<HierarchyItem>, dataFolder: String) {
        for (f in files!!) {
            if (f.isDirectory && f.canRead() && !f.isHidden) {
                addFileToTheList(result, dataFolder, f)
                getFilesToIndex(f.listFiles(), result, dataFolder)
            } else {
                if (f.canRead() && !f.isHidden) {
                    addFileToTheList(result, dataFolder, f)
                }
            }
        }
    }

    private fun addFileToTheList(result: MutableList<HierarchyItem>, dataFolder: String, f: File) {
        val quote = Pattern.quote(dataFolder)
        try {
            var context = f.absolutePath.replace("(?i)$quote".toRegex(), "")
            context = context.replace("\\\\".toRegex(), "/")
            if (f.isDirectory) {
                context += "/"
            }
            result.add(engine.getHierarchyItem(context)!!)
        } catch (e: Exception) {
            logger.logDebug("Cannot add file to the list: " + f.absolutePath)
        }
    }


    /**
     * Indexes files in storage using Apache Lucene engine for indexing and Apache Tika.
     */
    internal class Indexer
    /**
     * Create instance of Indexer file.
     *
     * @param indexWriter       [IndexWriter] Lucene index writer.
     * @param files    List of the file to index.
     * @param logger   [Logger].
     * @param tika     [Tika] to read content.
     * @param dataRoot Files location root folder.
     */
    constructor(private val indexWriter: IndexWriter, private val files: List<HierarchyItem>, private val logger: Logger, private val tika: Tika, private val dataRoot: String) : RecursiveAction() {

        override fun compute() {
            if (files.size > BATCH_SIZE) {
                val tasks = ArrayList<Indexer>()
                val partitioned = chopped(files, BATCH_SIZE)
                for (sublist in partitioned) {
                    tasks.add(Indexer(indexWriter, sublist, logger, tika, dataRoot))
                }
                ForkJoinTask.invokeAll(tasks)
            } else {
                for (f in files) {
                    try {
                        indexFile(f.name, f.path, null, f)
                    } catch (e: ServerException) {
                        logger.logDebug("Cannot find path for this file.")
                    }
                }
            }
        }

        /**
         * Indexes file.
         *
         * @param fileName    File name to add to index.
         * @param currentPath Current relative path of the file.
         * @param oldPath     Old relative path of the file if it was moved.
         */
        fun indexFile(fileName: String, currentPath: String, oldPath: String?, item: HierarchyItem?) {
            val fullPath = Paths.get(dataRoot, currentPath)
            try {
                val metadata = Metadata()
                val doc = Document()
                doc.add(StringField(PATH, currentPath, Field.Store.YES))
                doc.add(TextField(PARENT_NAME, currentPath.replace(fileName, "").replace("/".toRegex(), ""), Field.Store.YES))
                doc.add(TextField(NAME, fileName, Field.Store.YES))
                if (item != null && item is FileImpl) {
                    try {
                        TikaInputStream.get(fullPath, metadata).use { stream ->
                            val content = tika.parseToString(stream, metadata, MAX_CONTENT_LENGTH)
                            doc.add(TextField(CONTENTS, content, Field.Store.YES))
                        }
                    } catch (e: Throwable) {
                        logger.logError("Error while indexing content: $fullPath", e)
                    }
                }
                if (indexWriter.config.openMode == IndexWriterConfig.OpenMode.CREATE) {
                    indexWriter.addDocument(doc)
                } else {
                    indexWriter.updateDocument(Term(PATH, oldPath ?: currentPath), doc)
                }
            } catch (e: Throwable) {
                logger.logError("Error while indexing file: $fullPath", e)
            }
        }

        /**
         * Close index and release lock
         */
        fun stop() {
            try {
                indexWriter.close()
            } catch (e: IOException) {
                logger.logError("Cannot release index resources", e)
            }
        }

        /**
         * Deletes specified file information from the index.
         *
         * @param file [FileImpl] to delete from index.
         */
        fun deleteIndex(file: HierarchyItem) {
            try {
                indexWriter.deleteDocuments(Term(PATH, file.path))
            } catch (e: Exception) {
                logger.logError("Cannot delete index for the file.", e)
            }
        }

        /**
         * Timer task implementation to commit index changes from time to time.
         */
        internal class CommitTask
        /**
         * Creates instance of [CommitTask].
         *
         * @param indexWriter [IndexWriter] Lucene index writer.
         * @param logger      [Logger].
         */
        (private val indexWriter: IndexWriter, private val logger: Logger) : TimerTask() {

            /**
             * The action to be performed by this timer task.
             */
            override fun run() {
                try {
                    indexWriter.commit()
                } catch (e: IOException) {
                    logger.logError("Cannot commit.", e)
                }
            }

            /**
             * Schedule timer executions at the specified Interval.
             *
             * @param interval Timer interval.
             */
            fun schedule(interval: Int?) {
                Timer(true).scheduleAtFixedRate(this, 0, (if (interval == null) TASK_INTERVAL else interval * 1000).toLong())
            }
        }

        companion object {
            const val MAX_CONTENT_LENGTH = 10 * 1024 * 1024
            private const val TASK_INTERVAL = 30 * 1000
            const val PATH = "path"
            const val NAME = "name"
            const val PARENT_NAME = "parent_name"
            const val CONTENTS = "contents"
            private const val BATCH_SIZE = 100

            private fun <T> chopped(list: List<T>, l: Int): List<List<T>> {
                val parts = ArrayList<List<T>>()
                val n = list.size
                var i = 0
                while (i < n) {
                    parts.add(ArrayList(
                            list.subList(i, Math.min(n, i + l)))
                    )
                    i += l
                }
                return parts
            }
        }
    }

    /**
     * Search files information in Lucene index.
     */
    internal class Searcher
    /**
     * Creates instance of [Searcher].
     *
     * @param indexFolder      Index folder absolute location.
     * @param standardAnalyzer Lucene [StandardAnalyzer].
     * @param logger           [Logger].
     */
    constructor(private val indexFolder: String, standardAnalyzer: StandardAnalyzer, private val logger: Logger) {
        private val nameParser: QueryParser = QueryParser(Indexer.NAME, standardAnalyzer)
        private val contentParser: QueryParser
        private val parentParser: QueryParser
        private var indexSearcher: IndexSearcher? = null

        init {
            nameParser.allowLeadingWildcard = true
            contentParser = QueryParser(Indexer.CONTENTS, standardAnalyzer)
            contentParser.allowLeadingWildcard = true
            parentParser = QueryParser(Indexer.PARENT_NAME, standardAnalyzer)
            parentParser.allowLeadingWildcard = true
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
         * @param options    [SearchOptions] indicates where to search.
         * @param parent     Folder location in which to search.
         * @return Map of paths to found items.
         */
        fun search(searchLine: String, options: SearchOptions, parent: String, snippet: Boolean): Map<String, String> {
            var localSearchLine = searchLine
            localSearchLine = StringEscapeUtils.escapeJava(localSearchLine)
            localSearchLine = localSearchLine.replace("%".toRegex(), "*")
            localSearchLine = localSearchLine.replace("_".toRegex(), "?")
            val paths = LinkedHashMap<String, String>()
            try {
                DirectoryReader.open(FSDirectory.open(Paths.get(indexFolder))).use { reader ->
                    indexSearcher = IndexSearcher(reader)
                    if (options.isSearchContent) {
                        paths.putAll(searchContent(localSearchLine, parent, snippet, reader))
                    }
                    if (options.isSearchName) {
                        paths.putAll(searchName(localSearchLine, parent))
                    }
                }
            } catch (e: Exception) {
                logger.logError("Error while doing index search.", e)
            }
            return paths
        }

        //  Searches files by search line in file name
        @Throws(Exception::class)
        private fun searchName(searchLine: String, parent: String): Map<String, String> {
            return search(addParentQuery(parent, nameParser.parse(searchLine)).build())
        }

        //  Searches files by search line in file content
        @Throws(Exception::class)
        private fun searchContent(searchLine: String, parent: String, withSnippet: Boolean, indexReader: IndexReader): Map<String, String> {
            val finalQuery = addParentQuery(parent,  contentParser.parse(searchLine))
            val booleanQuery = finalQuery.build()
            return if (withSnippet) {
                searchWithSnippet(indexReader, booleanQuery)
            } else search(booleanQuery)
        }

        //  Searches files by search line in file name and adds highlights for found words
        @Throws(Exception::class)
        private fun searchWithSnippet(indexReader: IndexReader, query: Query): Map<String, String> {
            val queryScorer = QueryScorer(query, Indexer.CONTENTS)
            val highlighter = Highlighter(SimpleHTMLFormatter(), queryScorer)
            //            highlighter.setMaxDocCharsToAnalyze(MAX_CONTENT_LENGTH);
            highlighter.textFragmenter = SimpleSpanFragmenter(queryScorer)
            val scoreDocs = indexSearcher!!.search(query, 100).scoreDocs
            val result = LinkedHashMap<String, String>()
            for (scoreDoc in scoreDocs) {
                val document = indexSearcher!!.doc(scoreDoc.doc)
                val text = document.get(Indexer.CONTENTS)
                val path = document.get(Indexer.PATH)
                val tokenStream = TokenSources.getAnyTokenStream(indexReader,
                        scoreDoc.doc, Indexer.CONTENTS, document, StandardAnalyzer())
                val fragment = highlighter.getBestFragment(tokenStream, text)
                result[path] = fragment ?: ""
            }
            return result
        }

        // Adds parent folder to the query to make search only in this folder
        @Throws(ParseException::class)
        private fun addParentQuery(parent: String, query: Query): BooleanQuery.Builder {
            val finalQuery = BooleanQuery.Builder()
            finalQuery.add(query, BooleanClause.Occur.MUST) // MUST implies that the keyword must occur.
            val searchString = parent.replace("/".toRegex(), "") + "*"
            if (parent != "/") {
                val parentQuery = parentParser.parse(searchString)
                finalQuery.add(parentQuery, BooleanClause.Occur.MUST)
            }
            return finalQuery
        }

        // Searches files by search query either in file name or in content.
        @Throws(IOException::class)
        private fun search(query: Query): Map<String, String> {
            val search = indexSearcher!!.search(query, 100)
            val hits = search.scoreDocs
            val paths = LinkedHashMap<String, String>()
            for (hit in hits) {
                val doc = indexSearcher!!.doc(hit.doc)
                val path = doc.get(Indexer.PATH)
                paths[path] = ""
            }
            return paths
        }
    }
}
package com.ithit.webdav.samples.deltavservlet;

import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.search.SearchOptions;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Search files information in Lucene index
 */
class Searcher {

    private String indexFolder;
    private QueryParser nameParser;
    private QueryParser contentParser;
    private Logger logger;
    private IndexSearcher indexSearcher;

    /**
     * Creates instance of {@link Searcher}.
     * @param indexFolder Index folder absolute location.
     * @param standardAnalyzer Lucene {@link StandardAnalyzer}.
     * @param logger {@link Logger}.
     */
    Searcher(String indexFolder, StandardAnalyzer standardAnalyzer, Logger logger) {
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
     * @param options {@link SearchOptions} indicates where to search.
     * @return Map of ids to found items.
     */
    Map<String, String> search(String searchLine, SearchOptions options, boolean snippet) {
        searchLine = StringEscapeUtils.escapeJava(searchLine);
        searchLine = searchLine.replaceAll("%", "*");
        searchLine = searchLine.replaceAll("_", "?");
        Map<String, String> paths = new LinkedHashMap<>();
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexFolder)))){
            indexSearcher = new IndexSearcher(reader);
            if (options.isSearchName()) {
                paths.putAll(searchName(searchLine));
            }
            if (options.isSearchContent()) {
                paths.putAll(searchContent(searchLine, snippet, reader));
            }
        } catch (Exception e) {
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

        ScoreDoc scoreDocs[] = indexSearcher.search(query, 100).scoreDocs;
        Map<String, String> result = new LinkedHashMap<>();
        for (ScoreDoc scoreDoc : scoreDocs)
        {
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

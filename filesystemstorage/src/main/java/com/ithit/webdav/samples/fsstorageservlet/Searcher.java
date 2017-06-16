package com.ithit.webdav.samples.fsstorageservlet;

import com.ithit.webdav.server.Logger;
import com.ithit.webdav.server.search.SearchOptions;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * Search files information in Lucene index.
 */
class Searcher {

    private String indexFolder;
    private QueryParser nameParser;
    private QueryParser contentParser;
    private QueryParser parentParser;
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
        parentParser = new QueryParser(Indexer.PARENT_NAME, standardAnalyzer);
        parentParser.setAllowLeadingWildcard(true);
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
     * @param parent Folder location in which to search.
     * @return Map of paths to found items.
     */
    Map<String, String> search(String searchLine, SearchOptions options, String parent, boolean snippet) {
        searchLine = StringEscapeUtils.escapeJava(searchLine);
        searchLine = searchLine.replaceAll("%", "*");
        searchLine = searchLine.replaceAll("_", "?");
        Map<String, String> paths = new LinkedHashMap<>();
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexFolder)))){
            indexSearcher = new IndexSearcher(reader);
            if (options.isSearchContent()) {
                paths.putAll(searchContent(searchLine, parent, snippet, reader));
            }
            if (options.isSearchName()) {
                paths.putAll(searchName(searchLine, parent));
            }
        } catch (Exception e) {
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
        highlighter.setMaxDocCharsToAnalyze(Indexer.MAX_CONTENT_LENGTH);
        highlighter.setTextFragmenter(fragmenter);

        ScoreDoc scoreDocs[] = indexSearcher.search(query, 100).scoreDocs;
        Map<String, String> result = new LinkedHashMap<>();
        for (ScoreDoc scoreDoc : scoreDocs)
        {
            Document document = indexSearcher.doc(scoreDoc.doc);
            String text = document.get(Indexer.CONTENTS);
            String path = document.get(Indexer.PATH);
            TokenStream tokenStream = TokenSources.getAnyTokenStream(indexReader,
                    scoreDoc.doc, Indexer.CONTENTS, document, new StandardAnalyzer());
            String fragment = highlighter.getBestFragment(tokenStream, text);
            result.put(path, fragment == null ? "" : fragment);
        }
        return result;
    }

    // Adds parent folder to the query to make search only in this folder
    private BooleanQuery.Builder addParentQuery(String parent, Query query) throws ParseException {
        BooleanQuery.Builder finalQuery = new BooleanQuery.Builder();
        finalQuery.add(query, BooleanClause.Occur.MUST); // MUST implies that the keyword must occur.
        String searchString = parent.replaceAll("/", "") + "*";
        if (!Objects.equals(parent, "/")) {
            Query parentQuery = parentParser.parse(searchString);
            finalQuery.add(parentQuery, BooleanClause.Occur.MUST);
        }
        return finalQuery;
    }

    // Searches files by search query either in file name or in content.
    private Map<String, String>  search(Query query) throws IOException {
        TopDocs search = indexSearcher.search(query, 100);
        ScoreDoc[] hits = search.scoreDocs;
        Map<String, String>  paths = new LinkedHashMap<>();
        for (ScoreDoc hit : hits) {
            Document doc = indexSearcher.doc(hit.doc);
            String path = doc.get(Indexer.PATH);
            paths.put(path, "");
        }
        return paths;
    }
}

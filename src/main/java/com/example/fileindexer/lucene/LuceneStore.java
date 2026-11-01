package com.example.fileindexer.lucene;

import com.example.fileindexer.config.IndexerProperties;
import com.example.fileindexer.model.SearchHit;
import com.example.fileindexer.model.SearchType;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Component;

@Component
public class LuceneStore {

    public static final String F_PATH = "path";
    public static final String F_DRIVE = "drive";
    public static final String F_NAME = "name";
    public static final String F_CONTENT = "content";

    private final Directory directory;
    private final Analyzer analyzer = new StandardAnalyzer();
    private final int maxResults;

    public LuceneStore(IndexerProperties props) throws IOException {
        Path dir = Path.of(props.luceneDir()).toAbsolutePath();
        Files.createDirectories(dir);
        this.directory = FSDirectory.open(dir);
        this.maxResults = props.maxResults();
    }

    public Analyzer analyzer() {
        return analyzer;
    }

    /** Opens a writer; the caller must close it (this releases the index lock). */
    public IndexWriter openWriter() throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        return new IndexWriter(directory, config);
    }

    public List<SearchHit> search(SearchType type, String text, Collection<String> driveIds)
            throws IOException, ParseException {
        if (!DirectoryReader.indexExists(directory)) {
            return List.of();
        }
        Query userQuery = switch (type) {
            case REGULAR -> regularQuery(text);
            case REGEX -> regexQuery(text);
        };
        List<BytesRef> drives = driveIds.stream().map(BytesRef::new).toList();
        Query query = new BooleanQuery.Builder()
                .add(userQuery, Occur.MUST)
                .add(new TermInSetQuery(F_DRIVE, drives), Occur.FILTER)
                .build();

        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs top = searcher.search(query, maxResults);
            StoredFields stored = searcher.storedFields();
            List<SearchHit> hits = new java.util.ArrayList<>();
            for (ScoreDoc sd : top.scoreDocs) {
                var doc = stored.document(sd.doc);
                hits.add(new SearchHit(doc.get(F_PATH), doc.get(F_DRIVE), sd.score));
            }
            return hits;
        }
    }

    private Query regularQuery(String text) throws ParseException {
        MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[] {F_CONTENT, F_NAME}, analyzer);
        parser.setDefaultOperator(QueryParser.Operator.AND);
        try {
            return parser.parse(text);
        } catch (ParseException e) {
            // Input such as "foo(" is not valid query syntax: search it literally.
            return parser.parse(QueryParser.escape(text));
        }
    }

    private Query regexQuery(String regex) {
        // Lucene regex syntax, matched against whole indexed terms (lower-cased words).
        return new BooleanQuery.Builder()
                .add(new RegexpQuery(new Term(F_CONTENT, regex)), Occur.SHOULD)
                .add(new RegexpQuery(new Term(F_NAME, regex)), Occur.SHOULD)
                .setMinimumNumberShouldMatch(1)
                .build();
    }

    @PreDestroy
    void close() throws IOException {
        directory.close();
    }
}

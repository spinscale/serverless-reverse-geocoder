package de.spinscale.query;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LatLonShape;
import org.apache.lucene.document.ShapeField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LatLonPointPrototypeQueries;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.util.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper class against lucene to search for a latitude/longitude
 */
public class Searcher implements Closeable  {

    private static final String FIELD_NAME_SHAPE = "geoshape";
    private final IndexSearcher searcher;
    private final DirectoryReader reader;

    Searcher(DirectoryReader reader) {
        this.searcher = new IndexSearcher(reader);
        this.reader = reader;
    }

    public SearchResult search(double latitude, double longitude) throws IOException {
        long start = System.nanoTime();
        SearchResult searchResult = search(SearchResult.Hint.Shape, latitude, longitude);
        if (searchResult.isEmpty()) {
            searchResult = search(SearchResult.Hint.Point, latitude, longitude);
        }
        long end = System.nanoTime();
        searchResult.setTook(TimeUnit.NANOSECONDS.toMillis(end - start));
        return searchResult;
    }

    private SearchResult search(SearchResult.Hint hint, double latitude, double longitude) throws IOException {
        TopDocs docs;
        if (hint == SearchResult.Hint.Shape) {
            Query query = LatLonShape.newPointQuery(FIELD_NAME_SHAPE,  ShapeField.QueryRelation.INTERSECTS,
                    new double[] { latitude, longitude });
            docs = searcher.search(query, 1);
        } else {
            docs = LatLonPointPrototypeQueries.nearest(searcher, "location", latitude, longitude, 1);
        }

        if (docs.totalHits.value == 0) {
            return SearchResult.empty(hint);
        }

        ScoreDoc scoreDoc = docs.scoreDocs[0];
        Document doc = searcher.doc(scoreDoc.doc);
        String city = doc.getField("city").stringValue();
        return new SearchResult(city, hint);
    }

    @Override
    public void close() {
        IOUtils.closeWhileHandlingException(reader.directory(), reader);
    }

    private static final ConcurrentHashMap<Path, Searcher> searchers = new ConcurrentHashMap<>();

    /**
     * Get a searcher. The searcher may have been precreated already
     *
     * @param path The path to the directory
     */
    public static Searcher getSearcher(Path path) throws IOException {
        if (searchers.containsKey(path) == false) {
            Directory directory = new NIOFSDirectory(path, NoLockFactory.INSTANCE);
            DirectoryReader reader = DirectoryReader.open(directory);
            Searcher searcher = new Searcher(reader);
            Searcher putIfAbsentSearcher = searchers.putIfAbsent(path, searcher);
            // if these two differ, close the reader/directories to not leak
            // and use searcher that has been added in the meantime instead
            if (putIfAbsentSearcher != null && searcher.equals(putIfAbsentSearcher) == false) {
                IOUtils.closeWhileHandlingException(reader, directory);
            }
        }

        return searchers.get(path);
    }

    public static void closeAll() {
        searchers.values().forEach(Searcher::close);
    }

}

package de.spinscale.query;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static de.spinscale.query.SearchResult.Hint.Point;
import static de.spinscale.query.SearchResult.Hint.Shape;
import static org.assertj.core.api.Assertions.assertThat;

public class SearcherTests {

    @Test
    public void testSearchesWork() throws Exception {
        Path indexPath = Paths.get(System.getProperty("user.dir"), "..", "indexer", "build", "indices");

        try (Searcher searcher = new Searcher(DirectoryReader.open(new NIOFSDirectory(indexPath)))) {
            // munich city centre
            assertLatitudeLongitude(48.1374, 11.5755, "München", Shape, searcher);
            // Munich/Hasenbergl in the Nord to make sure this does not become Oberschleissheim
            assertLatitudeLongitude(48.2162, 11.5580, "München", Shape, searcher);

            assertLatitudeLongitude(48.2697, 11.5712, "Unterschleißheim", Shape, searcher);
            assertLatitudeLongitude(48.1028, 11.4230, "Planegg", Point, searcher);
            assertLatitudeLongitude(51.7447, 14.6243, "Forst (Lausitz)", Point, searcher);
            assertLatitudeLongitude(50.9664, 6.8946, "Köln", Shape, searcher);
            assertLatitudeLongitude(54.1837, 7.8833, "Helgoland", Shape, searcher);
            assertLatitudeLongitude(51.4699, 7.1022, "Essen", Shape, searcher);
            assertLatitudeLongitude(51.2318, 6.7247, "Düsseldorf", Shape, searcher);

            // ensure the otherside of an island does not get considered to be its neighbour
            assertLatitudeLongitude(53.7200, 7.3287, "Norderney", Shape, searcher);
            assertLatitudeLongitude(53.6846, 7.0945, "Juist", Shape, searcher);
        }
    }

    private void assertLatitudeLongitude(double latitude, double longitude, String expectedCity, SearchResult.Hint expectedHint,
                                         Searcher searcher) throws Exception {
        SearchResult searchResult = searcher.search(latitude, longitude);
        assertThat(searchResult.city).isEqualTo(expectedCity);
        assertThat(searchResult.hint)
                .withFailMessage("expected city %s to be indexed as %s, but was %s", expectedCity, expectedHint, searchResult.hint)
                .isEqualTo(expectedHint);
        assertThat(searchResult.getTook()).isGreaterThanOrEqualTo(0L);
    }
}

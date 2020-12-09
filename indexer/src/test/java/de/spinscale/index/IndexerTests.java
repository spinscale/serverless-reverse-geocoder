package de.spinscale.index;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexerTests {

    @Test
    public void testIndexHasEnoughDocuments() throws Exception {
        Path indexPath = Paths.get(System.getProperty("user.dir"), "..", "indexer", "build", "indices");

        try (Directory directory = new NIOFSDirectory(indexPath); DirectoryReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            assertThat(searcher.count(new MatchAllDocsQuery())).isGreaterThan(49000);
        }
    }
}

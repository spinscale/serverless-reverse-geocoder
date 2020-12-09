package de.spinscale.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.LatLonShape;
import org.apache.lucene.document.ShapeField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.geo.Polygon;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

public class CsvShapeIndexer {

    private final List<CsvShapeParser.Shape> shapes;
    private final Path directory;
    private final String downloadDirectory;

    public static void main(String[] args) throws Exception {
        // shortcut to only reindex if not needed if shapes file has been changed
        // tried to make this work in the build.gradle file, but failed...
        long shapesLastModified = new File("src/main/resources/shapes.csv").lastModified();
        File buildIndexDirectory = new File("build/indices");
        if (!buildIndexDirectory.exists() || buildIndexDirectory.list().length == 0 || shapesLastModified > buildIndexDirectory.lastModified()) {
            final CsvShapeIndexer indexer = new CsvShapeIndexer(args[0], args[1]);
            indexer.createIndex();
        }
    }

    public CsvShapeIndexer(String downloadDirectory, String indexDirectory) throws IOException {
        final CsvShapeParser parser = new CsvShapeParser(downloadDirectory);
        this.shapes = parser.getShapes();
        this.downloadDirectory = downloadDirectory;
        this.directory = Paths.get(indexDirectory);
        if (Files.exists(this.directory) == false) {
            Files.createDirectories(directory);
        }
    }

    public void createIndex() throws IOException, ParseException {
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig().setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        try (Directory directory = new NIOFSDirectory(this.directory);
             IndexWriter writer = new IndexWriter(directory, indexWriterConfig)) {

            for (CsvShapeParser.Shape shape : shapes) {
                if (shape == null || shape.id == null || shape.id.isEmpty()) {
                    continue;
                }
                writer.addDocument(createDocument(shape));
            }

            System.out.println(String.format(Locale.ROOT, "Indexed %s shapes, committing", shapes.size()));
            writer.commit();

            System.out.println("Enriching index with post code locations, that are outside of the indexed shape");

            CsvPointParser pointParser = new CsvPointParser();
            final DirectoryReader reader = DirectoryReader.open(writer);
            final IndexSearcher searcher = new IndexSearcher(reader);
            int skipped = 0;
            int indexed = 0;
            for (CsvPointParser.GeoPoint point : pointParser.getPoints()) {
                Query q = LatLonShape.newPointQuery("geoshape",  ShapeField.QueryRelation.INTERSECTS,
                        new double[] { point.getLat() , point.getLon() });
                final TopDocs topDocs = searcher.search(q, 1);

                if (topDocs.totalHits.value == 0) { // index geopoint
                    writer.addDocument(createDocument(point));
                    indexed++;
                } else if (topDocs.totalHits.value == 1) { // do nothing
                    skipped++;
                } else { // throw exception
                    throw new RuntimeException("Found " + topDocs.totalHits.value + " hits for" + point);
                }
            }

            System.out.println(String.format(Locale.ROOT, "Indexed %s points, skipped %s. Merging down to one segment...", indexed, skipped));
            writer.commit();
            writer.forceMerge(1);
        }
    }

    private Document createDocument(CsvShapeParser.Shape shape) throws IOException, ParseException {
        Document doc = new Document();
        doc.add(new StringField("city" , shape.id, Field.Store.YES));

        byte[] bytes = Files.readAllBytes(Paths.get(downloadDirectory).resolve(shape.getFilename()));;
        final String geojson = new String(bytes, StandardCharsets.UTF_8);
        final SimpleGeoJSONPolygonParser parser = new SimpleGeoJSONPolygonParser(geojson);
        Polygon[] polygons = parser.parse();
        for (Field field : LatLonShape.createIndexableFields("geoshape", polygons[0])) {
            doc.add(field);
        }

        if (parser.getGeomLatitude() == null && parser.getGeomLatitude() == null) {
            System.out.println(String.format("Could not add point for shape %s", shape.id));
        } else {
            doc.add(new LatLonPoint("location", parser.getGeomLatitude(), parser.getGeomLongitude()));
        }

        return doc;
    }

    private Document createDocument(CsvPointParser.GeoPoint point) {
        Document doc = new Document();
        doc.add(new StringField("city" , point.getName(), Field.Store.YES));
        doc.add(new LatLonPoint("location", point.getLat(), point.getLon()));
        return doc;
    }
}

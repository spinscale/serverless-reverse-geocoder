package de.spinscale.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LatLonShape;
import org.apache.lucene.document.ShapeField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class GeoJsonTests {

    private static List<CsvShapeParser.Shape> shapes;
    private static final String indicesDirectory = "build/indices/";
    private static final String downloadDirectory = "src/main/resources/downloads/";
    private static DirectoryReader reader;
    private static IndexSearcher searcher;

    @BeforeAll
    public static void loadShapes() throws Exception {
        final CsvShapeParser parser = new CsvShapeParser(downloadDirectory);
        shapes = parser.getShapes();

        reader = DirectoryReader.open(new NIOFSDirectory(Paths.get(indicesDirectory)));
        searcher = new IndexSearcher(reader);
    }

    @AfterAll
    public static void closeResources() {
        IOUtils.closeWhileHandlingException(reader);
    }

    @ParameterizedTest(name = "[{0}] is indexed properly")
    @MethodSource("retrieveGeoShapes")
    public void testGeoShapes(CsvShapeParser.Shape shape) throws Exception {
        if (shape.geoPoints().isEmpty()) {
            // extract latitude longitude from shape file if exists
            byte[] bytes = Files.readAllBytes(Paths.get("src/main/resources/downloads").resolve(shape.getFilename()));;
            final String geojson = new String(bytes, StandardCharsets.UTF_8);
            final SimpleGeoJSONPolygonParser polygonParser = new SimpleGeoJSONPolygonParser(geojson);
            polygonParser.parse();
            assertThat(polygonParser.getGeomLatitude()).isNotNull();
            assertThat(polygonParser.getGeomLongitude()).isNotNull();
            executeLatLonTest(shape.id, polygonParser.getGeomLatitude(), polygonParser.getGeomLongitude());
        } else {
            for (CsvShapeParser.Point point : shape.geoPoints()) {
                executeLatLonTest(shape.id, point.lat, point.lon);
            }
        }
    }

    private void executeLatLonTest(String expectedCity, Double latitude, Double longitude) throws IOException {
        Query q = LatLonShape.newBoxQuery("geoshape",  ShapeField.QueryRelation.INTERSECTS,
                latitude - 0.00001, latitude + 0.00001,
                longitude - 0.00001, longitude + 0.00001);
        final TopDocs topDocs = searcher.search(q, 10);

        // there can be only one result!
        final long value = topDocs.totalHits.value;
        if (value == 1) {
            Document doc = searcher.doc(topDocs.scoreDocs[0].doc);
            String city = doc.getField("city").stringValue();
            assertThat(city).isEqualTo(expectedCity)
                    .withFailMessage("Expected city %s for coordinates %s/%s", expectedCity, latitude, longitude);

        // nothing found is an error condition for sure, let's dig deeper what is wrong
        } else if (value == 0) {
            // check if the city is not indexed at all
            final TopDocs cityTopDocs = searcher.search(new TermQuery(new Term("city", expectedCity)), 1);
            assertThat(cityTopDocs.totalHits.value).isGreaterThan(0L)
                    .withFailMessage("Shape for %s has not been indexed", expectedCity);

            final String pointNotWithinCity = String.format("Point %s/%s is not within %s", latitude, longitude, expectedCity);
            fail(pointNotWithinCity);
        // more than one hit means we indexed wrong data
        } else {
            fail(String.format("Expected one hit, found %s", value));
        }
    }

    private static Stream<Arguments> retrieveGeoShapes() {
        return shapes.stream().map(Arguments::of);
    }
}

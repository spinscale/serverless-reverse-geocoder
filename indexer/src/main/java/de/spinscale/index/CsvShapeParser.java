package de.spinscale.index;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CsvShapeParser {

    private static final String SHAPE_FILE_NAME = "/shapes.csv";
    private final Path directory;
    private final List<Shape> shapes;

    public static void main(String[] args) throws IOException, URISyntaxException {
        final CsvShapeParser parser = new CsvShapeParser(args[0]);
        parser.downloadShapes();
    }

    public CsvShapeParser(String directory) throws IOException {
        this.directory = Paths.get(directory);
        if (Files.exists(this.directory) == false) {
            Files.createDirectories(this.directory);
        }
        shapes = readCsv();
        checkForDuplicates();
    }

    // checks for duplicate ids, duplicate json filenames and duplicate urls
    private void checkForDuplicates() {
        final Map<String, Long> duplicateIds = duplicates(Shape::getId);
        final Map<String, Long> duplicateUrls = duplicates(Shape::getUrl);
        final Map<String, Long> duplicateFilenames = duplicates(Shape::getFilename);

        if (!duplicateFilenames.isEmpty() || !duplicateUrls.isEmpty() || !duplicateIds.isEmpty()) {
            if (!duplicateFilenames.isEmpty()) {
                System.out.println("Found duplicate filenames " + duplicateFilenames.keySet());
            }
            if (!duplicateUrls.isEmpty()) {
                System.out.println("Found duplicate urls " + duplicateUrls.keySet());
            }
            if (!duplicateIds.isEmpty()) {
                System.out.println("Found duplicate ids " + duplicateIds.keySet());
            }
            throw new RuntimeException("duplicate content in shapes.csv file found, aborting");
        }
    }

    private Map<String, Long> duplicates(Function<Shape, String> function) {
        return shapes.stream()
                .map(function)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }



    public List<Shape> getShapes() {
        return shapes;
    }

    private List<Shape> readCsv() throws IOException {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY)
                .enable(CsvParser.Feature.SKIP_EMPTY_LINES)
                .schemaFor(Shape.class).withComments()
                .withColumnSeparator(',')
                .withComments()
                .withSkipFirstDataRow(true);

        try (InputStream is = CsvShapeParser.class.getResourceAsStream(SHAPE_FILE_NAME)) {
            MappingIterator<Shape> it = mapper.readerFor(Shape.class).with(schema).readValues(is);
            return it.readAll();
        }
    }

    void downloadShapes() throws IOException {
        OkHttpClient httpClient = new OkHttpClient();
        System.out.println("Checking for " + shapes.size() + " shapes.");
        for (Shape shape : shapes) {
            final Path filePath = directory.resolve(shape.getFilename());
            if (!Files.exists(filePath)) {
                Request request = new Request.Builder().url(shape.url).build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.code() == 200) {
                        Files.write(filePath, response.body().bytes());
                        System.out.println("Wrote " + filePath.getFileName());
                    } else {
                        throw new RuntimeException("Error querying " + shape.url + ", response code was " + response.code());
                    }
                }
            }
        }
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    @JsonPropertyOrder({ "id", "filename", "url", "points" })
    public static final class Shape {
        public String id;
        private String filename;
        public String url;
        public String points;

        public String getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }

        public String getFilename() {
            if (filename == null || filename.length() == 0) {
                return id.toLowerCase(Locale.ROOT) + ".json";
            } else {
                return filename;
            }
        }

        public List<Point> geoPoints() {
            if (points == null || points.length() == 0) {
                return Collections.emptyList();
            }
            final String[] pointsAsString = points.split("_");
            List<Point> points = new ArrayList<>(pointsAsString.length);

            for (String pointAsString : pointsAsString) {
                final String[] split = pointAsString.split("\\|", 2);
                double lat = Double.parseDouble(split[0].trim());
                double lon = Double.parseDouble(split[1].trim());
                points.add(new Point(lat, lon));
            }

            return points;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public static final class Point {
        public final double lat;
        public final double lon;

        public Point(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public String toString() {
            return lat + "/" + lon;
        }
    }

}

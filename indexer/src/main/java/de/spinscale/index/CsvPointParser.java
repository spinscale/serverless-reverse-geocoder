package de.spinscale.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class CsvPointParser {

    private static final String POINT_FILE_NAME = "/DE.tab";

    private final List<CsvPointParser.GeoPoint> points;

    public CsvPointParser() throws IOException {
        points = readCsv();
    }

    private List<CsvPointParser.GeoPoint> readCsv() throws IOException {
        List<CsvPointParser.GeoPoint> points = new ArrayList<>();
        try (Scanner scanner = new Scanner(getClass().getResourceAsStream(POINT_FILE_NAME))) {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                // skip commented line
                if (line.startsWith("#")) {
                    continue;
                }
                final List<String> record = getRecordFromLine(line);
                final String name = record.get(3);
                final String lat = record.get(4);
                final String lon = record.get(5);
                final String plz = record.get(7);
                if (name != null && !name.isEmpty() && lat != null && !lat.isEmpty() && lon != null && !lon.isEmpty() && plz != null && !plz.isEmpty()) {
                    points.add(new GeoPoint(name, Double.parseDouble(lat), Double.parseDouble(lon)));
                }
            }
        }
        return points;
    }


    private List<String> getRecordFromLine(String line) {
        List<String> values = new ArrayList<String>();
        try (Scanner rowScanner = new Scanner(line)) {
            rowScanner.useDelimiter("\t");
            while (rowScanner.hasNext()) {
                values.add(rowScanner.next());
            }
        }
        return values;
    }

    public List<GeoPoint> getPoints() {
        return points;
    }

    public static final class GeoPoint {
        private String name;
        private Double lon;
        private Double lat;

        public GeoPoint(String name, Double lat, Double lon) {
            this.name = name;
            this.lon = lon;
            this.lat = lat;
        }

        public String getName() {
            return name;
        }

        public Double getLon() {
            return lon;
        }

        public Double getLat() {
            return lat;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%s: lat %s/lon %s", name, lat, lon);
        }
    }
}

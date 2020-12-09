package de.spinscale.cli;

import de.spinscale.query.SearchResult;
import de.spinscale.query.Searcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class CliSearcher {

    public static void main(String[] args) throws IOException {
        String program = System.getProperty("sun.java.command"); // not set with graal...
        if (args.length != 3) {
            System.out.println("Exactly three arguments required!");
            System.out.println("Usage: " + program + " path-to-lucene-index latitude longitude\n");
            System.out.println("Example usage: " + program + " indices/ 48.1374 11.5755\n\n");
            System.exit(1);
        }

        String path = args[0];
        Double lat = Double.valueOf(args[1]);
        Double lon = Double.valueOf(args[2]);
        Path indexPath = Paths.get(path);

        if (!Files.isDirectory(indexPath)) {
            System.err.println(String.format(Locale.ROOT, "ERROR: path %s is not a directory", indexPath));
            System.exit(-1);
        }

        if (!Files.isReadable(indexPath)) {
            System.err.println(String.format(Locale.ROOT, "ERROR: path %s is not readable", indexPath));
            System.exit(-1);
        }

        System.out.println(String.format(Locale.ROOT, "Searching for city at lat %s/lon %s", lat, lon));
        Searcher searcher = Searcher.getSearcher(indexPath);
        SearchResult result = searcher.search(lat, lon);

        if (result.isEmpty()) {
            System.out.println("No results found");
        } else {
            String hint = result.hint == SearchResult.Hint.Shape ? "within": "nearby";
            String format = "Point is %s %s, search took %sms";
            System.out.println(String.format(Locale.ROOT, format, hint, result.city, result.getTook()));
        }

        Searcher.closeAll();
    }
}

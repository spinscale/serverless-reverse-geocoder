package de.spinscale.index;

import org.junit.jupiter.api.Test;

public class PolygonParserTests {

    @Test
    public void testArrayParsingWithEscapedStrings() throws Exception {
        StringBuilder b = new StringBuilder();
        b.append("{\n");
        b.append("  \"type\": \"Polygon\",\n");
        b.append("  \"coordinates\": [\n");
        b.append("    [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0],\n");
        b.append("      [100.0, 1.0], [100.0, 0.0] ]\n");
        b.append("  ],\n");
        b.append("  \"properties\": {\n");
        b.append("    \"spam\" : [ \"foo \\\"bar\\\"\" ]\n");
        b.append("  }\n");
        b.append("}\n");

        new SimpleGeoJSONPolygonParser(b.toString()).parse();
    }
}

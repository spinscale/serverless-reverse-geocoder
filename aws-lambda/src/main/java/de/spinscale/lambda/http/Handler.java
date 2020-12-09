package de.spinscale.lambda.http;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import de.spinscale.query.SearchResult;
import de.spinscale.query.Searcher;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Handler implements RequestHandler<Map<String, Object>, Handler.Response> {

    private static final String EMPTY_BODY_RESPONSE = Json.object().add("error", "empty body").toString();
    private final Path luceneIndex = getTaskRoot().resolve("indices");

    @Override
    public Response handleRequest(Map<String, Object> input, Context context) {
        try {
            if (input.containsKey("body")) {
                JsonValue body = Json.parse(input.get("body").toString());
                double latitude, longitude;
                try {
                    final JsonObject json = body.asObject();
                    latitude = json.get("latitude").asDouble();
                    longitude = json.get("longitude").asDouble();
                } catch (NullPointerException e) {
                    return new Response(400, EMPTY_BODY_RESPONSE);
                }

                Searcher searcher = Searcher.getSearcher(luceneIndex);
                if (searcher == null) {
                    return new Response(500, "cannot serve requests");
                }
                SearchResult searchResult = searcher.search(latitude, longitude);
                JsonObject jsonObject = Json.object().add("city", searchResult.city).add("took", searchResult.getTook());
                return new Response(200, jsonObject.toString());
            }
            return new Response(400, EMPTY_BODY_RESPONSE);
        } catch (Exception e) {
            try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
                e.printStackTrace(pw);
                return new Response(500, e.getMessage() + "\n" + sw.toString());
            } catch (IOException ioe) {
                return new Response(500, ioe.getMessage());
            }

        }
    }

    protected Path getTaskRoot() {
        return Paths.get(System.getenv("LAMBDA_TASK_ROOT"));
    }

    static final class Response {
        private final boolean isIsBase64Encoded = false;
        private final int statusCode;
        private final String body;

        public Response(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public boolean isIsBase64Encoded() {
            return isIsBase64Encoded;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }
    }
}

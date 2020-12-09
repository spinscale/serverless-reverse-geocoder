package de.spinscale.webserver;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import de.spinscale.query.SearchResult;
import de.spinscale.query.Searcher;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

import static de.spinscale.webserver.TokenAccessManager.UserRole;

public class SearchHandler implements Handler {

    private final Searcher searcher;

    public SearchHandler(final Path indexDirectory) throws IOException {
        searcher = Searcher.getSearcher(indexDirectory);
    }

    @Override
    public void handle(@NotNull final Context ctx) throws Exception {
        final UserRole role = ctx.attribute("auth-type");
        if (UserRole.SEARCH_ALLOWED == role) {
            try {
                JsonValue body = Json.parse(ctx.body());
                double latitude = body.asObject().get("latitude").asDouble();
                double longitude = body.asObject().get("longitude").asDouble();
                final SearchResult result = searcher.search(latitude, longitude);
                if (result.isEmpty()) {
                    final JsonObject errorJson = Json.object().add("error",
                            String.format("Could not find location for lat %s/%s", latitude, longitude));
                    ctx.contentType("application/json").result(errorJson.toString());
                } else {
                    ctx.contentType("application/json").result("{ \"location\" : \"" + result.city + "\" }");
                }
            } catch (Exception e) {
                final JsonObject errorJson = Json.object().add("error",
                        String.format("Error parsing input, need 'latitude' and 'longitude' JSON fields"));
                ctx.contentType("application/json").result(errorJson.toString());
            }
        } else if (UserRole.SEARCH_REJECTED == role) {
            ctx.status(401).contentType("application/json").result("{ \"error\" : \"request rejected.\" }");
        } else {
            ctx.status(403);
        }
    }
}

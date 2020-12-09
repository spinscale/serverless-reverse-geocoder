package de.spinscale.lambda.http;

import com.amazonaws.services.lambda.runtime.Context;
import com.eclipsesource.json.Json;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class HandlerTests {

    @Test
    public void testGoodCase() throws IOException {
        String json = Json.object().add("latitude", 48.1374).add("longitude", 11.5755).toString();

        Handler handler = new TestHandler();
        Context context = mock(Context.class);
        final Map<String, Object> body = Collections.singletonMap("body", json);
        Handler.Response response = handler.handleRequest(body, context);
        assertThat(response.getStatusCode()).isEqualTo(200);
        String city = Json.parse(response.getBody()).asObject().get("city").asString();
        assertThat(city).isEqualTo("München");

        // ensure a second request works just fine, no index readers are closed
        handler.handleRequest(body, context);
        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    public class TestHandler extends Handler {
        @Override
        protected Path getTaskRoot() {
            return Paths.get(System.getProperty("user.dir"), "..", "indexer", "build");
        }
    }

}
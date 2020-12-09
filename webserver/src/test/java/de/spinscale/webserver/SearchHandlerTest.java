package de.spinscale.webserver;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.json.JavalinJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchHandlerTest {

    private Handler handler;
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final Context context = new Context(request, response, Collections.emptyMap());

    @BeforeEach
    public void setupSearchHandler() throws IOException {
        handler = new SearchHandler(Paths.get("../indexer/build/indices/"));
    }

    @Test
    public void testRegularSearch() throws Exception {
        when(request.getAttribute(eq("auth-type"))).thenReturn(TokenAccessManager.UserRole.SEARCH_ALLOWED);
        ServletInputStream is = new TestServletInputStream("{ \"latitude\" : 48.223790, \"longitude\": 11.571607 }");
        when(request.getInputStream()).thenReturn(is);
        handler.handle(context);

        verify(response).setContentType(eq("application/json"));
        assertThat(context.resultString()).isNotNull();
        final Map<String, Object> data = JavalinJson.fromJson(context.resultString(), Map.class);
        assertThat(data).hasSize(1);
        assertThat(data).containsEntry("location", "München");
    }

    @Test
    public void testSearchWithMissingLatitudeField() throws Exception {
        when(request.getAttribute(eq("auth-type"))).thenReturn(TokenAccessManager.UserRole.SEARCH_ALLOWED);
        ServletInputStream is = new TestServletInputStream("{ \"longitude\": 11.571607 }");
        when(request.getInputStream()).thenReturn(is);
        handler.handle(context);

        verify(response).setContentType(eq("application/json"));
        final Map<String, Object> data = JavalinJson.fromJson(context.resultString(), Map.class);
        assertThat(data).hasSize(1);
        assertThat(data).containsEntry("error", "Error parsing input, need 'latitude' and 'longitude' JSON fields");
    }

    @Test
    public void testSearchWithMissingLongitudeField() throws Exception {
        when(request.getAttribute(eq("auth-type"))).thenReturn(TokenAccessManager.UserRole.SEARCH_ALLOWED);
        ServletInputStream is = new TestServletInputStream("{ \"latitude\" : 48.223790 }");
        when(request.getInputStream()).thenReturn(is);
        handler.handle(context);

        verify(response).setContentType(eq("application/json"));
        final Map<String, Object> data = JavalinJson.fromJson(context.resultString(), Map.class);
        assertThat(data).hasSize(1);
        assertThat(data).containsEntry("error", "Error parsing input, need 'latitude' and 'longitude' JSON fields");
    }

    @Test
    public void testSearchInvalidJson() throws Exception {
        when(request.getAttribute(eq("auth-type"))).thenReturn(TokenAccessManager.UserRole.SEARCH_ALLOWED);
        ServletInputStream is = new TestServletInputStream("{ \"latitude\" : 48.223790,,, }");
        when(request.getInputStream()).thenReturn(is);
        handler.handle(context);

        verify(response).setContentType(eq("application/json"));
    }

    @Test
    public void testUnAuthorized() throws Exception {
        when(request.getAttribute(eq("auth-type"))).thenReturn(TokenAccessManager.UserRole.NONE);
        handler.handle(context);
        verify(response).setStatus(eq(403));
    }

    @Test
    public void testRejected() throws Exception {
        when(request.getAttribute(eq("auth-type"))).thenReturn(TokenAccessManager.UserRole.SEARCH_REJECTED);
        handler.handle(context);

        verify(response).setStatus(eq(401));
        verify(response).setContentType(eq("application/json"));
        final Map<String, Object> data = JavalinJson.fromJson(context.resultString(), Map.class);
        assertThat(data).hasSize(1);
        assertThat(data).containsEntry("error", "request rejected.");
    }

    private static final class TestServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream byteArrayInputStream;

        public TestServletInputStream(String input) {
            byteArrayInputStream = new ByteArrayInputStream(input.getBytes());
        }

        @Override
        public boolean isFinished() {
            return byteArrayInputStream.available() <= 0;
        }

        @Override
        public boolean isReady() {
            return byteArrayInputStream.available() >= 0;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
        }

        @Override
        public int read() throws IOException {
            return byteArrayInputStream.read();
        }
    }
}
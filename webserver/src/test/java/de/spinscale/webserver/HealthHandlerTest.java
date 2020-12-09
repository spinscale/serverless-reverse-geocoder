package de.spinscale.webserver;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.json.JavalinJson;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;

import static de.spinscale.webserver.TokenAccessManager.UserRole.OPERATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HealthHandlerTest {

    final Handler handler = new HealthHandler();
    final HttpServletRequest request = mock(HttpServletRequest.class);
    final HttpServletResponse response = mock(HttpServletResponse.class);
    final Context context = new Context(request, response, Collections.emptyMap());

    @Test
    public void testHealthResponse() throws Exception {
        when(request.getAttribute(eq("auth-type"))).thenReturn(OPERATIONS);
        handler.handle(context);

        verify(response).setContentType(eq("application/json"));
        final Map<String, Object> data = JavalinJson.fromJson(context.resultString(), Map.class);
        assertThat(data).hasSize(1);
        assertThat(data).containsEntry("state", "OK");
    }
}
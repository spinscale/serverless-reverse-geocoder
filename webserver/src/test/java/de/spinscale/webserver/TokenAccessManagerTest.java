package de.spinscale.webserver;

import de.spinscale.fst.AuthFST;
import de.spinscale.fst.AuthFSTWriter;
import io.javalin.core.security.AccessManager;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import static de.spinscale.webserver.TokenAccessManager.UserRole.SEARCH_ALLOWED;
import static io.javalin.core.security.SecurityUtil.roles;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TokenAccessManagerTest {

    @TempDir
    public Path folder;

    private AccessManager accessManager;
    private final Handler handler = mock(Handler.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final Context context = new Context(request, response, Collections.emptyMap());

    @BeforeEach
    public void writeFST() throws IOException {
        final Path authFstPath = simpleAuth("allowed-token", "rejected-token", "operations-token");
        final AuthFST authFST = AuthFST.readFrom(authFstPath);
        this.accessManager = new TokenAccessManager(authFST);
    }

    @Test
    public void testPermittedRole() throws Exception {
        when(request.getHeader(eq("Authorization"))).thenReturn("allowed-token");

        accessManager.manage(handler, context, roles(SEARCH_ALLOWED));

        verify(request).setAttribute(eq("auth-type"), eq(SEARCH_ALLOWED));
        verify(handler).handle(eq(context));
    }

    @Test
    public void testNonPermittedRole() throws Exception {
        // even though authorization exists in fst, it is not allowed
        when(request.getHeader(eq("Authorization"))).thenReturn("allowed-token");
        accessManager.manage(handler, context, roles(TokenAccessManager.UserRole.SEARCH_REJECTED));
        verify(response).setStatus(eq(403));
    }

    @Test
    public void testMissingAuthenticationHeader() throws Exception {
        when(request.getHeader(eq("Authorization"))).thenReturn(null);
        accessManager.manage(handler, context, roles(TokenAccessManager.UserRole.SEARCH_REJECTED));
        verify(response).setStatus(eq(403));
    }

    private Path simpleAuth(String allowed, String rejected, String operations) throws IOException {
        final AuthFSTWriter writer = new AuthFSTWriter();
        writer.rejected(rejected);
        writer.allowed(allowed);
        writer.operations(operations);
        final Path path = folder.resolve("my-path");
        writer.save(path);
        return path;
    }

}

package de.spinscale.webserver;

import de.spinscale.fst.AuthFST;
import de.spinscale.query.Searcher;
import io.javalin.Javalin;
import io.javalin.core.compression.CompressionStrategy;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static de.spinscale.webserver.TokenAccessManager.UserRole.OPERATIONS;
import static de.spinscale.webserver.TokenAccessManager.UserRole.SEARCH_ALLOWED;
import static de.spinscale.webserver.TokenAccessManager.UserRole.SEARCH_REJECTED;
import static io.javalin.core.security.SecurityUtil.roles;
import static java.util.Objects.requireNonNull;

public class Webserver {

    private final Logger logger = LoggerFactory.getLogger(Webserver.class);
    private final Javalin app;

    public static void main(final String[] args) throws Exception {
        requireNonNull(System.getenv("AUTH_FILE"), "environment variable AUTH_FILE not set");
        requireNonNull(System.getenv("INDEX_DIRECTORY"), "environment variable INDEX_DIRECTORY not set");
        requireNonNull(System.getenv("PORT"), "environment variable PORT not set");

        final Path authFstPath = Paths.get(System.getenv("AUTH_FILE"));
        final Path indexDirectory = Paths.get(System.getenv("INDEX_DIRECTORY"));
        final String portAsString = System.getenv("PORT");

        final Webserver webserver = new Webserver(indexDirectory, authFstPath);

        webserver.start(Integer.parseInt(portAsString));
    }

    private Webserver(final Path indexDirectory, final Path authFstPath) throws IOException {
        final AuthFST authFST = AuthFST.readFrom(authFstPath);

        this.app = Javalin
                .create(config -> {
                    config.showJavalinBanner = false;
                    // ensure every request and its token gets logged, so we can analyze this later on for billing
                    config.requestLogger((ctx, executionTimeMs) -> {
                        TokenAccessManager.UserRole role = ctx.attribute("auth-type");
                        final String roleAsString = role != null ? role.name() : "NOT_AUTHENTICATED";
                        logger.info("path[{}], status[{}], role[{}], executionTimeMs[{}]",
                                ctx.path(), ctx.res.getStatus(), roleAsString, executionTimeMs);
                    });
                    config.accessManager(new TokenAccessManager(authFST));
                    config.compressionStrategy(CompressionStrategy.NONE);
                    config.server(() -> {
                        // similar to JettyUtil.getOrDefault but without mbean registration, startup optimization for graal
                        return new Server(new QueuedThreadPool(100, 2, 60_000));
                    });
                });

        app.get("/", ctx -> ctx.redirect("https://website.de"));
        app.get("/health", new HealthHandler(), roles(OPERATIONS));
        app.post("/search", new SearchHandler(indexDirectory), roles(SEARCH_ALLOWED, SEARCH_REJECTED));

        // most crude catch all exception logger
        app.exception(Exception.class, (exception, ctx) -> {
            logger.error("Exception in request [{} {}]", ctx.req.getMethod(), ctx.req.getRequestURI(), exception);
        });

        // clean shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            Searcher.closeAll();
        }));
    }

    private void start(final int port) {
        app.start(port);
    }
}

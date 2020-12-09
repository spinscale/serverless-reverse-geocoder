package de.spinscale.webserver;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

// do not use ctx.json as we do not want to invoke a JSON mapper
// but remain as fast as possible
public class HealthHandler implements Handler {

    private final static String OK_STATE = "{ \"state\" : \"OK\" }";

    @Override
    public void handle(@NotNull final Context ctx) throws Exception {
        ctx.contentType("application/json").result(OK_STATE);
    }
}

package de.spinscale.webserver;

import de.spinscale.fst.AuthFST;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TokenAccessManager implements AccessManager {

    public static final String AUTH_ATTRIBUTE_NAME = "auth-type";

    private final AuthFST auth;

    TokenAccessManager(final AuthFST auth) {
        this.auth = auth;
    }

    @Override
    public void manage(@NotNull final Handler handler, @NotNull final Context ctx, @NotNull final Set<Role> permittedRoles) throws Exception {
        final String authorization = ctx.header("Authorization");
        if (authorization == null || authorization.length() == 0) {
            ctx.status(403);
        } else {
            final String operation = auth.find(authorization);
            final Role role = UserRole.fromString(operation);
            ctx.attribute(AUTH_ATTRIBUTE_NAME, role);
            if (permittedRoles.contains(role)) {
                handler.handle(ctx);
            } else {
                ctx.status(403);
            }
        }
    }

    enum UserRole implements Role {
        OPERATIONS, SEARCH_ALLOWED, SEARCH_REJECTED, NONE;

        private static Role fromString(final String input) {
            switch (input) {
                case "operations":
                    return OPERATIONS;
                case "allowed":
                    return SEARCH_ALLOWED;
                case "rejected":
                    return SEARCH_REJECTED;
                default:
                    return NONE;
            }
        }
    }
}

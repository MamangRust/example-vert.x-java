package com.sanedge.example_crud.middleware;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

public final class RoleMiddleware {

  private RoleMiddleware() {
  }

  public static Handler<RoutingContext> requireRole(String role) {
    return ctx -> {
      JsonArray roles = ctx.user()
          .principal()
          .getJsonArray("roleNames");

      if (roles == null || !roles.contains(role)) {
        ctx.response().setStatusCode(403).end("Forbidden");
        return;
      }

      ctx.next();
    };
  }
}

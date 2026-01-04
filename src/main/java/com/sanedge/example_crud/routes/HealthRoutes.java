package com.sanedge.example_crud.routes;

import io.vertx.ext.web.Router;

public final class HealthRoutes {

  private HealthRoutes() {
  }

  public static void mount(Router router) {
    router.get("/health").handler(ctx -> ctx.response()
        .putHeader("Content-Type", "application/json")
        .end("{\"status\":\"UP\",\"service\":\"app\"}"));
  }
}

package com.sanedge.example_crud.middleware;

import io.vertx.core.Handler;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;

public final class JwtMiddleware {

  private JwtMiddleware() {
  }

  public static Handler<RoutingContext> jwt(JWTAuth jwtAuth) {
    return JWTAuthHandler.create(jwtAuth);
  }
}

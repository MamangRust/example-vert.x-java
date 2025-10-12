package com.sanedge.example_crud.handler;

import com.sanedge.example_crud.service.AuthService;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class AuthHandler {
  private final AuthService service;

  public AuthHandler(AuthService service) {
    this.service = service;
  }

  public void login(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    service.login(body.getString("email"), body.getString("password"))
        .onSuccess(token -> ctx.response().putHeader("Content-Type", "application/json")
            .end(new JsonObject().put("token", token).encode()))
        .onFailure(err -> ctx.response().setStatusCode(401)
            .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  public void register(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    service
        .register(body.getString("name"), body.getString("email"), body.getString("password"), body.getString("role"))
        .onSuccess(user -> ctx.response().putHeader("Content-Type", "application/json")
            .end(user.toJson().encode()))
        .onFailure(err -> ctx.fail(500, err));
  }
}

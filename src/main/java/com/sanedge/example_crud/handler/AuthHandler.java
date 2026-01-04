package com.sanedge.example_crud.handler;

import com.sanedge.example_crud.domain.requests.user.CreateUserRequest;
import com.sanedge.example_crud.service.AuthService;
import com.sanedge.example_crud.service.UserService;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthHandler {
  private final AuthService service;
  private final UserService userService;

  public void login(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    service.login(body.getString("email"), body.getString("password"))
        .onSuccess(token -> ctx.response().setStatusCode(200).putHeader("Content-Type", "application/json")
            .end(Json.encode(token)));
  }

  public void register(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    CreateUserRequest register = CreateUserRequest.builder()
        .firstName(body.getString("firstname"))
        .lastName(body.getString("lastname"))
        .email(body.getString("email"))
        .password(body.getString("password"))
        .build();

    service
        .register(register)
        .onSuccess(user -> ctx.response().setStatusCode(201).putHeader("Content-Type", "application/json")
            .end(Json.encode(user)));
  }

  public void refreshToken(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    service.refreshToken(body.getString("refresh_token"))
        .onSuccess(token -> ctx.response().setStatusCode(200).putHeader("Content-Type", "application/json")
            .end(Json.encode(token)));
  }

  public void getMe(RoutingContext ctx) {
    Integer userid = ctx.user().principal().getInteger("userId");

    userService.getUserById(userid)
        .onSuccess(user -> ctx.response().setStatusCode(200).end(Json.encode(user)));
  }

  public void logout(RoutingContext ctx) {
    Integer userid = ctx.user().principal().getInteger("userId");

    service.logout(userid)
        .onSuccess(user -> ctx.response().setStatusCode(200).end(Json.encode(user)));
  }
}

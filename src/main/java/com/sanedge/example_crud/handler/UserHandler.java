package com.sanedge.example_crud.handler;

import com.sanedge.example_crud.model.User;
import com.sanedge.example_crud.service.UserService;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class UserHandler {
  private final UserService service;

  public UserHandler(UserService service) {
    this.service = service;
  }

  public void create(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    User user = new User(body.getString("name"), body.getString("email"), null, null);

    service.createUser(user)
        .onSuccess(created -> ctx.response()
            .putHeader("Content-Type", "application/json")
            .end(created.toJson().encode()))
        .onFailure(err -> ctx.fail(500, err));
  }

  public void findAll(RoutingContext ctx) {
    service.getAllUsers()
        .onSuccess(users -> {
          ctx.response().putHeader("Content-Type", "application/json");
          ctx.response().end(users.stream().map(User::toJson).toList().toString());
        })
        .onFailure(err -> ctx.fail(500, err));
  }

  public void findById(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    service.getUserById(id)
        .onSuccess(user -> {
          if (user == null) {
            ctx.response().setStatusCode(404).end("Not Found");
            return;
          }
          ctx.response().putHeader("Content-Type", "application/json")
              .end(user.toJson().encode());
        })
        .onFailure(err -> ctx.fail(500, err));
  }

  public void update(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();
    User user = new User(id, body.getString("name"), body.getString("email"), null);

    service.updateUser(id, user)
        .onSuccess(v -> ctx.response().setStatusCode(204).end())
        .onFailure(err -> ctx.fail(500, err));
  }

  public void delete(RoutingContext ctx) {
    int id = Integer.parseInt(ctx.pathParam("id"));
    service.deleteUser(id)
        .onSuccess(v -> ctx.response().setStatusCode(204).end())
        .onFailure(err -> ctx.fail(500, err));
  }
}

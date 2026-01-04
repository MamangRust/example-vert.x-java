package com.sanedge.example_crud.handler;

import com.sanedge.example_crud.domain.requests.user.CreateUserRequest;
import com.sanedge.example_crud.domain.requests.user.FindAllUsers;
import com.sanedge.example_crud.domain.requests.user.UpdateUserRequest;
import com.sanedge.example_crud.service.UserService;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserHandler {
  private final UserService service;

  public void findAll(RoutingContext ctx) {
    FindAllUsers req = mapFindAllUsers(ctx);

    service.getAllUsers(req)
        .onSuccess(resp -> ctx.response().putHeader("Content-Type", "application/json").setStatusCode(200)
            .end(Json.encode(resp)));
  }

  public void findActive(RoutingContext ctx) {
    FindAllUsers req = mapFindAllUsers(ctx);

    service.getActiveUsers(req)
        .onSuccess(resp -> ctx.response().putHeader("Content-Type", "application/json").setStatusCode(200)
            .end(Json.encode(resp)));
  }

  public void findTrashed(RoutingContext ctx) {
    FindAllUsers req = mapFindAllUsers(ctx);

    service.getTrashedUsers(req)
        .onSuccess(resp -> ctx.response().putHeader("Content-Type", "application/json").setStatusCode(200)
            .end(Json.encode(resp)));
  }

  public void findById(RoutingContext ctx) {
    Integer userId = Integer.parseInt(ctx.pathParam("id"));
    service.getUserById(userId)
        .onSuccess(resp -> {
          ctx.response().putHeader("Content-Type", "application/json")
              .end(Json.encode(resp));
        });
  }

  public void create(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    CreateUserRequest register = CreateUserRequest.builder()
        .firstName(body.getString("firstname"))
        .lastName(body.getString("lastname"))
        .email(body.getString("email"))
        .password(body.getString("password"))
        .build();

    service.createUser(register)
        .onSuccess(created -> ctx.response()
            .putHeader("Content-Type", "application/json")
            .setStatusCode(201).end(Json.encode(created)));
  }

  public void update(RoutingContext ctx) {
    Integer userId = Integer.parseInt(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
        .userId(userId)
        .firstName(body.getString("firstname"))
        .lastName(body.getString("lastname"))
        .email(body.getString("email"))
        .password(body.getString("password"))
        .build();

    service.updateUser(updateUserRequest)
        .onSuccess(resp -> ctx.response().setStatusCode(200).end(Json.encode(resp)));
  }

  public void trashed(RoutingContext ctx) {
    Integer userId = Integer.parseInt(ctx.pathParam("id"));
    service.trashed(userId)
        .onSuccess(resp -> ctx.response().setStatusCode(200).end(Json.encode(resp)));
  }

  public void restore(RoutingContext ctx) {
    Integer userId = Integer.parseInt(ctx.pathParam("id"));
    service.restore(userId)
        .onSuccess(resp -> ctx.response().setStatusCode(200).end(Json.encode(resp)));
  }

  public void deletePermanent(RoutingContext ctx) {
    Integer userId = Integer.parseInt(ctx.pathParam("id"));
    service.deletePermanent(userId)
        .onSuccess(v -> ctx.response().setStatusCode(200).end(Json.encode(v)));
  }

  private FindAllUsers mapFindAllUsers(RoutingContext ctx) {
    FindAllUsers req = new FindAllUsers();

    req.setSearch(ctx.queryParams().get("search"));
    req.setPage(
        ctx.queryParams().contains("page")
            ? Integer.parseInt(ctx.queryParams().get("page"))
            : 1);
    req.setPageSize(
        ctx.queryParams().contains("pageSize")
            ? Integer.parseInt(ctx.queryParams().get("pageSize"))
            : 10);

    return req;
  }
}

package com.sanedge.example_crud.handler;

import com.sanedge.example_crud.domain.requests.role.CreateRoleRequest;
import com.sanedge.example_crud.domain.requests.role.FindAllRoles;
import com.sanedge.example_crud.domain.requests.role.UpdateRoleRequest;
import com.sanedge.example_crud.service.RoleService;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RoleHandler {
  private final RoleService service;

  public void findAll(RoutingContext ctx) {
    FindAllRoles req = mapFindAllRoles(ctx);

    service.getAllRoles(req)
        .onSuccess(resp -> {
          ctx.response()
              .putHeader("Content-Type", "application/json")
              .end(Json.encode(resp));
        });
  }

  public void findActive(RoutingContext ctx) {
    FindAllRoles req = mapFindAllRoles(ctx);

    service.getActiveRoles(req)
        .onSuccess(resp -> {
          ctx.response()
              .putHeader("Content-Type", "application/json")
              .end(Json.encode(resp));
        });
  }

  public void findTrashed(RoutingContext ctx) {
    FindAllRoles req = mapFindAllRoles(ctx);

    service.getTrashedRoles(req)
        .onSuccess(resp -> {
          ctx.response()
              .putHeader("Content-Type", "application/json")
              .end(Json.encode(resp));
        });
  }

  public void findById(RoutingContext ctx) {
    Integer roleId = Integer.parseInt(ctx.pathParam("id"));
    service.getRoleById(roleId)
        .onSuccess(role -> {
          ctx.response().setStatusCode(200).putHeader("Content-Type", "application/json")
              .end(Json.encode(role));
        });
  }

  public void create(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    CreateRoleRequest req = CreateRoleRequest.builder().name(body.getString("roleName")).build();

    service.createRole(req)
        .onSuccess(created -> ctx.response()
            .putHeader("Content-Type", "application/json")
            .setStatusCode(200)
            .end(Json.encode(created)));
  }

  public void update(RoutingContext ctx) {
    Integer roleId = Integer.parseInt(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    UpdateRoleRequest updateRoleRequest = UpdateRoleRequest.builder().roleId(roleId).name(body.getString("roleName"))
        .build();

    service.updateRole(updateRoleRequest)
        .onSuccess(v -> ctx.response().setStatusCode(200).end(Json.encode(v)));
  }

  public void trashed(RoutingContext ctx) {
    Integer roleId = Integer.parseInt(ctx.pathParam("id"));
    service.trashed(roleId)
        .onSuccess(v -> ctx.response().setStatusCode(200).end(Json.encode(v)));
  }

  public void restore(RoutingContext ctx) {
    Integer roleId = Integer.parseInt(ctx.pathParam("id"));
    service.restore(roleId)
        .onSuccess(v -> ctx.response().setStatusCode(200).end(Json.encode(v)));
  }

  public void deletePermanent(RoutingContext ctx) {
    Integer roleId = Integer.parseInt(ctx.pathParam("id"));
    service.deletePermanent(roleId)
        .onSuccess(v -> ctx.response().setStatusCode(200).end(Json.encode(v)));
  }

  private FindAllRoles mapFindAllRoles(RoutingContext ctx) {
    FindAllRoles req = new FindAllRoles();

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

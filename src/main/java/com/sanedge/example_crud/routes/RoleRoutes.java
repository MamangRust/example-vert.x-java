package com.sanedge.example_crud.routes;

import com.sanedge.example_crud.handler.RoleHandler;
import com.sanedge.example_crud.middleware.JwtMiddleware;
import com.sanedge.example_crud.middleware.RoleMiddleware;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;

public final class RoleRoutes {
  private RoleRoutes() {
  }

  public static void mount(
      Router router,
      JWTAuth jwtAuth,
      RoleHandler roleHandler) {

    router.route("/roles*")
        .handler(JwtMiddleware.jwt(jwtAuth));

    router.get("/roles")
        .handler(RoleMiddleware.requireRole("ADMIN"))
        .handler(roleHandler::findAll);

    router.get("/roles")
        .handler(RoleMiddleware.requireRole("ADMIN"))
        .handler(roleHandler::findActive);

    router.get("/roles")
        .handler(RoleMiddleware.requireRole("ADMIN"))
        .handler(roleHandler::findTrashed);

    router.get("/roles/:id")
        .handler(roleHandler::findById);

    router.post("/roles")
        .handler(roleHandler::create);

    router.post("/roles/:id")
        .handler(roleHandler::update);

    router.post("/roles/restore/:id")
        .handler(roleHandler::restore);

    router.post("/roles/trashed/:id")
        .handler(roleHandler::trashed);

    router.delete("/roles/deletePermanent/:id")
        .handler(roleHandler::deletePermanent);
  }
}

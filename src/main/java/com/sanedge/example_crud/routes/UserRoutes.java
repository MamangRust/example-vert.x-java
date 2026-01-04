package com.sanedge.example_crud.routes;

import com.sanedge.example_crud.handler.UserHandler;
import com.sanedge.example_crud.middleware.JwtMiddleware;
import com.sanedge.example_crud.middleware.RoleMiddleware;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;

public final class UserRoutes {

  private UserRoutes() {
  }

  public static void mount(
      Router router,
      JWTAuth jwtAuth,
      UserHandler userHandler) {

    router.route("/users*")
        .handler(JwtMiddleware.jwt(jwtAuth));

    router.get("/users")
        .handler(RoleMiddleware.requireRole("ADMIN"))
        .handler(userHandler::findAll);

    router.get("/users")
        .handler(RoleMiddleware.requireRole("ADMIN"))
        .handler(userHandler::findActive);

    router.get("/users")
        .handler(RoleMiddleware.requireRole("ADMIN"))
        .handler(userHandler::findTrashed);

    router.get("/users/:id")
        .handler(userHandler::findById);

    router.post("/users/update/:id")
        .handler(userHandler::update);

    router.post("/users/restore/:id")
        .handler(userHandler::restore);

    router.post("/users/trashed/:id")
        .handler(userHandler::trashed);

    router.delete("/users/deletePermanent/:id")
        .handler(userHandler::deletePermanent);
  }
}

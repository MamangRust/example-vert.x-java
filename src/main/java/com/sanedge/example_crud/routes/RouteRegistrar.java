package com.sanedge.example_crud.routes;

import com.sanedge.example_crud.handler.AuthHandler;
import com.sanedge.example_crud.handler.RoleHandler;
import com.sanedge.example_crud.handler.UserHandler;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public final class RouteRegistrar {

  private RouteRegistrar() {
  }

  public static Router register(
      Vertx vertx,
      JWTAuth jwtAuth,
      AuthHandler authHandler,
      UserHandler userHandler, RoleHandler roleHandler) {

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    AuthRoutes.mount(router, jwtAuth, authHandler);
    UserRoutes.mount(router, jwtAuth, userHandler);
    HealthRoutes.mount(router);
    RoleRoutes.mount(router, jwtAuth, roleHandler);

    return router;
  }
}

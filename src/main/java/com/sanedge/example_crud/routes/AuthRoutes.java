package com.sanedge.example_crud.routes;

import com.sanedge.example_crud.handler.AuthHandler;
import com.sanedge.example_crud.middleware.JwtMiddleware;

import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;

public final class AuthRoutes {

  private AuthRoutes() {
  }

  public static void mount(
      Router router,
      JWTAuth jwtAuth,
      AuthHandler authHandler) {
    router.post("/register").handler(authHandler::register);
    router.post("/login").handler(authHandler::login);
    router.post("/refresh-token").handler(authHandler::refreshToken);

    router.get("/me").handler(JwtMiddleware.jwt(jwtAuth)).handler(authHandler::getMe);
    router.get("/logout").handler(JwtMiddleware.jwt(jwtAuth)).handler(authHandler::logout);
  }
}

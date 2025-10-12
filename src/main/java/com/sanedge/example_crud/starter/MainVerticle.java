package com.sanedge.example_crud.starter;

import com.sanedge.example_crud.config.JwtConfig;
import com.sanedge.example_crud.handler.AuthHandler;
import com.sanedge.example_crud.handler.UserHandler;
import com.sanedge.example_crud.repository.AuthRepository;
import com.sanedge.example_crud.repository.UserRepository;
import com.sanedge.example_crud.service.AuthService;
import com.sanedge.example_crud.service.UserService;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() {
    PgConnectOptions connectOptions = new PgConnectOptions()
        .setPort(5432)
        .setHost("172.17.0.2")
        .setDatabase("example_vertx_crud")
        .setUser("postgress")
        .setPassword("password");

    JWTAuth jwtProvider = JwtConfig.createProvider(vertx);

    PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
    Pool client = Pool.pool(vertx, connectOptions, poolOptions);

    AuthRepository authRepo = new AuthRepository(client);
    AuthService authService = new AuthService(authRepo, jwtProvider);
    AuthHandler authHandler = new AuthHandler(authService);

    UserRepository userRepo = new UserRepository(client);
    UserService userService = new UserService(userRepo);
    UserHandler userHandler = new UserHandler(userService);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.post("/register").handler(authHandler::register);
    router.post("/login").handler(authHandler::login);

    router.route("/users*").handler(JWTAuthHandler.create(jwtProvider));

    router.get("/users").handler(ctx -> {
      String role = ctx.user().principal().getString("role");
      if (!"admin".equals(role)) {
        ctx.response().setStatusCode(403).end("Forbidden");
        return;
      }
      userHandler.findAll(ctx);
    });

    router.get("/users/:id").handler(userHandler::findById);
    router.put("/users/:id").handler(userHandler::update);
    router.delete("/users/:id").handler(userHandler::delete);

    vertx.createHttpServer()
        .requestHandler(router)
        .listen(8888)
        .onSuccess(s -> System.out.println("✅ Server running on http://localhost:8888"));
  }
}

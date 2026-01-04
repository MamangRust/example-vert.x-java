package com.sanedge.example_crud.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.example_crud.config.FlywayConfig;
import com.sanedge.example_crud.config.JwtConfig;
import com.sanedge.example_crud.config.RedisConfig;
import com.sanedge.example_crud.config.TelemetryConfig;
import com.sanedge.example_crud.domain.response.api.ApiResponse;
import com.sanedge.example_crud.exception.ApiException;
import com.sanedge.example_crud.handler.AuthHandler;
import com.sanedge.example_crud.handler.RoleHandler;
import com.sanedge.example_crud.handler.UserHandler;
import com.sanedge.example_crud.repository.RefreshTokenRepository;
import com.sanedge.example_crud.repository.RoleRepository;
import com.sanedge.example_crud.repository.UserRepository;
import com.sanedge.example_crud.repository.UserRoleRepository;
import com.sanedge.example_crud.routes.RouteRegistrar;
import com.sanedge.example_crud.seeder.DatabaseSeeder;
import com.sanedge.example_crud.service.AuthService;
import com.sanedge.example_crud.service.RedisService;
import com.sanedge.example_crud.service.RoleService;
import com.sanedge.example_crud.service.UserService;

import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.tracing.opentelemetry.OpenTelemetryOptions;

public class MainVerticle extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class.getName());

  private OpenTelemetry telemetry;

  public MainVerticle() {
  }

  public MainVerticle(OpenTelemetry telemetry) {
    this.telemetry = telemetry;
  }

  private void setupGlobalErrorHandler(Router router) {
    router.errorHandler(404, this::handleNotFound);
    router.errorHandler(500, this::handleGeneralFailure);
  }

  private void handleNotFound(RoutingContext ctx) {
    ApiResponse<Object> response = ApiResponse.error("Resource not found");
    ctx.response()
        .setStatusCode(404)
        .end(Json.encodePrettily(response));
  }

  private void handleGeneralFailure(RoutingContext ctx) {
    Throwable failure = ctx.failure();
    int statusCode = 500;
    String message = "An unexpected error occurred";

    if (failure instanceof ApiException) {
      ApiException apiException = (ApiException) failure;
      statusCode = apiException.getStatusCode();
      message = apiException.getMessage();
    } else {
      logger.error("Unexpected error", failure);
    }

    ApiResponse<Object> response = ApiResponse.error(message);

    HttpServerResponse res = ctx.response();
    res.setStatusCode(statusCode);
    res.putHeader("Content-Type", "application/json");
    res.end(Json.encodePrettily(response));
  }

  public static void main(String[] args) {
    JsonObject config = new JsonObject()
        .put("service.name", System.getenv().getOrDefault("OTEL_SERVICE_NAME", "app"))
        .put("service.version", System.getenv().getOrDefault("OTEL_SERVICE_VERSION", "1.0.0"))
        .put("otel.exporter.otlp.endpoint",
            System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT", "http://otel-collector:4317"))
        .put("otel.jfr.enabled", Boolean.parseBoolean(System.getenv().getOrDefault("OTEL_JFR_ENABLED", "true")));

    TelemetryConfig telemetryConfig = new TelemetryConfig(config);
    OpenTelemetry telemetry = telemetryConfig.initialize();

    VertxOptions options = new VertxOptions()
        .setTracingOptions(new OpenTelemetryOptions(telemetry));

    Vertx vertx = Vertx.vertx(options);

    vertx.deployVerticle(new MainVerticle(telemetry))
        .onSuccess(id -> {
          logger.info("✅ MainVerticle deployed successfully with ID: " + id);
          logger.info("📊 OpenTelemetry initialized - traces will be sent to: " +
              config.getString("otel.exporter.otlp.endpoint"));
          logger.info("🔍 Service name: " + config.getString("service.name"));
          logger.info("🌍 Deployment environment: " +
              System.getenv().getOrDefault("DEPLOYMENT_ENV", "local"));
        })
        .onFailure(err -> {
          logger.error("❌ Failed to deploy MainVerticle: " + err.getMessage());
          err.printStackTrace();
          System.exit(1);
        });

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutting down application...");

      vertx.close()
          .onSuccess(v -> {
            logger.info("Vertx shutdown complete");
            telemetryConfig.shutdown();
          })
          .onFailure(err -> {
            logger.error("Error during Vertx shutdown", err);
            telemetryConfig.shutdown();
          });
    }));

  }

  @Override
  public void start() {
    if (telemetry == null) {
      logger.warn("Telemetry not initialized in constructor, creating new instance");
      JsonObject config = new JsonObject()
          .put("service.name", System.getenv().getOrDefault("OTEL_SERVICE_NAME", "app"))
          .put("service.version", System.getenv().getOrDefault("OTEL_SERVICE_VERSION", "1.0.0"))
          .put("otel.exporter.otlp.endpoint",
              System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT", "http://otel-collector:4317"))
          .put("otel.jfr.enabled",
              Boolean.parseBoolean(System.getenv().getOrDefault("OTEL_JFR_ENABLED", "true")));

      this.telemetry = new TelemetryConfig(config).initialize();
    }

    PgConnectOptions connectOptions = new PgConnectOptions()
        .setPort(5432)
        .setHost("app-db")
        .setDatabase("example_vertx_crud")
        .setUser("postgress")
        .setPassword("password");

    FlywayConfig.runMigrations(connectOptions);

    JWTAuth jwtProvider = JwtConfig.createProvider(vertx);

    PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
    Pool client = Pool.pool(vertx, connectOptions, poolOptions);

    RedisAPI redisAPI = RedisConfig.createClient(vertx);
    RedisService redisService = new RedisService(redisAPI, telemetry);

    redisService.ping()
        .onSuccess(response -> logger.info("✅ Redis connected successfully: {}", response))
        .onFailure(err -> logger.error("❌ Failed to connect to Redis: {}", err.getMessage()));

    DatabaseSeeder.runSeeder(client, true);

    UserRepository userRepo = new UserRepository(client);
    RoleRepository roleRepo = new RoleRepository(client);
    RefreshTokenRepository refreshTokenRepository = new RefreshTokenRepository(client);
    UserRoleRepository userRoleRepo = new UserRoleRepository(client);

    UserService userService = new UserService(userRepo, roleRepo, userRoleRepo, redisService, telemetry);
    UserHandler userHandler = new UserHandler(userService);

    AuthService authService = new AuthService(userRepo, refreshTokenRepository, redisService, jwtProvider, telemetry);
    AuthHandler authHandler = new AuthHandler(authService, userService);

    RoleService roleService = new RoleService(roleRepo, redisService, telemetry);
    RoleHandler roleHandler = new RoleHandler(roleService);

    Router router = RouteRegistrar.register(vertx, jwtProvider, authHandler, userHandler, roleHandler);

    setupGlobalErrorHandler(router);

    vertx.createHttpServer()
        .requestHandler(router)
        .listen(8888)
        .onSuccess(s -> logger.info("✅ Server running on http://localhost:8888"));
  }
}

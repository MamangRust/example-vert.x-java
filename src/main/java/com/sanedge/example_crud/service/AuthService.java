package com.sanedge.example_crud.service;

import com.sanedge.example_crud.domain.requests.user.CreateUserRequest;
import com.sanedge.example_crud.domain.response.TokenResponse;
import com.sanedge.example_crud.domain.response.api.ApiResponse;
import com.sanedge.example_crud.domain.response.user.UserResponse;
import com.sanedge.example_crud.model.Role;
import com.sanedge.example_crud.model.User;
import com.sanedge.example_crud.repository.RefreshTokenRepository;
import com.sanedge.example_crud.repository.UserRepository;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {
  private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
  private final UserRepository repo;
  private final RefreshTokenRepository refreshTokenRepository;
  private final RedisService redisService;
  private final JWTAuth jwtProvider;
  private final Tracer tracer;
  private final LongCounter requestsTotal;
  private final DoubleHistogram requestDurationSeconds;

  public AuthService(
      UserRepository repo,
      RefreshTokenRepository refreshTokenRepository,
      RedisService redisService,
      JWTAuth jwtProvider,
      OpenTelemetry openTelemetry) {
    Tracer tracer = openTelemetry.getTracer("auth-service", "1.0.0");
    Meter meter = openTelemetry.getMeter("auth-service");

    this.repo = repo;
    this.refreshTokenRepository = refreshTokenRepository;
    this.redisService = redisService;
    this.jwtProvider = jwtProvider;
    this.tracer = tracer;
    this.requestsTotal = meter.counterBuilder("requests_total")
        .setDescription("Total number of requests")
        .build();
    this.requestDurationSeconds = meter.histogramBuilder("request_duration_seconds")
        .setDescription("Request duration in seconds")
        .setUnit("s")
        .build();
  }

  public Future<ApiResponse<TokenResponse>> login(String email, String password) {
    Span span = tracer.spanBuilder("AuthService.login")
        .setAttribute("auth.email", email)
        .startSpan();

    long startTime = System.currentTimeMillis();
    String userCacheKey = "user:email:" + email;

    return redisService.get(userCacheKey)
        .compose(cachedUser -> repo.getUserByEmailWithRoles(email))
        .compose(user -> {
          if (user == null) {
            return Future.failedFuture("User not found");
          }

          BCrypt.Result res = BCrypt.verifyer()
              .verify(password.toCharArray(), user.getPassword());

          if (!res.verified) {
            return Future.failedFuture("Invalid password");
          }

          JsonObject userCache = new JsonObject()
              .put("userId", user.getUserId())
              .put("email", user.getEmail())
              .put("roles", user.getRoles().stream().map(Role::getRoleName).toList());

          redisService.set(userCacheKey, userCache.encode(), Duration.ofMinutes(10))
              .onFailure(err -> logger.warn("Failed to cache user {}: {}", email, err.getMessage()));

          String accessToken = generateAccessToken(user);
          String jti = UUID.randomUUID().toString();
          String refreshTokenStr = generateRefreshToken(user.getUserId(), jti);
          LocalDateTime refreshExpiry = LocalDateTime.now().plusDays(7);

          return refreshTokenRepository.deleteByUserId(user.getUserId())
              .recover(err -> {
                logger.warn("Failed to delete refresh token for user {}: {}",
                    user.getUserId(), err.getMessage());
                return Future.succeededFuture();
              })
              .compose(v -> refreshTokenRepository.create(user.getUserId(), refreshTokenStr, refreshExpiry))
              .recover(err -> {
                logger.error("Failed to create refresh token - database schema issue: {}",
                    err.getMessage());
                return Future.failedFuture("Database configuration error. Please contact administrator.");
              })
              .compose(rt -> {
                String sessionCacheKey = "session:" + user.getUserId();
                JsonObject sessionData = new JsonObject()
                    .put("userId", user.getUserId())
                    .put("email", user.getEmail())
                    .put("accessToken", accessToken)
                    .put("refreshToken", rt.getToken())
                    .put("roles", user.getRoles().stream().map(Role::getRoleName).toList());
                return redisService.set(sessionCacheKey, sessionData.encode(), Duration.ofHours(1))
                    .map(v -> rt);
              })
              .map(rt -> {
                TokenResponse tokenResponse = TokenResponse.builder()
                    .access_token(accessToken)
                    .refresh_token(rt.getToken())
                    .build();
                span.setAttribute("auth.success", true);
                span.setAttribute("auth.user_id", user.getUserId());
                recordRequestMetrics("login", "success", startTime);
                span.end();
                return ApiResponse.success("Login success", tokenResponse);
              });

        })
        .onFailure(err -> {
          span.recordException(err);
          recordRequestMetrics("login", "failed", startTime);
          span.end();
        });
  }

  public Future<ApiResponse<UserResponse>> register(CreateUserRequest user) {
    Span span = tracer.spanBuilder("AuthService.register")
        .setAttribute("user.email", user.getEmail())
        .setAttribute("user.firstname", user.getFirstName())
        .setAttribute("user.lastname", user.getLastName())
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("Registration attempt for email: {}", user.getEmail());

    String hashed = BCrypt.withDefaults()
        .hashToString(12, user.getPassword().toCharArray());
    user.setPassword(hashed);

    return repo.createUser(user)
        .map(createdUser -> {
          UserResponse userResponse = UserResponse.from(createdUser);

          span.setAttribute("auth.success", true);
          span.setAttribute("auth.user_id", createdUser.getUserId());
          recordRequestMetrics("register", "success", startTime);

          return ApiResponse.success("User created", userResponse);
        })
        .recover(err -> {
          logger.error("Registration failed for email: {}", user.getEmail(), err);

          span.recordException(err);
          span.setAttribute("auth.success", false);
          recordRequestMetrics("register", "failed", startTime);

          return Future.succeededFuture(
              ApiResponse.<UserResponse>error(
                  "Failed to register user: " + err.getMessage()));
        })
        .onComplete(v -> span.end());
  }

  public Future<ApiResponse<String>> logout(Integer userId) {
    Span span = tracer.spanBuilder("AuthService.logout")
        .setAttribute("auth.user_id", userId)
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("Attempting to logout user: {}", userId);

    String sessionCacheKey = "session:" + userId;

    return refreshTokenRepository.deleteByUserId(userId)
        .compose(v -> redisService.delete(sessionCacheKey))
        .map(deletedCount -> {
          logger.info("User {} logged out successfully. {} cache keys deleted.", userId, deletedCount);
          span.setAttribute("auth.success", true);
          recordRequestMetrics("logout", "success", startTime);
          span.end();

          return ApiResponse.success("Logged out successfully", "Session and refresh tokens cleared");
        })
        .recover(throwable -> {
          logger.error("Failed to logout user: {}", userId, throwable);
          span.recordException(throwable);
          span.setAttribute("auth.success", false);
          recordRequestMetrics("logout", "failed", startTime);
          span.end();

          return Future.succeededFuture(ApiResponse.error("Failed to logout: " + throwable.getMessage()));
        });
  }

  private void recordRequestMetrics(String operation, String result, long startTime) {
    long duration = System.currentTimeMillis() - startTime;
    double durationSeconds = duration / 1000.0;

    requestsTotal.add(1, io.opentelemetry.api.common.Attributes.builder()
        .put("service", "auth")
        .put("operation", operation)
        .put("result", result)
        .build());

    requestDurationSeconds.record(durationSeconds, io.opentelemetry.api.common.Attributes.builder()
        .put("service", "auth")
        .put("operation", operation)
        .put("result", result)
        .build());
  }

  private String generateAccessToken(User user) {
    return jwtProvider.generateToken(
        new JsonObject()
            .put("sub", "access")
            .put("userId", user.getUserId())
            .put("email", user.getEmail())
            .put(
                "roleNames",
                user.getRoles()
                    .stream()
                    .map(Role::getRoleName)
                    .toList()),
        new JWTOptions().setExpiresInMinutes(60));
  }

  private String generateRefreshToken(Integer userId, String jti) {
    return jwtProvider.generateToken(
        new JsonObject()
            .put("sub", "refresh")
            .put("userId", userId)
            .put("jti", jti),
        new JWTOptions().setExpiresInMinutes(60 * 24 * 7));
  }

  public Future<ApiResponse<TokenResponse>> refreshToken(String refreshTokenStr) {
    Span span = tracer.spanBuilder("AuthService.refreshToken")
        .startSpan();

    long startTime = System.currentTimeMillis();

    return refreshTokenRepository.findByToken(refreshTokenStr)
        .compose(refreshToken -> {
          if (refreshToken == null) {
            return Future.failedFuture("Invalid or expired refresh token");
          }

          LocalDateTime now = LocalDateTime.now();
          LocalDateTime expiry = refreshToken.getExpiration();
          boolean needsRenewal = expiry.minusDays(1).isBefore(now);

          return repo.getUserByIdWithRoles(refreshToken.getUserId())
              .compose(user -> {
                if (user == null) {
                  return Future.failedFuture("User not found");
                }

                String accessToken = generateAccessToken(user);
                Future<Void> renewalFuture;
                String finalRefreshTokenStr;

                if (needsRenewal) {
                  String jti = UUID.randomUUID().toString();
                  finalRefreshTokenStr = generateRefreshToken(user.getUserId(), jti);
                  LocalDateTime refreshExpiry = LocalDateTime.now().plusDays(7);

                  renewalFuture = refreshTokenRepository.deleteByUserId(refreshToken.getUserId())
                      .compose(
                          v -> refreshTokenRepository.create(user.getUserId(), finalRefreshTokenStr, refreshExpiry))
                      .map(createdUser -> null);
                } else {
                  finalRefreshTokenStr = refreshTokenStr;
                  renewalFuture = Future.succeededFuture();
                }

                return renewalFuture.compose(v -> {
                  String sessionCacheKey = "session:" + user.getUserId();
                  JsonObject sessionData = new JsonObject()
                      .put("userId", user.getUserId())
                      .put("email", user.getEmail())
                      .put("accessToken", accessToken)
                      .put("refreshToken", finalRefreshTokenStr)
                      .put("roles", user.getRoles().stream().map(Role::getRoleName).toList());

                  redisService.set(sessionCacheKey, sessionData.encode(), Duration.ofHours(1))
                      .onFailure(err -> logger.warn("Failed to cache updated session: {}", err.getMessage()));

                  TokenResponse tokenResponse = TokenResponse.builder()
                      .access_token(accessToken)
                      .refresh_token(finalRefreshTokenStr)
                      .build();

                  span.setAttribute("auth.success", true);
                  span.setAttribute("auth.user_id", user.getUserId());
                  span.setAttribute("auth.renewed", needsRenewal);
                  recordRequestMetrics("refresh_token", "success", startTime);
                  span.end();

                  return Future.succeededFuture(ApiResponse.success("Token refreshed successfully", tokenResponse));
                });
              });
        })
        .recover(err -> {
          span.recordException(err);
          recordRequestMetrics("refresh_token", "failed", startTime);
          span.end();

          return Future.succeededFuture(ApiResponse.error("Failed to refresh token: " + err.getMessage()));
        });
  }
}

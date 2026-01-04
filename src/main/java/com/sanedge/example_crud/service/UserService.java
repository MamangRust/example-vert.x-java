package com.sanedge.example_crud.service;

import java.time.Duration;
import java.util.List;

import com.sanedge.example_crud.domain.requests.user.CreateUserRequest;
import com.sanedge.example_crud.domain.requests.user.FindAllUsers;
import com.sanedge.example_crud.domain.requests.user.UpdateUserRequest;
import com.sanedge.example_crud.domain.response.api.ApiResponse;
import com.sanedge.example_crud.domain.response.api.ApiResponsePagination;
import com.sanedge.example_crud.domain.response.api.PagedResult;
import com.sanedge.example_crud.domain.response.api.PaginationMeta;
import com.sanedge.example_crud.domain.response.user.UserResponse;
import com.sanedge.example_crud.domain.response.user.UserResponseDeleteAt;
import com.sanedge.example_crud.exception.NotFoundException;
import com.sanedge.example_crud.model.User;
import com.sanedge.example_crud.model.UserRole;
import com.sanedge.example_crud.repository.RoleRepository;
import com.sanedge.example_crud.repository.UserRepository;
import com.sanedge.example_crud.repository.UserRoleRepository;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {
  private static final Logger logger = LoggerFactory.getLogger(UserService.class);
  private final UserRepository repository;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final RedisService redisService;
  private final Tracer tracer;
  private final LongCounter requestsTotal;
  private final DoubleHistogram requestDurationSeconds;

  public UserService(UserRepository repository, RoleRepository roleRepository, UserRoleRepository userRoleRepository,
      RedisService redisService, OpenTelemetry openTelemetry) {
    Tracer tracer = openTelemetry.getTracer("user-service", "1.0.0");
    Meter meter = openTelemetry.getMeter("user-service");

    this.repository = repository;
    this.roleRepository = roleRepository;
    this.userRoleRepository = userRoleRepository;
    this.redisService = redisService;
    this.tracer = tracer;
    this.requestsTotal = meter.counterBuilder("requests_total")
        .setDescription("Total number of requests")
        .build();
    this.requestDurationSeconds = meter.histogramBuilder("request_duration_seconds")
        .setDescription("Request duration in seconds")
        .setUnit("s")
        .build();
  }

  public Future<ApiResponsePagination<List<UserResponse>>> getAllUsers(
      FindAllUsers req) {
    Span span = tracer.spanBuilder("UserService.getAllUsers").startSpan();
    long startTime = System.currentTimeMillis();

    int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    logger.info(
        "Fetching users | search={}, page={}, pageSize={}",
        keyword, page, pageSize);

    req.setPage(page);
    req.setPageSize(pageSize);
    req.setSearch(keyword);

    return repository.getUsers(req)
        .map(result -> mapUserPagination(startTime, span,
            result, req, "Users users fetched successfully"))
        .onFailure(throwable -> {
          logger.error("Failed to fetch users", throwable);

          span.recordException(throwable);
          span.setAttribute("users.success", false);

          recordRequestMetrics("get_all", "failed", startTime);
          span.end();
        });
  }

  public Future<ApiResponsePagination<List<UserResponseDeleteAt>>> getActiveUsers(
      FindAllUsers req) {
    Span span = tracer.spanBuilder("UserService.getActiveUsers").startSpan();
    long startTime = System.currentTimeMillis();

    int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    logger.info(
        "Fetching users | search={}, page={}, pageSize={}",
        keyword, page, pageSize);

    req.setPage(page);
    req.setPageSize(pageSize);
    req.setSearch(keyword);

    return repository.getActiveUsers(req)
        .map(result -> mapUserPaginationDeleteAt("get_active", startTime, span,
            result, req, "Users users fetched successfully"))
        .onFailure(throwable -> {
          logger.error("Failed to fetch users", throwable);

          span.recordException(throwable);
          span.setAttribute("users.success", false);

          recordRequestMetrics("get_active", "failed", startTime);
          span.end();
        });
  }

  public Future<ApiResponsePagination<List<UserResponseDeleteAt>>> getTrashedUsers(
      FindAllUsers req) {
    Span span = tracer.spanBuilder("UserService.getActiveUsers").startSpan();
    long startTime = System.currentTimeMillis();

    int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    logger.info(
        "Fetching users | search={}, page={}, pageSize={}",
        keyword, page, pageSize);

    req.setPage(page);
    req.setPageSize(pageSize);
    req.setSearch(keyword);

    return repository.getTrashedUsers(req)
        .map(result -> mapUserPaginationDeleteAt("get_trashed", startTime, span,
            result, req, "Users users fetched successfully"))
        .onFailure(throwable -> {
          logger.error("Failed to fetch users", throwable);

          span.recordException(throwable);
          span.setAttribute("users.success", false);

          recordRequestMetrics("get_trashed", "failed", startTime);
          span.end();
        });
  }

  public Future<ApiResponse<UserResponse>> createUser(CreateUserRequest req) {
    Span span = tracer.spanBuilder("UserService.createUser")
        .setAttribute("user.email", req.getEmail())
        .setAttribute("user.firstname", req.getFirstName())
        .setAttribute("user.lastname", req.getLastName())
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("Creating user: {} {}, email: {}", req.getFirstName(), req.getLastName(), req.getEmail());

    String hashed = BCrypt.withDefaults().hashToString(12, req.getPassword().toCharArray());
    req.setPassword(hashed);

    return repository.createUser(req)
        .compose((User createdUser) -> {
          logger.info("User created in DB: {}, user_id: {}", createdUser.getEmail(), createdUser.getUserId());
          return roleRepository.getRoleByName("ADMIN")
              .compose(role -> {
                if (role == null) {
                  return Future
                      .failedFuture(new IllegalStateException("Default 'ADMIN' role not found in the database."));
                }

                UserRole userRole = new UserRole();

                userRole.setRoleId(role.getRoleId());
                userRole.setUserId(createdUser.getUserId());

                return userRoleRepository.assignRoleToUser(userRole)
                    .map(v -> createdUser);
              });
        })
        .map(createdUser -> {
          logger.info("User created and role assigned successfully: {}, user_id: {}", createdUser.getEmail(),
              createdUser.getUserId());

          UserResponse userResponse = UserResponse.from(createdUser);

          span.setAttribute("user.success", true);
          span.setAttribute("user.user_id", createdUser.getUserId());
          recordRequestMetrics("create", "success", startTime);
          span.end();

          return ApiResponse.success("User created successfully", userResponse);
        })
        .recover(throwable -> {
          logger.error("Failed to create user: {}", req.getEmail(), throwable);
          span.recordException(throwable);
          span.setAttribute("user.success", false);
          recordRequestMetrics("create", "failed", startTime);
          span.end();

          return Future.succeededFuture(ApiResponse.error("Failed to create user: " + throwable.getMessage()));
        });
  }

  public Future<ApiResponse<UserResponse>> getUserById(Integer userId) {
    Span span = tracer.spanBuilder("UserService.getUserById")
        .setAttribute("user.id", userId)
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("Fetching user by id: {}", userId);

    String cacheKey = "user:" + userId;

    return redisService.get(cacheKey)
        .compose(cachedUser -> {
          if (cachedUser != null && !cachedUser.isEmpty()) {
            logger.info("User {} found in cache", userId);
            span.setAttribute("user.cache_hit", true);
            recordRequestMetrics("get_by_id", "success", startTime);
            span.end();

            try {
              User user = User.fromJson(new JsonObject(cachedUser));

              return Future.succeededFuture(ApiResponse.success(
                  "User fetched successfully (from cache)",
                  UserResponse.from(user)));
            } catch (Exception e) {
              logger.warn("Failed to parse cached user data for user {}: {}", userId, e.getMessage());
              return fetchUserFromDatabase(userId, span, startTime);
            }
          } else {
            return fetchUserFromDatabase(userId, span, startTime);
          }
        })
        .recover(err -> {
          logger.error("Failed to fetch user by id: {}", userId, err);
          span.recordException(err);
          span.setAttribute("user.success", false);
          recordRequestMetrics("get_by_id", "failed", startTime);
          span.end();

          return Future.succeededFuture(
              ApiResponse.<UserResponse>error(
                  "Failed to fetch user: " + err.getMessage()));
        });
  }

  private Future<ApiResponse<UserResponse>> fetchUserFromDatabase(Integer userId, Span span, long startTime) {
    span.setAttribute("user.cache_hit", false);

    return repository.getUserById(userId)
        .compose((User user) -> {
          if (user == null) {
            span.setAttribute("user.success", false);
            span.setAttribute("user.reason", "not_found");
            recordRequestMetrics("get_by_id", "not_found", startTime);
            span.end();

            return Future.succeededFuture(ApiResponse.<UserResponse>error("User not found"));
          }

          span.setAttribute("user.success", true);
          span.setAttribute("user.email", user.getEmail());

          String cacheKey = "user:" + userId;
          redisService.setJson(cacheKey, user.toJson(), Duration.ofMinutes(30))
              .onSuccess(v -> logger.debug("User {} cached successfully", userId))
              .onFailure(err -> logger.warn("Failed to cache user {}: {}", userId, err.getMessage()));

          recordRequestMetrics("get_by_id", "success", startTime);
          span.end();

          return Future.succeededFuture(ApiResponse.success(
              "User fetched successfully",
              UserResponse.from(user)));
        });
  }

  public Future<ApiResponse<UserResponse>> updateUser(UpdateUserRequest req) {
    Integer userId = req.getUserId();

    Span span = tracer.spanBuilder("UserService.updateUser")
        .setAttribute("user.id", userId)
        .setAttribute("user.email", req.getEmail())
        .setAttribute("user.firstname", req.getFirstName())
        .setAttribute("user.lastname", req.getLastName())
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("Updating user: {}, email: {}", userId, req.getEmail());

    return repository.updateUser(req)
        .compose(user -> {
          String cacheKey = "user:" + user.getUserId();
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("User {} cache invalidated", user.getUserId());
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for user {}: {}", user.getUserId(), err.getMessage()))
              .map(deleted -> user);
        })
        .map(user -> {
          logger.info("User updated successfully: {}", user.getUserId());
          span.setAttribute("user.success", true);
          recordRequestMetrics("update", "success", startTime);
          span.end();

          return ApiResponse.success(
              "User updated successfully",
              UserResponse.from(user));
        })
        .recover(err -> {
          logger.error("Failed to update user: {}", userId, err);
          span.recordException(err);
          span.setAttribute("user.success", false);
          recordRequestMetrics("update", "failed", startTime);
          span.end();

          return Future.succeededFuture(
              ApiResponse.<UserResponse>error(
                  "Failed to update user: " + err.getMessage()));
        });
  }

  public Future<ApiResponse<UserResponseDeleteAt>> trashed(Integer userId) {
    Span span = tracer.spanBuilder("UserService.trashed")
        .setAttribute("user.id", userId)
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("Trashed user: {}", userId);

    return repository.trashed(userId)
        .compose(user -> {
          if (user == null) {
            span.setAttribute("user.success", false);
            span.setAttribute("user.reason", "not_found");
            recordRequestMetrics("trashed", "not_found", startTime);
            span.end();
            throw new NotFoundException("User not found with id: " + userId);
          }

          String cacheKey = "user:" + userId;
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("User {} cache invalidated on trash", userId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for trashed user {}: {}", userId, err.getMessage()))
              .map(user);
        })
        .map(user -> {
          logger.info("User trashed successfully: {}", userId);
          UserResponseDeleteAt userResponseDeleteAt = UserResponseDeleteAt.from(user);

          span.setAttribute("user.success", true);
          recordRequestMetrics("delete", "success", startTime);
          span.end();

          return ApiResponse.success(
              "User trashed successfully",
              userResponseDeleteAt);
        })
        .recover(err -> {
          logger.error("Failed to trashed user: {}", userId, err);
          span.recordException(err);
          span.setAttribute("user.success", false);
          recordRequestMetrics("delete", "failed", startTime);
          span.end();

          return Future.succeededFuture(
              ApiResponse.<UserResponseDeleteAt>error(
                  "Failed to trashed user: " + err.getMessage()));
        });
  }

  public Future<ApiResponse<UserResponseDeleteAt>> restore(Integer userId) {
    Span span = tracer.spanBuilder("UserService.restore")
        .setAttribute("user.id", userId)
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("Restoring user: {}", userId);

    return repository.restore(userId)
        .compose(user -> {
          if (user == null) {
            span.setAttribute("user.success", false);
            span.setAttribute("user.reason", "not_found");
            recordRequestMetrics("restore", "not_found", startTime);
            span.end();

            throw new NotFoundException("User not found with id: " + userId);
          }

          String cacheKey = "user:" + userId;
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("User {} cache invalidated on restore", userId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for restored user {}: {}", userId, err.getMessage()))
              .map(user);
        })
        .map(user -> {
          logger.info("User restored successfully: {}", userId);
          UserResponseDeleteAt userResponseDeleteAt = UserResponseDeleteAt.from(user);

          span.setAttribute("user.success", true);
          recordRequestMetrics("restore", "success", startTime);
          span.end();

          return ApiResponse.success(
              "User restored successfully",
              userResponseDeleteAt);
        })
        .recover(err -> {
          logger.error("Failed to restore user: {}", userId, err);
          span.recordException(err);
          span.setAttribute("user.success", false);
          recordRequestMetrics("restore", "failed", startTime);
          span.end();

          return Future.succeededFuture(
              ApiResponse.<UserResponseDeleteAt>error(
                  "Failed to restore user: " + err.getMessage()));
        });
  }

  public Future<ApiResponse<Void>> deletePermanent(Integer userId) {
    Span span = tracer.spanBuilder("UserService.deleteUser")
        .setAttribute("user.id", userId)
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("Deleting user: {}", userId);

    return repository.deletePermanent(userId)
        .compose(v -> {
          String cacheKey = "user:" + userId;
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("User {} cache invalidated on permanent delete", userId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for deleted user {}: {}", userId, err.getMessage()))
              .map(v);
        })
        .map(v -> {
          logger.info("User deleted successfully: {}", userId);
          span.setAttribute("user.success", true);
          recordRequestMetrics("delete", "success", startTime);
          span.end();

          return ApiResponse.<Void>success("User deleted successfully", null);
        })
        .recover(throwable -> {
          logger.error("Failed to delete user: {}", userId, throwable);
          span.recordException(throwable);
          span.setAttribute("user.success", false);
          recordRequestMetrics("delete", "failed", startTime);
          span.end();

          return Future.succeededFuture(
              ApiResponse.<Void>error("Failed to delete user: " + throwable.getMessage()));
        });
  }

  private ApiResponsePagination<List<UserResponse>> mapUserPagination(
      long startTime,
      Span span,
      PagedResult<User> result,
      FindAllUsers req,
      String message) {

    int pageSize = req.getPageSize();
    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil(
        (double) totalRecords / pageSize);
    List<UserResponse> data = result.getData()
        .stream()
        .map(UserResponse::from)
        .toList();

    span.setAttribute("users.count", data.size());
    span.setAttribute("users.total_records", totalRecords);
    span.setAttribute("users.success", true);

    recordRequestMetrics("get_all", "success", startTime);
    span.end();

    return new ApiResponsePagination<>(
        "success",
        message,
        data,
        new PaginationMeta(
            req.getPage() + 1,
            pageSize,
            totalPages,
            totalRecords));
  }

  private ApiResponsePagination<List<UserResponseDeleteAt>> mapUserPaginationDeleteAt(
      String operation,
      long startTime,
      Span span,
      PagedResult<User> result,
      FindAllUsers req,
      String message) {

    int pageSize = req.getPageSize();
    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil(
        (double) totalRecords / pageSize);
    List<UserResponseDeleteAt> data = result.getData()
        .stream()
        .map(UserResponseDeleteAt::from)
        .toList();

    span.setAttribute("user.count", data.size());
    span.setAttribute("user.total_records", totalRecords);
    span.setAttribute("user.success", true);

    recordRequestMetrics(operation, "success", startTime);
    span.end();

    return new ApiResponsePagination<>(
        "success",
        message,
        data,
        new PaginationMeta(
            req.getPage() + 1,
            pageSize,
            totalPages,
            totalRecords));
  }

  private void recordRequestMetrics(String operation, String result, long startTime) {
    long duration = System.currentTimeMillis() - startTime;
    double durationSeconds = duration / 1000.0;

    requestsTotal.add(1, io.opentelemetry.api.common.Attributes.builder()
        .put("service", "user")
        .put("operation", operation)
        .put("result", result)
        .build());

    requestDurationSeconds.record(durationSeconds, io.opentelemetry.api.common.Attributes.builder()
        .put("service", "user")
        .put("operation", operation)
        .put("result", result)
        .build());
  }
}

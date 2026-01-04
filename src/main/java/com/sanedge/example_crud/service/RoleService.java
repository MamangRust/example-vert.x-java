package com.sanedge.example_crud.service;

import java.time.Duration;
import java.util.List;

import com.sanedge.example_crud.domain.requests.role.CreateRoleRequest;
import com.sanedge.example_crud.domain.requests.role.FindAllRoles;
import com.sanedge.example_crud.domain.requests.role.UpdateRoleRequest;
import com.sanedge.example_crud.domain.response.api.ApiResponse;
import com.sanedge.example_crud.domain.response.api.ApiResponsePagination;
import com.sanedge.example_crud.domain.response.api.PagedResult;
import com.sanedge.example_crud.domain.response.api.PaginationMeta;
import com.sanedge.example_crud.domain.response.role.RoleResponse;
import com.sanedge.example_crud.domain.response.role.RoleResponseDeleteAt;
import com.sanedge.example_crud.exception.NotFoundException;
import com.sanedge.example_crud.model.Role;
import com.sanedge.example_crud.repository.RoleRepository;

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

public class RoleService {
  private static final Logger logger = LoggerFactory.getLogger(RoleService.class);
  private final RoleRepository repo;
  private final RedisService redisService;
  private final Tracer tracer;
  private final LongCounter requestsTotal;
  private final DoubleHistogram requestDurationSeconds;

  public RoleService(
      RoleRepository repo,
      RedisService redisService,
      OpenTelemetry openTelemetry) {
    Tracer tracer = openTelemetry.getTracer("role-service", "1.0.0");
    Meter meter = openTelemetry.getMeter("role-service");

    this.repo = repo;
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

  public Future<ApiResponsePagination<List<RoleResponse>>> getAllRoles(
      FindAllRoles req) {

    Span span = tracer.spanBuilder("RoleService.getAllRoles").startSpan();
    long startTime = System.currentTimeMillis();

    int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    req.setSearch(keyword);

    logger.info(
        "Fetching roles | search={}, page={}, pageSize={}",
        req.getSearch(), page, pageSize);

    req.setPage(page);
    req.setPageSize(pageSize);
    req.setSearch(keyword);

    return repo.getRoles(req)
        .map(result -> mapRolePagination(startTime, span, result, req, keyword))
        .onFailure(throwable -> {
          logger.error("Failed to fetch roles", throwable);

          span.recordException(throwable);
          span.setAttribute("role.success", false);

          recordRequestMetrics("get_all", "failed", startTime);
          span.end();
        });
  }

  public Future<ApiResponsePagination<List<RoleResponseDeleteAt>>> getActiveRoles(
      FindAllRoles req) {

    Span span = tracer.spanBuilder("RoleService.getActiveRoles").startSpan();
    long startTime = System.currentTimeMillis();

    int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    req.setSearch(keyword);

    logger.info(
        "Fetching roles | search={}, page={}, pageSize={}",
        req.getSearch(), page, pageSize);

    return repo.getActiveRoles(req)
        .map(result -> mapRolePaginationDeleteAt("get_active", startTime, span, result, req, keyword))
        .onFailure(throwable -> {
          logger.error("Failed to fetch roles", throwable);

          span.recordException(throwable);
          span.setAttribute("role.success", false);

          recordRequestMetrics("get_active", "failed", startTime);
          span.end();
        });
  }

  public Future<ApiResponsePagination<List<RoleResponseDeleteAt>>> getTrashedRoles(
      FindAllRoles req) {
    Span span = tracer.spanBuilder("RoleService.getTrashedRoles").startSpan();
    long startTime = System.currentTimeMillis();

    int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    req.setSearch(keyword);

    logger.info(
        "Fetching roles | search={}, page={}, pageSize={}",
        req.getSearch(), page, pageSize);

    return repo.getTrashedRoles(req)
        .map(result -> mapRolePaginationDeleteAt("get_trashed", startTime, span, result, req, keyword))
        .onFailure(throwable -> {
          logger.error("Failed to fetch roles", throwable);

          span.recordException(throwable);
          span.setAttribute("role.success", false);

          recordRequestMetrics("get_trashed", "failed", startTime);
          span.end();
        });
  }

  public Future<ApiResponse<RoleResponse>> getRoleById(Integer roleId) {
    Span span = tracer.spanBuilder("RoleService.getRoleById")
        .setAttribute("role.id", roleId)
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("Fetching role by id: {}", roleId);

    String cacheKey = "role:" + roleId;

    return redisService.get(cacheKey)
        .compose(cachedRole -> {
          if (cachedRole != null && !cachedRole.isEmpty()) {
            logger.info("Role {} found in cache", roleId);
            span.setAttribute("role.cache_hit", true);
            recordRequestMetrics("get_by_id", "success", startTime);
            span.end();

            try {
              Role role = Role.fromJson(new JsonObject(cachedRole));
              return Future.succeededFuture(ApiResponse.success(
                  "Role fetched successfully (from cache)",
                  RoleResponse.from(role)));
            } catch (Exception e) {
              logger.warn("Failed to parse cached role data for role {}: {}", roleId, e.getMessage());
              return fetchRoleFromDatabase(roleId, span, startTime);
            }
          } else {
            return fetchRoleFromDatabase(roleId, span, startTime);
          }
        })
        .recover(err -> {
          logger.error("Failed to fetch role by id: {}", roleId, err);
          span.recordException(err);
          span.setAttribute("role.success", false);
          recordRequestMetrics("get_by_id", "failed", startTime);
          span.end();

          return Future.succeededFuture(
              ApiResponse.<RoleResponse>error(
                  "Failed to fetch role: " + err.getMessage()));
        });
  }

  private Future<ApiResponse<RoleResponse>> fetchRoleFromDatabase(Integer roleId, Span span, long startTime) {
    span.setAttribute("role.cache_hit", false);

    return repo.getRoleById(roleId)
        .compose((Role role) -> {
          if (role == null) {
            span.setAttribute("role.success", false);
            span.setAttribute("role.reason", "not_found");
            recordRequestMetrics("get_by_id", "not_found", startTime);
            span.end();

            return Future.succeededFuture(ApiResponse.<RoleResponse>error("Role not found"));
          }

          span.setAttribute("role.success", true);
          span.setAttribute("role.name", role.getRoleName());

          String cacheKey = "role:" + roleId;
          redisService.setJson(cacheKey, role.toJson(), Duration.ofMinutes(60))
              .onSuccess(v -> logger.debug("Role {} cached successfully", roleId))
              .onFailure(err -> logger.warn("Failed to cache role {}: {}", roleId, err.getMessage()));

          recordRequestMetrics("get_by_id", "success", startTime);
          span.end();

          return Future.succeededFuture(ApiResponse.success(
              "Role fetched successfully",
              RoleResponse.from(role)));
        });
  }

  public Future<ApiResponse<RoleResponse>> createRole(CreateRoleRequest req) {
    Span span = tracer.spanBuilder("RoleService.createRole")
        .setAttribute("role.name", req.getName())
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("Creating role: {}", req.getName());

    return repo.createRole(req)
        .map(created -> {
          span.setAttribute("role.success", true);
          span.setAttribute("role.id", created.getRoleId());
          recordRequestMetrics("create", "success", startTime);
          span.end();

          return ApiResponse.success(
              "Role created successfully",
              RoleResponse.from(created));
        })
        .recover(err -> {
          logger.error("Failed to create role: {}", req.getName(), err);
          span.recordException(err);
          span.setAttribute("role.success", false);
          recordRequestMetrics("create", "failed", startTime);
          span.end();

          return Future.succeededFuture(
              ApiResponse.<RoleResponse>error(
                  "Failed to create role: " + err.getMessage()));
        });
  }

  public Future<ApiResponse<RoleResponse>> updateRole(UpdateRoleRequest req) {
    Integer roleId = req.getRoleId();

    Span span = tracer.spanBuilder("RoleService.updateRole")
        .setAttribute("role.id", roleId)
        .setAttribute("role.name", req.getName())
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("Updating role: {}, name: {}", roleId, req.getName());

    return repo.updateRole(req)
        .compose((Role dota) -> {
          String cacheKey = "role:" + roleId;
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Role {} cache invalidated", roleId);
                }
              })
              .onFailure(err -> logger.warn("Failed to invalidate cache for role {}: {}", roleId, err.getMessage()))
              .map(dota);
        })
        .map((Role dota) -> {
          RoleResponse roleResponse = RoleResponse.from(dota);

          span.setAttribute("role.success", true);
          recordRequestMetrics("update", "success", startTime);
          span.end();

          return ApiResponse.success(
              "Role updated successfully",
              roleResponse);
        })
        .recover(err -> {
          logger.error("Failed to update role: {}", roleId, err);
          span.recordException(err);
          span.setAttribute("role.success", false);
          recordRequestMetrics("update", "failed", startTime);
          span.end();

          return Future.succeededFuture(
              ApiResponse.<RoleResponse>error(
                  "Failed to update role: " + err.getMessage()));
        });
  }

  public Future<ApiResponse<RoleResponseDeleteAt>> trashed(Integer roleId) {
    Span span = tracer.spanBuilder("RoleService.trashed")
        .setAttribute("role.id", roleId)
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("Trashed role: {}", roleId);

    return repo.trashed(roleId)
        .compose(role -> {
          if (role == null) {
            span.setAttribute("role.success", false);
            span.setAttribute("role.reason", "not_found");
            recordRequestMetrics("trashed", "not_found", startTime);
            span.end();

            throw new NotFoundException("Role not found with id: " + roleId);
          }
          String cacheKey = "role:" + roleId;
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Role {} cache invalidated on trash", roleId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for trashed role {}: {}", roleId, err.getMessage()))
              .map(role);
        })
        .map(role -> {
          RoleResponseDeleteAt roleResponseDeleteAt = RoleResponseDeleteAt.from(role);

          span.setAttribute("role.success", true);
          recordRequestMetrics("trashed", "success", startTime);
          span.end();

          return ApiResponse.success("Role trashed successfully", roleResponseDeleteAt);
        })
        .recover(err -> {
          logger.error("Failed to trashed role: {}", roleId, err);
          span.recordException(err);
          span.setAttribute("role.success", false);
          recordRequestMetrics("trashed", "failed", startTime);
          span.end();

          return Future.succeededFuture(
              ApiResponse.<RoleResponseDeleteAt>error(
                  "Failed to trashed role: " + err.getMessage()));
        });
  }

  public Future<ApiResponse<RoleResponseDeleteAt>> restore(Integer roleId) {
    Span span = tracer.spanBuilder("RoleService.restore")
        .setAttribute("role.id", roleId)
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("Restore role: {}", roleId);

    return repo.restore(roleId)
        .compose(role -> {
          String cacheKey = "role:" + roleId;
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Role {} cache invalidated on restore", roleId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for restored role {}: {}", roleId, err.getMessage()))
              .map(role);
        })
        .map(role -> {
          if (role == null) {
            span.setAttribute("role.success", false);
            span.setAttribute("role.reason", "not_found");
            recordRequestMetrics("restore", "not_found", startTime);
            span.end();

            throw new NotFoundException("Role not found with id: " + roleId);
          }

          RoleResponseDeleteAt response = RoleResponseDeleteAt.from(role);

          span.setAttribute("role.success", true);
          recordRequestMetrics("restore", "success", startTime);
          span.end();

          return ApiResponse.success(
              "Role restored successfully",
              response);
        })
        .recover(err -> {
          logger.error("Failed to restore role: {}", roleId, err);
          span.recordException(err);
          span.setAttribute("role.success", false);
          recordRequestMetrics("restore", "failed", startTime);
          span.end();

          return Future.succeededFuture(
              ApiResponse.<RoleResponseDeleteAt>error(
                  "Failed to restore role: " + err.getMessage()));
        });
  }

  public Future<ApiResponse<Void>> deletePermanent(Integer roleId) {
    Span span = tracer.spanBuilder("RoleService.deletePermanent")
        .setAttribute("role.id", roleId)
        .startSpan();

    long startTime = System.currentTimeMillis();
    logger.info("delete Permanent role: {}", roleId);

    return repo.deletePermanent(roleId)
        .compose(v -> {
          String cacheKey = "role:" + roleId;
          return redisService.delete(cacheKey)
              .onSuccess(deleted -> {
                if (deleted > 0) {
                  logger.debug("Role {} cache invalidated on permanent delete", roleId);
                }
              })
              .onFailure(
                  err -> logger.warn("Failed to invalidate cache for deleted role {}: {}", roleId, err.getMessage()))
              .map(v);
        })
        .map(v -> {
          logger.info("Role deleted successfully: {}", roleId);
          span.setAttribute("role.success", true);
          recordRequestMetrics("deletePermanent", "success", startTime);
          span.end();
          return ApiResponse.<Void>success("success", null);
        })
        .recover(throwable -> {
          logger.error("Failed to deletePermanent role: {}", roleId, throwable);
          span.recordException(throwable);
          span.setAttribute("role.success", false);
          recordRequestMetrics("deletePermanent", "failed", startTime);
          span.end();

          return Future.succeededFuture(
              ApiResponse.<Void>error("Failed to delete role: " + throwable.getMessage()));
        });
  }

  private ApiResponsePagination<List<RoleResponse>> mapRolePagination(
      long startTime,
      Span span,
      PagedResult<Role> result,
      FindAllRoles req,
      String message) {

    int pageSize = req.getPageSize();
    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil(
        (double) totalRecords / pageSize);
    List<RoleResponse> data = result.getData()
        .stream()
        .map(RoleResponse::from)
        .toList();

    span.setAttribute("roles.count", data.size());
    span.setAttribute("roles.total_records", totalRecords);
    span.setAttribute("roles.success", true);

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

  private ApiResponsePagination<List<RoleResponseDeleteAt>> mapRolePaginationDeleteAt(
      String operation,
      long startTime,
      Span span,
      PagedResult<Role> result,
      FindAllRoles req,
      String message) {

    int pageSize = req.getPageSize();
    int totalRecords = result.getTotalRecords();
    int totalPages = (int) Math.ceil(
        (double) totalRecords / pageSize);

    List<RoleResponseDeleteAt> data = result.getData()
        .stream()
        .map(RoleResponseDeleteAt::from)
        .toList();

    span.setAttribute("role.count", data.size());
    span.setAttribute("role.total_records", totalRecords);
    span.setAttribute("role.success", true);

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
        .put("service", "role")
        .put("operation", operation)
        .put("result", result)
        .build());

    requestDurationSeconds.record(durationSeconds, io.opentelemetry.api.common.Attributes.builder()
        .put("service", "role")
        .put("operation", operation)
        .put("result", result)
        .build());
  }
}

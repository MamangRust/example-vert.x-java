package com.sanedge.example_crud.service;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.LongCounter;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.RedisAPI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RedisService {
  private static final Logger logger = LoggerFactory.getLogger(RedisService.class.getName());

  private final RedisAPI redisAPI;
  private final Tracer tracer;
  private final Meter meter;
  private final LongCounter cacheHitCounter;
  private final LongCounter cacheMissCounter;
  private final LongCounter cacheSetCounter;

  public RedisService(RedisAPI redisAPI, OpenTelemetry openTelemetry) {
    this.redisAPI = redisAPI;
    this.tracer = openTelemetry.getTracer(RedisService.class.getName());
    this.meter = openTelemetry.getMeter(RedisService.class.getName());

    this.cacheHitCounter = meter.counterBuilder("redis.cache.hits")
        .setDescription("Number of cache hits")
        .setUnit("1")
        .build();

    this.cacheMissCounter = meter.counterBuilder("redis.cache.misses")
        .setDescription("Number of cache misses")
        .setUnit("1")
        .build();

    this.cacheSetCounter = meter.counterBuilder("redis.cache.sets")
        .setDescription("Number of cache sets")
        .setUnit("1")
        .build();
  }

  public Future<String> get(String key) {
    Span span = tracer.spanBuilder("redis.get")
        .setAttribute("redis.key", key)
        .startSpan();

    return redisAPI.get(key)
        .onSuccess(response -> {
          if (response != null && !response.toString().isEmpty()) {
            cacheHitCounter.add(1);
            logger.debug("Cache hit for key: {}", key);
          } else {
            cacheMissCounter.add(1);
            logger.debug("Cache miss for key: {}", key);
          }
        })
        .onFailure(err -> {
          logger.error("Redis GET error for key {}: {}", key, err.getMessage());
          span.recordException(err);
        })
        .map(response -> response != null ? response.toString() : null)
        .onComplete(ar -> span.end());
  }

  public Future<String> set(String key, String value) {
    return set(key, value, null);
  }

  public Future<String> set(String key, String value, Duration ttl) {
    Span span = tracer.spanBuilder("redis.set")
        .setAttribute("redis.key", key)
        .setAttribute("redis.ttl_seconds", ttl != null ? ttl.getSeconds() : 0)
        .startSpan();

    List<String> args = ttl != null
        ? Arrays.asList(key, value, "EX", String.valueOf(ttl.getSeconds()))
        : Arrays.asList(key, value);

    return redisAPI.set(args)
        .onSuccess(response -> {
          cacheSetCounter.add(1);
          logger.debug("Cache set for key: {} with TTL: {} seconds", key,
              ttl != null ? ttl.getSeconds() : "none");
        })
        .onFailure(err -> {
          logger.error("Redis SET error for key {}: {}", key, err.getMessage());
          span.recordException(err);
        })
        .map(response -> response.toString())
        .onComplete(ar -> span.end());
  }

  public Future<Long> delete(String key) {
    Span span = tracer.spanBuilder("redis.delete")
        .setAttribute("redis.key", key)
        .startSpan();

    return redisAPI.del(List.of(key))
        .onSuccess(response -> {
          logger.debug("Deleted key: {}", key);
        })
        .onFailure(err -> {
          logger.error("Redis DELETE error for key {}: {}", key, err.getMessage());
          span.recordException(err);
        })
        .map(response -> response.toLong())
        .onComplete(ar -> span.end());
  }

  public Future<Boolean> exists(String key) {
    Span span = tracer.spanBuilder("redis.exists")
        .setAttribute("redis.key", key)
        .startSpan();

    return redisAPI.exists(List.of(key))
        .onSuccess(response -> {
          logger.debug("Exists check for key: {} = {}", key, response.toLong() > 0);
        })
        .onFailure(err -> {
          logger.error("Redis EXISTS error for key {}: {}", key, err.getMessage());
          span.recordException(err);
        })
        .map(response -> response.toLong() > 0)
        .onComplete(ar -> span.end());
  }

  public Future<String> setJson(String key, JsonObject value, Duration ttl) {
    return set(key, value.encode(), ttl);
  }

  public Future<JsonObject> getJson(String key) {
    return get(key)
        .compose(jsonStr -> {
          if (jsonStr == null || jsonStr.isEmpty()) {
            return Future.succeededFuture(null);
          }
          try {
            return Future.succeededFuture(new JsonObject(jsonStr));
          } catch (Exception e) {
            logger.error("Failed to parse JSON from Redis for key {}: {}", key, e.getMessage());
            return Future.failedFuture(e);
          }
        });
  }

  public Future<String> ping() {
    Span span = tracer.spanBuilder("redis.ping").startSpan();

    return redisAPI.ping(Collections.emptyList())
        .onSuccess(response -> {
          logger.debug("Redis PING response: {}", response.toString());
        })
        .onFailure(err -> {
          logger.error("Redis PING error: {}", err.getMessage());
          span.recordException(err);
        })
        .map(response -> response.toString())
        .onComplete(ar -> span.end());
  }
}

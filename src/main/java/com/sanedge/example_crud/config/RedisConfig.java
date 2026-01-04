package com.sanedge.example_crud.config;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;

public class RedisConfig {

  public static RedisAPI createClient(Vertx vertx) {
    String redisHost = System.getenv().getOrDefault("REDIS_HOST", "redis");
    int redisPort = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
    String redisPassword = System.getenv().getOrDefault("REDIS_PASSWORD", "dragon_knight");

    RedisOptions options = new RedisOptions()
        .setConnectionString("redis://" + redisHost + ":" + redisPort)
        .setPassword(redisPassword);

    return RedisAPI.api(Redis.createClient(vertx, options));
  }
}

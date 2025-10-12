package com.sanedge.example_crud.config;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

import io.vertx.ext.auth.PubSecKeyOptions;

public class JwtConfig {

  public static JWTAuth createProvider(Vertx vertx) {
    JWTAuthOptions config = new JWTAuthOptions()
        .addPubSecKey(new PubSecKeyOptions()
            .setAlgorithm("HS256")
            .setBuffer("my-secret-key"));

    return JWTAuth.create(vertx, config);
  }
}

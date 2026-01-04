package com.sanedge.example_crud.config;

import org.flywaydb.core.Flyway;

import io.vertx.pgclient.PgConnectOptions;

public class FlywayConfig {

  public static void runMigrations(PgConnectOptions connectOptions) {
    String url = String.format("jdbc:postgresql://%s:%d/%s",
        connectOptions.getHost(),
        connectOptions.getPort(),
        connectOptions.getDatabase());

    Flyway flyway = Flyway.configure()
        .dataSource(url, connectOptions.getUser(), connectOptions.getPassword())
        .load();

    try {
      flyway.migrate();
      System.out.println("✅ Flyway migrations completed successfully");
    } catch (Exception e) {
      System.err.println("❌ Flyway migration failed: " + e.getMessage());
      throw e;
    }
  }
}

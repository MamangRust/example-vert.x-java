package com.sanedge.example_crud.repository;

import java.time.LocalDateTime;

import com.sanedge.example_crud.model.RefreshToken;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RefreshTokenRepository {
  private final Pool client;

  public Future<RefreshToken> create(
      Integer userId,
      String token,
      LocalDateTime expiration) {
    return client
        .preparedQuery("""
            INSERT INTO refresh_tokens (user_id, token, expiration, created_at, updated_at)
            VALUES ($1, $2, $3, current_timestamp, current_timestamp)
            RETURNING refresh_token_id, user_id, token, expiration, created_at, updated_at, deleted_at
            """)
        .execute(Tuple.of(userId, token, expiration))
        .map(this::mapSingleOrNull);
  }

  public Future<RefreshToken> findByToken(String token) {
    return client
        .preparedQuery("""
            SELECT refresh_token_id, user_id, token, expiration, created_at, updated_at, deleted_at
            FROM refresh_tokens
            WHERE token = $1 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(token))
        .map(this::mapSingleOrNull);
  }

  public Future<RefreshToken> findLatestByUserId(Integer userId) {
    return client
        .preparedQuery("""
            SELECT refresh_token_id, user_id, token, expiration, created_at, updated_at, deleted_at
            FROM refresh_tokens
            WHERE user_id = $1 AND deleted_at IS NULL
            ORDER BY created_at DESC
            LIMIT 1
            """)
        .execute(Tuple.of(userId))
        .map(this::mapSingleOrNull);
  }

  public Future<RefreshToken> updateByUserId(
      Integer userId,
      String newToken,
      java.sql.Timestamp newExpiration) {
    return client
        .preparedQuery("""
            UPDATE refresh_tokens
            SET token = $2,
                expiration = $3,
                updated_at = current_timestamp
            WHERE user_id = $1 AND deleted_at IS NULL
            RETURNING refresh_token_id, user_id, token, expiration, created_at, updated_at, deleted_at
            """)
        .execute(Tuple.of(userId, newToken, newExpiration))
        .map(this::mapSingleOrNull);
  }

  public Future<Void> deleteByToken(String token) {
    return client
        .preparedQuery("""
            DELETE FROM refresh_tokens
            WHERE token = $1
            """)
        .execute(Tuple.of(token))
        .mapEmpty();
  }

  public Future<Void> deleteByUserId(Integer userId) {
    return client
        .preparedQuery("""
            DELETE FROM refresh_tokens
            WHERE user_id = $1
            """)
        .execute(Tuple.of(userId))
        .mapEmpty();
  }

  private RefreshToken mapSingleOrNull(RowSet<io.vertx.sqlclient.Row> rows) {
    return rows.iterator().hasNext()
        ? RefreshToken.fromRow(rows.iterator().next())
        : null;
  }
}

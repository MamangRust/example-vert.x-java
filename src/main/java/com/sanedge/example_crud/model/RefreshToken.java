package com.sanedge.example_crud.model;

import java.time.LocalDateTime;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
  private Integer refreshTokenId;
  private Integer userId;
  private String token;
  private LocalDateTime expiration;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime deletedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("refreshTokenId", refreshTokenId)
        .put("userId", userId)
        .put("token", token);

    if (expiration != null) {
      json.put("expiration", expiration.toString());
    }
    if (createdAt != null) {
      json.put("createdAt", createdAt.toString());
    }
    if (updatedAt != null) {
      json.put("updatedAt", updatedAt.toString());
    }
    if (deletedAt != null) {
      json.put("deletedAt", deletedAt.toString());
    }

    return json;
  }

  public static RefreshToken fromRow(Row row) {
    if (row == null) {
      return null;
    }

    Integer refreshTokenId = row.getInteger("refresh_token_id");
    if (refreshTokenId == null) {
      refreshTokenId = row.getInteger("refreshTokenId");
    }

    Integer userId = row.getInteger("user_id");
    if (userId == null) {
      userId = row.getInteger("userId");
    }

    String token = row.getString("token");
    LocalDateTime expiration = row.getLocalDateTime("expiration");
    LocalDateTime createdAt = row.getLocalDateTime("created_at");
    LocalDateTime updatedAt = row.getLocalDateTime("updated_at");
    LocalDateTime deletedAt = row.getLocalDateTime("deleted_at");

    return RefreshToken.builder()
        .refreshTokenId(refreshTokenId)
        .userId(userId)
        .token(token)
        .expiration(expiration)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}

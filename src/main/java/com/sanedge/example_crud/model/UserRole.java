package com.sanedge.example_crud.model;

import java.sql.Timestamp;
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
public class UserRole {
  private Integer userRoleId;
  private Integer userId;
  private Integer roleId;
  private Timestamp createdAt;
  private Timestamp updatedAt;
  private Timestamp deletedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("userRoleId", userRoleId)
        .put("userId", userId)
        .put("roleId", roleId);

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

  public static UserRole fromRow(Row row) {
    if (row == null)
      return null;

    Integer userRoleId = row.getInteger("user_role_id");
    if (userRoleId == null)
      userRoleId = row.getInteger("userRoleId");

    Integer userId = row.getInteger("user_id");
    if (userId == null)
      userId = row.getInteger("userId");

    Integer roleId = row.getInteger("role_id");
    if (roleId == null)
      roleId = row.getInteger("roleId");

    Timestamp createdAt = null;
    LocalDateTime createdAtLocal = row.get(LocalDateTime.class, "created_at");
    if (createdAtLocal != null) {
      createdAt = Timestamp.valueOf(createdAtLocal);
    }

    Timestamp updatedAt = null;
    LocalDateTime updatedAtLocal = row.get(LocalDateTime.class, "updated_at");
    if (updatedAtLocal != null) {
      updatedAt = Timestamp.valueOf(updatedAtLocal);
    }

    Timestamp deletedAt = null;
    LocalDateTime deletedAtLocal = row.get(LocalDateTime.class, "deleted_at");
    if (deletedAtLocal != null) {
      deletedAt = Timestamp.valueOf(deletedAtLocal);
    }

    return UserRole.builder()
        .userRoleId(userRoleId)
        .userId(userId)
        .roleId(roleId)
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

package com.sanedge.example_crud.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

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
public class Role {
  private Integer roleId;
  private String roleName;
  private Timestamp createdAt;
  private Timestamp updatedAt;
  private Timestamp deletedAt;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("roleId", roleId)
        .put("roleName", roleName);

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

  public static Role fromJson(JsonObject json) {
    if (json == null) {
      return null;
    }

    Role role = new Role();
    role.setRoleId(json.getInteger("roleId"));
    role.setRoleName(json.getString("roleName"));

    role.setCreatedAt(parseTimestamp(json, "createdAt"));
    role.setUpdatedAt(parseTimestamp(json, "updatedAt"));
    role.setDeletedAt(parseTimestamp(json, "deletedAt"));

    return role;
  }

  public static Role fromRow(Row row) {
    if (row == null)
      return null;

    Integer roleId = row.getInteger("role_id");
    if (roleId == null)
      roleId = row.getInteger("roleId");

    String roleName = row.getString("role_name");
    if (roleName == null)
      roleName = row.getString("roleName");

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

    return Role.builder()
        .roleId(roleId)
        .roleName(roleName)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();
  }

  private static Timestamp parseTimestamp(JsonObject json, String field) {
    Object value = json.getValue(field);

    if (value == null) {
      return null;
    }

    if (value instanceof Timestamp ts) {
      return ts;
    }

    if (value instanceof String str && !str.isBlank()) {
      try {
        return Timestamp.from(Instant.parse(str));
      } catch (DateTimeParseException e) {
        return null;
      }
    }

    if (value instanceof Number num) {
      return new Timestamp(num.longValue());
    }

    return null;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}

package com.sanedge.example_crud.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
  private Integer userId;
  private String firstname;
  private String lastname;
  private String email;
  private String password;
  private Timestamp createdAt;
  private Timestamp updatedAt;
  private Timestamp deletedAt;

  private List<Role> roles;

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
        .put("userId", userId)
        .put("firstname", firstname)
        .put("lastname", lastname)
        .put("email", email);

    if (createdAt != null) {
      json.put("createdAt", createdAt.toString());
    }
    if (updatedAt != null) {
      json.put("updatedAt", updatedAt.toString());
    }
    if (deletedAt != null) {
      json.put("deletedAt", deletedAt.toString());
    }
    if (roles != null && !roles.isEmpty()) {
      json.put("roles", roles.stream().map(Role::toJson).toList());
    }

    return json;
  }

  public static User fromJson(JsonObject json) {
    if (json == null) {
      return null;
    }

    User user = new User();

    user.setUserId(json.getInteger("userId"));
    user.setFirstname(json.getString("firstname"));
    user.setLastname(json.getString("lastname"));
    user.setEmail(json.getString("email"));

    user.setCreatedAt(parseTimestamp(json, "createdAt"));
    user.setUpdatedAt(parseTimestamp(json, "updatedAt"));
    user.setDeletedAt(parseTimestamp(json, "deletedAt"));

    JsonArray rolesArray = json.getJsonArray("roles");
    if (rolesArray != null) {
      List<Role> roles = rolesArray.stream()
          .filter(JsonObject.class::isInstance)
          .map(JsonObject.class::cast)
          .map(Role::fromJson)
          .toList();
      user.setRoles(roles);
    }

    return user;
  }

  public static User fromRow(Row row) {
    if (row == null)
      return null;

    Integer userId = row.getInteger("user_id");
    if (userId == null)
      userId = row.getInteger("userId");

    String firstname = row.getString("firstname");
    String lastname = row.getString("lastname");
    String email = row.getString("email");
    String password = row.getString("password");

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

    return User.builder()
        .userId(userId)
        .firstname(firstname)
        .lastname(lastname)
        .email(email)
        .password(password)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();
  }

  public static List<User> fromRowsToUsersWithRoles(RowSet<Row> rows) {
    if (rows == null) {
      return List.of();
    }

    Map<Integer, User> userMap = new HashMap<>();

    for (Row row : rows) {
      Integer userId = row.getInteger("user_id");

      User user = userMap.get(userId);
      if (user == null) {
        user = User.fromRow(row);
        user.setRoles(new ArrayList<>());
        userMap.put(userId, user);
      }

      Integer roleId = row.getInteger("role_id");
      if (roleId != null) {
        Role role = Role.builder()
            .roleId(roleId)
            .roleName(row.getString("role_name"))
            .createdAt(getTimestampFromRow(row, "role_created_at"))
            .updatedAt(getTimestampFromRow(row, "role_updated_at"))
            .deletedAt(getTimestampFromRow(row, "role_deleted_at"))
            .build();

        user.getRoles().add(role);
      }
    }

    return new ArrayList<>(userMap.values());
  }

  public static User fromRowsWithRoles(RowSet<Row> rows) {
    if (rows == null || !rows.iterator().hasNext()) {
      return null;
    }

    User user = fromRow(rows.iterator().next());

    user.setRoles(new ArrayList<>());

    for (Row row : rows) {
      Integer roleId = row.getInteger("role_id");
      if (roleId != null) {
        Role role = Role.builder()
            .roleId(roleId)
            .roleName(row.getString("role_name"))
            .createdAt(getTimestampFromRow(row, "role_created_at"))
            .updatedAt(getTimestampFromRow(row, "role_updated_at"))
            .deletedAt(getTimestampFromRow(row, "role_deleted_at"))
            .build();

        user.getRoles().add(role);
      }
    }

    return user;
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

  private static Timestamp getTimestampFromRow(Row row, String column) {
    LocalDateTime localDateTime = row.get(LocalDateTime.class, column);
    return localDateTime != null ? Timestamp.valueOf(localDateTime) : null;
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}

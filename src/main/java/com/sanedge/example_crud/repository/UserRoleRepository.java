package com.sanedge.example_crud.repository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.sanedge.example_crud.model.UserRole;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserRoleRepository {
  private final Pool client;

  public Future<UserRole> assignRoleToUser(UserRole userRole) {
    return client
        .preparedQuery("""
              INSERT INTO user_roles (user_id, role_id)
              VALUES ($1, $2)
              RETURNING user_role_id, user_id, role_id, created_at, updated_at, deleted_at
            """)
        .execute(Tuple.of(userRole.getUserId(), userRole.getRoleId()))
        .map(rows -> UserRole.fromRow(rows.iterator().next()));
  }

  public Future<List<UserRole>> getUserRoles(Integer userId) {
    return client
        .preparedQuery("""
              SELECT ur.user_role_id, ur.user_id, ur.role_id, ur.created_at, ur.updated_at, ur.deleted_at
              FROM user_roles ur
              WHERE ur.user_id = $1 AND ur.deleted_at IS NULL
              ORDER BY ur.user_role_id
            """)
        .execute(Tuple.of(userId))
        .map(rows -> StreamSupport.stream(rows.spliterator(), false)
            .map(UserRole::fromRow)
            .collect(Collectors.toList()));
  }

  public Future<List<UserRole>> getRoleUsers(Integer roleId) {
    return client
        .preparedQuery("""
              SELECT ur.user_role_id, ur.user_id, ur.role_id, ur.created_at, ur.updated_at, ur.deleted_at
              FROM user_roles ur
              WHERE ur.role_id = $1 AND ur.deleted_at IS NULL
              ORDER BY ur.user_role_id
            """)
        .execute(Tuple.of(roleId))
        .map(rows -> StreamSupport.stream(rows.spliterator(), false)
            .map(UserRole::fromRow)
            .collect(Collectors.toList()));
  }

  public Future<UserRole> getUserRole(UserRole userRole) {
    return client
        .preparedQuery("""
              SELECT user_role_id, user_id, role_id, created_at, updated_at, deleted_at
              FROM user_roles
              WHERE user_id = $1 AND role_id = $2 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(userRole.getUserId(), userRole.getRoleId()))
        .map(rows -> rows.iterator().hasNext() ? UserRole.fromRow(rows.iterator().next()) : null);
  }

  public Future<Void> removeUserRole(UserRole userRole) {
    return client
        .preparedQuery("""
              UPDATE user_roles
              SET deleted_at = CURRENT_TIMESTAMP
              WHERE user_id = $1 AND role_id = $2 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(userRole.getUserId(), userRole.getRoleId()))
        .mapEmpty();
  }

  public Future<Void> updateUserRole(UserRole userRole) {
    return client
        .preparedQuery("""
              UPDATE user_roles
              SET user_id = $1, role_id = $2, updated_at = CURRENT_TIMESTAMP
              WHERE user_role_id = $3 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(userRole.getUserId(), userRole.getRoleId(), userRole.getUserRoleId()))
        .mapEmpty();
  }

  public Future<Void> hardDeleteUserRole(UserRole userRole) {
    return client
        .preparedQuery("""
              DELETE FROM user_roles
              WHERE user_id = $1 AND role_id = $2
            """)
        .execute(Tuple.of(userRole.getUserId(), userRole.getRoleId()))
        .mapEmpty();
  }

  public Future<Boolean> hasRole(UserRole req) {
    return getUserRole(req)
        .map(userRole -> userRole != null);
  }
}

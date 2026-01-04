package com.sanedge.example_crud.repository;

import java.util.ArrayList;
import java.util.List;

import com.sanedge.example_crud.domain.requests.role.CreateRoleRequest;
import com.sanedge.example_crud.domain.requests.role.FindAllRoles;
import com.sanedge.example_crud.domain.requests.role.UpdateRoleRequest;
import com.sanedge.example_crud.domain.response.api.PagedResult;
import com.sanedge.example_crud.model.Role;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RoleRepository {
  private final Pool client;

  public Future<PagedResult<Role>> getRoles(
      FindAllRoles req) {
    int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();

    return client
        .preparedQuery("""
            SELECT
                role_id,
                role_name,
                created_at,
                updated_at,
                deleted_at,
                COUNT(*) OVER() AS total_count
            FROM roles
            WHERE ($1::TEXT IS NULL OR role_name ILIKE '%' || $1 || '%')
            ORDER BY created_at ASC
            LIMIT $2 OFFSET $3
            """)
        .execute(Tuple.of(
            normalizeSearch(req.getSearch()),
            req.getPageSize(),
            offset))
        .map(this::mapPagedRoles);
  }

  public Future<PagedResult<Role>> getActiveRoles(
      FindAllRoles req) {
    int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();

    return client
        .preparedQuery("""
            SELECT
                role_id,
                role_name,
                created_at,
                updated_at,
                deleted_at,
                COUNT(*) OVER() AS total_count
            FROM roles
            WHERE deleted_at IS NULL
              AND ($1::TEXT IS NULL OR role_name ILIKE '%' || $1 || '%')
            ORDER BY created_at ASC
            LIMIT $2 OFFSET $3
            """)
        .execute(Tuple.of(
            normalizeSearch(req.getSearch()),
            req.getPageSize(),
            offset))
        .map(this::mapPagedRoles);
  }

  public Future<PagedResult<Role>> getTrashedRoles(
      FindAllRoles req) {
    int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();

    return client
        .preparedQuery("""
            SELECT
                role_id,
                role_name,
                created_at,
                updated_at,
                deleted_at,
                COUNT(*) OVER() AS total_count
            FROM roles
            WHERE deleted_at IS NOT NULL
              AND ($1::TEXT IS NULL OR role_name ILIKE '%' || $1 || '%')
            ORDER BY deleted_at DESC
            LIMIT $2 OFFSET $3
            """)
        .execute(Tuple.of(
            normalizeSearch(req.getSearch()),
            req.getPageSize(),
            offset))
        .map(this::mapPagedRoles);
  }

  public Future<Role> getRoleById(Integer roleId) {
    return client
        .preparedQuery("""
              SELECT role_id, role_name, created_at, updated_at, deleted_at
              FROM roles
              WHERE role_id = $1 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(roleId))
        .map(this::mapSingleOrNull);
  }

  public Future<Role> getRoleByName(String roleName) {
    return client
        .preparedQuery("""
              SELECT role_id, role_name, created_at, updated_at, deleted_at
              FROM roles
              WHERE role_name = $1 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(roleName))
        .map(this::mapSingleOrNull);
  }

  public Future<Role> createRole(CreateRoleRequest req) {
    return client
        .preparedQuery("""
              INSERT INTO roles (role_name)
              VALUES ($1)
              RETURNING role_id, role_name, created_at, updated_at, deleted_at
            """)
        .execute(Tuple.of(req.getName()))
        .map(this::mapSingleOrNull);
  }

  public Future<Role> updateRole(UpdateRoleRequest req) {
    return client
        .preparedQuery("""
              UPDATE roles
              SET role_name = $1, updated_at = CURRENT_TIMESTAMP
              WHERE role_id = $2 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(req.getName(), req.getRoleId()))
        .map(this::mapSingleOrNull);
  }

  public Future<Role> trashed(Integer roleId) {
    return client
        .preparedQuery("""
              UPDATE roles
              SET deleted_at = CURRENT_TIMESTAMP
              WHERE role_id = $1 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(roleId))
        .map(this::mapSingleOrNull);
  }

  public Future<Role> restore(Integer roleId) {
    return client
        .preparedQuery("""
              UPDATE roles
              SET deleted_at = null
              WHERE role_id = $1 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(roleId))
        .map(this::mapSingleOrNull);
  }

  public Future<Void> deletePermanent(Integer roleId) {
    return client
        .preparedQuery("DELETE FROM roles WHERE role_id = $1")
        .execute(Tuple.of(roleId))
        .mapEmpty();
  }

  private String normalizeSearch(String search) {
    if (search == null || search.isBlank()) {
      return null;
    }
    return search;
  }

  private Role mapSingleOrNull(RowSet<io.vertx.sqlclient.Row> rows) {
    return rows.iterator().hasNext() ? Role.fromRow(rows.iterator().next()) : null;
  }

  private PagedResult<Role> mapPagedRoles(RowSet<Row> rows) {
    List<Role> roles = new ArrayList<>();
    int total = 0;

    for (Row row : rows) {
      roles.add(Role.fromRow(row));
      if (total == 0) {
        total = row.getInteger("total_count");
      }
    }

    return new PagedResult<>(roles, total);
  }
}

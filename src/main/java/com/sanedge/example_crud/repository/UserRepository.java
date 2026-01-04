package com.sanedge.example_crud.repository;

import java.util.ArrayList;
import java.util.List;

import com.sanedge.example_crud.domain.requests.user.CreateUserRequest;
import com.sanedge.example_crud.domain.requests.user.FindAllUsers;
import com.sanedge.example_crud.domain.requests.user.UpdateUserRequest;
import com.sanedge.example_crud.domain.response.api.PagedResult;
import com.sanedge.example_crud.model.User;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserRepository {
  private final Pool client;

  public Future<PagedResult<User>> getUsers(FindAllUsers req) {
    int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();

    return client
        .preparedQuery("""
            SELECT
                *,
                COUNT(*) OVER() AS total_count
            FROM users
            WHERE deleted_at IS NULL
              AND (
                $1::TEXT IS NULL
                OR firstname ILIKE '%' || $1 || '%'
                OR lastname ILIKE '%' || $1 || '%'
                OR email ILIKE '%' || $1 || '%'
              )
            ORDER BY created_at DESC
            LIMIT $2 OFFSET $3
            """)
        .execute(Tuple.of(
            normalizeSearch(req.getSearch()),
            req.getPageSize(),
            offset))
        .map(this::mapPagedUsers);
  }

  public Future<PagedResult<User>> getActiveUsers(
      FindAllUsers req) {
    int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();

    return client
        .preparedQuery("""
            SELECT
                *,
                COUNT(*) OVER() AS total_count
            FROM users
            WHERE deleted_at IS NULL
              AND ($1::TEXT IS NULL
                   OR firstname ILIKE '%' || $1 || '%'
                   OR lastname ILIKE '%' || $1 || '%'
                   OR email ILIKE '%' || $1 || '%')
            ORDER BY created_at DESC
            LIMIT $2 OFFSET $3
            """)
        .execute(Tuple.of(
            normalizeSearch(req.getSearch()),
            req.getPageSize(),
            offset))
        .map(this::mapPagedUsers);
  }

  public Future<PagedResult<User>> getTrashedUsers(
      FindAllUsers req) {
    int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();

    return client
        .preparedQuery("""
            SELECT
                *,
                COUNT(*) OVER() AS total_count
            FROM users
            WHERE deleted_at IS NOT NULL
              AND ($1::TEXT IS NULL
                   OR firstname ILIKE '%' || $1 || '%'
                   OR lastname ILIKE '%' || $1 || '%'
                   OR email ILIKE '%' || $1 || '%')
            ORDER BY created_at DESC
            LIMIT $2 OFFSET $3
            """)
        .execute(Tuple.of(
            normalizeSearch(req.getSearch()),
            req.getPageSize(),
            offset))
        .map(this::mapPagedUsers);
  }

  public Future<List<User>> getAllUsersWithRoles() {
    return client
        .query(
            """
                SELECT
                  u.user_id, u.firstname, u.lastname, u.email, u.password, u.created_at, u.updated_at, u.deleted_at,
                  r.role_id, r.role_name, r.created_at as role_created_at, r.updated_at as role_updated_at, r.deleted_at as role_deleted_at
                FROM users u
                LEFT JOIN user_roles ur ON u.user_id = ur.user_id AND ur.deleted_at IS NULL
                LEFT JOIN roles r ON ur.role_id = r.role_id AND r.deleted_at IS NULL
                WHERE u.deleted_at IS NULL
                ORDER BY u.user_id
                """)
        .execute()
        .map(User::fromRowsToUsersWithRoles);
  }

  public Future<User> getUserById(Integer userId) {
    return client
        .preparedQuery("""
              SELECT
                u.user_id,
                u.firstname,
                u.lastname,
                u.email,
                u.password,
                u.created_at,
                u.updated_at,
                u.deleted_at
              FROM users u
              WHERE u.user_id = $1 AND u.deleted_at IS NULL
            """)
        .execute(Tuple.of(userId))
        .map(rows -> rows.iterator().hasNext() ? User.fromRow(rows.iterator().next()) : null);
  }

  public Future<User> getUserByIdWithRoles(Integer userId) {
    return client
        .preparedQuery(
            """
                SELECT
                  u.user_id, u.firstname, u.lastname, u.email, u.password, u.created_at, u.updated_at, u.deleted_at,
                  r.role_id, r.role_name, r.created_at as role_created_at, r.updated_at as role_updated_at, r.deleted_at as role_deleted_at
                FROM users u
                LEFT JOIN user_roles ur ON u.user_id = ur.user_id AND ur.deleted_at IS NULL
                LEFT JOIN roles r ON ur.role_id = r.role_id AND r.deleted_at IS NULL
                WHERE u.user_id = $1 AND u.deleted_at IS NULL
                ORDER BY u.user_id
                """)
        .execute(Tuple.of(userId))
        .map(User::fromRowsWithRoles);
  }

  public Future<User> getUserByEmail(String email) {
    return client
        .preparedQuery("""
              SELECT
                u.user_id,
                u.firstname,
                u.lastname,
                u.email,
                u.password,
                u.created_at,
                u.updated_at,
                u.deleted_at
              FROM users u
              WHERE u.email = $1 AND u.deleted_at IS NULL
            """)
        .execute(Tuple.of(email))
        .map(rows -> rows.iterator().hasNext() ? User.fromRow(rows.iterator().next()) : null);
  }

  public Future<User> getUserByEmailWithRoles(String email) {
    return client
        .preparedQuery("""
            SELECT
              u.user_id,
              u.firstname,
              u.lastname,
              u.email,
              u.password,
              u.created_at,
              u.updated_at,
              u.deleted_at,
              r.role_id,
              r.role_name,
              r.created_at as role_created_at,
              r.updated_at as role_updated_at,
              r.deleted_at as role_deleted_at
            FROM users u
            LEFT JOIN user_roles ur ON u.user_id = ur.user_id AND ur.deleted_at IS NULL
            LEFT JOIN roles r ON ur.role_id = r.role_id AND r.deleted_at IS NULL
            WHERE u.email = $1 AND u.deleted_at IS NULL
            ORDER BY u.user_id
            """)
        .execute(Tuple.of(email))
        .map(User::fromRowsWithRoles);
  }

  public Future<User> createUser(CreateUserRequest req) {
    return client
        .preparedQuery("""
              INSERT INTO users (firstname, lastname, email, password)
              VALUES ($1, $2, $3, $4)
              RETURNING user_id, firstname, lastname, email, password, created_at, updated_at, deleted_at
            """)
        .execute(Tuple.of(req.getFirstName(), req.getLastName(), req.getEmail(), req.getPassword()))
        .map(rows -> User.fromRow(rows.iterator().next()));
  }

  public Future<User> updateUser(UpdateUserRequest req) {
    return client
        .preparedQuery("""
              UPDATE users
              SET firstname = $1, lastname = $2, email = $3, updated_at = CURRENT_TIMESTAMP
              WHERE user_id = $4 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(req.getFirstName(), req.getLastName(), req.getEmail(), req.getUserId()))
        .map(this::mapSingleOrNull);
  }

  public Future<User> updatePassword(Integer userId, String password) {
    return client
        .preparedQuery("""
              UPDATE users
              SET password = $1, updated_at = CURRENT_TIMESTAMP
              WHERE user_id = $2 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(password, userId))
        .map(this::mapSingleOrNull);
  }

  public Future<User> restore(Integer userId) {
    return client
        .preparedQuery("""
              UPDATE users
              SET deleted_at = null
              WHERE user_id = $1 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(userId))
        .map(this::mapSingleOrNull);
  }

  public Future<User> trashed(Integer userId) {
    return client
        .preparedQuery("""
              UPDATE users
              SET deleted_at = CURRENT_TIMESTAMP
              WHERE user_id = $1 AND deleted_at IS NULL
            """)
        .execute(Tuple.of(userId))
        .map(this::mapSingleOrNull);
  }

  public Future<Void> deletePermanent(Integer userId) {
    return client
        .preparedQuery("DELETE FROM users WHERE user_id = $1")
        .execute(Tuple.of(userId))
        .mapEmpty();
  }

  private User mapSingleOrNull(RowSet<io.vertx.sqlclient.Row> rows) {
    return rows.iterator().hasNext() ? User.fromRow(rows.iterator().next()) : null;
  }

  private String normalizeSearch(String search) {
    if (search == null || search.isBlank()) {
      return null;
    }
    return search;
  }

  private PagedResult<User> mapPagedUsers(RowSet<Row> rows) {
    List<User> users = new ArrayList<>();
    int total = 0;

    for (Row row : rows) {
      users.add(User.fromRow(row));
      if (total == 0) {
        total = row.getInteger("total_count");
      }
    }

    return new PagedResult<>(users, total);
  }
}

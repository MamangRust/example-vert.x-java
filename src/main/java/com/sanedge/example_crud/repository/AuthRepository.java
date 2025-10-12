package com.sanedge.example_crud.repository;

import com.sanedge.example_crud.model.User;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

public class AuthRepository {
  private final Pool client;

  public AuthRepository(Pool client) {
    this.client = client;
  }

  public Future<User> findByEmail(String email) {
    return client
        .preparedQuery("""
              SELECT
                u.id AS user_id,
                u.name AS user_name,
                u.email AS user_email,
                u.password AS user_password,
                COALESCE(r.name, 'USER') AS role_name
              FROM users u
              LEFT JOIN roles r ON r.id = u.role_id
              WHERE u.email = $1
            """)
        .execute(Tuple.of(email))
        .map(rows -> {
          if (!rows.iterator().hasNext())
            return null;
          var row = rows.iterator().next();
          return new User(
              row.getInteger("user_id"),
              row.getString("user_name"),
              row.getString("user_email"),
              row.getString("user_password"),
              row.getString("role_name"));
        });
  }

  public Future<User> createUser(User user) {
    return client
        .preparedQuery("""
              INSERT INTO users (name, email, password, role_id)
              VALUES ($1, $2, $3,
                CASE
                  WHEN $4 IS NOT NULL AND (SELECT id FROM roles WHERE name = $4) IS NOT NULL
                  THEN (SELECT id FROM roles WHERE name = $4)
                  ELSE (SELECT id FROM roles WHERE name = 'USER' LIMIT 1)
                END
              )
              RETURNING id, name, email, password,
                (SELECT name FROM roles WHERE id = role_id) as role_name
            """)
        .execute(Tuple.of(user.getName(), user.getEmail(), user.getPassword(), user.getRole()))
        .map(rows -> User.fromRow(rows.iterator().next()));
  }
}

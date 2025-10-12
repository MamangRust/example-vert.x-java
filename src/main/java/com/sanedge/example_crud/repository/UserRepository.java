package com.sanedge.example_crud.repository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.sanedge.example_crud.model.User;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

public class UserRepository {
  private final Pool client;

  public UserRepository(Pool client) {
    this.client = client;
  }

  public Future<User> createUser(User user) {
    return client
        .preparedQuery("""
              INSERT INTO users (name, email, password, role_id)
              VALUES ($1, $2, $3, (SELECT id FROM roles WHERE name = $4))
              RETURNING id, name, email, password, (SELECT name FROM roles WHERE id = role_id) as role_name
            """)
        .execute(Tuple.of(user.getName(), user.getEmail(), user.getPassword(), user.getRole()))
        .map(rows -> User.fromRow(rows.iterator().next()));
  }

  public Future<List<User>> getAllUsers() {
    return client
        .query("""
              SELECT
                u.id AS user_id,
                u.name AS user_name,
                u.email AS user_email,
                u.password AS user_password,
                r.name AS role_name
              FROM users u
              LEFT JOIN roles r ON r.id = u.role_id
              ORDER BY u.id
            """)
        .execute()
        .map(rows -> StreamSupport.stream(rows.spliterator(), false)
            .map(User::fromRow)
            .collect(Collectors.toList()));
  }

  public Future<User> getUserById(int id) {
    return client
        .preparedQuery("""
              SELECT
                u.id AS user_id,
                u.name AS user_name,
                u.email AS user_email,
                u.password AS user_password,
                r.name AS role_name
              FROM users u
              LEFT JOIN roles r ON r.id = u.role_id
              WHERE u.id = $1
            """)
        .execute(Tuple.of(id))
        .map(rows -> rows.iterator().hasNext() ? User.fromRow(rows.iterator().next()) : null);
  }

  public Future<Void> updateUser(int id, User user) {
    return client
        .preparedQuery("UPDATE users SET name=$1, email=$2 WHERE id=$3")
        .execute(Tuple.of(user.getName(), user.getEmail(), id))
        .mapEmpty();
  }

  public Future<Void> deleteUser(int id) {
    return client
        .preparedQuery("DELETE FROM users WHERE id=$1")
        .execute(Tuple.of(id))
        .mapEmpty();
  }
}

package com.sanedge.example_crud.model;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

public class User {
  private Integer id;
  private String name;
  private String email;
  private String password;
  private String role;

  public User() {
  }

  // full constructor (disarankan dipakai oleh fromRow dan repository)
  public User(Integer id, String name, String email, String password, String role) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.password = password;
    this.role = role;
  }

  // convenience constructor untuk create tanpa id
  public User(String name, String email, String password, String role) {
    this(null, name, email, password, role);
  }

  // convenience constructor untuk update/toMap tanpa password
  public User(Integer id, String name, String email, String role) {
    this(id, name, email, null, role);
  }

  // getters
  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public String getRole() {
    return role;
  }

  public JsonObject toJson() {
    return new JsonObject()
        .put("id", id)
        .put("name", name)
        .put("email", email)
        .put("role", role);
  }

  public static User fromRow(Row row) {
    if (row == null)
      return null;

    Integer id = row.getInteger("user_id");
    if (id == null)
      id = row.getInteger("id");

    String name = row.getString("user_name");
    if (name == null)
      name = row.getString("name");

    String email = row.getString("user_email");
    if (email == null)
      email = row.getString("email");

    String password = row.getString("user_password");
    if (password == null)
      password = row.getString("password");

    String role = row.getString("role_name");
    if (role == null)
      role = row.getString("role");
    if (role == null)
      role = "USER";

    return new User(id, name, email, password, role);
  }

  @Override
  public String toString() {
    return toJson().encode();
  }
}

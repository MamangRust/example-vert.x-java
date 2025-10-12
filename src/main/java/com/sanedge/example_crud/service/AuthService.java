package com.sanedge.example_crud.service;

import com.sanedge.example_crud.model.User;
import com.sanedge.example_crud.repository.AuthRepository;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.vertx.core.Future;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;

public class AuthService {
  private final AuthRepository repo;
  private final JWTAuth jwtProvider;

  public AuthService(AuthRepository repo, JWTAuth jwtProvider) {
    this.repo = repo;
    this.jwtProvider = jwtProvider;
  }

  public Future<String> login(String email, String password) {
    return repo.findByEmail(email)
        .compose(user -> {
          if (user == null)
            return Future.failedFuture("User not found");
          BCrypt.Result res = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());
          if (!res.verified)
            return Future.failedFuture("Invalid password");

          String token = jwtProvider.generateToken(
              new io.vertx.core.json.JsonObject()
                  .put("id", user.getId())
                  .put("email", user.getEmail())
                  .put("role", user.getRole()),
              new JWTOptions().setExpiresInMinutes(60));
          return Future.succeededFuture(token);
        });
  }

  public Future<User> register(String name, String email, String password, String role) {
    String hashed = BCrypt.withDefaults().hashToString(12, password.toCharArray());
    User user = new User(null, name, email, hashed, role);
    return repo.createUser(user);
  }
}

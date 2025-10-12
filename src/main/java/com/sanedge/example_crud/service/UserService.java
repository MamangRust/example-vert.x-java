package com.sanedge.example_crud.service;

import java.util.List;

import com.sanedge.example_crud.model.User;
import com.sanedge.example_crud.repository.UserRepository;

import io.vertx.core.Future;

public class UserService {
  private final UserRepository repository;

  public UserService(UserRepository repository) {
    this.repository = repository;
  }

  public Future<User> createUser(User user) {
    return repository.createUser(user);
  }

  public Future<List<User>> getAllUsers() {
    return repository.getAllUsers();
  }

  public Future<User> getUserById(int id) {
    return repository.getUserById(id);
  }

  public Future<Void> updateUser(int id, User user) {
    return repository.updateUser(id, user);
  }

  public Future<Void> deleteUser(int id) {
    return repository.deleteUser(id);
  }
}

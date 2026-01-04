package com.sanedge.example_crud.exception;

public class NotFoundException extends ApiException {
  public NotFoundException(String message) {
    super(message, 404);
  }
}

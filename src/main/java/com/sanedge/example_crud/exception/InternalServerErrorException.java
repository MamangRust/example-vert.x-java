package com.sanedge.example_crud.exception;

public class InternalServerErrorException extends ApiException {
  public InternalServerErrorException(String message) {
    super(message, 500);
  }

  public InternalServerErrorException(String message, Throwable cause) {
    super(message, 500);
    this.initCause(cause);
  }
}

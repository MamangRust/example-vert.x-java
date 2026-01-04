package com.sanedge.example_crud.domain.requests.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterRequest {
  private String firstName;
  private String lastName;
  private String email;
  private String password;
  private String confirmPassword;
}

package com.sanedge.example_crud.domain.requests.auth;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LoginRequest {
  private String email;
  private String password;
}

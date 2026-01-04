package com.sanedge.example_crud.domain.requests.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserRequest {
  private Integer userId;
  private String firstName;
  private String lastName;
  private String email;
  private String password;
  private String confirmPassword;
}

package com.sanedge.example_crud.domain.requests.role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateRoleRequest {
  private String name;
}

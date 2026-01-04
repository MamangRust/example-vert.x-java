package com.sanedge.example_crud.domain.requests.role;

import lombok.Data;

@Data
public class FindAllRoles {
  private Integer page = 1;
  private Integer pageSize = 10;
  private String search = "";
}

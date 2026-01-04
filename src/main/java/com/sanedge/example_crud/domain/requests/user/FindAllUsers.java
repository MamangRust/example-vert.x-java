package com.sanedge.example_crud.domain.requests.user;

import lombok.Data;

@Data
public class FindAllUsers {
  private Integer page = 1;
  private Integer pageSize = 10;
  private String search = "";
}

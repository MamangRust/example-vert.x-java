package com.sanedge.example_crud.domain.response.api;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PagedResult<T> {
  private List<T> data;
  private int totalRecords;
}

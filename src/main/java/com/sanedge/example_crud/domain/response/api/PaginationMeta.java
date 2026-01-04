package com.sanedge.example_crud.domain.response.api;

public record PaginationMeta(
    int currentPage,
    int pageSize,
    int totalPages,
    int totalRecords) {
}

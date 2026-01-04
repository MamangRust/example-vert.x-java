package com.sanedge.example_crud.domain.response.api;

public record ApiResponsePagination<T>(
    String status,
    String message,
    T data,
    PaginationMeta pagination) {
}

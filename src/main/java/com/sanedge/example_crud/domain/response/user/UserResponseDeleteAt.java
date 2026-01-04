package com.sanedge.example_crud.domain.response.user;

import com.sanedge.example_crud.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDeleteAt {
  private Integer id;
  private String firstname;
  private String lastname;
  private String email;
  private String createdAt;
  private String updatedAt;
  private String deletedAt;

  public static UserResponseDeleteAt from(User user) {
    return UserResponseDeleteAt.builder()
        .id(user.getUserId().intValue())
        .firstname(user.getFirstname())
        .lastname(user.getLastname())
        .email(user.getEmail())
        .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
        .updatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null)
        .deletedAt(user.getDeletedAt() != null ? user.getDeletedAt().toString() : null)
        .build();
  }
}

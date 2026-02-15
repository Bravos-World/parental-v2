package com.bravos.parentalv2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

  @NotBlank(message = "Old password is required")
  private String oldPassword;

  @NotBlank(message = "New password is required")
  @Size(min = 4, message = "New password must be at least 4 characters")
  private String newPassword;

}

package org.app.backend.modules.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordDTO {
  @NotBlank
  @Email
  @Size(max = 255)
  private String email;
}

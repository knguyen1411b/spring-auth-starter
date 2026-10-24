package org.app.backend.modules.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.app.backend.common.validation.constraints.ValidPassword;

@Getter
@Setter
public class ResetPasswordDTO {
  @NotBlank
  @Size(max = 128)
  private String token;

  @ValidPassword private String newPassword;
}

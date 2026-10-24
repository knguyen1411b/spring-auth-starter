package org.app.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordDTO {
  @NotBlank(message = "auth.current_password_not_blank")
  String currentPassword;

  @NotBlank(message = "auth.new_password_not_blank")
  @org.app.backend.common.validation.constraints.ValidPassword
  String newPassword;

  boolean logoutAllSessions;

  String deviceId;
}

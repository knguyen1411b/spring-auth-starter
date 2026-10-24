package org.app.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignInDTO {
  @NotBlank(message = "auth.username_not_blank")
  String username;

  @NotBlank(message = "auth.password_not_blank")
  String password;

  @NotBlank(message = "auth.device_id_not_blank")
  @jakarta.validation.constraints.Size(max = 100)
  String deviceId;

  @jakarta.validation.constraints.Size(max = 255)
  String deviceName;
}

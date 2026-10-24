package org.app.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefreshTokenDTO {
  @NotBlank(message = "auth.refreshToken_not_blank")
  String refreshToken;

  @NotBlank(message = "auth.deviceId_not_blank")
  @jakarta.validation.constraints.Size(max = 100)
  String deviceId;
}

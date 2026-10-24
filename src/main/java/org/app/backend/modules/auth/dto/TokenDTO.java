package org.app.backend.modules.auth.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenDTO {
  String accessToken;
  String refreshToken;
  Long accessTokenExpiration;
  Long refreshTokenExpiration;
}

package org.app.backend.common.constants;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "application.security.jwt")
@FieldDefaults(level = AccessLevel.PRIVATE)
@org.springframework.validation.annotation.Validated
public class JwtProperties {
  @jakarta.validation.constraints.NotBlank
  @jakarta.validation.constraints.Size(min = 64)
  String secretKey;

  @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive
  Long accessTokenExpiration;

  @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive
  Long refreshTokenExpiration;
}

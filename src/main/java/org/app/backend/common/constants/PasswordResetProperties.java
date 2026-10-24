package org.app.backend.common.constants;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "application.security.password-reset")
public class PasswordResetProperties {
  @Positive private long expiration = 900000;
  @Positive private long resendCooldown = 60000;
}

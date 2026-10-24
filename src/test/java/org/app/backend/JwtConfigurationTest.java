package org.app.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.app.backend.common.constants.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class JwtConfigurationTest {
  private final ApplicationContextRunner context =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
          .withUserConfiguration(Config.class)
          .withPropertyValues(
              "application.security.jwt.access-token-expiration=900000",
              "application.security.jwt.refresh-token-expiration=604800000");

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(JwtProperties.class)
  static class Config {}

  @Test
  void missingSecretPreventsStartup() {
    context.run(result -> assertThat(result).hasFailed());
  }

  @Test
  void shortSecretPreventsStartup() {
    context
        .withPropertyValues("application.security.jwt.secret-key=short")
        .run(result -> assertThat(result).hasFailed());
  }

  @Test
  void invalidExpirationPreventsStartup() {
    context
        .withPropertyValues(
            "application.security.jwt.secret-key=" + "a".repeat(64),
            "application.security.jwt.access-token-expiration=0")
        .run(result -> assertThat(result).hasFailed());
  }

  @Test
  void validConfigurationStarts() {
    context
        .withPropertyValues("application.security.jwt.secret-key=" + "a".repeat(64))
        .run(result -> assertThat(result).hasNotFailed());
  }
}

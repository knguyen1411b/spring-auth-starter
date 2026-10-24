package org.app.backend.modules.auth.token;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefreshTokenCleanupJob {
  RefreshTokenRepository refreshTokenRepository;
  AuthSessionRepository authSessionRepository;
  PasswordResetTokenRepository passwordResetTokenRepository;

  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void cleanup() {
    int deleted = refreshTokenRepository.cleanupExpiredTokens();
    authSessionRepository.cleanup();
    passwordResetTokenRepository.cleanup();
    if (deleted > 0) {
      log.info("[LOG] Cleaned up {} expired refresh tokens", deleted);
    }
  }
}

package org.app.backend.modules.utils.mail;

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
public class VerificationTokenCleanupJob {
  VerificationTokenRepository verificationTokenRepository;

  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void cleanup() {
    int deleted = verificationTokenRepository.cleanupExpiredTokens();
    if (deleted > 0) {
      log.info("[LOG] Cleaned up {} expired or used verification tokens", deleted);
    }
  }
}

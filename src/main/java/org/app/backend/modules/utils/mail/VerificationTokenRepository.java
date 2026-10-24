package org.app.backend.modules.utils.mail;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  Optional<VerificationToken> findByToken(String token);

  @Modifying
  @Query(
      """
              delete from VerificationToken v
              where v.expiresAt < current_timestamp
                        or v.used = true
          """)
  int cleanupExpiredTokens();
}

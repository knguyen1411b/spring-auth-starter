package org.app.backend.modules.auth.token;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
  Optional<PasswordResetToken> findByTokenHash(String hash);

  Optional<PasswordResetToken> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

  @Modifying
  @Query(
      "update PasswordResetToken t set t.usedAt = :now where t.id = :id and t.usedAt is null and t.expiresAt > :now")
  int consume(UUID id, Instant now);

  @Modifying
  @Query(
      "update PasswordResetToken t set t.usedAt = CURRENT_TIMESTAMP where t.userId = :userId and t.usedAt is null")
  void invalidateAll(UUID userId);

  @Modifying
  @Query("delete from PasswordResetToken t where t.expiresAt < CURRENT_TIMESTAMP")
  int cleanup();
}

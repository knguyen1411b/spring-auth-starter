package org.app.backend.modules.auth.token;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
  boolean existsByIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(UUID id, UUID userId, Instant now);

  @Modifying
  @Query(
      "update AuthSession s set s.revokedAt = CURRENT_TIMESTAMP where s.userId = :userId and s.deviceId = :deviceId and s.revokedAt is null")
  void revokeDevice(UUID userId, String deviceId);

  @Modifying
  @Query(
      "update AuthSession s set s.revokedAt = CURRENT_TIMESTAMP where s.userId = :userId and s.revokedAt is null")
  void revokeAll(UUID userId);

  @Modifying
  @Query(
      "delete from AuthSession s where s.revokedAt is not null or s.expiresAt < CURRENT_TIMESTAMP")
  int cleanup();
}

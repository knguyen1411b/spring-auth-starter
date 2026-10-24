package org.app.backend.modules.auth.token;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  @Query(
      """
      select r from RefreshToken r
      where r.tokenHash = :hash
        and r.deviceId = :deviceId
        and r.revokedAt is null
    """)
  Optional<RefreshToken> findValidToken(String hash, String deviceId);

  @Modifying
  @Query(
      """
      update RefreshToken r
      set r.revokedAt = :revokedAt
      where r.userId = :userId
        and r.deviceId = :deviceId
        and r.revokedAt is null
    """)
  void revokeByUserAndDevice(UUID userId, String deviceId, Instant revokedAt);

  @Modifying
  @Query(
      """
      update RefreshToken r
      set r.revokedAt = CURRENT_TIMESTAMP
      where r.userId = :userId
        and r.revokedAt is null
    """)
  void revokeAllByUser(UUID userId);

  @Query(
      """
        select r from RefreshToken r, AuthSession s
        where r.sessionId = s.id
          and s.revokedAt is null
          and s.expiresAt > CURRENT_TIMESTAMP
          and r.userId = :userId
          and r.revokedAt is null
          and r.expiresAt > CURRENT_TIMESTAMP
        order by r.createdAt desc
    """)
  List<RefreshToken> findActiveSessions(UUID userId);

  @Modifying
  @Query(
      """
        update RefreshToken r
        set r.revokedAt = CURRENT_TIMESTAMP
        where r.id = :id
          and r.revokedAt is null
    """)
  int revokeById(UUID id);

  @Modifying
  @Query(
      """
        delete from RefreshToken r
        where r.expiresAt < CURRENT_TIMESTAMP
           or r.revokedAt is not null
    """)
  int cleanupExpiredTokens();

  @Modifying
  @Query(
      """
        update RefreshToken r
        set r.revokedAt = :revokedAt
        where r.userId = :userId
          and r.revokedAt is null
          and r.deviceId <> :currentDeviceId
    """)
  int revokeAllExceptDevice(UUID userId, String currentDeviceId, Instant revokedAt);
}

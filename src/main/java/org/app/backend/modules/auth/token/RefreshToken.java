package org.app.backend.modules.auth.token;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tbl_refresh_tokens")
public class RefreshToken {
  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  UUID id;

  @Column(name = "user_id", nullable = false)
  UUID userId;

  @Column(name = "token_hash", nullable = false)
  String tokenHash;

  @Column(name = "session_id")
  UUID sessionId;

  @Column(name = "device_id", nullable = false)
  String deviceId;

  @Column(name = "device_name")
  String deviceName;

  @Column(name = "ip_address")
  String ipAddress;

  @Column(name = "user_agent")
  String userAgent;

  @Column(name = "expires_at", nullable = false)
  Instant expiresAt;

  @Column(name = "revoked_at")
  Instant revokedAt;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  Instant createdAt;
}

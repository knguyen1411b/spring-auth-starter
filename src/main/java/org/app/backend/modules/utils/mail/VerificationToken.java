package org.app.backend.modules.utils.mail;

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
@Table(name = "tbl_verification_tokens")
public class VerificationToken {
  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  UUID id;

  @Column(name = "user_id", nullable = false)
  UUID userId;

  @Column(name = "token", nullable = false, unique = true)
  String token;

  @Column(name = "expires_at", nullable = false)
  Instant expiresAt;

  @Builder.Default boolean used = false;

  @CreationTimestamp Instant createdAt;
}

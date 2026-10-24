package org.app.backend.modules.log;

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
@Table(name = "tbl_audit_logs")
public class AuditLog {
  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  UUID id;

  UUID userId;

  @Column(nullable = false)
  String actionType;

  @Column(nullable = false)
  String targetType;

  UUID targetId;

  @Column(length = 500)
  String description;

  @CreationTimestamp Instant createdAt;

  @Column(length = 45)
  String ipAddress;

  String userAgent;
}

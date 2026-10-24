package org.app.backend.modules.auth.dto;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SessionDTO {
  String deviceId;
  String deviceName;
  String ipAddress;
  String userAgent;
  Instant createdAt;
  Instant expiresAt;
}

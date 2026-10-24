package org.app.backend.modules.user.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDTO {
  UUID id;
  String username;
  String email;
  String fullName;
  String avatarUrl;
  boolean active;
  boolean locked;
  boolean superuser;
  boolean passwordChanged;

  Instant createdAt;
  Instant updatedAt;
  Instant lastLoginAt;
}

package org.app.backend.modules.role.dto;

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
public class RoleDTO {
  UUID id;
  String code;
  String name;
  boolean system;
  String description;
  Instant createdAt;
}

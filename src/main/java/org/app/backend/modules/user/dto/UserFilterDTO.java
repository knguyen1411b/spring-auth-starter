package org.app.backend.modules.user.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserFilterDTO {
  String q;
  Boolean active;
  Boolean locked;
  Boolean superuser;
  String roleName;
}

package org.app.backend.modules.auth.dto;

import java.util.List;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.app.backend.modules.user.dto.UserDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileDTO extends UserDTO {
  Map<String, Object> metadata;
  List<String> permissions;
}

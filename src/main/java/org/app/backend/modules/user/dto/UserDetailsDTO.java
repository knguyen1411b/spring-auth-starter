package org.app.backend.modules.user.dto;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.app.backend.modules.role.dto.RoleDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDetailsDTO extends UserDTO {
  List<RoleDTO> roles;
}

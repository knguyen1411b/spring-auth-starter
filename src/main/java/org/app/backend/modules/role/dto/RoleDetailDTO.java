package org.app.backend.modules.role.dto;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.app.backend.modules.permission.dto.PermissionDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleDetailDTO extends RoleDTO {
  List<PermissionDTO> permissions;
}

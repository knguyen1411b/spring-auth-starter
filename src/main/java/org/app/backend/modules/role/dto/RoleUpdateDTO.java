package org.app.backend.modules.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.app.backend.common.validation.constraints.PermissionsExist;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleUpdateDTO {
  @NotBlank(message = "role.name_not_blank")
  String name;

  String description;

  @PermissionsExist(message = "role.permissionIds_not_exist")
  @NotNull(message = "role.permissionIds_not_null")
  List<UUID> permissionIds;
}

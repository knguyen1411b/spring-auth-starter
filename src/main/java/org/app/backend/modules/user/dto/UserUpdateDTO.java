package org.app.backend.modules.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.app.backend.common.validation.constraints.RolesExist;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateDTO {
  @Size(max = 255)
  @Pattern(regexp = ".*\\S.*", message = "user.fullName_not_blank")
  String fullName;

  Boolean active;
  Boolean locked;

  @RolesExist(message = "user.roleIds_not_exist")
  List<UUID> roleIds;
}

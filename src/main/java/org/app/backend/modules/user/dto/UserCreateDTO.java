package org.app.backend.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
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
public class UserCreateDTO {
  @Size(min = 6, max = 100, message = "user.username_size_min")
  @jakarta.validation.constraints.Pattern(regexp = "[A-Za-z0-9_.-]+")
  @NotBlank(message = "user.username_not_blank")
  String username;

  @org.app.backend.common.validation.constraints.ValidPassword
  @NotBlank(message = "user.password_not_blank")
  String password;

  @NotBlank(message = "user.email_not_blank")
  @jakarta.validation.constraints.Email
  @Size(max = 255)
  String email;

  @NotBlank(message = "user.fullName_not_blank")
  @Size(max = 255)
  String fullName;

  @RolesExist(message = "user.roleIds_not_exist")
  List<UUID> roleIds;
}

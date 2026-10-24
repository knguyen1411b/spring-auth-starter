package org.app.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignUpDTO {
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
}

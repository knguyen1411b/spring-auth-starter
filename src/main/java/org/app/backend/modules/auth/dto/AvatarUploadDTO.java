package org.app.backend.modules.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.app.backend.common.validation.constraints.ValidImage;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AvatarUploadDTO {
  @ValidImage(message = "auth.avatar_invalid_image")
  @NotNull(message = "auth.avatar_not_null")
  MultipartFile avatarFile;
}

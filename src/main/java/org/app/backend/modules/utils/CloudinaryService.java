package org.app.backend.modules.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.app.backend.common.constants.CloudinaryProperties;
import org.app.backend.common.constants.MessageConstants;
import org.app.backend.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class CloudinaryService {
  Cloudinary cloudinary;
  CloudinaryProperties cloudinaryProperties;

  public String uploadImage(MultipartFile file, UUID userId) {
    if (!cloudinaryProperties.isEnabled()) {
      throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "Avatar uploads are disabled");
    }
    try {
      var uploadResult =
          cloudinary
              .uploader()
              .upload(
                  file.getBytes(),
                  ObjectUtils.asMap(
                      "folder",
                      cloudinaryProperties.getFolder(),
                      "public_id",
                      cloudinaryProperties.getPublicIdPrefix() + userId,
                      "overwrite",
                      true,
                      "resource_type",
                      "image"));

      return (String) uploadResult.get("secure_url");
    } catch (IOException e) {
      throw new AppException(
          HttpStatus.INTERNAL_SERVER_ERROR, MessageConstants.INTERNAL_SERVER_ERROR.name());
    }
  }
}

package org.app.backend.common.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import org.app.backend.common.validation.constraints.ValidImage;
import org.springframework.web.multipart.MultipartFile;

public class ValidImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {
  private long maxSize;
  private String[] allowedTypes;

  @Override
  public void initialize(ValidImage constraintAnnotation) {
    this.maxSize = constraintAnnotation.maxSize();
    this.allowedTypes = constraintAnnotation.allowedTypes();
  }

  @Override
  public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
    if (file == null || file.isEmpty()) {
      return false;
    }
    if (file.getSize() > maxSize) {
      return false;
    }
    return Arrays.asList(allowedTypes).contains(file.getContentType());
  }
}

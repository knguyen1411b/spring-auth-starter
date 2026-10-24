package org.app.backend.common.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;
import org.app.backend.common.validation.constraints.ValidPassword;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {
  @Override
  public boolean isValid(String password, ConstraintValidatorContext context) {
    return password != null
        && !password.isBlank()
        && password.length() >= 12
        && password.length() <= 64
        && password.getBytes(StandardCharsets.UTF_8).length <= 72;
  }
}

package org.app.backend.common.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.app.backend.common.validation.constraints.PermissionsExist;
import org.app.backend.modules.permission.PermissionRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionsExistValidator
    implements ConstraintValidator<PermissionsExist, List<UUID>> {
  PermissionRepository permissionRepository;

  @Override
  public boolean isValid(List<UUID> permissionIds, ConstraintValidatorContext context) {
    if (permissionIds == null || permissionIds.isEmpty()) return true;
    return permissionRepository.countByIdIn(permissionIds) == permissionIds.size();
  }
}

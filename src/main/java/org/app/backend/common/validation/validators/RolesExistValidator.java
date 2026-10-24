package org.app.backend.common.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.app.backend.common.validation.constraints.RolesExist;
import org.app.backend.modules.role.RoleRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RolesExistValidator implements ConstraintValidator<RolesExist, List<UUID>> {
  RoleRepository roleRepository;

  @Override
  public boolean isValid(List<UUID> roleIds, ConstraintValidatorContext context) {
    if (roleIds == null || roleIds.isEmpty()) return true;
    return roleRepository.countByIdIn(roleIds) == roleIds.size();
  }
}

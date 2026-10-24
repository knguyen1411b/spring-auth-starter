package org.app.backend.modules.role;

import java.util.HashSet;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.app.backend.exception.AppException;
import org.app.backend.modules.log.AuditLogService;
import org.app.backend.modules.permission.PermissionRepository;
import org.app.backend.modules.role.dto.RoleCreateDTO;
import org.app.backend.modules.role.dto.RoleDTO;
import org.app.backend.modules.role.dto.RoleDetailDTO;
import org.app.backend.modules.role.dto.RoleUpdateDTO;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleServiceImpl implements RoleService {
  RoleRepository roleRepository;
  PermissionRepository permissionRepository;
  ModelMapper modelMapper;
  AuditLogService auditLogService;

  @Override
  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyAuthority('all:all', 'role:read', 'user:create', 'user:update')")
  public Page<RoleDTO> findAll(String q, Pageable pageable) {
    org.app.backend.common.validation.PageableValidation.validate(
        pageable, java.util.Set.of("id", "code", "name", "system", "createdAt"));
    return roleRepository
        .findByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrDescriptionContaining(
            q, q, q, pageable)
        .map(r -> modelMapper.map(r, RoleDTO.class));
  }

  @Override
  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyAuthority('all:all', 'role:read')")
  public RoleDetailDTO findById(UUID id) {
    return roleRepository
        .findById(id)
        .map(role -> modelMapper.map(role, RoleDetailDTO.class))
        .orElseThrow(
            () -> new AppException(HttpStatus.NOT_FOUND, RoleMessage.NOT_FOUND.getMessage()));
  }

  @Override
  @Transactional
  @PreAuthorize("hasAnyAuthority('all:all', 'role:create')")
  public void create(RoleCreateDTO dto) {
    if (roleRepository.existsByCode(dto.getCode())) {
      throw new AppException(HttpStatus.BAD_REQUEST, RoleMessage.CODE_EXISTS.getMessage());
    }
    Role role = modelMapper.map(dto, Role.class);
    role.setPermissions(new HashSet<>(permissionRepository.findAllById(dto.getPermissionIds())));
    roleRepository.save(role);
    auditLogService.created("Role", role.getId(), "Created role with code: " + role.getCode());
  }

  @Override
  @Transactional
  @PreAuthorize("hasAnyAuthority('all:all', 'role:update')")
  public void update(UUID id, RoleUpdateDTO dto) {
    Role role =
        roleRepository
            .findById(id)
            .orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, RoleMessage.NOT_FOUND.getMessage()));
    modelMapper.map(dto, role);
    role.setPermissions(new HashSet<>(permissionRepository.findAllById(dto.getPermissionIds())));
    auditLogService.updated("Role", role.getId());
  }

  @Override
  @Transactional
  @PreAuthorize("hasAnyAuthority('all:all', 'role:delete')")
  public void delete(UUID id) {
    Role role =
        roleRepository
            .findById(id)
            .orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, RoleMessage.NOT_FOUND.getMessage()));

    if (role.isSystem()) {
      throw new AppException(
          HttpStatus.BAD_REQUEST, RoleMessage.CANNOT_DELETE_SYSTEM_ROLE.getMessage());
    }
    roleRepository.delete(role);
    auditLogService.deleted("Role", role.getId());
  }
}

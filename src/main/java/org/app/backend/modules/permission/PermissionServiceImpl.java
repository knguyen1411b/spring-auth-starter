package org.app.backend.modules.permission;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.app.backend.exception.AppException;
import org.app.backend.modules.permission.dto.PermissionDTO;
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
public class PermissionServiceImpl implements PermissionService {
  PermissionRepository permissionRepository;
  ModelMapper modelMapper;

  @Override
  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyAuthority('all:all', 'permission:read', 'role:create', 'role:update')")
  public Page<PermissionDTO> findAll(String q, Pageable pageable) {
    org.app.backend.common.validation.PageableValidation.validate(
        pageable, java.util.Set.of("id", "code", "createdAt"));
    return permissionRepository
        .findByCodeContainingIgnoreCaseOrDescriptionContaining(q, q, pageable)
        .map(p -> modelMapper.map(p, PermissionDTO.class));
  }

  @Override
  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyAuthority('all:all', 'permission:read')")
  public PermissionDTO findById(UUID id) {
    return permissionRepository
        .findById(id)
        .map(p -> modelMapper.map(p, PermissionDTO.class))
        .orElseThrow(
            () -> new AppException(HttpStatus.NOT_FOUND, PermissionMessage.NOT_FOUND.getMessage()));
  }
}

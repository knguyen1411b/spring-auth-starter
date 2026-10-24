package org.app.backend.modules.user;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.app.backend.exception.AppException;
import org.app.backend.modules.auth.token.AuthSessionRepository;
import org.app.backend.modules.auth.token.RefreshTokenRepository;
import org.app.backend.modules.log.AuditLogService;
import org.app.backend.modules.role.RoleRepository;
import org.app.backend.modules.user.dto.*;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {
  UserRepository userRepository;
  RoleRepository roleRepository;
  PasswordEncoder passwordEncoder;
  ModelMapper modelMapper;
  AuditLogService auditLogService;
  AuthSessionRepository authSessionRepository;
  RefreshTokenRepository refreshTokenRepository;

  @Override
  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyAuthority('all:all', 'user:read')")
  public Page<UserDTO> findAll(UserFilterDTO filter, Pageable pageable) {
    org.app.backend.common.validation.PageableValidation.validate(
        pageable,
        java.util.Set.of(
            "id",
            "username",
            "email",
            "fullName",
            "active",
            "locked",
            "createdAt",
            "updatedAt",
            "lastLoginAt"));
    Specification<User> spec = UserSpecification.filter(filter);
    return userRepository.findAll(spec, pageable).map(user -> modelMapper.map(user, UserDTO.class));
  }

  @Override
  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyAuthority('all:all', 'user:read')")
  public UserDetailsDTO findById(UUID id) {
    return userRepository
        .findById(id)
        .map(user -> modelMapper.map(user, UserDetailsDTO.class))
        .orElseThrow(
            () -> new AppException(HttpStatus.NOT_FOUND, UserMessage.NOT_FOUND.getMessage()));
  }

  @Override
  @Transactional
  @PreAuthorize("hasAnyAuthority('all:all', 'user:create')")
  public void create(UserCreateDTO dto) {
    dto.setUsername(dto.getUsername().trim());
    dto.setEmail(dto.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
    if (userRepository.existsByUsername(dto.getUsername())) {
      throw new AppException(HttpStatus.BAD_REQUEST, UserMessage.USERNAME_EXISTS.getMessage());
    }
    User user =
        User.builder()
            .username(dto.getUsername())
            .email(dto.getEmail())
            .fullName(dto.getFullName())
            .build();
    user.setRoles(
        new HashSet<>(
            roleRepository.findAllById(
                dto.getRoleIds() == null ? java.util.List.of() : dto.getRoleIds())));
    user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
    userRepository.save(user);
    auditLogService.created("USER", user.getId(), "Created user with ID " + user.getId());
  }

  @Override
  @Transactional
  @PreAuthorize("hasAnyAuthority('all:all', 'user:update')")
  public void update(UUID id, UserUpdateDTO dto) {
    User user =
        userRepository
            .findByIdForUpdate(id)
            .orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, UserMessage.NOT_FOUND.getMessage()));
    if (dto.getRoleIds() != null)
      user.setRoles(new HashSet<>(roleRepository.findAllById(dto.getRoleIds())));
    if (dto.getFullName() != null) user.setFullName(dto.getFullName().trim());
    if (dto.getActive() != null) user.setActive(dto.getActive());
    if (dto.getLocked() != null) user.setLocked(dto.getLocked());
    if (user.isLocked() || !user.isActive()) {
      authSessionRepository.revokeAll(id);
      refreshTokenRepository.revokeAllByUser(id);
    }
    auditLogService.updated("USER", id);
  }

  @Override
  @Transactional
  @PreAuthorize("hasAnyAuthority('all:all', 'user:delete')")
  public void delete(UUID id) {
    if (!userRepository.existsById(id)) {
      throw new AppException(HttpStatus.NOT_FOUND, UserMessage.NOT_FOUND.getMessage());
    }
    userRepository.deleteById(id);
    auditLogService.deleted("USER", id);
  }

  @Override
  @Transactional(readOnly = true)
  public CustomUserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
    User user =
        userRepository
            .findById(UUID.fromString(id))
            .orElseThrow(() -> new UsernameNotFoundException(UserMessage.NOT_FOUND.getMessage()));
    CustomUserDetails userDetails = modelMapper.map(user, CustomUserDetails.class);
    if (user.getRoles() != null && !user.isSuperuser()) {
      List<GrantedAuthority> authorities =
          user.getRoles().stream()
              .flatMap(role -> role.getPermissions().stream())
              .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
              .collect(Collectors.toList());
      userDetails.setAuthorities(authorities);
    }
    return userDetails;
  }
}

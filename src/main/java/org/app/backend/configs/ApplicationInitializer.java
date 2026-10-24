package org.app.backend.configs;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.app.backend.common.constants.SuperUserProperties;
import org.app.backend.modules.permission.Permission;
import org.app.backend.modules.permission.PermissionRepository;
import org.app.backend.modules.user.User;
import org.app.backend.modules.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApplicationInitializer {
  PasswordEncoder passwordEncoder;
  SuperUserProperties superuser;

  @Bean
  CommandLineRunner initData(
      UserRepository userRepository, PermissionRepository permissionRepository) {
    return args -> {
      if (superuser.isEnabled()) {
        if (!org.springframework.util.StringUtils.hasText(superuser.getUsername())
            || !org.springframework.util.StringUtils.hasText(superuser.getEmail())
            || superuser.getPassword() == null
            || superuser.getPassword().length() < 12
            || superuser.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                > 72) {
          throw new IllegalStateException(
              "Bootstrap admin requires username, email and a password of at least 12 characters");
        }
      }
      if (superuser.isEnabled()) {
        superuser.setUsername(superuser.getUsername().trim());
        superuser.setEmail(superuser.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
      }
      if (superuser.isEnabled() && !userRepository.existsByUsername(superuser.getUsername())) {
        userRepository.save(
            User.builder()
                .username(superuser.getUsername().trim())
                .passwordHash(passwordEncoder.encode(superuser.getPassword()))
                .email(superuser.getEmail().trim().toLowerCase(java.util.Locale.ROOT))
                .fullName(superuser.getFullName())
                .superuser(true)
                .build());
        log.warn(
            "[LOG] Bootstrap admin created; disable BOOTSTRAP_ADMIN_ENABLED after provisioning.");
      }
      String[] defaultPermissions = {
        "permission:read",
        "role:read",
        "role:create",
        "role:update",
        "role:delete",
        "user:read",
        "user:create",
        "user:update",
        "user:delete",
      };
      for (String perm : defaultPermissions) {
        if (!permissionRepository.existsByCode(perm)) {
          permissionRepository.save(Permission.builder().code(perm).description(perm).build());
        }
      }

      log.info("[LOG] Default permissions initialized.");
      log.info("Application initialization completed...");
    };
  }
}

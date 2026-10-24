package org.app.backend.modules.permission;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
  Page<Permission> findByCodeContainingIgnoreCaseOrDescriptionContaining(
      String code, String description, Pageable pageable);

  boolean existsByCode(String code);

  Long countByIdIn(List<UUID> ids);
}

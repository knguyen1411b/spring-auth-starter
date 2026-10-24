package org.app.backend.modules.role;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {
  Page<Role> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrDescriptionContaining(
      String code, String name, String description, Pageable pageable);

  boolean existsByCode(String code);

  Long countByIdIn(List<UUID> ids);
}

package org.app.backend.modules.permission;

import java.util.UUID;
import org.app.backend.modules.permission.dto.PermissionDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PermissionService {
  Page<PermissionDTO> findAll(String q, Pageable pageable);

  PermissionDTO findById(UUID id);
}

package org.app.backend.modules.role;

import java.util.UUID;
import org.app.backend.modules.role.dto.RoleCreateDTO;
import org.app.backend.modules.role.dto.RoleDTO;
import org.app.backend.modules.role.dto.RoleDetailDTO;
import org.app.backend.modules.role.dto.RoleUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoleService {
  Page<RoleDTO> findAll(String q, Pageable pageable);

  RoleDetailDTO findById(UUID id);

  void create(RoleCreateDTO dto);

  void update(UUID id, RoleUpdateDTO dto);

  void delete(UUID id);
}

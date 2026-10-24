package org.app.backend.modules.user;

import java.util.UUID;
import org.app.backend.modules.user.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
  Page<UserDTO> findAll(UserFilterDTO filter, Pageable pageable);

  UserDetailsDTO findById(UUID id);

  void create(UserCreateDTO dto);

  void update(UUID id, UserUpdateDTO dto);

  void delete(UUID id);

  CustomUserDetails loadUserByUsername(String username);
}

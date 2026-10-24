package org.app.backend.modules.user;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomUserDetails implements UserDetails {
  UUID id;
  String username;
  String password;

  String email;

  String fullName;
  String avatarUrl;

  Map<String, Object> metadata;

  boolean superuser;
  boolean active;
  boolean locked;
  boolean passwordChanged;

  Instant createdAt;
  Instant updatedAt;
  Instant lastLoginAt;

  List<GrantedAuthority> authorities;

  @Override
  public boolean isEnabled() {
    return active;
  }

  @Override
  public boolean isAccountNonLocked() {
    return !locked;
  }

  @Override
  public List<GrantedAuthority> getAuthorities() {
    return superuser
        ? List.of(new SimpleGrantedAuthority("all:all"))
        : authorities != null ? authorities : List.of();
  }
}

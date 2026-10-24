package org.app.backend.modules.user;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.app.backend.modules.role.Role;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "tbl_users")
public class User {
  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  UUID id;

  @Column(length = 100, nullable = false, unique = true)
  String username;

  @Column(unique = true)
  String email;

  @Column(name = "password_hash", nullable = false)
  String passwordHash;

  @Column(name = "full_name")
  String fullName;

  @Column(name = "avatar_url")
  String avatarUrl;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  boolean active = true;

  @Builder.Default
  @Column(name = "is_locked", nullable = false)
  boolean locked = false;

  @Builder.Default
  @Column(name = "is_superuser", nullable = false)
  boolean superuser = false;

  @Builder.Default
  @Column(name = "password_changed", nullable = false)
  boolean passwordChanged = false;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  Instant updatedAt;

  @Column(name = "last_login_at")
  Instant lastLoginAt;

  @Builder.Default
  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb", nullable = false)
  Map<String, Object> metadata = new HashMap<>();

  @Builder.Default
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "tbl_user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  Set<Role> roles = new HashSet<>();
}

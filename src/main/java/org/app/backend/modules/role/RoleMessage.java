package org.app.backend.modules.role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public enum RoleMessage {
  INDEX_SUCCESS("role.index_success"),
  SHOW_SUCCESS("role.show_success"),
  CREATE_SUCCESS("role.create_success"),
  UPDATE_SUCCESS("role.update_success"),
  DELETE_SUCCESS("role.delete_success"),
  CODE_EXISTS("role.code_exists"),
  NOT_FOUND("role.not_found"),
  CANNOT_DELETE_SYSTEM_ROLE("role.cannot_delete_system_role");
  String message;
}

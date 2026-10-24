package org.app.backend.modules.permission;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public enum PermissionMessage {
  INDEX_SUCCESS("permission.index_success"),
  SHOW_SUCCESS("permission.show_success"),
  NOT_FOUND("permission.not_found"),
  ;
  String message;
}

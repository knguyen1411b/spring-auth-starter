package org.app.backend.modules.user;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum UserMessage {
  INDEX_SUCCESS("user.index_success"),
  SHOW_SUCCESS("user.show_success"),
  CREATE_SUCCESS("user.create_success"),
  UPDATE_SUCCESS("user.update_success"),
  DELETE_SUCCESS("user.delete_success"),
  USERNAME_EXISTS("user.username_exists"),
  USERNAME_TAKEN("auth.username_taken"),
  EMAIL_TAKEN("auth.email_taken"),
  NOT_FOUND("user.not_found");
  String message;
}

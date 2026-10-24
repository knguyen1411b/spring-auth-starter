package org.app.backend.common.constants;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum MessageConstants {
  INTERNAL_SERVER_ERROR("common.internal_server_error"),
  BAD_REQUEST("common.bad_request"),
  FORBIDDEN("common.forbidden"),
  METHOD_ARGUMENT_TYPE_MISMATCH("common.method_argument_type_mismatch"),
  UNAUTHORIZED("common.unauthorized"),
  RESOURCE_NOT_FOUND("common.resource_not_found");
  String message;
}

package org.app.backend.modules.auth;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum AuthMessage {
  INVALID_CREDENTIALS("auth.invalid_credentials"),
  INACTIVE("auth.inactive"),
  LOCKED("auth.locked"),
  INVALID_REFRESH_TOKEN("auth.invalid"),
  EXPIRED_REFRESH_TOKEN("auth.expired"),
  LOGIN_SUCCESS("auth.login_success"),
  TOKEN_REFRESH_SUCCESS("auth.token_refresh_success"),
  LOGOUT_SUCCESS("auth.logout_success"),
  SESSIONS_RETRIEVED("auth.sessions_retrieved"),
  PROFILE_RETRIEVED("auth.profile_retrieved"),
  SIGNUP_SUCCESS("auth.signup_success"),
  PASSWORD_CHANGE_SUCCESS("auth.password_change_success"),
  AVATAR_UPDATED("auth.avatar_updated"),
  ACCOUNT_ACTIVATION_SUCCESS("auth.account_activation_success"),
  EXPIRED_ACTIVATION_TOKEN("auth.expired_activation_token"),
  INVALID_ACTIVATION_TOKEN("auth.invalid_activation_token");
  String message;
}

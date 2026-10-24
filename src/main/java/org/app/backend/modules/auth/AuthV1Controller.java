package org.app.backend.modules.auth;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.app.backend.common.response.ApiResponse;
import org.app.backend.common.response.DataApiResponse;
import org.app.backend.modules.auth.dto.*;
import org.app.backend.modules.user.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "04. Auth (V1)", description = "Authentication and Authorization APIs")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthV1Controller {
  AuthService authService;

  @SecurityRequirements
  @PostMapping("/sign-in")
  public DataApiResponse<TokenDTO> signIn(
      @RequestBody @Valid SignInDTO dto, HttpServletRequest request) {
    return DataApiResponse.success(
        authService.signIn(dto, request), AuthMessage.LOGIN_SUCCESS.getMessage());
  }

  @SecurityRequirements
  @PostMapping("/refresh")
  public DataApiResponse<TokenDTO> refresh(@RequestBody @Valid RefreshTokenDTO dto) {
    return DataApiResponse.success(
        authService.refresh(dto), AuthMessage.TOKEN_REFRESH_SUCCESS.getMessage());
  }

  @SecurityRequirements
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  @PostMapping("/sign-up")
  public ApiResponse signUp(@RequestBody @Valid SignUpDTO dto) {
    authService.signUp(dto);
    return ApiResponse.created(AuthMessage.SIGNUP_SUCCESS.getMessage());
  }

  @GetMapping("/profile")
  public DataApiResponse<ProfileDTO> getProfile(@AuthenticationPrincipal CustomUserDetails user) {
    return DataApiResponse.success(
        authService.getProfile(user), AuthMessage.PROFILE_RETRIEVED.getMessage());
  }

  @GetMapping("/sessions")
  public DataApiResponse<List<SessionDTO>> listSessions(
      @AuthenticationPrincipal CustomUserDetails user) {
    return DataApiResponse.success(
        authService.listSessions(user.getId()), AuthMessage.SESSIONS_RETRIEVED.getMessage());
  }

  @DeleteMapping("/sessions")
  public ApiResponse signOutAll(@AuthenticationPrincipal CustomUserDetails user) {
    authService.signOutAll(user.getId());
    return ApiResponse.success(AuthMessage.LOGOUT_SUCCESS.getMessage());
  }

  @DeleteMapping("/sessions/{deviceId}")
  public ApiResponse signOut(
      @AuthenticationPrincipal CustomUserDetails user, @PathVariable String deviceId) {
    authService.signOut(user.getId(), deviceId);
    return ApiResponse.success(AuthMessage.LOGOUT_SUCCESS.getMessage());
  }

  @PostMapping("/change-password")
  public ApiResponse changePassword(
      @AuthenticationPrincipal CustomUserDetails user, @RequestBody @Valid ChangePasswordDTO dto) {
    authService.changePassword(user.getId(), dto);
    return ApiResponse.success(AuthMessage.PASSWORD_CHANGE_SUCCESS.getMessage());
  }

  @PostMapping(value = "/avatar", consumes = "multipart/form-data")
  public ApiResponse upload(
      @Valid @ModelAttribute AvatarUploadDTO avatarUploadDTO,
      @AuthenticationPrincipal CustomUserDetails user) {
    authService.uploadAvatar(user.getId(), avatarUploadDTO.getAvatarFile());
    return ApiResponse.success(AuthMessage.AVATAR_UPDATED.getMessage());
  }

  @SecurityRequirements
  @PostMapping("/activate")
  public ApiResponse activateAccount(@RequestBody @Valid ActiveAccountDTO dto) {
    authService.activateAccount(dto.getToken());
    return ApiResponse.success(AuthMessage.ACCOUNT_ACTIVATION_SUCCESS.getMessage());
  }

  @SecurityRequirements
  @PostMapping("/forgot-password")
  public ApiResponse forgotPassword(@RequestBody @Valid ForgotPasswordDTO dto) {
    authService.forgotPassword(dto);
    return ApiResponse.success("If the account is eligible, a password reset email will be sent.");
  }

  @SecurityRequirements
  @PostMapping("/reset-password")
  public ApiResponse resetPassword(@RequestBody @Valid ResetPasswordDTO dto) {
    authService.resetPassword(dto);
    return ApiResponse.success("Password reset successfully. Please sign in again.");
  }
}

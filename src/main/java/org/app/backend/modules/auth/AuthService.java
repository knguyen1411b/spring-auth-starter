package org.app.backend.modules.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.app.backend.modules.auth.dto.*;
import org.app.backend.modules.user.CustomUserDetails;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {
  ProfileDTO getProfile(CustomUserDetails userDetails);

  TokenDTO signIn(SignInDTO dto, HttpServletRequest request);

  TokenDTO refresh(RefreshTokenDTO dto);

  void signOut(UUID userId, String deviceId);

  void signOutAll(UUID userId);

  List<SessionDTO> listSessions(UUID userId);

  void signUp(SignUpDTO dto);

  void changePassword(UUID userId, ChangePasswordDTO dto);

  void uploadAvatar(UUID userId, MultipartFile avatarFile);

  void activateAccount(String token);

  void forgotPassword(ForgotPasswordDTO dto);

  void resetPassword(ResetPasswordDTO dto);
}

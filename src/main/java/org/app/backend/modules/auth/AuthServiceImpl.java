package org.app.backend.modules.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.app.backend.common.constants.JwtProperties;
import org.app.backend.common.constants.PasswordResetProperties;
import org.app.backend.exception.AppException;
import org.app.backend.modules.auth.dto.*;
import org.app.backend.modules.auth.token.AuthSession;
import org.app.backend.modules.auth.token.AuthSessionRepository;
import org.app.backend.modules.auth.token.PasswordResetToken;
import org.app.backend.modules.auth.token.PasswordResetTokenRepository;
import org.app.backend.modules.auth.token.RefreshToken;
import org.app.backend.modules.auth.token.RefreshTokenRepository;
import org.app.backend.modules.log.AuditLogService;
import org.app.backend.modules.user.*;
import org.app.backend.modules.utils.AuthUtils;
import org.app.backend.modules.utils.CloudinaryService;
import org.app.backend.modules.utils.JwtService;
import org.app.backend.modules.utils.mail.MailService;
import org.app.backend.modules.utils.mail.VerificationToken;
import org.app.backend.modules.utils.mail.VerificationTokenRepository;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {
  JwtService jwtService;
  JwtProperties jwtProperties;
  UserRepository userRepository;
  PasswordEncoder passwordEncoder;
  AuditLogService auditLogService;
  ModelMapper modelMapper;
  MailService mailService;
  CloudinaryService cloudinaryService;
  VerificationTokenRepository verificationTokenRepository;
  RefreshTokenRepository refreshTokenRepository;
  AuthSessionRepository authSessionRepository;
  PasswordResetTokenRepository passwordResetTokenRepository;
  PasswordResetProperties passwordResetProperties;

  @Override
  @Transactional
  public TokenDTO signIn(SignInDTO dto, HttpServletRequest request) {
    User user =
        userRepository
            .findByUsernameForUpdate(dto.getUsername().trim())
            .orElseThrow(
                () ->
                    new AppException(
                        HttpStatus.UNAUTHORIZED, AuthMessage.INVALID_CREDENTIALS.getMessage()));

    if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {

      throw new AppException(HttpStatus.UNAUTHORIZED, AuthMessage.INVALID_CREDENTIALS.getMessage());
    }

    if (!user.isActive()) {
      throw new AppException(HttpStatus.FORBIDDEN, AuthMessage.INACTIVE.getMessage());
    }

    if (user.isLocked()) {

      throw new AppException(HttpStatus.FORBIDDEN, AuthMessage.LOCKED.getMessage());
    }

    authSessionRepository.revokeDevice(user.getId(), dto.getDeviceId());
    var session =
        authSessionRepository.save(
            AuthSession.builder()
                .userId(user.getId())
                .deviceId(dto.getDeviceId())
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()))
                .build());
    String accessToken = jwtService.generateAccessToken(user.getId(), session.getId());

    String refreshTokenValue = AuthUtils.generateRefreshToken();
    String refreshTokenHash = AuthUtils.hashRefreshToken(refreshTokenValue);

    refreshTokenRepository.revokeByUserAndDevice(user.getId(), dto.getDeviceId(), Instant.now());

    RefreshToken refreshToken =
        RefreshToken.builder()
            .userId(user.getId())
            .sessionId(session.getId())
            .tokenHash(refreshTokenHash)
            .deviceId(dto.getDeviceId())
            .deviceName(dto.getDeviceName())
            .ipAddress(AuthUtils.resolveIp(request))
            .userAgent(AuthUtils.resolveUserAgent(request))
            .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()))
            .build();

    refreshTokenRepository.save(refreshToken);

    auditLogService.signIn();

    user.setLastLoginAt(Instant.now());
    userRepository.save(user);

    return TokenDTO.builder()
        .accessToken(accessToken)
        .refreshToken(refreshTokenValue)
        .accessTokenExpiration(jwtProperties.getAccessTokenExpiration())
        .refreshTokenExpiration(jwtProperties.getRefreshTokenExpiration())
        .build();
  }

  @Override
  @Transactional
  public TokenDTO refresh(RefreshTokenDTO dto) {

    String tokenHash = AuthUtils.hashRefreshToken(dto.getRefreshToken());

    RefreshToken oldToken =
        refreshTokenRepository
            .findValidToken(tokenHash, dto.getDeviceId())
            .orElseThrow(
                () ->
                    new AppException(
                        HttpStatus.UNAUTHORIZED, AuthMessage.INVALID_REFRESH_TOKEN.getMessage()));

    if (oldToken.getExpiresAt().isBefore(Instant.now())) {

      throw new AppException(
          HttpStatus.UNAUTHORIZED, AuthMessage.EXPIRED_REFRESH_TOKEN.getMessage());
    }

    User user =
        userRepository
            .findByIdForUpdate(oldToken.getUserId())
            .orElseThrow(
                () ->
                    new AppException(
                        HttpStatus.UNAUTHORIZED, AuthMessage.INVALID_REFRESH_TOKEN.getMessage()));
    if (!user.isActive()
        || user.isLocked()
        || oldToken.getSessionId() == null
        || !authSessionRepository.existsByIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(
            oldToken.getSessionId(), user.getId(), Instant.now())) {
      throw new AppException(
          HttpStatus.UNAUTHORIZED, AuthMessage.INVALID_REFRESH_TOKEN.getMessage());
    }
    int revoked = refreshTokenRepository.revokeById(oldToken.getId());
    if (revoked == 0)
      throw new AppException(
          HttpStatus.UNAUTHORIZED, AuthMessage.INVALID_REFRESH_TOKEN.getMessage());

    var session = authSessionRepository.findById(oldToken.getSessionId()).orElseThrow();
    session.setExpiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()));
    String newAccessToken = jwtService.generateAccessToken(oldToken.getUserId(), session.getId());
    String newRefreshValue = AuthUtils.generateRefreshToken();
    String newRefreshHash = AuthUtils.hashRefreshToken(newRefreshValue);

    RefreshToken newToken =
        RefreshToken.builder()
            .userId(oldToken.getUserId())
            .sessionId(oldToken.getSessionId())
            .deviceId(oldToken.getDeviceId())
            .deviceName(oldToken.getDeviceName())
            .ipAddress(oldToken.getIpAddress())
            .userAgent(oldToken.getUserAgent())
            .tokenHash(newRefreshHash)
            .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()))
            .build();
    refreshTokenRepository.save(newToken);

    return TokenDTO.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshValue)
        .accessTokenExpiration(jwtProperties.getAccessTokenExpiration())
        .refreshTokenExpiration(jwtProperties.getRefreshTokenExpiration())
        .build();
  }

  @Override
  @Transactional
  public void signOut(UUID userId, String deviceId) {
    userRepository.findByIdForUpdate(userId).orElseThrow();
    authSessionRepository.revokeDevice(userId, deviceId);
    refreshTokenRepository.revokeByUserAndDevice(userId, deviceId, Instant.now());
    auditLogService.signOut();
  }

  @Override
  @Transactional
  public void signOutAll(UUID userId) {
    userRepository.findByIdForUpdate(userId).orElseThrow();
    authSessionRepository.revokeAll(userId);
    refreshTokenRepository.revokeAllByUser(userId);
    auditLogService.signOutAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<SessionDTO> listSessions(UUID userId) {
    return refreshTokenRepository.findActiveSessions(userId).stream()
        .map(
            token ->
                SessionDTO.builder()
                    .deviceId(token.getDeviceId())
                    .deviceName(token.getDeviceName())
                    .ipAddress(token.getIpAddress())
                    .userAgent(token.getUserAgent())
                    .createdAt(token.getCreatedAt())
                    .expiresAt(token.getExpiresAt())
                    .build())
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ProfileDTO getProfile(CustomUserDetails userDetails) {
    ProfileDTO profileDTO = modelMapper.map(userDetails, ProfileDTO.class);
    profileDTO.setPermissions(
        userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    return profileDTO;
  }

  @Override
  @Transactional
  public void signUp(SignUpDTO dto) {
    dto.setUsername(dto.getUsername().trim());
    dto.setEmail(dto.getEmail().trim().toLowerCase(Locale.ROOT));
    if (userRepository.existsByUsername(dto.getUsername())) {
      throw new AppException(HttpStatus.CONFLICT, UserMessage.USERNAME_TAKEN.getMessage());
    }

    if (userRepository.existsByEmail(dto.getEmail())) {
      throw new AppException(HttpStatus.CONFLICT, UserMessage.EMAIL_TAKEN.getMessage());
    }

    User newUser =
        User.builder()
            .username(dto.getUsername())
            .email(dto.getEmail())
            .fullName(dto.getFullName())
            .passwordHash(passwordEncoder.encode(dto.getPassword()))
            .active(false)
            .passwordChanged(true)
            .build();

    userRepository.save(newUser);

    auditLogService.signUp();

    String newVerificationToken = AuthUtils.generateRefreshToken();
    String newVerificationTokenHash = AuthUtils.hashRefreshToken(newVerificationToken);

    verificationTokenRepository.save(
        VerificationToken.builder()
            .userId(newUser.getId())
            .token(newVerificationTokenHash)
            .expiresAt(Instant.now().plusMillis(60 * 60 * 1000 * 24))
            .build());

    mailService.sendActivationEmail(newUser.getEmail(), newVerificationToken);
  }

  @Override
  @Transactional
  public void activateAccount(String token) {
    String tokenHash = AuthUtils.hashRefreshToken(token);
    VerificationToken verificationToken =
        verificationTokenRepository
            .findByToken(tokenHash)
            .orElseThrow(
                () ->
                    new AppException(
                        HttpStatus.UNAUTHORIZED,
                        AuthMessage.INVALID_ACTIVATION_TOKEN.getMessage()));
    if (verificationToken.isUsed() || verificationToken.getExpiresAt().isBefore(Instant.now())) {
      throw new AppException(
          HttpStatus.UNAUTHORIZED, AuthMessage.EXPIRED_ACTIVATION_TOKEN.getMessage());
    }
    User user =
        userRepository
            .findById(verificationToken.getUserId())
            .orElseThrow(
                () ->
                    new AppException(
                        HttpStatus.UNAUTHORIZED,
                        AuthMessage.INVALID_ACTIVATION_TOKEN.getMessage()));
    user.setActive(true);
    userRepository.save(user);
    verificationToken.setUsed(true);
    verificationTokenRepository.save(verificationToken);
  }

  @Override
  @Transactional
  public void changePassword(UUID userId, ChangePasswordDTO dto) {
    User user =
        userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, UserMessage.NOT_FOUND.getMessage()));
    if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
      throw new AppException(HttpStatus.UNAUTHORIZED, AuthMessage.INVALID_CREDENTIALS.getMessage());
    }

    if (dto.isLogoutAllSessions()) {
      authSessionRepository.revokeAll(userId);
      refreshTokenRepository.revokeAllByUser(userId);
    }

    user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
    user.setPasswordChanged(true);
    passwordResetTokenRepository.invalidateAll(userId);
    auditLogService.changePassword();
  }

  @Override
  @Transactional
  public void uploadAvatar(UUID userId, MultipartFile avatarFile) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, UserMessage.NOT_FOUND.getMessage()));
    String avatarUrl = cloudinaryService.uploadImage(avatarFile, user.getId());
    user.setAvatarUrl(avatarUrl);
  }

  @Override
  @Transactional
  public void forgotPassword(ForgotPasswordDTO dto) {
    var existing = userRepository.findByEmail(dto.getEmail().trim().toLowerCase(Locale.ROOT));
    if (existing.isEmpty()) return;
    User user = userRepository.findByIdForUpdate(existing.get().getId()).orElse(null);
    if (user == null || !user.isActive()) return;
    Instant now = Instant.now();
    var latest = passwordResetTokenRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId());
    if (latest.isPresent()
        && latest
            .get()
            .getCreatedAt()
            .isAfter(now.minusMillis(passwordResetProperties.getResendCooldown()))) return;
    String token = AuthUtils.generateRefreshToken();
    passwordResetTokenRepository.invalidateAll(user.getId());
    passwordResetTokenRepository.save(
        PasswordResetToken.builder()
            .userId(user.getId())
            .tokenHash(AuthUtils.hashRefreshToken(token))
            .expiresAt(now.plusMillis(passwordResetProperties.getExpiration()))
            .build());
    try {
      mailService.sendPasswordResetEmail(user.getEmail(), token);
    } catch (MailException ex) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      log.warn("Password reset email could not be delivered");
    }
  }

  @Override
  @Transactional
  public void resetPassword(ResetPasswordDTO dto) {
    var reset =
        passwordResetTokenRepository
            .findByTokenHash(AuthUtils.hashRefreshToken(dto.getToken()))
            .orElseThrow(
                () ->
                    new AppException(
                        HttpStatus.UNAUTHORIZED, "Invalid or expired password reset token"));
    User user =
        userRepository
            .findByIdForUpdate(reset.getUserId())
            .orElseThrow(
                () ->
                    new AppException(
                        HttpStatus.UNAUTHORIZED, "Invalid or expired password reset token"));
    if (!user.isActive()
        || passwordResetTokenRepository.consume(reset.getId(), Instant.now()) == 0) {
      throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid or expired password reset token");
    }
    user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
    user.setPasswordChanged(true);
    passwordResetTokenRepository.invalidateAll(user.getId());
    authSessionRepository.revokeAll(user.getId());
    refreshTokenRepository.revokeAllByUser(user.getId());
    auditLogService.log("RESET_PASSWORD", "USER", user.getId(), "Password reset completed");
    mailService.sendPasswordChangedEmail(user.getEmail());
  }
}

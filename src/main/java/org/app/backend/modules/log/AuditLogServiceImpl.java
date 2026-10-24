package org.app.backend.modules.log;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.app.backend.modules.utils.AuthUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogServiceImpl implements AuditLogService {

  AuditLogRepository auditLogRepository;
  HttpServletRequest request;

  @Override
  @Transactional
  public void log(String action, String targetType, UUID targetId, String description) {
    auditLogRepository.save(
        AuditLog.builder()
            .userId(AuthUtils.currentUserId())
            .actionType(action)
            .targetType(targetType)
            .targetId(targetId)
            .description(description)
            .ipAddress(AuthUtils.resolveIp(request))
            .userAgent(AuthUtils.resolveUserAgent(request))
            .build());
  }

  @Override
  @Transactional
  public void created(String targetType, UUID targetId, String description) {
    log("CREATE", targetType, targetId, description);
  }

  @Override
  @Transactional
  public void updated(String targetType, UUID targetId) {
    log("UPDATE", targetType, targetId, "Updated " + targetType);
  }

  @Override
  @Transactional
  public void deleted(String targetType, UUID targetId) {
    log("DELETE", targetType, targetId, "Deleted " + targetType);
  }

  @Override
  @Transactional
  public void action(String action, String targetType, String description) {
    log(action, targetType, null, description);
  }

  @Override
  @Transactional
  public void signIn() {
    action("LOGIN", "AUTH", "User login");
  }

  @Override
  @Transactional
  public void signUp() {
    action("SIGNUP", "AUTH", "User registration");
  }

  @Override
  @Transactional
  public void signOut() {
    action("LOGOUT", "AUTH", "User logout");
  }

  @Override
  @Transactional
  public void signOutAll() {
    action("LOGOUT ALL", "AUTH", "User logout from all devices");
  }

  @Override
  @Transactional
  public void changePassword() {
    action("CHANGE_PASSWORD", "USER", "User changed password");
  }
}

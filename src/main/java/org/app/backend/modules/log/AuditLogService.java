package org.app.backend.modules.log;

import java.util.UUID;

public interface AuditLogService {
  void log(String action, String targetType, UUID targetId, String description);

  void created(String targetType, UUID targetId, String description);

  void updated(String targetType, UUID targetId);

  void deleted(String targetType, UUID targetId);

  void action(String action, String targetType, String description);

  void signIn();

  void signUp();

  void signOut();

  void signOutAll();

  void changePassword();
}

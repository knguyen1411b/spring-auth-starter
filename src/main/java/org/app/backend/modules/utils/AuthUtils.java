package org.app.backend.modules.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.app.backend.modules.user.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthUtils {
  public static String generateRefreshToken() {
    byte[] randomBytes = new byte[64];
    new SecureRandom().nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  public static String hashRefreshToken(String refreshToken) {
    return DigestUtils.sha256Hex(refreshToken);
  }

  public static String resolveIp(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  public static String resolveUserAgent(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    return userAgent != null ? userAgent : "unknown";
  }

  public static UUID currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
      return null;
    }
    return user.getId();
  }

  public static String resolveCaller() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return "anonymous";
    }
    if (auth.getPrincipal() instanceof CustomUserDetails user) {
      return user.getUsername();
    }
    return "anonymous";
  }
}

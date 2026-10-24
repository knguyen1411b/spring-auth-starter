package org.app.backend.modules.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.app.backend.common.constants.JwtProperties;
import org.app.backend.modules.auth.token.AuthSessionRepository;
import org.app.backend.modules.user.CustomUserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class JwtService {
  JwtProperties jwtProperties;
  AuthSessionRepository authSessionRepository;

  SecretKey getSignKey() {
    return Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(UUID id, UUID sessionId) {
    return Jwts.builder()
        .setSubject(id.toString())
        .claim("sid", sessionId.toString())
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(
            new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiration()))
        .signWith(getSignKey(), SignatureAlgorithm.HS512)
        .compact();
  }

  public String extractId(String token) {
    return extractAllClaims(token).getSubject();
  }

  public boolean isTokenValid(String token, CustomUserDetails userDetails) {
    try {
      final UUID id = UUID.fromString(extractId(token));
      String sessionClaim = extractAllClaims(token).get("sid", String.class);
      if (sessionClaim == null) return false;
      UUID sessionId = UUID.fromString(sessionClaim);
      return authSessionRepository.existsByIdAndUserIdAndRevokedAtIsNullAndExpiresAtAfter(
              sessionId, id, Instant.now())
          && id.equals(userDetails.getId())
          && userDetails.isActive()
          && !userDetails.isLocked()
          && !isTokenExpired(token);
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  boolean isTokenExpired(String token) {
    return extractAllClaims(token).getExpiration().before(new Date());
  }

  Claims extractAllClaims(String token) {
    return Jwts.parserBuilder().setSigningKey(getSignKey()).build().parseClaimsJws(token).getBody();
  }
}

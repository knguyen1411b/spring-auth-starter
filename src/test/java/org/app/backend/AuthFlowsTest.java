package org.app.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthFlowsTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("application.admin.enabled", () -> true);
    registry.add("application.admin.username", () -> "testadmin");
    registry.add("application.admin.password", () -> "Test-admin-123!");
    registry.add("application.admin.email", () -> "admin@example.test");
    registry.add("application.admin.full-name", () -> "Test Admin");
    registry.add(
        "application.security.jwt.secret-key",
        () -> "test-key-64-characters-minimum-abcdefghijklmnopqrstuvwxyz-0123456789");
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
  @MockitoBean JavaMailSender smtp;
  final Map<String, String> mail = new ConcurrentHashMap<>();
  static final String PASSWORD = "Example-password-123!";

  @BeforeEach
  void captureEmail() {
    org.mockito.Mockito.doAnswer(
            invocation -> {
              SimpleMailMessage message = invocation.getArgument(0);
              mail.put(message.getTo()[0], message.getText());
              return null;
            })
        .when(smtp)
        .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
  }

  JsonNode authPost(String path, Object body, int expected) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/v1/auth" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(body)))
            .andExpect(status().is(expected))
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  String register(String username) throws Exception {
    String email = username + "@example.test";
    authPost(
        "/sign-up",
        Map.of("username", username, "password", PASSWORD, "email", email, "fullName", "Test User"),
        201);
    var matcher =
        java.util.regex.Pattern.compile("http://localhost:8080/activate\\?token=([A-Za-z0-9_-]+)")
            .matcher(mail.get(email));
    assertThat(matcher.find()).as("email contains an activation URL").isTrue();
    return matcher.group(1);
  }

  JsonNode signIn(String username, String password, String device) throws Exception {
    return authPost(
            "/sign-in", Map.of("username", username, "password", password, "deviceId", device), 200)
        .get("data");
  }

  JsonNode user(String username) throws Exception {
    authPost("/activate", Map.of("token", register(username)), 200);
    return signIn(username, PASSWORD, "device-1");
  }

  String bearer(JsonNode tokens) {
    return "Bearer " + tokens.get("accessToken").asText();
  }

  JsonNode authorizedGet(String path, String bearer) throws Exception {
    return json.readTree(
        mvc.perform(get(path).header("Authorization", bearer))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  void refresh(JsonNode tokens, String device, int expected) throws Exception {
    authPost(
        "/refresh",
        Map.of("refreshToken", tokens.get("refreshToken").asText(), "deviceId", device),
        expected);
  }

  @Test
  void registrationRequiresActivationAndTokenCannotBeReused() throws Exception {
    String token = register("activation_user");
    authPost(
        "/sign-in",
        Map.of("username", "activation_user", "password", PASSWORD, "deviceId", "device-1"),
        403);
    mvc.perform(get("/activate").param("token", token))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString(token)))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("[[${token}]]"))));
    authPost("/activate", Map.of("token", token), 200);
    signIn("activation_user", PASSWORD, "device-1");
    authPost("/activate", Map.of("token", token), 401);
    authPost("/activate", Map.of("token", "invalid"), 401);
  }

  @Test
  void expiredActivationTokenCannotActivateAccount() throws Exception {
    String token = register("expired_activation");
    // Time-based fixture avoids waiting 24 hours.
    jdbc.update(
        "update tbl_verification_tokens set expires_at=now()-interval '1 second' where user_id=(select id from tbl_users where username=?)",
        "expired_activation");
    authPost("/activate", Map.of("token", token), 401);
  }

  @Test
  void invalidCredentialsHaveTheSamePublicResponse() throws Exception {
    JsonNode missing =
        authPost(
            "/sign-in",
            Map.of("username", "missing-user", "password", PASSWORD, "deviceId", "d"),
            401);
    JsonNode wrong =
        authPost(
            "/sign-in",
            Map.of("username", "testadmin", "password", "Wrong-password-123!", "deviceId", "d"),
            401);
    assertThat(missing).isEqualTo(wrong);
  }

  @Test
  void refreshRotatesTokensAndRejectsOldOrWrongDeviceTokens() throws Exception {
    JsonNode original = user("rotation_user");
    refresh(original, "wrong-device", 401);
    JsonNode rotated =
        authPost(
                "/refresh",
                Map.of(
                    "refreshToken", original.get("refreshToken").asText(), "deviceId", "device-1"),
                200)
            .get("data");
    assertThat(rotated.get("refreshToken").asText())
        .isNotEqualTo(original.get("refreshToken").asText());
    refresh(original, "device-1", 401);
    refresh(rotated, "device-1", 200);
  }

  @Test
  void onlyOneConcurrentRefreshCanSucceed() throws Exception {
    JsonNode original = user("concurrent_user");
    byte[] body =
        json.writeValueAsBytes(
            Map.of("refreshToken", original.get("refreshToken").asText(), "deviceId", "device-1"));
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> request =
          () -> {
            start.await();
            return mvc.perform(
                    post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var first = pool.submit(request);
      var second = pool.submit(request);
      start.countDown();
      assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 401);
    }
  }

  @Test
  void expiredRefreshTokenIsRejected() throws Exception {
    JsonNode tokens = user("expired_refresh");
    jdbc.update(
        "update tbl_refresh_tokens set expires_at=now()-interval '1 second' where user_id=(select id from tbl_users where username=?)",
        "expired_refresh");
    refresh(tokens, "device-1", 401);
  }

  @Test
  void logoutRevokesOnlySelectedDeviceThenAllRemainingSessions() throws Exception {
    JsonNode first = user("logout_user");
    JsonNode second = signIn("logout_user", PASSWORD, "device-2");
    mvc.perform(delete("/api/v1/auth/sessions/device-1").header("Authorization", bearer(first)))
        .andExpect(status().isOk());
    refresh(first, "device-1", 401);
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(first)))
        .andExpect(status().isUnauthorized());
    authorizedGet("/api/v1/auth/profile", bearer(second));
    JsonNode rotated =
        authPost(
                "/refresh",
                Map.of("refreshToken", second.get("refreshToken").asText(), "deviceId", "device-2"),
                200)
            .get("data");
    mvc.perform(delete("/api/v1/auth/sessions").header("Authorization", bearer(rotated)))
        .andExpect(status().isOk());
    refresh(rotated, "device-2", 401);
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(rotated)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void changingPasswordCanRevokeAllDevicesIncludingTheCurrentOne() throws Exception {
    JsonNode first = user("password_user");
    JsonNode second = signIn("password_user", PASSWORD, "device-2");
    String newPassword = "Changed-password-123!";
    mvc.perform(
            post("/api/v1/auth/change-password")
                .header("Authorization", bearer(first))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsBytes(
                        Map.of(
                            "currentPassword",
                            PASSWORD,
                            "newPassword",
                            newPassword,
                            "logoutAllSessions",
                            true))))
        .andExpect(status().isOk());
    refresh(first, "device-1", 401);
    refresh(second, "device-2", 401);
    authPost(
        "/sign-in",
        Map.of("username", "password_user", "password", PASSWORD, "deviceId", "d"),
        401);
    signIn("password_user", newPassword, "device-1");
  }

  @Test
  void accountStatusChangesTakeEffectForAccessAndRefresh() throws Exception {
    JsonNode tokens = user("status_user");
    String id =
        authorizedGet("/api/v1/auth/profile", bearer(tokens)).get("data").get("id").asText();
    String admin = bearer(signIn("testadmin", "Test-admin-123!", "status-admin"));
    mvc.perform(
            patch("/api/v1/users/" + id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"locked\":true}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(tokens)))
        .andExpect(status().isUnauthorized());
    refresh(tokens, "device-1", 401);
    mvc.perform(
            patch("/api/v1/users/" + id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"locked\":false,\"active\":false}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(tokens)))
        .andExpect(status().isUnauthorized());
    refresh(tokens, "device-1", 401);
  }

  @Test
  void roleChangesApplyToExistingAccessToken() throws Exception {
    JsonNode tokens = user("permission_user");
    String id =
        authorizedGet("/api/v1/auth/profile", bearer(tokens)).get("data").get("id").asText();
    mvc.perform(get("/api/v1/users").header("Authorization", bearer(tokens)))
        .andExpect(status().isForbidden());
    String admin = bearer(signIn("testadmin", "Test-admin-123!", "permission-admin"));
    String permissionId =
        authorizedGet("/api/v1/permissions?q=user:read", admin)
            .get("data")
            .get(0)
            .get("id")
            .asText();
    mvc.perform(
            post("/api/v1/roles")
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsBytes(
                        Map.of(
                            "code",
                            "test-reader",
                            "name",
                            "Reader",
                            "permissionIds",
                            List.of(permissionId)))))
        .andExpect(status().isCreated());
    String roleId =
        authorizedGet("/api/v1/roles?q=test-reader", admin).get("data").get(0).get("id").asText();
    mvc.perform(
            patch("/api/v1/users/" + id)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("roleIds", List.of(roleId)))))
        .andExpect(status().isOk());
    authorizedGet("/api/v1/users", bearer(tokens));
    mvc.perform(
            put("/api/v1/roles/" + roleId)
                .header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsBytes(Map.of("name", "Reader", "permissionIds", List.of()))))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/users").header("Authorization", bearer(tokens)))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidInputAndUnknownFieldsReturnBadRequest() throws Exception {
    authPost(
        "/sign-up",
        Map.of(
            "username",
            "invalid_user",
            "password",
            "short",
            "email",
            "invalid-email",
            "fullName",
            "User"),
        400);
    mvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest());
    authPost(
        "/sign-in",
        Map.of("username", "testadmin", "password", PASSWORD, "deviceId", "d", "extra", true),
        400);
  }

  @Test
  void bcryptPasswordCannotExceed72Utf8Bytes() throws Exception {
    authPost(
        "/sign-up",
        Map.of(
            "username",
            "unicode_password",
            "password",
            "界".repeat(30),
            "email",
            "utf8@example.test",
            "fullName",
            "User"),
        400);
  }

  @Test
  void missingOrInvalidBearerTokenIsUnauthorized() throws Exception {
    mvc.perform(get("/api/v1/auth/profile")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void allowedCorsPreflightSucceeds() throws Exception {
    mvc.perform(
            options("/api/v1/auth/profile")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
  }

  @Test
  void healthIsPublicAndDoesNotExposeDetails() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").doesNotExist());
  }

  @Test
  void paginationIsBoundedAndUnsupportedSortReturnsBadRequest() throws Exception {
    String admin = bearer(signIn("testadmin", "Test-admin-123!", "pagination-admin"));
    assertThat(authorizedGet("/api/v1/users?size=10000", admin).get("meta").get("size").asInt())
        .isEqualTo(100);
    mvc.perform(get("/api/v1/users?sort=passwordHash").header("Authorization", admin))
        .andExpect(status().isBadRequest());
  }

  @Test
  void overlongPasswordIsRejectedWithoutServerError() throws Exception {
    authPost(
        "/sign-in",
        Map.of("username", "testadmin", "password", "界".repeat(30), "deviceId", "d"),
        401);
    JsonNode tokens = user("long_password_user");
    mvc.perform(
            post("/api/v1/auth/change-password")
                .header("Authorization", bearer(tokens))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsBytes(
                        Map.of("currentPassword", "界".repeat(30), "newPassword", PASSWORD))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void passwordResetChangesPasswordAndImmediatelyRevokesEverySession() throws Exception {
    JsonNode first = user("reset_user");
    JsonNode second = signIn("reset_user", PASSWORD, "device-2");
    JsonNode known = authPost("/forgot-password", Map.of("email", "reset_user@example.test"), 200);
    JsonNode unknown = authPost("/forgot-password", Map.of("email", "missing@example.test"), 200);
    assertThat(known).isEqualTo(unknown);
    var matcher =
        java.util.regex.Pattern.compile("/reset-password#token=([A-Za-z0-9_-]+)")
            .matcher(mail.get("reset_user@example.test"));
    assertThat(matcher.find()).isTrue();
    String token = matcher.group(1);
    String changed = "Reset-password-123!";
    authPost("/reset-password", Map.of("token", token, "newPassword", changed), 200);
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(first)))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(second)))
        .andExpect(status().isUnauthorized());
    refresh(first, "device-1", 401);
    refresh(second, "device-2", 401);
    authPost(
        "/sign-in", Map.of("username", "reset_user", "password", PASSWORD, "deviceId", "d"), 401);
    signIn("reset_user", changed, "device-1");
    authPost("/reset-password", Map.of("token", token, "newPassword", PASSWORD), 401);
  }

  String resetLinkToken(String username) throws Exception {
    authPost("/forgot-password", Map.of("email", username + "@example.test"), 200);
    var matcher =
        java.util.regex.Pattern.compile("/reset-password#token=([A-Za-z0-9_-]+)")
            .matcher(mail.get(username + "@example.test"));
    assertThat(matcher.find()).isTrue();
    return matcher.group(1);
  }

  @Test
  void refreshKeepsAccessTokensValidUntilTheirSessionIsRevoked() throws Exception {
    JsonNode first = user("stable_session");
    JsonNode rotated =
        authPost(
                "/refresh",
                Map.of("refreshToken", first.get("refreshToken").asText(), "deviceId", "device-1"),
                200)
            .get("data");
    authorizedGet("/api/v1/auth/profile", bearer(first));
    authorizedGet("/api/v1/auth/profile", bearer(rotated));
    mvc.perform(delete("/api/v1/auth/sessions/device-1").header("Authorization", bearer(first)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(first)))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(rotated)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void signingInAgainOnTheSameDeviceRevokesPreviousAccessToken() throws Exception {
    JsonNode first = user("replace_session");
    JsonNode replacement = signIn("replace_session", PASSWORD, "device-1");
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(first)))
        .andExpect(status().isUnauthorized());
    refresh(first, "device-1", 401);
    authorizedGet("/api/v1/auth/profile", bearer(replacement));
  }

  @Test
  void passwordChangeRevokesAccessTokensAndInvalidatesRecoveryLinks() throws Exception {
    JsonNode tokens = user("change_recovery");
    String token = resetLinkToken("change_recovery");
    mvc.perform(
            post("/api/v1/auth/change-password")
                .header("Authorization", bearer(tokens))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsBytes(
                        Map.of(
                            "currentPassword",
                            PASSWORD,
                            "newPassword",
                            "Changed-password-123!",
                            "logoutAllSessions",
                            true))))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(tokens)))
        .andExpect(status().isUnauthorized());
    authPost("/reset-password", Map.of("token", token, "newPassword", PASSWORD), 401);
  }

  @Test
  void expiredResetTokensAndActivationTokensCannotResetPasswords() throws Exception {
    user("expired_reset_user");
    String token = resetLinkToken("expired_reset_user");
    jdbc.update(
        "update tbl_password_reset_tokens set expires_at=now()-interval '1 second' where user_id=(select id from tbl_users where username=?)",
        "expired_reset_user");
    authPost("/reset-password", Map.of("token", token, "newPassword", PASSWORD), 401);
    authPost("/reset-password", Map.of("token", "invalid", "newPassword", PASSWORD), 401);
    String activation = register("activation_reset");
    authPost("/reset-password", Map.of("token", activation, "newPassword", PASSWORD), 401);
  }

  @Test
  void invalidNewPasswordDoesNotConsumeResetToken() throws Exception {
    user("reset_validation");
    String token = resetLinkToken("reset_validation");
    authPost("/reset-password", Map.of("token", token, "newPassword", "short"), 400);
    authPost("/reset-password", Map.of("token", token, "newPassword", "界".repeat(30)), 400);
    authPost("/reset-password", Map.of("token", token, "newPassword", PASSWORD), 200);
  }

  @Test
  void resetEmailHasCooldownAndNewLinkInvalidatesPreviousLink() throws Exception {
    user("reset_cooldown");
    org.mockito.Mockito.clearInvocations(smtp);
    String first = resetLinkToken("reset_cooldown");
    assertThat(resetLinkToken("reset_cooldown")).isEqualTo(first);
    org.mockito.Mockito.verify(smtp, org.mockito.Mockito.times(1))
        .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    jdbc.update(
        "update tbl_password_reset_tokens set created_at=now()-interval '2 minutes' where user_id=(select id from tbl_users where username=?)",
        "reset_cooldown");
    String second = resetLinkToken("reset_cooldown");
    assertThat(second).isNotEqualTo(first);
    authPost("/reset-password", Map.of("token", first, "newPassword", PASSWORD), 401);
    authPost("/reset-password", Map.of("token", second, "newPassword", PASSWORD), 200);
  }

  @Test
  void onlyOneConcurrentResetCanSucceed() throws Exception {
    user("concurrent_reset");
    String token = resetLinkToken("concurrent_reset");
    byte[] body =
        json.writeValueAsBytes(Map.of("token", token, "newPassword", "Concurrent-password-123!"));
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> request =
          () -> {
            start.await();
            return mvc.perform(
                    post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      var first = pool.submit(request);
      var second = pool.submit(request);
      start.countDown();
      assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 401);
    }
    signIn("concurrent_reset", "Concurrent-password-123!", "device-1");
  }

  @Test
  void concurrentRefreshCannotRestoreALoggedOutSession() throws Exception {
    JsonNode tokens = user("logout_refresh_race");
    var start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var refreshed =
          pool.submit(
              () -> {
                start.await();
                return mvc.perform(
                        post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                json.writeValueAsBytes(
                                    Map.of(
                                        "refreshToken",
                                        tokens.get("refreshToken").asText(),
                                        "deviceId",
                                        "device-1"))))
                    .andReturn()
                    .getResponse();
              });
      var logout =
          pool.submit(
              () -> {
                start.await();
                return mvc.perform(
                        delete("/api/v1/auth/sessions").header("Authorization", bearer(tokens)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      start.countDown();
      assertThat(logout.get(15, TimeUnit.SECONDS)).isEqualTo(200);
      var response = refreshed.get(15, TimeUnit.SECONDS);
      assertThat(response.getStatus()).isIn(200, 401);
      if (response.getStatus() == 200) {
        JsonNode rotated = json.readTree(response.getContentAsString()).get("data");
        mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(rotated)))
            .andExpect(status().isUnauthorized());
        refresh(rotated, "device-1", 401);
      }
    }
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(tokens)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void failedResetMailDoesNotRevealAccountOrBlockRetry() throws Exception {
    user("reset_mail_failure");
    org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("SMTP unavailable"))
        .when(smtp)
        .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    JsonNode known =
        authPost("/forgot-password", Map.of("email", "reset_mail_failure@example.test"), 200);
    assertThat(authPost("/forgot-password", Map.of("email", "missing@example.test"), 200))
        .isEqualTo(known);
    captureEmail();
    String token = resetLinkToken("reset_mail_failure");
    authPost("/reset-password", Map.of("token", token, "newPassword", PASSWORD), 200);
  }

  @Test
  void legacyAccessTokenWithoutSessionIsRejected() throws Exception {
    JsonNode tokens = user("legacy_access");
    String id =
        authorizedGet("/api/v1/auth/profile", bearer(tokens)).get("data").get("id").asText();
    var key =
        io.jsonwebtoken.security.Keys.hmacShaKeyFor(
            "test-key-64-characters-minimum-abcdefghijklmnopqrstuvwxyz-0123456789"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String legacy =
        io.jsonwebtoken.Jwts.builder()
            .setSubject(id)
            .setExpiration(new java.util.Date(System.currentTimeMillis() + 60000))
            .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS512)
            .compact();
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", "Bearer " + legacy))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void resetPageIsPublicAndDoesNotCacheTokens() throws Exception {
    mvc.perform(get("/reset-password"))
        .andExpect(status().isOk())
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"reset-form\"")));
  }

  @Test
  void unlockingAnAccountDoesNotRestoreRevokedTokens() throws Exception {
    JsonNode tokens = user("unlock_session");
    String id =
        authorizedGet("/api/v1/auth/profile", bearer(tokens)).get("data").get("id").asText();
    String admin = bearer(signIn("testadmin", "Test-admin-123!", "unlock-admin"));
    for (boolean locked : List.of(true, false)) {
      mvc.perform(
              patch("/api/v1/users/" + id)
                  .header("Authorization", admin)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsBytes(Map.of("locked", locked))))
          .andExpect(status().isOk());
    }
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(tokens)))
        .andExpect(status().isUnauthorized());
    refresh(tokens, "device-1", 401);
    signIn("unlock_session", PASSWORD, "device-1");
  }

  @Test
  void expiredSessionRejectsAccessAndRefreshAndIsNotListed() throws Exception {
    JsonNode first = user("session_expiry");
    JsonNode second = signIn("session_expiry", PASSWORD, "device-2");
    jdbc.update(
        "update tbl_auth_sessions set expires_at=now()-interval '1 second' where user_id=(select id from tbl_users where username=?) and device_id='device-1'",
        "session_expiry");
    mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer(first)))
        .andExpect(status().isUnauthorized());
    refresh(first, "device-1", 401);
    JsonNode sessions = authorizedGet("/api/v1/auth/sessions", bearer(second)).get("data");
    assertThat(sessions.size()).isEqualTo(1);
    assertThat(sessions.get(0).get("deviceId").asText()).isEqualTo("device-2");
  }
}

package org.app.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthApiTest {
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
    registry.add("application.security.jwt.access-token-expiration", () -> 900000);
    registry.add("application.security.jwt.refresh-token-expiration", () -> 604800000);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  JsonNode authPost(String path, Object body, int status) throws Exception {
    return json.readTree(
        mvc.perform(
                post("/api/v1/auth" + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(body)))
            .andExpect(status().is(status))
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  @Test
  void lockedAccountCannotKeepUsingAccessToken() throws Exception {
    JsonNode tokens =
        authPost(
                "/sign-in",
                Map.of(
                    "username",
                    "testadmin",
                    "password",
                    "Test-admin-123!",
                    "deviceId",
                    "lock-test",
                    "deviceName",
                    "JUnit"),
                200)
            .get("data");
    String bearer = "Bearer " + tokens.get("accessToken").asText();
    JsonNode profile =
        json.readTree(
                mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("data");
    mvc.perform(
            patch("/api/v1/users/" + profile.get("id").asText())
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Test Admin\",\"active\":true,\"locked\":true}"))
        .andExpect(status().isOk());
    try {
      mvc.perform(get("/api/v1/auth/profile").header("Authorization", bearer))
          .andExpect(status().isUnauthorized());
    } finally {
      // Reset the fixture even when reproducing the original bug.
      jdbc.update("update tbl_users set is_locked=false where username='testadmin'");
    }
  }

  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

  @Test
  void sessionsCanBeListedAfterSignIn() throws Exception {
    JsonNode tokens =
        authPost(
                "/sign-in",
                Map.of(
                    "username",
                    "testadmin",
                    "password",
                    "Test-admin-123!",
                    "deviceId",
                    "sessions-test"),
                200)
            .get("data");
    mvc.perform(
            get("/api/v1/auth/sessions")
                .header("Authorization", "Bearer " + tokens.get("accessToken").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[?(@.deviceId == 'sessions-test')]").isNotEmpty());
  }
}

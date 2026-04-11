package com.example.todo;

import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProfileControllerTest extends BaseIntegrationTest {

    private static final String EMAIL    = "profile_test@example.com";
    private static final String USERNAME = "profile_user";
    private static final String PASSWORD = "Profile123!";

    private String token;

    @BeforeAll
    void setup() {
        token = registerAndLogin(USERNAME, EMAIL, PASSWORD);
    }

    @AfterAll
    void teardown() {
        cleanupUser(EMAIL);
        cleanupUser("profile_updated@example.com");
    }

    // ── GET /api/profile ──────────────────────────────────────────────────────

    @Test @Order(1)
    void getProfile_authenticated_returnsUser() {
        ResponseEntity<Map<String, Object>> resp = getMap("/api/profile", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKey("email");
        assertThat(resp.getBody().get("email")).isEqualTo(EMAIL);
        assertThat(resp.getBody().get("username")).isEqualTo(USERNAME);
        // password must never be exposed
        assertThat(resp.getBody()).doesNotContainKey("password");
    }

    @Test @Order(2)
    void getProfile_noToken_returns401() {
        ResponseEntity<String> resp = restTemplate.getForEntity(url("/api/profile"), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── PUT /api/profile ──────────────────────────────────────────────────────

    @Test @Order(3)
    void updateProfile_changeUsername_success() {
        ResponseEntity<Map<String, Object>> resp =
                putMap("/api/profile", Map.of("username", "profile_renamed"), token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("username")).isEqualTo("profile_renamed");

        // restore
        putMap("/api/profile", Map.of("username", USERNAME), token);
    }

    @Test @Order(4)
    void updateProfile_changeEmail_success() {
        ResponseEntity<Map<String, Object>> resp =
                putMap("/api/profile", Map.of("email", "profile_updated@example.com"), token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("email")).isEqualTo("profile_updated@example.com");

        // restore (email changed so re-login with new email first)
        token = login("profile_updated@example.com", PASSWORD);
        putMap("/api/profile", Map.of("email", EMAIL), token);
        token = login(EMAIL, PASSWORD);
    }

    @Test @Order(5)
    void updateProfile_duplicateEmail_returns400() {
        // admin@gmail.com is seeded by AdminSeeder
        ResponseEntity<String> resp = put("/api/profile", Map.of("email", "admin@gmail.com"), token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Email is already in use");
    }

    @Test @Order(6)
    void updateProfile_duplicateUsername_returns400() {
        ResponseEntity<String> resp = put("/api/profile", Map.of("username", "admin"), token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Username is already taken");
    }

    @Test @Order(7)
    void updateProfile_invalidEmail_returns400() {
        ResponseEntity<String> resp = put("/api/profile", Map.of("email", "not-valid"), token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── PUT /api/profile/change-password ─────────────────────────────────────

    @Test @Order(8)
    void changePassword_success() {
        Map<String, String> body = Map.of("currentPassword", PASSWORD, "newPassword", "NewProfile99!");
        ResponseEntity<String> resp = put("/api/profile/change-password", body, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("changed successfully");

        // restore original password
        String newToken = login(EMAIL, "NewProfile99!");
        put("/api/profile/change-password",
                Map.of("currentPassword", "NewProfile99!", "newPassword", PASSWORD), newToken, String.class);
        token = login(EMAIL, PASSWORD);
    }

    @Test @Order(9)
    void changePassword_wrongCurrentPassword_returns400() {
        Map<String, String> body = Map.of("currentPassword", "WrongPass99!", "newPassword", "NewPass123!");
        ResponseEntity<String> resp = put("/api/profile/change-password", body, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Current password is incorrect");
    }

    @Test @Order(10)
    void changePassword_shortNewPassword_returns400() {
        Map<String, String> body = Map.of("currentPassword", PASSWORD, "newPassword", "short");
        ResponseEntity<String> resp = put("/api/profile/change-password", body, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(11)
    void changePassword_noToken_returns401() {
        Map<String, String> body = Map.of("currentPassword", PASSWORD, "newPassword", "NewPass123!");
        ResponseEntity<String> resp = restTemplate.exchange(
                url("/api/profile/change-password"), HttpMethod.PUT,
                new HttpEntity<>(body, jsonHeaders()), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

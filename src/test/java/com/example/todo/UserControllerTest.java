package com.example.todo;

import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerTest extends BaseIntegrationTest {

    // Admin credentials seeded by AdminSeeder
    private static final String ADMIN_EMAIL    = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "F1ette20@";

    // A regular user for testing forbidden access
    private static final String USER_EMAIL    = "admin_test_user@example.com";
    private static final String USER_USERNAME = "admin_test_user";
    private static final String USER_PASSWORD = "User1234!";

    // A target user to create, update and delete
    private static final String TARGET_EMAIL    = "target_user@example.com";
    private static final String TARGET_USERNAME = "target_user";
    private static final String TARGET_PASSWORD = "Target123!";

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_MAP =
            new ParameterizedTypeReference<List<Map<String, Object>>>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<Map<String, Object>>() {};

    private String adminToken;
    private String userToken;
    private Long targetUserId;

    @BeforeAll
    void setup() {
        // Admin is seeded by AdminSeeder; just login
        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Regular (non-admin) user
        userToken = registerAndLogin(USER_USERNAME, USER_EMAIL, USER_PASSWORD);

        // Target user for CRUD tests
        cleanupUser(TARGET_EMAIL);
        signup(TARGET_USERNAME, TARGET_EMAIL, TARGET_PASSWORD);
        targetUserId = userRepository.findByEmail(TARGET_EMAIL).orElseThrow().getId();
    }

    @AfterAll
    void teardown() {
        cleanupUser(USER_EMAIL);
        cleanupUser(TARGET_EMAIL);
    }

    // ── GET /api/users ────────────────────────────────────────────────────────

    @Test @Order(1)
    void getAllUsers_admin_success() {
        ResponseEntity<List<Map<String, Object>>> resp = get("/api/users", adminToken, LIST_MAP);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotEmpty();
    }

    @Test @Order(2)
    void getAllUsers_nonAdmin_returns403() {
        ResponseEntity<String> resp = get("/api/users", userToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test @Order(3)
    void getAllUsers_noToken_returns401() {
        ResponseEntity<String> resp = restTemplate.getForEntity(url("/api/users"), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── GET /api/users/{id} ───────────────────────────────────────────────────

    @Test @Order(4)
    void getUserById_admin_success() {
        ResponseEntity<Map<String, Object>> resp = get("/api/users/" + targetUserId, adminToken, MAP_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("email")).isEqualTo(TARGET_EMAIL);
        assertThat(resp.getBody()).doesNotContainKey("password");
    }

    @Test @Order(5)
    void getUserById_notFound_returns404() {
        ResponseEntity<String> resp = get("/api/users/999999", adminToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @Order(6)
    void getUserById_nonAdmin_returns403() {
        ResponseEntity<String> resp = get("/api/users/" + targetUserId, userToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── PUT /api/users/{id} ───────────────────────────────────────────────────

    @Test @Order(7)
    void updateUser_changeUsername_success() {
        Map<String, String> body = Map.of("username", "target_renamed");
        ResponseEntity<Map<String, Object>> resp = put("/api/users/" + targetUserId, body, adminToken, MAP_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("username")).isEqualTo("target_renamed");

        // restore
        put("/api/users/" + targetUserId, Map.of("username", TARGET_USERNAME), adminToken, MAP_TYPE);
    }

    @Test @Order(8)
    void updateUser_duplicateEmail_returns400() {
        // Try to set target's email to admin's email
        Map<String, String> body = Map.of("email", ADMIN_EMAIL);
        ResponseEntity<String> resp = put("/api/users/" + targetUserId, body, adminToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Email is already in use");
    }

    @Test @Order(9)
    void updateUser_notFound_returns404() {
        Map<String, String> body = Map.of("username", "ghost");
        ResponseEntity<String> resp = put("/api/users/999999", body, adminToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @Order(10)
    void updateUser_nonAdmin_returns403() {
        Map<String, String> body = Map.of("username", "hacked");
        ResponseEntity<String> resp = put("/api/users/" + targetUserId, body, userToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── PATCH /api/users/{id}/role ────────────────────────────────────────────

    @Test @Order(11)
    void updateRole_toAdmin_success() {
        Map<String, String> body = Map.of("role", "ADMIN");
        ResponseEntity<Map<String, Object>> resp =
                patch("/api/users/" + targetUserId + "/role", body, adminToken, MAP_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("role")).isEqualTo("ADMIN");

        // restore to USER
        patch("/api/users/" + targetUserId + "/role", Map.of("role", "USER"), adminToken, MAP_TYPE);
    }

    @Test @Order(12)
    void updateRole_blankRole_returns400() {
        Map<String, String> body = Map.of("role", "");
        ResponseEntity<String> resp =
                patch("/api/users/" + targetUserId + "/role", body, adminToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(13)
    void updateRole_notFound_returns404() {
        Map<String, String> body = Map.of("role", "USER");
        ResponseEntity<String> resp = patch("/api/users/999999/role", body, adminToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @Order(14)
    void updateRole_nonAdmin_returns403() {
        Map<String, String> body = Map.of("role", "ADMIN");
        ResponseEntity<String> resp =
                patch("/api/users/" + targetUserId + "/role", body, userToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── GET /api/users/stats ──────────────────────────────────────────────────

    @Test @Order(15)
    void getStats_admin_success() {
        ResponseEntity<Map<String, Object>> resp = get("/api/users/stats", adminToken, MAP_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKeys("totalUsers", "admins", "regularUsers", "totalTodos");
        assertThat(((Number) resp.getBody().get("totalUsers")).longValue()).isGreaterThanOrEqualTo(1);
    }

    @Test @Order(16)
    void getStats_nonAdmin_returns403() {
        ResponseEntity<String> resp = get("/api/users/stats", userToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── DELETE /api/users/{id} ────────────────────────────────────────────────

    @Test @Order(17)
    void deleteUser_success() {
        // Create a temporary user specifically for deletion
        String tmpEmail = "tmp_delete@example.com";
        cleanupUser(tmpEmail);
        signup("tmp_delete_user", tmpEmail, "TmpDelete1!");
        Long tmpId = userRepository.findByEmail(tmpEmail).orElseThrow().getId();

        ResponseEntity<String> resp = delete("/api/users/" + tmpId, adminToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("deleted successfully");

        // Confirm gone
        assertThat(userRepository.findByEmail(tmpEmail)).isEmpty();
    }

    @Test @Order(18)
    void deleteUser_notFound_returns404() {
        ResponseEntity<String> resp = delete("/api/users/999999", adminToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @Order(19)
    void deleteUser_nonAdmin_returns403() {
        ResponseEntity<String> resp = delete("/api/users/" + targetUserId, userToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}

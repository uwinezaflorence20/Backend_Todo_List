package com.example.todo;

import com.example.todo.model.PasswordResetToken;
import com.example.todo.model.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerTest extends BaseIntegrationTest {

    private static final String EMAIL    = "auth_test@example.com";
    private static final String USERNAME = "auth_test_user";
    private static final String PASSWORD = "Test1234!";

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;
    private TransactionTemplate tx() { return new TransactionTemplate(transactionManager); }

    @BeforeAll
    void setup() {
        cleanupUser(EMAIL);
    }

    @AfterAll
    void teardown() {
        cleanupUser(EMAIL);
    }

    // ── Signup ────────────────────────────────────────────────────────────────

    @Test @Order(1)
    void signup_success() {
        ResponseEntity<String> resp = signup(USERNAME, EMAIL, PASSWORD);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("registered successfully");
    }

    @Test @Order(2)
    void signup_duplicateEmail_returns400() {
        ResponseEntity<String> resp = signup("other_user", EMAIL, PASSWORD);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Email is already in use");
    }

    @Test @Order(3)
    void signup_duplicateUsername_returns400() {
        ResponseEntity<String> resp = signup(USERNAME, "other_" + EMAIL, PASSWORD);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Username is already taken");
    }

    @Test @Order(4)
    void signup_blankUsername_returns400() {
        Map<String, String> body = Map.of("username", "", "email", "blank@example.com", "password", PASSWORD);
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/auth/signup"), body, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(5)
    void signup_shortPassword_returns400() {
        Map<String, String> body = Map.of("username", "shortpw", "email", "short@example.com", "password", "abc");
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/auth/signup"), body, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(6)
    void signup_invalidEmail_returns400() {
        Map<String, String> body = Map.of("username", "bademail", "email", "not-an-email", "password", PASSWORD);
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/auth/signup"), body, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Signin ────────────────────────────────────────────────────────────────

    @Test @Order(7)
    void signin_success_returnsToken() {
        String token = login(EMAIL, PASSWORD);
        assertThat(token).isNotBlank();
    }

    @Test @Order(8)
    void signin_wrongPassword_returns401() {
        Map<String, String> body = Map.of("email", EMAIL, "password", "WrongPassword!");
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/auth/signin"), body, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @Order(9)
    void signin_unknownEmail_returns401() {
        Map<String, String> body = Map.of("email", "nobody@example.com", "password", PASSWORD);
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/auth/signin"), body, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @Order(10)
    void signin_invalidEmailFormat_returns400() {
        Map<String, String> body = Map.of("email", "not-an-email", "password", PASSWORD);
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/auth/signin"), body, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Forgot password ───────────────────────────────────────────────────────

    @Test @Order(11)
    void forgotPassword_knownEmail_returns200() {
        Map<String, String> body = Map.of("email", EMAIL);
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/auth/forgot-password"), body, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("reset link has been sent");
    }

    @Test @Order(12)
    void forgotPassword_unknownEmail_still200() {
        Map<String, String> body = Map.of("email", "nobody@example.com");
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/auth/forgot-password"), body, String.class);
        // Security best practice: always 200 regardless of whether email exists
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @Order(13)
    void forgotPassword_invalidEmail_returns400() {
        Map<String, String> body = Map.of("email", "not-an-email");
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/auth/forgot-password"), body, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Reset password ────────────────────────────────────────────────────────

    @Test @Order(14)
    void resetPassword_invalidToken_returns400() {
        Map<String, String> body = Map.of("token", "invalid-token-xyz", "newPassword", "NewPass123!");
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/auth/reset-password"), body, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("Invalid reset token");
    }

    @Test @Order(15)
    void resetPassword_validToken_success() {
        // Seed a valid token inside an explicit transaction
        tx().execute(status -> {
            User u = userRepository.findByEmail(EMAIL).orElseThrow();
            tokenRepository.deleteByUser(u);
            tokenRepository.save(new PasswordResetToken("valid-test-token", u));
            return null;
        });

        Map<String, String> body = Map.of("token", "valid-test-token", "newPassword", "NewPass456!");
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/auth/reset-password"), body, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("reset successfully");

        // Restore original password so later ordered tests can still login
        tx().execute(status -> {
            User u = userRepository.findByEmail(EMAIL).orElseThrow();
            u.setPassword(passwordEncoder.encode(PASSWORD));
            userRepository.save(u);
            return null;
        });
    }

    @Test @Order(16)
    void resetPassword_shortPassword_returns400() {
        Map<String, String> body = Map.of("token", "any-token", "newPassword", "short");
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/auth/reset-password"), body, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

package com.example.todo;

import com.example.todo.repository.PasswordResetTokenRepository;
import com.example.todo.repository.TodoRepository;
import com.example.todo.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Properties;

import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @MockBean
    protected JavaMailSender mailSender;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected TodoRepository todoRepository;

    @Autowired
    protected PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void configureTestRestTemplate() throws Exception {
        // Apache HttpClient 5 supports PATCH; Java's HttpURLConnection does not.
        restTemplate.getRestTemplate().setRequestFactory(
                new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault()));

        // Use a real MimeMessage backed by a no-op Session — Mockito cannot instrument
        // jakarta.mail classes on Java 25 via Byte Buddy.
        Session session = Session.getInstance(new Properties());
        MimeMessage realMessage = new MimeMessage(session);
        when(mailSender.createMimeMessage()).thenReturn(realMessage);
        // mailSender.send(...) is already a no-op because JavaMailSender is a @MockBean
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    /** Register a new user and return the raw response body string. */
    protected ResponseEntity<String> signup(String username, String email, String password) {
        Map<String, String> body = Map.of("username", username, "email", email, "password", password);
        return restTemplate.postForEntity(url("/api/auth/signup"), body, String.class);
    }

    /** Login and return the JWT token. Throws if login fails. */
    protected String login(String email, String password) {
        Map<String, String> body = Map.of("email", email, "password", password);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url("/api/auth/signin"), HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {});
        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new IllegalStateException("Login failed for " + email + ": " + response.getStatusCode());
        }
        return (String) response.getBody().get("token");
    }

    /** Plain JSON headers (no auth). */
    protected HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    /** JSON headers with Bearer auth token. */
    protected HttpHeaders authHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    // ── Class<T> convenience methods (for String, plain objects) ──────────────

    protected <T> ResponseEntity<T> get(String path, String token, Class<T> type) {
        return restTemplate.exchange(url(path), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), type);
    }

    protected <T> ResponseEntity<T> post(String path, Object body, String token, Class<T> type) {
        return restTemplate.exchange(url(path), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), type);
    }

    protected <T> ResponseEntity<T> put(String path, Object body, String token, Class<T> type) {
        return restTemplate.exchange(url(path), HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders(token)), type);
    }

    protected <T> ResponseEntity<T> patch(String path, Object body, String token, Class<T> type) {
        return restTemplate.exchange(url(path), HttpMethod.PATCH,
                new HttpEntity<>(body, authHeaders(token)), type);
    }

    protected <T> ResponseEntity<T> delete(String path, String token, Class<T> type) {
        return restTemplate.exchange(url(path), HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)), type);
    }

    // ── ParameterizedTypeReference overloads (for Map<K,V>, List<T>, etc.) ───

    protected <T> ResponseEntity<T> get(String path, String token, ParameterizedTypeReference<T> type) {
        return restTemplate.exchange(url(path), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), type);
    }

    protected <T> ResponseEntity<T> post(String path, Object body, String token, ParameterizedTypeReference<T> type) {
        return restTemplate.exchange(url(path), HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(token)), type);
    }

    protected <T> ResponseEntity<T> put(String path, Object body, String token, ParameterizedTypeReference<T> type) {
        return restTemplate.exchange(url(path), HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders(token)), type);
    }

    protected <T> ResponseEntity<T> patch(String path, Object body, String token, ParameterizedTypeReference<T> type) {
        return restTemplate.exchange(url(path), HttpMethod.PATCH,
                new HttpEntity<>(body, authHeaders(token)), type);
    }

    protected <T> ResponseEntity<T> delete(String path, String token, ParameterizedTypeReference<T> type) {
        return restTemplate.exchange(url(path), HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)), type);
    }

    // ── Shortcuts that always return Map<String, Object> ─────────────────────

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<Map<String, Object>>() {};

    protected ResponseEntity<Map<String, Object>> getMap(String path, String token) {
        return get(path, token, MAP_TYPE);
    }

    protected ResponseEntity<Map<String, Object>> postMap(String path, Object body, String token) {
        return post(path, body, token, MAP_TYPE);
    }

    protected ResponseEntity<Map<String, Object>> putMap(String path, Object body, String token) {
        return put(path, body, token, MAP_TYPE);
    }

    protected ResponseEntity<Map<String, Object>> patchMap(String path, Object body, String token) {
        return patch(path, body, token, MAP_TYPE);
    }

    // ── Lifecycle helpers ─────────────────────────────────────────────────────

    /** Remove a user and all associated data inside an explicit transaction.
     *  Safe to call if user does not exist. */
    protected void cleanupUser(String email) {
        new TransactionTemplate(transactionManager).execute(status -> {
            userRepository.findByEmail(email).ifPresent(user -> {
                tokenRepository.deleteByUser(user);
                todoRepository.deleteByUser(user);
                userRepository.delete(user);
            });
            return null;
        });
    }

    /** Register + login in one step; returns JWT. Deletes any existing user first. */
    protected String registerAndLogin(String username, String email, String password) {
        cleanupUser(email);
        signup(username, email, password);
        return login(email, password);
    }
}

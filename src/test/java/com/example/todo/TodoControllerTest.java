package com.example.todo;

import org.junit.jupiter.api.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TodoControllerTest extends BaseIntegrationTest {

    private static final String EMAIL    = "todo_test@example.com";
    private static final String USERNAME = "todo_user";
    private static final String PASSWORD = "Todo1234!";

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_MAP =
            new ParameterizedTypeReference<List<Map<String, Object>>>() {};

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<Map<String, Object>>() {};

    private String token;
    private Long todoId;   // shared across ordered tests

    @BeforeAll
    void setup() {
        token = registerAndLogin(USERNAME, EMAIL, PASSWORD);
    }

    @AfterAll
    void teardown() {
        cleanupUser(EMAIL);
    }

    // ── POST /api/todos ───────────────────────────────────────────────────────

    @Test @Order(1)
    void createTodo_success() {
        Map<String, Object> body = Map.of("title", "Buy groceries", "description", "Milk, eggs", "completed", false);
        ResponseEntity<Map<String, Object>> resp = post("/api/todos", body, token, MAP_TYPE);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("title")).isEqualTo("Buy groceries");
        assertThat(resp.getBody().get("completed")).isEqualTo(false);
        todoId = ((Number) resp.getBody().get("id")).longValue();
    }

    @Test @Order(2)
    void createTodo_blankTitle_returns400() {
        Map<String, Object> body = Map.of("title", "", "completed", false);
        ResponseEntity<String> resp = post("/api/todos", body, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(3)
    void createTodo_noToken_returns401() {
        Map<String, Object> body = Map.of("title", "Unauthorized todo");
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/todos"), body, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── GET /api/todos ────────────────────────────────────────────────────────

    @Test @Order(4)
    void getAllTodos_success() {
        ResponseEntity<List<Map<String, Object>>> resp = get("/api/todos", token, LIST_MAP);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotEmpty();
    }

    @Test @Order(5)
    void getAllTodos_filterCompleted_false() {
        ResponseEntity<List<Map<String, Object>>> resp = get("/api/todos?completed=false", token, LIST_MAP);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).allMatch(t -> Boolean.FALSE.equals(t.get("completed")));
    }

    @Test @Order(6)
    void getAllTodos_filterCompleted_true_empty() {
        // Nothing completed yet
        ResponseEntity<List<Map<String, Object>>> resp = get("/api/todos?completed=true", token, LIST_MAP);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }

    // ── GET /api/todos/stats ──────────────────────────────────────────────────

    @Test @Order(7)
    void getStats_success() {
        ResponseEntity<Map<String, Object>> resp = getMap("/api/todos/stats", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKeys("total", "completed", "pending");
        assertThat(((Number) resp.getBody().get("total")).longValue()).isGreaterThanOrEqualTo(1);
        assertThat(((Number) resp.getBody().get("pending")).longValue()).isGreaterThanOrEqualTo(1);
    }

    // ── GET /api/todos/{id} ───────────────────────────────────────────────────

    @Test @Order(8)
    void getTodoById_success() {
        ResponseEntity<Map<String, Object>> resp = getMap("/api/todos/" + todoId, token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("id")).isEqualTo(todoId.intValue());
    }

    @Test @Order(9)
    void getTodoById_notFound_returns404() {
        ResponseEntity<String> resp = get("/api/todos/999999", token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @Order(10)
    void getTodoById_otherUserTodo_returns403() {
        // Create a second user and try to access first user's todo
        String otherToken = registerAndLogin("other_todo_user", "other_todo@example.com", "Other1234!");
        ResponseEntity<String> resp = get("/api/todos/" + todoId, otherToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        cleanupUser("other_todo@example.com");
    }

    // ── PUT /api/todos/{id} ───────────────────────────────────────────────────

    @Test @Order(11)
    void updateTodo_success() {
        Map<String, Object> body = Map.of("title", "Updated title", "description", "Updated desc", "completed", false);
        ResponseEntity<Map<String, Object>> resp = put("/api/todos/" + todoId, body, token, MAP_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("title")).isEqualTo("Updated title");
    }

    @Test @Order(12)
    void updateTodo_blankTitle_returns400() {
        Map<String, Object> body = Map.of("title", "", "completed", false);
        ResponseEntity<String> resp = put("/api/todos/" + todoId, body, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(13)
    void updateTodo_notFound_returns404() {
        Map<String, Object> body = Map.of("title", "Ghost todo", "completed", false);
        ResponseEntity<String> resp = put("/api/todos/999999", body, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── PATCH /api/todos/{id}/toggle ──────────────────────────────────────────

    @Test @Order(14)
    void toggleTodo_toCompleted() {
        ResponseEntity<Map<String, Object>> resp = patchMap("/api/todos/" + todoId + "/toggle", null, token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("completed")).isEqualTo(true);
    }

    @Test @Order(15)
    void toggleTodo_backToPending() {
        ResponseEntity<Map<String, Object>> resp = patchMap("/api/todos/" + todoId + "/toggle", null, token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("completed")).isEqualTo(false);
    }

    @Test @Order(16)
    void toggleTodo_notFound_returns404() {
        ResponseEntity<String> resp = patch("/api/todos/999999/toggle", null, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── DELETE /api/todos/{id} ────────────────────────────────────────────────

    @Test @Order(17)
    void deleteTodo_success() {
        // Create a fresh todo to delete
        Map<String, Object> body = Map.of("title", "To be deleted");
        ResponseEntity<Map<String, Object>> created = post("/api/todos", body, token, MAP_TYPE);
        long deleteId = ((Number) created.getBody().get("id")).longValue();

        ResponseEntity<String> resp = delete("/api/todos/" + deleteId, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Confirm it's gone
        ResponseEntity<String> check = get("/api/todos/" + deleteId, token, String.class);
        assertThat(check.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @Order(18)
    void deleteTodo_notFound_returns404() {
        ResponseEntity<String> resp = delete("/api/todos/999999", token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── DELETE /api/todos ─────────────────────────────────────────────────────

    @Test @Order(19)
    void deleteAllTodos_success() {
        // Create some todos first
        post("/api/todos", Map.of("title", "Todo A"), token, MAP_TYPE);
        post("/api/todos", Map.of("title", "Todo B"), token, MAP_TYPE);

        ResponseEntity<String> resp = delete("/api/todos", token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Confirm all are gone
        ResponseEntity<List<Map<String, Object>>> list = get("/api/todos", token, LIST_MAP);
        assertThat(list.getBody()).isEmpty();
    }

    @Test @Order(20)
    void deleteAllTodos_noToken_returns401() {
        ResponseEntity<String> resp = restTemplate.exchange(
                url("/api/todos"), HttpMethod.DELETE, new HttpEntity<>(jsonHeaders()), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

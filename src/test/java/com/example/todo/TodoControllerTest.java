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

    // Page<Todo> serialises as { content: [...], totalElements: N, ... }
    private static final ParameterizedTypeReference<Map<String, Object>> PAGE_TYPE =
            new ParameterizedTypeReference<Map<String, Object>>() {};

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
        cleanupUser("other_todo@example.com");
    }

    // ── POST /api/todos ───────────────────────────────────────────────────────

    @Test @Order(1)
    void createTodo_success() {
        Map<String, Object> body = Map.of("title", "Buy groceries", "description", "Milk, eggs", "completed", false);
        ResponseEntity<Map<String, Object>> resp = post("/api/todos", body, token, MAP_TYPE);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("title")).isEqualTo("Buy groceries");
        assertThat(resp.getBody().get("completed")).isEqualTo(false);
        assertThat(resp.getBody()).containsKey("priority");
        assertThat(resp.getBody()).containsKey("createdAt");
        todoId = ((Number) resp.getBody().get("id")).longValue();
    }

    @Test @Order(2)
    void createTodo_withPriorityAndDueDate_success() {
        Map<String, Object> body = Map.of(
                "title", "High priority task",
                "priority", "HIGH",
                "dueDate", "2026-12-31");
        ResponseEntity<Map<String, Object>> resp = post("/api/todos", body, token, MAP_TYPE);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("priority")).isEqualTo("HIGH");
        assertThat(resp.getBody().get("dueDate")).isEqualTo("2026-12-31");
    }

    @Test @Order(3)
    void createTodo_invalidPriority_returns400() {
        Map<String, Object> body = Map.of("title", "Bad priority", "priority", "URGENT");
        ResponseEntity<String> resp = post("/api/todos", body, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(4)
    void createTodo_blankTitle_returns400() {
        Map<String, Object> body = Map.of("title", "", "completed", false);
        ResponseEntity<String> resp = post("/api/todos", body, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(5)
    void createTodo_noToken_returns401() {
        Map<String, Object> body = Map.of("title", "Unauthorized todo");
        ResponseEntity<String> resp = restTemplate.postForEntity(url("/api/todos"), body, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── GET /api/todos ────────────────────────────────────────────────────────

    @Test @Order(6)
    void getAllTodos_success_returnsPage() {
        ResponseEntity<Map<String, Object>> resp = get("/api/todos", token, PAGE_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKeys("content", "totalElements", "totalPages");
        List<?> content = (List<?>) resp.getBody().get("content");
        assertThat(content).isNotEmpty();
    }

    @Test @Order(7)
    void getAllTodos_filterCompleted_false() {
        ResponseEntity<Map<String, Object>> resp = get("/api/todos?completed=false", token, PAGE_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> content = (List<Map<String, Object>>) resp.getBody().get("content");
        assertThat(content).allMatch(t -> Boolean.FALSE.equals(t.get("completed")));
    }

    @Test @Order(8)
    void getAllTodos_filterCompleted_true_empty() {
        ResponseEntity<Map<String, Object>> resp = get("/api/todos?completed=true", token, PAGE_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> content = (List<?>) resp.getBody().get("content");
        assertThat(content).isEmpty();
    }

    @Test @Order(9)
    void getAllTodos_filterByPriority_HIGH() {
        ResponseEntity<Map<String, Object>> resp = get("/api/todos?priority=HIGH", token, PAGE_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> content = (List<Map<String, Object>>) resp.getBody().get("content");
        assertThat(content).allMatch(t -> "HIGH".equals(t.get("priority")));
        assertThat(content).isNotEmpty();
    }

    @Test @Order(10)
    void getAllTodos_search_findsMatchingTitle() {
        ResponseEntity<Map<String, Object>> resp = get("/api/todos?search=groceries", token, PAGE_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> content = (List<Map<String, Object>>) resp.getBody().get("content");
        assertThat(content).isNotEmpty();
        assertThat(content).anyMatch(t ->
                ((String) t.get("title")).toLowerCase().contains("groceries"));
    }

    @Test @Order(11)
    void getAllTodos_search_noMatch_returnsEmptyPage() {
        ResponseEntity<Map<String, Object>> resp = get("/api/todos?search=xyznonexistent", token, PAGE_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> content = (List<?>) resp.getBody().get("content");
        assertThat(content).isEmpty();
    }

    @Test @Order(12)
    void getAllTodos_pagination_pageSizeRespected() {
        // Ensure at least 2 todos exist (created in orders 1 and 2)
        ResponseEntity<Map<String, Object>> resp = get("/api/todos?page=0&size=1", token, PAGE_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> content = (List<?>) resp.getBody().get("content");
        assertThat(content).hasSize(1);
        assertThat(((Number) resp.getBody().get("totalElements")).longValue()).isGreaterThanOrEqualTo(2);
    }

    @Test @Order(13)
    void getAllTodos_sortByTitle_asc() {
        ResponseEntity<Map<String, Object>> resp = get("/api/todos?sort=title,asc", token, PAGE_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── GET /api/todos/stats ──────────────────────────────────────────────────

    @Test @Order(14)
    void getStats_success() {
        ResponseEntity<Map<String, Object>> resp = getMap("/api/todos/stats", token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKeys(
                "total", "completed", "pending",
                "highPriority", "mediumPriority", "lowPriority");
        assertThat(((Number) resp.getBody().get("total")).longValue()).isGreaterThanOrEqualTo(1);
        assertThat(((Number) resp.getBody().get("highPriority")).longValue()).isGreaterThanOrEqualTo(1);
    }

    // ── GET /api/todos/{id} ───────────────────────────────────────────────────

    @Test @Order(15)
    void getTodoById_success() {
        ResponseEntity<Map<String, Object>> resp = getMap("/api/todos/" + todoId, token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("id")).isEqualTo(todoId.intValue());
    }

    @Test @Order(16)
    void getTodoById_notFound_returns404() {
        ResponseEntity<String> resp = get("/api/todos/999999", token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @Order(17)
    void getTodoById_otherUserTodo_returns403() {
        String otherToken = registerAndLogin("other_todo_user", "other_todo@example.com", "Other1234!");
        ResponseEntity<String> resp = get("/api/todos/" + todoId, otherToken, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        cleanupUser("other_todo@example.com");
    }

    // ── PUT /api/todos/{id} ───────────────────────────────────────────────────

    @Test @Order(18)
    void updateTodo_success() {
        Map<String, Object> body = Map.of(
                "title", "Updated title",
                "description", "Updated desc",
                "completed", false,
                "priority", "LOW");
        ResponseEntity<Map<String, Object>> resp = put("/api/todos/" + todoId, body, token, MAP_TYPE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("title")).isEqualTo("Updated title");
        assertThat(resp.getBody().get("priority")).isEqualTo("LOW");
    }

    @Test @Order(19)
    void updateTodo_blankTitle_returns400() {
        Map<String, Object> body = Map.of("title", "", "completed", false);
        ResponseEntity<String> resp = put("/api/todos/" + todoId, body, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @Order(20)
    void updateTodo_notFound_returns404() {
        Map<String, Object> body = Map.of("title", "Ghost todo", "completed", false);
        ResponseEntity<String> resp = put("/api/todos/999999", body, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── PATCH /api/todos/{id}/toggle ──────────────────────────────────────────

    @Test @Order(21)
    void toggleTodo_toCompleted() {
        ResponseEntity<Map<String, Object>> resp = patchMap("/api/todos/" + todoId + "/toggle", null, token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("completed")).isEqualTo(true);
    }

    @Test @Order(22)
    void toggleTodo_backToPending() {
        ResponseEntity<Map<String, Object>> resp = patchMap("/api/todos/" + todoId + "/toggle", null, token);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("completed")).isEqualTo(false);
    }

    @Test @Order(23)
    void toggleTodo_notFound_returns404() {
        ResponseEntity<String> resp = patch("/api/todos/999999/toggle", null, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── DELETE /api/todos/{id} ────────────────────────────────────────────────

    @Test @Order(24)
    void deleteTodo_success() {
        Map<String, Object> body = Map.of("title", "To be deleted");
        ResponseEntity<Map<String, Object>> created = post("/api/todos", body, token, MAP_TYPE);
        long deleteId = ((Number) created.getBody().get("id")).longValue();

        ResponseEntity<String> resp = delete("/api/todos/" + deleteId, token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> check = get("/api/todos/" + deleteId, token, String.class);
        assertThat(check.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test @Order(25)
    void deleteTodo_notFound_returns404() {
        ResponseEntity<String> resp = delete("/api/todos/999999", token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── DELETE /api/todos ─────────────────────────────────────────────────────

    @Test @Order(26)
    void deleteAllTodos_success() {
        post("/api/todos", Map.of("title", "Todo A"), token, MAP_TYPE);
        post("/api/todos", Map.of("title", "Todo B"), token, MAP_TYPE);

        ResponseEntity<String> resp = delete("/api/todos", token, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map<String, Object>> list = get("/api/todos", token, PAGE_TYPE);
        List<?> content = (List<?>) list.getBody().get("content");
        assertThat(content).isEmpty();
    }

    @Test @Order(27)
    void deleteAllTodos_noToken_returns401() {
        ResponseEntity<String> resp = restTemplate.exchange(
                url("/api/todos"), HttpMethod.DELETE, new HttpEntity<>(jsonHeaders()), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

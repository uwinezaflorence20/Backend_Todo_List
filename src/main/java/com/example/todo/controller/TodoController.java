package com.example.todo.controller;

import com.example.todo.dto.MessageResponse;
import com.example.todo.model.Todo;
import com.example.todo.model.User;
import com.example.todo.repository.TodoRepository;
import com.example.todo.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/todos")
public class TodoController {
    @Autowired
    TodoRepository todoRepository;

    @Autowired
    UserRepository userRepository;

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
    }

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "dueDate", "priority", "title", "completed");

    /**
     * GET /api/todos
     *
     * Optional query params:
     *   completed  – true | false
     *   priority   – LOW | MEDIUM | HIGH
     *   search     – substring match on title or description (case-insensitive)
     *   page       – 0-based page index (default 0)
     *   size       – page size (default 20, max 100)
     *   sort       – field,direction  e.g. dueDate,asc  or  createdAt,desc (default createdAt,desc)
     *
     * Returns a Spring Data Page object (includes content, totalElements, totalPages, etc.)
     */
    @GetMapping
    public ResponseEntity<Page<Todo>> getAllTodos(
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        User user = getCurrentUser();

        // Clamp page size to prevent abuse
        size = Math.min(size, 100);

        // Parse sort param: "field,direction"
        String[] sortParts = sort.split(",", 2);
        String sortField = ALLOWED_SORT_FIELDS.contains(sortParts[0]) ? sortParts[0] : "createdAt";
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        // Normalise empty strings to null so the JPQL IS NULL check works
        String searchParam = (search != null && search.isBlank()) ? null : search;
        String priorityParam = (priority != null && priority.isBlank()) ? null : priority;

        Page<Todo> result = todoRepository.findWithFilters(user, completed, priorityParam, searchParam, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        User user = getCurrentUser();
        long total = todoRepository.countByUser(user);
        long completedCount = todoRepository.countByUserAndCompleted(user, true);
        long pending = total - completedCount;
        long highCount = todoRepository.countByUserAndPriority(user, "HIGH");
        long mediumCount = todoRepository.countByUserAndPriority(user, "MEDIUM");
        long lowCount = todoRepository.countByUserAndPriority(user, "LOW");
        return ResponseEntity.ok(Map.of(
                "total", total,
                "completed", completedCount,
                "pending", pending,
                "highPriority", highCount,
                "mediumPriority", mediumCount,
                "lowPriority", lowCount
        ));
    }

    @PostMapping
    public Todo createTodo(@Valid @RequestBody Todo todo) {
        todo.setUser(getCurrentUser());
        return todoRepository.save(todo);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> getTodoById(@PathVariable Long id) {
        Todo todo = todoRepository.findById(id).orElseThrow();
        if (!todo.getUser().getId().equals(getCurrentUser().getId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(todo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Todo> updateTodo(@PathVariable Long id, @Valid @RequestBody Todo todoDetails) {
        Todo todo = todoRepository.findById(id).orElseThrow();
        if (!todo.getUser().getId().equals(getCurrentUser().getId())) {
            return ResponseEntity.status(403).build();
        }

        todo.setTitle(todoDetails.getTitle());
        todo.setDescription(todoDetails.getDescription());
        todo.setCompleted(todoDetails.isCompleted());
        todo.setPriority(todoDetails.getPriority() != null ? todoDetails.getPriority() : "MEDIUM");
        todo.setDueDate(todoDetails.getDueDate());

        return ResponseEntity.ok(todoRepository.save(todo));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Todo> toggleTodo(@PathVariable Long id) {
        Todo todo = todoRepository.findById(id).orElseThrow();
        if (!todo.getUser().getId().equals(getCurrentUser().getId())) {
            return ResponseEntity.status(403).build();
        }
        todo.setCompleted(!todo.isCompleted());
        return ResponseEntity.ok(todoRepository.save(todo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTodo(@PathVariable Long id) {
        Todo todo = todoRepository.findById(id).orElseThrow();
        if (!todo.getUser().getId().equals(getCurrentUser().getId())) {
            return ResponseEntity.status(403).build();
        }

        todoRepository.delete(todo);
        return ResponseEntity.ok(new MessageResponse("Todo deleted successfully."));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<?> deleteAllTodos() {
        todoRepository.deleteByUser(getCurrentUser());
        return ResponseEntity.ok(new MessageResponse("All todos deleted successfully."));
    }
}

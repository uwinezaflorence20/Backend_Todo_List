package com.example.todo.controller;

import com.example.todo.dto.MessageResponse;
import com.example.todo.model.Todo;
import com.example.todo.model.User;
import com.example.todo.repository.TodoRepository;
import com.example.todo.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping
    public List<Todo> getAllTodos(@RequestParam(required = false) Boolean completed) {
        User user = getCurrentUser();
        if (completed != null) {
            return todoRepository.findByUserAndCompleted(user, completed);
        }
        return todoRepository.findByUser(user);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        User user = getCurrentUser();
        long total = todoRepository.countByUser(user);
        long completedCount = todoRepository.countByUserAndCompleted(user, true);
        long pending = total - completedCount;
        return ResponseEntity.ok(Map.of(
                "total", total,
                "completed", completedCount,
                "pending", pending
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

package com.example.todo.controller;

import com.example.todo.model.Todo;
import com.example.todo.model.User;
import com.example.todo.repository.TodoRepository;
import com.example.todo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
    }

    @GetMapping
    public List<Todo> getAllTodos() {
        return todoRepository.findByUser(getCurrentUser());
    }

    @PostMapping
    public Todo createTodo(@RequestBody Todo todo) {
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
    public ResponseEntity<Todo> updateTodo(@PathVariable Long id, @RequestBody Todo todoDetails) {
        Todo todo = todoRepository.findById(id).orElseThrow();
        if (!todo.getUser().getId().equals(getCurrentUser().getId())) {
            return ResponseEntity.status(403).build();
        }
        
        todo.setTitle(todoDetails.getTitle());
        todo.setDescription(todoDetails.getDescription());
        todo.setCompleted(todoDetails.isCompleted());
        
        return ResponseEntity.ok(todoRepository.save(todo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTodo(@PathVariable Long id) {
        Todo todo = todoRepository.findById(id).orElseThrow();
        if (!todo.getUser().getId().equals(getCurrentUser().getId())) {
            return ResponseEntity.status(403).build();
        }
        
        todoRepository.delete(todo);
        return ResponseEntity.ok().build();
    }
}

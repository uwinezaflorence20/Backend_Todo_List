package com.example.todo.controller;

import com.example.todo.dto.MessageResponse;
import com.example.todo.model.User;
import com.example.todo.repository.PasswordResetTokenRepository;
import com.example.todo.repository.TodoRepository;
import com.example.todo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    UserRepository userRepository;

    @Autowired
    TodoRepository todoRepository;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        return userRepository.findById(id).map(user -> {
            String username = updates.get("username");
            String email = updates.get("email");
            String role = updates.get("role");

            if (username != null && !username.equals(user.getUsername())) {
                if (userRepository.existsByUsername(username)) {
                    return ResponseEntity.badRequest()
                            .body((Object) new MessageResponse("Error: Username is already taken!"));
                }
                user.setUsername(username);
            }

            if (email != null && !email.equals(user.getEmail())) {
                if (userRepository.existsByEmail(email)) {
                    return ResponseEntity.badRequest()
                            .body((Object) new MessageResponse("Error: Email is already in use!"));
                }
                user.setEmail(email);
            }

            if (role != null) {
                user.setRole(role.toUpperCase());
            }

            return ResponseEntity.ok((Object) userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (role == null || role.isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Role must not be blank."));
        }
        return userRepository.findById(id).map(user -> {
            user.setRole(role.toUpperCase());
            return ResponseEntity.ok((Object) userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            todoRepository.deleteByUser(user);
            passwordResetTokenRepository.deleteByUser(user);
            userRepository.delete(user);
            return ResponseEntity.ok((Object) new MessageResponse("User deleted successfully."));
        }).orElse(ResponseEntity.notFound().build());
    }
}

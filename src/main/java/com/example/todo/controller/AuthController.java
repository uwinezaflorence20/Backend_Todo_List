package com.example.todo.controller;

import com.example.todo.dto.*;
import com.example.todo.model.PasswordResetToken;
import com.example.todo.model.User;
import com.example.todo.repository.PasswordResetTokenRepository;
import com.example.todo.repository.UserRepository;
import com.example.todo.security.JwtUtils;
import com.example.todo.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    PasswordResetTokenRepository tokenRepository;

    @Autowired
    EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        return ResponseEntity.ok(new JwtResponse(jwt, loginRequest.getEmail()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String jwt = jwtUtils.extractTokenFromRequest(request);
        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            jwtUtils.blacklistToken(jwt);
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new MessageResponse("Logged out successfully."));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));

        String role = signUpRequest.getRole();
        if (role == null || role.isEmpty()) {
            role = "USER";
        }
        user.setRole(role.toUpperCase());

        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        // Always return the same message to prevent email enumeration
        if (user == null) {
            return ResponseEntity.ok(new MessageResponse("If the email exists, a reset link has been sent."));
        }

        tokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
        }

        return ResponseEntity.ok(new MessageResponse("If the email exists, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken()).orElse(null);

        if (resetToken == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid reset token."));
        }

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Reset token has expired."));
        }

        User user = resetToken.getUser();
        user.setPassword(encoder.encode(request.getNewPassword()));
        userRepository.save(user);
        tokenRepository.delete(resetToken);

        return ResponseEntity.ok(new MessageResponse("Password has been reset successfully!"));
    }
}

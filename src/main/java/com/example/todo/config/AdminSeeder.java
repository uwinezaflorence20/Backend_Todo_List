package com.example.todo.config;

import com.example.todo.model.User;
import com.example.todo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        seedAdminUser();
    }

    private void seedAdminUser() {
        String adminEmail = "admin@gmail.com";
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setEmail(adminEmail);
            System.out.println("Admin user created: " + adminEmail);
        } else {
            System.out.println("Admin user updated: " + adminEmail);
        }
        
        admin.setPassword(passwordEncoder.encode("F1ette20@"));
        admin.setRole("ADMIN");
        userRepository.save(admin);
    }
}

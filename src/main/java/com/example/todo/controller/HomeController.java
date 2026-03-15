package com.example.todo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Welcome to the Todo List API!");
        response.put("status", "Live");
        response.put("documentation", "/swagger-ui/index.html");
        return response;
    }
}

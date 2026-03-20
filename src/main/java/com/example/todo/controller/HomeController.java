package com.example.todo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @GetMapping("/")
    @ResponseBody
    public String home() {
        return "<html>" +
                "<head><title>Todo API</title></head>" +
                "<body style='font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background-color: #f4f4f9;'>" +
                "  <div style='text-align: center; padding: 50px; background: white; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1);'>" +
                "    <h1 style='color: #2c3e50;'>🚀 Todo List API is Live!</h1>" +
                "    <p style='color: #7f8c8d; font-size: 1.1em;'>The backend is running smoothly on Render.</p>" +
                "    <br>" +
                "    <a href='/swagger-ui/index.html' style='display: inline-block; padding: 12px 24px; background-color: #3498db; color: white; text-decoration: none; border-radius: 6px; font-weight: bold; transition: background 0.3s;'>" +
                "      View API Documentation (Swagger)" +
                "    </a>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
}

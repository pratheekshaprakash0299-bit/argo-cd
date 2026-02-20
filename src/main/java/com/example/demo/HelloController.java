package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String home() {
        return "Hello from Multi-Stage Docker Java App 🚀";
    }

    @GetMapping("/health")
    public String health() {
        return "Application is running successfully!";
    }
}

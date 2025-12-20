package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    @GetMapping("/api")
    public String apiInfo() {
        return """
                ✅ Доска объявлений - API<br>
                Доступные маршруты:<br>
                • <a href='/api/categories'>/api/categories</a> — категории<br>
                • <a href='/api/listings'>/api/listings</a> — объявления<br>
                • <a href='/api/messages'>/api/messages</a> — сообщения<br>
                • <a href='/api/reports'>/api/reports</a> — жалобы<br>
                """;
    }
}

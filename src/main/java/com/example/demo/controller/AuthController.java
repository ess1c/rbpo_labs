package com.example.demo.controller;

import com.example.demo.dto.TokenPairResponse;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import com.example.demo.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * POST /auth/register
     * Регистрирует нового пользователя
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            createdUser.setPassword(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Ошибка валидации пароля", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Ошибка регистрации", e.getMessage()));
        }
    }

    /**
     * POST /auth/login
     * Проверяет учётные данные и возвращает пару (access + refresh) токенов
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            logger.debug("Attempting login for username: {}", request.getUsername());
            
            User user;
            try {
                user = userService.findByUsername(request.getUsername());
                logger.debug("User found: {}", user.getUsername());
            } catch (RuntimeException e) {
                logger.warn("User not found: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Ошибка входа", "Неверные учетные данные"));
            }

            if (user == null) {
                logger.warn("User is null for username: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Ошибка входа", "Неверные учетные данные"));
            }

            boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
            logger.debug("Password match result: {}", passwordMatches);
            logger.debug("Stored password hash: {}", user.getPassword() != null ? user.getPassword().substring(0, Math.min(20, user.getPassword().length())) + "..." : "null");
            
            if (!passwordMatches) {
                logger.warn("Password mismatch for user: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Ошибка входа", "Неверные учетные данные"));
            }

            TokenService.TokenPair tokenPair = tokenService.createTokenPair(user);

            TokenPairResponse response = new TokenPairResponse(
                    tokenPair.getAccessToken(),
                    tokenPair.getRefreshToken(),
                    tokenPair.getExpiresIn()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Ошибка сервера", e.getMessage()));
        }
    }

    /**
     * POST /auth/refresh
     * Получает refresh-токен, выдаёт новую пару (access + refresh) токенов
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        try {
            if (request.getRefreshToken() == null || request.getRefreshToken().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Ошибка", "Refresh token is required"));
            }

            // Обновляем токены
            TokenService.TokenPair newTokenPair = tokenService.refreshTokens(request.getRefreshToken());

            // Возвращаем новую пару
            TokenPairResponse response = new TokenPairResponse(
                    newTokenPair.getAccessToken(),
                    newTokenPair.getRefreshToken(),
                    newTokenPair.getExpiresIn()
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Ошибка", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Ошибка сервера", e.getMessage()));
        }
    }

    // Inner DTOs
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    public static class ErrorResponse {
        private String error;
        private String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
package com.rbpo.board.controller;

import com.rbpo.board.model.Role;
import com.rbpo.board.model.User;
import com.rbpo.board.repository.UserRepository;
import com.rbpo.board.dto.UserDTO;
import com.rbpo.board.service.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        Map<String, String> errors = new HashMap<>();

        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            errors.put("username", "Username is required");
        } else if (userRepository.existsByUsername(user.getUsername())) {
            errors.put("username", "Username already exists");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            errors.put("email", "Email is required");
        } else if (userRepository.existsByEmail(user.getEmail())) {
            errors.put("email", "Email already exists");
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            errors.put("password", "Password is required");
        } else {
            String passwordError = validatePassword(user.getPassword());
            if (passwordError != null) {
                errors.put("password", passwordError);
            }
        }

        if (!errors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }

        User savedUser = userRepository.save(user);
        UserDTO userDTO = new UserDTO(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getCreatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(userDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Username and password are required"));
        }

        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }

        try {
            Map<String, String> tokens = tokenService.generateTokenPair(user);
            return ResponseEntity.ok(tokens);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate tokens"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Refresh token is required"));
        }

        try {
            Map<String, String> tokens = tokenService.refreshTokenPair(refreshToken);
            return ResponseEntity.ok(tokens);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private String validatePassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long";
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            return "Password must contain at least one special character";
        }

        boolean hasUpperCase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowerCase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        if (!hasUpperCase || !hasLowerCase || !hasDigit) {
            return "Password must contain at least one uppercase letter, one lowercase letter, and one digit";
        }

        return null;
    }
}

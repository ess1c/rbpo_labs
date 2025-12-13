package com.rbpo.board.controller;

import com.rbpo.board.dto.UserDTO;
import com.rbpo.board.model.User;
import com.rbpo.board.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserDTO> userDTOs = users.stream()
                .map(user -> new UserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> new UserDTO(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        UserDTO userDTO = new UserDTO(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), savedUser.getCreatedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(userDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        return userRepository.findById(id)
                .map(user -> {
                    if (!user.getUsername().equals(userDetails.getUsername()) && userRepository.existsByUsername(userDetails.getUsername())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
                    }
                    if (!user.getEmail().equals(userDetails.getEmail()) && userRepository.existsByEmail(userDetails.getEmail())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
                    }
                    user.setUsername(userDetails.getUsername());
                    user.setEmail(userDetails.getEmail());
                    if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                        user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
                    }
                    User updatedUser = userRepository.save(user);
                    UserDTO userDTO = new UserDTO(updatedUser.getId(), updatedUser.getUsername(), updatedUser.getEmail(), updatedUser.getCreatedAt());
                    return ResponseEntity.ok(userDTO);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

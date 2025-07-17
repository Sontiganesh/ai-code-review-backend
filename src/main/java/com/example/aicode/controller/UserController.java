package com.example.aicode.controller;

import com.example.aicode.model.User;
import com.example.aicode.repository.UserRepository;
import com.example.aicode.service.MailService;
import com.example.aicode.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.example.aicode.dto.RegisterRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MailService mailService;

   // ✅ Register a new user
@PostMapping("/register")
public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
    try {
        // ✅ Log incoming data for debugging
        System.out.println("👉 Register attempt: " + request.getUsername() + ", " + request.getEmail());

        // ✅ Check for existing user by email
        if (userRepository.findByEmail(request.getEmail()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("❌ User already exists with this email.");
        }

        // ✅ Create and save new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // Note: consider hashing in real applications

        User savedUser = userRepository.save(user);

        // ✅ Return success with user info (or hide sensitive info if needed)
        return ResponseEntity.ok(Map.of(
                "message", "✅ Registration successful!",
                "user", savedUser
        ));
    } catch (Exception e) {
        // ❌ Print detailed error in console and return 400
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("❌ Registration failed: " + e.getMessage());
    }
}

    // ✅ Login with JWT response
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ User not found");
        }
        if (!user.getPassword().equals(loginRequest.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ Incorrect password");
        }

        // ✅ Generate JWT token
        String token = jwtUtil.generateToken(user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ Login successful");
        response.put("token", token);
        response.put("user", user);

        return ResponseEntity.ok(response);
    }

    // ✅ Forgot password endpoint
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        User user = userRepository.findByEmail(email);

        if (user == null) {
            // Always respond positively to avoid email enumeration attacks
            return ResponseEntity.ok("📩 If this email is registered, a reset link has been sent.");
        }

        // ✅ Generate secure token and send via email
        String token = jwtUtil.generateResetToken(email);
        mailService.sendResetLink(email, token);

        return ResponseEntity.ok("📩 Reset link sent successfully.");
    }

    // ✅ Reset password endpoint
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");

        try {
            String email = jwtUtil.extractEmail(token);

            // Validate token
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("❌ Invalid or expired token.");
            }

            // Update user password
            User user = userRepository.findByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("❌ User not found.");
            }

            user.setPassword(newPassword);
            userRepository.save(user);

            return ResponseEntity.ok("✅ Password reset successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("❌ Error resetting password: " + e.getMessage());
        }
    }

    // ✅ Get all users (optional)
    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}

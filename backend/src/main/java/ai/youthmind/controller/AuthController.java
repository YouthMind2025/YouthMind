package ai.youthmind.controller;

import ai.youthmind.entity.User;
import ai.youthmind.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");
            
            // Validate OTP (simplified here, should implement proper OTP validation)
            if (otp == null || otp.length() != 6) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid OTP");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Create user
            User user = userService.createUser(email, otp);
            
            // Generate JWT token
            String token = userService.generateToken(user);
            
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId().toString());
            data.put("token", token);
            data.put("expiresAt", "2024-12-31T23:59:59Z");
            data.put("message", "Registration successful");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String password = request.get("password");
            
            // Authenticate user
            User user = userService.authenticateUser(email, password);
            
            // Generate JWT token
            String token = userService.generateToken(user);
            
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId().toString());
            data.put("token", token);
            data.put("expiresAt", "2024-12-31T23:59:59Z");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Login failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 
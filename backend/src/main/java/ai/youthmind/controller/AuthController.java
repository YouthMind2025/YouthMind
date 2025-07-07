package ai.youthmind.controller;

import ai.youthmind.dto.AuthResponse;
import ai.youthmind.dto.RegisterRequest;
import ai.youthmind.dto.UserDto;
import ai.youthmind.entity.User;
import ai.youthmind.service.JWTService;
import ai.youthmind.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "User authentication operations")
public class AuthController {

    private static final String MOCK_OTP = "123456";

    @Autowired
    private UserService userService;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Register a new user with email and OTP verification. Use '123456' as the OTP."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid OTP, validation error, or email already exists"),
        @ApiResponse(responseCode = "409", description = "User with this email already exists")
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        
        // Validate OTP
        if (!MOCK_OTP.equals(request.getPassword())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid OTP. Please use '123456'");
            return ResponseEntity.badRequest().body(error);
        }
        
        // Check if user already exists
        if (userService.existsByEmail(request.getEmail())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User with this email already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
        
        try {
            // Create new user
            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            
            User savedUser = userService.saveUser(user);
            
            // Generate JWT token
            Pair<String, Timestamp> tokenPair = jwtService.generateToken(savedUser.getEmail());
            
            // Create UserDto for response
            UserDto userDto = new UserDto();
            userDto.setId(savedUser.getId());
            userDto.setEmail(savedUser.getEmail());
            userDto.setFirstName(savedUser.getFirstName());
            userDto.setLastName(savedUser.getLastName());
            userDto.setCreatedAt(savedUser.getCreatedAt());
            userDto.setUpdatedAt(savedUser.getUpdatedAt());
            
            // Calculate expires in milliseconds
            long expiresIn = tokenPair.getSecond().getTime() - System.currentTimeMillis();
            
            // Build AuthResponse
            AuthResponse response = new AuthResponse();
            response.setAccessToken(tokenPair.getFirst());
            response.setRefreshToken(null); // Not implemented yet
            response.setTokenType("Bearer");
            response.setExpiresIn(expiresIn);
            response.setUser(userDto);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/test")
    @Operation(
        summary = "Test authentication endpoint",
        description = "Test endpoint to verify authentication is working"
    )
    @ApiResponse(responseCode = "200", description = "Authentication test successful")
    public ResponseEntity<Map<String, String>> testAuth() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Authentication test successful");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }
}

package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.AuthResponse;
import ai.youthmind.dto.LoginRequest;
import ai.youthmind.dto.LogoutResponse;
import ai.youthmind.dto.RefreshTokenResponse;
import ai.youthmind.dto.RegisterRequest;
import ai.youthmind.dto.RegisterResponse;
import ai.youthmind.dto.SendOtpRequest;
import ai.youthmind.dto.SendOtpResponse;
import ai.youthmind.dto.UserDto;
import ai.youthmind.entity.User;
import ai.youthmind.service.JWTService;
import ai.youthmind.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    @PostMapping("/send-otp")
    @Operation(
        summary = "Send OTP for registration",
        description = "Send a verification code to the user's email for registration"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid email format")
    })
    public ResponseEntity<ApiResponse<SendOtpResponse>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        try {
            // In a real implementation, this would send an actual OTP
            // For now, we'll just return a mock response
            SendOtpResponse response = new SendOtpResponse(
                true,
                "Verification code sent to your email",
                LocalDateTime.now().plusMinutes(10)
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            SendOtpResponse errorResponse = new SendOtpResponse(
                false,
                "Failed to send OTP: " + e.getMessage(),
                null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Register a new user with email and OTP verification. Use '123456' as the OTP."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid OTP or validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User with this email already exists")
    })
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        
        // Validate OTP
        if (!MOCK_OTP.equals(request.getOtp())) {
            RegisterResponse errorResponse = new RegisterResponse();
            errorResponse.setMessage("Invalid OTP. Please use '123456'");
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse, 400));
        }
        
        // Check if user already exists
        if (userService.existsByEmail(request.getEmail())) {
            RegisterResponse errorResponse = new RegisterResponse();
            errorResponse.setMessage("User with this email already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(errorResponse, 409));
        }
        
        try {
            // Create new user
            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getOtp())); // Use OTP as password for now
            user.setFirstName(""); // Default empty values
            user.setLastName("");
            
            User savedUser = userService.saveUser(user);

            // Generate JWT token
            Pair<String, Timestamp> tokenPair = jwtService.generateToken(savedUser.getEmail());
            
            // Convert Timestamp to LocalDateTime for response
            LocalDateTime expiresAt = tokenPair.getSecond().toLocalDateTime();
            
            // Build RegisterResponse according to specification
            RegisterResponse response = new RegisterResponse(
                savedUser.getId(),
                tokenPair.getFirst(),
                expiresAt,
                "Registration successful"
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, 201));
            
        } catch (Exception e) {
            RegisterResponse errorResponse = new RegisterResponse(
                null, null, null, "Registration failed: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse, 500));
        }
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login user",
        description = "Authenticate user with email and password, return JWT tokens"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials - wrong email or password")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody @Parameter(description = "User login credentials", required = true) LoginRequest request) {
        
        try {
            // Find user by email
            Optional<User> userOpt = userService.getUserByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(error, 401));
            }

            User user = userOpt.get();
            
            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(error, 401));
            }

            // Generate JWT token (using same logic as register)
            Pair<String, Timestamp> tokenPair = jwtService.generateToken(user.getEmail());
            
            // Calculate expires in milliseconds
            long expiresIn = tokenPair.getSecond().getTime() - System.currentTimeMillis();
            
            // Build response according to API specification
            Map<String, Object> response = new HashMap<>();
            response.put("token", tokenPair.getFirst());
            response.put("userId", user.getId());
            response.put("expiresAt", tokenPair.getSecond().toLocalDateTime());
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error, 500));
        }
    }

    @PostMapping("/refresh-token")
    @Operation(
        summary = "Refresh JWT token",
        description = "Refresh the current JWT token to extend its validity"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken() {
        try {
            // Extract user from current authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, HttpStatus.UNAUTHORIZED.value()));
            }

            UUID userId = (UUID) authentication.getPrincipal();
            Optional<User> userOpt = userService.getUserById(userId);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, HttpStatus.UNAUTHORIZED.value()));
            }

            User user = userOpt.get();
            
            // Generate new token
            Pair<String, Timestamp> tokenPair = jwtService.generateToken(user.getEmail());
            
            RefreshTokenResponse response = new RefreshTokenResponse(
                tokenPair.getFirst(),
                tokenPair.getSecond().toLocalDateTime()
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Logout the current user and invalidate their session"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or missing token")
    })
    public ResponseEntity<ApiResponse<LogoutResponse>> logout() {
        try {
            // In a real implementation, this would invalidate the token
            // For now, we'll just return a success response
            LogoutResponse response = new LogoutResponse(true, "Logged out successfully");
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/test")
    @Operation(
        summary = "Test authentication endpoint",
        description = "Test endpoint to verify authentication is working"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authentication test successful")
    public ResponseEntity<Map<String, String>> testAuth() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Authentication test successful");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }
}

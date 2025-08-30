package ai.youthmind.controller;

import ai.youthmind.dto.AdminLoginRequest;
import ai.youthmind.dto.AdminLoginResponse;
import ai.youthmind.dto.AdminRegisterRequest;
import ai.youthmind.dto.AdminRegisterResponse;
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
    private static final String ADMIN_SECRET_KEY = "YOUTHMIND_ADMIN_2024";

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

            // Generate JWT token with role
            Pair<String, Timestamp> tokenPair = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole().toString());
            
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

            // Generate JWT token with role
            Pair<String, Timestamp> tokenPair = jwtService.generateToken(user.getEmail(), user.getRole().toString());
            
            // Build response according to API specification
            Map<String, Object> response = new HashMap<>();
            response.put("token", tokenPair.getFirst());
            response.put("userId", user.getId());
            response.put("expiresAt", tokenPair.getSecond().toLocalDateTime());
            response.put("role", user.getRole().toString());
            
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
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(@RequestHeader(value = "Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(null, HttpStatus.UNAUTHORIZED.value()));
            }

            String token = authHeader.substring(7);
            if (!jwtService.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(null, HttpStatus.UNAUTHORIZED.value()));
            }

            String email = jwtService.extractEmail(token);
            jwtService.blacklistToken(token);
            
            // Generate new token
            Pair<String, Timestamp> tokenPair = jwtService.generateToken(email);
            
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
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(@RequestHeader(value = "Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                LogoutResponse response = new LogoutResponse(false, "Authorization header not found or invalid");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(response, HttpStatus.UNAUTHORIZED.value()));
            }

            String token = authHeader.substring(7);
            if (!jwtService.validateToken(token)) {
                LogoutResponse response = new LogoutResponse(false, "JWT token invalid");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(response, HttpStatus.UNAUTHORIZED.value()));
            }

            jwtService.blacklistToken(token);
            LogoutResponse response = new LogoutResponse(true, "Logged out successfully");
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/admin/register")
    @Operation(
        summary = "Register a new admin user",
        description = "Register a new admin user with admin secret key validation"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Admin registered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid admin secret key or validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User with this email already exists")
    })
    public ResponseEntity<ApiResponse<AdminRegisterResponse>> registerAdmin(@Valid @RequestBody AdminRegisterRequest request) {
        
        // Validate admin secret key
        if (!ADMIN_SECRET_KEY.equals(request.getAdminSecretKey())) {
            AdminRegisterResponse errorResponse = new AdminRegisterResponse();
            errorResponse.setMessage("Invalid admin secret key");
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse, 400));
        }
        
        // Check if user already exists
        if (userService.existsByEmail(request.getEmail())) {
            AdminRegisterResponse errorResponse = new AdminRegisterResponse();
            errorResponse.setMessage("User with this email already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(errorResponse, 409));
        }
        
        try {
            // Create new admin user
            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setUsername(request.getUsername());
            user.setRole(User.Role.ADMIN);
            
            User savedUser = userService.saveUser(user);

            // Generate JWT token with admin role
            Pair<String, Timestamp> tokenPair = jwtService.generateToken(savedUser.getEmail(), "ADMIN");
            
            // Convert Timestamp to LocalDateTime for response
            LocalDateTime expiresAt = tokenPair.getSecond().toLocalDateTime();
            
            // Build AdminRegisterResponse
            AdminRegisterResponse response = new AdminRegisterResponse(
                savedUser.getId(),
                tokenPair.getFirst(),
                expiresAt,
                "ADMIN",
                "Admin registration successful"
            );
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, 201));
            
        } catch (Exception e) {
            AdminRegisterResponse errorResponse = new AdminRegisterResponse(
                null, null, null, null, "Admin registration failed: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse, 500));
        }
    }

    @PostMapping("/admin/login")
    @Operation(
        summary = "Login admin user",
        description = "Authenticate admin user with email and password, return JWT tokens with admin privileges"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin login successful"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials or not an admin user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User is not an admin")
    })
    public ResponseEntity<ApiResponse<AdminLoginResponse>> loginAdmin(@Valid @RequestBody @Parameter(description = "Admin login credentials", required = true) AdminLoginRequest request) {
        
        try {
            // Find user by email
            Optional<User> userOpt = userService.getUserByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                AdminLoginResponse error = new AdminLoginResponse();
                error.setMessage("Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(error, 401));
            }

            User user = userOpt.get();
            
            // Check if user is admin
            if (user.getRole() != User.Role.ADMIN) {
                AdminLoginResponse error = new AdminLoginResponse();
                error.setMessage("Access denied: Admin privileges required");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(error, 403));
            }
            
            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                AdminLoginResponse error = new AdminLoginResponse();
                error.setMessage("Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(error, 401));
            }

            // Generate JWT token with admin role
            Pair<String, Timestamp> tokenPair = jwtService.generateToken(user.getEmail(), "ADMIN");
            
            // Build response
            AdminLoginResponse response = new AdminLoginResponse(
                tokenPair.getFirst(),
                user.getId(),
                tokenPair.getSecond().toLocalDateTime(),
                "ADMIN",
                "Admin login successful"
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            AdminLoginResponse error = new AdminLoginResponse();
            error.setMessage("Admin login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error, 500));
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

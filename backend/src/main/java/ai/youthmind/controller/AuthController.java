package ai.youthmind.controller;

import ai.youthmind.config.AdminConfig;
import ai.youthmind.dto.*;
import ai.youthmind.entity.User;
import ai.youthmind.service.JWTService;
import ai.youthmind.service.UserService;
import ai.youthmind.service.OtpService;
import ai.youthmind.service.EmailService;
import ai.youthmind.util.AuthenticationUtils;
import ai.youthmind.util.UsernameGenerator;
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

    @Autowired
    private UserService userService;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminConfig adminConfig;
    
    @Autowired
    private OtpService otpService;
    
    @Autowired
    private EmailService emailService;

    @PostMapping("/send-otp")
    @Operation(
        summary = "Send OTP for registration",
        description = "Send a verification code to the user's email for registration"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid email format"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many requests")
    })
    public ResponseEntity<ApiResponse<SendOtpResponse>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        try {
            // Send real OTP via email
            otpService.sendOtp(request.getEmail());
            
            // Get expiration time for response
            LocalDateTime expiresAt = otpService.getOtpExpiration(request.getEmail());
            if (expiresAt == null) {
                expiresAt = LocalDateTime.now().plusMinutes(10); // fallback
            }
            
            SendOtpResponse response = new SendOtpResponse(
                true,
                "Verification code sent to your email",
                expiresAt
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
        description = "Register a new user with email and OTP verification"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid OTP or validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User with this email already exists")
    })
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        System.out.println("🔍 DEBUG: Register method called with email: " + request.getEmail());
        
        // Validate OTP using real OTP service
        System.out.println("🔍 DEBUG: Validating OTP: " + request.getOtp());
        if (!otpService.validateOtp(request.getEmail(), request.getOtp())) {
            System.out.println("🔍 DEBUG: OTP validation failed");
            RegisterResponse errorResponse = new RegisterResponse();
            errorResponse.setMessage("Invalid or expired OTP. Please request a new verification code.");
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse, 400));
        }
        System.out.println("🔍 DEBUG: OTP validation passed");
        
        // Check if user already exists
        System.out.println("🔍 DEBUG: Checking if user exists");
        if (userService.existsByEmail(request.getEmail())) {
            System.out.println("🔍 DEBUG: User already exists");
            RegisterResponse errorResponse = new RegisterResponse();
            errorResponse.setMessage("User with this email already exists");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(errorResponse, 409));
        }
        System.out.println("🔍 DEBUG: User does not exist, proceeding with registration");
        
        try {
            System.out.println("🔍 DEBUG: Starting user creation");
            // Create new user
            User user = new User();
            user.setEmail(request.getEmail());
            
            // Handle optional username - generate if not provided
            String username = request.getUsername();
            if (username == null || username.trim().isEmpty()) {
                // Generate a random but readable username
                username = generateUniqueUsername();
            }
            user.setUsername(username);
            System.out.println("🔍 DEBUG: Username set: " + username);
            
            user.setPassword(passwordEncoder.encode(request.getPassword())); // Use actual password
            user.setFirstName(""); // Default empty values
            user.setLastName("");
            System.out.println("🔍 DEBUG: User object created, saving to database");
            
            User savedUser = userService.saveUser(user);
            System.out.println("🔍 DEBUG: User saved with ID: " + savedUser.getId());

            // Generate JWT token with role
            System.out.println("🔍 DEBUG: Generating JWT token");
            Pair<String, Timestamp> tokenPair = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole().toString());
            System.out.println("🔍 DEBUG: JWT token generated successfully");
            
            // Convert Timestamp to LocalDateTime for response
            LocalDateTime expiresAt = tokenPair.getSecond().toLocalDateTime();
            
            // Build RegisterResponse according to specification
            RegisterResponse response = new RegisterResponse(
                savedUser.getId(),
                tokenPair.getFirst(),
                expiresAt,
                "Registration successful"
            );
            System.out.println("🔍 DEBUG: RegisterResponse created, returning success");
            
            // Send welcome email (non-blocking)
            try {
                emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getUsername());
            } catch (Exception e) {
                // Log but don't fail registration if welcome email fails
                System.err.println("Failed to send welcome email: " + e.getMessage());
            }
            
            System.out.println("🔍 DEBUG: About to return success response");
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, 201));
            
        } catch (Exception e) {
            System.err.println("🔍 DEBUG: Exception in register method: " + e.getMessage());
            e.printStackTrace();
            RegisterResponse errorResponse = new RegisterResponse(
                null, null, null, "Registration failed: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse, 500));
        }
    }
    
    /**
     * Generate a unique username by checking against existing usernames
     */
    private String generateUniqueUsername() {
        String username;
        int attempts = 0;
        int maxAttempts = 10;
        
        do {
            username = UsernameGenerator.generateUsername();
            attempts++;
        } while (attempts < maxAttempts && userService.existsByUsername(username));
        
        // If we couldn't find a unique username after max attempts, 
        // add timestamp to ensure uniqueness
        if (userService.existsByUsername(username)) {
            username = UsernameGenerator.generateUsername() + System.currentTimeMillis() % 1000;
        }
        
        return username;
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
        if (!adminConfig.getSecretKey().equals(request.getAdminSecretKey())) {
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

    @PostMapping("/admin/revoke-token")
    @Operation(
            summary = "Revoke User Tokens",
            description = "Blacklist all JWT Tokens of a user"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tokens revoked successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User is not an admin")
    })
    public ResponseEntity<ApiResponse<RevokeUserTokensResponse>> revokeUserTokens(
            @Valid @RequestBody @Parameter(description = "User Tokens Revocation credentials", required = true)
            RevokeUserTokensRequest request
    ) {
        RevokeUserTokensResponse response = new RevokeUserTokensResponse();

        try {
            // Find user by email or id
            Optional<User> userOpt = userService.getUserByEmail(request.getEmail())
                    .or(() -> userService.getUserById(request.getUserId()));

            if (userOpt.isEmpty()) {
                response.setMessage("Invalid credentials");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error(response, 401));
            }

            User user = userOpt.get();
            String email = user.getEmail();
            jwtService.blacklistUserTokens(email);

            response.setUserId(user.getId());
            response.setMessage("User Tokens revoked successful");
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            response.setMessage("Admin login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(response, 500));
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

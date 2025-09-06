package ai.youthmind.controller;

import ai.youthmind.dto.*;
import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.AuthResponse;
import ai.youthmind.dto.LoginRequest;
import ai.youthmind.dto.LogoutResponse;
import ai.youthmind.dto.RegisterRequest;
import ai.youthmind.dto.RegisterResponse;
import ai.youthmind.dto.SendOtpRequest;
import ai.youthmind.dto.SendOtpResponse;
import ai.youthmind.dto.UserDto;
import ai.youthmind.entity.User;
import ai.youthmind.service.AuditLogService;
import ai.youthmind.service.JWTService;
import ai.youthmind.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    private AuditLogService auditLogService;

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
            auditLogService.logSecurityEvent("REGISTRATION_FAILED", request.getEmail(),
                    "Registration failed - invalid OTP", false);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse, 400));
        }
        
        // Check if user already exists
        if (userService.existsByEmail(request.getEmail())) {
            RegisterResponse errorResponse = new RegisterResponse();
            errorResponse.setMessage("User with this email already exists");
            auditLogService.logSecurityEvent("REGISTRATION_FAILED", request.getEmail(),
                    "Registration failed - email already exists", false
            );
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
            TokenPair tokenPair = jwtService.generateTokenPair(savedUser.getEmail());
            
            // Convert Timestamp to LocalDateTime for response
            LocalDateTime expiresAt = tokenPair.getAccessTokenExpiry().toLocalDateTime();

            // Build RegisterResponse according to specification
            RegisterResponse response = new RegisterResponse(
                savedUser.getId(),
                tokenPair.getAccessToken(),
                expiresAt,
                "Registration successful"
            );

            auditLogService.logSecurityEvent("REGISTRATION_SUCCESS", savedUser.getEmail(),
                    "User registered successfully", true);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, 201));
            
        } catch (Exception e) {
            RegisterResponse errorResponse = new RegisterResponse(
                null, null, null, "Registration failed: " + e.getMessage()
            );
            auditLogService.logSecurityEvent("REGISTRATION_ERROR", request.getEmail(),
                    "Registration failed with error: " + e.getMessage(), false);
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
                auditLogService.logSecurityEvent("LOGIN_FAILED", request.getEmail(),
                        "Login failed - user not found", false);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(error, 401));
            }

            User user = userOpt.get();
            
            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid credentials");
                auditLogService.logSecurityEvent("LOGIN_FAILED", request.getEmail(),
                        "Login failed - invalid password", false);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(error, 401));
            }

            // Generate JWT token (using same logic as register)
            TokenPair tokenPair = jwtService.generateTokenPair(user.getEmail());

            // Calculate expires in milliseconds
            LocalDateTime expiresAt = tokenPair.getAccessTokenExpiry().toLocalDateTime();
            
            // Build response according to API specification
            Map<String, Object> response = new HashMap<>();
            response.put("token", tokenPair.getAccessToken());
            response.put("refreshToken", tokenPair.getRefreshToken());
            response.put("userId", user.getId());
            response.put("expiresAt", expiresAt);

            auditLogService.logSecurityEvent("LOGIN_SUCCESS", user.getEmail(),
                    "User logged in successfully", true);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error, 500));
        }
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout user and revoke tokens",
            description = "Invalidates the provided access token and refresh token, effectively logging out the user"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful, tokens revoked"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request - missing tokens"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or expired tokens")
    })
    public ResponseEntity<LogoutResponse> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) LogoutRequest logoutRequest) {

        try {
            String accessToken = null;
            String refreshToken = null;
            String email = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
            }

            if (logoutRequest != null) {
                refreshToken = logoutRequest.getRefreshToken();
            }

            if (accessToken == null && refreshToken == null) {
                auditLogService.logSecurityEvent("LOGOUT_FAILED", "unknown",
                        "Logout failed - no tokens provided", false);
                return ResponseEntity.badRequest().body(
                        new LogoutResponse(false, "No tokens provided for revocation"));
            }

            if (accessToken != null) {
                jwtService.blacklistToken(accessToken);
                email = jwtService.extractEmail(accessToken);
            }

            if (refreshToken != null) {
                jwtService.blacklistToken(refreshToken);
                email = jwtService.extractEmail(refreshToken);
            }

            auditLogService.logSecurityEvent("LOGOUT_SUCCESS", email != null ? email : "unknown",
                    "User logged out successfully", true);

            return ResponseEntity.ok(
                    new LogoutResponse(true, "Logout successful. Tokens have been revoked."));

        } catch (Exception e) {
            auditLogService.logSecurityEvent("LOGOUT_ERROR", "unknown",
                    "Logout failed with error: " + e.getMessage(), false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LogoutResponse(false, "Logout failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Use a valid refresh token to obtain a new access token and refresh token"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tokens refreshed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request - missing refresh token"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or expired refresh token")
    })
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest refreshRequest) {

        try {
            if (refreshRequest.getRefreshToken() == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Refresh token is required");

                auditLogService.logSecurityEvent("TOKEN_REFRESH_FAILED", "unknown",
                        "Token refresh failed - no refresh token provided", false);

                return ResponseEntity.badRequest().body(error);
            }

            String email = jwtService.extractEmail(refreshRequest.getRefreshToken());

            Optional<User> userOpt = userService.getUserByEmail(email);
            if (userOpt.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid refresh token");

                auditLogService.logSecurityEvent("TOKEN_REFRESH_FAILED", email,
                        "Token refresh failed - user not found", false);

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            User user = userOpt.get();
            TokenPair tokenPair = jwtService.refreshTokens(refreshRequest.getRefreshToken(), user);

            UserDto userDto = new UserDto();
            userDto.setId(user.getId());
            userDto.setEmail(user.getEmail());
            userDto.setFirstName(user.getFirstName());
            userDto.setLastName(user.getLastName());
            userDto.setCreatedAt(user.getCreatedAt());
            userDto.setUpdatedAt(user.getUpdatedAt());

            long expiresIn = tokenPair.getAccessTokenExpiry().getTime() - System.currentTimeMillis();

            // Build AuthResponse
            AuthResponse response = new AuthResponse();
            response.setAccessToken(tokenPair.getAccessToken());
            response.setRefreshToken(tokenPair.getRefreshToken());
            response.setTokenType("Bearer");
            response.setExpiresIn(expiresIn);
            response.setUser(userDto);

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid refresh token: " + e.getMessage());

            auditLogService.logSecurityEvent("TOKEN_REFRESH_FAILED", "unknown",
                    "Token refresh failed - invalid refresh token: " + e.getMessage(), false);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Token refresh failed: " + e.getMessage());

            auditLogService.logSecurityEvent("TOKEN_REFRESH_ERROR", "unknown",
                    "Token refresh failed with error: " + e.getMessage(), false);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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

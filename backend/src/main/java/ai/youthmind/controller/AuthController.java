package ai.youthmind.controller;

import ai.youthmind.dto.AuthResponse;
import ai.youthmind.dto.LoginRequest;
import ai.youthmind.dto.RegisterRequest;
import ai.youthmind.dto.UserDto;
import ai.youthmind.entity.User;
import ai.youthmind.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and user registration endpoints")
public class AuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Create a new user account with email, password, and personal information"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "User registered successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "tokenType": "Bearer",
                        "expiresIn": 86400000,
                        "user": {
                            "id": 1,
                            "email": "user@example.com",
                            "firstName": "John",
                            "lastName": "Doe",
                            "createdAt": "2024-01-15T10:30:00",
                            "updatedAt": "2024-01-15T10:30:00"
                        }
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<AuthResponse> register(
        @Parameter(description = "User registration data", required = true)
        @Valid @RequestBody RegisterRequest request
    ) {
        // Check if user already exists
        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        User savedUser = userService.saveUser(user);

        // Convert to DTO
        UserDto userDto = new UserDto(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            savedUser.getCreatedAt(),
            savedUser.getUpdatedAt()
        );

        // TODO: Generate JWT tokens (implement with JWT service)
        AuthResponse response = new AuthResponse(
            "dummy-jwt-token", // Replace with actual JWT generation
            "dummy-refresh-token", // Replace with actual refresh token
            "Bearer",
            86400000L,
            userDto
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login user",
        description = "Authenticate user with email and password, return JWT tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Login successful",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "tokenType": "Bearer",
                        "expiresIn": 86400000,
                        "user": {
                            "id": 1,
                            "email": "user@example.com",
                            "firstName": "John",
                            "lastName": "Doe",
                            "createdAt": "2024-01-15T10:30:00",
                            "updatedAt": "2024-01-15T10:30:00"
                        }
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(
        @Parameter(description = "User login credentials", required = true)
        @Valid @RequestBody LoginRequest request
    ) {
        Optional<User> userOpt = userService.getUserByEmail(request.getEmail());
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userOpt.get();
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Convert to DTO
        UserDto userDto = new UserDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );

        // TODO: Generate JWT tokens (implement with JWT service)
        AuthResponse response = new AuthResponse(
            "dummy-jwt-token", // Replace with actual JWT generation
            "dummy-refresh-token", // Replace with actual refresh token
            "Bearer",
            86400000L,
            userDto
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile",
        description = "Retrieve the profile of the currently authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<UserDto> getCurrentUser(
        @Parameter(description = "JWT token for authentication", required = true)
        @RequestHeader("Authorization") String authHeader
    ) {
        // TODO: Implement JWT token validation and user extraction
        // For now, return a dummy response
        UserDto userDto = new UserDto(
            1L,
            "user@example.com",
            "John",
            "Doe",
            null,
            null
        );
        
        return ResponseEntity.ok(userDto);
    }
} 
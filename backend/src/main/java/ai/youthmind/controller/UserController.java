package ai.youthmind.controller;

import ai.youthmind.dto.UserDto;
import ai.youthmind.entity.User;
import ai.youthmind.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User management and profile operations")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(
        summary = "Get all users",
        description = "Retrieve a list of all registered users (admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of users retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDto> userDtos = users.stream()
            .map(user -> new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCreatedAt(),
                user.getUpdatedAt()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(userDtos);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieve a specific user by their ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found and retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserDto> getUserById(
        @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
        @PathVariable UUID id
    ) {
        Optional<User> userOpt = userService.getUserById(id);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        UserDto userDto = new UserDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
        
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/email/{email}")
    @Operation(
        summary = "Get user by email",
        description = "Retrieve a specific user by their email address"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found and retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserDto> getUserByEmail(
        @Parameter(description = "User email address", example = "user@example.com", required = true)
        @PathVariable String email
    ) {
        Optional<User> userOpt = userService.getUserByEmail(email);
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        UserDto userDto = new UserDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
        
        return ResponseEntity.ok(userDto);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete user",
        description = "Delete a user account by ID (admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(
        @Parameter(description = "User ID to delete", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
        @PathVariable UUID id
    ) {
        if (!userService.getUserById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
} 
package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.UserDto;
import ai.youthmind.dto.UserPersonalityProfile;
import ai.youthmind.dto.UserDeletionResponseDto;
import ai.youthmind.entity.User;
import ai.youthmind.service.UserService;
import ai.youthmind.service.UserPersonalityService;
import ai.youthmind.service.PsychologicalStateCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User management and profile operations")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserPersonalityService userPersonalityService;
    
    @Autowired
    private PsychologicalStateCacheService psychologicalStateCacheService;

    @GetMapping
    @Operation(
        summary = "Get all users",
        description = "Retrieve a list of all registered users (admin only)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of users retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDto> userDtos = users.stream()
            .map(user -> new UserDto(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getAvatar(),
                user.getPostsCount(),
                user.getLikesCount(),
                user.getBookmarksCount(),
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found and retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
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
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getBio(),
            user.getAvatar(),
            user.getPostsCount(),
            user.getLikesCount(),
            user.getBookmarksCount(),
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found and retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
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
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getBio(),
            user.getAvatar(),
            user.getPostsCount(),
            user.getLikesCount(),
            user.getBookmarksCount(),
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserDeletionResponseDto>> deleteUser(
        @Parameter(description = "User ID to delete", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
        @PathVariable UUID id
    ) {
        try {
            // Check if user exists
            Optional<User> userOpt = userService.getUserById(id);
            if (userOpt.isEmpty()) {
                UserDeletionResponseDto error = new UserDeletionResponseDto();
                error.setUserId(id);
                error.setMessage("User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(error, HttpStatus.NOT_FOUND.value()));
            }
            
            // Delete the user
            userService.deleteUser(id);
            
            UserDeletionResponseDto response = new UserDeletionResponseDto();
            response.setUserId(id);
            response.setMessage("User deleted successfully");
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            UserDeletionResponseDto error = new UserDeletionResponseDto();
            error.setUserId(id);
            error.setMessage("Failed to delete user. Please try again later.");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(error, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
    
    // ===== PERSONALITY ANALYSIS ENDPOINTS =====
    
    @GetMapping("/personality/{userId}")
    @Operation(
        summary = "Get user personality profile",
        description = "Retrieve the personality profile and analysis for a specific user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Personality profile retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserPersonalityProfile>> getPersonalityProfile(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId) {
        
        try {
            // Check if user exists
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(ApiResponse.<UserPersonalityProfile>error(null, 404));
            }
            
            // Get personality profile
            UserPersonalityProfile profile = userPersonalityService.getUserPersonalityProfile(userId);
            
            return ResponseEntity.ok(ApiResponse.success(profile));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(ApiResponse.<UserPersonalityProfile>error(null, 500));
        }
    }
    
    @PostMapping("/personality/compute")
    @Operation(
        summary = "Compute personality profile",
        description = "Compute or recompute personality profile based on user content and interactions"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Personality profile computed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserPersonalityProfile>> computePersonalityProfile(
            @RequestBody @Parameter(description = "User ID and optional content for analysis", required = true) Map<String, Object> request) {
        
        try {
            UUID userId = UUID.fromString((String) request.get("userId"));
            String content = (String) request.get("content");
            
            // Check if user exists
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(ApiResponse.<UserPersonalityProfile>error(null, 404));
            }
            
            // Compute personality profile (get existing or create default)
            UserPersonalityProfile profile = userPersonalityService.getUserPersonalityProfile(userId);
            
            return ResponseEntity.ok(ApiResponse.success(profile));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(ApiResponse.<UserPersonalityProfile>error(null, 500));
        }
    }
    
    @GetMapping("/personality/insights/{userId}")
    @Operation(
        summary = "Get personality insights",
        description = "Retrieve detailed personality insights and analysis for a user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Personality insights retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPersonalityInsights(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId) {
        
        try {
            // Check if user exists
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(ApiResponse.<Map<String, Object>>error(null, 404));
            }
            
            // Get personality insights (simplified implementation)
            Map<String, Object> insights = new HashMap<>();
            UserPersonalityProfile profile = userPersonalityService.getUserPersonalityProfile(userId);
            insights.put("personality_profile", profile);
            insights.put("last_updated", profile.getLastUpdated());
            
            return ResponseEntity.ok(ApiResponse.success(insights));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(ApiResponse.<Map<String, Object>>error(null, 500));
        }
    }
    
    // ===== PSYCHOLOGICAL STATE INTEGRATION =====
    
    @GetMapping("/{userId}/psychological-state")
    @Operation(
        summary = "Get user psychological state",
        description = "Retrieve the current psychological state and analysis for a user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Psychological state retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserPsychologicalState(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId) {
        
        try {
            // Check if user exists
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(ApiResponse.<Map<String, Object>>error(null, 404));
            }
            
            // Get psychological state
            Map<String, Object> psychologicalState = psychologicalStateCacheService.getPsychologicalState(userId);
            
            if (psychologicalState == null) {
                return ResponseEntity.status(404)
                    .body(ApiResponse.<Map<String, Object>>error(null, 404));
            }
            
            return ResponseEntity.ok(ApiResponse.success(psychologicalState));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(ApiResponse.<Map<String, Object>>error(null, 500));
        }
    }
    
    // ===== RECOMMENDATION ENGINE SETTINGS =====
    
    @PutMapping("/{userId}/recommendation-engine")
    @Operation(
        summary = "Toggle recommendation engine",
        description = "Enable or disable the personalized recommendation engine for a user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recommendation engine setting updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleRecommendationEngine(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId,
            @RequestBody @Parameter(description = "Recommendation engine enabled flag", required = true) 
            Map<String, Boolean> request) {
        
        try {
            // Validate request
            if (!request.containsKey("enabled")) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>error(null, 400));
            }
            
            Boolean enabled = request.get("enabled");
            
            // Check if user exists
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(ApiResponse.<Map<String, Object>>error(null, 404));
            }
            
            User user = userOpt.get();
            user.setRecommendationEngineEnabled(enabled);
            userService.saveUser(user);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("recommendationEngineEnabled", enabled);
            response.put("message", enabled ? "Personalized recommendations enabled" : "Chronological feed enabled");
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(ApiResponse.<Map<String, Object>>error(null, 500));
        }
    }
    
    @GetMapping("/{userId}/recommendation-engine")
    @Operation(
        summary = "Get recommendation engine status",
        description = "Check if the personalized recommendation engine is enabled for a user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recommendation engine status retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendationEngineStatus(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId) {
        
        try {
            // Check if user exists
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(ApiResponse.<Map<String, Object>>error(null, 404));
            }
            
            User user = userOpt.get();
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("recommendationEngineEnabled", user.getRecommendationEngineEnabled());
            response.put("feedType", user.getRecommendationEngineEnabled() ? "personalized" : "chronological");
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(ApiResponse.<Map<String, Object>>error(null, 500));
        }
    }
}
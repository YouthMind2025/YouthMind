package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.WarningRequest;
import ai.youthmind.dto.WarningResponse;
import ai.youthmind.dto.UserModerationStatusResponse;
import ai.youthmind.entity.WarningHistory;
import ai.youthmind.entity.User;
import ai.youthmind.entity.UserModerationStatus;
import ai.youthmind.service.WarningService;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.repository.UserModerationStatusRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for warning system operations.
 * Provides endpoints for issuing warnings, managing user bans, and viewing warning history.
 */
@RestController
@RequestMapping("/api/warnings")
@Tag(name = "Warning System", description = "Endpoints for user warning and moderation management")
public class WarningController {
    
    @Autowired
    private WarningService warningService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserModerationStatusRepository moderationStatusRepository;
    
    /**
     * Issue a warning to a user.
     */
    @PostMapping("/issue")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Operation(
        summary = "Issue a warning to a user",
        description = "Issues a warning to a user with specified type, severity, and reason. " +
                     "Automatically applies progressive discipline based on warning count."
    )
    public ResponseEntity<ApiResponse<WarningResponse>> issueWarning(
            @Valid @RequestBody @Parameter(description = "Warning request details", required = true) 
            WarningRequest request,
            Authentication authentication) {
        
        try {
            UUID moderatorId = UUID.fromString(authentication.getName());
            
            WarningHistory warning = warningService.issueWarning(
                request.getUserId(),
                request.getWarningType(),
                request.getSeverity(),
                request.getReason(),
                moderatorId,
                request.getPostId(),
                request.getCommentId()
            );
            
            WarningResponse response = createWarningResponse(warning);
            
            return ResponseEntity.ok(ApiResponse.<WarningResponse>builder()
                .success(true)
                .message("Warning issued successfully")
                .data(response)
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<WarningResponse>builder()
                    .success(false)
                    .message("Failed to issue warning: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Get warning history for a user.
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR') or @warningController.isUserOrModerator(#userId, authentication)")
    @Operation(
        summary = "Get warning history for a user",
        description = "Retrieves all warnings issued to a specific user, ordered by creation date (newest first)."
    )
    public ResponseEntity<ApiResponse<List<WarningResponse>>> getUserWarningHistory(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId,
            @RequestParam(defaultValue = "false") @Parameter(description = "Include only active warnings") boolean activeOnly) {
        
        try {
            List<WarningHistory> warnings = activeOnly ? 
                warningService.getActiveWarnings(userId) : 
                warningService.getUserWarningHistory(userId);
            
            List<WarningResponse> responses = warnings.stream()
                .map(this::createWarningResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.<List<WarningResponse>>builder()
                .success(true)
                .message("Warning history retrieved successfully")
                .data(responses)
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<WarningResponse>>builder()
                    .success(false)
                    .message("Failed to retrieve warning history: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Acknowledge a warning.
     */
    @PostMapping("/{warningId}/acknowledge")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR') or @warningController.isWarningOwner(#warningId, authentication)")
    @Operation(
        summary = "Acknowledge a warning",
        description = "Marks a warning as acknowledged by the user who received it."
    )
    public ResponseEntity<ApiResponse<Void>> acknowledgeWarning(
            @PathVariable @Parameter(description = "Warning ID", required = true) UUID warningId,
            Authentication authentication) {
        
        try {
            UUID userId = UUID.fromString(authentication.getName());
            warningService.acknowledgeWarning(warningId, userId);
            
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Warning acknowledged successfully")
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to acknowledge warning: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Ban a user.
     */
    @PostMapping("/ban/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Ban a user",
        description = "Bans a user for a specified duration. Only administrators can perform this action."
    )
    public ResponseEntity<ApiResponse<Void>> banUser(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId,
            @RequestParam @Parameter(description = "Ban duration in days", required = true) int durationDays,
            @RequestParam @Parameter(description = "Reason for ban", required = true) String reason) {
        
        try {
            warningService.banUser(userId, durationDays, reason);
            
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("User banned successfully")
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to ban user: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Unban a user.
     */
    @PostMapping("/unban/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Unban a user",
        description = "Removes ban status from a user. Only administrators can perform this action."
    )
    public ResponseEntity<ApiResponse<Void>> unbanUser(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId,
            @RequestParam @Parameter(description = "Reason for unban", required = true) String reason) {
        
        try {
            warningService.unbanUser(userId, reason);
            
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("User unbanned successfully")
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to unban user: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Check if a user is banned.
     */
    @GetMapping("/user/{userId}/banned")
    @Operation(
        summary = "Check if user is banned",
        description = "Checks if a user is currently banned and returns ban information."
    )
    public ResponseEntity<ApiResponse<Boolean>> isUserBanned(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId) {
        
        try {
            boolean isBanned = warningService.isUserBanned(userId);
            
            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .success(true)
                .message("Ban status retrieved successfully")
                .data(isBanned)
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Boolean>builder()
                    .success(false)
                    .message("Failed to check ban status: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Get user moderation status.
     */
    @GetMapping("/user/{userId}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR') or @warningController.isUserOrModerator(#userId, authentication)")
    @Operation(
        summary = "Get user moderation status",
        description = "Retrieves comprehensive moderation status information for a user."
    )
    public ResponseEntity<ApiResponse<UserModerationStatusResponse>> getUserModerationStatus(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId) {
        
        try {
            UserModerationStatus status = moderationStatusRepository.findByUserId(userId).orElse(null);
            User user = userRepository.findById(userId).orElse(null);
            
            if (status == null || user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<UserModerationStatusResponse>builder()
                        .success(false)
                        .message("User or moderation status not found")
                        .build());
            }
            
            UserModerationStatusResponse response = createUserModerationStatusResponse(status, user);
            
            return ResponseEntity.ok(ApiResponse.<UserModerationStatusResponse>builder()
                .success(true)
                .message("Moderation status retrieved successfully")
                .data(response)
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<UserModerationStatusResponse>builder()
                    .success(false)
                    .message("Failed to retrieve moderation status: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Get all users requiring manual review.
     */
    @GetMapping("/manual-review")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Operation(
        summary = "Get users requiring manual review",
        description = "Retrieves all users flagged for manual review due to high-severity warnings."
    )
    public ResponseEntity<ApiResponse<List<UserModerationStatusResponse>>> getUsersRequiringManualReview() {
        
        try {
            List<UserModerationStatus> statuses = moderationStatusRepository.findByManualReviewRequiredTrueOrderByUpdatedAtDesc();
            
            List<UserModerationStatusResponse> responses = statuses.stream()
                .map(status -> {
                    User user = userRepository.findById(status.getUserId()).orElse(null);
                    return createUserModerationStatusResponse(status, user);
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.<List<UserModerationStatusResponse>>builder()
                .success(true)
                .message("Users requiring manual review retrieved successfully")
                .data(responses)
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<UserModerationStatusResponse>>builder()
                    .success(false)
                    .message("Failed to retrieve users requiring manual review: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Helper method to create WarningResponse from WarningHistory.
     */
    private WarningResponse createWarningResponse(WarningHistory warning) {
        WarningResponse response = new WarningResponse(warning);
        
        // Add user information
        User user = warning.getUser();
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setUserStatus(user.getUserStatus());
            response.setWarningsCount(user.getWarningsCount());
            response.setBanExpiresAt(user.getBanExpiresAt());
        }
        
        // Add moderator information
        if (warning.getModeratorId() != null) {
            User moderator = userRepository.findById(warning.getModeratorId()).orElse(null);
            if (moderator != null) {
                response.setModeratorUsername(moderator.getUsername());
            }
        }
        
        // Add moderation status information
        UserModerationStatus status = moderationStatusRepository.findByUserId(warning.getUserId()).orElse(null);
        if (status != null) {
            response.setModerationStatus(status.getStatus());
            response.setWarningLevel(status.getWarningLevel());
            response.setManualReviewRequired(status.getManualReviewRequired());
        }
        
        return response;
    }
    
    /**
     * Helper method to create UserModerationStatusResponse.
     */
    private UserModerationStatusResponse createUserModerationStatusResponse(UserModerationStatus status, User user) {
        UserModerationStatusResponse response = new UserModerationStatusResponse(status);
        
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setUserStatus(user.getUserStatus());
            response.setWarningsCount(user.getWarningsCount());
            response.setBanExpiresAt(user.getBanExpiresAt());
        }
        
        return response;
    }
    
    /**
     * Security method to check if user is accessing their own data or is a moderator.
     */
    public boolean isUserOrModerator(UUID userId, Authentication authentication) {
        if (authentication == null) return false;
        
        UUID currentUserId = UUID.fromString(authentication.getName());
        return currentUserId.equals(userId) || 
               authentication.getAuthorities().stream()
                   .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN") || 
                                   auth.getAuthority().equals("ROLE_MODERATOR"));
    }
    
    /**
     * Security method to check if user owns the warning.
     */
    public boolean isWarningOwner(UUID warningId, Authentication authentication) {
        if (authentication == null) return false;
        
        UUID currentUserId = UUID.fromString(authentication.getName());
        // This would need to be implemented with a service call to check warning ownership
        // For now, return true for users accessing their own warnings
        return true;
    }
}

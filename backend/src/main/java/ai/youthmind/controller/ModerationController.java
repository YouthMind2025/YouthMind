package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.ModerationHistoryResponse;
import ai.youthmind.dto.ModerationStatusResponse;
import ai.youthmind.dto.ModerationSubmissionRequest;
import ai.youthmind.service.ModerationService;
import ai.youthmind.util.AuthenticationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for moderation-related endpoints.
 * Provides admin-only endpoints for content moderation operations.
 * 
 * All endpoints require ADMIN role authentication.
 */
@RestController
@RequestMapping("/api/moderation")
@Tag(name = "Moderation", description = "Content moderation operations for administrators")
@SecurityRequirement(name = "bearer-jwt")
public class ModerationController {
    
    @Autowired
    private ModerationService moderationService;
    
    /**
     * Submit content for moderation review
     * 
     * @param request The moderation submission request
     * @param authentication Current authentication context
     * @return ModerationStatusResponse with initial status
     */
    @PostMapping("/submit")
    @Operation(
        summary = "Submit content for moderation",
        description = "Submit content (post, comment, user) for moderation review. Admin-only endpoint."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Content submitted for moderation successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Content not found")
    })
    public ResponseEntity<ApiResponse<ModerationStatusResponse>> submitForModeration(
        @Valid @RequestBody @Parameter(description = "Moderation submission request", required = true) 
        ModerationSubmissionRequest request,
        Authentication authentication
    ) {
        try {
            // Get current admin user ID
            UUID adminId = AuthenticationUtils.getCurrentUserId();
            
            // Submit for moderation
            ModerationStatusResponse response = moderationService.submitForModeration(request, adminId);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            ModerationStatusResponse errorResponse = new ModerationStatusResponse();
            errorResponse.setStatus("error");
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(errorResponse, 400));
        } catch (Exception e) {
            ModerationStatusResponse errorResponse = new ModerationStatusResponse();
            errorResponse.setStatus("error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse, 500));
        }
    }
    
    /**
     * Check moderation status for specific content
     * 
     * @param contentId ID of the content to check
     * @param authentication Current authentication context
     * @return ModerationStatusResponse with current status
     */
    @GetMapping("/status/{contentId}")
    @Operation(
        summary = "Check moderation status",
        description = "Get the current moderation status for specific content. Admin-only endpoint."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Moderation status retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Content not found")
    })
    public ResponseEntity<ApiResponse<ModerationStatusResponse>> getModerationStatus(
        @Parameter(description = "Content ID to check", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID contentId,
        Authentication authentication
    ) {
        try {
            // Get moderation status
            ModerationStatusResponse response = moderationService.getModerationStatus(contentId);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            ModerationStatusResponse errorResponse = new ModerationStatusResponse();
            errorResponse.setStatus("error");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(errorResponse, 404));
        } catch (Exception e) {
            ModerationStatusResponse errorResponse = new ModerationStatusResponse();
            errorResponse.setStatus("error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse, 500));
        }
    }
    
    /**
     * Get moderation history for a specific user
     * 
     * @param userId ID of the user to get history for
     * @param authentication Current authentication context
     * @return ModerationHistoryResponse with user's moderation history
     */
    @GetMapping("/history/{userId}")
    @Operation(
        summary = "Get user moderation history",
        description = "Retrieve complete moderation history for a specific user. Admin-only endpoint."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Moderation history retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<ModerationHistoryResponse>> getModerationHistory(
        @Parameter(description = "User ID to get history for", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID userId,
        Authentication authentication
    ) {
        try {
            // Get moderation history
            ModerationHistoryResponse response = moderationService.getModerationHistory(userId);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (IllegalArgumentException e) {
            ModerationHistoryResponse errorResponse = new ModerationHistoryResponse();
            errorResponse.setUserId(userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(errorResponse, 404));
        } catch (Exception e) {
            ModerationHistoryResponse errorResponse = new ModerationHistoryResponse();
            errorResponse.setUserId(userId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse, 500));
        }
    }
}

package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.AIModerationRequest;
import ai.youthmind.dto.AIModerationResponse;
import ai.youthmind.dto.ModerationHistoryResponse;
import ai.youthmind.dto.ModerationResult;
import ai.youthmind.dto.ModerationStatusResponse;
import ai.youthmind.dto.ModerationSubmissionRequest;
import ai.youthmind.service.AIServiceClient;
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
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
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
    
    /**
     * Moderate content using AI (DeepSeek API)
     * 
     * @param request The AI moderation request
     * @param authentication Current authentication context
     * @return AIModerationResponse with AI analysis results
     */
    @PostMapping("/ai/moderate")
    @Operation(
        summary = "AI-powered content moderation",
        description = "Moderate content using DeepSeek AI API. Returns detailed analysis with safety recommendations. Admin-only endpoint."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Content moderated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI service error")
    })
    public ResponseEntity<ApiResponse<AIModerationResponse>> moderateWithAI(
        @Valid @RequestBody @Parameter(description = "AI moderation request", required = true) 
        AIModerationRequest request,
        Authentication authentication
    ) {
        try {
            // Validate request
            if (!request.isValid()) {
                AIModerationResponse errorResponse = new AIModerationResponse();
                errorResponse.setReason("Invalid request parameters");
                errorResponse.setServiceStatus("ERROR");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(errorResponse, 400));
            }
            
            // Perform AI moderation
            ModerationResult result = aiServiceClient.moderateContent(
                request.getContent(), 
                request.getContentType().toString()
            );
            
            // Convert to response
            AIModerationResponse response = AIModerationResponse.fromModerationResult(
                result, 
                aiServiceClient.getServiceStatus()
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            AIModerationResponse errorResponse = new AIModerationResponse();
            errorResponse.setReason("AI moderation failed: " + e.getMessage());
            errorResponse.setServiceStatus("ERROR");
            errorResponse.setModeratedAt(java.time.LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse, 500));
        }
    }
    
    /**
     * Get AI service status and configuration
     * 
     * @param authentication Current authentication context
     * @return Service status information
     */
    @GetMapping("/ai/status")
    @Operation(
        summary = "Get AI service status",
        description = "Get the current status and configuration of the AI moderation service. Admin-only endpoint."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Service status retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getAIServiceStatus(
        Authentication authentication
    ) {
        try {
            java.util.Map<String, Object> status = new java.util.HashMap<>();
            status.put("serviceStatus", aiServiceClient.getServiceStatus());
            status.put("isConfigured", aiServiceClient.isConfigured());
            status.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(status));
            
        } catch (Exception e) {
            java.util.Map<String, Object> errorStatus = new java.util.HashMap<>();
            errorStatus.put("serviceStatus", "ERROR");
            errorStatus.put("error", e.getMessage());
            errorStatus.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorStatus, 500));
        }
    }
    
    /**
     * Test AI moderation with sample content
     * 
     * @param authentication Current authentication context
     * @return Test moderation result
     */
    @PostMapping("/ai/test")
    @Operation(
        summary = "Test AI moderation service",
        description = "Test the AI moderation service with sample content to verify functionality. Admin-only endpoint."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Test completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI service error")
    })
    public ResponseEntity<ApiResponse<AIModerationResponse>> testAIModeration(
        Authentication authentication
    ) {
        try {
            // Test with sample content
            String testContent = "This is a test post about mental health support. I'm feeling better today and wanted to share my positive experience.";
            
            ModerationResult result = aiServiceClient.moderateContent(testContent, "POST");
            AIModerationResponse response = AIModerationResponse.fromModerationResult(
                result, 
                aiServiceClient.getServiceStatus()
            );
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            AIModerationResponse errorResponse = new AIModerationResponse();
            errorResponse.setReason("AI test failed: " + e.getMessage());
            errorResponse.setServiceStatus("ERROR");
            errorResponse.setModeratedAt(java.time.LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorResponse, 500));
        }
    }
}

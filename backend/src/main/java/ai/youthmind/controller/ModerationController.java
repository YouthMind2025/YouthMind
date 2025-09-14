package ai.youthmind.controller;

import ai.youthmind.dto.*;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
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

    /**
     * Get moderation queue with pagination and filtering
     *
     * @param status Filter by moderation status
     * @param contentType Filter by content type
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sort Sort field and direction (default: createdAt,desc)
     * @param authentication Current authentication context
     * @return Paginated moderation queue response
     */
    @GetMapping("/queue")
    @Operation(
            summary = "Get moderation queue",
            description = "Retrieve paginated list of content awaiting moderation with filtering options. Admin-only endpoint."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Moderation queue retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    public ResponseEntity<ApiResponse<ModerationQueueResponse>> getModerationQueue(
            @Parameter(description = "Filter by moderation status")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by content type")
            @RequestParam(required = false) String contentType,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction (format: field,direction)")
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Authentication authentication
    ) {
        try {
            // Parse sort parameter
            String[] sortParams = sort.split(",");
            Sort.Direction direction = sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1])
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

            // Get moderation queue
            ModerationQueueResponse response = moderationService.getModerationQueue(
                    status, contentType, pageable
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            ModerationQueueResponse errorResponse = new ModerationQueueResponse();
            errorResponse.setTotalItems(0L);
            errorResponse.setCurrentPage(page);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(errorResponse, 500));
        }
    }

    /**
     * Update moderation status for content
     *
     * @param request The moderation update request
     * @param authentication Current authentication context
     * @return Updated moderation status
     */
    @PutMapping("/update-status")
    @Operation(
            summary = "Update moderation status",
            description = "Update the moderation status for content (approve, reject, etc.). Admin-only endpoint."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Moderation status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Content not found")
    })
    public ResponseEntity<ApiResponse<ModerationStatusResponse>> updateModerationStatus(
            @Valid @RequestBody @Parameter(description = "Moderation update request", required = true)
            ModerationUpdateRequest request,
            Authentication authentication
    ) {
        try {
            // Get current admin user ID
            UUID adminId = AuthenticationUtils.getCurrentUserId();

            // Update moderation status
            ModerationStatusResponse response = moderationService.updateModerationStatus(
                    request, adminId
            );

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
     * Get moderation statistics
     *
     * @param timeframe Timeframe for statistics (e.g., "24h", "7d", "30d")
     * @param authentication Current authentication context
     * @return Moderation statistics
     */
    @GetMapping("/stats")
    @Operation(
            summary = "Get moderation statistics",
            description = "Retrieve moderation statistics with optional timeframe filter. Admin-only endpoint."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    public ResponseEntity<ApiResponse<ModerationStatsResponse>> getModerationStats(
            @Parameter(description = "Timeframe for statistics (e.g., 24h, 7d, 30d)")
            @RequestParam(required = false) String timeframe,
            Authentication authentication
    ) {
        try {
            // Get moderation statistics
            ModerationStatsResponse response = moderationService.getModerationStats(timeframe);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            ModerationStatsResponse errorResponse = new ModerationStatsResponse();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(errorResponse, 500));
        }
    }

    /**
     * Bulk update moderation status
     *
     * @param requests List of moderation update requests
     * @param authentication Current authentication context
     * @return List of updated moderation statuses
     */
    @PutMapping("/bulk-update")
    @Operation(
            summary = "Bulk update moderation status",
            description = "Update moderation status for multiple content items in bulk. Admin-only endpoint."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bulk update completed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "207", description = "Multi-status - some updates failed")
    })
    public ResponseEntity<ApiResponse<List<ModerationStatusResponse>>> bulkUpdateModerationStatus(
            @Valid @RequestBody @Parameter(description = "List of moderation update requests", required = true)
            List<ModerationUpdateRequest> requests,
            Authentication authentication
    ) {
        try {
            // Get current admin user ID
            UUID adminId = AuthenticationUtils.getCurrentUserId();

            // Process bulk update
            List<ModerationStatusResponse> responses = new ArrayList<>();
            List<ModerationStatusResponse> failedResponses = new ArrayList<>();

            for (ModerationUpdateRequest request : requests) {
                try {
                    ModerationStatusResponse response = moderationService.updateModerationStatus(
                            request, adminId
                    );
                    responses.add(response);
                } catch (Exception e) {
                    ModerationStatusResponse errorResponse = new ModerationStatusResponse();
                    errorResponse.setContentId(request.getContentId());
                    errorResponse.setStatus("error");
                    errorResponse.setReason(e.getMessage());
                    failedResponses.add(errorResponse);
                }
            }

            // Return appropriate status based on results
            if (failedResponses.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(responses));
            } else if (failedResponses.size() == requests.size()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(responses, 400));
            } else {
                // Return multi-status response
                responses.addAll(failedResponses);
                return ResponseEntity.status(HttpStatus.MULTI_STATUS)
                        .body(new ApiResponse<>(responses, 207));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(new ArrayList<>(), 500));
        }
    }
}

package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.service.AIEmotionClassificationService;
import ai.youthmind.service.RVectorService;
import ai.youthmind.service.FeedBlockPreCalculationService;
import ai.youthmind.util.ResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Administrative endpoints for system management
 * Provides admin-only endpoints for user management and system configuration
 */
@RestController
@RequestMapping("/admin/v1")
@Tag(name = "Admin Management", description = "Administrative operations for system management")
@SecurityRequirement(name = "bearer-jwt")
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RVectorService rVectorService;
    
    @Autowired
    private FeedBlockPreCalculationService feedBlockPreCalculationService;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private AIEmotionClassificationService aiEmotionClassificationService;

    /**
     * Update user's recommendation engine toggle
     * PUT /admin/v1/users/{userId}/recommendation-toggle
     */
    @PutMapping("/users/{userId}/recommendation-toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update user recommendation engine toggle",
        description = "Enable or disable personalized recommendations for a specific user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recommendation toggle updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRecommendationToggle(
        @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID userId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Recommendation toggle request")
        @Valid @RequestBody RecommendationToggleRequest request
    ) {
        try {
            // Find user
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            
            // Update recommendation engine setting
            Boolean previousValue = user.getRecommendationEngineEnabled();
            user.setRecommendationEngineEnabled(request.getEnabled());
            userRepository.save(user);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("previousValue", previousValue);
            response.put("newValue", request.getEnabled());
            response.put("updatedAt", user.getUpdatedAt());
            
            return ResponseBuilder.success(response, "Recommendation engine toggle updated successfully");
            
        } catch (ResourceNotFoundException e) {
            return ResponseBuilder.notFound(e.getMessage());
        } catch (Exception e) {
            return ResponseBuilder.internalServerError("Failed to update recommendation toggle");
        }
    }

    /**
     * Get user's recommendation engine status and related information
     * GET /admin/v1/users/{userId}/recommendation-status
     */
    @GetMapping("/users/{userId}/recommendation-status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get user recommendation engine status",
        description = "Get user's current recommendation engine status, R vector state, and feed statistics"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recommendation status retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendationStatus(
        @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID userId
    ) {
        try {
            // Find user
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("recommendationEngineEnabled", user.getRecommendationEngineEnabled());
            
            // Get R vector information if available
            try {
                UserRecommendationVector rVector = rVectorService.getCurrentVector(userId);
                Map<String, Object> rVectorInfo = new HashMap<>();
                rVectorInfo.put("version", rVector.getVersion());
                rVectorInfo.put("createdAt", rVector.getCreatedAt());
                rVectorInfo.put("updatedAt", rVector.getUpdatedAt());
                rVectorInfo.put("topEmotions", rVectorService.getTopThreeEmotions(rVector));
                response.put("rVector", rVectorInfo);
            } catch (Exception e) {
                response.put("rVector", null);
                response.put("rVectorError", "No R vector found for user");
            }
            
            // Get feed statistics (basic counts)
            Map<String, Object> feedStats = new HashMap<>();
            feedStats.put("postsCount", user.getPostsCount());
            feedStats.put("likesCount", user.getLikesCount());
            feedStats.put("bookmarksCount", user.getBookmarksCount());
            response.put("feedStatistics", feedStats);
            
            return ResponseBuilder.success(response, "Recommendation status retrieved successfully");
            
        } catch (ResourceNotFoundException e) {
            return ResponseBuilder.notFound(e.getMessage());
        } catch (Exception e) {
            return ResponseBuilder.internalServerError("Failed to get recommendation status");
        }
    }

    /**
     * Batch classify emotions for posts without user-provided emotions
     * POST /admin/v1/posts/classify-emotions
     */
    @PostMapping("/posts/classify-emotions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Batch classify emotions for posts",
        description = "Process all existing posts without user-provided emotions using AI classification. Requires ADMIN role."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Batch emotion classification completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error during batch processing")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchClassifyEmotions(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Batch classification request parameters")
        @RequestBody(required = false) BatchEmotionClassificationRequest request
    ) {
        try {
            // Set default values if request is null
            if (request == null) {
                request = new BatchEmotionClassificationRequest();
            }
            
            int limit = request.getLimit() != null ? request.getLimit() : 100;
            boolean dryRun = request.isDryRun() != null ? request.isDryRun() : false;
            
            // Validate limit
            if (limit <= 0 || limit > 1000) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", "Limit must be between 1 and 1000");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(errorData, 400));
            }
            
            long startTime = System.currentTimeMillis();
            
            // Find posts without user-provided emotions
            List<Post> postsToProcess = postRepository.findByEmotionIsNullOrEmotionIsEmpty(limit);
            
            if (postsToProcess.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("totalProcessed", 0);
                response.put("successCount", 0);
                response.put("failureCount", 0);
                response.put("duration", 0.0);
                response.put("message", "No posts found without emotions");
                
                return ResponseEntity.ok(ApiResponse.success(response));
            }
            
            // Extract post contents for batch processing
            List<String> postContents = postsToProcess.stream()
                .map(Post::getContent)
                .collect(Collectors.toList());
            
            // Perform batch AI emotion classification
            List<ai.youthmind.dto.EmotionClassificationResult> classificationResults = 
                aiEmotionClassificationService.classifyEmotionBatch(postContents);
            
            int successCount = 0;
            int failureCount = 0;
            
            // Update posts with classification results (unless dry run)
            if (!dryRun) {
                for (int i = 0; i < postsToProcess.size() && i < classificationResults.size(); i++) {
                    Post post = postsToProcess.get(i);
                    ai.youthmind.dto.EmotionClassificationResult result = classificationResults.get(i);
                    
                    try {
                        if (result != null && result.isValid()) {
                            // Update post with AI classification
                            post.setAiEmotion(result.getEmotion());
                            post.setEmotionConfidence(result.getConfidence());
                            post.setEmotionSource(result.getSource());
                            post.setEmotion(result.getEmotion()); // For R vector updates
                            post.setUpdatedAt(LocalDateTime.now());
                            
                            successCount++;
                        } else {
                            // Set fallback values
                            post.setEmotionSource("ai_fallback");
                            post.setEmotion("neutral");
                            post.setAiEmotion("neutral");
                            post.setEmotionConfidence(0.0);
                            post.setUpdatedAt(LocalDateTime.now());
                            
                            failureCount++;
                        }
                        
                        // Save the updated post
                        postRepository.save(post);
                        
                    } catch (Exception e) {
                        failureCount++;
                        // Log error but continue processing
                        System.err.println("Error updating post " + post.getId() + ": " + e.getMessage());
                    }
                }
            } else {
                // Dry run - just count successes and failures
                for (ai.youthmind.dto.EmotionClassificationResult result : classificationResults) {
                    if (result != null && result.isValid()) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                }
            }
            
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("totalProcessed", postsToProcess.size());
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("duration", duration);
            response.put("dryRun", dryRun);
            response.put("limit", limit);
            
            String message = dryRun ? 
                "Dry run completed - no changes saved" : 
                "Batch emotion classification completed successfully";
            response.put("message", message);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Batch emotion classification failed");
            errorResponse.put("details", e.getMessage());
            
            return ResponseEntity.status(500)
                .body(ApiResponse.error(errorResponse, 500));
        }
    }

    /**
     * Request DTO for recommendation toggle
     */
    public static class RecommendationToggleRequest {
        private Boolean enabled;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Request DTO for batch emotion classification
     */
    public static class BatchEmotionClassificationRequest {
        private Integer limit;
        private Boolean dryRun;

        public Integer getLimit() {
            return limit;
        }

        public void setLimit(Integer limit) {
            this.limit = limit;
        }

        public Boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(Boolean dryRun) {
            this.dryRun = dryRun;
        }
    }
}

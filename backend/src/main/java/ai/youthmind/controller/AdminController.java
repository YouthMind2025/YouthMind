package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.MappingRuleUpdateRequest;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.RecommendationMappingRule;
import ai.youthmind.entity.User;
import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.RecommendationMappingRuleRepository;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.service.AIEmotionClassificationService;
import ai.youthmind.service.DataMigrationService;
import ai.youthmind.service.RecommendationMappingService;
import ai.youthmind.util.ResponseBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private AIEmotionClassificationService aiEmotionClassificationService;
    
    @Autowired
    private DataMigrationService dataMigrationService;
    
    @Autowired
    private RecommendationMappingRuleRepository recommendationMappingRuleRepository;
    
    @Autowired
    private RecommendationMappingService recommendationMappingService;
    
    @Autowired
    private ai.youthmind.service.AIPostAnalysisService aiPostAnalysisService;

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
                // UserRecommendationVector rVector = rVectorService.getCurrentVector(userId);
                UserRecommendationVector rVector = null; // Service removed
                Map<String, Object> rVectorInfo = new HashMap<>();
                if (rVector != null) {
                    rVectorInfo.put("version", rVector.getVersion());
                    rVectorInfo.put("createdAt", rVector.getCreatedAt());
                    rVectorInfo.put("updatedAt", rVector.getUpdatedAt());
                    // rVectorInfo.put("topEmotions", rVectorService.getTopThreeEmotions(rVector));
                    rVectorInfo.put("topEmotions", List.of("Service removed")); // Service removed
                    response.put("rVector", rVectorInfo);
                } else {
                    response.put("rVector", null);
                    response.put("rVectorError", "No R vector found for user");
                }
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
     * POST /admin/v1/posts/batch-classify-emotions
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
            // Find posts without emotions - using a simpler approach
            List<Post> allPosts = postRepository.findAll();
            List<Post> postsToProcess = allPosts.stream()
                .filter(post -> post.getEmotion() == null || post.getEmotion().trim().isEmpty())
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
            
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
     * Classify emotion for a single post content
     * POST /admin/v1/posts/classify-emotion
     */
    @PostMapping("/posts/classify-emotion")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Classify emotion for post content",
        description = "Classify emotion for a single post content using AI analysis. Requires ADMIN role."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Emotion classification completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error during classification")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> classifyEmotions(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Post content for emotion classification")
        @RequestBody EmotionClassificationRequest request
    ) {
        try {
            // Validate request
            if (request == null || request.getContent() == null || request.getContent().trim().isEmpty()) {
                return ResponseBuilder.badRequest("Content is required for emotion classification");
            }
            
            log.info("Classifying emotion for post content: {} characters", request.getContent().length());
            
            // Use AIEmotionClassificationService for single post classification
            ai.youthmind.dto.EmotionClassificationResult result = aiEmotionClassificationService.classifyEmotion(request.getContent());
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("content", request.getContent());
            response.put("emotion", result.getEmotion());
            response.put("confidence", result.getConfidence());
            response.put("source", result.getSource());
            response.put("isValid", result.isValid());
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseBuilder.success(response, "Emotion classification completed successfully");
            
        } catch (Exception e) {
            log.error("Error during emotion classification: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError("Failed to classify emotions");
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
    
    /**
     * Migrate existing users to recommendation engine
     * POST /admin/v1/users/migrate
     */
    @PostMapping("/users/migrate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Migrate existing users to recommendation engine",
        description = "Initialize R vectors and feed states for all existing users who don't have them"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Migration completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error during migration")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> migrateUsers(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Migration configuration")
        @RequestBody(required = false) MigrationRequest request
    ) {
        try {
            // Use default values if request is null
            boolean dryRun = request != null && Boolean.TRUE.equals(request.getDryRun());
            int batchSize = request != null && request.getBatchSize() != null ? request.getBatchSize() : 100;
            
            // Validate batch size
            if (batchSize < 1 || batchSize > 1000) {
                return ResponseBuilder.badRequest("Batch size must be between 1 and 1000");
            }
            
            log.info("Starting user migration - dryRun: {}, batchSize: {}", dryRun, batchSize);
            
            // Run migration
            DataMigrationService.MigrationResult result = dataMigrationService.runAllMigrations();
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("dryRun", dryRun);
            response.put("batchSize", batchSize);
            response.put("totalProcessed", result.total());
            response.put("rVectorsCreated", result.created());
            response.put("feedStatesCreated", result.created()); // Same count for both
            response.put("skipped", result.skipped());
            response.put("errors", result.errors());
            response.put("successful", result.isSuccessful());
            response.put("hasErrors", result.hasErrors());
            response.put("completedAt", LocalDateTime.now());
            
            String message = dryRun ? 
                "Migration dry run completed successfully" : 
                "Migration completed successfully";
            
            return ResponseBuilder.success(response, message);
            
        } catch (Exception e) {
            log.error("Migration failed: {}", e.getMessage(), e);
            return ResponseBuilder.internalServerError("Migration failed: " + e.getMessage());
        }
    }
    
    /**
     * Request DTO for emotion classification
     */
    public static class EmotionClassificationRequest {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
    
    /**
     * Request DTO for migration endpoint
     */
    public static class MigrationRequest {
        private Boolean dryRun = false;
        private Integer batchSize = 100;
        
        public Boolean getDryRun() {
            return dryRun;
        }
        
        public void setDryRun(Boolean dryRun) {
            this.dryRun = dryRun;
        }
        
        public Integer getBatchSize() {
            return batchSize;
        }
        
        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }
    }
    
    // ===== MAPPING RULES MANAGEMENT ENDPOINTS =====
    
    /**
     * Get all recommendation mapping rules
     * GET /admin/v1/recommendationmapping/rules
     */
    @GetMapping("/recommendationmapping/rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get all mapping rules",
        description = "Retrieve all 20 recommendation mapping rules ordered by emotion combination"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mapping rules retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    public ResponseEntity<ApiResponse<List<RecommendationMappingRule>>> getAllMappingRules() {
        try {
            List<RecommendationMappingRule> rules = recommendationMappingRuleRepository.findAllByOrderByEmotionCombinationAsc();
            return ResponseBuilder.success(rules, "Mapping rules retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to retrieve mapping rules", e);
            return ResponseBuilder.internalServerError("Failed to retrieve mapping rules");
        }
    }
    
    /**
     * Get single recommendation mapping rule by ID
     * GET /admin/v1/recommendationmapping/rules/{id}
     */
    @GetMapping("/recommendationmapping/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get mapping rule by ID",
        description = "Retrieve a specific recommendation mapping rule by its ID"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mapping rule retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mapping rule not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    public ResponseEntity<ApiResponse<RecommendationMappingRule>> getMappingRule(
        @Parameter(description = "Mapping rule ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        try {
            RecommendationMappingRule rule = recommendationMappingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MappingRule", "id", id));
            return ResponseBuilder.success(rule, "Mapping rule retrieved successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseBuilder.notFound(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to retrieve mapping rule with ID: {}", id, e);
            return ResponseBuilder.internalServerError("Failed to retrieve mapping rule");
        }
    }
    
    /**
     * Update recommendation mapping rule percentages
     * PUT /admin/v1/recommendationmapping/rules/{id}
     */
    @PutMapping("/recommendationmapping/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update mapping rule percentages",
        description = "Update the emotion percentage distribution for a specific mapping rule. Percentages must sum to exactly 100."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mapping rule updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request - percentages must sum to 100"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mapping rule not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    public ResponseEntity<ApiResponse<RecommendationMappingRule>> updateMappingRule(
        @Parameter(description = "Mapping rule ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated percentage distribution")
        @Valid @RequestBody MappingRuleUpdateRequest request
    ) {
        try {
            // Find existing rule
            RecommendationMappingRule rule = recommendationMappingRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MappingRule", "id", id));
            
            // Validate percentages sum to 100
            request.validatePercentages();
            
            // Update percentages
            rule.setSadPercentage(request.getSadPercentage());
            rule.setFearPercentage(request.getFearPercentage());
            rule.setAngryPercentage(request.getAngryPercentage());
            rule.setHappyPercentage(request.getHappyPercentage());
            rule.setConflictedPercentage(request.getConflictedPercentage());
            rule.setSurprisedPercentage(request.getSurprisedPercentage());
            
            // Save updated rule
            RecommendationMappingRule updatedRule = recommendationMappingRuleRepository.save(rule);
            
            return ResponseBuilder.success(updatedRule, "Mapping rule updated successfully");
            
        } catch (ResourceNotFoundException e) {
            return ResponseBuilder.notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseBuilder.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update mapping rule with ID: {}", id, e);
            return ResponseBuilder.internalServerError("Failed to update mapping rule");
        }
    }
    
}

package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.entity.StateUpdateHistory;
import ai.youthmind.service.PsychologicalStateService;
import ai.youthmind.service.StateVectorCalculator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Psychological State Management
 * Provides endpoints for state updates, similarity calculations, and state history
 */
@RestController
@RequestMapping("/api/psychological-state")
@Tag(name = "Psychological State Management", description = "Manage user psychological states and state updates")
public class PsychologicalStateController {
    
    private static final Logger logger = LoggerFactory.getLogger(PsychologicalStateController.class);
    
    @Autowired
    private PsychologicalStateService psychologicalStateService;
    
    @Autowired
    private StateVectorCalculator stateVectorCalculator;
    
    /**
     * Update psychological state from a post
     * POST /api/psychological-state/update-from-post
     */
    @PostMapping("/update-from-post")
    @Operation(
        summary = "Update psychological state from post",
        description = "Updates user's psychological state using the posting algorithm: S_new = α·V_raw + (1-α)·S_prev"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "State updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStateFromPost(
            @Valid @RequestBody @Parameter(description = "Post state update request", required = true) 
            PostStateUpdateRequest request) {
        
        try {
            logger.info("Updating psychological state from post for user: {}", request.getUserId());
            
            Map<String, Object> result = psychologicalStateService.updateStateFromPost(
                request.getUserId(), 
                request.getPostContent(), 
                request.getPostId()
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error updating psychological state from post: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(Map.of("error", "Failed to update psychological state from post: " + e.getMessage()), 500)
            );
        }
    }
    
    /**
     * Update psychological state from an interaction
     * POST /api/psychological-state/update-from-interaction
     */
    @PostMapping("/update-from-interaction")
    @Operation(
        summary = "Update psychological state from interaction",
        description = "Updates user's psychological state using the interaction algorithm: S_new = Ws·R′′ + (1-Ws)·S_prev"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "State updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStateFromInteraction(
            @Valid @RequestBody @Parameter(description = "Interaction state update request", required = true) 
            InteractionStateUpdateRequest request) {
        
        try {
            logger.info("Updating psychological state from interaction for user: {}", request.getUserId());
            
            Map<String, Object> result = psychologicalStateService.updateStateFromInteraction(
                request.getUserId(),
                request.getTargetPostId(),
                request.getInteractionType(),
                request.getInteractionWeight()
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error updating psychological state from interaction: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(Map.of("error", "Failed to update psychological state from interaction: " + e.getMessage()), 500)
            );
        }
    }
    
    /**
     * Get current psychological state
     * GET /api/psychological-state/current/{userId}
     */
    @GetMapping("/current/{userId}")
    @Operation(
        summary = "Get current psychological state",
        description = "Retrieves the current 43-dimensional psychological state vector for a user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current state retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentState(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId) {
        
        try {
            logger.info("Getting current psychological state for user: {}", userId);
            
            List<Double> currentState = psychologicalStateService.getCurrentState(userId);
            
            Map<String, Object> result = Map.of(
                "userId", userId,
                "currentState", currentState,
                "dimensions", currentState.size(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error getting current psychological state for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(Map.of("error", "Failed to get current psychological state: " + e.getMessage()), 500)
            );
        }
    }
    
    /**
     * Calculate similarity between two psychological states
     * POST /api/psychological-state/similarity
     */
    @PostMapping("/similarity")
    @Operation(
        summary = "Calculate state similarity",
        description = "Calculates cosine similarity between two psychological state vectors"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Similarity calculated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateStateSimilarity(
            @Valid @RequestBody @Parameter(description = "Similarity calculation request", required = true) 
            StateSimilarityRequest request) {
        
        try {
            logger.info("Calculating state similarity for user: {}", request.getUserId());
            
            double similarity = psychologicalStateService.calculateStateSimilarity(
                request.getState1(), 
                request.getState2()
            );
            
            Map<String, Object> result = Map.of(
                "userId", request.getUserId(),
                "similarity", similarity,
                "isSimilar", similarity >= 0.7,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error calculating state similarity: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(Map.of("error", "Failed to calculate state similarity: " + e.getMessage()), 500)
            );
        }
    }
    
    /**
     * Get psychological state history
     * GET /api/psychological-state/history/{userId}
     */
    @GetMapping("/history/{userId}")
    @Operation(
        summary = "Get psychological state history",
        description = "Retrieves the psychological state update history for a user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "State history retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStateHistory(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId,
            @RequestParam(defaultValue = "50") @Parameter(description = "Maximum number of history entries") int limit) {
        
        try {
            logger.info("Getting psychological state history for user: {} with limit: {}", userId, limit);
            
            List<StateUpdateHistory> history = psychologicalStateService.getStateHistory(userId, limit);
            
            Map<String, Object> result = Map.of(
                "userId", userId,
                "history", history,
                "count", history.size(),
                "limit", limit,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error getting psychological state history for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(Map.of("error", "Failed to get psychological state history: " + e.getMessage()), 500)
            );
        }
    }
    
    /**
     * Initialize psychological state for a new user
     * POST /api/psychological-state/initialize/{userId}
     */
    @PostMapping("/initialize/{userId}")
    @Operation(
        summary = "Initialize psychological state",
        description = "Initializes the psychological state for a new user with default values"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "State initialized successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> initializeState(
            @PathVariable @Parameter(description = "User ID", required = true) UUID userId) {
        
        try {
            logger.info("Initializing psychological state for user: {}", userId);
            
            Map<String, Object> result = psychologicalStateService.initializeState(userId);
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error initializing psychological state for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(Map.of("error", "Failed to initialize psychological state: " + e.getMessage()), 500)
            );
        }
    }
    
    /**
     * Calculate vector operations
     * POST /api/psychological-state/vector-operations
     */
    @PostMapping("/vector-operations")
    @Operation(
        summary = "Perform vector operations",
        description = "Performs mathematical operations on psychological state vectors"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vector operations completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> performVectorOperations(
            @Valid @RequestBody @Parameter(description = "Vector operations request", required = true) 
            VectorOperationsRequest request) {
        
        try {
            logger.info("Performing vector operations for user: {}", request.getUserId());
            
            Map<String, Object> result = Map.of();
            
            switch (request.getOperation()) {
                case "add" -> {
                    List<Double> sum = stateVectorCalculator.addVectors(request.getVector1(), request.getVector2());
                    result = Map.of("result", sum, "operation", "add");
                }
                case "multiply" -> {
                    List<Double> product = stateVectorCalculator.multiplyByScalar(request.getVector1(), request.getScalar());
                    result = Map.of("result", product, "operation", "multiply", "scalar", request.getScalar());
                }
                case "normalize" -> {
                    List<Double> normalized = stateVectorCalculator.normalizeVector(request.getVector1());
                    result = Map.of("result", normalized, "operation", "normalize");
                }
                case "distance" -> {
                    double distance = stateVectorCalculator.calculateDistance(request.getVector1(), request.getVector2());
                    result = Map.of("result", distance, "operation", "distance");
                }
                case "similarity" -> {
                    double similarity = stateVectorCalculator.calculateCosineSimilarity(request.getVector1(), request.getVector2());
                    result = Map.of("result", similarity, "operation", "similarity");
                }
                case "clip" -> {
                    List<Double> clipped = stateVectorCalculator.applyClipping(request.getVector1());
                    result = Map.of("result", clipped, "operation", "clip");
                }
                default -> throw new IllegalArgumentException("Unsupported operation: " + request.getOperation());
            }
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error performing vector operations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(Map.of("error", "Failed to perform vector operations: " + e.getMessage()), 500)
            );
        }
    }
    
    // ==================== REQUEST DTOs ====================
    
    public static class PostStateUpdateRequest {
        @NotNull(message = "User ID is required")
        private UUID userId;
        
        @NotBlank(message = "Post content is required")
        private String postContent;
        
        @NotNull(message = "Post ID is required")
        private UUID postId;
        
        // Getters and setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public String getPostContent() { return postContent; }
        public void setPostContent(String postContent) { this.postContent = postContent; }
        
        public UUID getPostId() { return postId; }
        public void setPostId(UUID postId) { this.postId = postId; }
    }
    
    public static class InteractionStateUpdateRequest {
        @NotNull(message = "User ID is required")
        private UUID userId;
        
        @NotNull(message = "Target post ID is required")
        private UUID targetPostId;
        
        @NotBlank(message = "Interaction type is required")
        private String interactionType;
        
        @NotNull(message = "Interaction weight is required")
        private Double interactionWeight;
        
        // Getters and setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public UUID getTargetPostId() { return targetPostId; }
        public void setTargetPostId(UUID targetPostId) { this.targetPostId = targetPostId; }
        
        public String getInteractionType() { return interactionType; }
        public void setInteractionType(String interactionType) { this.interactionType = interactionType; }
        
        public Double getInteractionWeight() { return interactionWeight; }
        public void setInteractionWeight(Double interactionWeight) { this.interactionWeight = interactionWeight; }
    }
    
    public static class StateSimilarityRequest {
        @NotNull(message = "User ID is required")
        private UUID userId;
        
        @NotNull(message = "State 1 is required")
        private List<Double> state1;
        
        @NotNull(message = "State 2 is required")
        private List<Double> state2;
        
        // Getters and setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public List<Double> getState1() { return state1; }
        public void setState1(List<Double> state1) { this.state1 = state1; }
        
        public List<Double> getState2() { return state2; }
        public void setState2(List<Double> state2) { this.state2 = state2; }
    }
    
    public static class VectorOperationsRequest {
        @NotNull(message = "User ID is required")
        private UUID userId;
        
        @NotBlank(message = "Operation is required")
        private String operation;
        
        @NotNull(message = "Vector 1 is required")
        private List<Double> vector1;
        
        private List<Double> vector2;
        private Double scalar;
        
        // Getters and setters
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        
        public List<Double> getVector1() { return vector1; }
        public void setVector1(List<Double> vector1) { this.vector1 = vector1; }
        
        public List<Double> getVector2() { return vector2; }
        public void setVector2(List<Double> vector2) { this.vector2 = vector2; }
        
        public Double getScalar() { return scalar; }
        public void setScalar(Double scalar) { this.scalar = scalar; }
    }
}

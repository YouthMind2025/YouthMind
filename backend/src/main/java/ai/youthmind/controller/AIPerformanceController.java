package ai.youthmind.controller;

import ai.youthmind.dto.AIChatRequest;
import ai.youthmind.dto.AIChatResponse;
import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.AsyncTaskResponse;
import ai.youthmind.service.AIChatService;
import ai.youthmind.service.AIAsyncProcessingService;
import ai.youthmind.service.AIDatabaseOptimizationService;
import ai.youthmind.service.AIRequestBatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for AI performance optimization features
 */
@RestController
@RequestMapping("/api/ai/performance")
public class AIPerformanceController {
    
    private static final Logger logger = LoggerFactory.getLogger(AIPerformanceController.class);
    
    @Autowired
    private AIChatService aiChatService;
    
    @Autowired
    private AIAsyncProcessingService asyncProcessingService;
    
    @Autowired
    private AIDatabaseOptimizationService databaseOptimizationService;
    
    @Autowired
    private AIRequestBatchingService requestBatchingService;
    
    /**
     * Process AI chat request asynchronously and return taskId immediately
     */
    @PostMapping("/chat/async")
    public ResponseEntity<ApiResponse<AsyncTaskResponse<AIChatResponse>>> processChatAsync(
            @RequestBody AIChatRequest request,
            @RequestHeader("X-User-ID") UUID userId) {
        
        try {
            logger.info("Starting async AI chat request for user: {}", userId);
            
            // Start async processing and get taskId immediately
            String taskId = asyncProcessingService.startChatRequestAsync(request, userId);
            
            // Return taskId immediately so client can track progress
            AsyncTaskResponse<AIChatResponse> taskResponse = AsyncTaskResponse.processing(
                taskId,
                "AI chat request is being processed. Use the taskId to check status."
            );
            
            ApiResponse<AsyncTaskResponse<AIChatResponse>> apiResponse = ApiResponse.<AsyncTaskResponse<AIChatResponse>>builder()
                .success(true)
                .data(taskResponse)
                .message("Async task started successfully")
                .build();
            
            return ResponseEntity.ok(apiResponse);
                
        } catch (Exception e) {
            logger.error("Error initiating async AI chat request: {}", e.getMessage(), e);
            
            AsyncTaskResponse<AIChatResponse> errorTask = AsyncTaskResponse.failed(
                null,
                "Error initiating AI chat request: " + e.getMessage()
            );
            
            ApiResponse<AsyncTaskResponse<AIChatResponse>> errorResponse = ApiResponse.<AsyncTaskResponse<AIChatResponse>>builder()
                .success(false)
                .data(errorTask)
                .message("Error initiating AI chat request: " + e.getMessage())
                .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Process AI chat request with batching
     */
    @PostMapping("/chat/batch")
    public CompletableFuture<ResponseEntity<ApiResponse<String>>> processChatBatch(
            @RequestBody AIChatRequest request,
            @RequestHeader("X-User-ID") UUID userId) {
        
        try {
            logger.info("Processing batched AI chat request for user: {}", userId);
            
            return aiChatService.processBatchedChatRequest(request, userId)
                .thenApply(response -> {
                    ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                        .success(true)
                        .data(response)
                        .message("AI chat processed successfully with batching")
                        .build();
                    
                    return ResponseEntity.ok(apiResponse);
                })
                .exceptionally(throwable -> {
                    logger.error("Error processing batched AI chat request: {}", throwable.getMessage());
                    ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                        .success(false)
                        .message("Error processing batched AI chat request: " + throwable.getMessage())
                        .build();
                    
                    return ResponseEntity.internalServerError().body(errorResponse);
                });
                
        } catch (Exception e) {
            logger.error("Error initiating batched AI chat request: {}", e.getMessage(), e);
            ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                .success(false)
                .message("Error initiating batched AI chat request: " + e.getMessage())
                .build();
            
            return CompletableFuture.completedFuture(ResponseEntity.internalServerError().body(errorResponse));
        }
    }
    
    /**
     * Get performance statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPerformanceStatistics() {
        try {
            logger.info("Getting AI performance statistics");
            
            Map<String, Object> stats = aiChatService.getPerformanceStatistics();
            
            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .data(stats)
                .message("Performance statistics retrieved successfully")
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting performance statistics: {}", e.getMessage(), e);
            ApiResponse<Map<String, Object>> errorResponse = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Error retrieving performance statistics: " + e.getMessage())
                .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Get task status
     */
    @GetMapping("/task/{taskId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTaskStatus(@PathVariable String taskId) {
        try {
            logger.info("Getting task status for: {}", taskId);
            
            Map<String, Object> status = asyncProcessingService.getTaskStatus(taskId);
            
            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .data(status)
                .message("Task status retrieved successfully")
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting task status: {}", e.getMessage(), e);
            ApiResponse<Map<String, Object>> errorResponse = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Error retrieving task status: " + e.getMessage())
                .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Get task result
     */
    @GetMapping("/task/{taskId}/result")
    public ResponseEntity<ApiResponse<Object>> getTaskResult(@PathVariable String taskId) {
        try {
            logger.info("Getting task result for: {}", taskId);
            
            Object result = asyncProcessingService.getTaskResult(taskId);
            
            ApiResponse<Object> response = ApiResponse.<Object>builder()
                .success(true)
                .data(result)
                .message("Task result retrieved successfully")
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting task result: {}", e.getMessage(), e);
            ApiResponse<Object> errorResponse = ApiResponse.<Object>builder()
                .success(false)
                .message("Error retrieving task result: " + e.getMessage())
                .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Cancel a task
     */
    @PostMapping("/task/{taskId}/cancel")
    public ResponseEntity<ApiResponse<Boolean>> cancelTask(@PathVariable String taskId) {
        try {
            logger.info("Cancelling task: {}", taskId);
            
            boolean cancelled = asyncProcessingService.cancelTask(taskId);
            
            ApiResponse<Boolean> response = ApiResponse.<Boolean>builder()
                .success(true)
                .data(cancelled)
                .message(cancelled ? "Task cancelled successfully" : "Task could not be cancelled")
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error cancelling task: {}", e.getMessage(), e);
            ApiResponse<Boolean> errorResponse = ApiResponse.<Boolean>builder()
                .success(false)
                .message("Error cancelling task: " + e.getMessage())
                .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Get cache statistics
     */
    @GetMapping("/cache/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStatistics() {
        try {
            logger.info("Getting cache statistics");
            
            Map<String, Object> stats = databaseOptimizationService.getCacheStatistics();
            
            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .data(stats)
                .message("Cache statistics retrieved successfully")
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting cache statistics: {}", e.getMessage(), e);
            ApiResponse<Map<String, Object>> errorResponse = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Error retrieving cache statistics: " + e.getMessage())
                .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Clear user cache
     */
    @PostMapping("/cache/user/{userId}/clear")
    public ResponseEntity<ApiResponse<String>> clearUserCache(@PathVariable UUID userId) {
        try {
            logger.info("Clearing cache for user: {}", userId);
            
            databaseOptimizationService.clearUserCache(userId);
            
            ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .data("Cache cleared for user: " + userId)
                .message("User cache cleared successfully")
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error clearing user cache: {}", e.getMessage(), e);
            ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                .success(false)
                .message("Error clearing user cache: " + e.getMessage())
                .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Clear conversation cache
     */
    @PostMapping("/cache/conversation/{conversationId}/clear")
    public ResponseEntity<ApiResponse<String>> clearConversationCache(@PathVariable UUID conversationId) {
        try {
            logger.info("Clearing cache for conversation: {}", conversationId);
            
            databaseOptimizationService.clearConversationCache(conversationId);
            
            ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .data("Cache cleared for conversation: " + conversationId)
                .message("Conversation cache cleared successfully")
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error clearing conversation cache: {}", e.getMessage(), e);
            ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                .success(false)
                .message("Error clearing conversation cache: " + e.getMessage())
                .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Get batch statistics
     */
    @GetMapping("/batch/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBatchStatistics() {
        try {
            logger.info("Getting batch statistics");
            
            Map<String, Object> stats = requestBatchingService.getBatchStatistics();
            
            ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .data(stats)
                .message("Batch statistics retrieved successfully")
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting batch statistics: {}", e.getMessage(), e);
            ApiResponse<Map<String, Object>> errorResponse = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message("Error retrieving batch statistics: " + e.getMessage())
                .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}

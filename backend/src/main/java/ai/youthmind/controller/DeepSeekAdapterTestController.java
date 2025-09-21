package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.ModerationResult;
import ai.youthmind.service.DeepSeekAdapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for testing DeepSeek Adapter Service functionality.
 * Tests retry logic, streaming, caching, batch processing, and error handling.
 */
@RestController
@RequestMapping("/v1/test/deepseek-adapter")
@Tag(name = "DeepSeek Adapter Test", description = "API for testing DeepSeek Adapter Service")
public class DeepSeekAdapterTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekAdapterTestController.class);
    
    @Autowired
    private DeepSeekAdapterService deepSeekAdapterService;
    
    /**
     * Test retry logic with exponential backoff
     */
    @PostMapping("/test-retry")
    @Operation(summary = "Test retry logic", 
               description = "Tests retry logic with exponential backoff and jitter")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testRetryLogic(
        @RequestParam @Parameter(description = "Content to moderate", required = true) String content,
        @RequestParam(defaultValue = "test") @Parameter(description = "Content type") String contentType,
        @RequestParam(required = false) @Parameter(description = "User ID for testing") UUID userId
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            long startTime = System.currentTimeMillis();
            ModerationResult moderationResult = deepSeekAdapterService.moderateContentWithRetry(
                content, contentType, userId, null);
            long endTime = System.currentTimeMillis();
            
            result.put("moderationResult", moderationResult);
            result.put("responseTimeMs", endTime - startTime);
            result.put("content", content);
            result.put("contentType", contentType);
            result.put("userId", userId);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Retry test failed: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Retry test failed: " + e.getMessage());
            errorResult.put("exceptionType", e.getClass().getSimpleName());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Test streaming response handling
     */
    @PostMapping("/test-streaming")
    @Operation(summary = "Test streaming responses", 
               description = "Tests streaming response handling")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testStreaming(
        @RequestParam @Parameter(description = "Content to moderate", required = true) String content,
        @RequestParam(defaultValue = "test") @Parameter(description = "Content type") String contentType,
        @RequestParam(required = false) @Parameter(description = "User ID for testing") UUID userId
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            long startTime = System.currentTimeMillis();
            CompletableFuture<ModerationResult> future = deepSeekAdapterService.moderateContentStreaming(
                content, contentType, userId);
            
            // Wait for completion with timeout
            ModerationResult moderationResult = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            
            result.put("moderationResult", moderationResult);
            result.put("responseTimeMs", endTime - startTime);
            result.put("content", content);
            result.put("contentType", contentType);
            result.put("userId", userId);
            result.put("streamingCompleted", true);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Streaming test failed: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Streaming test failed: " + e.getMessage());
            errorResult.put("exceptionType", e.getClass().getSimpleName());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Test batch request processing
     */
    @PostMapping("/test-batch")
    @Operation(summary = "Test batch processing", 
               description = "Tests batch request processing")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testBatchProcessing(
        @RequestBody @Parameter(description = "List of content to moderate", required = true) 
        List<String> contentList,
        @RequestParam(defaultValue = "test") @Parameter(description = "Content type") String contentType,
        @RequestParam(required = false) @Parameter(description = "User ID for testing") UUID userId
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Create batch requests
            List<DeepSeekAdapterService.BatchModerationRequest> batchRequests = new ArrayList<>();
            for (String content : contentList) {
                batchRequests.add(new DeepSeekAdapterService.BatchModerationRequest(
                    content, contentType, userId, null));
            }
            
            long startTime = System.currentTimeMillis();
            List<ModerationResult> batchResults = deepSeekAdapterService.moderateContentBatch(batchRequests);
            long endTime = System.currentTimeMillis();
            
            result.put("batchResults", batchResults);
            result.put("requestCount", contentList.size());
            result.put("resultCount", batchResults.size());
            result.put("responseTimeMs", endTime - startTime);
            result.put("contentType", contentType);
            result.put("userId", userId);
            result.put("batchProcessingCompleted", true);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Batch test failed: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Batch test failed: " + e.getMessage());
            errorResult.put("exceptionType", e.getClass().getSimpleName());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Test timeout handling
     */
    @PostMapping("/test-timeout")
    @Operation(summary = "Test timeout handling", 
               description = "Tests timeout handling with 800ms default")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testTimeoutHandling(
        @RequestParam @Parameter(description = "Content to moderate", required = true) String content,
        @RequestParam(defaultValue = "test") @Parameter(description = "Content type") String contentType,
        @RequestParam(required = false) @Parameter(description = "User ID for testing") UUID userId
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            long startTime = System.currentTimeMillis();
            ModerationResult moderationResult = deepSeekAdapterService.moderateContentWithRetry(
                content, contentType, userId, null);
            long endTime = System.currentTimeMillis();
            
            long responseTime = endTime - startTime;
            boolean withinTimeout = responseTime <= 800; // 800ms timeout
            
            result.put("moderationResult", moderationResult);
            result.put("responseTimeMs", responseTime);
            result.put("withinTimeout", withinTimeout);
            result.put("timeoutLimitMs", 800);
            result.put("content", content);
            result.put("contentType", contentType);
            result.put("userId", userId);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Timeout test failed: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Timeout test failed: " + e.getMessage());
            errorResult.put("exceptionType", e.getClass().getSimpleName());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Test error handling and validation
     */
    @PostMapping("/test-error-handling")
    @Operation(summary = "Test error handling", 
               description = "Tests comprehensive error handling and response validation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testErrorHandling(
        @RequestParam @Parameter(description = "Content to moderate", required = true) String content,
        @RequestParam(defaultValue = "test") @Parameter(description = "Content type") String contentType,
        @RequestParam(required = false) @Parameter(description = "User ID for testing") UUID userId,
        @RequestParam(defaultValue = "false") @Parameter(description = "Simulate error") boolean simulateError
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            if (simulateError) {
                // Test with invalid content to trigger error handling
                content = null;
            }
            
            long startTime = System.currentTimeMillis();
            ModerationResult moderationResult = deepSeekAdapterService.moderateContentWithRetry(
                content, contentType, userId, null);
            long endTime = System.currentTimeMillis();
            
            result.put("moderationResult", moderationResult);
            result.put("responseTimeMs", endTime - startTime);
            result.put("content", content);
            result.put("contentType", contentType);
            result.put("userId", userId);
            result.put("simulateError", simulateError);
            result.put("errorHandlingTestCompleted", true);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error handling test failed: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Error handling test failed: " + e.getMessage());
            errorResult.put("exceptionType", e.getClass().getSimpleName());
            errorResult.put("simulateError", simulateError);
            errorResult.put("errorHandlingWorking", true); // Error was caught and handled
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Test caching layer
     */
    @PostMapping("/test-caching")
    @Operation(summary = "Test caching layer", 
               description = "Tests caching layer for API responses")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testCaching(
        @RequestParam @Parameter(description = "Content to moderate", required = true) String content,
        @RequestParam(defaultValue = "test") @Parameter(description = "Content type") String contentType,
        @RequestParam(required = false) @Parameter(description = "User ID for testing") UUID userId
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // First request (should miss cache)
            long startTime1 = System.currentTimeMillis();
            ModerationResult result1 = deepSeekAdapterService.moderateContentWithRetry(
                content, contentType, userId, null);
            long endTime1 = System.currentTimeMillis();
            
            // Second request (should hit cache)
            long startTime2 = System.currentTimeMillis();
            ModerationResult result2 = deepSeekAdapterService.moderateContentWithRetry(
                content, contentType, userId, null);
            long endTime2 = System.currentTimeMillis();
            
            long firstRequestTime = endTime1 - startTime1;
            long secondRequestTime = endTime2 - startTime2;
            boolean cacheHit = secondRequestTime < firstRequestTime;
            
            result.put("firstRequest", Map.of(
                "result", result1,
                "responseTimeMs", firstRequestTime
            ));
            result.put("secondRequest", Map.of(
                "result", result2,
                "responseTimeMs", secondRequestTime
            ));
            result.put("cacheHit", cacheHit);
            result.put("performanceImprovement", firstRequestTime - secondRequestTime);
            result.put("content", content);
            result.put("contentType", contentType);
            result.put("userId", userId);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Caching test failed: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Caching test failed: " + e.getMessage());
            errorResult.put("exceptionType", e.getClass().getSimpleName());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Comprehensive integration test
     */
    @PostMapping("/integration-test")
    @Operation(summary = "Comprehensive integration test", 
               description = "Runs comprehensive test of all adapter service features")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runIntegrationTest(
        @RequestParam(defaultValue = "This is a comprehensive test of the DeepSeek Adapter Service") 
        @Parameter(description = "Test content") String content,
        @RequestParam(required = false) @Parameter(description = "User ID for testing") UUID userId
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            UUID testUserId = userId != null ? userId : UUID.randomUUID();
            
            // 1. Test retry logic
            long startTime = System.currentTimeMillis();
            ModerationResult retryResult = deepSeekAdapterService.moderateContentWithRetry(
                content, "integration-test", testUserId, null);
            long retryTime = System.currentTimeMillis() - startTime;
            
            result.put("retryTest", Map.of(
                "result", retryResult,
                "responseTimeMs", retryTime
            ));
            
            // 2. Test streaming
            startTime = System.currentTimeMillis();
            CompletableFuture<ModerationResult> streamingFuture = deepSeekAdapterService.moderateContentStreaming(
                content, "integration-test", testUserId);
            ModerationResult streamingResult = streamingFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
            long streamingTime = System.currentTimeMillis() - startTime;
            
            result.put("streamingTest", Map.of(
                "result", streamingResult,
                "responseTimeMs", streamingTime
            ));
            
            // 3. Test batch processing
            List<DeepSeekAdapterService.BatchModerationRequest> batchRequests = Arrays.asList(
                new DeepSeekAdapterService.BatchModerationRequest(content, "integration-test", testUserId, null),
                new DeepSeekAdapterService.BatchModerationRequest(content + " (batch 2)", "integration-test", testUserId, null)
            );
            
            startTime = System.currentTimeMillis();
            List<ModerationResult> batchResults = deepSeekAdapterService.moderateContentBatch(batchRequests);
            long batchTime = System.currentTimeMillis() - startTime;
            
            result.put("batchTest", Map.of(
                "results", batchResults,
                "responseTimeMs", batchTime,
                "requestCount", batchRequests.size()
            ));
            
            // 4. Test caching
            startTime = System.currentTimeMillis();
            ModerationResult cacheResult = deepSeekAdapterService.moderateContentWithRetry(
                content, "integration-test", testUserId, null);
            long cacheTime = System.currentTimeMillis() - startTime;
            
            result.put("cacheTest", Map.of(
                "result", cacheResult,
                "responseTimeMs", cacheTime
            ));
            
            result.put("integrationTestCompleted", true);
            result.put("testUserId", testUserId);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Integration test failed: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Integration test failed: " + e.getMessage());
            errorResult.put("exceptionType", e.getClass().getSimpleName());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
}

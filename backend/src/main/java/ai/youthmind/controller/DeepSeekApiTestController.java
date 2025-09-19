package ai.youthmind.controller;

import ai.youthmind.config.DeepSeekConfig;
import ai.youthmind.dto.ApiResponse;
import ai.youthmind.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for testing DeepSeek API integration and configuration.
 * Provides comprehensive testing endpoints for all DeepSeek services.
 */
@RestController
@RequestMapping("/v1/test/deepseek")
@Tag(name = "DeepSeek API Test", description = "API for testing DeepSeek integration and configuration")
public class DeepSeekApiTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekApiTestController.class);
    
    @Autowired
    private DeepSeekConfig deepSeekConfig;
    
    @Autowired
    private DeepSeekRateLimitService rateLimitService;
    
    @Autowired
    private DeepSeekCostOptimizationService costOptimizationService;
    
    @Autowired
    private DeepSeekOffPeakRoutingService offPeakRoutingService;
    
    @Autowired
    private DeepSeekModelSelectionService modelSelectionService;
    
    @Autowired
    private DeepSeekUsageMonitoringService usageMonitoringService;
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    /**
     * Test DeepSeek API configuration
     */
    @GetMapping("/config")
    @Operation(summary = "Test DeepSeek API configuration", 
               description = "Returns current DeepSeek API configuration and validation status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testConfiguration() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // Basic configuration
            config.put("baseUrl", deepSeekConfig.getBaseUrl());
            config.put("apiKeyMasked", deepSeekConfig.getMaskedApiKey());
            config.put("timeout", deepSeekConfig.getTimeout());
            config.put("maxRetries", deepSeekConfig.getMaxRetries());
            config.put("defaultModel", deepSeekConfig.getDefaultModel());
            config.put("isValid", deepSeekConfig.isValid());
            
            // Rate limiting
            config.put("rateLimitPerMinute", deepSeekConfig.getRateLimitPerMinute());
            config.put("rateLimitBurst", deepSeekConfig.getRateLimitBurst());
            
            // Cost optimization
            config.put("cacheHitDiscount", deepSeekConfig.getCacheHitDiscount());
            config.put("enableCacheHitPricing", deepSeekConfig.isEnableCacheHitPricing());
            
            // Off-peak routing
            config.put("enableOffPeakRouting", deepSeekConfig.isEnableOffPeakRouting());
            config.put("offPeakStartTime", deepSeekConfig.getOffPeakStartTime());
            config.put("offPeakEndTime", deepSeekConfig.getOffPeakEndTime());
            config.put("offPeakTimezone", deepSeekConfig.getOffPeakTimezone());
            
            // Available models
            config.put("availableModels", deepSeekConfig.getAvailableModels());
            
            // Circuit breaker
            config.put("circuitBreakerFailureThreshold", deepSeekConfig.getCircuitBreakerFailureThreshold());
            config.put("circuitBreakerRecoveryTimeout", deepSeekConfig.getCircuitBreakerRecoveryTimeout());
            
            return ResponseEntity.ok(ApiResponse.success(config));
            
        } catch (Exception e) {
            logger.error("Error testing DeepSeek configuration: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to retrieve configuration: " + e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Test rate limiting functionality
     */
    @GetMapping("/rate-limit")
    @Operation(summary = "Test rate limiting", 
               description = "Tests rate limiting functionality and returns current status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testRateLimit(
        @RequestParam(required = false) @Parameter(description = "User ID for testing") UUID userId,
        @RequestParam(defaultValue = "moderation") @Parameter(description = "Request type") String requestType
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Test rate limit check
            DeepSeekRateLimitService.RateLimitResult rateLimitResult = 
                rateLimitService.checkRateLimit(userId != null ? userId.toString() : null, requestType);
            
            result.put("allowed", rateLimitResult.isAllowed());
            result.put("message", rateLimitResult.getMessage());
            result.put("status", rateLimitResult.getStatus());
            
            // Get current status
            DeepSeekRateLimitService.RateLimitStatus status = 
                rateLimitService.getRateLimitStatus(userId != null ? userId.toString() : null, requestType);
            
            result.put("currentStatus", status);
            
            // Get all rate limit statuses
            Map<String, DeepSeekRateLimitService.RateLimitStatus> allStatuses = 
                rateLimitService.getAllRateLimitStatus();
            result.put("allStatuses", allStatuses);
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error testing rate limit: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Rate limit test failed: " + e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Test cost optimization and caching
     */
    @GetMapping("/cost-optimization")
    @Operation(summary = "Test cost optimization", 
               description = "Tests cost optimization and caching functionality")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testCostOptimization(
        @RequestParam(defaultValue = "deepseek-chat") @Parameter(description = "Model name") String modelName,
        @RequestParam(defaultValue = "100") @Parameter(description = "Input tokens") int inputTokens,
        @RequestParam(defaultValue = "50") @Parameter(description = "Output tokens") int outputTokens,
        @RequestParam(defaultValue = "false") @Parameter(description = "Is cache hit") boolean isCacheHit
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Test cost calculation
            DeepSeekCostOptimizationService.CostOptimizationResult costResult = 
                costOptimizationService.calculateOptimizedCost(modelName, inputTokens, outputTokens, 
                    isCacheHit, UUID.randomUUID());
            
            result.put("costResult", costResult);
            
            // Test cache functionality
            String testContent = "This is a test content for caching";
            String cacheHash = costOptimizationService.generateCacheHash(testContent, modelName, 0.7);
            
            DeepSeekCostOptimizationService.CacheCheckResult cacheResult = 
                costOptimizationService.checkCache(cacheHash);
            
            result.put("cacheHash", cacheHash);
            result.put("cacheResult", cacheResult);
            
            // Store test response in cache
            Map<String, Object> testResponse = Map.of(
                "content", "This is a cached test response",
                "model", modelName,
                "timestamp", System.currentTimeMillis()
            );
            
            costOptimizationService.storeInCache(cacheHash, testResponse, 1); // 1 hour TTL
            
            // Check cache again
            DeepSeekCostOptimizationService.CacheCheckResult cacheResultAfter = 
                costOptimizationService.checkCache(cacheHash);
            
            result.put("cacheResultAfter", cacheResultAfter);
            
            // Get cost summary
            DeepSeekCostOptimizationService.CostSummary costSummary = 
                costOptimizationService.getCostSummary(null, 7); // Last 7 days
            
            result.put("costSummary", costSummary);
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error testing cost optimization: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Cost optimization test failed: " + e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Test off-peak routing
     */
    @GetMapping("/off-peak-routing")
    @Operation(summary = "Test off-peak routing", 
               description = "Tests off-peak routing functionality and current status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testOffPeakRouting(
        @RequestParam(defaultValue = "moderation") @Parameter(description = "Request type") String requestType,
        @RequestParam(required = false) @Parameter(description = "User ID for testing") UUID userId
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Test routing decision
            DeepSeekOffPeakRoutingService.RoutingDecision routingDecision = 
                offPeakRoutingService.determineOptimalRouting(requestType, userId);
            
            result.put("routingDecision", routingDecision);
            
            // Get off-peak window info
            DeepSeekOffPeakRoutingService.OffPeakWindowInfo windowInfo = 
                offPeakRoutingService.getOffPeakWindowInfo();
            
            result.put("windowInfo", windowInfo);
            
            // Test optimal model selection
            String optimalModel = offPeakRoutingService.getOptimalModel(requestType, "medium", "medium");
            result.put("optimalModel", optimalModel);
            
            // Get routing statistics
            Map<String, DeepSeekOffPeakRoutingService.RoutingMetrics> routingStats = 
                offPeakRoutingService.getRoutingStatistics();
            
            result.put("routingStatistics", routingStats);
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error testing off-peak routing: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Off-peak routing test failed: " + e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Test model selection
     */
    @GetMapping("/model-selection")
    @Operation(summary = "Test model selection", 
               description = "Tests intelligent model selection functionality")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testModelSelection(
        @RequestParam(defaultValue = "moderation") @Parameter(description = "Request type") String requestType,
        @RequestParam(defaultValue = "medium") @Parameter(description = "Complexity level") String complexity,
        @RequestParam(defaultValue = "medium") @Parameter(description = "Urgency level") String urgency,
        @RequestParam(defaultValue = "medium") @Parameter(description = "Cost sensitivity") String costSensitivity,
        @RequestParam(required = false) @Parameter(description = "User ID for testing") UUID userId
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Test model selection
            DeepSeekModelSelectionService.ModelSelectionResult selectionResult = 
                modelSelectionService.selectOptimalModel(requestType, complexity, urgency, costSensitivity, userId);
            
            result.put("selectionResult", selectionResult);
            
            // Get model recommendations
            var recommendations = modelSelectionService.getModelRecommendations(requestType);
            result.put("recommendations", recommendations);
            
            // Get model performance statistics
            Map<String, DeepSeekModelSelectionService.ModelPerformanceMetrics> performanceStats = 
                modelSelectionService.getModelPerformanceStatistics();
            
            result.put("performanceStatistics", performanceStats);
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error testing model selection: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Model selection test failed: " + e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Test usage monitoring
     */
    @GetMapping("/usage-monitoring")
    @Operation(summary = "Test usage monitoring", 
               description = "Tests usage monitoring and cost tracking functionality")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testUsageMonitoring(
        @RequestParam(required = false) @Parameter(description = "User ID for testing") UUID userId,
        @RequestParam(defaultValue = "7") @Parameter(description = "Days to look back") int days
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Test usage statistics
            DeepSeekUsageMonitoringService.UsageStatistics usageStats = 
                usageMonitoringService.getUsageStatistics(userId, null, days);
            
            result.put("usageStatistics", usageStats);
            
            // Test cost analysis
            DeepSeekUsageMonitoringService.CostAnalysis costAnalysis = 
                usageMonitoringService.getCostAnalysis(userId, days);
            
            result.put("costAnalysis", costAnalysis);
            
            // Test performance metrics
            DeepSeekUsageMonitoringService.PerformanceMetrics performanceMetrics = 
                usageMonitoringService.getPerformanceMetrics(userId, days);
            
            result.put("performanceMetrics", performanceMetrics);
            
            // Get real-time metrics
            Map<String, DeepSeekUsageMonitoringService.UsageMetrics> realtimeMetrics = 
                usageMonitoringService.getAllRealtimeMetrics();
            
            result.put("realtimeMetrics", realtimeMetrics);
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error testing usage monitoring: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Usage monitoring test failed: " + e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Test actual DeepSeek API connectivity
     */
    @PostMapping("/api-connectivity")
    @Operation(summary = "Test DeepSeek API connectivity", 
               description = "Tests actual connectivity to DeepSeek API with a simple request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testApiConnectivity(
        @RequestParam(defaultValue = "Hello, how are you?") @Parameter(description = "Test message") String message,
        @RequestParam(defaultValue = "deepseek-chat") @Parameter(description = "Model to test") String model,
        @RequestParam(required = false) @Parameter(description = "User ID for testing") UUID userId
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            long startTime = System.currentTimeMillis();
            
            // Test AI service client
            var moderationResult = aiServiceClient.moderateContent(message, "test", userId, null);
            
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            
            result.put("moderationResult", moderationResult);
            result.put("responseTimeMs", responseTime);
            result.put("model", model);
            result.put("message", message);
            
            // Record usage for monitoring
            usageMonitoringService.recordUsage(userId, model, "test", 
                message.length() / 4, // Rough token estimate
                moderationResult.getReason() != null ? moderationResult.getReason().length() / 4 : 10,
                responseTime, true, 0.001, false); // Small test cost
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error testing API connectivity: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "API connectivity test failed: " + e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Comprehensive integration test
     */
    @PostMapping("/integration-test")
    @Operation(summary = "Comprehensive integration test", 
               description = "Runs a comprehensive test of all DeepSeek services")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runIntegrationTest(
        @RequestParam(defaultValue = "This is a comprehensive test of the DeepSeek integration") 
        @Parameter(description = "Test content") String content,
        @RequestParam(required = false) @Parameter(description = "User ID for testing") UUID userId
    ) {
        try {
            Map<String, Object> result = new HashMap<>();
            UUID testUserId = userId != null ? userId : UUID.randomUUID();
            
            // 1. Test configuration
            Map<String, Object> configTest = new HashMap<>();
            configTest.put("isValid", deepSeekConfig.isValid());
            configTest.put("availableModels", deepSeekConfig.getAvailableModels());
            result.put("configuration", configTest);
            
            // 2. Test rate limiting
            DeepSeekRateLimitService.RateLimitResult rateLimitResult = 
                rateLimitService.checkRateLimit(testUserId.toString(), "integration-test");
            result.put("rateLimit", rateLimitResult);
            
            // 3. Test model selection
            DeepSeekModelSelectionService.ModelSelectionResult modelSelection = 
                modelSelectionService.selectOptimalModel("moderation", "medium", "medium", "medium", testUserId);
            result.put("modelSelection", modelSelection);
            
            // 4. Test off-peak routing
            DeepSeekOffPeakRoutingService.RoutingDecision routingDecision = 
                offPeakRoutingService.determineOptimalRouting("moderation", testUserId);
            result.put("routingDecision", routingDecision);
            
            // 5. Test cost optimization
            String cacheHash = costOptimizationService.generateCacheHash(content, modelSelection.getSelectedModel(), 0.7);
            DeepSeekCostOptimizationService.CacheCheckResult cacheResult = 
                costOptimizationService.checkCache(cacheHash);
            result.put("cacheCheck", cacheResult);
            
            // 6. Test actual API call
            long startTime = System.currentTimeMillis();
            var moderationResult = aiServiceClient.moderateContent(content, "integration-test", testUserId, null);
            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            
            result.put("apiResponse", moderationResult);
            result.put("responseTimeMs", responseTime);
            
            // 7. Record usage
            usageMonitoringService.recordUsage(testUserId, modelSelection.getSelectedModel(), "integration-test",
                content.length() / 4,
                moderationResult.getReason() != null ? moderationResult.getReason().length() / 4 : 10,
                responseTime, true, 0.001, cacheResult.isCacheHit());
            
            // 8. Test usage monitoring
            DeepSeekUsageMonitoringService.UsageStatistics usageStats = 
                usageMonitoringService.getUsageStatistics(testUserId, null, 1);
            result.put("usageStatistics", usageStats);
            
            result.put("testCompleted", true);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error running integration test: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Integration test failed: " + e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
    
    /**
     * Reset all test data
     */
    @PostMapping("/reset")
    @Operation(summary = "Reset test data", 
               description = "Resets all test data and metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetTestData() {
        try {
            // Reset rate limits
            rateLimitService.resetRateLimits();
            
            // Clear caches
            costOptimizationService.clearAllCaches();
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "All test data has been reset");
            result.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(ApiResponse.success(result));
            
        } catch (Exception e) {
            logger.error("Error resetting test data: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Failed to reset test data: " + e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(errorResult, 500));
        }
    }
}

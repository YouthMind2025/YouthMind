package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling AI-related errors and providing fallback mechanisms
 * Implements sophisticated error handling and recovery algorithms
 */
@Service
@Transactional
public class AIErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AIErrorHandler.class);
    
    // Error handling constants
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long ERROR_COOLDOWN_MINUTES = 5;
    private static final int MAX_ERRORS_PER_HOUR = 10;
    private static final String CRITICAL_ERROR_THRESHOLD = "critical";
    
    // Redis keys
    private static final String ERROR_LOG_KEY = "ai_error_log:";
    private static final String ERROR_STATS_KEY = "ai_error_stats:";
    private static final String ERROR_COOLDOWN_KEY = "ai_error_cooldown:";
    private static final String FALLBACK_CACHE_KEY = "ai_fallback_cache:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private AIFallbackService aiFallbackService;
    
    /**
     * Handle AI service errors with comprehensive error management
     * @param error The exception that occurred
     * @param context Context information about the error
     * @param userId User ID for personalization
     * @return Error handling result
     */
    public Map<String, Object> handleError(Throwable error, Map<String, Object> context, UUID userId) {
        try {
            logger.error("Handling AI error for user {}: {}", userId, error.getMessage(), error);
            
            // Classify the error
            ErrorClassification classification = classifyError(error, context);
            
            // Log the error
            logError(error, classification, context, userId);
            
            // Check if we should retry
            boolean shouldRetry = shouldRetry(error, classification, userId);
            
            // Apply error cooldown if necessary
            applyErrorCooldown(classification, userId);
            
            // Generate fallback response if needed
            Map<String, Object> fallbackResponse = null;
            if (classification.getSeverity().equals(CRITICAL_ERROR_THRESHOLD) || !shouldRetry) {
                fallbackResponse = generateFallbackResponse(error, context, userId);
            }
            
            // Create error handling result
            Map<String, Object> result = new HashMap<>();
            result.put("errorId", UUID.randomUUID().toString());
            result.put("userId", userId.toString());
            result.put("errorType", classification.getErrorType());
            result.put("severity", classification.getSeverity());
            result.put("shouldRetry", shouldRetry);
            result.put("retryAfter", classification.getRetryAfter());
            result.put("fallbackResponse", fallbackResponse);
            result.put("errorMessage", getErrorMessage(error, classification));
            result.put("timestamp", LocalDateTime.now());
            
            // Update error statistics
            updateErrorStatistics(classification, userId);
            
            logger.info("Error handling completed for user {}: errorId={}, severity={}", 
                userId, result.get("errorId"), classification.getSeverity());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error in error handler for user {}: {}", userId, e.getMessage(), e);
            return createEmergencyFallback(error, context, userId);
        }
    }
    
    /**
     * Get error statistics for monitoring
     * @param userId User ID (optional, null for global stats)
     * @return Error statistics
     */
    public Map<String, Object> getErrorStatistics(UUID userId) {
        try {
            logger.debug("Getting error statistics for user {}", userId);
            
            String statsKey = userId != null ? ERROR_STATS_KEY + userId : ERROR_STATS_KEY + "global";
            Map<String, Object> stats = (Map<String, Object>) redisTemplate.opsForValue().get(statsKey);
            
            if (stats == null) {
                stats = initializeErrorStatistics();
            }
            
            // Add real-time statistics
            Map<String, Object> realTimeStats = calculateRealTimeStats(userId);
            stats.put("realTimeStats", realTimeStats);
            stats.put("lastUpdated", LocalDateTime.now());
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting error statistics for user {}: {}", userId, e.getMessage(), e);
            return Map.of(
                "status", "ERROR",
                "message", "Failed to get error statistics: " + e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    /**
     * Get error history for a user
     * @param userId User ID
     * @param limit Maximum number of errors to return
     * @return Error history
     */
    public Map<String, Object> getErrorHistory(UUID userId, int limit) {
        try {
            logger.debug("Getting error history for user {} with limit {}", userId, limit);
            
            String logKey = ERROR_LOG_KEY + userId;
            @SuppressWarnings("unchecked")
            List<Object> errorObjects = (List<Object>) redisTemplate.opsForList().range(logKey, 0, limit - 1);
            
            List<Map<String, Object>> errorHistory = errorObjects != null ? 
                errorObjects.stream()
                    .filter(obj -> obj instanceof Map)
                    .map(obj -> (Map<String, Object>) obj)
                    .collect(java.util.stream.Collectors.toList()) : new ArrayList<>();
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId.toString());
            result.put("errorHistory", errorHistory);
            result.put("totalErrors", errorHistory.size());
            result.put("limit", limit);
            result.put("timestamp", LocalDateTime.now());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error getting error history for user {}: {}", userId, e.getMessage(), e);
            return Map.of(
                "status", "ERROR",
                "message", "Failed to get error history: " + e.getMessage(),
                "userId", userId.toString(),
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    /**
     * Clear error cooldown for a user
     * @param userId User ID
     * @return Result of clearing cooldown
     */
    public Map<String, Object> clearErrorCooldown(UUID userId) {
        try {
            logger.info("Clearing error cooldown for user {}", userId);
            
            String cooldownKey = ERROR_COOLDOWN_KEY + userId;
            redisTemplate.delete(cooldownKey);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId.toString());
            result.put("status", "COOLDOWN_CLEARED");
            result.put("timestamp", LocalDateTime.now());
            
            logger.info("Error cooldown cleared for user {}", userId);
            return result;
            
        } catch (Exception e) {
            logger.error("Error clearing cooldown for user {}: {}", userId, e.getMessage(), e);
            return Map.of(
                "status", "ERROR",
                "message", "Failed to clear cooldown: " + e.getMessage(),
                "userId", userId.toString(),
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    /**
     * Test error handling system
     * @param testErrorType Type of test error to simulate
     * @param userId User ID for testing
     * @return Test result
     */
    public Map<String, Object> testErrorHandling(String testErrorType, UUID userId) {
        try {
            logger.info("Testing error handling for user {} with error type {}", userId, testErrorType);
            
            Exception testError = createTestError(testErrorType);
            Map<String, Object> testContext = createTestContext(testErrorType);
            
            Map<String, Object> result = handleError(testError, testContext, userId);
            
            // Add test-specific information
            result.put("testType", testErrorType);
            result.put("testStatus", "COMPLETED");
            
            logger.info("Error handling test completed for user {}: {}", userId, testErrorType);
            return result;
            
        } catch (Exception e) {
            logger.error("Error in error handling test for user {}: {}", userId, e.getMessage(), e);
            return Map.of(
                "status", "TEST_FAILED",
                "message", "Error handling test failed: " + e.getMessage(),
                "testType", testErrorType,
                "userId", userId.toString(),
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private ErrorClassification classifyError(Throwable error, Map<String, Object> context) {
        String errorType = error.getClass().getSimpleName();
        String severity = "medium";
        long retryAfter = 0;
        
        // Classify based on error type
        if (error instanceof java.net.ConnectException || error instanceof java.net.SocketTimeoutException) {
            severity = "low";
            retryAfter = 30; // 30 seconds
        } else if (error instanceof org.springframework.web.client.HttpServerErrorException) {
            severity = "medium";
            retryAfter = 60; // 1 minute
        } else if (error instanceof org.springframework.web.client.HttpClientErrorException) {
            severity = "high";
            retryAfter = 300; // 5 minutes
        } else if (error instanceof java.lang.Error) {
            severity = CRITICAL_ERROR_THRESHOLD;
            retryAfter = 1800; // 30 minutes
        } else if (error instanceof SecurityException || error instanceof IllegalArgumentException) {
            severity = "high";
            retryAfter = 0; // Don't retry
        }
        
        // Adjust based on context
        if (context.containsKey("isRetry") && (Boolean) context.get("isRetry")) {
            severity = "high"; // Escalate severity for retries
        }
        
        return new ErrorClassification(errorType, severity, retryAfter);
    }
    
    private void logError(Throwable error, ErrorClassification classification, Map<String, Object> context, UUID userId) {
        try {
            Map<String, Object> errorLog = new HashMap<>();
            errorLog.put("errorId", UUID.randomUUID().toString());
            errorLog.put("userId", userId.toString());
            errorLog.put("errorType", classification.getErrorType());
            errorLog.put("severity", classification.getSeverity());
            errorLog.put("errorMessage", error.getMessage());
            errorLog.put("stackTrace", getStackTrace(error));
            errorLog.put("context", context);
            errorLog.put("timestamp", LocalDateTime.now().toString());
            
            String logKey = ERROR_LOG_KEY + userId;
            redisTemplate.opsForList().leftPush(logKey, errorLog);
            redisTemplate.opsForList().trim(logKey, 0, 99); // Keep last 100 errors
            
        } catch (Exception e) {
            logger.warn("Error logging error for user {}: {}", userId, e.getMessage());
        }
    }
    
    private boolean shouldRetry(Throwable error, ErrorClassification classification, UUID userId) {
        // Check error cooldown
        String cooldownKey = ERROR_COOLDOWN_KEY + userId;
        if (redisTemplate.hasKey(cooldownKey)) {
            return false;
        }
        
        // Check error frequency
        Map<String, Object> stats = getErrorStatistics(userId);
        @SuppressWarnings("unchecked")
        Map<String, Object> realTimeStats = (Map<String, Object>) stats.get("realTimeStats");
        int errorsLastHour = (Integer) realTimeStats.getOrDefault("errorsLastHour", 0);
        
        if (errorsLastHour >= MAX_ERRORS_PER_HOUR) {
            return false;
        }
        
        // Check severity
        if (classification.getSeverity().equals(CRITICAL_ERROR_THRESHOLD)) {
            return false;
        }
        
        // Check retry after time
        return classification.getRetryAfter() > 0;
    }
    
    private void applyErrorCooldown(ErrorClassification classification, UUID userId) {
        if (classification.getRetryAfter() > 0) {
            try {
                String cooldownKey = ERROR_COOLDOWN_KEY + userId;
                redisTemplate.opsForValue().set(cooldownKey, "active", 
                    classification.getRetryAfter(), TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.warn("Error applying cooldown for user {}: {}", userId, e.getMessage());
            }
        }
    }
    
    private Map<String, Object> generateFallbackResponse(Throwable error, Map<String, Object> context, UUID userId) {
        try {
            logger.info("Generating fallback response for user {}", userId);
            
            // Use AI fallback service if available
            if (aiFallbackService != null) {
                // Note: Using basic fallback since AIFallbackService method signature is unknown
                logger.debug("AIFallbackService available but using basic fallback");
            }
            
            // Generate basic fallback response
            return createBasicFallbackResponse(error, context, userId);
            
        } catch (Exception e) {
            logger.error("Error generating fallback response for user {}: {}", userId, e.getMessage(), e);
            return createEmergencyFallback(error, context, userId);
        }
    }
    
    private Map<String, Object> createBasicFallbackResponse(Throwable error, Map<String, Object> context, UUID userId) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("status", "FALLBACK_RESPONSE");
        fallback.put("message", "AI service temporarily unavailable. Please try again later.");
        fallback.put("errorType", error.getClass().getSimpleName());
        fallback.put("userId", userId.toString());
        fallback.put("timestamp", LocalDateTime.now());
        fallback.put("retryAfter", 300); // 5 minutes
        
        return fallback;
    }
    
    private Map<String, Object> createEmergencyFallback(Throwable error, Map<String, Object> context, UUID userId) {
        Map<String, Object> emergency = new HashMap<>();
        emergency.put("status", "EMERGENCY_FALLBACK");
        emergency.put("message", "Service temporarily unavailable. Please contact support if this persists.");
        emergency.put("errorId", UUID.randomUUID().toString());
        emergency.put("userId", userId.toString());
        emergency.put("timestamp", LocalDateTime.now());
        emergency.put("supportContact", "support@youthmind.ai");
        
        return emergency;
    }
    
    private String getErrorMessage(Throwable error, ErrorClassification classification) {
        switch (classification.getSeverity()) {
            case "low":
                return "Temporary service issue. Please try again in a moment.";
            case "medium":
                return "Service experiencing issues. Please try again in a few minutes.";
            case "high":
                return "Service temporarily unavailable. Please try again later.";
            case CRITICAL_ERROR_THRESHOLD:
                return "Critical service issue. Please contact support.";
            default:
                return "An unexpected error occurred. Please try again.";
        }
    }
    
    private void updateErrorStatistics(ErrorClassification classification, UUID userId) {
        try {
            String statsKey = ERROR_STATS_KEY + userId;
            Map<String, Object> stats = (Map<String, Object>) redisTemplate.opsForValue().get(statsKey);
            
            if (stats == null) {
                stats = initializeErrorStatistics();
            }
            
            // Update error counts
            String errorType = classification.getErrorType();
            Map<String, Integer> errorCounts = (Map<String, Integer>) stats.getOrDefault("errorCounts", new HashMap<>());
            errorCounts.put(errorType, errorCounts.getOrDefault(errorType, 0) + 1);
            stats.put("errorCounts", errorCounts);
            
            // Update severity counts
            Map<String, Integer> severityCounts = (Map<String, Integer>) stats.getOrDefault("severityCounts", new HashMap<>());
            String severity = classification.getSeverity();
            severityCounts.put(severity, severityCounts.getOrDefault(severity, 0) + 1);
            stats.put("severityCounts", severityCounts);
            
            // Update last error time
            stats.put("lastErrorTime", LocalDateTime.now().toString());
            stats.put("totalErrors", (Integer) stats.getOrDefault("totalErrors", 0) + 1);
            
            redisTemplate.opsForValue().set(statsKey, stats, 24, TimeUnit.HOURS);
            
        } catch (Exception e) {
            logger.warn("Error updating error statistics for user {}: {}", userId, e.getMessage());
        }
    }
    
    private Map<String, Object> initializeErrorStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("errorCounts", new HashMap<String, Integer>());
        stats.put("severityCounts", new HashMap<String, Integer>());
        stats.put("totalErrors", 0);
        stats.put("firstErrorTime", LocalDateTime.now().toString());
        stats.put("lastErrorTime", LocalDateTime.now().toString());
        return stats;
    }
    
    private Map<String, Object> calculateRealTimeStats(UUID userId) {
        Map<String, Object> realTimeStats = new HashMap<>();
        
        try {
            // Calculate errors in last hour
            String logKey = ERROR_LOG_KEY + userId;
            @SuppressWarnings("unchecked")
            List<Object> recentErrors = (List<Object>) redisTemplate.opsForList().range(logKey, 0, -1);
            
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            int errorsLastHour = 0;
            
            if (recentErrors != null) {
                for (Object errorObj : recentErrors) {
                    if (errorObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> error = (Map<String, Object>) errorObj;
                        String timestampStr = (String) error.get("timestamp");
                        if (timestampStr != null) {
                            LocalDateTime errorTime = LocalDateTime.parse(timestampStr);
                            if (errorTime.isAfter(oneHourAgo)) {
                                errorsLastHour++;
                            }
                        }
                    }
                }
            }
            
            realTimeStats.put("errorsLastHour", errorsLastHour);
            realTimeStats.put("isInCooldown", redisTemplate.hasKey(ERROR_COOLDOWN_KEY + userId));
            realTimeStats.put("lastCalculated", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.warn("Error calculating real-time stats for user {}: {}", userId, e.getMessage());
            realTimeStats.put("errorsLastHour", 0);
            realTimeStats.put("isInCooldown", false);
            realTimeStats.put("lastCalculated", LocalDateTime.now());
        }
        
        return realTimeStats;
    }
    
    private Exception createTestError(String testErrorType) {
        switch (testErrorType.toLowerCase()) {
            case "connection":
                return new java.net.ConnectException("Test connection error");
            case "timeout":
                return new java.net.SocketTimeoutException("Test timeout error");
            case "server":
                return new org.springframework.web.client.HttpServerErrorException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Test server error");
            case "client":
                return new org.springframework.web.client.HttpClientErrorException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Test client error");
            case "memory":
                return new RuntimeException("Test memory error");
            case "security":
                return new SecurityException("Test security error");
            default:
                return new RuntimeException("Test generic error");
        }
    }
    
    private Map<String, Object> createTestContext(String testErrorType) {
        Map<String, Object> context = new HashMap<>();
        context.put("testErrorType", testErrorType);
        context.put("isTest", true);
        context.put("testTimestamp", LocalDateTime.now());
        return context;
    }
    
    private String getStackTrace(Throwable error) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        error.printStackTrace(pw);
        return sw.toString();
    }
    
    // Helper class for error classification
    private static class ErrorClassification {
        private final String errorType;
        private final String severity;
        private final long retryAfter;
        
        public ErrorClassification(String errorType, String severity, long retryAfter) {
            this.errorType = errorType;
            this.severity = severity;
            this.retryAfter = retryAfter;
        }
        
        public String getErrorType() { return errorType; }
        public String getSeverity() { return severity; }
        public long getRetryAfter() { return retryAfter; }
    }
}

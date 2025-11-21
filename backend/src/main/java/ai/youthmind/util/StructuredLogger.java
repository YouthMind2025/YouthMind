package ai.youthmind.util;

import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.*;

/**
 * Utility class for consistent structured logging across recommendation services.
 * 
 * This class provides standardized logging methods for:
 * - R vector updates with before/after values
 * - Feed block generation with timing
 * - Event publishing and processing
 * - Cache operations (hits/misses)
 * 
 * All methods use SLF4J's MDC for correlation ID propagation and
 * Logstash structured arguments for JSON formatting.
 */
public class StructuredLogger {
    
    /**
     * Log R vector update with before and after values
     */
    public static void logRVectorUpdate(Logger logger, UUID userId, 
                                       Map<String, Double> beforeVector, 
                                       Map<String, Double> afterVector,
                                       String eventType, String correlationId) {
        MDC.put("operation", "rvector_update");
        MDC.put("userId", userId.toString());
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        
        logger.info("R vector updated",
            kv("userId", userId),
            kv("eventType", eventType),
            kv("before", beforeVector),
            kv("after", afterVector),
            kv("correlationId", correlationId)
        );
        
        MDC.remove("operation");
    }
    
    /**
     * Log the start of feed block generation
     */
    public static void logBlockGenerationStart(Logger logger, UUID userId, 
                                              Integer blockNumber, Integer vectorVersion) {
        MDC.put("operation", "block_generation");
        MDC.put("userId", userId.toString());
        MDC.put("blockNumber", blockNumber.toString());
        MDC.put("vectorVersion", vectorVersion.toString());
        
        logger.info("Feed block generation started",
            kv("userId", userId),
            kv("blockNumber", blockNumber),
            kv("vectorVersion", vectorVersion),
            kv("timestamp", System.currentTimeMillis())
        );
    }
    
    /**
     * Log the completion of feed block generation with timing
     */
    public static void logBlockGenerationComplete(Logger logger, UUID userId,
                                                 Integer blockNumber, Long durationMs,
                                                 Integer postCount) {
        logger.info("Feed block generation completed",
            kv("userId", userId),
            kv("blockNumber", blockNumber),
            kv("durationMs", durationMs),
            kv("postCount", postCount),
            kv("timestamp", System.currentTimeMillis())
        );
        
        MDC.remove("operation");
        MDC.remove("blockNumber");
        MDC.remove("vectorVersion");
    }
    
    /**
     * Log event publishing to Redis Streams
     */
    public static void logEventPublished(Logger logger, String eventType, 
                                        UUID userId, String streamName,
                                        String recordId) {
        logger.info("Event published to stream",
            kv("eventType", eventType),
            kv("userId", userId),
            kv("streamName", streamName),
            kv("recordId", recordId),
            kv("timestamp", System.currentTimeMillis())
        );
    }
    
    /**
     * Log the start of event processing
     */
    public static void logEventProcessingStart(Logger logger, String eventType,
                                              UUID userId, String eventId) {
        MDC.put("operation", "event_processing");
        MDC.put("eventType", eventType);
        MDC.put("eventId", eventId);
        
        logger.info("Event processing started",
            kv("eventType", eventType),
            kv("userId", userId),
            kv("eventId", eventId)
        );
    }
    
    /**
     * Log the completion of event processing with timing
     */
    public static void logEventProcessingComplete(Logger logger, String eventType,
                                                  Long durationMs, boolean success) {
        logger.info("Event processing completed",
            kv("eventType", eventType),
            kv("durationMs", durationMs),
            kv("success", success)
        );
        
        MDC.remove("operation");
        MDC.remove("eventType");
        MDC.remove("eventId");
    }
    
    /**
     * Log cache operations (hits/misses)
     */
    public static void logCacheOperation(Logger logger, String operation,
                                        String cacheKey, boolean hit) {
        logger.debug("Cache operation",
            kv("operation", operation),
            kv("cacheKey", cacheKey),
            kv("hit", hit),
            kv("timestamp", System.currentTimeMillis())
        );
    }
    
    /**
     * Log state update with before and after values
     */
    public static void logStateUpdate(Logger logger, UUID userId, String updateType,
                                     Map<String, Double> beforeState, Map<String, Double> afterState,
                                     double alpha) {
        logger.info("State update applied",
            kv("userId", userId),
            kv("updateType", updateType),
            kv("beforeState", beforeState),
            kv("afterState", afterState),
            kv("alpha", alpha),
            kv("dimensionsChanged", calculateChangedDimensions(beforeState, afterState))
        );
    }
    
    /**
     * Log error with structured context
     */
    public static void logError(Logger logger, String operation, String errorMessage,
                               String errorType, Map<String, Object> context) {
        logger.error("Operation failed",
            kv("operation", operation),
            kv("error", errorMessage),
            kv("errorType", errorType),
            kv("context", context),
            kv("timestamp", System.currentTimeMillis())
        );
    }
    
    /**
     * Calculate the number of changed dimensions between before and after states
     */
    private static int calculateChangedDimensions(Map<String, Double> before, Map<String, Double> after) {
        if (before == null || after == null) {
            return 0;
        }
        
        int changed = 0;
        for (Map.Entry<String, Double> entry : before.entrySet()) {
            String key = entry.getKey();
            Double beforeValue = entry.getValue();
            Double afterValue = after.get(key);
            
            if (afterValue == null || Math.abs(beforeValue - afterValue) > 0.001) {
                changed++;
            }
        }
        
        return changed;
    }
}

package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.service.AIServiceClient;
import ai.youthmind.service.AIFallbackService;
import ai.youthmind.util.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for AI services
 * Provides monitoring and diagnostics for AI system health
 */
@RestController
@RequestMapping("/v1/ai/health")
@Tag(name = "AI Health", description = "AI service health monitoring and diagnostics")
public class AIHealthController {
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    @Autowired
    private AIFallbackService fallbackService;
    
    @GetMapping("/status")
    @Operation(
        summary = "Get AI service status",
        description = "Get the current status of AI services including circuit breaker state"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAIStatus() {
        try {
            Map<String, Object> status = aiServiceClient.getDetailedHealthStatus();
            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (Exception e) {
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("error", "Failed to get AI status: " + e.getMessage());
            errorStatus.put("status", "ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorStatus, 500));
        }
    }
    
    @GetMapping("/circuit-breaker")
    @Operation(
        summary = "Get circuit breaker health",
        description = "Get detailed circuit breaker health information"
    )
    public ResponseEntity<ApiResponse<CircuitBreaker.CircuitBreakerHealth>> getCircuitBreakerHealth() {
        try {
            CircuitBreaker.CircuitBreakerHealth health = aiServiceClient.getCircuitBreakerHealth();
            return ResponseEntity.ok(ApiResponse.success(health));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }
    
    @PostMapping("/circuit-breaker/reset")
    @Operation(
        summary = "Reset circuit breaker",
        description = "Manually reset the circuit breaker to CLOSED state"
    )
    public ResponseEntity<ApiResponse<String>> resetCircuitBreaker() {
        try {
            aiServiceClient.resetCircuitBreaker();
            return ResponseEntity.ok(ApiResponse.success("Circuit breaker reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to reset circuit breaker: " + e.getMessage(), 500));
        }
    }
    
    @GetMapping("/fallback")
    @Operation(
        summary = "Get fallback service status",
        description = "Get the status of the AI fallback service"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFallbackStatus() {
        try {
            Map<String, Object> status = fallbackService.getFallbackServiceStatus();
            
            // Add AI service information
            status.put("ai_service_status", aiServiceClient.getServiceStatus());
            status.put("ai_service_configured", aiServiceClient.isConfigured());
            status.put("circuit_breaker_health", aiServiceClient.getCircuitBreakerHealth());
            
            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (Exception e) {
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("error", "Failed to get fallback status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorStatus, 500));
        }
    }
    
    @GetMapping("/diagnostics")
    @Operation(
        summary = "Get comprehensive AI diagnostics",
        description = "Get comprehensive diagnostics for all AI services and components"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDiagnostics() {
        try {
            Map<String, Object> diagnostics = new HashMap<>();
            
            // AI Service Client diagnostics
            diagnostics.put("aiServiceClient", aiServiceClient.getDetailedHealthStatus());
            
            // Fallback service diagnostics
            diagnostics.put("fallbackService", fallbackService.getFallbackServiceStatus());
            
            // Overall system health
            String overallStatus = determineOverallStatus(
                aiServiceClient.getServiceStatus(),
                fallbackService.isAIServiceAvailable()
            );
            diagnostics.put("overallStatus", overallStatus);
            diagnostics.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(diagnostics));
        } catch (Exception e) {
            Map<String, Object> errorDiagnostics = new HashMap<>();
            errorDiagnostics.put("error", "Failed to get diagnostics: " + e.getMessage());
            errorDiagnostics.put("overallStatus", "ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorDiagnostics, 500));
        }
    }
    
    @GetMapping("/metrics")
    @Operation(
        summary = "Get AI service metrics",
        description = "Get performance and usage metrics for AI services"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // Circuit breaker metrics
            CircuitBreaker.CircuitBreakerHealth circuitHealth = aiServiceClient.getCircuitBreakerHealth();
            Map<String, Object> circuitMetrics = new HashMap<>();
            circuitMetrics.put("state", circuitHealth.getState().toString());
            circuitMetrics.put("failureCount", circuitHealth.getFailureCount());
            circuitMetrics.put("successCount", circuitHealth.getSuccessCount());
            circuitMetrics.put("lastFailureTime", circuitHealth.getLastFailureTime());
            circuitMetrics.put("lastSuccessTime", circuitHealth.getLastSuccessTime());
            circuitMetrics.put("halfOpenCalls", circuitHealth.getHalfOpenCalls());
            metrics.put("circuitBreaker", circuitMetrics);
            
            // Service status metrics
            metrics.put("serviceStatus", aiServiceClient.getServiceStatus());
            metrics.put("fallbackAvailable", fallbackService.isAIServiceAvailable());
            
            // Timestamp
            metrics.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(ApiResponse.success(metrics));
        } catch (Exception e) {
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("error", "Failed to get metrics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(errorStatus, 500));
        }
    }
    
    /**
     * Determine overall system health status
     */
    private String determineOverallStatus(String aiServiceStatus, boolean fallbackAvailable) {
        if ("READY".equals(aiServiceStatus)) {
            return "HEALTHY";
        } else if ("CIRCUIT_OPEN".equals(aiServiceStatus) && fallbackAvailable) {
            return "DEGRADED";
        } else if ("RECOVERING".equals(aiServiceStatus)) {
            return "RECOVERING";
        } else if (fallbackAvailable) {
            return "FALLBACK";
        } else {
            return "UNHEALTHY";
        }
    }
}

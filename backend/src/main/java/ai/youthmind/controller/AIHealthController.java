package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.service.AIHealthMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for AI service health monitoring endpoints.
 */
@RestController
@RequestMapping("/api/ai/health")
@RequiredArgsConstructor
public class AIHealthController {

    private final AIHealthMonitoringService healthMonitoringService;

    /**
     * Perform a comprehensive health check of the AI service.
     */
    @GetMapping("/check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> performHealthCheck() {
        Map<String, Object> healthStatus = healthMonitoringService.performHealthCheck();
        
        return ResponseEntity.ok(
            ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("AI service health check completed")
                .data(healthStatus)
                .build()
        );
    }

    /**
     * Get current AI service status and configuration.
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServiceStatus() {
        Map<String, Object> status = healthMonitoringService.getServiceStatus();
        
        return ResponseEntity.ok(
            ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("AI service status retrieved")
                .data(status)
                .build()
        );
    }

    /**
     * Get AI service performance metrics.
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPerformanceMetrics() {
        Map<String, Object> metrics = healthMonitoringService.getPerformanceMetrics();
        
        return ResponseEntity.ok(
            ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("AI service performance metrics retrieved")
                .data(metrics)
                .build()
        );
    }

    /**
     * Get basic health status for monitoring systems.
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> pingResponse = Map.of(
            "status", healthMonitoringService.isServiceHealthy() ? "UP" : "DOWN",
            "timestamp", System.currentTimeMillis(),
            "service", "AI Service"
        );
        
        return ResponseEntity.ok(pingResponse);
    }
}
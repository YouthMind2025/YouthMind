package ai.youthmind.controller;

import ai.youthmind.service.ConcurrencyManager;
import ai.youthmind.util.PerformanceMonitor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * REST Controller for system health and concurrency monitoring
 */
@RestController
@RequestMapping("/v1/health")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Health & Monitoring", description = "System health and performance monitoring APIs")
public class HealthController {

    private final ConcurrencyManager concurrencyManager;

    @GetMapping("/status")
    @Operation(summary = "Get system health status", description = "Get comprehensive system health information")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        
        ConcurrencyManager.SystemHealth health = concurrencyManager.getSystemHealth();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("concurrency", Map.of(
            "activeConnections", health.getActiveConnections(),
            "activeRequests", health.getActiveRequests(),
            "totalRequests", health.getTotalRequests(),
            "totalErrors", health.getTotalErrors(),
            "errorRate", health.getErrorRate()
        ));
        response.put("resources", Map.of(
            "databaseConnectionsAvailable", health.getDatabaseConnectionsAvailable(),
            "aiServiceConnectionsAvailable", health.getAiServiceConnectionsAvailable(),
            "vectorSearchConnectionsAvailable", health.getVectorSearchConnectionsAvailable()
        ));
        response.put("system", Map.of(
            "memoryUsage", health.getMemoryUsage(),
            "cpuUsage", health.getCpuUsage()
        ));
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get performance metrics", description = "Get detailed performance metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("performance", PerformanceMonitor.getAllStats());
        metrics.put("concurrency", concurrencyManager.getSystemHealth());
        metrics.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness check", description = "Check if system is ready to handle requests")
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        
        ConcurrencyManager.SystemHealth health = concurrencyManager.getSystemHealth();
        
        boolean isReady = health.getActiveRequests() < 10000 && 
                         health.getDatabaseConnectionsAvailable() > 0 &&
                         health.getErrorRate() < 5.0;
        
        Map<String, Object> response = new HashMap<>();
        response.put("ready", isReady);
        response.put("timestamp", System.currentTimeMillis());
        
        if (!isReady) {
            response.put("reason", "System overloaded or unhealthy");
            return ResponseEntity.status(503).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/live")
    @Operation(summary = "Liveness check", description = "Check if system is alive")
    public ResponseEntity<Map<String, Object>> livenessCheck() {
        
        Map<String, Object> response = new HashMap<>();
        response.put("alive", true);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}

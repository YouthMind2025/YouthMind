package ai.youthmind.controller;

import ai.youthmind.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API controller for AI Service Connection Pool management and monitoring.
 * Provides endpoints for:
 * - Connection pool health monitoring
 * - Load balancing metrics and management
 * - Performance metrics and trends
 * - Connection pool scaling and configuration
 */
@RestController
@RequestMapping("/v1/ai-service/connection-pool")
@RequiredArgsConstructor
@Slf4j
public class AIServiceConnectionPoolController {

    private final AIServiceConnectionPoolManager connectionPoolManager;
    private final AIServiceConnectionHealthMonitor healthMonitor;
    private final AIServiceLoadBalancer loadBalancer;
    private final AIServiceConnectionMetrics metrics;

    /**
     * Get overall connection pool health status
     */
    @GetMapping("/health")
    public ResponseEntity<AIServiceConnectionPoolManager.ConnectionPoolHealthStatus> getHealthStatus() {
        try {
            AIServiceConnectionPoolManager.ConnectionPoolHealthStatus health = connectionPoolManager.getHealthStatus();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error getting connection pool health status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get connection pool metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<AIServiceConnectionPoolManager.ConnectionPoolMetrics> getMetrics() {
        try {
            AIServiceConnectionPoolManager.ConnectionPoolMetrics metrics = connectionPoolManager.getMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting connection pool metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get service-specific health status
     */
    @GetMapping("/health/{serviceType}")
    public ResponseEntity<Map<String, AIServiceConnectionHealthMonitor.ServiceHealthStatus>> getServiceHealth(
            @PathVariable String serviceType) {
        try {
            Map<String, AIServiceConnectionHealthMonitor.ServiceHealthStatus> allHealth = healthMonitor.getAllServiceHealthStatus();
            // Filter by serviceType
            AIServiceConnectionHealthMonitor.ServiceHealthStatus serviceHealth = allHealth.get(serviceType);
            
            if (serviceHealth == null) {
                // Return empty map if service not found (consistent with other endpoints)
                return ResponseEntity.ok(Map.of());
            }
            
            // Return map with only the requested service
            return ResponseEntity.ok(Map.of(serviceType, serviceHealth));
        } catch (Exception e) {
            log.error("Error getting service health status for: {}", serviceType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get load balancing metrics for service
     */
    @GetMapping("/load-balancer/{serviceType}/metrics")
    public ResponseEntity<AIServiceLoadBalancer.LoadBalancingMetrics> getLoadBalancingMetrics(
            @PathVariable String serviceType) {
        try {
            AIServiceLoadBalancer.LoadBalancingMetrics metrics = loadBalancer.getMetrics(serviceType);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting load balancing metrics for service: {}", serviceType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all service endpoints
     */
    @GetMapping("/load-balancer/endpoints")
    public ResponseEntity<Map<String, java.util.List<AIServiceLoadBalancer.ServiceEndpoint>>> getAllEndpoints() {
        try {
            Map<String, java.util.List<AIServiceLoadBalancer.ServiceEndpoint>> endpoints = loadBalancer.getAllServiceEndpoints();
            return ResponseEntity.ok(endpoints);
        } catch (Exception e) {
            log.error("Error getting all service endpoints", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get service-specific metrics
     */
    @GetMapping("/metrics/{serviceType}")
    public ResponseEntity<AIServiceConnectionMetrics.ServiceMetricsSummary> getServiceMetrics(
            @PathVariable String serviceType) {
        try {
            AIServiceConnectionMetrics.ServiceMetricsSummary serviceMetrics = metrics.getServiceMetrics(serviceType);
            return ResponseEntity.ok(serviceMetrics);
        } catch (Exception e) {
            log.error("Error getting service metrics for: {}", serviceType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get global metrics summary
     */
    @GetMapping("/metrics/global")
    public ResponseEntity<AIServiceConnectionMetrics.GlobalMetricsSummary> getGlobalMetrics() {
        try {
            AIServiceConnectionMetrics.GlobalMetricsSummary globalMetrics = metrics.getGlobalMetrics();
            return ResponseEntity.ok(globalMetrics);
        } catch (Exception e) {
            log.error("Error getting global metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get performance trends for service
     */
    @GetMapping("/metrics/{serviceType}/trends")
    public ResponseEntity<java.util.List<AIServiceConnectionMetrics.PerformanceTrend>> getPerformanceTrends(
            @PathVariable String serviceType,
            @RequestParam(defaultValue = "24") int hours) {
        try {
            java.util.List<AIServiceConnectionMetrics.PerformanceTrend> trends = metrics.getPerformanceTrends(serviceType, hours);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("Error getting performance trends for service: {}", serviceType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Scale connection pool for service
     */
    @PostMapping("/scale/{serviceType}")
    public ResponseEntity<String> scaleConnectionPool(
            @PathVariable String serviceType,
            @RequestParam(required = true) int targetConnections) {
        
        try {
            // Min validation (reject zero/negative values)
            if (targetConnections < 1) {
                return ResponseEntity.badRequest().body("targetConnections must be at least 1");
            }
            
            connectionPoolManager.scalePool(serviceType, targetConnections);
            return ResponseEntity.ok("Connection pool scaled successfully for service: " + serviceType + " to " + targetConnections + " connections");
        } catch (Exception e) {
            log.error("Error scaling connection pool for service: {}", serviceType, e);
            return ResponseEntity.internalServerError().body("Error scaling connection pool: " + e.getMessage());
        }
    }

    /**
     * Update endpoint health status
     */
    @PutMapping("/load-balancer/{serviceType}/endpoint/{endpointId}/health")
    public ResponseEntity<String> updateEndpointHealth(
            @PathVariable String serviceType,
            @PathVariable String endpointId,
            @RequestParam(required = true) boolean isHealthy) {
        
        try {
            loadBalancer.updateEndpointHealth(serviceType, endpointId, isHealthy);
            return ResponseEntity.ok("Endpoint health updated successfully");
        } catch (Exception e) {
            log.error("Error updating endpoint health for service: {} endpoint: {}", serviceType, endpointId, e);
            return ResponseEntity.internalServerError().body("Error updating endpoint health: " + e.getMessage());
        }
    }

    /**
     * Register new service endpoint
     */
    @PostMapping("/load-balancer/{serviceType}/endpoint")
    public ResponseEntity<String> registerEndpoint(
            @PathVariable String serviceType,
            @Valid @RequestBody AIServiceLoadBalancer.ServiceEndpoint endpoint) {
        try {
            loadBalancer.registerEndpoint(serviceType, endpoint);
            return ResponseEntity.ok("Endpoint registered successfully");
        } catch (Exception e) {
            log.error("Error registering endpoint for service: {}", serviceType, e);
            return ResponseEntity.internalServerError().body("Error registering endpoint: " + e.getMessage());
        }
    }

    /**
     * Unregister service endpoint
     */
    @DeleteMapping("/load-balancer/{serviceType}/endpoint/{endpointId}")
    public ResponseEntity<String> unregisterEndpoint(
            @PathVariable String serviceType,
            @PathVariable String endpointId) {
        try {
            loadBalancer.unregisterEndpoint(serviceType, endpointId);
            return ResponseEntity.ok("Endpoint unregistered successfully");
        } catch (Exception e) {
            log.error("Error unregistering endpoint for service: {} endpoint: {}", serviceType, endpointId, e);
            return ResponseEntity.internalServerError().body("Error unregistering endpoint: " + e.getMessage());
        }
    }

    /**
     * Get health trends for service
     */
    @GetMapping("/health/{serviceType}/trend")
    public ResponseEntity<AIServiceConnectionHealthMonitor.HealthTrend> getHealthTrend(
            @PathVariable String serviceType) {
        try {
            AIServiceConnectionHealthMonitor.HealthTrend trend = healthMonitor.getHealthTrend(serviceType);
            return ResponseEntity.ok(trend);
        } catch (Exception e) {
            log.error("Error getting health trend for service: {}", serviceType, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

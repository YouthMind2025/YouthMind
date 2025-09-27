package ai.youthmind.controller;

import ai.youthmind.service.AIServiceLoadBalancer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Load Balancing Management and Monitoring.
 * Provides endpoints for:
 * - Service discovery and registration
 * - Load balancing configuration management
 * - Auto-scaling operations
 * - Failover management
 * - Health monitoring and metrics
 */
@RestController
@RequestMapping("/v1/load-balancing")
@RequiredArgsConstructor
@Slf4j
public class LoadBalancingController {

    private final AIServiceLoadBalancer loadBalancer;

    /**
     * Get all service endpoints
     */
    @GetMapping("/services")
    public ResponseEntity<Map<String, List<AIServiceLoadBalancer.ServiceEndpoint>>> getAllServiceEndpoints() {
        try {
            Map<String, List<AIServiceLoadBalancer.ServiceEndpoint>> endpoints = loadBalancer.getAllServiceEndpoints();
            return ResponseEntity.ok(endpoints);
        } catch (Exception e) {
            log.error("Error getting all service endpoints", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get service discovery information for specific service
     */
    @GetMapping("/services/{serviceType}/discovery")
    public ResponseEntity<AIServiceLoadBalancer.ServiceDiscoveryInfo> getServiceDiscoveryInfo(
            @PathVariable String serviceType) {
        try {
            AIServiceLoadBalancer.ServiceDiscoveryInfo info = loadBalancer.getServiceDiscoveryInfo(serviceType);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Error getting service discovery info for: {}", serviceType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Register new service endpoint
     */
    @PostMapping("/services/{serviceType}/endpoints")
    public ResponseEntity<String> registerEndpoint(
            @PathVariable String serviceType,
            @RequestBody AIServiceLoadBalancer.ServiceEndpoint endpoint) {
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
    @DeleteMapping("/services/{serviceType}/endpoints/{endpointId}")
    public ResponseEntity<String> unregisterEndpoint(
            @PathVariable String serviceType,
            @PathVariable String endpointId) {
        try {
            loadBalancer.unregisterEndpoint(serviceType, endpointId);
            return ResponseEntity.ok("Endpoint unregistered successfully");
        } catch (Exception e) {
            log.error("Error unregistering endpoint: {} for service: {}", endpointId, serviceType, e);
            return ResponseEntity.internalServerError().body("Error unregistering endpoint: " + e.getMessage());
        }
    }

    /**
     * Get load balancing metrics for specific service
     */
    @GetMapping("/services/{serviceType}/metrics")
    public ResponseEntity<AIServiceLoadBalancer.LoadBalancingMetrics> getServiceMetrics(
            @PathVariable String serviceType) {
        try {
            AIServiceLoadBalancer.LoadBalancingMetrics metrics = loadBalancer.getMetrics(serviceType);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting metrics for service: {}", serviceType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get comprehensive load balancing health status
     */
    @GetMapping("/health")
    public ResponseEntity<AIServiceLoadBalancer.LoadBalancingHealthStatus> getLoadBalancingHealthStatus() {
        try {
            AIServiceLoadBalancer.LoadBalancingHealthStatus healthStatus = loadBalancer.getLoadBalancingHealthStatus();
            return ResponseEntity.ok(healthStatus);
        } catch (Exception e) {
            log.error("Error getting load balancing health status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Auto-scale service to target number of instances
     */
    @PostMapping("/services/{serviceType}/scale")
    public ResponseEntity<String> autoScaleService(
            @PathVariable String serviceType,
            @RequestParam int targetInstances) {
        try {
            loadBalancer.autoScaleService(serviceType, targetInstances);
            return ResponseEntity.ok("Service auto-scaled successfully to " + targetInstances + " instances");
        } catch (Exception e) {
            log.error("Error auto-scaling service: {} to {} instances", serviceType, targetInstances, e);
            return ResponseEntity.internalServerError().body("Error auto-scaling service: " + e.getMessage());
        }
    }

    /**
     * Perform failover for failed endpoint
     */
    @PostMapping("/services/{serviceType}/failover")
    public ResponseEntity<AIServiceLoadBalancer.ServiceEndpoint> performFailover(
            @PathVariable String serviceType,
            @RequestParam String failedEndpointId) {
        try {
            AIServiceLoadBalancer.ServiceEndpoint newEndpoint = loadBalancer.performFailover(serviceType, failedEndpointId);
            return ResponseEntity.ok(newEndpoint);
        } catch (Exception e) {
            log.error("Error performing failover for service: {} endpoint: {}", serviceType, failedEndpointId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update load balancing configuration
     */
    @PutMapping("/services/{serviceType}/config")
    public ResponseEntity<String> updateLoadBalancingConfig(
            @PathVariable String serviceType,
            @RequestBody AIServiceLoadBalancer.LoadBalancingConfig config) {
        try {
            loadBalancer.updateLoadBalancingConfig(serviceType, config);
            return ResponseEntity.ok("Load balancing configuration updated successfully");
        } catch (Exception e) {
            log.error("Error updating load balancing config for service: {}", serviceType, e);
            return ResponseEntity.internalServerError().body("Error updating configuration: " + e.getMessage());
        }
    }

    /**
     * Update endpoint health status
     */
    @PutMapping("/services/{serviceType}/endpoints/{endpointId}/health")
    public ResponseEntity<String> updateEndpointHealth(
            @PathVariable String serviceType,
            @PathVariable String endpointId,
            @RequestParam boolean isHealthy) {
        try {
            loadBalancer.updateEndpointHealth(serviceType, endpointId, isHealthy);
            return ResponseEntity.ok("Endpoint health status updated successfully");
        } catch (Exception e) {
            log.error("Error updating health status for endpoint: {} service: {}", endpointId, serviceType, e);
            return ResponseEntity.internalServerError().body("Error updating health status: " + e.getMessage());
        }
    }

    /**
     * Record request result for endpoint
     */
    @PostMapping("/services/{serviceType}/endpoints/{endpointId}/result")
    public ResponseEntity<String> recordRequestResult(
            @PathVariable String serviceType,
            @PathVariable String endpointId,
            @RequestParam boolean success,
            @RequestParam long responseTimeMs) {
        try {
            loadBalancer.recordRequestResult(serviceType, endpointId, success, responseTimeMs);
            return ResponseEntity.ok("Request result recorded successfully");
        } catch (Exception e) {
            log.error("Error recording request result for endpoint: {} service: {}", endpointId, serviceType, e);
            return ResponseEntity.internalServerError().body("Error recording request result: " + e.getMessage());
        }
    }

    /**
     * Select endpoint for service operation
     */
    @PostMapping("/services/{serviceType}/select")
    public ResponseEntity<AIServiceLoadBalancer.ServiceEndpoint> selectEndpoint(
            @PathVariable String serviceType,
            @RequestParam String userId,
            @RequestParam String operation) {
        try {
            AIServiceLoadBalancer.ServiceEndpoint endpoint = loadBalancer.selectEndpoint(serviceType, userId, operation);
            return ResponseEntity.ok(endpoint);
        } catch (Exception e) {
            log.error("Error selecting endpoint for service: {} user: {} operation: {}", serviceType, userId, operation, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

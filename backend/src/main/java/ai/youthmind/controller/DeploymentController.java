package ai.youthmind.controller;

import ai.youthmind.service.DeploymentManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for Deployment Management.
 * Provides endpoints for:
 * - Service deployment and rollback
 * - Deployment status and monitoring
 * - Health validation and testing
 * - Configuration management
 * - Deployment metrics and history
 */
@RestController
@RequestMapping("/v1/deployment")
@RequiredArgsConstructor
@Slf4j
public class DeploymentController {

    private final DeploymentManager deploymentManager;

    /**
     * Deploy psychological analysis service
     */
    @PostMapping("/services/{serviceName}/deploy")
    public ResponseEntity<DeploymentManager.DeploymentResult> deployService(
            @PathVariable String serviceName,
            @RequestBody DeploymentManager.DeploymentRequest request) {
        try {
            request.setServiceName(serviceName);
            CompletableFuture<DeploymentManager.DeploymentResult> deploymentFuture = deploymentManager.deployService(request);
            DeploymentManager.DeploymentResult result = deploymentFuture.get();
            
            if (result.getStatus() == DeploymentManager.DeploymentStatus.SUCCESS) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("Error deploying service: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Rollback service to previous version
     */
    @PostMapping("/services/{serviceName}/rollback")
    public ResponseEntity<DeploymentManager.RollbackResult> rollbackService(
            @PathVariable String serviceName,
            @RequestParam String targetVersion) {
        try {
            CompletableFuture<DeploymentManager.RollbackResult> rollbackFuture = deploymentManager.rollbackService(serviceName, targetVersion);
            DeploymentManager.RollbackResult result = rollbackFuture.get();
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("Error rolling back service: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get deployment status for service
     */
    @GetMapping("/services/{serviceName}/status")
    public ResponseEntity<Map<String, Object>> getDeploymentStatus(@PathVariable String serviceName) {
        try {
            DeploymentManager.DeploymentStatus status = deploymentManager.getDeploymentStatus(serviceName);
            
            Map<String, Object> response = Map.of(
                "serviceName", serviceName,
                "status", status.toString(),
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting deployment status for service: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Validate service health
     */
    @PostMapping("/services/{serviceName}/validate")
    public ResponseEntity<DeploymentManager.HealthValidationResult> validateServiceHealth(@PathVariable String serviceName) {
        try {
            CompletableFuture<DeploymentManager.HealthValidationResult> validationFuture = deploymentManager.validateServiceHealth(serviceName);
            DeploymentManager.HealthValidationResult result = validationFuture.get();
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error validating health for service: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get deployment metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<DeploymentManager.DeploymentMetrics> getDeploymentMetrics() {
        try {
            DeploymentManager.DeploymentMetrics metrics = deploymentManager.getDeploymentMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting deployment metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get service configuration
     */
    @GetMapping("/services/{serviceName}/config")
    public ResponseEntity<DeploymentManager.ServiceConfiguration> getServiceConfiguration(@PathVariable String serviceName) {
        try {
            DeploymentManager.ServiceConfiguration config = deploymentManager.getServiceConfiguration(serviceName);
            if (config != null) {
                return ResponseEntity.ok(config);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting configuration for service: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update service configuration
     */
    @PutMapping("/services/{serviceName}/config")
    public ResponseEntity<String> updateServiceConfiguration(
            @PathVariable String serviceName,
            @RequestBody DeploymentManager.ServiceConfiguration config) {
        try {
            config.setServiceName(serviceName);
            deploymentManager.updateServiceConfiguration(serviceName, config);
            return ResponseEntity.ok("Service configuration updated successfully");
        } catch (Exception e) {
            log.error("Error updating configuration for service: {}", serviceName, e);
            return ResponseEntity.internalServerError().body("Error updating configuration: " + e.getMessage());
        }
    }

    /**
     * Get deployment history for service
     */
    @GetMapping("/services/{serviceName}/history")
    public ResponseEntity<List<DeploymentManager.DeploymentResult>> getDeploymentHistory(@PathVariable String serviceName) {
        try {
            DeploymentManager.DeploymentMetrics metrics = deploymentManager.getDeploymentMetrics();
            DeploymentManager.DeploymentHistory history = metrics.getDeploymentHistory().get(serviceName);
            
            if (history != null) {
                return ResponseEntity.ok(history.getDeployments());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting deployment history for service: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all service configurations
     */
    @GetMapping("/configurations")
    public ResponseEntity<Map<String, DeploymentManager.ServiceConfiguration>> getAllServiceConfigurations() {
        try {
            // This would need to be implemented in DeploymentManager
            return ResponseEntity.ok(Map.of());
        } catch (Exception e) {
            log.error("Error getting all service configurations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Initialize deployment manager
     */
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeDeploymentManager() {
        try {
            deploymentManager.initialize();
            return ResponseEntity.ok("Deployment manager initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing deployment manager", e);
            return ResponseEntity.internalServerError().body("Error initializing deployment manager: " + e.getMessage());
        }
    }

    /**
     * Get deployment health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getDeploymentHealth() {
        try {
            DeploymentManager.DeploymentMetrics metrics = deploymentManager.getDeploymentMetrics();
            
            Map<String, Object> health = Map.of(
                "status", "UP",
                "totalDeployments", metrics.getTotalDeployments(),
                "successRate", metrics.getSuccessRate(),
                "activeDeployments", metrics.getActiveDeployments(),
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error getting deployment health", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

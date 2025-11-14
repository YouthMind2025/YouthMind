package ai.youthmind.controller;

import ai.youthmind.service.DeploymentMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * REST Controller for Deployment Monitoring.
 * Provides endpoints for:
 * - Deployment monitoring and status
 * - Health checks and metrics
 * - Deployment events and history
 * - Monitoring reports and summaries
 */
@RestController
@RequestMapping("/v1/deployment-monitoring")
@RequiredArgsConstructor
@Slf4j
public class DeploymentMonitoringController {

    private final DeploymentMonitoringService monitoringService;

    /**
     * Start monitoring deployment
     */
    @PostMapping("/services/{serviceName}/start")
    public ResponseEntity<?> startMonitoring(
            @PathVariable String serviceName,
            @RequestParam(required = true) String version) {
        try {
            CompletableFuture<DeploymentMonitoringService.DeploymentMonitor> monitorFuture = 
                monitoringService.startMonitoring(serviceName, version);
            
            // Add timeout to prevent indefinite blocking (safety measure)
            DeploymentMonitoringService.DeploymentMonitor monitor;
            try {
                monitor = monitorFuture.get(10, TimeUnit.SECONDS);  // 10 second timeout
            } catch (TimeoutException e) {
                log.error("Monitoring operation timed out after 10 seconds for service: {}, version: {}", 
                    serviceName, version);
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Monitoring operation timed out. Please check system status.");
                error.put("serviceName", serviceName);
                error.put("version", version);
                error.put("timeout", 10);
                return ResponseEntity.status(org.springframework.http.HttpStatus.REQUEST_TIMEOUT).body(error);
            }
            
            return ResponseEntity.ok(monitor);
        } catch (Exception e) {
            log.error("Error starting monitoring for service: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Stop monitoring deployment
     */
    @PostMapping("/services/{serviceName}/stop")
    public ResponseEntity<String> stopMonitoring(@PathVariable String serviceName) {
        try {
            CompletableFuture<Void> stopFuture = monitoringService.stopMonitoring(serviceName);
            stopFuture.get();
            
            return ResponseEntity.ok("Monitoring stopped for service: " + serviceName);
        } catch (Exception e) {
            log.error("Error stopping monitoring for service: {}", serviceName, e);
            return ResponseEntity.internalServerError().body("Error stopping monitoring: " + e.getMessage());
        }
    }

    /**
     * Get deployment status
     */
    @GetMapping("/services/{serviceName}/status")
    public ResponseEntity<Map<String, Object>> getDeploymentStatus(@PathVariable String serviceName) {
        try {
            DeploymentMonitoringService.DeploymentStatus status = monitoringService.getDeploymentStatus(serviceName);
            
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
     * Get deployment metrics
     */
    @GetMapping("/services/{serviceName}/metrics")
    public ResponseEntity<DeploymentMonitoringService.DeploymentMetrics> getDeploymentMetrics(@PathVariable String serviceName) {
        try {
            DeploymentMonitoringService.DeploymentMetrics metrics = monitoringService.getDeploymentMetrics(serviceName);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting deployment metrics for service: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get deployment events
     */
    @GetMapping("/services/{serviceName}/events")
    public ResponseEntity<List<DeploymentMonitoringService.DeploymentEvent>> getDeploymentEvents(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<DeploymentMonitoringService.DeploymentEvent> events = 
                monitoringService.getDeploymentEvents(serviceName, limit);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            log.error("Error getting deployment events for service: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get comprehensive monitoring report
     */
    @GetMapping("/services/{serviceName}/report")
    public ResponseEntity<DeploymentMonitoringService.MonitoringReport> getMonitoringReport(@PathVariable String serviceName) {
        try {
            DeploymentMonitoringService.MonitoringReport report = monitoringService.getMonitoringReport(serviceName);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("Error getting monitoring report for service: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all active monitors
     */
    @GetMapping("/monitors")
    public ResponseEntity<Map<String, DeploymentMonitoringService.DeploymentMonitor>> getAllActiveMonitors() {
        try {
            Map<String, DeploymentMonitoringService.DeploymentMonitor> monitors = 
                monitoringService.getAllActiveMonitors();
            return ResponseEntity.ok(monitors);
        } catch (Exception e) {
            log.error("Error getting all active monitors", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get monitoring summary
     */
    @GetMapping("/summary")
    public ResponseEntity<DeploymentMonitoringService.MonitoringSummary> getMonitoringSummary() {
        try {
            DeploymentMonitoringService.MonitoringSummary summary = monitoringService.getMonitoringSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error getting monitoring summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Initialize monitoring service
     */
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeMonitoringService() {
        try {
            monitoringService.initialize();
            return ResponseEntity.ok("Deployment monitoring service initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing monitoring service", e);
            return ResponseEntity.internalServerError().body("Error initializing monitoring service: " + e.getMessage());
        }
    }

    /**
     * Get monitoring health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getMonitoringHealth() {
        try {
            DeploymentMonitoringService.MonitoringSummary summary = monitoringService.getMonitoringSummary();
            
            Map<String, Object> health = Map.of(
                "status", "UP",
                "totalMonitors", summary.getTotalMonitors(),
                "activeMonitors", summary.getActiveMonitors(),
                "averageHealthScore", summary.getAverageHealthScore(),
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error getting monitoring health", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

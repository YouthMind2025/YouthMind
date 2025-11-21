package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Deployment Monitoring Service.
 * Monitors deployment status, health, and performance metrics.
 * Features:
 * - Real-time deployment monitoring
 * - Health check monitoring
 * - Performance metrics collection
 * - Alerting and notifications
 * - Deployment history tracking
 * - Rollback monitoring
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentMonitoringService {

    private final AIServiceLoadBalancer loadBalancer;
    private final AIServiceConnectionHealthMonitor healthMonitor;

    // Monitoring data
    private final Map<String, DeploymentMonitor> activeMonitors = new ConcurrentHashMap<>();
    private final Map<String, List<DeploymentEvent>> deploymentEvents = new ConcurrentHashMap<>();
    private final Map<String, DeploymentMetrics> deploymentMetrics = new ConcurrentHashMap<>();
    
    // Background monitoring
    private final ScheduledExecutorService monitoringScheduler = Executors.newScheduledThreadPool(4);
    
    // Configuration
    private static final int MONITORING_INTERVAL_SECONDS = 30;
    private static final int HEALTH_CHECK_INTERVAL_SECONDS = 60;
    private static final int METRICS_RETENTION_HOURS = 24;

    /**
     * Initialize deployment monitoring
     */
    public void initialize() {
        try {
            // Start background monitoring tasks
            startMonitoringTasks();
            
            // Initialize monitoring for existing services
            initializeServiceMonitoring();
            
            log.info("Deployment Monitoring Service initialized");
            
        } catch (Exception e) {
            log.error("Error initializing Deployment Monitoring Service", e);
        }
    }

    /**
     * Start monitoring deployment
     */
    public CompletableFuture<DeploymentMonitor> startMonitoring(String serviceName, String version) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting monitoring for service: {} version: {}", serviceName, version);
                
                DeploymentMonitor monitor = DeploymentMonitor.builder()
                    .serviceName(serviceName)
                    .version(version)
                    .status(MonitoringStatus.ACTIVE)
                    .startedAt(LocalDateTime.now())
                    .lastCheckAt(LocalDateTime.now())
                    .build();
                
                activeMonitors.put(serviceName, monitor);
                
                // Record deployment event
                recordDeploymentEvent(serviceName, DeploymentEventType.MONITORING_STARTED, 
                    "Monitoring started for version: " + version);
                
                log.info("Monitoring started for service: {}", serviceName);
                
                return monitor;
                
            } catch (Exception e) {
                log.error("Error starting monitoring for service: {}", serviceName, e);
                throw new RuntimeException("Failed to start monitoring", e);
            }
        });
    }

    /**
     * Stop monitoring deployment
     */
    public CompletableFuture<Void> stopMonitoring(String serviceName) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("Stopping monitoring for service: {}", serviceName);
                
                DeploymentMonitor monitor = activeMonitors.remove(serviceName);
                if (monitor != null) {
                    monitor.setStatus(MonitoringStatus.STOPPED);
                    monitor.setStoppedAt(LocalDateTime.now());
                    
                    // Record deployment event
                    recordDeploymentEvent(serviceName, DeploymentEventType.MONITORING_STOPPED, 
                        "Monitoring stopped");
                    
                    log.info("Monitoring stopped for service: {}", serviceName);
                }
                
            } catch (Exception e) {
                log.error("Error stopping monitoring for service: {}", serviceName, e);
            }
        });
    }

    /**
     * Get deployment status
     */
    public DeploymentStatus getDeploymentStatus(String serviceName) {
        DeploymentMonitor monitor = activeMonitors.get(serviceName);
        if (monitor == null) {
            return DeploymentStatus.NOT_MONITORED;
        }
        
        return monitor.getDeploymentStatus();
    }

    /**
     * Get deployment metrics
     */
    public DeploymentMetrics getDeploymentMetrics(String serviceName) {
        return deploymentMetrics.getOrDefault(serviceName, DeploymentMetrics.builder()
            .serviceName(serviceName)
            .timestamp(LocalDateTime.now())
            .build());
    }

    /**
     * Get deployment events
     */
    public List<DeploymentEvent> getDeploymentEvents(String serviceName, int limit) {
        List<DeploymentEvent> events = deploymentEvents.getOrDefault(serviceName, new ArrayList<>());
        
        return events.stream()
            .sorted((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()))
            .limit(limit)
            .toList();
    }

    /**
     * Get comprehensive monitoring report
     */
    public MonitoringReport getMonitoringReport(String serviceName) {
        try {
            DeploymentMonitor monitor = activeMonitors.get(serviceName);
            DeploymentMetrics metrics = deploymentMetrics.get(serviceName);
            List<DeploymentEvent> recentEvents = getDeploymentEvents(serviceName, 10);
            
            // Calculate health score
            double healthScore = calculateHealthScore(serviceName);
            
            // Get service discovery info
            AIServiceLoadBalancer.ServiceDiscoveryInfo discoveryInfo = loadBalancer.getServiceDiscoveryInfo(serviceName);
            
            return MonitoringReport.builder()
                .serviceName(serviceName)
                .monitor(monitor)
                .metrics(metrics)
                .recentEvents(recentEvents)
                .healthScore(healthScore)
                .serviceDiscoveryInfo(discoveryInfo)
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error generating monitoring report for service: {}", serviceName, e);
            return MonitoringReport.builder()
                .serviceName(serviceName)
                .error(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Get all active monitors
     */
    public Map<String, DeploymentMonitor> getAllActiveMonitors() {
        return new HashMap<>(activeMonitors);
    }

    /**
     * Get monitoring summary
     */
    public MonitoringSummary getMonitoringSummary() {
        try {
            int totalMonitors = activeMonitors.size();
            int activeMonitorsCount = (int) activeMonitors.values().stream()
                .filter(m -> m.getStatus() == MonitoringStatus.ACTIVE)
                .count();
            
            double averageHealthScore = activeMonitors.values().stream()
                .mapToDouble(m -> calculateHealthScore(m.getServiceName()))
                .average()
                .orElse(0.0);
            
            return MonitoringSummary.builder()
                .totalMonitors(totalMonitors)
                .activeMonitors(activeMonitorsCount)
                .stoppedMonitors(totalMonitors - activeMonitorsCount)
                .averageHealthScore(averageHealthScore)
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error generating monitoring summary", e);
            return MonitoringSummary.builder()
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    // Private helper methods

    private void startMonitoringTasks() {
        // Monitor active deployments every 30 seconds
        monitoringScheduler.scheduleAtFixedRate(() -> {
            try {
                monitorActiveDeployments();
            } catch (Exception e) {
                log.error("Error monitoring active deployments", e);
            }
        }, 0, MONITORING_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        // Perform health checks every minute
        monitoringScheduler.scheduleAtFixedRate(() -> {
            try {
                performHealthChecks();
            } catch (Exception e) {
                log.error("Error performing health checks", e);
            }
        }, 0, HEALTH_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        // Clean up old metrics every hour
        monitoringScheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupOldMetrics();
            } catch (Exception e) {
                log.error("Error cleaning up old metrics", e);
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    private void initializeServiceMonitoring() {
        // Initialize monitoring for psychological analysis services
        String[] services = {
            "psychological_analysis",
            "state_update",
            "personality_analysis",
            "mood_analysis"
        };
        
        for (String service : services) {
            try {
                startMonitoring(service, "1.0.0").get();
            } catch (Exception e) {
                log.error("Error initializing monitoring for service: {}", service, e);
            }
        }
    }

    private void monitorActiveDeployments() {
        for (DeploymentMonitor monitor : activeMonitors.values()) {
            if (monitor.getStatus() == MonitoringStatus.ACTIVE) {
                try {
                    // Update last check time
                    monitor.setLastCheckAt(LocalDateTime.now());
                    
                    // Check deployment health
                    double healthScore = calculateHealthScore(monitor.getServiceName());
                    
                    // Update metrics
                    updateDeploymentMetrics(monitor.getServiceName(), healthScore);
                    
                    // Check for alerts
                    checkForAlerts(monitor.getServiceName(), healthScore);
                    
                } catch (Exception e) {
                    log.error("Error monitoring deployment: {}", monitor.getServiceName(), e);
                }
            }
        }
    }

    private void performHealthChecks() {
        for (DeploymentMonitor monitor : activeMonitors.values()) {
            if (monitor.getStatus() == MonitoringStatus.ACTIVE) {
                try {
                    // Perform comprehensive health check
                    HealthCheckResult healthResult = performComprehensiveHealthCheck(monitor.getServiceName());
                    
                    // Update monitor with health status
                    monitor.setLastHealthCheck(healthResult);
                    
                    // Record health check event
                    if (!healthResult.isHealthy()) {
                        recordDeploymentEvent(monitor.getServiceName(), DeploymentEventType.HEALTH_CHECK_FAILED,
                            "Health check failed: " + healthResult.getError());
                    }
                    
                } catch (Exception e) {
                    log.error("Error performing health check for service: {}", monitor.getServiceName(), e);
                }
            }
        }
    }

    private HealthCheckResult performComprehensiveHealthCheck(String serviceName) {
        try {
            // Check service discovery
            AIServiceLoadBalancer.ServiceDiscoveryInfo discoveryInfo = loadBalancer.getServiceDiscoveryInfo(serviceName);
            if (!discoveryInfo.isAvailable()) {
                return HealthCheckResult.builder()
                    .serviceName(serviceName)
                    .isHealthy(false)
                    .error("Service discovery failed")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Check load balancer health
            AIServiceLoadBalancer.LoadBalancingHealthStatus lbHealth = loadBalancer.getLoadBalancingHealthStatus();
            if (lbHealth.getOverallHealth() < 0.5) {
                return HealthCheckResult.builder()
                    .serviceName(serviceName)
                    .isHealthy(false)
                    .error("Load balancer health is low: " + lbHealth.getOverallHealth())
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Check connection health
            Map<String, AIServiceConnectionHealthMonitor.ServiceHealthStatus> allHealth = healthMonitor.getAllServiceHealthStatus();
            AIServiceConnectionHealthMonitor.ServiceHealthStatus connHealth = allHealth.get(serviceName);
            if (connHealth != null && connHealth.getHealthScore() < 0.5) {
                return HealthCheckResult.builder()
                    .serviceName(serviceName)
                    .isHealthy(false)
                    .error("Connection health is low: " + connHealth.getHealthScore())
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            return HealthCheckResult.builder()
                .serviceName(serviceName)
                .isHealthy(true)
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            return HealthCheckResult.builder()
                .serviceName(serviceName)
                .isHealthy(false)
                .error("Health check error: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    private double calculateHealthScore(String serviceName) {
        try {
            // Get service discovery health
            AIServiceLoadBalancer.ServiceDiscoveryInfo discoveryInfo = loadBalancer.getServiceDiscoveryInfo(serviceName);
            double discoveryHealth = discoveryInfo.isAvailable() ? 1.0 : 0.0;
            
            // Get load balancer health
            AIServiceLoadBalancer.LoadBalancingHealthStatus lbHealth = loadBalancer.getLoadBalancingHealthStatus();
            double lbHealthScore = lbHealth.getOverallHealth();
            
            // Get connection health
            Map<String, AIServiceConnectionHealthMonitor.ServiceHealthStatus> allHealth = healthMonitor.getAllServiceHealthStatus();
            AIServiceConnectionHealthMonitor.ServiceHealthStatus connHealth = allHealth.get(serviceName);
            double connHealthScore = connHealth != null ? connHealth.getHealthScore() : 0.0;
            
            // Calculate weighted average
            return (discoveryHealth * 0.3 + lbHealthScore * 0.4 + connHealthScore * 0.3);
            
        } catch (Exception e) {
            log.error("Error calculating health score for service: {}", serviceName, e);
            return 0.0;
        }
    }

    private void updateDeploymentMetrics(String serviceName, double healthScore) {
        DeploymentMetrics metrics = deploymentMetrics.computeIfAbsent(serviceName, 
            k -> DeploymentMetrics.builder()
                .serviceName(serviceName)
                .timestamp(LocalDateTime.now())
                .build());
        
        // Update health score
        metrics.setCurrentHealthScore(healthScore);
        metrics.setLastUpdated(LocalDateTime.now());
        
        // Update health score history
        if (metrics.getHealthScoreHistory() == null) {
            metrics.setHealthScoreHistory(new ArrayList<>());
        }
        
        metrics.getHealthScoreHistory().add(HealthScoreEntry.builder()
            .score(healthScore)
            .timestamp(LocalDateTime.now())
            .build());
        
        // Keep only last 100 entries
        if (metrics.getHealthScoreHistory().size() > 100) {
            metrics.getHealthScoreHistory().remove(0);
        }
    }

    private void checkForAlerts(String serviceName, double healthScore) {
        if (healthScore < 0.5) {
            recordDeploymentEvent(serviceName, DeploymentEventType.ALERT,
                "Health score is critically low: " + String.format("%.2f", healthScore));
        } else if (healthScore < 0.7) {
            recordDeploymentEvent(serviceName, DeploymentEventType.WARNING,
                "Health score is below threshold: " + String.format("%.2f", healthScore));
        }
    }

    private void recordDeploymentEvent(String serviceName, DeploymentEventType eventType, String message) {
        DeploymentEvent event = DeploymentEvent.builder()
            .serviceName(serviceName)
            .eventType(eventType)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
        
        deploymentEvents.computeIfAbsent(serviceName, k -> new ArrayList<>()).add(event);
        
        // Keep only last 1000 events per service
        List<DeploymentEvent> events = deploymentEvents.get(serviceName);
        if (events.size() > 1000) {
            events.remove(0);
        }
    }

    private void cleanupOldMetrics() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(METRICS_RETENTION_HOURS);
        
        for (DeploymentMetrics metrics : deploymentMetrics.values()) {
            if (metrics.getHealthScoreHistory() != null) {
                metrics.getHealthScoreHistory().removeIf(entry -> 
                    entry.getTimestamp().isBefore(cutoffTime));
            }
        }
    }

    // Inner classes

    @lombok.Data
    @lombok.Builder
    public static class DeploymentMonitor {
        private String serviceName;
        private String version;
        private MonitoringStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime stoppedAt;
        private LocalDateTime lastCheckAt;
        private HealthCheckResult lastHealthCheck;
        
        public DeploymentStatus getDeploymentStatus() {
            if (status == MonitoringStatus.ACTIVE) {
                if (lastHealthCheck != null && lastHealthCheck.isHealthy()) {
                    return DeploymentStatus.HEALTHY;
                } else {
                    return DeploymentStatus.UNHEALTHY;
                }
            } else {
                return DeploymentStatus.STOPPED;
            }
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class HealthCheckResult {
        private String serviceName;
        private boolean isHealthy;
        private String error;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class DeploymentEvent {
        private String serviceName;
        private DeploymentEventType eventType;
        private String message;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class DeploymentMetrics {
        private String serviceName;
        private double currentHealthScore;
        private List<HealthScoreEntry> healthScoreHistory;
        private LocalDateTime lastUpdated;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class HealthScoreEntry {
        private double score;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class MonitoringReport {
        private String serviceName;
        private DeploymentMonitor monitor;
        private DeploymentMetrics metrics;
        private List<DeploymentEvent> recentEvents;
        private double healthScore;
        private AIServiceLoadBalancer.ServiceDiscoveryInfo serviceDiscoveryInfo;
        private String error;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class MonitoringSummary {
        private int totalMonitors;
        private int activeMonitors;
        private int stoppedMonitors;
        private double averageHealthScore;
        private LocalDateTime timestamp;
    }

    public enum MonitoringStatus {
        ACTIVE, STOPPED, PAUSED
    }

    public enum DeploymentStatus {
        NOT_MONITORED, HEALTHY, UNHEALTHY, STOPPED
    }

    public enum DeploymentEventType {
        MONITORING_STARTED, MONITORING_STOPPED, HEALTH_CHECK_FAILED, ALERT, WARNING, INFO
    }
}

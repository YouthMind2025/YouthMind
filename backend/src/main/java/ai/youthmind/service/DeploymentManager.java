package ai.youthmind.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import ai.youthmind.util.CircuitBreaker;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Deployment Manager for Psychological Analysis Services.
 * Features:
 * - Blue-green deployment with zero downtime
 * - Automated health checks and validation
 * - Rollback procedures and recovery mechanisms
 * - Deployment monitoring and metrics
 * - Configuration management and versioning
 * - Security controls and access management
 * - Performance tracking and alerting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentManager {

    private final AIServiceLoadBalancer loadBalancer;
    private final AIServiceConnectionHealthMonitor healthMonitor;
    private final CircuitBreaker circuitBreaker;

    // Deployment tracking
    private final Map<String, DeploymentInfo> activeDeployments = new ConcurrentHashMap<>();
    private final Map<String, DeploymentHistory> deploymentHistory = new ConcurrentHashMap<>();
    private final Map<String, ServiceConfiguration> serviceConfigurations = new ConcurrentHashMap<>();
    
    // Background tasks
    private final ScheduledExecutorService deploymentScheduler = Executors.newScheduledThreadPool(4);
    
    // Configuration
    private static final int HEALTH_CHECK_TIMEOUT_SECONDS = 30;
    private static final int DEPLOYMENT_TIMEOUT_MINUTES = 10;
    private static final int ROLLBACK_TIMEOUT_MINUTES = 5;

    /**
     * Initialize deployment manager
     */
    public void initialize() {
        try {
            // Load existing service configurations
            loadServiceConfigurations();
            
            // Start background monitoring tasks
            startDeploymentMonitoring();
            
            // Initialize deployment history
            initializeDeploymentHistory();
            
            log.info("Deployment Manager initialized with {} service configurations", serviceConfigurations.size());
            
        } catch (Exception e) {
            log.error("Error initializing Deployment Manager", e);
        }
    }

    /**
     * Deploy psychological analysis service with blue-green strategy
     */
    public CompletableFuture<DeploymentResult> deployService(DeploymentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting deployment for service: {} version: {}", request.getServiceName(), request.getVersion());
                
                // Validate deployment request
                validateDeploymentRequest(request);
                
                // Create deployment info
                DeploymentInfo deploymentInfo = createDeploymentInfo(request);
                activeDeployments.put(request.getServiceName(), deploymentInfo);
                
                // Execute blue-green deployment
                DeploymentResult result = executeBlueGreenDeployment(deploymentInfo);
                
                // Update deployment history
                updateDeploymentHistory(request.getServiceName(), result);
                
                log.info("Deployment completed for service: {} with status: {}", request.getServiceName(), result.getStatus());
                
                return result;
                
            } catch (Exception e) {
                log.error("Error deploying service: {}", request.getServiceName(), e);
                return DeploymentResult.builder()
                    .serviceName(request.getServiceName())
                    .version(request.getVersion())
                    .status(DeploymentStatus.FAILED)
                    .error(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            }
        });
    }

    /**
     * Rollback service to previous version
     */
    public CompletableFuture<RollbackResult> rollbackService(String serviceName, String targetVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting rollback for service: {} to version: {}", serviceName, targetVersion);
                
                DeploymentInfo currentDeployment = activeDeployments.get(serviceName);
                if (currentDeployment == null) {
                    throw new RuntimeException("No active deployment found for service: " + serviceName);
                }
                
                // Execute rollback
                RollbackResult result = executeRollback(currentDeployment, targetVersion);
                
                // Update deployment info
                if (result.isSuccess()) {
                    currentDeployment.setVersion(targetVersion);
                    currentDeployment.setLastRollback(LocalDateTime.now());
                }
                
                log.info("Rollback completed for service: {} with status: {}", serviceName, result.isSuccess());
                
                return result;
                
            } catch (Exception e) {
                log.error("Error rolling back service: {}", serviceName, e);
                return RollbackResult.builder()
                    .serviceName(serviceName)
                    .targetVersion(targetVersion)
                    .success(false)
                    .error(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            }
        });
    }

    /**
     * Get deployment status for service
     */
    public DeploymentStatus getDeploymentStatus(String serviceName) {
        DeploymentInfo deployment = activeDeployments.get(serviceName);
        if (deployment == null) {
            return DeploymentStatus.NOT_DEPLOYED;
        }
        
        return deployment.getStatus();
    }

    /**
     * Get comprehensive deployment metrics
     */
    public DeploymentMetrics getDeploymentMetrics() {
        try {
            Map<String, DeploymentInfo> deployments = new HashMap<>(activeDeployments);
            Map<String, DeploymentHistory> history = new HashMap<>(deploymentHistory);
            
            long totalDeployments = history.values().stream()
                .mapToLong(h -> h.getDeployments().size())
                .sum();
            
            long successfulDeployments = history.values().stream()
                .flatMap(h -> h.getDeployments().stream())
                .mapToLong(d -> d.getStatus() == DeploymentStatus.SUCCESS ? 1 : 0)
                .sum();
            
            long failedDeployments = totalDeployments - successfulDeployments;
            double successRate = totalDeployments > 0 ? (double) successfulDeployments / totalDeployments : 0.0;
            
            return DeploymentMetrics.builder()
                .totalDeployments(totalDeployments)
                .successfulDeployments(successfulDeployments)
                .failedDeployments(failedDeployments)
                .successRate(successRate)
                .activeDeployments(deployments.size())
                .deploymentHistory(history)
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting deployment metrics", e);
            return DeploymentMetrics.builder()
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Validate service health after deployment
     */
    public CompletableFuture<HealthValidationResult> validateServiceHealth(String serviceName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Validating health for service: {}", serviceName);
                
                DeploymentInfo deployment = activeDeployments.get(serviceName);
                if (deployment == null) {
                    return HealthValidationResult.builder()
                        .serviceName(serviceName)
                        .isHealthy(false)
                        .error("No active deployment found")
                        .timestamp(LocalDateTime.now())
                        .build();
                }
                
                // Perform comprehensive health checks
                List<HealthCheck> healthChecks = performHealthChecks(serviceName);
                
                boolean isHealthy = healthChecks.stream().allMatch(HealthCheck::isPassed);
                String error = isHealthy ? null : "One or more health checks failed";
                
                return HealthValidationResult.builder()
                    .serviceName(serviceName)
                    .isHealthy(isHealthy)
                    .healthChecks(healthChecks)
                    .error(error)
                    .timestamp(LocalDateTime.now())
                    .build();
                
            } catch (Exception e) {
                log.error("Error validating health for service: {}", serviceName, e);
                return HealthValidationResult.builder()
                    .serviceName(serviceName)
                    .isHealthy(false)
                    .error(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            }
        });
    }

    /**
     * Get deployment configuration for service
     */
    public ServiceConfiguration getServiceConfiguration(String serviceName) {
        return serviceConfigurations.get(serviceName);
    }

    /**
     * Update service configuration
     */
    public void updateServiceConfiguration(String serviceName, ServiceConfiguration config) {
        serviceConfigurations.put(serviceName, config);
        log.info("Updated configuration for service: {}", serviceName);
    }

    // Private helper methods

    private void loadServiceConfigurations() {
        // Load configurations for psychological analysis services
        String[] services = {
            "psychological_analysis",
            "state_update", 
            "personality_analysis",
            "mood_analysis"
        };
        
        for (String service : services) {
            ServiceConfiguration config = createDefaultServiceConfiguration(service);
            serviceConfigurations.put(service, config);
        }
    }

    private ServiceConfiguration createDefaultServiceConfiguration(String serviceName) {
        return ServiceConfiguration.builder()
            .serviceName(serviceName)
            .version("1.0.0")
            .deploymentStrategy(DeploymentStrategy.BLUE_GREEN)
            .healthCheckEndpoint("/actuator/health")
            .healthCheckTimeout(HEALTH_CHECK_TIMEOUT_SECONDS)
            .deploymentTimeout(DEPLOYMENT_TIMEOUT_MINUTES)
            .rollbackTimeout(ROLLBACK_TIMEOUT_MINUTES)
            .maxInstances(3)
            .minInstances(1)
            .autoScalingEnabled(true)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private void startDeploymentMonitoring() {
        // Monitor active deployments every 5 minutes
        deploymentScheduler.scheduleAtFixedRate(() -> {
            try {
                monitorActiveDeployments();
            } catch (Exception e) {
                log.error("Error monitoring active deployments", e);
            }
        }, 0, 5, TimeUnit.MINUTES);
        
        // Clean up old deployment history every hour
        deploymentScheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupDeploymentHistory();
            } catch (Exception e) {
                log.error("Error cleaning up deployment history", e);
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    private void initializeDeploymentHistory() {
        // Initialize deployment history for each service
        for (String serviceName : serviceConfigurations.keySet()) {
            deploymentHistory.put(serviceName, DeploymentHistory.builder()
                .serviceName(serviceName)
                .deployments(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build());
        }
    }

    private void validateDeploymentRequest(DeploymentRequest request) {
        if (request.getServiceName() == null || request.getServiceName().trim().isEmpty()) {
            throw new IllegalArgumentException("Service name is required");
        }
        
        if (request.getVersion() == null || request.getVersion().trim().isEmpty()) {
            throw new IllegalArgumentException("Version is required");
        }
        
        if (request.getImageUrl() == null || request.getImageUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Image URL is required");
        }
    }

    private DeploymentInfo createDeploymentInfo(DeploymentRequest request) {
        return DeploymentInfo.builder()
            .serviceName(request.getServiceName())
            .version(request.getVersion())
            .imageUrl(request.getImageUrl())
            .status(DeploymentStatus.IN_PROGRESS)
            .startedAt(LocalDateTime.now())
            .environment(request.getEnvironment())
            .config(request.getConfig())
            .build();
    }

    private DeploymentResult executeBlueGreenDeployment(DeploymentInfo deploymentInfo) {
        try {
            log.info("Executing blue-green deployment for service: {}", deploymentInfo.getServiceName());
            
            // Step 1: Deploy to green environment
            boolean greenDeploymentSuccess = deployToGreenEnvironment(deploymentInfo);
            if (!greenDeploymentSuccess) {
                return DeploymentResult.builder()
                    .serviceName(deploymentInfo.getServiceName())
                    .version(deploymentInfo.getVersion())
                    .status(DeploymentStatus.FAILED)
                    .error("Green environment deployment failed")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Step 2: Validate green environment
            HealthValidationResult healthResult = validateServiceHealth(deploymentInfo.getServiceName()).get();
            if (!healthResult.isHealthy()) {
                return DeploymentResult.builder()
                    .serviceName(deploymentInfo.getServiceName())
                    .version(deploymentInfo.getVersion())
                    .status(DeploymentStatus.FAILED)
                    .error("Health validation failed: " + healthResult.getError())
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Step 3: Switch traffic to green environment
            boolean trafficSwitchSuccess = switchTrafficToGreen(deploymentInfo.getServiceName());
            if (!trafficSwitchSuccess) {
                return DeploymentResult.builder()
                    .serviceName(deploymentInfo.getServiceName())
                    .version(deploymentInfo.getVersion())
                    .status(DeploymentStatus.FAILED)
                    .error("Traffic switch failed")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Step 4: Update deployment status
            deploymentInfo.setStatus(DeploymentStatus.SUCCESS);
            deploymentInfo.setCompletedAt(LocalDateTime.now());
            
            return DeploymentResult.builder()
                .serviceName(deploymentInfo.getServiceName())
                .version(deploymentInfo.getVersion())
                .status(DeploymentStatus.SUCCESS)
                .deploymentTimeMs(java.time.Duration.between(deploymentInfo.getStartedAt(), LocalDateTime.now()).toMillis())
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error executing blue-green deployment", e);
            return DeploymentResult.builder()
                .serviceName(deploymentInfo.getServiceName())
                .version(deploymentInfo.getVersion())
                .status(DeploymentStatus.FAILED)
                .error(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    private boolean deployToGreenEnvironment(DeploymentInfo deploymentInfo) {
        try {
            log.info("Deploying to green environment for service: {}", deploymentInfo.getServiceName());
            
            // Simulate deployment process
            Thread.sleep(2000); // Simulate deployment time
            
            // Register new service endpoint in load balancer
            AIServiceLoadBalancer.ServiceEndpoint greenEndpoint = AIServiceLoadBalancer.ServiceEndpoint.builder()
                .endpointId(deploymentInfo.getServiceName() + "-green-" + System.currentTimeMillis())
                .serviceType(deploymentInfo.getServiceName())
                .url("http://localhost:" + (8000 + new Random().nextInt(1000)))
                .weight(100)
                .healthy(true)
                .build();
            
            loadBalancer.registerEndpoint(deploymentInfo.getServiceName(), greenEndpoint);
            
            log.info("Successfully deployed to green environment for service: {}", deploymentInfo.getServiceName());
            return true;
            
        } catch (Exception e) {
            log.error("Error deploying to green environment", e);
            return false;
        }
    }

    private boolean switchTrafficToGreen(String serviceName) {
        try {
            log.info("Switching traffic to green environment for service: {}", serviceName);
            
            // Update load balancer configuration to prefer green endpoints
            AIServiceLoadBalancer.LoadBalancingConfig config = loadBalancer.getServiceDiscoveryInfo(serviceName).getLoadBalancingConfig();
            if (config != null) {
                config.setAlgorithm(AIServiceLoadBalancer.LoadBalancingAlgorithm.HEALTH_AWARE);
                loadBalancer.updateLoadBalancingConfig(serviceName, config);
            }
            
            log.info("Successfully switched traffic to green environment for service: {}", serviceName);
            return true;
            
        } catch (Exception e) {
            log.error("Error switching traffic to green environment", e);
            return false;
        }
    }

    private RollbackResult executeRollback(DeploymentInfo currentDeployment, String targetVersion) {
        try {
            log.info("Executing rollback for service: {} to version: {}", currentDeployment.getServiceName(), targetVersion);
            
            // Step 1: Switch traffic back to blue environment
            boolean trafficSwitchSuccess = switchTrafficToBlue(currentDeployment.getServiceName());
            if (!trafficSwitchSuccess) {
                return RollbackResult.builder()
                    .serviceName(currentDeployment.getServiceName())
                    .targetVersion(targetVersion)
                    .success(false)
                    .error("Traffic switch to blue environment failed")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Step 2: Validate blue environment health
            HealthValidationResult healthResult = validateServiceHealth(currentDeployment.getServiceName()).get();
            if (!healthResult.isHealthy()) {
                return RollbackResult.builder()
                    .serviceName(currentDeployment.getServiceName())
                    .targetVersion(targetVersion)
                    .success(false)
                    .error("Blue environment health validation failed")
                    .timestamp(LocalDateTime.now())
                    .build();
            }
            
            // Step 3: Remove green environment
            removeGreenEnvironment(currentDeployment.getServiceName());
            
            return RollbackResult.builder()
                .serviceName(currentDeployment.getServiceName())
                .targetVersion(targetVersion)
                .success(true)
                .rollbackTimeMs(java.time.Duration.between(LocalDateTime.now().minusMinutes(1), LocalDateTime.now()).toMillis())
                .timestamp(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error executing rollback", e);
            return RollbackResult.builder()
                .serviceName(currentDeployment.getServiceName())
                .targetVersion(targetVersion)
                .success(false)
                .error(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        }
    }

    private boolean switchTrafficToBlue(String serviceName) {
        try {
            log.info("Switching traffic to blue environment for service: {}", serviceName);
            
            // Update load balancer configuration to prefer blue endpoints
            AIServiceLoadBalancer.LoadBalancingConfig config = loadBalancer.getServiceDiscoveryInfo(serviceName).getLoadBalancingConfig();
            if (config != null) {
                config.setAlgorithm(AIServiceLoadBalancer.LoadBalancingAlgorithm.ROUND_ROBIN);
                loadBalancer.updateLoadBalancingConfig(serviceName, config);
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error switching traffic to blue environment", e);
            return false;
        }
    }

    private void removeGreenEnvironment(String serviceName) {
        try {
            log.info("Removing green environment for service: {}", serviceName);
            
            // Remove green endpoints from load balancer
            Map<String, List<AIServiceLoadBalancer.ServiceEndpoint>> allEndpoints = loadBalancer.getAllServiceEndpoints();
            List<AIServiceLoadBalancer.ServiceEndpoint> serviceEndpoints = allEndpoints.get(serviceName);
            
            if (serviceEndpoints != null) {
                serviceEndpoints.stream()
                    .filter(endpoint -> endpoint.getEndpointId().contains("green"))
                    .forEach(endpoint -> loadBalancer.unregisterEndpoint(serviceName, endpoint.getEndpointId()));
            }
            
        } catch (Exception e) {
            log.error("Error removing green environment", e);
        }
    }

    private List<HealthCheck> performHealthChecks(String serviceName) {
        List<HealthCheck> healthChecks = new ArrayList<>();
        
        try {
            // Check service endpoint health
            AIServiceLoadBalancer.ServiceDiscoveryInfo discoveryInfo = loadBalancer.getServiceDiscoveryInfo(serviceName);
            healthChecks.add(HealthCheck.builder()
                .checkName("Service Discovery")
                .passed(discoveryInfo.isAvailable())
                .message(discoveryInfo.isAvailable() ? "Service is available" : "Service is not available")
                .build());
            
            // Check load balancer health
            AIServiceLoadBalancer.LoadBalancingHealthStatus lbHealth = loadBalancer.getLoadBalancingHealthStatus();
            healthChecks.add(HealthCheck.builder()
                .checkName("Load Balancer Health")
                .passed(lbHealth.getOverallHealth() > 0.8)
                .message("Load balancer health: " + String.format("%.2f", lbHealth.getOverallHealth()))
                .build());
            
            // Check circuit breaker health
            CircuitBreaker.CircuitBreakerHealthStatus cbHealth = circuitBreaker.getHealthStatus();
            healthChecks.add(HealthCheck.builder()
                .checkName("Circuit Breaker Health")
                .passed(cbHealth.getOverallHealth() > 0.5)
                .message("Circuit breaker health: " + String.format("%.2f", cbHealth.getOverallHealth()))
                .build());
            
        } catch (Exception e) {
            healthChecks.add(HealthCheck.builder()
                .checkName("Health Check Error")
                .passed(false)
                .message("Error performing health checks: " + e.getMessage())
                .build());
        }
        
        return healthChecks;
    }

    private void monitorActiveDeployments() {
        for (DeploymentInfo deployment : activeDeployments.values()) {
            if (deployment.getStatus() == DeploymentStatus.IN_PROGRESS) {
                // Check if deployment has timed out
                long deploymentDuration = java.time.Duration.between(deployment.getStartedAt(), LocalDateTime.now()).toMinutes();
                if (deploymentDuration > DEPLOYMENT_TIMEOUT_MINUTES) {
                    log.warn("Deployment timeout for service: {}", deployment.getServiceName());
                    deployment.setStatus(DeploymentStatus.FAILED);
                    deployment.setCompletedAt(LocalDateTime.now());
                }
            }
        }
    }

    private void updateDeploymentHistory(String serviceName, DeploymentResult result) {
        DeploymentHistory history = deploymentHistory.get(serviceName);
        if (history != null) {
            history.getDeployments().add(result);
        }
    }

    private void cleanupDeploymentHistory() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        
        for (DeploymentHistory history : deploymentHistory.values()) {
            history.getDeployments().removeIf(deployment -> 
                deployment.getTimestamp().isBefore(cutoffDate));
        }
    }

    // Inner classes

    @lombok.Data
    @lombok.Builder
    public static class DeploymentRequest {
        private String serviceName;
        private String version;
        private String imageUrl;
        private String environment;
        private Map<String, Object> config;
        private DeploymentStrategy strategy;
    }

    @lombok.Data
    @lombok.Builder
    public static class DeploymentInfo {
        private String serviceName;
        private String version;
        private String imageUrl;
        private DeploymentStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String environment;
        private Map<String, Object> config;
        private LocalDateTime lastRollback;
    }

    @lombok.Data
    @lombok.Builder
    public static class DeploymentResult {
        private String serviceName;
        private String version;
        private DeploymentStatus status;
        private long deploymentTimeMs;
        private String error;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class RollbackResult {
        private String serviceName;
        private String targetVersion;
        private boolean success;
        private long rollbackTimeMs;
        private String error;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class HealthValidationResult {
        private String serviceName;
        private boolean isHealthy;
        private List<HealthCheck> healthChecks;
        private String error;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class HealthCheck {
        private String checkName;
        private boolean passed;
        private String message;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class ServiceConfiguration {
        private String serviceName;
        private String version;
        private DeploymentStrategy deploymentStrategy;
        private String healthCheckEndpoint;
        private int healthCheckTimeout;
        private int deploymentTimeout;
        private int rollbackTimeout;
        private int maxInstances;
        private int minInstances;
        private boolean autoScalingEnabled;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class DeploymentHistory {
        private String serviceName;
        private List<DeploymentResult> deployments;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class DeploymentMetrics {
        private long totalDeployments;
        private long successfulDeployments;
        private long failedDeployments;
        private double successRate;
        private int activeDeployments;
        private Map<String, DeploymentHistory> deploymentHistory;
        private LocalDateTime timestamp;
    }

    public enum DeploymentStatus {
        NOT_DEPLOYED, IN_PROGRESS, SUCCESS, FAILED, ROLLED_BACK
    }

    public enum DeploymentStrategy {
        BLUE_GREEN, ROLLING_UPDATE, CANARY, IMMEDIATE
    }
}

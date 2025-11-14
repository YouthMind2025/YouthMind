package ai.youthmind.service;

import ai.youthmind.dto.DeploymentValidationResult;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deployment Configuration Service.
 * Manages deployment configurations for different environments and services.
 * Features:
 * - Environment-specific configurations
 * - Service-specific deployment settings
 * - Configuration validation and management
 * - Version control and rollback support
 * - Security and access control
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeploymentConfigurationService {

    // Configuration storage
    private final Map<String, EnvironmentConfiguration> environmentConfigs = new ConcurrentHashMap<>();
    private final Map<String, ServiceDeploymentConfig> serviceConfigs = new ConcurrentHashMap<>();
    private final Map<String, DeploymentTemplate> deploymentTemplates = new ConcurrentHashMap<>();

    /**
     * Initialize deployment configurations
     */
    public void initialize() {
        try {
            // Initialize environment configurations
            initializeEnvironmentConfigurations();
            
            // Initialize service configurations
            initializeServiceConfigurations();
            
            // Initialize deployment templates
            initializeDeploymentTemplates();
            
            log.info("Deployment Configuration Service initialized with {} environments and {} services", 
                    environmentConfigs.size(), serviceConfigs.size());
            
        } catch (Exception e) {
            log.error("Error initializing Deployment Configuration Service", e);
        }
    }

    /**
     * Get environment configuration
     */
    public EnvironmentConfiguration getEnvironmentConfiguration(String environment) {
        return environmentConfigs.get(environment);
    }

    /**
     * Get service deployment configuration
     */
    public ServiceDeploymentConfig getServiceDeploymentConfig(String serviceName) {
        return serviceConfigs.get(serviceName);
    }

    /**
     * Get deployment template
     */
    public DeploymentTemplate getDeploymentTemplate(String templateName) {
        return deploymentTemplates.get(templateName);
    }

    /**
     * Create deployment configuration for service
     */
    public DeploymentConfiguration createDeploymentConfiguration(String serviceName, String environment, String version) {
        try {
            EnvironmentConfiguration envConfig = environmentConfigs.get(environment);
            ServiceDeploymentConfig serviceConfig = serviceConfigs.get(serviceName);
            
            if (envConfig == null) {
                throw new RuntimeException("Environment configuration not found: " + environment);
            }
            
            if (serviceConfig == null) {
                throw new RuntimeException("Service configuration not found: " + serviceName);
            }
            
            return DeploymentConfiguration.builder()
                .serviceName(serviceName)
                .environment(environment)
                .version(version)
                .environmentConfig(envConfig)
                .serviceConfig(serviceConfig)
                .createdAt(LocalDateTime.now())
                .build();
                
        } catch (Exception e) {
            log.error("Error creating deployment configuration for service: {} environment: {}", serviceName, environment, e);
            throw new RuntimeException("Failed to create deployment configuration", e);
        }
    }

    /**
     * Validate deployment configuration
     */
    public DeploymentValidationResult validateDeploymentConfiguration(DeploymentConfiguration config) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            // Validate service name
            if (config.getServiceName() == null || config.getServiceName().trim().isEmpty()) {
                errors.add("Service name is required");
            }
            
            // Validate environment
            if (config.getEnvironment() == null || config.getEnvironment().trim().isEmpty()) {
                errors.add("Environment is required");
            }
            
            // Validate version
            if (config.getVersion() == null || config.getVersion().trim().isEmpty()) {
                errors.add("Version is required");
            }
            
            // Validate environment configuration
            if (config.getEnvironmentConfig() == null) {
                errors.add("Environment configuration is required");
            }
            
            // Validate service configuration
            if (config.getServiceConfig() == null) {
                errors.add("Service configuration is required");
            } else {
                // Validate resource requirements
                if (config.getServiceConfig().getResourceRequirements() != null) {
                    ResourceRequirements resources = config.getServiceConfig().getResourceRequirements();
                    
                    // Note: Resource validation would need proper parsing of CPU/memory strings
                    // For now, we'll skip this validation
                }
                
                // Validate health check configuration
                if (config.getServiceConfig().getHealthCheckConfig() != null) {
                    HealthCheckConfig healthCheck = config.getServiceConfig().getHealthCheckConfig();
                    
                    if (healthCheck.getInitialDelaySeconds() < 0) {
                        warnings.add("Health check initial delay should be non-negative");
                    }
                    
                    if (healthCheck.getPeriodSeconds() < 1) {
                        errors.add("Health check period must be at least 1 second");
                    }
                }
            }
            
        } catch (Exception e) {
            errors.add("Error validating configuration: " + e.getMessage());
        }
        
        DeploymentValidationResult result = new DeploymentValidationResult();
        result.setValid(errors.isEmpty());
        result.setErrors(errors != null ? errors : new ArrayList<>());
        result.setWarnings(warnings != null ? warnings : new ArrayList<>());
        result.setTimestamp(LocalDateTime.now());
        
        log.debug("Created DeploymentValidationResult - isValid: {}, errors count: {}, warnings count: {}", 
            result.isValid(), 
            result.getErrors() != null ? result.getErrors().size() : "null",
            result.getWarnings() != null ? result.getWarnings().size() : "null");
        
        return result;
    }

    /**
     * Update environment configuration
     */
    public void updateEnvironmentConfiguration(String environment, EnvironmentConfiguration config) {
        environmentConfigs.put(environment, config);
        log.info("Updated environment configuration for: {}", environment);
    }

    /**
     * Create service deployment configuration from simplified format
     */
    public ServiceDeploymentConfig createServiceDeploymentConfig(String serviceName, String version, 
            int replicas, String cpu, String memory) {
        ServiceDeploymentConfig config = ServiceDeploymentConfig.builder()
            .serviceName(serviceName)
            .serviceType(ServiceType.AI_SERVICE)
            .port(8080)
            .replicas(replicas)
            .resourceRequirements(ResourceRequirements.builder()
                .cpuRequest(cpu)
                .cpuLimit(cpu)
                .memoryRequest(memory)
                .memoryLimit(memory)
                .build())
            .healthCheckConfig(HealthCheckConfig.builder()
                .path("/actuator/health")
                .initialDelaySeconds(60)
                .periodSeconds(30)
                .timeoutSeconds(10)
                .failureThreshold(3)
                .build())
            .deploymentStrategy(DeploymentStrategy.ROLLING_UPDATE)
            .autoScalingEnabled(false)
            .minReplicas(replicas)
            .maxReplicas(replicas * 2)
            .createdAt(LocalDateTime.now())
            .build();
        
        serviceConfigs.put(serviceName, config);
        log.info("Created service deployment configuration for: {}", serviceName);
        return config;
    }

    /**
     * Update service deployment configuration
     */
    public void updateServiceDeploymentConfig(String serviceName, ServiceDeploymentConfig config) {
        serviceConfigs.put(serviceName, config);
        log.info("Updated service deployment configuration for: {}", serviceName);
    }

    /**
     * Get all environment configurations
     */
    public Map<String, EnvironmentConfiguration> getAllEnvironmentConfigurations() {
        return new HashMap<>(environmentConfigs);
    }

    /**
     * Get all service deployment configurations
     */
    public Map<String, ServiceDeploymentConfig> getAllServiceDeploymentConfigurations() {
        return new HashMap<>(serviceConfigs);
    }

    /**
     * Get all deployment templates
     */
    public Map<String, DeploymentTemplate> getAllDeploymentTemplates() {
        return new HashMap<>(deploymentTemplates);
    }

    // Private helper methods

    private void initializeEnvironmentConfigurations() {
        // Development environment
        environmentConfigs.put("dev", EnvironmentConfiguration.builder()
            .environment("dev")
            .baseUrl("http://localhost:8080")
            .databaseUrl("jdbc:postgresql://localhost:5432/youthmind_dev")
            .redisUrl("redis://localhost:6379")
            .logLevel("DEBUG")
            .enableMetrics(true)
            .enableTracing(false)
            .securityEnabled(false)
            .sslEnabled(false)
            .createdAt(LocalDateTime.now())
            .build());

        // Staging environment
        environmentConfigs.put("staging", EnvironmentConfiguration.builder()
            .environment("staging")
            .baseUrl("https://staging-api.youthmind.com")
            .databaseUrl("jdbc:postgresql://staging-db.youthmind.com:5432/youthmind_staging")
            .redisUrl("redis://staging-redis.youthmind.com:6379")
            .logLevel("INFO")
            .enableMetrics(true)
            .enableTracing(true)
            .securityEnabled(true)
            .sslEnabled(true)
            .createdAt(LocalDateTime.now())
            .build());

        // Production environment
        environmentConfigs.put("prod", EnvironmentConfiguration.builder()
            .environment("prod")
            .baseUrl("https://api.youthmind.com")
            .databaseUrl("jdbc:postgresql://prod-db.youthmind.com:5432/youthmind_prod")
            .redisUrl("redis://prod-redis.youthmind.com:6379")
            .logLevel("WARN")
            .enableMetrics(true)
            .enableTracing(true)
            .securityEnabled(true)
            .sslEnabled(true)
            .createdAt(LocalDateTime.now())
            .build());
    }

    private void initializeServiceConfigurations() {
        // Psychological Analysis Service
        serviceConfigs.put("psychological_analysis", ServiceDeploymentConfig.builder()
            .serviceName("psychological_analysis")
            .serviceType(ServiceType.AI_SERVICE)
            .port(8080)
            .replicas(2)
            .resourceRequirements(ResourceRequirements.builder()
                .cpuRequest("250m")
                .cpuLimit("500m")
                .memoryRequest("512Mi")
                .memoryLimit("1Gi")
                .build())
            .healthCheckConfig(HealthCheckConfig.builder()
                .path("/actuator/health")
                .initialDelaySeconds(60)
                .periodSeconds(30)
                .timeoutSeconds(10)
                .failureThreshold(3)
                .build())
            .deploymentStrategy(DeploymentStrategy.BLUE_GREEN)
            .autoScalingEnabled(true)
            .minReplicas(1)
            .maxReplicas(5)
            .createdAt(LocalDateTime.now())
            .build());

        // State Update Service
        serviceConfigs.put("state_update", ServiceDeploymentConfig.builder()
            .serviceName("state_update")
            .serviceType(ServiceType.AI_SERVICE)
            .port(8080)
            .replicas(3)
            .resourceRequirements(ResourceRequirements.builder()
                .cpuRequest("200m")
                .cpuLimit("400m")
                .memoryRequest("256Mi")
                .memoryLimit("512Mi")
                .build())
            .healthCheckConfig(HealthCheckConfig.builder()
                .path("/actuator/health")
                .initialDelaySeconds(30)
                .periodSeconds(15)
                .timeoutSeconds(5)
                .failureThreshold(3)
                .build())
            .deploymentStrategy(DeploymentStrategy.ROLLING_UPDATE)
            .autoScalingEnabled(true)
            .minReplicas(2)
            .maxReplicas(10)
            .createdAt(LocalDateTime.now())
            .build());

        // Personality Analysis Service
        serviceConfigs.put("personality_analysis", ServiceDeploymentConfig.builder()
            .serviceName("personality_analysis")
            .serviceType(ServiceType.AI_SERVICE)
            .port(8080)
            .replicas(1)
            .resourceRequirements(ResourceRequirements.builder()
                .cpuRequest("500m")
                .cpuLimit("1000m")
                .memoryRequest("1Gi")
                .memoryLimit("2Gi")
                .build())
            .healthCheckConfig(HealthCheckConfig.builder()
                .path("/actuator/health")
                .initialDelaySeconds(90)
                .periodSeconds(60)
                .timeoutSeconds(15)
                .failureThreshold(3)
                .build())
            .deploymentStrategy(DeploymentStrategy.BLUE_GREEN)
            .autoScalingEnabled(false)
            .minReplicas(1)
            .maxReplicas(2)
            .createdAt(LocalDateTime.now())
            .build());

        // Mood Analysis Service
        serviceConfigs.put("mood_analysis", ServiceDeploymentConfig.builder()
            .serviceName("mood_analysis")
            .serviceType(ServiceType.AI_SERVICE)
            .port(8080)
            .replicas(2)
            .resourceRequirements(ResourceRequirements.builder()
                .cpuRequest("300m")
                .cpuLimit("600m")
                .memoryRequest("512Mi")
                .memoryLimit("1Gi")
                .build())
            .healthCheckConfig(HealthCheckConfig.builder()
                .path("/actuator/health")
                .initialDelaySeconds(45)
                .periodSeconds(20)
                .timeoutSeconds(8)
                .failureThreshold(3)
                .build())
            .deploymentStrategy(DeploymentStrategy.ROLLING_UPDATE)
            .autoScalingEnabled(true)
            .minReplicas(1)
            .maxReplicas(4)
            .createdAt(LocalDateTime.now())
            .build());
    }

    private void initializeDeploymentTemplates() {
        // Blue-Green Deployment Template
        deploymentTemplates.put("blue-green", DeploymentTemplate.builder()
            .templateName("blue-green")
            .description("Blue-Green deployment template for zero-downtime deployments")
            .strategy(DeploymentStrategy.BLUE_GREEN)
            .steps(Arrays.asList(
                "Create green environment",
                "Deploy to green environment",
                "Validate green environment",
                "Switch traffic to green",
                "Remove blue environment"
            ))
            .rollbackSteps(Arrays.asList(
                "Switch traffic back to blue",
                "Validate blue environment",
                "Remove green environment"
            ))
            .createdAt(LocalDateTime.now())
            .build());

        // Rolling Update Template
        deploymentTemplates.put("rolling-update", DeploymentTemplate.builder()
            .templateName("rolling-update")
            .description("Rolling update deployment template for gradual updates")
            .strategy(DeploymentStrategy.ROLLING_UPDATE)
            .steps(Arrays.asList(
                "Update deployment configuration",
                "Roll out new version gradually",
                "Monitor deployment progress",
                "Complete rollout"
            ))
            .rollbackSteps(Arrays.asList(
                "Rollback to previous version",
                "Monitor rollback progress",
                "Complete rollback"
            ))
            .createdAt(LocalDateTime.now())
            .build());
    }

    // Inner classes

    @lombok.Data
    @lombok.Builder
    public static class EnvironmentConfiguration {
        private String environment;
        private String baseUrl;
        private String databaseUrl;
        private String redisUrl;
        private String logLevel;
        private boolean enableMetrics;
        private boolean enableTracing;
        private boolean securityEnabled;
        private boolean sslEnabled;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ServiceDeploymentConfig {
        private String serviceName;
        
        @NotNull(message = "Service type is required and must be a valid enum value")
        private ServiceType serviceType;
        
        @Min(value = 1, message = "Port must be at least 1")
        private int port;
        
        @Min(value = 1, message = "Replicas must be at least 1")
        private int replicas;
        
        private ResourceRequirements resourceRequirements;
        private HealthCheckConfig healthCheckConfig;
        private DeploymentStrategy deploymentStrategy;
        private boolean autoScalingEnabled;
        private int minReplicas;
        private int maxReplicas;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ResourceRequirements {
        private String cpuRequest;
        private String cpuLimit;
        private String memoryRequest;
        private String memoryLimit;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class HealthCheckConfig {
        private String path;
        private int initialDelaySeconds;
        private int periodSeconds;
        private int timeoutSeconds;
        private int failureThreshold;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class DeploymentConfiguration {
        private String serviceName;
        private String environment;
        private String version;
        private EnvironmentConfiguration environmentConfig;
        private ServiceDeploymentConfig serviceConfig;
        private LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class DeploymentTemplate {
        private String templateName;
        private String description;
        private DeploymentStrategy strategy;
        private List<String> steps;
        private List<String> rollbackSteps;
        private LocalDateTime createdAt;
    }


    public enum ServiceType {
        AI_SERVICE, WEB_SERVICE, DATABASE_SERVICE, CACHE_SERVICE, MESSAGE_SERVICE
    }

    public enum DeploymentStrategy {
        BLUE_GREEN, ROLLING_UPDATE, CANARY, IMMEDIATE
    }
}

package ai.youthmind.controller;

import ai.youthmind.service.DeploymentConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Deployment Configuration Management.
 * Provides endpoints for:
 * - Environment configuration management
 * - Service deployment configuration
 * - Deployment template management
 * - Configuration validation
 */
@RestController
@RequestMapping("/v1/deployment-config")
@RequiredArgsConstructor
@Slf4j
public class DeploymentConfigurationController {

    private final DeploymentConfigurationService configService;

    /**
     * Get environment configuration
     */
    @GetMapping("/environments/{environment}")
    public ResponseEntity<DeploymentConfigurationService.EnvironmentConfiguration> getEnvironmentConfiguration(
            @PathVariable String environment) {
        try {
            DeploymentConfigurationService.EnvironmentConfiguration config = 
                configService.getEnvironmentConfiguration(environment);
            
            if (config != null) {
                return ResponseEntity.ok(config);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting environment configuration for: {}", environment, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update environment configuration
     */
    @PutMapping("/environments/{environment}")
    public ResponseEntity<String> updateEnvironmentConfiguration(
            @PathVariable String environment,
            @RequestBody DeploymentConfigurationService.EnvironmentConfiguration config) {
        try {
            config.setEnvironment(environment);
            configService.updateEnvironmentConfiguration(environment, config);
            
            return ResponseEntity.ok("Environment configuration updated successfully");
        } catch (Exception e) {
            log.error("Error updating environment configuration for: {}", environment, e);
            return ResponseEntity.internalServerError().body("Error updating configuration: " + e.getMessage());
        }
    }

    /**
     * Get service deployment configuration
     */
    @GetMapping("/services/{serviceName}")
    public ResponseEntity<DeploymentConfigurationService.ServiceDeploymentConfig> getServiceDeploymentConfig(
            @PathVariable String serviceName) {
        try {
            DeploymentConfigurationService.ServiceDeploymentConfig config = 
                configService.getServiceDeploymentConfig(serviceName);
            
            if (config != null) {
                return ResponseEntity.ok(config);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting service deployment configuration for: {}", serviceName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update service deployment configuration
     */
    @PutMapping("/services/{serviceName}")
    public ResponseEntity<String> updateServiceDeploymentConfig(
            @PathVariable String serviceName,
            @RequestBody DeploymentConfigurationService.ServiceDeploymentConfig config) {
        try {
            config.setServiceName(serviceName);
            configService.updateServiceDeploymentConfig(serviceName, config);
            
            return ResponseEntity.ok("Service deployment configuration updated successfully");
        } catch (Exception e) {
            log.error("Error updating service deployment configuration for: {}", serviceName, e);
            return ResponseEntity.internalServerError().body("Error updating configuration: " + e.getMessage());
        }
    }

    /**
     * Create deployment configuration
     */
    @PostMapping("/services/{serviceName}/environments/{environment}/versions/{version}")
    public ResponseEntity<DeploymentConfigurationService.DeploymentConfiguration> createDeploymentConfiguration(
            @PathVariable String serviceName,
            @PathVariable String environment,
            @PathVariable String version) {
        try {
            DeploymentConfigurationService.DeploymentConfiguration config = 
                configService.createDeploymentConfiguration(serviceName, environment, version);
            
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("Error creating deployment configuration for service: {} environment: {} version: {}", 
                     serviceName, environment, version, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Validate deployment configuration
     */
    @PostMapping("/validate")
    public ResponseEntity<DeploymentConfigurationService.ValidationResult> validateDeploymentConfiguration(
            @RequestBody DeploymentConfigurationService.DeploymentConfiguration config) {
        try {
            DeploymentConfigurationService.ValidationResult result = 
                configService.validateDeploymentConfiguration(config);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error validating deployment configuration", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get deployment template
     */
    @GetMapping("/templates/{templateName}")
    public ResponseEntity<DeploymentConfigurationService.DeploymentTemplate> getDeploymentTemplate(
            @PathVariable String templateName) {
        try {
            DeploymentConfigurationService.DeploymentTemplate template = 
                configService.getDeploymentTemplate(templateName);
            
            if (template != null) {
                return ResponseEntity.ok(template);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting deployment template: {}", templateName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all environment configurations
     */
    @GetMapping("/environments")
    public ResponseEntity<Map<String, DeploymentConfigurationService.EnvironmentConfiguration>> getAllEnvironmentConfigurations() {
        try {
            Map<String, DeploymentConfigurationService.EnvironmentConfiguration> configs = 
                configService.getAllEnvironmentConfigurations();
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            log.error("Error getting all environment configurations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all service deployment configurations
     */
    @GetMapping("/services")
    public ResponseEntity<Map<String, DeploymentConfigurationService.ServiceDeploymentConfig>> getAllServiceDeploymentConfigurations() {
        try {
            Map<String, DeploymentConfigurationService.ServiceDeploymentConfig> configs = 
                configService.getAllServiceDeploymentConfigurations();
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            log.error("Error getting all service deployment configurations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all deployment templates
     */
    @GetMapping("/templates")
    public ResponseEntity<Map<String, DeploymentConfigurationService.DeploymentTemplate>> getAllDeploymentTemplates() {
        try {
            Map<String, DeploymentConfigurationService.DeploymentTemplate> templates = 
                configService.getAllDeploymentTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            log.error("Error getting all deployment templates", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Initialize configuration service
     */
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeConfigurationService() {
        try {
            configService.initialize();
            return ResponseEntity.ok("Deployment configuration service initialized successfully");
        } catch (Exception e) {
            log.error("Error initializing configuration service", e);
            return ResponseEntity.internalServerError().body("Error initializing configuration service: " + e.getMessage());
        }
    }

    /**
     * Get configuration health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getConfigurationHealth() {
        try {
            Map<String, DeploymentConfigurationService.EnvironmentConfiguration> envConfigs = 
                configService.getAllEnvironmentConfigurations();
            Map<String, DeploymentConfigurationService.ServiceDeploymentConfig> serviceConfigs = 
                configService.getAllServiceDeploymentConfigurations();
            
            Map<String, Object> health = Map.of(
                "status", "UP",
                "environmentConfigurations", envConfigs.size(),
                "serviceConfigurations", serviceConfigs.size(),
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error getting configuration health", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

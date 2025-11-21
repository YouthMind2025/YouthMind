package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing psychological analysis model versions
 * Implements sophisticated model versioning and deployment algorithms
 */
@Service
@Transactional
public class ModelVersioningService {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelVersioningService.class);
    
    // Versioning constants
    private static final String DEFAULT_VERSION_SCHEME = "semantic"; // semantic, numeric, timestamp
    private static final int MAX_VERSIONS_PER_MODEL = 50;
    private static final int DEFAULT_ROLLBACK_LIMIT = 5;
    
    // Redis keys
    private static final String MODEL_VERSIONS_KEY = "model_versions:";
    private static final String ACTIVE_VERSION_KEY = "active_version:";
    private static final String VERSION_METADATA_KEY = "version_metadata:";
    private static final String DEPLOYMENT_HISTORY_KEY = "deployment_history:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Version a model
     * @param modelId Model ID to version
     * @param deploymentConfig Deployment configuration
     * @return Versioning result
     */
    public Map<String, Object> versionModel(String modelId, Map<String, Object> deploymentConfig) {
        try {
            logger.info("Versioning model {} with config: {}", modelId, deploymentConfig);
            
            // Generate new version
            String newVersion = generateVersion(modelId, deploymentConfig);
            
            // Create version metadata
            Map<String, Object> versionMetadata = createVersionMetadata(modelId, newVersion, deploymentConfig);
            
            // Store version information
            storeVersion(modelId, newVersion, versionMetadata);
            
            // Update active version if specified
            if (deploymentConfig.containsKey("setAsActive") && (Boolean) deploymentConfig.get("setAsActive")) {
                setActiveVersion(modelId, newVersion);
            }
            
            // Create versioning result
            Map<String, Object> result = new HashMap<>();
            result.put("modelId", modelId);
            result.put("newVersion", newVersion);
            result.put("versionMetadata", versionMetadata);
            result.put("versioningMethod", deploymentConfig.getOrDefault("versionScheme", DEFAULT_VERSION_SCHEME));
            result.put("timestamp", LocalDateTime.now());
            
            // Store versioning result
            storeVersioningResult(modelId, result);
            
            logger.info("Model versioning completed for model {}: version {}", modelId, newVersion);
            return result;
            
        } catch (Exception e) {
            logger.error("Error versioning model {}: {}", modelId, e.getMessage(), e);
            return Map.of(
                "status", "ERROR",
                "message", "Failed to version model: " + e.getMessage(),
                "modelId", modelId,
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    /**
     * Get model versions
     * @param modelId Model ID
     * @return List of model versions
     */
    public Map<String, Object> getModelVersions(String modelId) {
        try {
            logger.debug("Getting versions for model {}", modelId);
            
            String versionsKey = MODEL_VERSIONS_KEY + modelId;
            @SuppressWarnings("unchecked")
            List<Object> versionObjects = (List<Object>) redisTemplate.opsForList().range(versionsKey, 0, -1);
            List<String> versions = versionObjects != null ? 
                versionObjects.stream().map(Object::toString).collect(Collectors.toList()) : new ArrayList<>();
            
            // Get version metadata for each version
            List<Map<String, Object>> versionDetails = new ArrayList<>();
            for (String version : versions) {
                Map<String, Object> metadata = getVersionMetadata(modelId, version);
                versionDetails.add(metadata);
            }
            
            // Sort versions by creation date (newest first)
            versionDetails.sort((a, b) -> {
                LocalDateTime dateA = LocalDateTime.parse((String) a.get("createdAt"));
                LocalDateTime dateB = LocalDateTime.parse((String) b.get("createdAt"));
                return dateB.compareTo(dateA);
            });
            
            // Get active version
            String activeVersion = getActiveVersion(modelId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("modelId", modelId);
            result.put("versions", versionDetails);
            result.put("activeVersion", activeVersion);
            result.put("totalVersions", versions.size());
            result.put("timestamp", LocalDateTime.now());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error getting versions for model {}: {}", modelId, e.getMessage(), e);
            return Map.of(
                "status", "ERROR",
                "message", "Failed to get model versions: " + e.getMessage(),
                "modelId", modelId,
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    /**
     * Set active version
     * @param modelId Model ID
     * @param version Version to set as active
     * @return Result of setting active version
     */
    public Map<String, Object> setActiveVersion(String modelId, String version) {
        try {
            logger.info("Setting active version for model {} to {}", modelId, version);
            
            // Validate version exists
            if (!versionExists(modelId, version)) {
                return Map.of(
                    "status", "ERROR",
                    "message", "Version does not exist: " + version,
                    "modelId", modelId,
                    "timestamp", LocalDateTime.now()
                );
            }
            
            // Get previous active version
            String previousActiveVersion = getActiveVersion(modelId);
            
            // Set new active version
            String activeVersionKey = ACTIVE_VERSION_KEY + modelId;
            redisTemplate.opsForValue().set(activeVersionKey, version, 365, TimeUnit.DAYS);
            
            // Update version metadata
            Map<String, Object> versionMetadata = getVersionMetadata(modelId, version);
            versionMetadata.put("isActive", true);
            versionMetadata.put("activatedAt", LocalDateTime.now().toString());
            storeVersionMetadata(modelId, version, versionMetadata);
            
            // Update previous active version metadata
            if (previousActiveVersion != null && !previousActiveVersion.equals(version)) {
                Map<String, Object> previousMetadata = getVersionMetadata(modelId, previousActiveVersion);
                previousMetadata.put("isActive", false);
                previousMetadata.put("deactivatedAt", LocalDateTime.now().toString());
                storeVersionMetadata(modelId, previousActiveVersion, previousMetadata);
            }
            
            // Record deployment history
            recordDeploymentHistory(modelId, version, previousActiveVersion);
            
            Map<String, Object> result = new HashMap<>();
            result.put("modelId", modelId);
            result.put("activeVersion", version);
            result.put("previousActiveVersion", previousActiveVersion);
            result.put("status", "SUCCESS");
            result.put("timestamp", LocalDateTime.now());
            
            logger.info("Active version set for model {}: {} (previous: {})", modelId, version, previousActiveVersion);
            return result;
            
        } catch (Exception e) {
            logger.error("Error setting active version for model {} to {}: {}", modelId, version, e.getMessage(), e);
            return Map.of(
                "status", "ERROR",
                "message", "Failed to set active version: " + e.getMessage(),
                "modelId", modelId,
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    /**
     * Rollback to previous version
     * @param modelId Model ID
     * @param rollbackConfig Rollback configuration
     * @return Rollback result
     */
    public Map<String, Object> rollbackModel(String modelId, Map<String, Object> rollbackConfig) {
        try {
            logger.info("Rolling back model {} with config: {}", modelId, rollbackConfig);
            
            // Get current active version
            String currentActiveVersion = getActiveVersion(modelId);
            if (currentActiveVersion == null) {
                return Map.of(
                    "status", "ERROR",
                    "message", "No active version found for model",
                    "modelId", modelId,
                    "timestamp", LocalDateTime.now()
                );
            }
            
            // Determine target version for rollback
            String targetVersion = determineRollbackTarget(modelId, currentActiveVersion, rollbackConfig);
            
            if (targetVersion == null) {
                return Map.of(
                    "status", "ERROR",
                    "message", "No suitable version found for rollback",
                    "modelId", modelId,
                    "timestamp", LocalDateTime.now()
                );
            }
            
            // Perform rollback
            Map<String, Object> rollbackResult = setActiveVersion(modelId, targetVersion);
            
            // Add rollback-specific information
            rollbackResult.put("rollbackType", rollbackConfig.getOrDefault("rollbackType", "automatic"));
            rollbackResult.put("rollbackReason", rollbackConfig.getOrDefault("reason", "Performance issues"));
            rollbackResult.put("rollbackFrom", currentActiveVersion);
            rollbackResult.put("rollbackTo", targetVersion);
            
            // Record rollback in history
            recordRollbackHistory(modelId, currentActiveVersion, targetVersion, rollbackConfig);
            
            logger.info("Model rollback completed for model {}: {} -> {}", modelId, currentActiveVersion, targetVersion);
            return rollbackResult;
            
        } catch (Exception e) {
            logger.error("Error rolling back model {}: {}", modelId, e.getMessage(), e);
            return Map.of(
                "status", "ERROR",
                "message", "Failed to rollback model: " + e.getMessage(),
                "modelId", modelId,
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    /**
     * Compare model versions
     * @param modelId Model ID
     * @param version1 First version to compare
     * @param version2 Second version to compare
     * @return Comparison result
     */
    public Map<String, Object> compareVersions(String modelId, String version1, String version2) {
        try {
            logger.info("Comparing versions {} and {} for model {}", version1, version2, modelId);
            
            // Get metadata for both versions
            Map<String, Object> metadata1 = getVersionMetadata(modelId, version1);
            Map<String, Object> metadata2 = getVersionMetadata(modelId, version2);
            
            if (metadata1.isEmpty() || metadata2.isEmpty()) {
                return Map.of(
                    "status", "ERROR",
                    "message", "One or both versions not found",
                    "modelId", modelId,
                    "timestamp", LocalDateTime.now()
                );
            }
            
            // Compare version metadata
            Map<String, Object> comparison = compareVersionMetadata(metadata1, metadata2);
            
            // Compare performance metrics if available
            Map<String, Object> performanceComparison = comparePerformanceMetrics(modelId, version1, version2);
            
            // Create comparison result
            Map<String, Object> result = new HashMap<>();
            result.put("modelId", modelId);
            result.put("version1", version1);
            result.put("version2", version2);
            result.put("metadataComparison", comparison);
            result.put("performanceComparison", performanceComparison);
            result.put("comparisonTimestamp", LocalDateTime.now());
            
            logger.info("Version comparison completed for model {}: {} vs {}", modelId, version1, version2);
            return result;
            
        } catch (Exception e) {
            logger.error("Error comparing versions {} and {} for model {}: {}", version1, version2, modelId, e.getMessage(), e);
            return Map.of(
                "status", "ERROR",
                "message", "Failed to compare versions: " + e.getMessage(),
                "modelId", modelId,
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    /**
     * Get deployment history for a model
     * @param modelId Model ID
     * @return Deployment history
     */
    public Map<String, Object> getDeploymentHistory(String modelId) {
        try {
            logger.debug("Getting deployment history for model {}", modelId);
            
            String historyKey = DEPLOYMENT_HISTORY_KEY + modelId;
            @SuppressWarnings("unchecked")
            List<Object> historyObjects = (List<Object>) redisTemplate.opsForList().range(historyKey, 0, -1);
            List<Map<String, Object>> history = historyObjects != null ? 
                historyObjects.stream()
                    .filter(obj -> obj instanceof Map)
                    .map(obj -> (Map<String, Object>) obj)
                    .collect(Collectors.toList()) : new ArrayList<>();
            
            Map<String, Object> result = new HashMap<>();
            result.put("modelId", modelId);
            result.put("deploymentHistory", history != null ? history : Collections.emptyList());
            result.put("totalDeployments", history != null ? history.size() : 0);
            result.put("timestamp", LocalDateTime.now());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error getting deployment history for model {}: {}", modelId, e.getMessage(), e);
            return Map.of(
                "status", "ERROR",
                "message", "Failed to get deployment history: " + e.getMessage(),
                "modelId", modelId,
                "timestamp", LocalDateTime.now()
            );
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private String generateVersion(String modelId, Map<String, Object> deploymentConfig) {
        String versionScheme = (String) deploymentConfig.getOrDefault("versionScheme", DEFAULT_VERSION_SCHEME);
        
        switch (versionScheme.toLowerCase()) {
            case "semantic":
                return generateSemanticVersion(modelId);
            case "numeric":
                return generateNumericVersion(modelId);
            case "timestamp":
                return generateTimestampVersion();
            default:
                return generateSemanticVersion(modelId);
        }
    }
    
    private String generateSemanticVersion(String modelId) {
        // Get existing versions to determine next semantic version
        String versionsKey = MODEL_VERSIONS_KEY + modelId;
        @SuppressWarnings("unchecked")
        List<Object> versionObjects = (List<Object>) redisTemplate.opsForList().range(versionsKey, 0, -1);
        List<String> existingVersions = versionObjects != null ? 
            versionObjects.stream().map(Object::toString).collect(Collectors.toList()) : new ArrayList<>();
        
        if (existingVersions == null || existingVersions.isEmpty()) {
            return "1.0.0";
        }
        
        // Parse existing versions and increment
        List<SemanticVersion> versions = existingVersions.stream()
            .map(this::parseSemanticVersion)
            .filter(Objects::nonNull)
            .sorted(Collections.reverseOrder())
            .collect(Collectors.toList());
        
        if (versions.isEmpty()) {
            return "1.0.0";
        }
        
        SemanticVersion latest = versions.get(0);
        return latest.incrementPatch().toString();
    }
    
    private String generateNumericVersion(String modelId) {
        String versionsKey = MODEL_VERSIONS_KEY + modelId;
        Long versionCount = redisTemplate.opsForList().size(versionsKey);
        return String.valueOf((versionCount != null ? versionCount : 0) + 1);
    }
    
    private String generateTimestampVersion() {
        return String.valueOf(System.currentTimeMillis());
    }
    
    private Map<String, Object> createVersionMetadata(String modelId, String version, Map<String, Object> deploymentConfig) {
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("modelId", modelId);
        metadata.put("version", version);
        metadata.put("createdAt", LocalDateTime.now().toString());
        metadata.put("createdBy", deploymentConfig.getOrDefault("createdBy", "system"));
        metadata.put("description", deploymentConfig.getOrDefault("description", "Model version"));
        metadata.put("tags", deploymentConfig.getOrDefault("tags", Collections.emptyList()));
        metadata.put("isActive", false);
        metadata.put("deploymentConfig", deploymentConfig);
        
        // Add performance metrics if available
        if (deploymentConfig.containsKey("performanceMetrics")) {
            metadata.put("performanceMetrics", deploymentConfig.get("performanceMetrics"));
        }
        
        return metadata;
    }
    
    private void storeVersion(String modelId, String version, Map<String, Object> metadata) {
        try {
            // Store version in list
            String versionsKey = MODEL_VERSIONS_KEY + modelId;
            redisTemplate.opsForList().leftPush(versionsKey, version);
            
            // Limit number of versions
            redisTemplate.opsForList().trim(versionsKey, 0, MAX_VERSIONS_PER_MODEL - 1);
            
            // Store version metadata
            storeVersionMetadata(modelId, version, metadata);
            
        } catch (Exception e) {
            logger.warn("Error storing version {} for model {}: {}", version, modelId, e.getMessage());
        }
    }
    
    private void storeVersionMetadata(String modelId, String version, Map<String, Object> metadata) {
        try {
            String metadataKey = VERSION_METADATA_KEY + modelId + ":" + version;
            redisTemplate.opsForValue().set(metadataKey, metadata, 365, TimeUnit.DAYS);
        } catch (Exception e) {
            logger.warn("Error storing version metadata for model {} version {}: {}", modelId, version, e.getMessage());
        }
    }
    
    private Map<String, Object> getVersionMetadata(String modelId, String version) {
        try {
            String metadataKey = VERSION_METADATA_KEY + modelId + ":" + version;
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) redisTemplate.opsForValue().get(metadataKey);
            return metadata != null ? metadata : new HashMap<>();
        } catch (Exception e) {
            logger.warn("Error getting version metadata for model {} version {}: {}", modelId, version, e.getMessage());
            return new HashMap<>();
        }
    }
    
    private String getActiveVersion(String modelId) {
        try {
            String activeVersionKey = ACTIVE_VERSION_KEY + modelId;
            return (String) redisTemplate.opsForValue().get(activeVersionKey);
        } catch (Exception e) {
            logger.warn("Error getting active version for model {}: {}", modelId, e.getMessage());
            return null;
        }
    }
    
    private boolean versionExists(String modelId, String version) {
        try {
            String versionsKey = MODEL_VERSIONS_KEY + modelId;
            @SuppressWarnings("unchecked")
            List<Object> versionObjects = (List<Object>) redisTemplate.opsForList().range(versionsKey, 0, -1);
            List<String> versions = versionObjects != null ? 
                versionObjects.stream().map(Object::toString).collect(Collectors.toList()) : new ArrayList<>();
            return versions.contains(version);
        } catch (Exception e) {
            logger.warn("Error checking if version exists for model {} version {}: {}", modelId, version, e.getMessage());
            return false;
        }
    }
    
    private String determineRollbackTarget(String modelId, String currentVersion, Map<String, Object> rollbackConfig) {
        String rollbackType = (String) rollbackConfig.getOrDefault("rollbackType", "automatic");
        
        switch (rollbackType.toLowerCase()) {
            case "automatic":
                return findPreviousStableVersion(modelId, currentVersion);
            case "specific":
                return (String) rollbackConfig.get("targetVersion");
            case "steps":
                int steps = (Integer) rollbackConfig.getOrDefault("steps", 1);
                return findVersionBySteps(modelId, currentVersion, steps);
            default:
                return findPreviousStableVersion(modelId, currentVersion);
        }
    }
    
    private String findPreviousStableVersion(String modelId, String currentVersion) {
        try {
            String versionsKey = MODEL_VERSIONS_KEY + modelId;
            @SuppressWarnings("unchecked")
            List<Object> versionObjects = (List<Object>) redisTemplate.opsForList().range(versionsKey, 0, -1);
            List<String> versions = versionObjects != null ? 
                versionObjects.stream().map(Object::toString).collect(Collectors.toList()) : new ArrayList<>();
            
            if (versions.isEmpty()) {
                return null;
            }
            
            // Find previous stable version
            int currentIndex = versions.indexOf(currentVersion);
            for (int i = currentIndex + 1; i < versions.size(); i++) {
                String version = versions.get(i);
                Map<String, Object> metadata = getVersionMetadata(modelId, version);
                
                // Check if version is stable (has good performance metrics)
                if (isStableVersion(metadata)) {
                    return version;
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.warn("Error finding previous stable version for model {}: {}", modelId, e.getMessage());
            return null;
        }
    }
    
    private String findVersionBySteps(String modelId, String currentVersion, int steps) {
        try {
            String versionsKey = MODEL_VERSIONS_KEY + modelId;
            @SuppressWarnings("unchecked")
            List<Object> versionObjects = (List<Object>) redisTemplate.opsForList().range(versionsKey, 0, -1);
            List<String> versions = versionObjects != null ? 
                versionObjects.stream().map(Object::toString).collect(Collectors.toList()) : new ArrayList<>();
            
            if (versions.isEmpty()) {
                return null;
            }
            
            int currentIndex = versions.indexOf(currentVersion);
            int targetIndex = currentIndex + steps;
            
            if (targetIndex >= 0 && targetIndex < versions.size()) {
                return versions.get(targetIndex);
            }
            
            return null;
        } catch (Exception e) {
            logger.warn("Error finding version by steps for model {}: {}", modelId, e.getMessage());
            return null;
        }
    }
    
    private boolean isStableVersion(Map<String, Object> metadata) {
        // Check if version has good performance metrics
        if (metadata.containsKey("performanceMetrics")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metrics = (Map<String, Object>) metadata.get("performanceMetrics");
            Double accuracy = (Double) metrics.get("accuracy");
            return accuracy != null && accuracy >= 0.8; // 80% accuracy threshold
        }
        
        return false;
    }
    
    private Map<String, Object> compareVersionMetadata(Map<String, Object> metadata1, Map<String, Object> metadata2) {
        Map<String, Object> comparison = new HashMap<>();
        
        comparison.put("version1", metadata1.get("version"));
        comparison.put("version2", metadata2.get("version"));
        comparison.put("createdAt1", metadata1.get("createdAt"));
        comparison.put("createdAt2", metadata2.get("createdAt"));
        comparison.put("isActive1", metadata1.get("isActive"));
        comparison.put("isActive2", metadata2.get("isActive"));
        
        // Compare descriptions
        String desc1 = (String) metadata1.get("description");
        String desc2 = (String) metadata2.get("description");
        comparison.put("descriptionChanged", !Objects.equals(desc1, desc2));
        
        return comparison;
    }
    
    private Map<String, Object> comparePerformanceMetrics(String modelId, String version1, String version2) {
        Map<String, Object> comparison = new HashMap<>();
        
        Map<String, Object> metadata1 = getVersionMetadata(modelId, version1);
        Map<String, Object> metadata2 = getVersionMetadata(modelId, version2);
        
        if (metadata1.containsKey("performanceMetrics") && metadata2.containsKey("performanceMetrics")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metrics1 = (Map<String, Object>) metadata1.get("performanceMetrics");
            @SuppressWarnings("unchecked")
            Map<String, Object> metrics2 = (Map<String, Object>) metadata2.get("performanceMetrics");
            
            comparison.put("metrics1", metrics1);
            comparison.put("metrics2", metrics2);
            
            // Calculate improvement
            Double accuracy1 = (Double) metrics1.get("accuracy");
            Double accuracy2 = (Double) metrics2.get("accuracy");
            if (accuracy1 != null && accuracy2 != null) {
                comparison.put("accuracyImprovement", accuracy2 - accuracy1);
            }
        }
        
        return comparison;
    }
    
    private void recordDeploymentHistory(String modelId, String version, String previousVersion) {
        try {
            String historyKey = DEPLOYMENT_HISTORY_KEY + modelId;
            
            Map<String, Object> deploymentRecord = new HashMap<>();
            deploymentRecord.put("version", version);
            deploymentRecord.put("previousVersion", previousVersion);
            deploymentRecord.put("deployedAt", LocalDateTime.now().toString());
            deploymentRecord.put("deploymentType", "version_change");
            
            redisTemplate.opsForList().leftPush(historyKey, deploymentRecord);
            redisTemplate.opsForList().trim(historyKey, 0, 99); // Keep last 100 deployments
        } catch (Exception e) {
            logger.warn("Error recording deployment history for model {}: {}", modelId, e.getMessage());
        }
    }
    
    private void recordRollbackHistory(String modelId, String fromVersion, String toVersion, Map<String, Object> rollbackConfig) {
        try {
            String historyKey = DEPLOYMENT_HISTORY_KEY + modelId;
            
            Map<String, Object> rollbackRecord = new HashMap<>();
            rollbackRecord.put("fromVersion", fromVersion);
            rollbackRecord.put("toVersion", toVersion);
            rollbackRecord.put("rolledBackAt", LocalDateTime.now().toString());
            rollbackRecord.put("rollbackType", rollbackConfig.getOrDefault("rollbackType", "automatic"));
            rollbackRecord.put("rollbackReason", rollbackConfig.getOrDefault("reason", "Performance issues"));
            rollbackRecord.put("deploymentType", "rollback");
            
            redisTemplate.opsForList().leftPush(historyKey, rollbackRecord);
            redisTemplate.opsForList().trim(historyKey, 0, 99); // Keep last 100 deployments
        } catch (Exception e) {
            logger.warn("Error recording rollback history for model {}: {}", modelId, e.getMessage());
        }
    }
    
    private void storeVersioningResult(String modelId, Map<String, Object> result) {
        try {
            String resultKey = "versioning_result:" + modelId;
            redisTemplate.opsForValue().set(resultKey, result, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            logger.warn("Error storing versioning result for model {}: {}", modelId, e.getMessage());
        }
    }
    
    // Helper class for semantic version parsing
    private static class SemanticVersion implements Comparable<SemanticVersion> {
        private final int major;
        private final int minor;
        private final int patch;
        
        public SemanticVersion(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }
        
        public SemanticVersion incrementPatch() {
            return new SemanticVersion(major, minor, patch + 1);
        }
        
        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }
        
        @Override
        public int compareTo(SemanticVersion other) {
            if (major != other.major) return Integer.compare(major, other.major);
            if (minor != other.minor) return Integer.compare(minor, other.minor);
            return Integer.compare(patch, other.patch);
        }
    }
    
    private SemanticVersion parseSemanticVersion(String versionString) {
        try {
            String[] parts = versionString.split("\\.");
            if (parts.length == 3) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int patch = Integer.parseInt(parts[2]);
                return new SemanticVersion(major, minor, patch);
            }
        } catch (NumberFormatException e) {
            logger.warn("Error parsing semantic version: {}", versionString);
        }
        return null;
    }
}

package ai.youthmind.controller;

import ai.youthmind.service.ModelTrainingService;
import ai.youthmind.service.TrainingDataProcessor;
import ai.youthmind.service.ModelValidationService;
import ai.youthmind.service.ModelVersioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Model Training & Fine-tuning Services
 * Exposes endpoints for model training, validation, versioning, and deployment
 */
@RestController
@RequestMapping("/api/model-training")
public class ModelTrainingController {
    
    private static final Logger logger = LoggerFactory.getLogger(ModelTrainingController.class);
    
    @Autowired
    private ModelTrainingService modelTrainingService;
    
    @Autowired
    private TrainingDataProcessor trainingDataProcessor;
    
    @Autowired
    private ModelValidationService modelValidationService;
    
    @Autowired
    private ModelVersioningService modelVersioningService;
    
    /**
     * Train a psychological analysis model
     * Returns a jobId/modelId that can be used to track training progress and access the trained model
     * 
     * @param trainingConfig Training configuration
     * @param userId User ID for personalization
     * @return Training result with jobId and modelId (both are the same value)
     */
    @PostMapping("/train/{userId}")
    public ResponseEntity<Map<String, Object>> trainModel(
            @RequestBody Map<String, Object> trainingConfig,
            @PathVariable UUID userId) {
        logger.info("Received request to train model for user: {}", userId);
        try {
            Map<String, Object> result = modelTrainingService.trainModel(trainingConfig, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error training model for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Validate model performance
     * 
     * @param modelId Model ID to validate
     * @param validationData Validation dataset
     * @return Validation result
     */
    @PostMapping("/validate/{modelId}")
    public ResponseEntity<Map<String, Object>> validateModel(
            @PathVariable String modelId,
            @RequestBody Map<String, Object> validationData) {
        logger.info("Received request to validate model: {}", modelId);
        try {
            Map<String, Object> result = modelValidationService.validateAccuracy(modelId, validationData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error validating model {}: {}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Test model performance
     * 
     * @param modelId Model ID to test
     * @param testData Test dataset
     * @return Test result
     */
    @PostMapping("/test/{modelId}")
    public ResponseEntity<Map<String, Object>> testModel(
            @PathVariable String modelId,
            @RequestBody Map<String, Object> testData) {
        logger.info("Received request to test model: {}", modelId);
        try {
            Map<String, Object> result = modelValidationService.testModel(modelId, testData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error testing model {}: {}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Benchmark model performance
     * 
     * @param modelId Model ID to benchmark
     * @param benchmarkData Benchmark dataset
     * @return Benchmark result
     */
    @PostMapping("/benchmark/{modelId}")
    public ResponseEntity<Map<String, Object>> benchmarkModel(
            @PathVariable String modelId,
            @RequestBody Map<String, Object> benchmarkData) {
        logger.info("Received request to benchmark model: {}", modelId);
        try {
            Map<String, Object> result = modelValidationService.benchmarkModel(modelId, benchmarkData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error benchmarking model {}: {}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Compare models
     * 
     * @param modelId Primary model ID
     * @param comparisonData Comparison dataset
     * @return Comparison result
     */
    @PostMapping("/compare/{modelId}")
    public ResponseEntity<Map<String, Object>> compareModels(
            @PathVariable String modelId,
            @RequestBody Map<String, Object> comparisonData) {
        logger.info("Received request to compare model: {}", modelId);
        try {
            Map<String, Object> result = modelValidationService.compareModels(modelId, comparisonData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error comparing model {}: {}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Optimize model performance
     * 
     * @param modelId Model ID to optimize
     * @param optimizationConfig Optimization configuration
     * @return Optimization result
     */
    @PostMapping("/optimize/{modelId}")
    public ResponseEntity<Map<String, Object>> optimizeModel(
            @PathVariable String modelId,
            @RequestBody Map<String, Object> optimizationConfig) {
        logger.info("Received request to optimize model: {}", modelId);
        try {
            Map<String, Object> result = modelTrainingService.optimizeModel(modelId, optimizationConfig);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error optimizing model {}: {}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Deploy model to production
     * 
     * @param modelId Model ID to deploy
     * @param deploymentConfig Deployment configuration
     * @return Deployment result
     */
    @PostMapping("/deploy/{modelId}")
    public ResponseEntity<Map<String, Object>> deployModel(
            @PathVariable String modelId,
            @RequestBody Map<String, Object> deploymentConfig) {
        logger.info("Received request to deploy model: {}", modelId);
        try {
            Map<String, Object> result = modelTrainingService.deployModel(modelId, deploymentConfig);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error deploying model {}: {}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Version a model
     * 
     * @param modelId Model ID to version
     * @param deploymentConfig Deployment configuration
     * @return Versioning result
     */
    @PostMapping("/version/{modelId}")
    public ResponseEntity<Map<String, Object>> versionModel(
            @PathVariable String modelId,
            @RequestBody Map<String, Object> deploymentConfig) {
        logger.info("Received request to version model: {}", modelId);
        try {
            Map<String, Object> result = modelVersioningService.versionModel(modelId, deploymentConfig);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error versioning model {}: {}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get model versions
     * 
     * @param modelId Model ID
     * @return List of model versions
     */
    @GetMapping("/versions/{modelId}")
    public ResponseEntity<Map<String, Object>> getModelVersions(@PathVariable String modelId) {
        logger.info("Received request to get versions for model: {}", modelId);
        try {
            Map<String, Object> result = modelVersioningService.getModelVersions(modelId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting versions for model {}: {}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Set active version
     * 
     * @param modelId Model ID
     * @param version Version to set as active
     * @return Result of setting active version
     */
    @PostMapping("/versions/{modelId}/set-active/{version}")
    public ResponseEntity<Map<String, Object>> setActiveVersion(
            @PathVariable String modelId,
            @PathVariable String version) {
        logger.info("Received request to set active version {} for model: {}", version, modelId);
        try {
            Map<String, Object> result = modelVersioningService.setActiveVersion(modelId, version);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error setting active version {} for model {}: {}", version, modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Rollback to previous version
     * 
     * @param modelId Model ID
     * @param rollbackConfig Rollback configuration
     * @return Rollback result
     */
    @PostMapping("/rollback/{modelId}")
    public ResponseEntity<Map<String, Object>> rollbackModel(
            @PathVariable String modelId,
            @RequestBody Map<String, Object> rollbackConfig) {
        logger.info("Received request to rollback model: {}", modelId);
        try {
            Map<String, Object> result = modelVersioningService.rollbackModel(modelId, rollbackConfig);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error rolling back model {}: {}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Compare model versions
     * 
     * @param modelId Model ID
     * @param version1 First version to compare
     * @param version2 Second version to compare
     * @return Comparison result
     */
    @GetMapping("/compare-versions/{modelId}/{version1}/{version2}")
    public ResponseEntity<Map<String, Object>> compareVersions(
            @PathVariable String modelId,
            @PathVariable String version1,
            @PathVariable String version2) {
        logger.info("Received request to compare versions {} and {} for model: {}", version1, version2, modelId);
        try {
            Map<String, Object> result = modelVersioningService.compareVersions(modelId, version1, version2);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error comparing versions {} and {} for model {}: {}", version1, version2, modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get deployment history for a model
     * 
     * @param modelId Model ID
     * @return Deployment history
     */
    @GetMapping("/deployment-history/{modelId}")
    public ResponseEntity<Map<String, Object>> getDeploymentHistory(@PathVariable String modelId) {
        logger.info("Received request to get deployment history for model: {}", modelId);
        try {
            Map<String, Object> result = modelVersioningService.getDeploymentHistory(modelId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting deployment history for model {}: {}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get training status for a user
     * 
     * @param userId User ID
     * @return Training status
     */
    @GetMapping("/training-status/{userId}")
    public ResponseEntity<Map<String, Object>> getTrainingStatus(@PathVariable UUID userId) {
        logger.info("Received request to get training status for user: {}", userId);
        try {
            Map<String, Object> result = modelTrainingService.getTrainingStatus(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting training status for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get model performance metrics
     * 
     * @param modelId Model ID (use the modelId/jobId returned from the /train endpoint)
     * @return Model performance metrics
     */
    @GetMapping("/performance/{modelId}")
    public ResponseEntity<Map<String, Object>> getModelPerformance(@PathVariable String modelId) {
        logger.info("Received request to get performance metrics for model: {}", modelId);
        try {
            Map<String, Object> result = modelTrainingService.getModelPerformance(modelId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting performance metrics for model {}: {}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get validation history for a model
     * 
     * @param modelId Model ID
     * @return Validation history
     */
    @GetMapping("/validation-history/{modelId}")
    public ResponseEntity<Map<String, Object>> getValidationHistory(@PathVariable String modelId) {
        logger.info("Received request to get validation history for model: {}", modelId);
        try {
            Map<String, Object> result = modelValidationService.getValidationHistory(modelId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting validation history for model {}: {}", modelId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Prepare training data
     * 
     * @param trainingConfig Training configuration
     * @param userId User ID for personalization
     * @return Prepared training data
     */
    @PostMapping("/prepare-data/{userId}")
    public ResponseEntity<Map<String, Object>> prepareData(
            @RequestBody Map<String, Object> trainingConfig,
            @PathVariable UUID userId) {
        logger.info("Received request to prepare training data for user: {}", userId);
        try {
            Map<String, Object> result = trainingDataProcessor.prepareData(trainingConfig, userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error preparing training data for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get prepared training data for a user
     * 
     * @param userId User ID
     * @return Prepared training data
     */
    @GetMapping("/prepared-data/{userId}")
    public ResponseEntity<Map<String, Object>> getPreparedData(@PathVariable UUID userId) {
        logger.info("Received request to get prepared training data for user: {}", userId);
        try {
            Map<String, Object> result = trainingDataProcessor.getPreparedData(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting prepared training data for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

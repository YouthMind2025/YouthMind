package ai.youthmind.service;

import ai.youthmind.YouthmindBackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for Model Training & Fine-tuning Services
 */
@SpringBootTest(classes = YouthmindBackendApplication.class)
@ActiveProfiles("test")
public class ModelTrainingIntegrationTest {

    @Autowired
    private ModelTrainingService modelTrainingService;

    @Autowired
    private TrainingDataProcessor trainingDataProcessor;

    @Autowired
    private ModelValidationService modelValidationService;

    @Autowired
    private ModelVersioningService modelVersioningService;

    @Test
    void testServicesCanBeInstantiated() {
        assertNotNull(modelTrainingService);
        assertNotNull(trainingDataProcessor);
        assertNotNull(modelValidationService);
        assertNotNull(modelVersioningService);
    }

    @Test
    void testModelTrainingWorkflow() {
        UUID userId = UUID.randomUUID();
        
        // Create training configuration
        Map<String, Object> trainingConfig = Map.of(
            "modelType", "psychological_analysis",
            "trainingData", createMockTrainingData(),
            "batchSize", 32,
            "epochs", 10,
            "learningRate", 0.001
        );
        
        // Test training data preparation
        Map<String, Object> preparedData = trainingDataProcessor.prepareData(trainingConfig, userId);
        assertNotNull(preparedData);
        assertTrue(preparedData.containsKey("splitData"));
        assertTrue(preparedData.containsKey("statistics"));
        
        // Test model training
        Map<String, Object> trainingResult = modelTrainingService.trainModel(trainingConfig, userId);
        assertNotNull(trainingResult);
        assertTrue(trainingResult.containsKey("status"));
        assertTrue(trainingResult.containsKey("jobId"));
        
        // Test training status
        Map<String, Object> trainingStatus = modelTrainingService.getTrainingStatus(userId);
        assertNotNull(trainingStatus);
        assertTrue(trainingStatus.containsKey("status"));
    }

    @Test
    void testModelValidationWorkflow() {
        String modelId = "test_model_" + System.currentTimeMillis();
        
        // Create validation data
        Map<String, Object> validationData = Map.of(
            "testData", createMockTestData()
        );
        
        // Test accuracy validation
        Map<String, Object> accuracyResult = modelValidationService.validateAccuracy(modelId, validationData);
        assertNotNull(accuracyResult);
        assertTrue(accuracyResult.containsKey("accuracyResult"));
        assertTrue(accuracyResult.containsKey("crossValidationResult"));
        
        // Test model testing
        Map<String, Object> testResult = modelValidationService.testModel(modelId, validationData);
        assertNotNull(testResult);
        assertTrue(testResult.containsKey("performanceTest"));
        assertTrue(testResult.containsKey("consistencyTest"));
        
        // Test benchmarking
        Map<String, Object> benchmarkResult = modelValidationService.benchmarkModel(modelId, validationData);
        assertNotNull(benchmarkResult);
        assertTrue(benchmarkResult.containsKey("speedBenchmark"));
        assertTrue(benchmarkResult.containsKey("memoryBenchmark"));
        
        // Test model comparison
        Map<String, Object> comparisonResult = modelValidationService.compareModels(modelId, validationData);
        assertNotNull(comparisonResult);
        assertTrue(comparisonResult.containsKey("baselineComparison"));
        assertTrue(comparisonResult.containsKey("versionComparison"));
    }

    @Test
    void testModelVersioningWorkflow() {
        String modelId = "test_model_" + System.currentTimeMillis();
        
        // Create deployment configuration
        Map<String, Object> deploymentConfig = Map.of(
            "versionScheme", "semantic",
            "description", "Test model version",
            "setAsActive", true
        );
        
        // Test model versioning
        Map<String, Object> versionResult = modelVersioningService.versionModel(modelId, deploymentConfig);
        assertNotNull(versionResult);
        assertTrue(versionResult.containsKey("newVersion"));
        assertTrue(versionResult.containsKey("versionMetadata"));
        
        String newVersion = (String) versionResult.get("newVersion");
        
        // Test getting model versions
        Map<String, Object> versions = modelVersioningService.getModelVersions(modelId);
        assertNotNull(versions);
        assertTrue(versions.containsKey("versions"));
        assertTrue(versions.containsKey("activeVersion"));
        
        // Test setting active version
        Map<String, Object> setActiveResult = modelVersioningService.setActiveVersion(modelId, newVersion);
        assertNotNull(setActiveResult);
        assertTrue(setActiveResult.containsKey("activeVersion"));
        assertEquals(newVersion, setActiveResult.get("activeVersion"));
        
        // Test version comparison
        Map<String, Object> comparisonResult = modelVersioningService.compareVersions(modelId, newVersion, newVersion);
        assertNotNull(comparisonResult);
        assertTrue(comparisonResult.containsKey("metadataComparison"));
        
        // Test deployment history
        Map<String, Object> deploymentHistory = modelVersioningService.getDeploymentHistory(modelId);
        assertNotNull(deploymentHistory);
        assertTrue(deploymentHistory.containsKey("deploymentHistory"));
    }

    @Test
    void testModelOptimizationAndDeployment() {
        String modelId = "test_model_" + System.currentTimeMillis();
        
        // Create optimization configuration
        Map<String, Object> optimizationConfig = Map.of(
            "hyperparameterTuning", true,
            "featureSelection", true,
            "ensembleMethods", true,
            "regularization", true
        );
        
        // Test model optimization
        Map<String, Object> optimizationResult = modelTrainingService.optimizeModel(modelId, optimizationConfig);
        assertNotNull(optimizationResult);
        assertTrue(optimizationResult.containsKey("optimizationResult"));
        assertTrue(optimizationResult.containsKey("validationResult"));
        
        // Create deployment configuration
        Map<String, Object> deploymentConfig = Map.of(
            "deploymentType", "production",
            "scalingConfig", Map.of("minInstances", 2, "maxInstances", 10),
            "monitoringEnabled", true
        );
        
        // Test model deployment
        Map<String, Object> deploymentResult = modelTrainingService.deployModel(modelId, deploymentConfig);
        assertNotNull(deploymentResult);
        assertTrue(deploymentResult.containsKey("deploymentResult"));
        assertTrue(deploymentResult.containsKey("versionResult"));
        
        // Test model performance
        Map<String, Object> performanceResult = modelTrainingService.getModelPerformance(modelId);
        assertNotNull(performanceResult);
        assertTrue(performanceResult.containsKey("modelId"));
    }

    @Test
    void testTrainingDataProcessing() {
        UUID userId = UUID.randomUUID();
        
        // Create training configuration with various data processing options
        Map<String, Object> trainingConfig = Map.of(
            "modelType", "psychological_analysis",
            "trainingData", createMockTrainingData(),
            "augmentationFactor", 2,
            "textAugmentation", true,
            "syntheticGeneration", true,
            "noiseInjection", true,
            "trainSplit", 0.7,
            "validationSplit", 0.15,
            "testSplit", 0.15
        );
        
        // Test data preparation with augmentation
        Map<String, Object> preparedData = trainingDataProcessor.prepareData(trainingConfig, userId);
        assertNotNull(preparedData);
        
        // Verify data splitting
        Map<String, Object> splitData = (Map<String, Object>) preparedData.get("splitData");
        assertNotNull(splitData);
        assertTrue(splitData.containsKey("trainData"));
        assertTrue(splitData.containsKey("validationData"));
        assertTrue(splitData.containsKey("testData"));
        
        // Verify statistics
        Map<String, Object> statistics = (Map<String, Object>) preparedData.get("statistics");
        assertNotNull(statistics);
        assertTrue(statistics.containsKey("contentStatistics"));
        assertTrue(statistics.containsKey("vectorStatistics"));
        assertTrue(statistics.containsKey("classStatistics"));
        
        // Test getting prepared data
        Map<String, Object> retrievedData = trainingDataProcessor.getPreparedData(userId);
        assertNotNull(retrievedData);
        assertTrue(retrievedData.containsKey("userId"));
    }

    @Test
    void testValidationHistory() {
        String modelId = "test_model_" + System.currentTimeMillis();
        
        // Test getting validation history (should be empty initially)
        Map<String, Object> validationHistory = modelValidationService.getValidationHistory(modelId);
        assertNotNull(validationHistory);
        assertTrue(validationHistory.containsKey("modelId"));
        assertTrue(validationHistory.containsKey("history"));
        
        // Perform some validations to create history
        Map<String, Object> validationData = Map.of("testData", createMockTestData());
        
        modelValidationService.validateAccuracy(modelId, validationData);
        modelValidationService.testModel(modelId, validationData);
        
        // Get validation history again
        Map<String, Object> updatedHistory = modelValidationService.getValidationHistory(modelId);
        assertNotNull(updatedHistory);
        assertTrue(updatedHistory.containsKey("history"));
    }

    // Helper methods for creating mock data
    private java.util.List<Map<String, Object>> createMockTrainingData() {
        java.util.List<Map<String, Object>> trainingData = new java.util.ArrayList<>();
        
        for (int i = 0; i < 50; i++) {
            Map<String, Object> sample = new java.util.HashMap<>();
            sample.put("content", "Sample psychological content " + i);
            sample.put("psychologicalVector", createMockPsychologicalVector());
            sample.put("label", "class_" + (i % 3));
            trainingData.add(sample);
        }
        
        return trainingData;
    }

    private java.util.List<Map<String, Object>> createMockTestData() {
        java.util.List<Map<String, Object>> testData = new java.util.ArrayList<>();
        
        for (int i = 0; i < 20; i++) {
            Map<String, Object> sample = new java.util.HashMap<>();
            sample.put("content", "Test psychological content " + i);
            sample.put("psychologicalVector", createMockPsychologicalVector());
            sample.put("label", "class_" + (i % 3));
            testData.add(sample);
        }
        
        return testData;
    }

    private java.util.List<Double> createMockPsychologicalVector() {
        java.util.List<Double> vector = new java.util.ArrayList<>();
        for (int i = 0; i < 43; i++) {
            vector.add(Math.random());
        }
        return vector;
    }
}

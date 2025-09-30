package ai.youthmind.test;

import ai.youthmind.service.*;
import ai.youthmind.controller.PsychologicalAIChatController;
import ai.youthmind.dto.PsychologicalAnalysisRequest;
import ai.youthmind.dto.PsychologicalAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration test for Youseef's psychological analysis services
 * Tests the complete workflow from psychological state analysis to response generation
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PsychologicalAnalysisIntegrationTest {

    @Autowired
    private PsychologicalAIChatController psychologicalController;

    @Autowired
    private AIChatService aiChatService;

    @Autowired
    private PsychologicalStateCacheService cacheService;

    @Autowired
    private PsychologicalAnalysisBatchProcessor batchProcessor;

    @Autowired
    private PsychologicalVectorGenerator psychologicalVectorGenerator;

    @Autowired
    private BuddhistFactorAnalyzer buddhistFactorAnalyzer;

    @Autowired
    private IntensityPredictor intensityPredictor;

    @Autowired
    private StateVectorProcessor stateVectorProcessor;

    @Autowired
    private AIErrorHandler aiErrorHandler;

    private UUID testUserId;
    private String testMessage;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testMessage = "I'm feeling really anxious about my upcoming exams and I can't seem to focus on studying.";
    }

    @Test
    @DisplayName("Complete psychological analysis workflow integration test")
    void testCompletePsychologicalAnalysisWorkflow() {
        // Step 1: Generate psychological vector
        Map<String, Object> vectorResult = psychologicalVectorGenerator.generateVector(testMessage, testUserId);
        assertNotNull(vectorResult);
        assertTrue(vectorResult.containsKey("vector"));
        @SuppressWarnings("unchecked")
        List<Double> psychologicalVector = (List<Double>) vectorResult.get("vector");
        assertEquals(43, psychologicalVector.size(), "Psychological vector should have 43 dimensions");

        // Step 2: Analyze Buddhist factors
        Map<String, Object> factorsResult = buddhistFactorAnalyzer.analyzeFactors(testMessage, testUserId);
        assertNotNull(factorsResult);
        assertTrue(factorsResult.containsKey("factors"));
        @SuppressWarnings("unchecked")
        Map<String, Double> buddhistFactors = (Map<String, Double>) factorsResult.get("factors");
        assertTrue(buddhistFactors.containsKey("mindfulness"));
        assertTrue(buddhistFactors.containsKey("compassion"));
        assertTrue(buddhistFactors.containsKey("equanimity"));

        // Step 3: Calculate intensity factor
        double intensityFactor = intensityPredictor.calculateImpactScore(0.5, psychologicalVector, factorsResult);
        assertTrue(intensityFactor >= 0.0 && intensityFactor <= 1.0, "Intensity factor should be between 0 and 1");

        // Step 4: Process state vector
        Map<String, Object> processedResult = stateVectorProcessor.processVector(psychologicalVector, testUserId);
        assertNotNull(processedResult);
        assertTrue(processedResult.containsKey("processedVector"));
        @SuppressWarnings("unchecked")
        List<Double> processedStateVector = (List<Double>) processedResult.get("processedVector");
        assertEquals(43, processedStateVector.size(), "Processed state vector should have 43 dimensions");

        // Step 5: Cache psychological state
        cacheService.cachePsychologicalState(testUserId, psychologicalVector, buddhistFactors);
        var cachedState = cacheService.getPsychologicalState(testUserId);
        assertNotNull(cachedState);
        assertEquals(psychologicalVector, cachedState.get("vector"));

        // Step 6: Generate psychological state-aware response
        var response = aiChatService.generatePsychologicalStateAwareResponse(testUserId.toString(), testMessage);
        assertNotNull(response);
        assertTrue(response.containsKey("reply"));
        assertTrue(response.containsKey("tone"));
        assertTrue(response.containsKey("psychologicalState"));

        // Step 7: Verify response tone is appropriate for anxiety
        String tone = (String) response.get("tone");
        assertTrue(tone.equals("calming") || tone.equals("empathetic"), 
                  "Response tone should be calming or empathetic for anxious message");

        System.out.println("✅ Complete psychological analysis workflow test passed");
    }

    @Test
    @DisplayName("Batch processing integration test")
    void testBatchProcessingIntegration() {
        // Enqueue multiple analysis tasks
        for (int i = 0; i < 5; i++) {
            String message = "Test message " + i + " about feeling " + (i % 2 == 0 ? "anxious" : "peaceful");
            batchProcessor.enqueueAnalysis(testUserId, message, "session_" + i);
        }

        // Verify tasks are enqueued
        assertTrue(batchProcessor.getCurrentQueueSize() > 0, "Tasks should be enqueued");

        // Wait for batch processing
        try {
            Thread.sleep(3000); // Wait for batch processing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify psychological state is cached
        var cachedState = cacheService.getPsychologicalState(testUserId);
        assertNotNull(cachedState, "Psychological state should be cached after batch processing");

        // Get batch statistics
        var batchStats = batchProcessor.getBatchStatistics();
        assertNotNull(batchStats);
        assertTrue(batchStats.containsKey("processedCount"));

        System.out.println("✅ Batch processing integration test passed");
    }

    @Test
    @DisplayName("Cache management integration test")
    void testCacheManagementIntegration() {
        // Generate and cache psychological state
        Map<String, Object> vectorResult = psychologicalVectorGenerator.generateVector(testMessage, testUserId);
        @SuppressWarnings("unchecked")
        List<Double> psychologicalVector = (List<Double>) vectorResult.get("vector");
        Map<String, Object> factorsResult = buddhistFactorAnalyzer.analyzeFactors(testMessage, testUserId);
        @SuppressWarnings("unchecked")
        Map<String, Double> buddhistFactors = (Map<String, Double>) factorsResult.get("factors");
        
        cacheService.cachePsychologicalState(testUserId, psychologicalVector, buddhistFactors);

        // Verify cache hit
        assertTrue(cacheService.isPsychologicalStateCached(testUserId), "State should be cached");

        // Warm up cache
        cacheService.warmUpCache(testUserId);
        var cachedFactors = cacheService.getBuddhistFactors(testUserId);
        assertNotNull(cachedFactors, "Buddhist factors should be cached after warmup");

        // Get cache statistics
        var cacheStats = cacheService.getCacheStatistics();
        assertNotNull(cacheStats);
        assertTrue(cacheStats.containsKey("psychologicalStateTTL"));

        // Invalidate cache
        cacheService.invalidatePsychologicalState(testUserId);
        assertFalse(cacheService.isPsychologicalStateCached(testUserId), "State should not be cached after invalidation");

        System.out.println("✅ Cache management integration test passed");
    }

    @Test
    @DisplayName("Error handling integration test")
    void testErrorHandlingIntegration() {
        // Test error handling with invalid input
        Map<String, Object> errorContext = new HashMap<>();
        errorContext.put("userId", testUserId.toString());
        errorContext.put("message", "");
        errorContext.put("operation", "psychological_analysis");

        var errorResult = aiErrorHandler.handleError(
            new RuntimeException("Test error"), 
            errorContext, 
            testUserId
        );

        assertNotNull(errorResult);
        assertTrue(errorResult.containsKey("errorId"));
        assertTrue(errorResult.containsKey("errorType"));
        assertTrue(errorResult.containsKey("severity"));
        assertTrue(errorResult.containsKey("action"));

        System.out.println("✅ Error handling integration test passed");
    }

    @Test
    @DisplayName("Async psychological analysis integration test")
    void testAsyncPsychologicalAnalysisIntegration() {
        // Test async analysis
        CompletableFuture<Void> analysisFuture = aiChatService.analyzeAndUpdatePsychologicalState(
            testUserId.toString(), 
            testMessage
        );

        // Wait for completion
        assertDoesNotThrow(() -> {
            analysisFuture.get(5, TimeUnit.SECONDS);
        }, "Async analysis should complete within 5 seconds");

        // Verify state is updated
        var cachedState = cacheService.getPsychologicalState(testUserId);
        assertNotNull(cachedState, "State should be updated after async analysis");

        System.out.println("✅ Async psychological analysis integration test passed");
    }

    @Test
    @DisplayName("Controller integration test")
    void testControllerIntegration() {
        // Test psychological state endpoint
        var stateResponse = psychologicalController.getPsychologicalState(testUserId.toString());
        assertNotNull(stateResponse);
        assertEquals(200, stateResponse.getStatusCodeValue());

        // Test psychological chat endpoint
        var chatResponse = psychologicalController.generatePsychologicalResponse(
            testUserId.toString(), 
            Map.of("message", testMessage)
        );
        assertNotNull(chatResponse);
        assertEquals(200, chatResponse.getStatusCodeValue());

        // Test analysis endpoint
        var analysisResponse = psychologicalController.analyzeMessage(
            testUserId.toString(),
            Map.of("message", testMessage)
        );
        assertNotNull(analysisResponse);
        assertEquals(200, analysisResponse.getStatusCodeValue());

        // Test health endpoint
        var healthResponse = psychologicalController.healthCheck();
        assertNotNull(healthResponse);
        assertEquals(200, healthResponse.getStatusCodeValue());

        System.out.println("✅ Controller integration test passed");
    }

    @Test
    @DisplayName("Comprehensive psychological analysis request/response test")
    void testComprehensivePsychologicalAnalysisRequestResponse() {
        // Create comprehensive analysis request
        var request = new PsychologicalAnalysisRequest();
        request.setContent(testMessage);
        request.setUserId(testUserId);
        request.setSessionId("test_session");
        // Configure analysis settings
        PsychologicalAnalysisRequest.AnalysisConfig config = new PsychologicalAnalysisRequest.AnalysisConfig();
        config.setGeneratePsychologicalVector(true);
        config.setAnalyzeBuddhistFactors(true);
        config.setCalculateIntensityFactor(true);
        config.setProcessStateVector(true);
        config.setIncludeDetailedAnalysis(true);
        config.setIncludeRecommendations(true);
        config.setMaxAnalysisDepth(5);
        config.setAnalysisMode("comprehensive");
        
        request.setAnalysisConfig(config);

        // This would test the AIServiceClient.performPsychologicalAnalysis method
        // For now, we'll test the individual components
        
        // Test psychological vector generation
        Map<String, Object> vectorResult = psychologicalVectorGenerator.generateVector(testMessage, testUserId);
        @SuppressWarnings("unchecked")
        List<Double> psychologicalVector = (List<Double>) vectorResult.get("vector");
        assertNotNull(psychologicalVector);
        assertEquals(43, psychologicalVector.size());

        // Test Buddhist factor analysis
        Map<String, Object> factorsResult = buddhistFactorAnalyzer.analyzeFactors(testMessage, testUserId);
        @SuppressWarnings("unchecked")
        Map<String, Double> buddhistFactors = (Map<String, Double>) factorsResult.get("factors");
        assertNotNull(buddhistFactors);

        // Test intensity prediction
        double intensityFactor = intensityPredictor.calculateImpactScore(0.5, psychologicalVector, factorsResult);
        assertTrue(intensityFactor >= 0.0 && intensityFactor <= 1.0);

        // Test state vector processing
        Map<String, Object> processedResult = stateVectorProcessor.processVector(psychologicalVector, testUserId);
        assertNotNull(processedResult);
        assertTrue(processedResult.containsKey("processedVector"));

        System.out.println("✅ Comprehensive psychological analysis request/response test passed");
    }

    @Test
    @DisplayName("Performance and scalability test")
    void testPerformanceAndScalability() {
        long startTime = System.currentTimeMillis();

        // Test multiple concurrent analyses
        var futures = new CompletableFuture[10];
        for (int i = 0; i < 10; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                String message = "Performance test message " + index;
                Map<String, Object> vectorResult = psychologicalVectorGenerator.generateVector(message, testUserId);
                @SuppressWarnings("unchecked")
                List<Double> vector = (List<Double>) vectorResult.get("vector");
                Map<String, Object> factorsResult = buddhistFactorAnalyzer.analyzeFactors(message, testUserId);
                @SuppressWarnings("unchecked")
                Map<String, Double> factors = (Map<String, Double>) factorsResult.get("factors");
                double intensity = intensityPredictor.calculateImpactScore(0.5, vector, factorsResult);
                Map<String, Object> processedResult = stateVectorProcessor.processVector(vector, testUserId);
                assertNotNull(processedResult);
                assertTrue(processedResult.containsKey("processedVector"));
            });
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures).join();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("✅ Performance test completed in " + duration + "ms");
        assertTrue(duration < 10000, "Performance test should complete within 10 seconds");
    }

    @Test
    @DisplayName("Data consistency and validation test")
    void testDataConsistencyAndValidation() {
        // Test with various message types
        String[] testMessages = {
            "I'm feeling anxious and overwhelmed",
            "I feel peaceful and mindful today",
            "I'm grateful for the support I've received",
            "I'm struggling with depression and loneliness",
            "I'm excited about my future plans"
        };

        for (String message : testMessages) {
            // Generate psychological vector
            Map<String, Object> vectorResult = psychologicalVectorGenerator.generateVector(message, testUserId);
            @SuppressWarnings("unchecked")
            List<Double> vector = (List<Double>) vectorResult.get("vector");
            assertNotNull(vector);
            assertEquals(43, vector.size());

            // Verify all values are valid
            for (double value : vector) {
                assertTrue(value >= 0.0 && value <= 1.0, 
                          "All psychological vector values should be between 0 and 1");
            }

            // Analyze Buddhist factors
            Map<String, Object> factorsResult = buddhistFactorAnalyzer.analyzeFactors(message, testUserId);
            assertNotNull(factorsResult);
            assertTrue(factorsResult.containsKey("factors"));
            @SuppressWarnings("unchecked")
            Map<String, Double> factors = (Map<String, Double>) factorsResult.get("factors");

            // Verify all factor values are valid
            for (double value : factors.values()) {
                assertTrue(value >= 0.0 && value <= 1.0, 
                          "All Buddhist factor values should be between 0 and 1");
            }

            // Calculate intensity factor
            double intensity = intensityPredictor.calculateImpactScore(0.5, vector, factorsResult);
            assertTrue(intensity >= 0.0 && intensity <= 1.0, 
                      "Intensity factor should be between 0 and 1");
        }

        System.out.println("✅ Data consistency and validation test passed");
    }
}

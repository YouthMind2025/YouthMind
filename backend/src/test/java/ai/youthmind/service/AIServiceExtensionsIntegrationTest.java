package ai.youthmind.service;

import ai.youthmind.YouthmindBackendApplication;
import ai.youthmind.dto.PsychologicalAnalysisRequest;
import ai.youthmind.dto.PsychologicalAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for AI Service Extensions
 */
@SpringBootTest(classes = YouthmindBackendApplication.class)
@ActiveProfiles("test")
public class AIServiceExtensionsIntegrationTest {

    @Autowired
    private AIServiceClient aiServiceClient;

    @Autowired
    private AIErrorHandler aiErrorHandler;

    @Autowired
    private PsychologicalVectorGenerator psychologicalVectorGenerator;

    @Autowired
    private BuddhistFactorAnalyzer buddhistFactorAnalyzer;

    @Autowired
    private IntensityPredictor intensityPredictor;

    @Autowired
    private StateVectorProcessor stateVectorProcessor;

    @Test
    void testServicesCanBeInstantiated() {
        assertNotNull(aiServiceClient);
        assertNotNull(aiErrorHandler);
        assertNotNull(psychologicalVectorGenerator);
        assertNotNull(buddhistFactorAnalyzer);
        assertNotNull(intensityPredictor);
        assertNotNull(stateVectorProcessor);
    }

    @Test
    void testPsychologicalAnalysisWorkflow() {
        UUID userId = UUID.randomUUID();
        
        // Create psychological analysis request
        PsychologicalAnalysisRequest request = createTestAnalysisRequest(userId);
        
        // Test psychological analysis
        PsychologicalAnalysisResponse response = aiServiceClient.performPsychologicalAnalysis(request);
        
        assertNotNull(response);
        assertEquals(request.getRequestId(), response.getRequestId());
        assertEquals(request.getUserId(), response.getUserId());
        assertNotNull(response.getMetadata());
        assertNotNull(response.getProcessingInfo());
        
        // Verify analysis components
        if (request.getAnalysisConfig().isGeneratePsychologicalVector()) {
            assertNotNull(response.getPsychologicalVector());
            assertTrue(response.getPsychologicalVector().getVector().size() == 43);
        }
        
        if (request.getAnalysisConfig().isAnalyzeBuddhistFactors()) {
            assertNotNull(response.getBuddhistFactors());
            assertNotNull(response.getBuddhistFactors().getFactorScores());
        }
        
        if (request.getAnalysisConfig().isCalculateIntensityFactor()) {
            assertNotNull(response.getIntensityFactor());
            assertNotNull(response.getIntensityFactor().getIntensityClassification());
        }
        
        if (request.getAnalysisConfig().isProcessStateVector()) {
            assertNotNull(response.getStateVector());
        }
        
        if (request.getAnalysisConfig().isIncludeDetailedAnalysis()) {
            assertNotNull(response.getComprehensiveAnalysis());
            assertNotNull(response.getComprehensiveAnalysis().getEmotionalAnalysis());
            assertNotNull(response.getComprehensiveAnalysis().getCognitiveAnalysis());
            assertNotNull(response.getComprehensiveAnalysis().getSocialAnalysis());
            assertNotNull(response.getComprehensiveAnalysis().getStressAnalysis());
            assertNotNull(response.getComprehensiveAnalysis().getPersonalityAnalysis());
            assertNotNull(response.getComprehensiveAnalysis().getMentalHealthAnalysis());
            assertNotNull(response.getComprehensiveAnalysis().getCopingAnalysis());
            assertNotNull(response.getComprehensiveAnalysis().getResilienceAnalysis());
        }
        
        // Verify recommendations and insights
        assertNotNull(response.getRecommendations());
        assertNotNull(response.getInsights());
        assertNotNull(response.getRiskAssessment());
    }

    @Test
    void testErrorHandlingWorkflow() {
        UUID userId = UUID.randomUUID();
        
        // Test error statistics
        Map<String, Object> stats = aiErrorHandler.getErrorStatistics(userId);
        assertNotNull(stats);
        assertTrue(stats.containsKey("errorCounts"));
        assertTrue(stats.containsKey("severityCounts"));
        assertTrue(stats.containsKey("totalErrors"));
        
        // Test error history
        Map<String, Object> history = aiErrorHandler.getErrorHistory(userId, 10);
        assertNotNull(history);
        assertTrue(history.containsKey("userId"));
        assertTrue(history.containsKey("errorHistory"));
        assertTrue(history.containsKey("totalErrors"));
        
        // Test error handling with simulated error
        Exception testError = new RuntimeException("Test error for integration testing");
        Map<String, Object> context = Map.of(
            "testType", "integration_test",
            "userId", userId.toString()
        );
        
        Map<String, Object> errorResult = aiErrorHandler.handleError(testError, context, userId);
        assertNotNull(errorResult);
        assertTrue(errorResult.containsKey("errorId"));
        assertTrue(errorResult.containsKey("userId"));
        assertTrue(errorResult.containsKey("errorType"));
        assertTrue(errorResult.containsKey("severity"));
        assertTrue(errorResult.containsKey("shouldRetry"));
        
        // Test error handling test
        Map<String, Object> testResult = aiErrorHandler.testErrorHandling("connection", userId);
        assertNotNull(testResult);
        assertTrue(testResult.containsKey("testType"));
        assertTrue(testResult.containsKey("testStatus"));
    }

    @Test
    void testErrorCooldownManagement() {
        UUID userId = UUID.randomUUID();
        
        // Test clearing cooldown
        Map<String, Object> clearResult = aiErrorHandler.clearErrorCooldown(userId);
        assertNotNull(clearResult);
        assertTrue(clearResult.containsKey("userId"));
        assertTrue(clearResult.containsKey("status"));
        assertEquals("COOLDOWN_CLEARED", clearResult.get("status"));
    }

    @Test
    void testPsychologicalAnalysisComponents() {
        UUID userId = UUID.randomUUID();
        String testContent = "I'm feeling really happy today and excited about my future plans!";
        
        // Test psychological vector generation
        Map<String, Object> vectorResult = psychologicalVectorGenerator.generateVector(testContent, userId);
        assertNotNull(vectorResult);
        assertTrue(vectorResult.containsKey("psychologicalVector"));
        assertTrue(vectorResult.containsKey("confidenceScore"));
        assertTrue(vectorResult.containsKey("isValid"));
        
        @SuppressWarnings("unchecked")
        java.util.List<Double> vector = (java.util.List<Double>) vectorResult.get("psychologicalVector");
        assertEquals(43, vector.size());
        
        // Test Buddhist factors analysis
        Map<String, Object> factorsResult = buddhistFactorAnalyzer.analyzeFactors(testContent, userId);
        assertNotNull(factorsResult);
        assertTrue(factorsResult.containsKey("factorScores"));
        assertTrue(factorsResult.containsKey("detectedKeywords"));
        assertTrue(factorsResult.containsKey("classifiedFactors"));
        
        // Test intensity prediction
        Map<String, Object> intensityResult = intensityPredictor.predictIntensity(testContent, userId, vector, factorsResult);
        assertNotNull(intensityResult);
        assertTrue(intensityResult.containsKey("intensityScore"));
        assertTrue(intensityResult.containsKey("impactScore"));
        assertTrue(intensityResult.containsKey("intensityClassification"));
        
        // Test state vector processing
        Map<String, Object> stateResult = stateVectorProcessor.processVector(vector, userId);
        assertNotNull(stateResult);
        assertTrue(stateResult.containsKey("processedVector"));
        assertTrue(stateResult.containsKey("processingDetails"));
        assertTrue(stateResult.containsKey("isValid"));
    }

    @Test
    void testAnalysisRequestValidation() {
        UUID userId = UUID.randomUUID();
        
        // Test valid request
        PsychologicalAnalysisRequest validRequest = createTestAnalysisRequest(userId);
        assertNotNull(validRequest);
        assertEquals(userId, validRequest.getUserId());
        assertNotNull(validRequest.getContent());
        assertNotNull(validRequest.getAnalysisConfig());
        
        // Test invalid request (null content)
        PsychologicalAnalysisRequest invalidRequest = new PsychologicalAnalysisRequest();
        invalidRequest.setUserId(userId);
        invalidRequest.setContent(null);
        
        try {
            aiServiceClient.performPsychologicalAnalysis(invalidRequest);
            assertTrue(false, "Should have thrown exception for null content");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Content cannot be null or empty"));
        }
        
        // Test invalid request (null user ID)
        PsychologicalAnalysisRequest invalidRequest2 = new PsychologicalAnalysisRequest();
        invalidRequest2.setUserId(null);
        invalidRequest2.setContent("Test content");
        
        try {
            aiServiceClient.performPsychologicalAnalysis(invalidRequest2);
            assertTrue(false, "Should have thrown exception for null user ID");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("User ID cannot be null"));
        }
    }

    @Test
    void testAnalysisConfigurationOptions() {
        UUID userId = UUID.randomUUID();
        
        // Test with minimal configuration
        PsychologicalAnalysisRequest minimalRequest = createMinimalAnalysisRequest(userId);
        PsychologicalAnalysisResponse minimalResponse = aiServiceClient.performPsychologicalAnalysis(minimalRequest);
        
        assertNotNull(minimalResponse);
        assertNotNull(minimalResponse.getMetadata());
        assertNotNull(minimalResponse.getProcessingInfo());
        
        // Test with comprehensive configuration
        PsychologicalAnalysisRequest comprehensiveRequest = createComprehensiveAnalysisRequest(userId);
        PsychologicalAnalysisResponse comprehensiveResponse = aiServiceClient.performPsychologicalAnalysis(comprehensiveRequest);
        
        assertNotNull(comprehensiveResponse);
        assertNotNull(comprehensiveResponse.getPsychologicalVector());
        assertNotNull(comprehensiveResponse.getBuddhistFactors());
        assertNotNull(comprehensiveResponse.getIntensityFactor());
        assertNotNull(comprehensiveResponse.getStateVector());
        assertNotNull(comprehensiveResponse.getComprehensiveAnalysis());
        assertNotNull(comprehensiveResponse.getRecommendations());
        assertNotNull(comprehensiveResponse.getInsights());
        assertNotNull(comprehensiveResponse.getRiskAssessment());
    }

    @Test
    void testErrorRecoveryAndFallback() {
        UUID userId = UUID.randomUUID();
        
        // Test error handling with different error types
        String[] errorTypes = {"connection", "timeout", "server", "client", "memory", "security"};
        
        for (String errorType : errorTypes) {
            Map<String, Object> testResult = aiErrorHandler.testErrorHandling(errorType, userId);
            assertNotNull(testResult);
            assertTrue(testResult.containsKey("testType"));
            assertTrue(testResult.containsKey("testStatus"));
            assertEquals(errorType, testResult.get("testType"));
        }
        
        // Test error statistics after multiple errors
        Map<String, Object> finalStats = aiErrorHandler.getErrorStatistics(userId);
        assertNotNull(finalStats);
        assertTrue(finalStats.containsKey("errorCounts"));
        assertTrue(finalStats.containsKey("severityCounts"));
        assertTrue(finalStats.containsKey("totalErrors"));
    }

    // Helper methods for creating test data
    private PsychologicalAnalysisRequest createTestAnalysisRequest(UUID userId) {
        PsychologicalAnalysisRequest request = new PsychologicalAnalysisRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setUserId(userId);
        request.setSessionId(UUID.randomUUID().toString());
        request.setContent("I'm feeling really happy today and excited about my future plans!");
        request.setContentType("text");
        request.setLanguage("en");
        request.setTimestamp(java.time.LocalDateTime.now());
        request.setSource("api");
        request.setVersion("1.0.0");
        
        // Analysis config
        PsychologicalAnalysisRequest.AnalysisConfig analysisConfig = new PsychologicalAnalysisRequest.AnalysisConfig();
        analysisConfig.setGeneratePsychologicalVector(true);
        analysisConfig.setAnalyzeBuddhistFactors(true);
        analysisConfig.setCalculateIntensityFactor(true);
        analysisConfig.setProcessStateVector(true);
        analysisConfig.setIncludeDetailedAnalysis(true);
        analysisConfig.setIncludeRecommendations(true);
        analysisConfig.setMaxAnalysisDepth(3);
        analysisConfig.setAnalysisMode("standard");
        request.setAnalysisConfig(analysisConfig);
        
        // User profile
        PsychologicalAnalysisRequest.UserProfile userProfile = new PsychologicalAnalysisRequest.UserProfile();
        userProfile.setAgeGroup("young_adult");
        userProfile.setGender("unknown");
        userProfile.setCulturalBackground("general");
        userProfile.setTimezone("UTC");
        userProfile.setPreferredLanguage("en");
        userProfile.setRiskLevel("low");
        request.setUserProfile(userProfile);
        
        // Preferences
        PsychologicalAnalysisRequest.AnalysisPreferences preferences = new PsychologicalAnalysisRequest.AnalysisPreferences();
        preferences.setIncludeEmotionalAnalysis(true);
        preferences.setIncludeCognitiveAnalysis(true);
        preferences.setIncludeSocialAnalysis(true);
        preferences.setIncludeStressIndicators(true);
        preferences.setIncludePersonalityTraits(true);
        preferences.setIncludeMentalHealthIndicators(true);
        preferences.setIncludeCopingMechanisms(true);
        preferences.setIncludeResilienceFactors(true);
        preferences.setOutputFormat("json");
        preferences.setDetailLevel("intermediate");
        request.setPreferences(preferences);
        
        return request;
    }

    private PsychologicalAnalysisRequest createMinimalAnalysisRequest(UUID userId) {
        PsychologicalAnalysisRequest request = new PsychologicalAnalysisRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setUserId(userId);
        request.setContent("Minimal test content");
        request.setTimestamp(java.time.LocalDateTime.now());
        
        // Minimal analysis config
        PsychologicalAnalysisRequest.AnalysisConfig analysisConfig = new PsychologicalAnalysisRequest.AnalysisConfig();
        analysisConfig.setGeneratePsychologicalVector(false);
        analysisConfig.setAnalyzeBuddhistFactors(false);
        analysisConfig.setCalculateIntensityFactor(false);
        analysisConfig.setProcessStateVector(false);
        analysisConfig.setIncludeDetailedAnalysis(false);
        analysisConfig.setIncludeRecommendations(false);
        analysisConfig.setMaxAnalysisDepth(1);
        analysisConfig.setAnalysisMode("quick");
        request.setAnalysisConfig(analysisConfig);
        
        return request;
    }

    private PsychologicalAnalysisRequest createComprehensiveAnalysisRequest(UUID userId) {
        PsychologicalAnalysisRequest request = createTestAnalysisRequest(userId);
        
        // Update content for comprehensive analysis
        request.setContent("I've been going through a challenging time lately. I feel overwhelmed with work and personal relationships, but I'm trying to stay positive and focus on self-care. I've been practicing mindfulness and reaching out to friends for support. I'm hopeful about the future and believe I can overcome these challenges.");
        
        // Comprehensive analysis config
        PsychologicalAnalysisRequest.AnalysisConfig analysisConfig = request.getAnalysisConfig();
        analysisConfig.setIncludeDetailedAnalysis(true);
        analysisConfig.setIncludeRecommendations(true);
        analysisConfig.setMaxAnalysisDepth(5);
        analysisConfig.setAnalysisMode("comprehensive");
        
        return request;
    }
}

package ai.youthmind.service;

import ai.youthmind.entity.PsychologicalStateVector;
import ai.youthmind.entity.StateUpdateHistory;
import ai.youthmind.repository.PsychologicalStateVectorRepository;
import ai.youthmind.repository.StateUpdateHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Psychological State Management services
 * Tests the complete flow of state updates and calculations
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PsychologicalStateIntegrationTest {
    
    @Autowired
    private PsychologicalStateService psychologicalStateService;
    
    @Autowired
    private StateVectorCalculator stateVectorCalculator;
    
    @Autowired
    private IntensityFactorCalculator intensityFactorCalculator;
    
    @Autowired
    private StateUpdateRuleEngine stateUpdateRuleEngine;
    
    @Autowired
    private StateNormalizationService stateNormalizationService;
    
    @Autowired
    private PsychologicalStateVectorRepository stateVectorRepository;
    
    @Autowired
    private StateUpdateHistoryRepository stateUpdateHistoryRepository;
    
    private UUID testUserId;
    private String testPostContent;
    private UUID testPostId;
    
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPostContent = "I'm feeling really excited about this new opportunity! It's amazing how things can change so quickly.";
        testPostId = UUID.randomUUID();
    }
    
    @Test
    void testCompletePostingUpdateFlow() {
        // Test the complete posting update flow
        Map<String, Object> result = psychologicalStateService.updateStateFromPost(
            testUserId, testPostContent, testPostId
        );
        
        // Verify result structure
        assertNotNull(result);
        assertTrue(result.containsKey("userId"));
        assertTrue(result.containsKey("previousState"));
        assertTrue(result.containsKey("newState"));
        assertTrue(result.containsKey("vRaw"));
        assertTrue(result.containsKey("alpha"));
        assertTrue(result.containsKey("thetaFactor"));
        assertTrue(result.containsKey("updateType"));
        
        // Verify user ID
        assertEquals(testUserId, result.get("userId"));
        
        // Verify state vectors have correct dimensions
        @SuppressWarnings("unchecked")
        List<Double> previousState = (List<Double>) result.get("previousState");
        @SuppressWarnings("unchecked")
        List<Double> newState = (List<Double>) result.get("newState");
        @SuppressWarnings("unchecked")
        List<Double> vRaw = (List<Double>) result.get("vRaw");
        
        assertEquals(43, previousState.size());
        assertEquals(43, newState.size());
        assertEquals(43, vRaw.size());
        
        // Verify update type
        assertEquals("POSTING", result.get("updateType"));
        
        // Verify alpha is within valid range
        Double alpha = (Double) result.get("alpha");
        assertTrue(alpha >= 0.01 && alpha <= 0.9);
        
        // Verify theta factor is within valid range
        Double thetaFactor = (Double) result.get("thetaFactor");
        assertTrue(thetaFactor >= 0.0 && thetaFactor <= 1.0);
    }
    
    @Test
    void testCompleteInteractionUpdateFlow() {
        // Test the complete interaction update flow
        Map<String, Object> result = psychologicalStateService.updateStateFromInteraction(
            testUserId, testPostId, "resonance", 0.2
        );
        
        // Verify result structure
        assertNotNull(result);
        assertTrue(result.containsKey("userId"));
        assertTrue(result.containsKey("previousState"));
        assertTrue(result.containsKey("newState"));
        assertTrue(result.containsKey("rRaw"));
        assertTrue(result.containsKey("rPrime"));
        assertTrue(result.containsKey("interactionWeight"));
        assertTrue(result.containsKey("updateType"));
        
        // Verify user ID
        assertEquals(testUserId, result.get("userId"));
        
        // Verify state vectors have correct dimensions
        @SuppressWarnings("unchecked")
        List<Double> previousState = (List<Double>) result.get("previousState");
        @SuppressWarnings("unchecked")
        List<Double> newState = (List<Double>) result.get("newState");
        @SuppressWarnings("unchecked")
        List<Double> rRaw = (List<Double>) result.get("rRaw");
        @SuppressWarnings("unchecked")
        List<Double> rPrime = (List<Double>) result.get("rPrime");
        
        assertEquals(43, previousState.size());
        assertEquals(43, newState.size());
        assertEquals(43, rRaw.size());
        assertEquals(43, rPrime.size());
        
        // Verify update type
        assertEquals("INTERACTION", result.get("updateType"));
        
        // Verify interaction weight
        Double interactionWeight = (Double) result.get("interactionWeight");
        assertEquals(0.2, interactionWeight, 0.001);
    }
    
    @Test
    void testStateSimilarityCalculation() {
        // Create two test vectors
        List<Double> state1 = createTestVector(0.5);
        List<Double> state2 = createTestVector(0.7);
        
        // Calculate similarity
        double similarity = psychologicalStateService.calculateStateSimilarity(state1, state2);
        
        // Verify similarity is within valid range
        assertTrue(similarity >= -1.0 && similarity <= 1.0);
        
        // Test with identical vectors
        double identicalSimilarity = psychologicalStateService.calculateStateSimilarity(state1, state1);
        assertEquals(1.0, identicalSimilarity, 0.001);
    }
    
    @Test
    void testStateVectorCalculatorOperations() {
        // Test vector addition
        List<Double> vector1 = createTestVector(0.5);
        List<Double> vector2 = createTestVector(0.3);
        List<Double> sum = stateVectorCalculator.addVectors(vector1, vector2);
        
        assertEquals(43, sum.size());
        for (int i = 0; i < 43; i++) {
            assertEquals(0.8, sum.get(i), 0.001);
        }
        
        // Test scalar multiplication
        List<Double> scaled = stateVectorCalculator.multiplyByScalar(vector1, 2.0);
        assertEquals(43, scaled.size());
        for (int i = 0; i < 43; i++) {
            assertEquals(1.0, scaled.get(i), 0.001);
        }
        
        // Test normalization
        List<Double> normalized = stateVectorCalculator.normalizeVector(vector1);
        assertEquals(43, normalized.size());
        
        // Test distance calculation
        double distance = stateVectorCalculator.calculateDistance(vector1, vector2);
        assertTrue(distance >= 0.0);
        
        // Test cosine similarity
        double similarity = stateVectorCalculator.calculateCosineSimilarity(vector1, vector2);
        assertTrue(similarity >= -1.0 && similarity <= 1.0);
    }
    
    @Test
    void testIntensityFactorCalculation() {
        // Test intensity factor calculation
        List<Double> vRaw = createTestVector(0.5);
        double intensity = intensityFactorCalculator.calculateIntensityFactor(testPostContent, testUserId, vRaw);
        
        // Verify intensity is within valid range
        assertTrue(intensity >= 0.0 && intensity <= 1.0);
        
        // Test with different content
        String highIntensityContent = "I'm absolutely devastated and furious about this situation!";
        double highIntensity = intensityFactorCalculator.calculateIntensityFactor(highIntensityContent, testUserId, vRaw);
        assertTrue(highIntensity > intensity);
        
        // Test with low intensity content
        String lowIntensityContent = "This is okay, I guess.";
        double lowIntensity = intensityFactorCalculator.calculateIntensityFactor(lowIntensityContent, testUserId, vRaw);
        assertTrue(lowIntensity < intensity);
    }
    
    @Test
    void testStateUpdateRuleEngine() {
        // Test posting update rule
        List<Double> currentState = createTestVector(0.5);
        List<Double> vRaw = createTestVector(0.7);
        double alpha = 0.2;
        
        List<Double> newState = stateUpdateRuleEngine.applyPostingUpdate(currentState, vRaw, alpha);
        
        assertEquals(43, newState.size());
        
        // Test interaction update rule
        List<Double> rPrime = createTestVector(0.6);
        double wsWeight = 0.3;
        
        List<Double> interactionState = stateUpdateRuleEngine.applyInteractionUpdate(currentState, rPrime, wsWeight);
        
        assertEquals(43, interactionState.size());
        
        // Test dynamic alpha calculation
        double baseAlpha = 0.1;
        double thetaFactor = 0.3;
        double dynamicAlpha = stateUpdateRuleEngine.calculateDynamicAlpha(baseAlpha, thetaFactor);
        
        assertEquals(0.4, dynamicAlpha, 0.001);
    }
    
    @Test
    void testStateNormalizationService() {
        // Test L2 normalization
        List<Double> vector = createTestVector(2.0);
        List<Double> normalized = stateNormalizationService.normalizeVector(vector);
        
        assertEquals(43, normalized.size());
        
        // Test softmax normalization
        List<Double> softmax = stateNormalizationService.applySoftmaxNormalization(vector);
        
        assertEquals(43, softmax.size());
        
        // Verify softmax probabilities sum to 1
        double sum = softmax.stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, sum, 0.001);
        
        // Test thresholding
        List<Double> thresholded = stateNormalizationService.applyThresholding(vector, 1.0);
        
        assertEquals(43, thresholded.size());
        
        // Test clipping
        List<Double> clipped = stateNormalizationService.applyClipping(vector);
        
        assertEquals(43, clipped.size());
        
        // Verify clipped values are within bounds
        for (Double value : clipped) {
            assertTrue(value >= -2.0 && value <= 2.0);
        }
    }
    
    @Test
    void testStateHistoryPersistence() {
        // Update state from post
        psychologicalStateService.updateStateFromPost(testUserId, testPostContent, testPostId);
        
        // Check that state update history was created
        List<StateUpdateHistory> history = psychologicalStateService.getStateHistory(testUserId, 10);
        
        assertFalse(history.isEmpty());
        assertEquals(1, history.size());
        
        StateUpdateHistory update = history.get(0);
        assertEquals(testUserId, update.getUserId());
        assertEquals(StateUpdateHistory.UpdateType.POSTING, update.getUpdateType());
        assertEquals(testPostId, update.getSourceId());
        assertNotNull(update.getNewState());
        assertEquals(43, update.getNewState().size());
    }
    
    @Test
    void testStateInitialization() {
        // Initialize state for new user
        Map<String, Object> result = psychologicalStateService.initializeState(testUserId);
        
        assertNotNull(result);
        assertTrue(result.containsKey("userId"));
        assertTrue(result.containsKey("state"));
        assertTrue(result.containsKey("initialized"));
        
        assertEquals(testUserId, result.get("userId"));
        assertTrue((Boolean) result.get("initialized"));
        
        @SuppressWarnings("unchecked")
        List<Double> state = (List<Double>) result.get("state");
        assertEquals(43, state.size());
    }
    
    // ==================== HELPER METHODS ====================
    
    private List<Double> createTestVector(double value) {
        return java.util.Collections.nCopies(43, value);
    }
}


package ai.youthmind.service;

import ai.youthmind.entity.Post;
import ai.youthmind.entity.PostResonance;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.PostResonanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for InteractionModulationService
 */
@ExtendWith(MockitoExtension.class)
class InteractionModulationServiceTest {
    
    @Mock
    private PostResonanceRepository postResonanceRepository;
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private ResonanceProcessor resonanceProcessor;
    
    @Mock
    private LingerProcessor lingerProcessor;
    
    @Mock
    private PositiveBalanceCalculator positiveBalanceCalculator;
    
    @Mock
    private InteractionWeightService interactionWeightService;
    
    @InjectMocks
    private InteractionModulationService interactionModulationService;
    
    private UUID testUserId;
    private UUID testPostId;
    private Post testPost;
    
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPostId = UUID.randomUUID();
        
        testPost = new Post();
        testPost.setId(testPostId);
        testPost.setUserId(testUserId);
        testPost.setContent("Test post content");
        testPost.setEmotion("happy");
        testPost.setResonance(5);
        testPost.setView(10);
        testPost.setCollect(2);
        testPost.setCreatedAt(LocalDateTime.now().minusHours(2));
    }
    
    @Test
    void testProcessResonance_NewResonance() {
        // Arrange
        when(postResonanceRepository.existsByUserIdAndPostId(testUserId, testPostId)).thenReturn(false);
        when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
        when(resonanceProcessor.calculateResonanceImpact(testUserId, testPostId)).thenReturn(0.8);
        when(resonanceProcessor.updateStateFromResonance(testUserId, testPostId)).thenReturn(createMockStateUpdate());
        when(resonanceProcessor.trackResonancePatterns(testUserId)).thenReturn(createMockPatterns());
        when(positiveBalanceCalculator.calculatePositiveBalance(testUserId, testPostId)).thenReturn(0.7);
        when(interactionWeightService.calculateBeta()).thenReturn(1.2);
        
        // Act
        InteractionModulationService.InteractionModulationResult result = 
            interactionModulationService.processResonance(testUserId, testPostId);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("RESONANCE_ADD", result.getModulationType());
        assertEquals(testUserId, result.getUserId());
        assertEquals(testPostId, result.getPostId());
        assertTrue(result.getModulationValue() > 0);
        assertEquals(0.8, result.getResonanceImpact());
        assertEquals(0.7, result.getPositiveBalance());
        
        verify(postResonanceRepository).save(any(PostResonance.class));
        verify(resonanceProcessor).calculateResonanceImpact(testUserId, testPostId);
        verify(resonanceProcessor).updateStateFromResonance(testUserId, testPostId);
        verify(resonanceProcessor).trackResonancePatterns(testUserId);
        verify(positiveBalanceCalculator).calculatePositiveBalance(testUserId, testPostId);
    }
    
    @Test
    void testProcessResonance_ExistingResonance() {
        // Arrange
        when(postResonanceRepository.existsByUserIdAndPostId(testUserId, testPostId)).thenReturn(true);
        when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
        when(resonanceProcessor.calculateResonanceImpact(testUserId, testPostId)).thenReturn(0.6);
        when(resonanceProcessor.updateStateFromResonance(testUserId, testPostId)).thenReturn(createMockStateUpdate());
        when(resonanceProcessor.trackResonancePatterns(testUserId)).thenReturn(createMockPatterns());
        when(positiveBalanceCalculator.calculatePositiveBalance(testUserId, testPostId)).thenReturn(0.5);
        when(interactionWeightService.calculateBeta()).thenReturn(1.1);
        
        // Act
        InteractionModulationService.InteractionModulationResult result = 
            interactionModulationService.processResonance(testUserId, testPostId);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("RESONANCE_REMOVE", result.getModulationType());
        assertEquals(testUserId, result.getUserId());
        assertEquals(testPostId, result.getPostId());
        assertTrue(result.getModulationValue() >= 0);
        assertEquals(-0.6, result.getResonanceImpact()); // Should be negative for removal
        
        verify(postResonanceRepository).deleteByUserIdAndPostId(testUserId, testPostId);
        verify(postResonanceRepository, never()).save(any(PostResonance.class));
    }
    
    @Test
    void testProcessLinger() {
        // Arrange
        long lingerDuration = 30; // 30 seconds
        when(lingerProcessor.calculateLingerImpact(lingerDuration)).thenReturn(0.3);
        when(lingerProcessor.updateStateFromLinger(testUserId, testPostId, lingerDuration)).thenReturn(createMockStateUpdate());
        when(positiveBalanceCalculator.calculatePositiveBalance(testUserId, testPostId)).thenReturn(0.6);
        when(interactionWeightService.calculateBeta()).thenReturn(1.0);
        
        // Act
        InteractionModulationService.InteractionModulationResult result = 
            interactionModulationService.processLinger(testUserId, testPostId, lingerDuration);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("LINGER", result.getModulationType());
        assertEquals(testUserId, result.getUserId());
        assertEquals(testPostId, result.getPostId());
        assertEquals(lingerDuration, result.getLingerDuration());
        assertEquals(0.3, result.getLingerImpact());
        assertEquals(0.6, result.getPositiveBalance());
        
        verify(lingerProcessor).calculateLingerImpact(lingerDuration);
        verify(lingerProcessor).updateStateFromLinger(testUserId, testPostId, lingerDuration);
        verify(lingerProcessor).trackLingerDuration(testUserId, testPostId, lingerDuration);
        verify(positiveBalanceCalculator).calculatePositiveBalance(testUserId, testPostId);
    }
    
    @Test
    void testCalculateModulation() {
        // Arrange
        double resonanceWeight = 0.8;
        double lingerWeight = 0.3;
        double balanceWeight = 0.6;
        when(interactionWeightService.calculateBeta()).thenReturn(1.2);
        
        // Act
        double modulation = interactionModulationService.calculateModulation(
            resonanceWeight, lingerWeight, balanceWeight);
        
        // Assert
        assertTrue(modulation >= 0.0);
        assertTrue(modulation <= 1.0);
        assertTrue(modulation > 0); // Should be positive with given inputs
        
        verify(interactionWeightService).calculateBeta();
    }
    
    @Test
    void testApplyPositiveBalance() {
        // Arrange
        when(positiveBalanceCalculator.calculatePositiveBalance(testUserId, testPostId)).thenReturn(0.75);
        
        // Act
        double balance = interactionModulationService.applyPositiveBalance(testUserId, testPostId);
        
        // Assert
        assertEquals(0.75, balance);
        assertTrue(balance >= 0.0);
        assertTrue(balance <= 1.0);
        
        verify(positiveBalanceCalculator).calculatePositiveBalance(testUserId, testPostId);
        verify(positiveBalanceCalculator).updateBalanceVector(testUserId, testPostId, balance);
        verify(positiveBalanceCalculator).trackBalanceTrends(testUserId, balance);
    }
    
    @Test
    void testGetComprehensiveAnalysis() {
        // Arrange
        when(postResonanceRepository.findByUserIdAndResonancedAtAfter(
            eq(testUserId), any(LocalDateTime.class))).thenReturn(createMockResonances());
        when(resonanceProcessor.trackResonancePatterns(testUserId)).thenReturn(createMockPatterns());
        when(lingerProcessor.getLingerStatistics(testUserId)).thenReturn(createMockLingerStats());
        when(positiveBalanceCalculator.getBalanceTrends(testUserId)).thenReturn(createMockBalanceTrends());
        when(interactionWeightService.calculateBeta()).thenReturn(1.1);
        
        // Act
        InteractionModulationService.ComprehensiveInteractionAnalysis analysis = 
            interactionModulationService.getComprehensiveAnalysis(testUserId);
        
        // Assert
        assertNotNull(analysis);
        assertEquals(testUserId, analysis.getUserId());
        assertTrue(analysis.getResonanceCount() >= 0);
        assertNotNull(analysis.getResonancePatterns());
        assertNotNull(analysis.getLingerStatistics());
        assertNotNull(analysis.getBalanceTrends());
        assertTrue(analysis.getInteractionIntensity() > 0);
        assertNotNull(analysis.getAnalysisTimestamp());
        
        verify(resonanceProcessor).trackResonancePatterns(testUserId);
        verify(lingerProcessor).getLingerStatistics(testUserId);
        verify(positiveBalanceCalculator).getBalanceTrends(testUserId);
        verify(interactionWeightService).calculateBeta();
    }
    
    @Test
    void testProcessResonance_ErrorHandling() {
        // Arrange
        when(postResonanceRepository.existsByUserIdAndPostId(testUserId, testPostId))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act
        InteractionModulationService.InteractionModulationResult result = 
            interactionModulationService.processResonance(testUserId, testPostId);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertEquals("Failed to process resonance", result.getErrorMessage());
    }
    
    // Helper methods for creating mock data
    private java.util.Map<String, Object> createMockStateUpdate() {
        java.util.Map<String, Object> stateUpdate = new java.util.HashMap<>();
        stateUpdate.put("resonance_count", 5);
        stateUpdate.put("resonance_intensity", 0.8);
        Map<String, Object> emotionalResonance = new HashMap<>();
        emotionalResonance.put("emotion", "happy");
        stateUpdate.put("emotional_resonance", emotionalResonance);
        return stateUpdate;
    }
    
    private java.util.Map<String, Object> createMockPatterns() {
        java.util.Map<String, Object> patterns = new java.util.HashMap<>();
        patterns.put("consistency", 0.7);
        patterns.put("frequency", 2.5);
        Map<String, Object> temporal = new HashMap<>();
        temporal.put("peak_hour", 14);
        patterns.put("temporal", temporal);
        return patterns;
    }
    
    private java.util.Map<String, Object> createMockLingerStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total_linger_sessions", 10);
        stats.put("avg_linger_duration", 25.5);
        stats.put("max_linger_duration", 120L);
        return stats;
    }
    
    private java.util.Map<String, Object> createMockBalanceTrends() {
        java.util.Map<String, Object> trends = new java.util.HashMap<>();
        trends.put("trend_direction", "increasing");
        trends.put("trend_strength", 0.3);
        trends.put("avg_balance", 0.6);
        return trends;
    }
    
    private java.util.List<PostResonance> createMockResonances() {
        PostResonance resonance1 = new PostResonance(testUserId, testPostId);
        PostResonance resonance2 = new PostResonance(testUserId, UUID.randomUUID());
        return java.util.List.of(resonance1, resonance2);
    }
}

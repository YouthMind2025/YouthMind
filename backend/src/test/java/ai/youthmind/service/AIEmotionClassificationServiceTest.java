package ai.youthmind.service;

import ai.youthmind.dto.EmotionClassificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AIEmotionClassificationService
 * Tests the core emotion classification logic without database dependencies
 */
@ExtendWith(MockitoExtension.class)
public class AIEmotionClassificationServiceTest {
    
    @Mock
    private AIServiceClient aiServiceClient;
    
    @InjectMocks
    private AIEmotionClassificationService aiEmotionClassificationService;
    
    @Test
    void testClassifyEmotion_ValidContent_ReturnsClassification() {
        // Given
        String content = "I am feeling very happy today!";
        String mockResponse = "EMOTION: happy CONFIDENCE: 0.85";
        
        when(aiServiceClient.generateCompletion(anyString(), anyString()))
            .thenReturn(mockResponse);
        
        // When
        EmotionClassificationResult result = aiEmotionClassificationService.classifyEmotion(content);
        
        // Then
        assertNotNull(result);
        assertEquals("happy", result.getEmotion());
        assertEquals(0.85, result.getConfidence());
        assertEquals("ai_classified", result.getSource());
        assertTrue(result.isValid());
    }
    
    @Test
    void testClassifyEmotion_EmptyContent_ReturnsFallback() {
        // Given
        String content = "";
        
        // When
        EmotionClassificationResult result = aiEmotionClassificationService.classifyEmotion(content);
        
        // Then
        assertNotNull(result);
        assertEquals("neutral", result.getEmotion());
        assertEquals(0.0, result.getConfidence());
        assertEquals("ai_fallback", result.getSource());
    }
    
    @Test
    void testClassifyEmotion_NullContent_ReturnsFallback() {
        // Given
        String content = null;
        
        // When
        EmotionClassificationResult result = aiEmotionClassificationService.classifyEmotion(content);
        
        // Then
        assertNotNull(result);
        assertEquals("neutral", result.getEmotion());
        assertEquals(0.0, result.getConfidence());
        assertEquals("ai_fallback", result.getSource());
    }
    
    @Test
    void testClassifyEmotion_AIResponseInvalid_ReturnsFallback() {
        // Given
        String content = "I am feeling sad";
        String mockResponse = "Invalid response format";
        
        when(aiServiceClient.generateCompletion(anyString(), anyString()))
            .thenReturn(mockResponse);
        
        // When
        EmotionClassificationResult result = aiEmotionClassificationService.classifyEmotion(content);
        
        // Then
        assertNotNull(result);
        assertEquals("neutral", result.getEmotion());
        assertEquals(0.0, result.getConfidence());
        assertEquals("ai_fallback", result.getSource());
    }
    
    @Test
    void testClassifyEmotionBatch_ValidContents_ReturnsClassifications() {
        // Given
        List<String> contents = Arrays.asList(
            "I am very happy!",
            "I am so sad today.",
            "I am angry about this."
        );
        
        when(aiServiceClient.generateCompletion(anyString(), anyString()))
            .thenReturn("EMOTION: happy CONFIDENCE: 0.8")
            .thenReturn("EMOTION: sad CONFIDENCE: 0.9")
            .thenReturn("EMOTION: angry CONFIDENCE: 0.7");
        
        // When
        List<EmotionClassificationResult> results = aiEmotionClassificationService.classifyEmotionBatch(contents);
        
        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        
        assertEquals("happy", results.get(0).getEmotion());
        assertEquals(0.8, results.get(0).getConfidence());
        
        assertEquals("sad", results.get(1).getEmotion());
        assertEquals(0.9, results.get(1).getConfidence());
        
        assertEquals("angry", results.get(2).getEmotion());
        assertEquals(0.7, results.get(2).getConfidence());
    }
    
    @Test
    void testIsValidEmotion_ValidEmotions_ReturnsTrue() {
        // Test all valid emotions
        assertTrue(aiEmotionClassificationService.isValidEmotion("happy"));
        assertTrue(aiEmotionClassificationService.isValidEmotion("sad"));
        assertTrue(aiEmotionClassificationService.isValidEmotion("angry"));
        assertTrue(aiEmotionClassificationService.isValidEmotion("conflicted"));
        assertTrue(aiEmotionClassificationService.isValidEmotion("surprised"));
        assertTrue(aiEmotionClassificationService.isValidEmotion("fear"));
    }
    
    @Test
    void testIsValidEmotion_InvalidEmotions_ReturnsFalse() {
        // Test invalid emotions
        assertFalse(aiEmotionClassificationService.isValidEmotion("excited"));
        assertFalse(aiEmotionClassificationService.isValidEmotion("calm"));
        assertFalse(aiEmotionClassificationService.isValidEmotion("neutral"));
        assertFalse(aiEmotionClassificationService.isValidEmotion(""));
        assertFalse(aiEmotionClassificationService.isValidEmotion(null));
    }
    
    @Test
    void testGetValidEmotions_ReturnsCorrectList() {
        // When
        List<String> validEmotions = aiEmotionClassificationService.getValidEmotions();
        
        // Then
        assertNotNull(validEmotions);
        assertEquals(6, validEmotions.size());
        assertTrue(validEmotions.contains("happy"));
        assertTrue(validEmotions.contains("sad"));
        assertTrue(validEmotions.contains("angry"));
        assertTrue(validEmotions.contains("conflicted"));
        assertTrue(validEmotions.contains("surprised"));
        assertTrue(validEmotions.contains("fear"));
    }
    
    @Test
    void testGetClassificationStats_ValidResults_ReturnsCorrectStats() {
        // Given
        List<EmotionClassificationResult> results = Arrays.asList(
            new EmotionClassificationResult("happy", 0.8, "ai_classified"),
            new EmotionClassificationResult("sad", 0.9, "ai_classified"),
            new EmotionClassificationResult("neutral", 0.0, "ai_fallback")
        );
        
        // When
        var stats = aiEmotionClassificationService.getClassificationStats(results);
        
        // Then
        assertNotNull(stats);
        assertEquals(3, stats.get("total"));
        assertEquals(2L, stats.get("ai_classified"));
        assertEquals(1L, stats.get("ai_fallback"));
        assertEquals(0.5667, (Double) stats.get("average_confidence"), 0.001);
    }
}

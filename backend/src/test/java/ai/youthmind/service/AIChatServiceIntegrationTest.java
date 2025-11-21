package ai.youthmind.service;

import ai.youthmind.dto.AIChatRequest;
import ai.youthmind.dto.AIChatResponse;
import ai.youthmind.dto.UserPersonalityProfile;
import ai.youthmind.entity.User;
import ai.youthmind.repository.ConversationRepository;
import ai.youthmind.repository.MessageRepository;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.util.NLPAnalyzer;
import ai.youthmind.util.PersonalityAdapter;
import ai.youthmind.util.SafetyFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class AIChatServiceIntegrationTest {

    @MockBean
    private AIServiceClient aiServiceClient;
    
    @MockBean
    private ConversationRepository conversationRepository;
    
    @MockBean
    private MessageRepository messageRepository;
    
    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private RedisTemplate<String, Object> redisTemplate;
    
    @MockBean
    private ValueOperations<String, Object> valueOperations;
    
    @MockBean
    private NLPAnalyzer nlpAnalyzer;
    
    @MockBean
    private PersonalityAdapter personalityAdapter;
    
    @MockBean
    private SafetyFilter safetyFilter;
    
    @Autowired
    private AIChatService aiChatService;
    
    @BeforeEach
    void setUp() {
        // Spring will automatically inject the mocked dependencies via @MockBean
        // No manual reflection injection needed
    }
    
    @Test
    void testProcessChatRequest_WithMockAIResponse() {
        // Given
        UUID userId = UUID.randomUUID();
        AIChatRequest request = new AIChatRequest();
        request.setMessage("I'm feeling anxious about school");
        request.setConversationId(UUID.randomUUID().toString());
        request.setEnablePersonalityAdaptation(true);
        request.setEnableSafetyFiltering(true);
        
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        Map<String, Object> nlpAnalysis = new HashMap<>();
        nlpAnalysis.put("emotion", "anxious");
        nlpAnalysis.put("sentiment", "negative");
        nlpAnalysis.put("confidence", 0.8);
        
        UserPersonalityProfile personalityProfile = new UserPersonalityProfile();
        personalityProfile.setUserId(userId.toString());
        personalityProfile.setPreferredPersonalityType("supportive");
        personalityProfile.setCommunicationStyle("gentle");
        personalityProfile.setEmotionalState("anxious");
        personalityProfile.setInterests(Arrays.asList("mental health", "anxiety"));
        personalityProfile.setEnableHumor(true);
        personalityProfile.setEnableEmpathy(true);
        personalityProfile.setLastUpdated(LocalDateTime.now());
        
        // Mock dependencies
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(nlpAnalyzer.analyzeMessage(anyString())).thenReturn(nlpAnalysis);
        when(personalityAdapter.adaptPersonality(any(), any(), anyString())).thenReturn("supportive");
        when(safetyFilter.filterMessage(anyString(), any(), any())).thenReturn(
            ai.youthmind.dto.AISafetyFilterResult.safe("Content is safe")
        );
        
        // Mock AI service response
        ai.youthmind.dto.DeepSeekModerationResponse aiResponse = new ai.youthmind.dto.DeepSeekModerationResponse();
        ai.youthmind.dto.DeepSeekModerationResponse.Choice choice = new ai.youthmind.dto.DeepSeekModerationResponse.Choice();
        ai.youthmind.dto.DeepSeekModerationResponse.Message message = new ai.youthmind.dto.DeepSeekModerationResponse.Message();
        message.setContent("I understand that school can feel overwhelming sometimes. It's completely normal to feel anxious about academic pressures. What specific aspects of school are causing you the most stress?");
        choice.setMessage(message);
        aiResponse.setChoices(Arrays.asList(choice));
        
        when(aiServiceClient.generateChatCompletion(any())).thenReturn(aiResponse);
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            ai.youthmind.entity.Message msg = invocation.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });
        
        // When
        AIChatResponse response = aiChatService.processChatRequest(request, userId);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getResponse());
        assertTrue(response.getResponse().contains("anxious") || response.getResponse().contains("school"));
        assertEquals("supportive", response.getPersonalityUsed());
        assertEquals("anxious", response.getEmotionDetected());
        assertFalse(response.isSafetyFiltered());
        assertNotNull(response.getTimestamp());
        
        // Verify interactions
        verify(aiServiceClient).generateChatCompletion(any());
        verify(safetyFilter, atLeastOnce()).filterMessage(anyString(), any(), any());
        verify(messageRepository, times(2)).save(any()); // User message + AI message
    }
    
    @Test
    void testProcessChatRequest_WithSafetyFilterBlock() {
        // Given
        UUID userId = UUID.randomUUID();
        AIChatRequest request = new AIChatRequest();
        request.setMessage("I want to hurt myself");
        request.setConversationId(UUID.randomUUID().toString());
        request.setEnablePersonalityAdaptation(true);
        request.setEnableSafetyFiltering(true);
        
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        Map<String, Object> nlpAnalysis = new HashMap<>();
        nlpAnalysis.put("emotion", "distressed");
        nlpAnalysis.put("sentiment", "negative");
        nlpAnalysis.put("confidence", 0.9);
        
        // Mock safety filter to block the message
        ai.youthmind.dto.AISafetyFilterResult blockedResult = ai.youthmind.dto.AISafetyFilterResult.unsafe(
            "Content contains self-harm references", 
            3, 
            "blocked", 
            Arrays.asList("hurt", "myself"), 
            Arrays.asList("self-harm"), 
            0.95, 
            true
        );
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(nlpAnalyzer.analyzeMessage(anyString())).thenReturn(nlpAnalysis);
        when(safetyFilter.filterMessage(anyString(), any(), any())).thenReturn(blockedResult);
        
        // When
        AIChatResponse response = aiChatService.processChatRequest(request, userId);
        
        // Then
        assertNotNull(response);
        assertTrue(response.isSafetyFiltered());
        assertEquals("Content contains self-harm references", response.getSafetyReason());
        assertTrue(response.getResponse().contains("safe") || response.getResponse().contains("support"));
        assertTrue(response.isNeedsHumanReview());
        
        // Verify AI service was not called due to safety filter
        verify(aiServiceClient, never()).generateChatCompletion(any());
        verify(safetyFilter).filterMessage(anyString(), any(), any());
    }
    
    @Test
    void testGetUserPersonalityProfile_FromCache() {
        // Given
        UUID userId = UUID.randomUUID();
        UserPersonalityProfile cachedProfile = new UserPersonalityProfile();
        cachedProfile.setUserId(userId.toString());
        cachedProfile.setPreferredPersonalityType("empathetic");
        cachedProfile.setLastUpdated(LocalDateTime.now());
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user_personality:" + userId)).thenReturn(cachedProfile);
        
        // When
        UserPersonalityProfile result = aiChatService.getUserPersonalityProfile(userId);
        
        // Then
        assertNotNull(result);
        assertEquals("empathetic", result.getPreferredPersonalityType());
        verify(userRepository, never()).findById(any());
    }
    
    @Test
    void testGetUserPersonalityProfile_FromDatabase() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user_personality:" + userId)).thenReturn(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // When
        UserPersonalityProfile result = aiChatService.getUserPersonalityProfile(userId);
        
        // Then
        assertNotNull(result);
        assertEquals("supportive", result.getPreferredPersonalityType()); // Default
        assertEquals(userId.toString(), result.getUserId());
        verify(userRepository).findById(userId);
    }
}

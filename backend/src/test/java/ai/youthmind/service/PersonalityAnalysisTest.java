package ai.youthmind.service;

import ai.youthmind.dto.AIChatRequest;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalityAnalysisTest {

    @Mock
    private AIServiceClient aiServiceClient;
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @Mock
    private MessageRepository messageRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private NLPAnalyzer nlpAnalyzer;
    
    @Mock
    private PersonalityAdapter personalityAdapter;
    
    @Mock
    private SafetyFilter safetyFilter;
    
    private AIChatService aiChatService;
    
    @BeforeEach
    void setUp() {
        aiChatService = new AIChatService();
        // Use reflection to inject mocks
        try {
            java.lang.reflect.Field aiServiceClientField = AIChatService.class.getDeclaredField("aiServiceClient");
            aiServiceClientField.setAccessible(true);
            aiServiceClientField.set(aiChatService, aiServiceClient);
            
            java.lang.reflect.Field conversationRepositoryField = AIChatService.class.getDeclaredField("conversationRepository");
            conversationRepositoryField.setAccessible(true);
            conversationRepositoryField.set(aiChatService, conversationRepository);
            
            java.lang.reflect.Field messageRepositoryField = AIChatService.class.getDeclaredField("messageRepository");
            messageRepositoryField.setAccessible(true);
            messageRepositoryField.set(aiChatService, messageRepository);
            
            java.lang.reflect.Field userRepositoryField = AIChatService.class.getDeclaredField("userRepository");
            userRepositoryField.setAccessible(true);
            userRepositoryField.set(aiChatService, userRepository);
            
            java.lang.reflect.Field redisTemplateField = AIChatService.class.getDeclaredField("redisTemplate");
            redisTemplateField.setAccessible(true);
            redisTemplateField.set(aiChatService, redisTemplate);
            
            java.lang.reflect.Field nlpAnalyzerField = AIChatService.class.getDeclaredField("nlpAnalyzer");
            nlpAnalyzerField.setAccessible(true);
            nlpAnalyzerField.set(aiChatService, nlpAnalyzer);
            
            java.lang.reflect.Field personalityAdapterField = AIChatService.class.getDeclaredField("personalityAdapter");
            personalityAdapterField.setAccessible(true);
            personalityAdapterField.set(aiChatService, personalityAdapter);
            
            java.lang.reflect.Field safetyFilterField = AIChatService.class.getDeclaredField("safetyFilter");
            safetyFilterField.setAccessible(true);
            safetyFilterField.set(aiChatService, safetyFilter);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject dependencies", e);
        }
    }
    
    @Test
    void testAnalyzeUserPersonality_WithDeepSeekAPI() {
        // Given
        UUID userId = UUID.randomUUID();
        String sampleMessage = "I'm really struggling with anxiety and need someone to talk to";
        
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        UserPersonalityProfile currentProfile = new UserPersonalityProfile();
        currentProfile.setUserId(userId.toString());
        currentProfile.setPreferredPersonalityType("supportive");
        currentProfile.setCommunicationStyle("gentle");
        currentProfile.setEmotionalState("neutral");
        currentProfile.setInterests(Arrays.asList("general"));
        currentProfile.setEnableHumor(true);
        currentProfile.setEnableEmpathy(true);
        currentProfile.setConversationCount(0);
        currentProfile.setLastUpdated(LocalDateTime.now());
        
        Map<String, Object> nlpAnalysis = new HashMap<>();
        nlpAnalysis.put("emotion", "anxious");
        nlpAnalysis.put("sentiment", "negative");
        nlpAnalysis.put("urgency", "high");
        nlpAnalysis.put("complexity", "medium");
        nlpAnalysis.put("confidence", 0.9);
        
        // Mock AI response for personality analysis
        ai.youthmind.dto.DeepSeekModerationResponse aiResponse = new ai.youthmind.dto.DeepSeekModerationResponse();
        ai.youthmind.dto.DeepSeekModerationResponse.Choice choice = new ai.youthmind.dto.DeepSeekModerationResponse.Choice();
        ai.youthmind.dto.DeepSeekModerationResponse.Message message = new ai.youthmind.dto.DeepSeekModerationResponse.Message();
        
        // Mock JSON response from DeepSeek API
        String jsonResponse = """
            {
                "preferredPersonalityType": "empathetic",
                "communicationStyle": "gentle",
                "emotionalState": "anxious",
                "interests": ["mental health", "anxiety", "support"],
                "enableHumor": false,
                "enableEmpathy": true,
                "preferredResponseLength": ["medium", "long"],
                "culturalContext": "general",
                "confidence": 0.85,
                "reasoning": "User shows high anxiety and seeks emotional support, indicating preference for empathetic responses"
            }
            """;
        
        message.setContent(jsonResponse);
        choice.setMessage(message);
        aiResponse.setChoices(Arrays.asList(choice));
        
        // Mock dependencies
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user_personality:" + userId)).thenReturn(currentProfile);
        when(nlpAnalyzer.analyzeMessage(sampleMessage)).thenReturn(nlpAnalysis);
        when(aiServiceClient.generateChatCompletion(any())).thenReturn(aiResponse);
        
        // When
        String result = aiChatService.analyzeUserPersonality(userId, sampleMessage);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("Personality Analysis Results"));
        assertTrue(result.contains("Previous Personality: supportive"));
        assertTrue(result.contains("Updated Personality: empathetic"));
        assertTrue(result.contains("Emotional State: anxious"));
        assertTrue(result.contains("Enable Empathy: true"));
        
        // Verify AI service was called
        verify(aiServiceClient).generateChatCompletion(any());
        verify(nlpAnalyzer).analyzeMessage(sampleMessage);
    }
    
    @Test
    void testAnalyzeUserPersonality_WithFallback() {
        // Given
        UUID userId = UUID.randomUUID();
        String sampleMessage = "I'm feeling happy today!";
        
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        UserPersonalityProfile currentProfile = new UserPersonalityProfile();
        currentProfile.setUserId(userId.toString());
        currentProfile.setPreferredPersonalityType("supportive");
        currentProfile.setCommunicationStyle("gentle");
        currentProfile.setEmotionalState("neutral");
        currentProfile.setInterests(Arrays.asList("general"));
        currentProfile.setEnableHumor(true);
        currentProfile.setEnableEmpathy(true);
        currentProfile.setConversationCount(5);
        currentProfile.setLastUpdated(LocalDateTime.now());
        
        Map<String, Object> nlpAnalysis = new HashMap<>();
        nlpAnalysis.put("emotion", "happy");
        nlpAnalysis.put("sentiment", "positive");
        nlpAnalysis.put("urgency", "normal");
        nlpAnalysis.put("complexity", "simple");
        nlpAnalysis.put("confidence", 0.8);
        
        // Mock AI service failure
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user_personality:" + userId)).thenReturn(currentProfile);
        when(nlpAnalyzer.analyzeMessage(sampleMessage)).thenReturn(nlpAnalysis);
        when(aiServiceClient.generateChatCompletion(any())).thenThrow(new RuntimeException("API Error"));
        
        // When
        String result = aiChatService.analyzeUserPersonality(userId, sampleMessage);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("Personality Analysis Results"));
        assertTrue(result.contains("Previous Personality: supportive"));
        // Should fallback to basic analysis which might change personality based on emotion
        assertTrue(result.contains("Updated Personality:"));
        
        // Verify AI service was called but failed
        verify(aiServiceClient).generateChatCompletion(any());
        verify(nlpAnalyzer).analyzeMessage(sampleMessage);
    }
    
    @Test
    void testPersonalityAnalysisPrompt() {
        // Given
        UUID userId = UUID.randomUUID();
        UserPersonalityProfile currentProfile = new UserPersonalityProfile();
        currentProfile.setUserId(userId.toString());
        currentProfile.setPreferredPersonalityType("supportive");
        currentProfile.setCommunicationStyle("gentle");
        currentProfile.setEmotionalState("neutral");
        currentProfile.setInterests(Arrays.asList("mental health"));
        currentProfile.setEnableHumor(true);
        currentProfile.setEnableEmpathy(true);
        currentProfile.setConversationCount(3);
        currentProfile.setLastUpdated(LocalDateTime.now());
        
        AIChatRequest request = new AIChatRequest();
        request.setMessage("I need help with my anxiety");
        request.setConversationId(UUID.randomUUID().toString());
        
        Map<String, Object> nlpAnalysis = new HashMap<>();
        nlpAnalysis.put("emotion", "anxious");
        nlpAnalysis.put("sentiment", "negative");
        nlpAnalysis.put("urgency", "high");
        nlpAnalysis.put("complexity", "medium");
        
        String aiResponse = "I understand you're feeling anxious. Let's work through this together.";
        String conversationContext = "User is discussing anxiety and seeking support.";
        
        // When - This would test the buildPersonalityAnalysisPrompt method
        // We can't directly test private methods, but we can verify the overall flow
        String result = aiChatService.analyzeUserPersonality(userId, request.getMessage());
        
        // Then
        assertNotNull(result);
        // The result should contain analysis information
        assertTrue(result.contains("Personality Analysis Results"));
    }
    
    @Test
    void testPersonalityProfileUpdate() {
        // Given
        UUID userId = UUID.randomUUID();
        UserPersonalityProfile currentProfile = new UserPersonalityProfile();
        currentProfile.setUserId(userId.toString());
        currentProfile.setPreferredPersonalityType("supportive");
        currentProfile.setCommunicationStyle("gentle");
        currentProfile.setEmotionalState("neutral");
        currentProfile.setInterests(Arrays.asList("general"));
        currentProfile.setEnableHumor(true);
        currentProfile.setEnableEmpathy(true);
        currentProfile.setConversationCount(0);
        currentProfile.setLastUpdated(LocalDateTime.now());
        
        // When
        UserPersonalityProfile updatedProfile = aiChatService.updateUserPersonalityProfile(userId, currentProfile);
        
        // Then
        assertNotNull(updatedProfile);
        assertEquals(userId.toString(), updatedProfile.getUserId());
        assertEquals(currentProfile.getPreferredPersonalityType(), updatedProfile.getPreferredPersonalityType());
        assertEquals(currentProfile.getCommunicationStyle(), updatedProfile.getCommunicationStyle());
        assertTrue(updatedProfile.getLastUpdated().isAfter(currentProfile.getLastUpdated()));
        
        // Verify caching
        verify(redisTemplate.opsForValue()).set(anyString(), any(), any(), any());
    }
}

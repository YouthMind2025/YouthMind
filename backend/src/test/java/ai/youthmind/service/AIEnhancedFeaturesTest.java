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
class AIEnhancedFeaturesTest {

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
    void testGenerateFollowUpQuestions_WithAI() {
        // Given
        String aiResponse = "I understand that you're feeling overwhelmed with school and social pressures. It's completely normal to feel this way, and you're not alone in experiencing these challenges. Let's work together to find some strategies that might help you manage these feelings.";
        
        // Mock AI response for follow-up questions
        ai.youthmind.dto.DeepSeekModerationResponse followUpResponse = new ai.youthmind.dto.DeepSeekModerationResponse();
        ai.youthmind.dto.DeepSeekModerationResponse.Choice choice = new ai.youthmind.dto.DeepSeekModerationResponse.Choice();
        ai.youthmind.dto.DeepSeekModerationResponse.Message message = new ai.youthmind.dto.DeepSeekModerationResponse.Message();
        
        String followUpQuestions = """
            1. What specific aspects of school are causing you the most stress right now?
            2. Have you noticed any patterns in when these overwhelming feelings tend to occur?
            3. What kind of support system do you have in place, and how can we strengthen it?
            """;
        
        message.setContent(followUpQuestions);
        choice.setMessage(message);
        followUpResponse.setChoices(Arrays.asList(choice));
        
        when(aiServiceClient.generateChatCompletion(any())).thenReturn(followUpResponse);
        
        // When - This would test the generateFollowUpQuestions method
        // We can't directly test private methods, but we can test through the main flow
        UUID userId = UUID.randomUUID();
        AIChatRequest request = new AIChatRequest();
        request.setMessage("I'm feeling overwhelmed with school and social pressures");
        request.setConversationId(UUID.randomUUID().toString());
        request.setEnablePersonalityAdaptation(true);
        request.setEnableSafetyFiltering(true);
        
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        UserPersonalityProfile profile = new UserPersonalityProfile();
        profile.setUserId(userId.toString());
        profile.setPreferredPersonalityType("supportive");
        profile.setCommunicationStyle("gentle");
        profile.setEmotionalState("overwhelmed");
        profile.setInterests(Arrays.asList("mental health"));
        profile.setEnableHumor(true);
        profile.setEnableEmpathy(true);
        profile.setConversationCount(0);
        profile.setLastUpdated(LocalDateTime.now());
        
        Map<String, Object> nlpAnalysis = new HashMap<>();
        nlpAnalysis.put("emotion", "overwhelmed");
        nlpAnalysis.put("sentiment", "negative");
        nlpAnalysis.put("urgency", "high");
        nlpAnalysis.put("complexity", "medium");
        nlpAnalysis.put("confidence", 0.9);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user_personality:" + userId)).thenReturn(profile);
        when(nlpAnalyzer.analyzeMessage(anyString())).thenReturn(nlpAnalysis);
        when(personalityAdapter.adaptPersonality(any(), any(), anyString())).thenReturn("supportive");
        when(safetyFilter.filterMessage(anyString(), any(), any())).thenReturn(
            ai.youthmind.dto.AISafetyFilterResult.safe("Content is safe")
        );
        
        // Mock AI response for main chat
        ai.youthmind.dto.DeepSeekModerationResponse chatResponse = new ai.youthmind.dto.DeepSeekModerationResponse();
        ai.youthmind.dto.DeepSeekModerationResponse.Choice chatChoice = new ai.youthmind.dto.DeepSeekModerationResponse.Choice();
        ai.youthmind.dto.DeepSeekModerationResponse.Message chatMessage = new ai.youthmind.dto.DeepSeekModerationResponse.Message();
        chatMessage.setContent(aiResponse);
        chatChoice.setMessage(chatMessage);
        chatResponse.setChoices(Arrays.asList(chatChoice));
        
        when(aiServiceClient.generateChatCompletion(any())).thenReturn(chatResponse, followUpResponse);
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            ai.youthmind.entity.Message msg = invocation.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });
        
        // When
        ai.youthmind.dto.AIChatResponse response = aiChatService.processChatRequest(request, userId);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getFollowUpQuestions());
        assertFalse(response.getFollowUpQuestions().isEmpty());
        assertTrue(response.getFollowUpQuestions().size() <= 3);
        
        // Verify the questions are relevant and well-formed
        for (String question : response.getFollowUpQuestions()) {
            assertTrue(question.endsWith("?"));
            assertTrue(question.length() > 10);
            assertTrue(question.length() < 200);
        }
        
        // Verify AI service was called for follow-up questions
        verify(aiServiceClient, atLeast(2)).generateChatCompletion(any());
    }
    
    @Test
    void testExtractSuggestedTopics_WithAI() {
        // Given
        String aiResponse = "I understand that you're struggling with anxiety about your upcoming exams and feeling isolated from your friends. This is a common experience for many students, and there are several strategies we can explore to help you manage these feelings and reconnect with your social circle.";
        
        // Mock AI response for topic extraction
        ai.youthmind.dto.DeepSeekModerationResponse topicResponse = new ai.youthmind.dto.DeepSeekModerationResponse();
        ai.youthmind.dto.DeepSeekModerationResponse.Choice choice = new ai.youthmind.dto.DeepSeekModerationResponse.Choice();
        ai.youthmind.dto.DeepSeekModerationResponse.Message message = new ai.youthmind.dto.DeepSeekModerationResponse.Message();
        
        String topics = "anxiety, exam stress, social isolation, student life, coping strategies";
        message.setContent(topics);
        choice.setMessage(message);
        topicResponse.setChoices(Arrays.asList(choice));
        
        when(aiServiceClient.generateChatCompletion(any())).thenReturn(topicResponse);
        
        // When - Test through the main chat flow
        UUID userId = UUID.randomUUID();
        AIChatRequest request = new AIChatRequest();
        request.setMessage("I'm struggling with anxiety about exams and feeling isolated from friends");
        request.setConversationId(UUID.randomUUID().toString());
        request.setEnablePersonalityAdaptation(true);
        request.setEnableSafetyFiltering(true);
        
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        UserPersonalityProfile profile = new UserPersonalityProfile();
        profile.setUserId(userId.toString());
        profile.setPreferredPersonalityType("supportive");
        profile.setCommunicationStyle("gentle");
        profile.setEmotionalState("anxious");
        profile.setInterests(Arrays.asList("mental health"));
        profile.setEnableHumor(true);
        profile.setEnableEmpathy(true);
        profile.setConversationCount(0);
        profile.setLastUpdated(LocalDateTime.now());
        
        Map<String, Object> nlpAnalysis = new HashMap<>();
        nlpAnalysis.put("emotion", "anxious");
        nlpAnalysis.put("sentiment", "negative");
        nlpAnalysis.put("urgency", "medium");
        nlpAnalysis.put("complexity", "medium");
        nlpAnalysis.put("confidence", 0.8);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user_personality:" + userId)).thenReturn(profile);
        when(nlpAnalyzer.analyzeMessage(anyString())).thenReturn(nlpAnalysis);
        when(personalityAdapter.adaptPersonality(any(), any(), anyString())).thenReturn("supportive");
        when(safetyFilter.filterMessage(anyString(), any(), any())).thenReturn(
            ai.youthmind.dto.AISafetyFilterResult.safe("Content is safe")
        );
        
        // Mock AI response for main chat
        ai.youthmind.dto.DeepSeekModerationResponse chatResponse = new ai.youthmind.dto.DeepSeekModerationResponse();
        ai.youthmind.dto.DeepSeekModerationResponse.Choice chatChoice = new ai.youthmind.dto.DeepSeekModerationResponse.Choice();
        ai.youthmind.dto.DeepSeekModerationResponse.Message chatMessage = new ai.youthmind.dto.DeepSeekModerationResponse.Message();
        chatMessage.setContent(aiResponse);
        chatChoice.setMessage(chatMessage);
        chatResponse.setChoices(Arrays.asList(chatChoice));
        
        when(aiServiceClient.generateChatCompletion(any())).thenReturn(chatResponse, topicResponse);
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            ai.youthmind.entity.Message msg = invocation.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });
        
        // When
        ai.youthmind.dto.AIChatResponse response = aiChatService.processChatRequest(request, userId);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getSuggestedTopics());
        assertFalse(response.getSuggestedTopics().isEmpty());
        assertTrue(response.getSuggestedTopics().size() <= 5);
        
        // Verify topics are relevant and well-formed
        for (String topic : response.getSuggestedTopics()) {
            assertTrue(topic.length() > 2);
            assertTrue(topic.length() < 30);
            assertFalse(topic.contains("?"));
        }
        
        // Verify AI service was called for topic extraction
        verify(aiServiceClient, atLeast(2)).generateChatCompletion(any());
    }
    
    @Test
    void testFollowUpQuestions_Fallback() {
        // Given
        String aiResponse = "I understand you're feeling anxious about your future.";
        
        // Mock AI service failure
        when(aiServiceClient.generateChatCompletion(any())).thenThrow(new RuntimeException("API Error"));
        
        // When - Test through the main chat flow
        UUID userId = UUID.randomUUID();
        AIChatRequest request = new AIChatRequest();
        request.setMessage("I'm anxious about my future");
        request.setConversationId(UUID.randomUUID().toString());
        request.setEnablePersonalityAdaptation(true);
        request.setEnableSafetyFiltering(true);
        
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        UserPersonalityProfile profile = new UserPersonalityProfile();
        profile.setUserId(userId.toString());
        profile.setPreferredPersonalityType("supportive");
        profile.setCommunicationStyle("gentle");
        profile.setEmotionalState("anxious");
        profile.setInterests(Arrays.asList("mental health"));
        profile.setEnableHumor(true);
        profile.setEnableEmpathy(true);
        profile.setConversationCount(0);
        profile.setLastUpdated(LocalDateTime.now());
        
        Map<String, Object> nlpAnalysis = new HashMap<>();
        nlpAnalysis.put("emotion", "anxious");
        nlpAnalysis.put("sentiment", "negative");
        nlpAnalysis.put("urgency", "medium");
        nlpAnalysis.put("complexity", "medium");
        nlpAnalysis.put("confidence", 0.8);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user_personality:" + userId)).thenReturn(profile);
        when(nlpAnalyzer.analyzeMessage(anyString())).thenReturn(nlpAnalysis);
        when(personalityAdapter.adaptPersonality(any(), any(), anyString())).thenReturn("supportive");
        when(safetyFilter.filterMessage(anyString(), any(), any())).thenReturn(
            ai.youthmind.dto.AISafetyFilterResult.safe("Content is safe")
        );
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            ai.youthmind.entity.Message msg = invocation.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });
        
        // When
        ai.youthmind.dto.AIChatResponse response = aiChatService.processChatRequest(request, userId);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getFollowUpQuestions());
        assertFalse(response.getFollowUpQuestions().isEmpty());
        
        // Should have fallback questions
        assertTrue(response.getFollowUpQuestions().size() >= 2);
        assertTrue(response.getFollowUpQuestions().size() <= 3);
        
        // Verify questions are relevant to anxiety
        boolean hasAnxietyQuestion = response.getFollowUpQuestions().stream()
            .anyMatch(q -> q.toLowerCase().contains("anxiety") || q.toLowerCase().contains("worried"));
        assertTrue(hasAnxietyQuestion);
    }
    
    @Test
    void testTopicExtraction_Fallback() {
        // Given
        String aiResponse = "I understand you're dealing with depression and relationship issues.";
        
        // Mock AI service failure
        when(aiServiceClient.generateChatCompletion(any())).thenThrow(new RuntimeException("API Error"));
        
        // When - Test through the main chat flow
        UUID userId = UUID.randomUUID();
        AIChatRequest request = new AIChatRequest();
        request.setMessage("I'm dealing with depression and relationship issues");
        request.setConversationId(UUID.randomUUID().toString());
        request.setEnablePersonalityAdaptation(true);
        request.setEnableSafetyFiltering(true);
        
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        
        UserPersonalityProfile profile = new UserPersonalityProfile();
        profile.setUserId(userId.toString());
        profile.setPreferredPersonalityType("supportive");
        profile.setCommunicationStyle("gentle");
        profile.setEmotionalState("sad");
        profile.setInterests(Arrays.asList("mental health"));
        profile.setEnableHumor(true);
        profile.setEnableEmpathy(true);
        profile.setConversationCount(0);
        profile.setLastUpdated(LocalDateTime.now());
        
        Map<String, Object> nlpAnalysis = new HashMap<>();
        nlpAnalysis.put("emotion", "sad");
        nlpAnalysis.put("sentiment", "negative");
        nlpAnalysis.put("urgency", "high");
        nlpAnalysis.put("complexity", "medium");
        nlpAnalysis.put("confidence", 0.9);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user_personality:" + userId)).thenReturn(profile);
        when(nlpAnalyzer.analyzeMessage(anyString())).thenReturn(nlpAnalysis);
        when(personalityAdapter.adaptPersonality(any(), any(), anyString())).thenReturn("supportive");
        when(safetyFilter.filterMessage(anyString(), any(), any())).thenReturn(
            ai.youthmind.dto.AISafetyFilterResult.safe("Content is safe")
        );
        when(messageRepository.save(any())).thenAnswer(invocation -> {
            ai.youthmind.entity.Message msg = invocation.getArgument(0);
            msg.setId(UUID.randomUUID());
            return msg;
        });
        
        // When
        ai.youthmind.dto.AIChatResponse response = aiChatService.processChatRequest(request, userId);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getSuggestedTopics());
        assertFalse(response.getSuggestedTopics().isEmpty());
        
        // Should have fallback topics
        assertTrue(response.getSuggestedTopics().size() >= 2);
        assertTrue(response.getSuggestedTopics().size() <= 5);
        
        // Verify topics include depression and relationships
        boolean hasDepressionTopic = response.getSuggestedTopics().stream()
            .anyMatch(t -> t.toLowerCase().contains("depression"));
        boolean hasRelationshipTopic = response.getSuggestedTopics().stream()
            .anyMatch(t -> t.toLowerCase().contains("relationship"));
        
        assertTrue(hasDepressionTopic || hasRelationshipTopic);
    }
}

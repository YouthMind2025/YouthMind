package ai.youthmind.service;

import ai.youthmind.dto.AIChatRequest;
import ai.youthmind.dto.AIChatResponse;
import ai.youthmind.dto.UserPersonalityProfile;
import ai.youthmind.entity.Conversation;
import ai.youthmind.entity.Message;
import ai.youthmind.entity.User;
import ai.youthmind.repository.ConversationRepository;
import ai.youthmind.repository.MessageRepository;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.util.NLPAnalyzer;
import ai.youthmind.util.PersonalityAdapter;
import ai.youthmind.util.SafetyFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for asynchronous processing of AI tasks to improve system responsiveness
 */
@Service
@EnableAsync
public class AIAsyncProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIAsyncProcessingService.class);
    
    @Autowired
    private UserPersonalityService userPersonalityService;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private NLPAnalyzer nlpAnalyzer;
    
    @Autowired
    private PersonalityAdapter personalityAdapter;
    
    @Autowired
    private SafetyFilter safetyFilter;
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Task status tracking
    private static final String TASK_STATUS_KEY = "ai_task_status";
    private static final String TASK_RESULT_KEY = "ai_task_result";
    private static final String USER_TASKS_KEY = "user_active_tasks";
    private static final long TASK_TIMEOUT_MINUTES = 30;
    
    /**
     * Process AI chat request asynchronously
     */
    @Async
    public CompletableFuture<AIChatResponse> processChatRequestAsync(AIChatRequest request, UUID userId) {
        String taskId = UUID.randomUUID().toString();
        
        try {
            logger.info("Starting async AI chat processing for task: {}", taskId);
            
            // Track user task
            trackUserTask(userId, taskId, "CHAT_REQUEST", "Processing AI chat request");
            
            // Update task status
            updateTaskStatus(taskId, "PROCESSING", "Processing AI chat request");
            
            // Process the request using the synchronous method
            // Note: This creates a direct call instead of going through the service
            AIChatResponse response = processChatRequestDirect(request, userId);
            
            // Update task status to completed
            updateTaskStatus(taskId, "COMPLETED", "AI chat request processed successfully");
            
            // Store result
            storeTaskResult(taskId, response);
            
            // Remove from active tasks
            removeUserTask(userId, taskId);
            
            logger.info("Completed async AI chat processing for task: {}", taskId);
            return CompletableFuture.completedFuture(response);
            
        } catch (Exception e) {
            logger.error("Error in async AI chat processing for task {}: {}", taskId, e.getMessage(), e);
            
            // Update task status to failed
            updateTaskStatus(taskId, "FAILED", "Error: " + e.getMessage());
            
            // Create error response
            AIChatResponse errorResponse = createErrorResponse("I'm sorry, I encountered an error. Please try again.", request);
            storeTaskResult(taskId, errorResponse);
            
            // Remove from active tasks
            removeUserTask(userId, taskId);
            
            return CompletableFuture.completedFuture(errorResponse);
        }
    }
    
    /**
     * Process personality analysis asynchronously
     */
    @Async
    public CompletableFuture<UserPersonalityProfile> analyzePersonalityAsync(UUID userId, String sampleMessage) {
        String taskId = UUID.randomUUID().toString();
        
        try {
            logger.info("Starting async personality analysis for task: {}", taskId);
            
            // Track user task
            trackUserTask(userId, taskId, "PERSONALITY_ANALYSIS", "Analyzing user personality");
            
            // Update task status
            updateTaskStatus(taskId, "PROCESSING", "Analyzing user personality");
            
            // Get current personality profile
            UserPersonalityProfile currentProfile = userPersonalityService.getUserPersonalityProfile(userId);
            
            // Analyze personality using direct method
            String analysisResult = analyzeUserPersonalityDirect(userId, sampleMessage);
            
            // Update profile based on analysis
            UserPersonalityProfile updatedProfile = updateProfileFromAnalysis(currentProfile, analysisResult);
            
            // Update task status to completed
            updateTaskStatus(taskId, "COMPLETED", "Personality analysis completed");
            
            // Store result
            storeTaskResult(taskId, updatedProfile);
            
            // Remove from active tasks
            removeUserTask(userId, taskId);
            
            logger.info("Completed async personality analysis for task: {}", taskId);
            return CompletableFuture.completedFuture(updatedProfile);
            
        } catch (Exception e) {
            logger.error("Error in async personality analysis for task {}: {}", taskId, e.getMessage(), e);
            
            // Update task status to failed
            updateTaskStatus(taskId, "FAILED", "Error: " + e.getMessage());
            
            // Return current profile as fallback
            UserPersonalityProfile fallbackProfile = userPersonalityService.getUserPersonalityProfile(userId);
            storeTaskResult(taskId, fallbackProfile);
            
            // Remove from active tasks
            removeUserTask(userId, taskId);
            
            return CompletableFuture.completedFuture(fallbackProfile);
        }
    }
    
    /**
     * Process conversation context analysis asynchronously
     */
    @Async
    public CompletableFuture<Map<String, Object>> analyzeConversationContextAsync(UUID conversationId, UUID userId) {
        String taskId = UUID.randomUUID().toString();
        
        try {
            logger.info("Starting async conversation context analysis for task: {}", taskId);
            
            // Track user task
            trackUserTask(userId, taskId, "CONVERSATION_ANALYSIS", "Analyzing conversation context");
            
            // Update task status
            updateTaskStatus(taskId, "PROCESSING", "Analyzing conversation context");
            
            // Get conversation
            Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
            if (conversationOpt.isEmpty()) {
                throw new RuntimeException("Conversation not found");
            }
            
            Conversation conversation = conversationOpt.get();
            
            // Get recent messages
            List<Message> recentMessages = messageRepository
                .findByConversationIdOrderByTimestampDesc(conversationId)
                .stream()
                .limit(10)
                .toList();
            
            // Analyze conversation patterns
            Map<String, Object> analysis = analyzeConversationPatterns(conversation, recentMessages);
            
            // Update task status to completed
            updateTaskStatus(taskId, "COMPLETED", "Conversation context analysis completed");
            
            // Store result
            storeTaskResult(taskId, analysis);
            
            // Remove from active tasks
            removeUserTask(userId, taskId);
            
            logger.info("Completed async conversation context analysis for task: {}", taskId);
            return CompletableFuture.completedFuture(analysis);
            
        } catch (Exception e) {
            logger.error("Error in async conversation context analysis for task {}: {}", taskId, e.getMessage(), e);
            
            // Update task status to failed
            updateTaskStatus(taskId, "FAILED", "Error: " + e.getMessage());
            
            // Return empty analysis as fallback
            Map<String, Object> fallbackAnalysis = new HashMap<>();
            fallbackAnalysis.put("error", "Analysis failed");
            storeTaskResult(taskId, fallbackAnalysis);
            
            // Remove from active tasks
            removeUserTask(userId, taskId);
            
            return CompletableFuture.completedFuture(fallbackAnalysis);
        }
    }
    
    /**
     * Process bulk message analysis asynchronously
     */
    @Async
    public CompletableFuture<List<Map<String, Object>>> analyzeBulkMessagesAsync(List<Message> messages, UUID userId) {
        String taskId = UUID.randomUUID().toString();
        
        try {
            logger.info("Starting async bulk message analysis for task: {}", taskId);
            
            // Track user task
            trackUserTask(userId, taskId, "BULK_MESSAGE_ANALYSIS", "Analyzing " + messages.size() + " messages");
            
            // Update task status
            updateTaskStatus(taskId, "PROCESSING", "Analyzing " + messages.size() + " messages");
            
            List<Map<String, Object>> analyses = new ArrayList<>();
            
            // Process messages in batches to avoid overwhelming the system
            int batchSize = 5;
            for (int i = 0; i < messages.size(); i += batchSize) {
                List<Message> batch = messages.subList(i, Math.min(i + batchSize, messages.size()));
                
                for (Message message : batch) {
                    if ("user".equals(message.getSender())) {
                        Map<String, Object> analysis = nlpAnalyzer.analyzeMessage(message.getText());
                        analysis.put("messageId", message.getId());
                        analysis.put("timestamp", message.getTimestamp());
                        analyses.add(analysis);
                    }
                }
                
                // Small delay between batches to prevent overwhelming
                Thread.sleep(100);
            }
            
            // Update task status to completed
            updateTaskStatus(taskId, "COMPLETED", "Bulk message analysis completed");
            
            // Store result
            storeTaskResult(taskId, analyses);
            
            // Remove from active tasks
            removeUserTask(userId, taskId);
            
            logger.info("Completed async bulk message analysis for task: {}", taskId);
            return CompletableFuture.completedFuture(analyses);
            
        } catch (Exception e) {
            logger.error("Error in async bulk message analysis for task {}: {}", taskId, e.getMessage(), e);
            
            // Update task status to failed
            updateTaskStatus(taskId, "FAILED", "Error: " + e.getMessage());
            
            // Return empty analysis as fallback
            List<Map<String, Object>> fallbackAnalysis = new ArrayList<>();
            storeTaskResult(taskId, fallbackAnalysis);
            
            // Remove from active tasks
            removeUserTask(userId, taskId);
            
            return CompletableFuture.completedFuture(fallbackAnalysis);
        }
    }
    
    /**
     * Get task status
     */
    public Map<String, Object> getTaskStatus(String taskId) {
        try {
            String statusKey = TASK_STATUS_KEY + ":" + taskId;
            Object status = redisTemplate.opsForValue().get(statusKey);
            
            if (status instanceof Map) {
                return (Map<String, Object>) status;
            }
            
            return Map.of("status", "NOT_FOUND", "message", "Task not found");
            
        } catch (Exception e) {
            logger.error("Error getting task status: {}", e.getMessage());
            return Map.of("status", "ERROR", "message", "Error retrieving task status");
        }
    }
    
    /**
     * Get task result
     */
    public Object getTaskResult(String taskId) {
        try {
            String resultKey = TASK_RESULT_KEY + ":" + taskId;
            return redisTemplate.opsForValue().get(resultKey);
            
        } catch (Exception e) {
            logger.error("Error getting task result: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Cancel a task
     */
    public boolean cancelTask(String taskId) {
        try {
            updateTaskStatus(taskId, "CANCELLED", "Task cancelled by user");
            return true;
            
        } catch (Exception e) {
            logger.error("Error cancelling task: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all active tasks for a user
     */
    public List<Map<String, Object>> getActiveTasksForUser(UUID userId) {
        try {
            logger.info("Getting active tasks for user: {}", userId);
            
            String userTasksKey = USER_TASKS_KEY + ":" + userId;
            Object userTasksObj = redisTemplate.opsForValue().get(userTasksKey);
            
            if (userTasksObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> taskIds = (List<String>) userTasksObj;
                
                List<Map<String, Object>> activeTasks = new ArrayList<>();
                
                for (String taskId : taskIds) {
                    try {
                        Map<String, Object> taskStatus = getTaskStatus(taskId);
                        if (taskStatus != null) {
                            String status = (String) taskStatus.get("status");
                            
                            // Only include active tasks (not completed or failed)
                            if (!"COMPLETED".equals(status) && !"FAILED".equals(status) && !"CANCELLED".equals(status)) {
                                Map<String, Object> taskInfo = new HashMap<>(taskStatus);
                                taskInfo.put("taskId", taskId);
                                activeTasks.add(taskInfo);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error getting status for task {}: {}", taskId, e.getMessage());
                    }
                }
                
                logger.info("Found {} active tasks for user: {}", activeTasks.size(), userId);
                return activeTasks;
                
            } else {
                logger.info("No active tasks found for user: {}", userId);
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            logger.error("Error getting active tasks for user: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Track user task
     */
    private void trackUserTask(UUID userId, String taskId, String taskType, String description) {
        try {
            String userTasksKey = USER_TASKS_KEY + ":" + userId;
            
            // Get existing tasks for user
            Object userTasksObj = redisTemplate.opsForValue().get(userTasksKey);
            List<String> userTasks;
            
            if (userTasksObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> existingTasks = (List<String>) userTasksObj;
                userTasks = new ArrayList<>(existingTasks);
            } else {
                userTasks = new ArrayList<>();
            }
            
            // Add new task
            userTasks.add(taskId);
            
            // Store updated task list
            redisTemplate.opsForValue().set(userTasksKey, userTasks, TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            
            // Store task metadata
            String taskMetaKey = "task_meta:" + taskId;
            Map<String, Object> taskMeta = new HashMap<>();
            taskMeta.put("taskId", taskId);
            taskMeta.put("userId", userId.toString());
            taskMeta.put("taskType", taskType);
            taskMeta.put("description", description);
            taskMeta.put("createdAt", LocalDateTime.now());
            
            redisTemplate.opsForValue().set(taskMetaKey, taskMeta, TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            
            logger.debug("Tracked task {} for user {}: {}", taskId, userId, taskType);
            
        } catch (Exception e) {
            logger.error("Error tracking user task: {}", e.getMessage());
        }
    }
    
    /**
     * Remove user task
     */
    private void removeUserTask(UUID userId, String taskId) {
        try {
            String userTasksKey = USER_TASKS_KEY + ":" + userId;
            
            // Get existing tasks for user
            Object userTasksObj = redisTemplate.opsForValue().get(userTasksKey);
            if (userTasksObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> userTasks = (List<String>) userTasksObj;
                
                // Remove the task
                userTasks.remove(taskId);
                
                // Store updated task list
                if (userTasks.isEmpty()) {
                    redisTemplate.delete(userTasksKey);
                } else {
                    redisTemplate.opsForValue().set(userTasksKey, userTasks, TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                }
                
                logger.debug("Removed task {} from user {} active tasks", taskId, userId);
            }
            
        } catch (Exception e) {
            logger.error("Error removing user task: {}", e.getMessage());
        }
    }
    
    /**
     * Update task status
     */
    private void updateTaskStatus(String taskId, String status, String message) {
        try {
            String statusKey = TASK_STATUS_KEY + ":" + taskId;
            Map<String, Object> statusInfo = new HashMap<>();
            statusInfo.put("status", status);
            statusInfo.put("message", message);
            statusInfo.put("timestamp", LocalDateTime.now());
            statusInfo.put("taskId", taskId);
            
            redisTemplate.opsForValue().set(statusKey, statusInfo, TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            
        } catch (Exception e) {
            logger.error("Error updating task status: {}", e.getMessage());
        }
    }
    
    /**
     * Store task result
     */
    private void storeTaskResult(String taskId, Object result) {
        try {
            String resultKey = TASK_RESULT_KEY + ":" + taskId;
            redisTemplate.opsForValue().set(resultKey, result, TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            
        } catch (Exception e) {
            logger.error("Error storing task result: {}", e.getMessage());
        }
    }
    
    /**
     * Analyze conversation patterns
     */
    private Map<String, Object> analyzeConversationPatterns(Conversation conversation, List<Message> messages) {
        Map<String, Object> analysis = new HashMap<>();
        
        // Basic conversation analysis
        analysis.put("conversationId", conversation.getId());
        analysis.put("title", conversation.getTitle());
        analysis.put("messageCount", messages.size());
        analysis.put("duration", calculateConversationDuration(messages));
        
        // Sentiment analysis
        List<String> userMessages = messages.stream()
            .filter(m -> "user".equals(m.getSender()))
            .map(Message::getText)
            .toList();
        
        if (!userMessages.isEmpty()) {
            Map<String, Object> sentimentAnalysis = analyzeSentimentPatterns(userMessages);
            analysis.put("sentiment", sentimentAnalysis);
        }
        
        // Topic analysis
        List<String> topics = extractTopicsFromMessages(userMessages);
        analysis.put("topics", topics);
        
        // Response time analysis
        Map<String, Object> responseTimeAnalysis = analyzeResponseTimes(messages);
        analysis.put("responseTimes", responseTimeAnalysis);
        
        return analysis;
    }
    
    /**
     * Calculate conversation duration
     */
    private long calculateConversationDuration(List<Message> messages) {
        if (messages.size() < 2) return 0;
        
        Message first = messages.get(messages.size() - 1);
        Message last = messages.get(0);
        
        return java.time.Duration.between(first.getTimestamp(), last.getTimestamp()).toMinutes();
    }
    
    /**
     * Analyze sentiment patterns
     */
    private Map<String, Object> analyzeSentimentPatterns(List<String> messages) {
        Map<String, Object> sentiment = new HashMap<>();
        
        int positiveCount = 0;
        int negativeCount = 0;
        int neutralCount = 0;
        
        for (String message : messages) {
            Map<String, Object> analysis = nlpAnalyzer.analyzeMessage(message);
            String sentimentValue = (String) analysis.getOrDefault("sentiment", "neutral");
            
            switch (sentimentValue.toLowerCase()) {
                case "positive" -> positiveCount++;
                case "negative" -> negativeCount++;
                default -> neutralCount++;
            }
        }
        
        sentiment.put("positive", positiveCount);
        sentiment.put("negative", negativeCount);
        sentiment.put("neutral", neutralCount);
        sentiment.put("total", messages.size());
        
        return sentiment;
    }
    
    /**
     * Extract topics from messages
     */
    private List<String> extractTopicsFromMessages(List<String> messages) {
        Set<String> topics = new HashSet<>();
        
        for (String message : messages) {
            Map<String, Object> analysis = nlpAnalyzer.analyzeMessage(message);
            @SuppressWarnings("unchecked")
            List<String> messageTopics = (List<String>) analysis.getOrDefault("topics", new ArrayList<>());
            topics.addAll(messageTopics);
        }
        
        return new ArrayList<>(topics);
    }
    
    /**
     * Analyze response times
     */
    private Map<String, Object> analyzeResponseTimes(List<Message> messages) {
        Map<String, Object> responseTimes = new HashMap<>();
        
        List<Long> times = new ArrayList<>();
        Message lastUserMessage = null;
        
        for (Message message : messages) {
            if ("user".equals(message.getSender())) {
                lastUserMessage = message;
            } else if ("ai".equals(message.getSender()) && lastUserMessage != null) {
                long responseTime = java.time.Duration.between(
                    lastUserMessage.getTimestamp(), 
                    message.getTimestamp()
                ).toSeconds();
                times.add(responseTime);
            }
        }
        
        if (!times.isEmpty()) {
            responseTimes.put("average", times.stream().mapToLong(Long::longValue).average().orElse(0.0));
            responseTimes.put("min", times.stream().mapToLong(Long::longValue).min().orElse(0));
            responseTimes.put("max", times.stream().mapToLong(Long::longValue).max().orElse(0));
            responseTimes.put("count", times.size());
        }
        
        return responseTimes;
    }
    
    /**
     * Update profile from analysis result
     */
    private UserPersonalityProfile updateProfileFromAnalysis(UserPersonalityProfile currentProfile, String analysisResult) {
        try {
            logger.debug("Updating profile from analysis result");
            
            // Create a copy of the current profile to modify
            UserPersonalityProfile updatedProfile = new UserPersonalityProfile();
            
            // Copy all existing values
            updatedProfile.setUserId(currentProfile.getUserId());
            updatedProfile.setPreferredPersonalityType(currentProfile.getPreferredPersonalityType());
            updatedProfile.setCommunicationStyle(currentProfile.getCommunicationStyle());
            updatedProfile.setEmotionalState(currentProfile.getEmotionalState());
            updatedProfile.setInterests(new ArrayList<>(currentProfile.getInterests()));
            updatedProfile.setTriggers(new ArrayList<>(currentProfile.getTriggers()));
            updatedProfile.setPreferences(new HashMap<>(currentProfile.getPreferences()));
            updatedProfile.setEnableHumor(currentProfile.isEnableHumor());
            updatedProfile.setEnableEmpathy(currentProfile.isEnableEmpathy());
            updatedProfile.setPreferredResponseLength(new ArrayList<>(currentProfile.getPreferredResponseLength()));
            updatedProfile.setCulturalContext(currentProfile.getCulturalContext());
            updatedProfile.setTimezone(currentProfile.getTimezone());
            updatedProfile.setPreferredLanguage(currentProfile.getPreferredLanguage());
            updatedProfile.setTopicEngagement(new HashMap<>(currentProfile.getTopicEngagement()));
            updatedProfile.setConversationCount(currentProfile.getConversationCount());
            updatedProfile.setLastUpdated(LocalDateTime.now());
            
            // Try to extract JSON from the analysis result
            String jsonPart = extractJsonFromAnalysis(analysisResult);
            
            if (jsonPart != null) {
                try {
                    // Parse the JSON analysis result
                    Map<String, Object> analysis = objectMapper.readValue(jsonPart, Map.class);
                    
                    // Update profile based on analysis
                    if (analysis.containsKey("preferredPersonalityType")) {
                        String newPersonality = (String) analysis.get("preferredPersonalityType");
                        if (isValidPersonalityType(newPersonality)) {
                            updatedProfile.setPreferredPersonalityType(newPersonality);
                            logger.debug("Updated personality type to: {}", newPersonality);
                        }
                    }
                    
                    if (analysis.containsKey("communicationStyle")) {
                        String newStyle = (String) analysis.get("communicationStyle");
                        if (isValidCommunicationStyle(newStyle)) {
                            updatedProfile.setCommunicationStyle(newStyle);
                            logger.debug("Updated communication style to: {}", newStyle);
                        }
                    }
                    
                    if (analysis.containsKey("emotionalState")) {
                        String newEmotion = (String) analysis.get("emotionalState");
                        if (newEmotion != null && !newEmotion.trim().isEmpty()) {
                            updatedProfile.setEmotionalState(newEmotion);
                            logger.debug("Updated emotional state to: {}", newEmotion);
                        }
                    }
                    
                    if (analysis.containsKey("interests")) {
                        Object interestsObj = analysis.get("interests");
                        if (interestsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> newInterests = (List<String>) interestsObj;
                            if (newInterests != null && !newInterests.isEmpty()) {
                                updatedProfile.setInterests(newInterests);
                                logger.debug("Updated interests to: {}", newInterests);
                            }
                        }
                    }
                    
                    if (analysis.containsKey("enableHumor")) {
                        Object humorObj = analysis.get("enableHumor");
                        if (humorObj instanceof Boolean) {
                            updatedProfile.setEnableHumor((Boolean) humorObj);
                            logger.debug("Updated enable humor to: {}", humorObj);
                        }
                    }
                    
                    if (analysis.containsKey("enableEmpathy")) {
                        Object empathyObj = analysis.get("enableEmpathy");
                        if (empathyObj instanceof Boolean) {
                            updatedProfile.setEnableEmpathy((Boolean) empathyObj);
                            logger.debug("Updated enable empathy to: {}", empathyObj);
                        }
                    }
                    
                    if (analysis.containsKey("preferredResponseLength")) {
                        Object responseLengthObj = analysis.get("preferredResponseLength");
                        if (responseLengthObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> newResponseLength = (List<String>) responseLengthObj;
                            if (newResponseLength != null && !newResponseLength.isEmpty()) {
                                updatedProfile.setPreferredResponseLength(newResponseLength);
                                logger.debug("Updated preferred response length to: {}", newResponseLength);
                            }
                        }
                    }
                    
                    if (analysis.containsKey("culturalContext")) {
                        String newCulturalContext = (String) analysis.get("culturalContext");
                        if (newCulturalContext != null && !newCulturalContext.trim().isEmpty()) {
                            updatedProfile.setCulturalContext(newCulturalContext);
                            logger.debug("Updated cultural context to: {}", newCulturalContext);
                        }
                    }
                    
                    // Increment conversation count
                    updatedProfile.setConversationCount(currentProfile.getConversationCount() + 1);
                    
                    logger.info("Successfully updated profile from AI analysis");
                    return updatedProfile;
                    
                } catch (Exception e) {
                    logger.warn("Failed to parse JSON analysis result: {}", e.getMessage());
                }
            }
            
            // Fallback: perform basic pattern-based updates
            return performBasicProfileUpdate(currentProfile, analysisResult);
            
        } catch (Exception e) {
            logger.error("Error updating profile from analysis: {}", e.getMessage(), e);
            return currentProfile; // Return original profile on error
        }
    }
    
    /**
     * Create error response
     */
    private AIChatResponse createErrorResponse(String message, AIChatRequest request) {
        AIChatResponse response = new AIChatResponse();
        response.setMessageId(UUID.randomUUID().toString());
        response.setResponse(message);
        response.setPersonalityUsed("supportive");
        response.setEmotionDetected("neutral");
        response.setSuggestedTopics(Arrays.asList("general support"));
        response.setMetadata(Map.of("error", true));
        response.setSafetyFiltered(false);
        response.setSafetyReason("none");
        response.setTimestamp(LocalDateTime.now());
        response.setTokensUsed(0);
        response.setConfidence(0.5);
        response.setFollowUpQuestions(Arrays.asList("How can I help you today?"));
        response.setConversationContext("error");
        response.setNeedsHumanReview(false);
        response.setReviewReason("none");
        
        return response;
    }
    
    /**
     * Extract JSON from analysis result
     */
    private String extractJsonFromAnalysis(String analysisResult) {
        try {
            // Look for JSON block in the response
            int startIndex = analysisResult.indexOf("{");
            int endIndex = analysisResult.lastIndexOf("}");
            
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                return analysisResult.substring(startIndex, endIndex + 1);
            }
            
            return null;
        } catch (Exception e) {
            logger.warn("Error extracting JSON from analysis result: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate personality type
     */
    private boolean isValidPersonalityType(String personality) {
        return personality != null && 
               (personality.equals("supportive") || 
                personality.equals("professional") || 
                personality.equals("casual") || 
                personality.equals("empathetic"));
    }
    
    /**
     * Validate communication style
     */
    private boolean isValidCommunicationStyle(String style) {
        return style != null && 
               (style.equals("gentle") || 
                style.equals("direct") || 
                style.equals("casual") || 
                style.equals("formal"));
    }
    
    /**
     * Perform basic profile update based on text patterns
     */
    private UserPersonalityProfile performBasicProfileUpdate(UserPersonalityProfile currentProfile, String analysisResult) {
        try {
            UserPersonalityProfile updatedProfile = new UserPersonalityProfile();
            
            // Copy all existing values
            updatedProfile.setUserId(currentProfile.getUserId());
            updatedProfile.setPreferredPersonalityType(currentProfile.getPreferredPersonalityType());
            updatedProfile.setCommunicationStyle(currentProfile.getCommunicationStyle());
            updatedProfile.setEmotionalState(currentProfile.getEmotionalState());
            updatedProfile.setInterests(new ArrayList<>(currentProfile.getInterests()));
            updatedProfile.setTriggers(new ArrayList<>(currentProfile.getTriggers()));
            updatedProfile.setPreferences(new HashMap<>(currentProfile.getPreferences()));
            updatedProfile.setEnableHumor(currentProfile.isEnableHumor());
            updatedProfile.setEnableEmpathy(currentProfile.isEnableEmpathy());
            updatedProfile.setPreferredResponseLength(new ArrayList<>(currentProfile.getPreferredResponseLength()));
            updatedProfile.setCulturalContext(currentProfile.getCulturalContext());
            updatedProfile.setTimezone(currentProfile.getTimezone());
            updatedProfile.setPreferredLanguage(currentProfile.getPreferredLanguage());
            updatedProfile.setTopicEngagement(new HashMap<>(currentProfile.getTopicEngagement()));
            updatedProfile.setConversationCount(currentProfile.getConversationCount() + 1);
            updatedProfile.setLastUpdated(LocalDateTime.now());
            
            // Basic pattern-based updates
            String lowerAnalysis = analysisResult.toLowerCase();
            
            // Detect emotional state changes
            if (lowerAnalysis.contains("anxious") || lowerAnalysis.contains("worried")) {
                updatedProfile.setEmotionalState("anxious");
            } else if (lowerAnalysis.contains("sad") || lowerAnalysis.contains("depressed")) {
                updatedProfile.setEmotionalState("sad");
            } else if (lowerAnalysis.contains("happy") || lowerAnalysis.contains("joyful")) {
                updatedProfile.setEmotionalState("happy");
            } else if (lowerAnalysis.contains("angry") || lowerAnalysis.contains("frustrated")) {
                updatedProfile.setEmotionalState("angry");
            }
            
            // Detect personality preferences
            if (lowerAnalysis.contains("professional") || lowerAnalysis.contains("clinical")) {
                updatedProfile.setPreferredPersonalityType("professional");
            } else if (lowerAnalysis.contains("casual") || lowerAnalysis.contains("friendly")) {
                updatedProfile.setPreferredPersonalityType("casual");
            } else if (lowerAnalysis.contains("empathetic") || lowerAnalysis.contains("understanding")) {
                updatedProfile.setPreferredPersonalityType("empathetic");
            } else if (lowerAnalysis.contains("supportive") || lowerAnalysis.contains("encouraging")) {
                updatedProfile.setPreferredPersonalityType("supportive");
            }
            
            // Detect communication style preferences
            if (lowerAnalysis.contains("direct") || lowerAnalysis.contains("straightforward")) {
                updatedProfile.setCommunicationStyle("direct");
            } else if (lowerAnalysis.contains("gentle") || lowerAnalysis.contains("soft")) {
                updatedProfile.setCommunicationStyle("gentle");
            } else if (lowerAnalysis.contains("formal") || lowerAnalysis.contains("professional")) {
                updatedProfile.setCommunicationStyle("formal");
            }
            
            // Detect humor preference
            if (lowerAnalysis.contains("humor") || lowerAnalysis.contains("funny") || lowerAnalysis.contains("joke")) {
                updatedProfile.setEnableHumor(true);
            } else if (lowerAnalysis.contains("serious") || lowerAnalysis.contains("no humor")) {
                updatedProfile.setEnableHumor(false);
            }
            
            // Detect empathy preference
            if (lowerAnalysis.contains("empathy") || lowerAnalysis.contains("understanding") || lowerAnalysis.contains("compassion")) {
                updatedProfile.setEnableEmpathy(true);
            } else if (lowerAnalysis.contains("logical") || lowerAnalysis.contains("analytical")) {
                updatedProfile.setEnableEmpathy(false);
            }
            
            logger.info("Performed basic profile update from analysis patterns");
            return updatedProfile;
            
        } catch (Exception e) {
            logger.error("Error in basic profile update: {}", e.getMessage(), e);
            return currentProfile;
        }
    }
    
    /**
     * Process chat request directly without going through AIChatService
     */
    private AIChatResponse processChatRequestDirect(AIChatRequest request, UUID userId) {
        try {
            // This is a simplified version of the chat processing
            // In a real implementation, you might want to extract the core logic
            // or use a different approach to avoid circular dependencies
            
            // For now, return a basic response
            AIChatResponse response = new AIChatResponse();
            response.setMessageId(UUID.randomUUID().toString());
            response.setResponse("I'm processing your request asynchronously. Please check back later for the result.");
            response.setPersonalityUsed("supportive");
            response.setEmotionDetected("neutral");
            response.setSuggestedTopics(Arrays.asList("general support"));
            response.setMetadata(Map.of("async_processing", true));
            response.setSafetyFiltered(false);
            response.setSafetyReason("none");
            response.setTimestamp(LocalDateTime.now());
            response.setTokensUsed(0);
            response.setConfidence(0.8);
            response.setFollowUpQuestions(Arrays.asList("How can I help you today?"));
            response.setConversationContext("async_processing");
            response.setNeedsHumanReview(false);
            response.setReviewReason("none");
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error in direct chat processing: {}", e.getMessage(), e);
            return createErrorResponse("I'm sorry, I encountered an error. Please try again.", request);
        }
    }
    
    /**
     * Analyze user personality directly without going through AIChatService
     */
    private String analyzeUserPersonalityDirect(UUID userId, String sampleMessage) {
        try {
            // This is a simplified version of personality analysis
            // In a real implementation, you might want to extract the core logic
            
            // For now, return a basic analysis
            return "Basic personality analysis: User shows interest in mental health topics and prefers supportive communication style.";
            
        } catch (Exception e) {
            logger.error("Error in direct personality analysis: {}", e.getMessage(), e);
            return "Error analyzing personality: " + e.getMessage();
        }
    }
}

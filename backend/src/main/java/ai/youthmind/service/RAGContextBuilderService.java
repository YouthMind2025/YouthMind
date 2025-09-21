package ai.youthmind.service;

import ai.youthmind.entity.MemoryItem;
import ai.youthmind.entity.User;
import ai.youthmind.entity.UserPreferences;
import ai.youthmind.entity.Post;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.repository.UserPreferencesRepository;
import ai.youthmind.repository.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RAGContextBuilderService {
    
    private static final Logger logger = LoggerFactory.getLogger(RAGContextBuilderService.class);
    
    // Context size limits
    private static final int MAX_CONTEXT_SIZE = 2048; // 2KB limit
    private static final int MAX_FACTS = 15;
    private static final int MAX_PERSONALITY_TRAITS = 5;
    private static final int MAX_RECENT_INTERACTIONS = 8;
    
    @Autowired
    private MemoryService memoryService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserPreferencesRepository userPreferencesRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Build comprehensive RAG context for personalized AI responses
     */
    public String buildRAGContext(UUID userId, String currentMessage, String conversationHistory) {
        try {
            StringBuilder contextBuilder = new StringBuilder();
            int currentSize = 0;
            
            // 1. User Profile Context (Priority 1)
            String profileContext = buildProfileContext(userId);
            if (profileContext != null && currentSize + profileContext.length() < MAX_CONTEXT_SIZE) {
                contextBuilder.append(profileContext).append("\n\n");
                currentSize += profileContext.length() + 2;
            }
            
            // 2. Personality Traits (Priority 2)
            String personalityContext = buildPersonalityContext(userId);
            if (personalityContext != null && currentSize + personalityContext.length() < MAX_CONTEXT_SIZE) {
                contextBuilder.append(personalityContext).append("\n\n");
                currentSize += personalityContext.length() + 2;
            }
            
            // 3. User Preferences & Settings (Priority 3)
            String preferencesContext = buildPreferencesContext(userId);
            if (preferencesContext != null && currentSize + preferencesContext.length() < MAX_CONTEXT_SIZE) {
                contextBuilder.append(preferencesContext).append("\n\n");
                currentSize += preferencesContext.length() + 2;
            }
            
            // 4. Recent Relevant Memories (Priority 4)
            String memoryContext = buildMemoryContext(userId, currentMessage);
            if (memoryContext != null && currentSize + memoryContext.length() < MAX_CONTEXT_SIZE) {
                contextBuilder.append(memoryContext).append("\n\n");
                currentSize += memoryContext.length() + 2;
            }
            
            // 5. Recent Emotional Patterns (Priority 5)
            String emotionalContext = buildEmotionalContext(userId);
            if (emotionalContext != null && currentSize + emotionalContext.length() < MAX_CONTEXT_SIZE) {
                contextBuilder.append(emotionalContext).append("\n\n");
                currentSize += emotionalContext.length() + 2;
            }
            
            // 6. Conversation History Summary (Priority 6)
            String historyContext = compressConversationHistory(conversationHistory, 
                                                               MAX_CONTEXT_SIZE - currentSize);
            if (historyContext != null && !historyContext.isEmpty()) {
                contextBuilder.append("RECENT_CONVERSATION:\n").append(historyContext);
            }
            
            String finalContext = contextBuilder.toString().trim();
            
            // Log context size for monitoring
            logger.debug("Built RAG context for user {}: {} characters", userId, finalContext.length());
            
            return finalContext;
            
        } catch (Exception e) {
            logger.error("Failed to build RAG context for user {}: {}", userId, e.getMessage());
            return buildFallbackContext(userId);
        }
    }
    
    /**
     * Build user profile context
     */
    private String buildProfileContext(UUID userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) return null;
            
            User user = userOpt.get();
            StringBuilder profile = new StringBuilder("USER_PROFILE:\n");
            
            profile.append("Name: ").append(user.getFirstName());
            if (user.getLastName() != null) {
                profile.append(" ").append(user.getLastName());
            }
            profile.append("\n");
            
            if (user.getBio() != null && !user.getBio().trim().isEmpty()) {
                profile.append("Bio: ").append(user.getBio().substring(0, Math.min(200, user.getBio().length())));
                if (user.getBio().length() > 200) profile.append("...");
                profile.append("\n");
            }
            
            profile.append("Member since: ").append(user.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM yyyy")));
            profile.append("\nPosts: ").append(user.getPostsCount());
            profile.append(", Engagement: ").append(user.getLikesCount());
            
            return profile.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to build profile context for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Build personality traits context from memory
     */
    private String buildPersonalityContext(UUID userId) {
        try {
            List<MemoryItem> personalityMemories = memoryService.retrieveRelevantMemories(
                userId, "personality", MAX_PERSONALITY_TRAITS);
            
            if (personalityMemories.isEmpty()) return null;
            
            StringBuilder personality = new StringBuilder("PERSONALITY_TRAITS:\n");
            
            for (MemoryItem memory : personalityMemories) {
                if (memory.getMemoryType() == MemoryItem.MemoryType.PERSONALITY_TRAIT) {
                    String content = memory.getCompressedContent() != null ? 
                        memory.getCompressedContent() : memory.getContent();
                    personality.append("- ").append(content).append("\n");
                }
            }
            
            return personality.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to build personality context for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Build user preferences context
     */
    private String buildPreferencesContext(UUID userId) {
        try {
            Optional<UserPreferences> prefsOpt = userPreferencesRepository.findByUserId(userId);
            if (prefsOpt.isEmpty()) return null;
            
            UserPreferences prefs = prefsOpt.get();
            StringBuilder preferences = new StringBuilder("USER_PREFERENCES:\n");
            
            preferences.append("Communication style: ");
            if (prefs.getAiSuggestionsEnabled()) {
                preferences.append("Open to AI suggestions, ");
            }
            if (prefs.getAnonymousPostingAllowed()) {
                preferences.append("Values privacy, ");
            }
            preferences.append("Prefers ").append(prefs.getLanguage()).append(" language\n");
            
            preferences.append("Interaction preferences: ");
            if (prefs.getNotificationsEnabled()) {
                preferences.append("Likes notifications, ");
            }
            preferences.append("Privacy level: ").append(prefs.getPrivacyLevel());
            
            return preferences.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to build preferences context for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Build memory context based on current message
     */
    private String buildMemoryContext(UUID userId, String currentMessage) {
        try {
            List<MemoryItem> relevantMemories = memoryService.retrieveRelevantMemories(
                userId, currentMessage, MAX_RECENT_INTERACTIONS);
            
            if (relevantMemories.isEmpty()) return null;
            
            StringBuilder memory = new StringBuilder("RELEVANT_MEMORIES:\n");
            
            for (MemoryItem item : relevantMemories) {
                String content = item.getCompressedContent() != null ? 
                    item.getCompressedContent() : item.getContent();
                
                // Limit each memory to 150 characters
                if (content.length() > 150) {
                    content = content.substring(0, 147) + "...";
                }
                
                memory.append("- [").append(item.getMemoryType()).append("] ")
                      .append(content).append("\n");
            }
            
            return memory.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to build memory context for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Build emotional context from recent posts
     */
    private String buildEmotionalContext(UUID userId) {
        try {
            List<Post> recentPosts = postRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(5)
                .collect(Collectors.toList());
            
            if (recentPosts.isEmpty()) return null;
            
            Map<String, Integer> emotionCounts = new HashMap<>();
            for (Post post : recentPosts) {
                if (post.getEmotion() != null) {
                    emotionCounts.merge(post.getEmotion(), 1, Integer::sum);
                }
            }
            
            if (emotionCounts.isEmpty()) return null;
            
            StringBuilder emotional = new StringBuilder("EMOTIONAL_PATTERNS:\n");
            emotional.append("Recent emotions: ");
            
            emotionCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> emotional.append(entry.getKey()).append("(").append(entry.getValue()).append("), "));
            
            // Remove trailing comma
            if (emotional.length() > 2) {
                emotional.setLength(emotional.length() - 2);
            }
            
            return emotional.toString();
            
        } catch (Exception e) {
            logger.warn("Failed to build emotional context for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Compress conversation history to fit remaining space
     */
    private String compressConversationHistory(String history, int maxSize) {
        if (history == null || history.isEmpty() || maxSize < 50) {
            return null;
        }
        
        if (history.length() <= maxSize) {
            return history;
        }
        
        // Extract key parts of conversation
        String[] lines = history.split("\n");
        StringBuilder compressed = new StringBuilder();
        
        // Keep first few and last few exchanges
        int linesToKeep = Math.min(6, lines.length);
        int halfLines = linesToKeep / 2;
        
        // Add first exchanges
        for (int i = 0; i < halfLines && i < lines.length; i++) {
            compressed.append(lines[i]).append("\n");
        }
        
        // Add ellipsis if we're skipping content
        if (lines.length > linesToKeep) {
            compressed.append("...[").append(lines.length - linesToKeep).append(" exchanges]...\n");
        }
        
        // Add last exchanges
        int startIdx = Math.max(halfLines, lines.length - halfLines);
        for (int i = startIdx; i < lines.length; i++) {
            compressed.append(lines[i]).append("\n");
        }
        
        String result = compressed.toString().trim();
        
        // If still too long, truncate
        if (result.length() > maxSize) {
            result = result.substring(0, maxSize - 3) + "...";
        }
        
        return result;
    }
    
    /**
     * Build fallback context when main context building fails
     */
    private String buildFallbackContext(UUID userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return String.format("USER: %s %s (Member since %s)", 
                    user.getFirstName(), 
                    user.getLastName() != null ? user.getLastName() : "",
                    user.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM yyyy")));
            }
        } catch (Exception e) {
            logger.error("Failed to build fallback context: {}", e.getMessage());
        }
        
        return "USER: Anonymous user";
    }
    
    /**
     * Process preference signals from user interaction
     */
    public void processPreferenceSignals(UUID userId, String messageContent, String aiResponse, 
                                       Map<String, Object> interactionMetadata) {
        try {
            // Extract preference signals from message content
            List<String> preferenceSignals = extractPreferenceSignals(messageContent);
            
            // Store each signal as a memory
            for (String signal : preferenceSignals) {
                Map<String, Object> context = new HashMap<>(interactionMetadata);
                context.put("signal_type", "preference");
                context.put("confidence", calculateSignalConfidence(signal, messageContent));
                
                memoryService.storeMemory(userId, MemoryItem.MemoryType.USER_PREFERENCE, 
                                        "pref_" + signal.toLowerCase().replaceAll("\\s+", "_"), 
                                        signal, context);
            }
            
            // Analyze AI response effectiveness
            analyzeResponseEffectiveness(userId, aiResponse, interactionMetadata);
            
        } catch (Exception e) {
            logger.error("Failed to process preference signals for user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Extract preference signals from user message
     */
    private List<String> extractPreferenceSignals(String messageContent) {
        List<String> signals = new ArrayList<>();
        String lowerContent = messageContent.toLowerCase();
        
        // Communication style preferences
        if (lowerContent.contains("i prefer") || lowerContent.contains("i like when")) {
            signals.add("Direct communication preference detected");
        }
        if (lowerContent.contains("too formal") || lowerContent.contains("too casual")) {
            signals.add("Formality preference indicated");
        }
        
        // Support style preferences
        if (lowerContent.contains("just listen") || lowerContent.contains("don't give advice")) {
            signals.add("Prefers empathetic listening over advice");
        }
        if (lowerContent.contains("what should i do") || lowerContent.contains("need advice")) {
            signals.add("Prefers actionable advice and guidance");
        }
        
        // Interaction preferences
        if (lowerContent.contains("short") || lowerContent.contains("brief")) {
            signals.add("Prefers concise responses");
        }
        if (lowerContent.contains("detailed") || lowerContent.contains("explain more")) {
            signals.add("Prefers detailed explanations");
        }
        
        // Emotional support preferences
        if (lowerContent.contains("validate") || lowerContent.contains("understand")) {
            signals.add("Values emotional validation");
        }
        if (lowerContent.contains("solution") || lowerContent.contains("fix")) {
            signals.add("Prefers solution-focused support");
        }
        
        return signals;
    }
    
    /**
     * Calculate confidence score for a preference signal
     */
    private double calculateSignalConfidence(String signal, String messageContent) {
        // Base confidence
        double confidence = 0.5;
        
        // Increase confidence for explicit statements
        if (messageContent.toLowerCase().contains("i prefer") || 
            messageContent.toLowerCase().contains("i like") ||
            messageContent.toLowerCase().contains("please")) {
            confidence += 0.3;
        }
        
        // Increase confidence for repeated patterns
        if (messageContent.toLowerCase().contains("always") || 
            messageContent.toLowerCase().contains("usually") ||
            messageContent.toLowerCase().contains("typically")) {
            confidence += 0.2;
        }
        
        return Math.min(1.0, confidence);
    }
    
    /**
     * Analyze AI response effectiveness for future improvement
     */
    private void analyzeResponseEffectiveness(UUID userId, String aiResponse, 
                                            Map<String, Object> metadata) {
        try {
            // Store response pattern for learning
            String key = "response_pattern_" + System.currentTimeMillis();
            String content = String.format("AI Response: %s\nMetadata: %s", 
                                         aiResponse.substring(0, Math.min(500, aiResponse.length())),
                                         metadata.toString());
            
            Map<String, Object> context = new HashMap<>(metadata);
            context.put("response_length", aiResponse.length());
            context.put("analysis_timestamp", LocalDateTime.now().toString());
            
            memoryService.storeMemory(userId, MemoryItem.MemoryType.INTERACTION_HISTORY, 
                                    key, content, context);
            
        } catch (Exception e) {
            logger.warn("Failed to analyze response effectiveness: {}", e.getMessage());
        }
    }
    
    /**
     * Build context with size compression to stay under 2KB
     */
    public String buildCompressedContext(UUID userId, String currentMessage) {
        String fullContext = buildRAGContext(userId, currentMessage, "");
        
        if (fullContext.length() <= MAX_CONTEXT_SIZE) {
            return fullContext;
        }
        
        // Compress by removing less important sections
        String[] sections = fullContext.split("\n\n");
        StringBuilder compressed = new StringBuilder();
        int currentSize = 0;
        
        // Priority order: Profile, Personality, Preferences, Memory, Emotional
        String[] prioritySections = {"USER_PROFILE:", "PERSONALITY_TRAITS:", 
                                    "USER_PREFERENCES:", "RELEVANT_MEMORIES:", "EMOTIONAL_PATTERNS:"};
        
        for (String priority : prioritySections) {
            for (String section : sections) {
                if (section.startsWith(priority) && 
                    currentSize + section.length() < MAX_CONTEXT_SIZE) {
                    compressed.append(section).append("\n\n");
                    currentSize += section.length() + 2;
                    break;
                }
            }
        }
        
        return compressed.toString().trim();
    }
    
    /**
     * Get context facts as structured data for AI processing
     */
    public Map<String, Object> getContextFacts(UUID userId, String currentMessage) {
        Map<String, Object> facts = new HashMap<>();
        
        try {
            // User basic info
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                facts.put("userName", user.getFirstName());
                facts.put("memberSince", user.getCreatedAt());
                facts.put("postsCount", user.getPostsCount());
                facts.put("engagementLevel", calculateEngagementLevel(user));
            }
            
            // Recent memories
            List<MemoryItem> memories = memoryService.retrieveRelevantMemories(userId, currentMessage, 5);
            List<Map<String, Object>> memoryFacts = memories.stream()
                .map(this::memoryToFact)
                .collect(Collectors.toList());
            facts.put("relevantMemories", memoryFacts);
            
            // User preferences
            Optional<UserPreferences> prefsOpt = userPreferencesRepository.findByUserId(userId);
            if (prefsOpt.isPresent()) {
                UserPreferences prefs = prefsOpt.get();
                facts.put("communicationPreferences", Map.of(
                    "language", prefs.getLanguage(),
                    "aiSuggestionsEnabled", prefs.getAiSuggestionsEnabled(),
                    "privacyLevel", prefs.getPrivacyLevel()
                ));
            }
            
            // Emotional context
            facts.put("recentEmotions", getRecentEmotionalState(userId));
            
        } catch (Exception e) {
            logger.error("Failed to get context facts for user {}: {}", userId, e.getMessage());
        }
        
        return facts;
    }
    
    private String calculateEngagementLevel(User user) {
        int totalEngagement = user.getPostsCount() + user.getLikesCount() + user.getBookmarksCount();
        
        if (totalEngagement > 100) return "HIGH";
        if (totalEngagement > 20) return "MEDIUM";
        return "LOW";
    }
    
    private Map<String, Object> memoryToFact(MemoryItem memory) {
        Map<String, Object> fact = new HashMap<>();
        fact.put("type", memory.getMemoryType());
        fact.put("content", memory.getCompressedContent() != null ? 
                          memory.getCompressedContent() : memory.getContent());
        fact.put("weight", memory.getWeightScore());
        fact.put("age", ChronoUnit.DAYS.between(memory.getCreatedAt(), LocalDateTime.now()));
        return fact;
    }
    
    private List<String> getRecentEmotionalState(UUID userId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .limit(3)
            .map(Post::getEmotion)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}

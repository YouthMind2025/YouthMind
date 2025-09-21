package ai.youthmind.service;

import ai.youthmind.entity.MemoryItem;
import ai.youthmind.repository.MemoryItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@EnableAsync
public class BackgroundProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BackgroundProcessingService.class);
    
    @Autowired
    private MemoryService memoryService;
    
    @Autowired
    private MemoryItemRepository memoryRepository;
    
    @Autowired
    private PersonalizedAIChatService personalizedChatService;
    
    @Autowired
    private RedisService redisService;
    
    /**
     * Background "better answer" update system
     */
    @Async
    public CompletableFuture<Void> improvePreviousResponse(UUID userId, String originalMessage, 
                                                          String originalResponse, String feedback) {
        try {
            logger.info("Starting background response improvement for user: {}", userId);
            
            // Analyze what went wrong with original response
            String improvementContext = analyzeResponseIssues(originalMessage, originalResponse, feedback);
            
            // Generate improved response
            String improvedResponse = generateImprovedResponse(userId, originalMessage, 
                                                             originalResponse, improvementContext);
            
            // Store improved response pattern in memory
            storeImprovedResponsePattern(userId, originalMessage, originalResponse, 
                                       improvedResponse, improvementContext);
            
            // Update user's preference signals
            updatePreferenceSignals(userId, feedback, improvementContext);
            
            logger.info("Background response improvement completed for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Background response improvement failed for user {}: {}", userId, e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Scheduled memory weight updates (every hour)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void updateMemoryWeights() {
        try {
            logger.info("Starting scheduled memory weight updates");
            
            // Get all users with active memories
            List<UUID> usersWithMemories = memoryRepository.findAll().stream()
                .filter(memory -> memory.getArchivedAt() == null)
                .map(MemoryItem::getUserId)
                .distinct()
                .toList();
            
            int updatedUsers = 0;
            for (UUID userId : usersWithMemories) {
                try {
                    memoryService.updateMemoryWeights(userId);
                    updatedUsers++;
                } catch (Exception e) {
                    logger.warn("Failed to update memory weights for user {}: {}", userId, e.getMessage());
                }
            }
            
            logger.info("Memory weight updates completed for {} users", updatedUsers);
            
        } catch (Exception e) {
            logger.error("Scheduled memory weight update failed: {}", e.getMessage());
        }
    }
    
    /**
     * Scheduled memory cleanup (daily at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void performMemoryCleanup() {
        try {
            logger.info("Starting scheduled memory cleanup");
            
            // Get all users
            List<UUID> allUsers = memoryRepository.findAll().stream()
                .map(MemoryItem::getUserId)
                .distinct()
                .toList();
            
            int cleanedUsers = 0;
            int totalArchivedMemories = 0;
            
            for (UUID userId : allUsers) {
                try {
                    memoryService.performMemoryCleanup(userId);
                    cleanedUsers++;
                } catch (Exception e) {
                    logger.warn("Memory cleanup failed for user {}: {}", userId, e.getMessage());
                }
            }
            
            // Archive very old memories globally
            int archivedCount = memoryRepository.archiveExpiredMemories(LocalDateTime.now());
            totalArchivedMemories += archivedCount;
            
            logger.info("Memory cleanup completed: {} users processed, {} memories archived", 
                       cleanedUsers, totalArchivedMemories);
            
        } catch (Exception e) {
            logger.error("Scheduled memory cleanup failed: {}", e.getMessage());
        }
    }
    
    /**
     * Background cache warming (every 30 minutes)
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void warmCaches() {
        try {
            logger.info("Starting cache warming");
            
            // Warm memory caches for active users
            List<UUID> activeUsers = getActiveUsers();
            
            for (UUID userId : activeUsers) {
                try {
                    // Pre-load important memories into cache
                    List<MemoryItem> importantMemories = memoryRepository.findByImportanceLevel(
                        userId, MemoryItem.ImportanceLevel.CRITICAL);
                    importantMemories.addAll(memoryRepository.findByImportanceLevel(
                        userId, MemoryItem.ImportanceLevel.HIGH));
                    
                    // Cache each important memory
                    for (MemoryItem memory : importantMemories) {
                        String cacheKey = "memory:" + userId + ":" + memory.getKeyIdentifier();
                        redisService.setValue(cacheKey, memory, 1, java.util.concurrent.TimeUnit.HOURS);
                    }
                    
                } catch (Exception e) {
                    logger.warn("Cache warming failed for user {}: {}", userId, e.getMessage());
                }
            }
            
            logger.info("Cache warming completed for {} users", activeUsers.size());
            
        } catch (Exception e) {
            logger.error("Cache warming failed: {}", e.getMessage());
        }
    }
    
    /**
     * Background response quality analysis
     */
    @Async
    public CompletableFuture<Void> analyzeResponseQuality(UUID userId, String userMessage, 
                                                         String aiResponse, Double userRating) {
        try {
            // Store quality feedback in memory
            String key = "quality_feedback_" + System.currentTimeMillis();
            String content = String.format("Message: %s\nResponse: %s\nRating: %.2f", 
                                         userMessage.substring(0, Math.min(100, userMessage.length())),
                                         aiResponse.substring(0, Math.min(100, aiResponse.length())),
                                         userRating);
            
            Map<String, Object> context = Map.of(
                "rating", userRating,
                "response_length", aiResponse.length(),
                "message_length", userMessage.length(),
                "analysis_type", "quality_feedback"
            );
            
            memoryService.storeMemory(userId, MemoryItem.MemoryType.INTERACTION_HISTORY, 
                                    key, content, context);
            
            // Update engagement weights based on rating
            if (userRating > 4.0) {
                memoryService.updateEngagementWeight(userId, key, "POSITIVE_FEEDBACK");
            } else if (userRating < 2.0) {
                memoryService.updateEngagementWeight(userId, key, "NEGATIVE_FEEDBACK");
            }
            
        } catch (Exception e) {
            logger.error("Response quality analysis failed: {}", e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    // ==================== UTILITY METHODS ====================
    
    private String analyzeResponseIssues(String originalMessage, String originalResponse, String feedback) {
        StringBuilder analysis = new StringBuilder("RESPONSE_IMPROVEMENT_ANALYSIS:\n");
        
        analysis.append("Original message length: ").append(originalMessage.length()).append("\n");
        analysis.append("Original response length: ").append(originalResponse.length()).append("\n");
        analysis.append("User feedback: ").append(feedback).append("\n");
        
        // Analyze common issues
        if (feedback.toLowerCase().contains("too long")) {
            analysis.append("Issue: Response too lengthy\n");
        }
        if (feedback.toLowerCase().contains("not helpful")) {
            analysis.append("Issue: Response not actionable enough\n");
        }
        if (feedback.toLowerCase().contains("generic")) {
            analysis.append("Issue: Response lacks personalization\n");
        }
        
        return analysis.toString();
    }
    
    private String generateImprovedResponse(UUID userId, String originalMessage, 
                                          String originalResponse, String improvementContext) {
        // This would call the AI service with improvement context
        // For now, return a placeholder
        return "Improved response based on feedback: " + improvementContext;
    }
    
    private void storeImprovedResponsePattern(UUID userId, String originalMessage, 
                                            String originalResponse, String improvedResponse, 
                                            String improvementContext) {
        String key = "improved_pattern_" + System.currentTimeMillis();
        String content = String.format("Original: %s\nImproved: %s\nContext: %s", 
                                      originalResponse, improvedResponse, improvementContext);
        
        Map<String, Object> context = Map.of(
            "improvement_type", "background_enhancement",
            "original_length", originalResponse.length(),
            "improved_length", improvedResponse.length()
        );
        
        memoryService.storeMemory(userId, MemoryItem.MemoryType.INTERACTION_HISTORY, 
                                key, content, context);
    }
    
    private void updatePreferenceSignals(UUID userId, String feedback, String improvementContext) {
        // Extract preference signals from feedback
        if (feedback.toLowerCase().contains("shorter")) {
            String key = "pref_response_length";
            String content = "User prefers shorter responses";
            Map<String, Object> context = Map.of("preference_type", "response_length", "value", "short");
            
            memoryService.storeMemory(userId, MemoryItem.MemoryType.USER_PREFERENCE, 
                                    key, content, context);
        }
        
        if (feedback.toLowerCase().contains("more detailed")) {
            String key = "pref_response_detail";
            String content = "User prefers more detailed responses";
            Map<String, Object> context = Map.of("preference_type", "response_detail", "value", "detailed");
            
            memoryService.storeMemory(userId, MemoryItem.MemoryType.USER_PREFERENCE, 
                                    key, content, context);
        }
    }
    
    private List<UUID> getActiveUsers() {
        // Get users who have been active in the last 24 hours
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        
        return memoryRepository.findRecentMemories(null, yesterday).stream()
            .map(MemoryItem::getUserId)
            .distinct()
            .limit(100) // Limit to top 100 active users
            .toList();
    }
}

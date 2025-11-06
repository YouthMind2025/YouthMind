package ai.youthmind.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import ai.youthmind.entity.Conversation;
import ai.youthmind.entity.Message;
import ai.youthmind.entity.User;
import ai.youthmind.repository.ConversationRepository;
import ai.youthmind.repository.MessageRepository;
import ai.youthmind.repository.UserRepository;

/**
 * Service for optimizing database queries related to AI features
 */
@Service
public class AIDatabaseOptimizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIDatabaseOptimizationService.class);
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Cache configuration
    private static final String CONVERSATION_CACHE_KEY = "conversation_context";
    private static final String MESSAGE_CACHE_KEY = "recent_messages";
    private static final String USER_PROFILE_CACHE_KEY = "user_profile";
    private static final long CACHE_TTL_MINUTES = 30;
    
    /**
     * Get optimized conversation context for AI processing
     */
    public String getOptimizedConversationContext(UUID conversationId, UUID userId) {
        String cacheKey = CONVERSATION_CACHE_KEY + ":" + conversationId + ":" + userId;
        
        try {
            // Try cache first
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                logger.debug("Cache hit for conversation context: {}", cacheKey);
                return (String) cached;
            }
        } catch (Exception e) {
            logger.warn("Failed to get conversation context from cache: {}", e.getMessage());
        }
        
        try {
            // Get conversation with optimized query
            Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
            if (conversationOpt.isEmpty()) {
                return "New conversation";
            }
            
            Conversation conversation = conversationOpt.get();
            if (!conversation.getUserId().equals(userId)) {
                return "New conversation";
            }
            
            // Get recent messages with pagination and sorting
            Pageable pageable = PageRequest.of(0, 5, Sort.by("timestamp").descending());
            Page<Message> messagePage = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId, pageable);
            
            List<Message> recentMessages = messagePage.getContent();
            
            // Build context string
            StringBuilder context = new StringBuilder();
            context.append("Conversation: ").append(conversation.getTitle()).append("\n");
            
            // Reverse to get chronological order
            Collections.reverse(recentMessages);
            for (Message msg : recentMessages) {
                context.append(msg.getSender()).append(": ").append(msg.getText()).append("\n");
            }
            
            String contextString = context.toString();
            
            // Cache the result
            try {
                redisTemplate.opsForValue().set(cacheKey, contextString, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                logger.debug("Cached conversation context: {}", cacheKey);
            } catch (Exception e) {
                logger.warn("Failed to cache conversation context: {}", e.getMessage());
            }
            
            return contextString;
            
        } catch (Exception e) {
            logger.error("Error getting optimized conversation context: {}", e.getMessage(), e);
            return "New conversation";
        }
    }
    
    /**
     * Get optimized recent messages for AI processing
     */
    public List<Message> getOptimizedRecentMessages(UUID conversationId, int limit) {
        String cacheKey = MESSAGE_CACHE_KEY + ":" + conversationId + ":" + limit;
        
        try {
            // Try cache first
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                @SuppressWarnings("unchecked")
                List<Message> cachedMessages = (List<Message>) cached;
                logger.debug("Cache hit for recent messages: {}", cacheKey);
                return cachedMessages;
            }
        } catch (Exception e) {
            logger.warn("Failed to get recent messages from cache: {}", e.getMessage());
        }
        
        try {
            // Use pagination for better performance
            Pageable pageable = PageRequest.of(0, limit, Sort.by("timestamp").descending());
            Page<Message> messagePage = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId, pageable);
            
            List<Message> messages = messagePage.getContent();
            
            // Cache the result
            try {
                redisTemplate.opsForValue().set(cacheKey, messages, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                logger.debug("Cached recent messages: {}", cacheKey);
            } catch (Exception e) {
                logger.warn("Failed to cache recent messages: {}", e.getMessage());
            }
            
            return messages;
            
        } catch (Exception e) {
            logger.error("Error getting optimized recent messages: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get optimized user profile for AI processing
     */
    public User getOptimizedUserProfile(UUID userId) {
        String cacheKey = USER_PROFILE_CACHE_KEY + ":" + userId;
        
        try {
            // Try cache first
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof User) {
                logger.debug("Cache hit for user profile: {}", cacheKey);
                return (User) cached;
            }
        } catch (Exception e) {
            logger.warn("Failed to get user profile from cache: {}", e.getMessage());
        }
        
        try {
            // Get user with optimized query
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return null;
            }
            
            User user = userOpt.get();
            
            // Cache the result
            try {
                redisTemplate.opsForValue().set(cacheKey, user, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                logger.debug("Cached user profile: {}", cacheKey);
            } catch (Exception e) {
                logger.warn("Failed to cache user profile: {}", e.getMessage());
            }
            
            return user;
            
        } catch (Exception e) {
            logger.error("Error getting optimized user profile: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get conversation statistics for AI processing
     */
    public Map<String, Object> getConversationStatistics(UUID conversationId, UUID userId) {
        String cacheKey = "conversation_stats:" + conversationId + ":" + userId;
        
        try {
            // Try cache first
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cachedStats = (Map<String, Object>) cached;
                logger.debug("Cache hit for conversation statistics: {}", cacheKey);
                return cachedStats;
            }
        } catch (Exception e) {
            logger.warn("Failed to get conversation statistics from cache: {}", e.getMessage());
        }
        
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Get conversation
            Optional<Conversation> conversationOpt = conversationRepository.findById(conversationId);
            if (conversationOpt.isEmpty()) {
                return stats;
            }
            
            Conversation conversation = conversationOpt.get();
            
            // Get message counts by sender
            List<Message> allMessages = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
            long totalMessages = allMessages.size();
            long userMessages = allMessages.stream().filter(m -> "user".equals(m.getSender())).count();
            long aiMessages = allMessages.stream().filter(m -> "ai".equals(m.getSender())).count();
            
            // Get conversation duration
            long durationMinutes = 0;
            if (!allMessages.isEmpty()) {
                Message first = allMessages.get(0);
                Message last = allMessages.get(allMessages.size() - 1);
                durationMinutes = java.time.Duration.between(first.getTimestamp(), last.getTimestamp()).toMinutes();
            }
            
            // Get average response time
            double avgResponseTime = calculateAverageResponseTime(allMessages);
            
            // Get message frequency
            Map<String, Long> messageFrequency = calculateMessageFrequency(allMessages);
            
            stats.put("conversationId", conversationId);
            stats.put("title", conversation.getTitle());
            stats.put("totalMessages", totalMessages);
            stats.put("userMessages", userMessages);
            stats.put("aiMessages", aiMessages);
            stats.put("durationMinutes", durationMinutes);
            stats.put("avgResponseTimeSeconds", avgResponseTime);
            stats.put("messageFrequency", messageFrequency);
            stats.put("createdAt", conversation.getCreatedAt());
            stats.put("lastUpdated", conversation.getUpdatedAt());
            
            // Cache the result
            try {
                redisTemplate.opsForValue().set(cacheKey, stats, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                logger.debug("Cached conversation statistics: {}", cacheKey);
            } catch (Exception e) {
                logger.warn("Failed to cache conversation statistics: {}", e.getMessage());
            }
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error getting conversation statistics: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Get user conversation history for AI processing
     */
    public List<Conversation> getUserConversationHistory(UUID userId, int limit) {
        String cacheKey = "user_conversations:" + userId + ":" + limit;
        
        try {
            // Try cache first
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                @SuppressWarnings("unchecked")
                List<Conversation> cachedConversations = (List<Conversation>) cached;
                logger.debug("Cache hit for user conversation history: {}", cacheKey);
                return cachedConversations;
            }
        } catch (Exception e) {
            logger.warn("Failed to get user conversation history from cache: {}", e.getMessage());
        }
        
        try {
            // Use pagination for better performance
            Pageable pageable = PageRequest.of(0, limit, Sort.by("updatedAt").descending());
            Page<Conversation> conversationPage = conversationRepository.findByUserIdOrderByIsPinnedDescUpdatedAtDesc(userId, pageable);
            
            List<Conversation> conversations = conversationPage.getContent();
            
            // Cache the result
            try {
                redisTemplate.opsForValue().set(cacheKey, conversations, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                logger.debug("Cached user conversation history: {}", cacheKey);
            } catch (Exception e) {
                logger.warn("Failed to cache user conversation history: {}", e.getMessage());
            }
            
            return conversations;
            
        } catch (Exception e) {
            logger.error("Error getting user conversation history: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get message patterns for AI processing
     */
    public Map<String, Object> getMessagePatterns(UUID userId, int days) {
        String cacheKey = "message_patterns:" + userId + ":" + days;
        
        try {
            // Try cache first
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cachedPatterns = (Map<String, Object>) cached;
                logger.debug("Cache hit for message patterns: {}", cacheKey);
                return cachedPatterns;
            }
        } catch (Exception e) {
            logger.warn("Failed to get message patterns from cache: {}", e.getMessage());
        }
        
        try {
            Map<String, Object> patterns = new HashMap<>();
            
            // Get messages from the last N days
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            // Get user's conversations first, then get messages from those conversations
            List<Conversation> userConversations = conversationRepository.findByUserIdOrderByIsPinnedDescUpdatedAtDesc(userId, PageRequest.of(0, 100)).getContent();
            List<UUID> conversationIds = userConversations.stream().map(Conversation::getId).toList();
            
            List<Message> messages = new ArrayList<>();
            for (UUID conversationId : conversationIds) {
                List<Message> conversationMessages = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
                messages.addAll(conversationMessages.stream()
                    .filter(m -> m.getTimestamp().isAfter(startDate))
                    .toList());
            }
            
            // Analyze patterns
            Map<String, Long> hourlyDistribution = calculateHourlyDistribution(messages);
            Map<String, Long> dailyDistribution = calculateDailyDistribution(messages);
            Map<String, Long> senderDistribution = calculateSenderDistribution(messages);
            
            // Calculate average message length
            double avgMessageLength = messages.stream()
                .mapToInt(m -> m.getText().length())
                .average()
                .orElse(0.0);
            
            // Calculate message frequency
            long totalMessages = messages.size();
            double messagesPerDay = totalMessages / (double) days;
            
            patterns.put("totalMessages", totalMessages);
            patterns.put("avgMessageLength", avgMessageLength);
            patterns.put("messagesPerDay", messagesPerDay);
            patterns.put("hourlyDistribution", hourlyDistribution);
            patterns.put("dailyDistribution", dailyDistribution);
            patterns.put("senderDistribution", senderDistribution);
            patterns.put("analysisPeriod", days);
            patterns.put("startDate", startDate);
            patterns.put("endDate", LocalDateTime.now());
            
            // Cache the result
            try {
                redisTemplate.opsForValue().set(cacheKey, patterns, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                logger.debug("Cached message patterns: {}", cacheKey);
            } catch (Exception e) {
                logger.warn("Failed to cache message patterns: {}", e.getMessage());
            }
            
            return patterns;
            
        } catch (Exception e) {
            logger.error("Error getting message patterns: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * Clear cache for a specific user
     * Uses SCAN instead of KEYS to avoid blocking Redis in production
     */
    public void clearUserCache(UUID userId) {
        try {
            // Clear user-specific cache keys using SCAN (non-blocking)
            String pattern = "*:" + userId + "*";
            Set<String> keys = new HashSet<>();
            
            // Use SCAN instead of KEYS for production safety
            ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100) // Scan 100 keys per iteration
                .build();
            
            // Execute SCAN operation using RedisTemplate's execute method
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        String key = new String(cursor.next(), StandardCharsets.UTF_8);
                        keys.add(key);
                    }
                } catch (Exception e) {
                    logger.warn("Error during SCAN operation: {}", e.getMessage());
                }
                return null;
            });
            
            // Delete found keys
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("Cleared cache for user: {} ({} keys)", userId, keys.size());
            } else {
                logger.info("No cache keys found for user: {}", userId);
            }
        } catch (Exception e) {
            logger.error("Error clearing user cache: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Clear cache for a specific conversation
     * Uses SCAN instead of KEYS to avoid blocking Redis in production
     */
    public void clearConversationCache(UUID conversationId) {
        try {
            // Clear conversation-specific cache keys using SCAN (non-blocking)
            String pattern = "*:" + conversationId + "*";
            Set<String> keys = new HashSet<>();
            
            // Use SCAN instead of KEYS for production safety
            ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100) // Scan 100 keys per iteration
                .build();
            
            // Execute SCAN operation using RedisTemplate's execute method
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        String key = new String(cursor.next(), StandardCharsets.UTF_8);
                        keys.add(key);
                    }
                } catch (Exception e) {
                    logger.warn("Error during SCAN operation: {}", e.getMessage());
                }
                return null;
            });
            
            // Delete found keys
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.info("Cleared cache for conversation: {} ({} keys)", conversationId, keys.size());
            } else {
                logger.info("No cache keys found for conversation: {}", conversationId);
            }
        } catch (Exception e) {
            logger.error("Error clearing conversation cache: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get cache key counts by pattern
            String[] patterns = {
                CONVERSATION_CACHE_KEY + ":*",
                MESSAGE_CACHE_KEY + ":*",
                USER_PROFILE_CACHE_KEY + ":*",
                "conversation_stats:*",
                "user_conversations:*",
                "message_patterns:*"
            };
            
            Map<String, Long> keyCounts = new HashMap<>();
            for (String pattern : patterns) {
                Set<String> keys = redisTemplate.keys(pattern);
                keyCounts.put(pattern, keys != null ? (long) keys.size() : 0L);
            }
            
            stats.put("keyCounts", keyCounts);
            stats.put("totalKeys", keyCounts.values().stream().mapToLong(Long::longValue).sum());
            stats.put("cacheTtlMinutes", CACHE_TTL_MINUTES);
            stats.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error getting cache statistics: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Calculate average response time
     */
    private double calculateAverageResponseTime(List<Message> messages) {
        List<Long> responseTimes = new ArrayList<>();
        Message lastUserMessage = null;
        
        for (Message message : messages) {
            if ("user".equals(message.getSender())) {
                lastUserMessage = message;
            } else if ("ai".equals(message.getSender()) && lastUserMessage != null) {
                long responseTime = java.time.Duration.between(
                    lastUserMessage.getTimestamp(), 
                    message.getTimestamp()
                ).toSeconds();
                responseTimes.add(responseTime);
            }
        }
        
        return responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Calculate message frequency
     */
    private Map<String, Long> calculateMessageFrequency(List<Message> messages) {
        return messages.stream()
            .collect(Collectors.groupingBy(
                m -> m.getTimestamp().toLocalDate().toString(),
                Collectors.counting()
            ));
    }
    
    /**
     * Calculate hourly distribution
     */
    private Map<String, Long> calculateHourlyDistribution(List<Message> messages) {
        return messages.stream()
            .collect(Collectors.groupingBy(
                m -> String.valueOf(m.getTimestamp().getHour()),
                Collectors.counting()
            ));
    }
    
    /**
     * Calculate daily distribution
     */
    private Map<String, Long> calculateDailyDistribution(List<Message> messages) {
        return messages.stream()
            .collect(Collectors.groupingBy(
                m -> m.getTimestamp().getDayOfWeek().toString(),
                Collectors.counting()
            ));
    }
    
    /**
     * Calculate sender distribution
     */
    private Map<String, Long> calculateSenderDistribution(List<Message> messages) {
        return messages.stream()
            .collect(Collectors.groupingBy(
                Message::getSender,
                Collectors.counting()
            ));
    }
}

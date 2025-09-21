package ai.youthmind.service;

import ai.youthmind.entity.MemoryItem;
import ai.youthmind.repository.MemoryItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MemoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryService.class);
    
    // Time decay constants (τ ≈ 30 days)
    private static final double TAU_DAYS = 30.0;
    private static final double TAU_SECONDS = TAU_DAYS * 24 * 60 * 60;
    
    // Engagement weights
    private static final Map<String, Double> ENGAGEMENT_WEIGHTS = Map.of(
        "SAVE", 10.0,
        "POST", 8.0, 
        "LIKE", 3.0,
        "COMMENT", 5.0,
        "VIEW", 1.0,
        "SHARE", 7.0,
        "RESONANCE", 6.0
    );
    
    // Memory type base weights
    private static final Map<MemoryItem.MemoryType, Double> TYPE_BASE_WEIGHTS = Map.of(
        MemoryItem.MemoryType.PERSONALITY_TRAIT, 10.0,
        MemoryItem.MemoryType.USER_PREFERENCE, 8.0,
        MemoryItem.MemoryType.CHAT_CONTEXT, 6.0,
        MemoryItem.MemoryType.ENGAGEMENT_PATTERN, 7.0,
        MemoryItem.MemoryType.EMOTIONAL_STATE, 5.0,
        MemoryItem.MemoryType.CONVERSATION_TOPIC, 4.0,
        MemoryItem.MemoryType.BEHAVIORAL_PATTERN, 6.0,
        MemoryItem.MemoryType.INTERACTION_HISTORY, 3.0
    );
    
    @Autowired
    private MemoryItemRepository memoryRepository;
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // ==================== MEMORY STORAGE ====================
    
    /**
     * Store a new memory item with automatic weight calculation
     */
    public MemoryItem storeMemory(UUID userId, MemoryItem.MemoryType type, String key, 
                                 String content, Map<String, Object> context) {
        MemoryItem memory = new MemoryItem();
        memory.setUserId(userId);
        memory.setMemoryType(type);
        memory.setKeyIdentifier(key);
        memory.setContent(content);
        
        // Set base weight based on type
        Double baseWeight = TYPE_BASE_WEIGHTS.getOrDefault(type, 5.0);
        memory.setBaseWeight(BigDecimal.valueOf(baseWeight));
        
        // Set importance level based on type
        memory.setImportanceLevel(determineImportanceLevel(type, content));
        
        // Store context as JSON
        if (context != null && !context.isEmpty()) {
            try {
                memory.setContextTags(objectMapper.writeValueAsString(context));
            } catch (Exception e) {
                logger.warn("Failed to serialize context for memory: {}", e.getMessage());
            }
        }
        
        // Compress content if it's large
        if (content.length() > 1000) {
            memory.setCompressedContent(compressContent(content));
        }
        
        // Calculate initial weight
        memory.setWeightScore(calculateInitialWeight(memory));
        
        MemoryItem savedMemory = memoryRepository.save(memory);
        
        // Cache frequently accessed memories
        cacheMemoryIfImportant(savedMemory);
        
        logger.debug("Stored memory for user {}: {} (weight: {})", 
                    userId, key, savedMemory.getWeightScore());
        
        return savedMemory;
    }
    
    /**
     * Store chat context memory
     */
    public MemoryItem storeChatMemory(UUID userId, String conversationContext, 
                                     String userMessage, String aiResponse) {
        String key = "chat_" + System.currentTimeMillis();
        String content = String.format("User: %s\nAI: %s\nContext: %s", 
                                      userMessage, aiResponse, conversationContext);
        
        Map<String, Object> context = Map.of(
            "type", "chat_interaction",
            "timestamp", LocalDateTime.now().toString(),
            "messageLength", userMessage.length(),
            "responseLength", aiResponse.length()
        );
        
        return storeMemory(userId, MemoryItem.MemoryType.CHAT_CONTEXT, key, content, context);
    }
    
    /**
     * Store engagement pattern memory
     */
    public MemoryItem storeEngagementMemory(UUID userId, String engagementType, 
                                           String contentType, String details) {
        String key = "engagement_" + engagementType + "_" + contentType;
        String content = String.format("User engaged with %s via %s: %s", 
                                      contentType, engagementType, details);
        
        Map<String, Object> context = Map.of(
            "engagementType", engagementType,
            "contentType", contentType,
            "weight", ENGAGEMENT_WEIGHTS.getOrDefault(engagementType.toUpperCase(), 1.0)
        );
        
        return storeMemory(userId, MemoryItem.MemoryType.ENGAGEMENT_PATTERN, key, content, context);
    }
    
    /**
     * Store personality trait memory
     */
    public MemoryItem storePersonalityTrait(UUID userId, String trait, String evidence, 
                                           double confidence) {
        String key = "personality_" + trait.toLowerCase();
        String content = String.format("Trait: %s (confidence: %.2f) - Evidence: %s", 
                                      trait, confidence, evidence);
        
        Map<String, Object> context = Map.of(
            "trait", trait,
            "confidence", confidence,
            "evidenceType", "behavioral_analysis"
        );
        
        return storeMemory(userId, MemoryItem.MemoryType.PERSONALITY_TRAIT, key, content, context);
    }
    
    // ==================== MEMORY RETRIEVAL ====================
    
    /**
     * Retrieve memories with hybrid weighting (recency + importance + engagement)
     */
    public List<MemoryItem> retrieveRelevantMemories(UUID userId, String context, int limit) {
        // First, update all memory weights with time decay
        updateMemoryWeights(userId);
        
        // Get context-relevant memories
        List<MemoryItem> contextMemories = memoryRepository.findByUserIdAndContext(userId, context);
        
        // Get high-importance memories
        List<MemoryItem> importantMemories = memoryRepository.findByImportanceLevel(
            userId, MemoryItem.ImportanceLevel.CRITICAL);
        importantMemories.addAll(memoryRepository.findByImportanceLevel(
            userId, MemoryItem.ImportanceLevel.HIGH));
        
        // Get recent memories
        List<MemoryItem> recentMemories = memoryRepository.findRecentMemories(
            userId, LocalDateTime.now().minusDays(7));
        
        // Combine and deduplicate
        Set<UUID> seenIds = new HashSet<>();
        List<MemoryItem> combinedMemories = new ArrayList<>();
        
        // Add context memories first (highest priority)
        for (MemoryItem memory : contextMemories) {
            if (seenIds.add(memory.getId())) {
                combinedMemories.add(memory);
            }
        }
        
        // Add important memories
        for (MemoryItem memory : importantMemories) {
            if (seenIds.add(memory.getId())) {
                combinedMemories.add(memory);
            }
        }
        
        // Add recent memories
        for (MemoryItem memory : recentMemories) {
            if (seenIds.add(memory.getId())) {
                combinedMemories.add(memory);
            }
        }
        
        // Sort by current weight and return top results
        return combinedMemories.stream()
            .sorted((m1, m2) -> m2.calculateCurrentWeight().compareTo(m1.calculateCurrentWeight()))
            .limit(limit)
            .peek(memory -> {
                // Increment access count
                memory.incrementAccess();
                memoryRepository.save(memory);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Get compressed context for AI (≤1-2KB)
     */
    public String getCompressedContext(UUID userId, String currentContext) {
        List<MemoryItem> relevantMemories = retrieveRelevantMemories(userId, currentContext, 10);
        
        StringBuilder contextBuilder = new StringBuilder();
        int totalLength = 0;
        int maxLength = 2048; // 2KB limit
        
        // Add personality traits first (highest priority)
        for (MemoryItem memory : relevantMemories) {
            if (memory.getMemoryType() == MemoryItem.MemoryType.PERSONALITY_TRAIT) {
                String content = memory.getCompressedContent() != null ? 
                    memory.getCompressedContent() : memory.getContent();
                
                if (totalLength + content.length() < maxLength) {
                    contextBuilder.append("[TRAIT] ").append(content).append("\n");
                    totalLength += content.length() + 8; // +8 for "[TRAIT] " and "\n"
                }
            }
        }
        
        // Add preferences
        for (MemoryItem memory : relevantMemories) {
            if (memory.getMemoryType() == MemoryItem.MemoryType.USER_PREFERENCE) {
                String content = memory.getCompressedContent() != null ? 
                    memory.getCompressedContent() : memory.getContent();
                
                if (totalLength + content.length() < maxLength) {
                    contextBuilder.append("[PREF] ").append(content).append("\n");
                    totalLength += content.length() + 7;
                }
            }
        }
        
        // Add recent chat context
        for (MemoryItem memory : relevantMemories) {
            if (memory.getMemoryType() == MemoryItem.MemoryType.CHAT_CONTEXT) {
                String content = memory.getCompressedContent() != null ? 
                    memory.getCompressedContent() : memory.getContent();
                
                if (totalLength + content.length() < maxLength) {
                    contextBuilder.append("[CHAT] ").append(content).append("\n");
                    totalLength += content.length() + 7;
                }
            }
        }
        
        return contextBuilder.toString().trim();
    }
    
    // ==================== WEIGHT CALCULATION ====================
    
    /**
     * Calculate initial weight for a memory item
     */
    private BigDecimal calculateInitialWeight(MemoryItem memory) {
        double baseWeight = memory.getBaseWeight().doubleValue();
        
        // Importance multiplier
        double importanceMultiplier = switch (memory.getImportanceLevel()) {
            case CRITICAL -> 3.0;
            case HIGH -> 2.0;
            case MEDIUM -> 1.0;
            case LOW -> 0.5;
        };
        
        // Content length factor (longer content might be more important)
        double contentLengthFactor = Math.min(2.0, 1.0 + (memory.getContent().length() / 1000.0));
        
        double finalWeight = baseWeight * importanceMultiplier * contentLengthFactor;
        
        return BigDecimal.valueOf(finalWeight).round(new MathContext(10, RoundingMode.HALF_UP));
    }
    
    /**
     * Update memory weights with time decay: weight = baseWeight * exp(-Δt/τ)
     */
    @Transactional
    public void updateMemoryWeights(UUID userId) {
        List<MemoryItem> userMemories = memoryRepository.findByUserIdAndArchivedAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();
        
        for (MemoryItem memory : userMemories) {
            BigDecimal newWeight = calculateTimeDecayedWeight(memory, now);
            memory.setWeightScore(newWeight);
            memoryRepository.save(memory);
        }
    }
    
    /**
     * Calculate time-decayed weight using exponential decay
     */
    private BigDecimal calculateTimeDecayedWeight(MemoryItem memory, LocalDateTime now) {
        // Calculate time difference in seconds
        long secondsSinceCreation = ChronoUnit.SECONDS.between(memory.getCreatedAt(), now);
        long secondsSinceAccess = memory.getLastAccessedAt() != null ? 
            ChronoUnit.SECONDS.between(memory.getLastAccessedAt(), now) : secondsSinceCreation;
        
        // Use the more recent of creation or last access for decay calculation
        long decaySeconds = Math.min(secondsSinceCreation, secondsSinceAccess);
        
        // Apply exponential decay: exp(-Δt/τ)
        double timeDecayFactor = Math.exp(-decaySeconds / TAU_SECONDS);
        
        // Access frequency boost
        double accessBoost = 1.0 + Math.log1p(memory.getAccessCount()) * 0.1;
        
        // Importance multiplier
        double importanceMultiplier = switch (memory.getImportanceLevel()) {
            case CRITICAL -> 2.0;
            case HIGH -> 1.5;
            case MEDIUM -> 1.0;
            case LOW -> 0.7;
        };
        
        // Calculate final weight
        double baseWeight = memory.getBaseWeight().doubleValue();
        double engagementMultiplier = memory.getEngagementMultiplier().doubleValue();
        
        double finalWeight = baseWeight * timeDecayFactor * accessBoost * 
                           importanceMultiplier * engagementMultiplier;
        
        return BigDecimal.valueOf(finalWeight).round(new MathContext(10, RoundingMode.HALF_UP));
    }
    
    /**
     * Update engagement multiplier based on user interaction
     */
    public void updateEngagementWeight(UUID userId, String memoryKey, String engagementType) {
        Optional<MemoryItem> memoryOpt = memoryRepository.findByUserIdAndKeyIdentifier(userId, memoryKey);
        
        if (memoryOpt.isPresent()) {
            MemoryItem memory = memoryOpt.get();
            Double engagementWeight = ENGAGEMENT_WEIGHTS.getOrDefault(engagementType.toUpperCase(), 1.0);
            
            // Update engagement multiplier
            BigDecimal currentMultiplier = memory.getEngagementMultiplier();
            BigDecimal newMultiplier = currentMultiplier.add(BigDecimal.valueOf(engagementWeight * 0.1));
            memory.setEngagementMultiplier(newMultiplier);
            
            // Recalculate weight
            memory.setWeightScore(calculateTimeDecayedWeight(memory, LocalDateTime.now()));
            
            memoryRepository.save(memory);
            
            // Update cache
            invalidateMemoryCache(userId);
        }
    }
    
    // ==================== MEMORY COMPRESSION ====================
    
    /**
     * Compress content while preserving key information
     */
    private String compressContent(String content) {
        if (content.length() <= 500) {
            return content; // No need to compress
        }
        
        // Extract key sentences and concepts
        String[] sentences = content.split("[.!?]+");
        StringBuilder compressed = new StringBuilder();
        
        // Keep first and last sentences
        if (sentences.length > 0) {
            compressed.append(sentences[0].trim());
        }
        
        // Add middle content summary if there are many sentences
        if (sentences.length > 3) {
            compressed.append(" ... [").append(sentences.length - 2).append(" sentences] ... ");
        }
        
        // Keep last sentence
        if (sentences.length > 1) {
            compressed.append(sentences[sentences.length - 1].trim());
        }
        
        return compressed.toString();
    }
    
    /**
     * Determine importance level based on content analysis
     */
    private MemoryItem.ImportanceLevel determineImportanceLevel(MemoryItem.MemoryType type, String content) {
        // Personality traits and preferences are always important
        if (type == MemoryItem.MemoryType.PERSONALITY_TRAIT || 
            type == MemoryItem.MemoryType.USER_PREFERENCE) {
            return MemoryItem.ImportanceLevel.HIGH;
        }
        
        // Check for critical keywords
        String lowerContent = content.toLowerCase();
        if (lowerContent.contains("crisis") || lowerContent.contains("emergency") || 
            lowerContent.contains("suicide") || lowerContent.contains("self-harm")) {
            return MemoryItem.ImportanceLevel.CRITICAL;
        }
        
        // Check content length and complexity
        if (content.length() > 1000 || content.split("\\s+").length > 100) {
            return MemoryItem.ImportanceLevel.HIGH;
        }
        
        if (content.length() > 200) {
            return MemoryItem.ImportanceLevel.MEDIUM;
        }
        
        return MemoryItem.ImportanceLevel.LOW;
    }
    
    // ==================== MEMORY CLEANUP ====================
    
    /**
     * Archive expired and low-value memories
     */
    @Transactional
    public void performMemoryCleanup(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Archive expired memories
        List<MemoryItem> expiredMemories = memoryRepository.findExpiredMemories(now);
        for (MemoryItem memory : expiredMemories) {
            if (memory.getUserId().equals(userId)) {
                memory.archive();
                memoryRepository.save(memory);
            }
        }
        
        // Archive unused old memories (older than 90 days, never accessed)
        List<MemoryItem> unusedMemories = memoryRepository.findUnusedOldMemories(
            userId, now.minusDays(90));
        for (MemoryItem memory : unusedMemories) {
            memory.archive();
            memoryRepository.save(memory);
        }
        
        // Check if user has too many memories
        Long memoryCount = memoryRepository.countActiveMemoriesByUser(userId);
        if (memoryCount > 1000) { // Limit per user
            // Archive lowest weight memories
            List<MemoryItem> lowWeightMemories = memoryRepository.findLowWeightMemories(
                userId, BigDecimal.valueOf(0.1));
            
            int toArchive = (int) (memoryCount - 800); // Keep 800 memories
            for (int i = 0; i < Math.min(toArchive, lowWeightMemories.size()); i++) {
                lowWeightMemories.get(i).archive();
                memoryRepository.save(lowWeightMemories.get(i));
            }
        }
        
        // Invalidate cache after cleanup
        invalidateMemoryCache(userId);
        
        logger.info("Memory cleanup completed for user {}: {} expired, {} unused archived", 
                   userId, expiredMemories.size(), unusedMemories.size());
    }
    
    /**
     * Archive all memories for a user (for account deletion)
     */
    @Transactional
    public void archiveAllUserMemories(UUID userId) {
        List<MemoryItem> userMemories = memoryRepository.findByUserIdAndArchivedAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();
        
        for (MemoryItem memory : userMemories) {
            memory.setArchivedAt(now);
        }
        
        memoryRepository.saveAll(userMemories);
        invalidateMemoryCache(userId);
        
        logger.info("Archived {} memories for user {}", userMemories.size(), userId);
    }
    
    // ==================== CACHING ====================
    
    /**
     * Cache important memories in Redis for fast access
     */
    private void cacheMemoryIfImportant(MemoryItem memory) {
        if (memory.getImportanceLevel() == MemoryItem.ImportanceLevel.CRITICAL ||
            memory.getImportanceLevel() == MemoryItem.ImportanceLevel.HIGH) {
            
            String cacheKey = "memory:" + memory.getUserId() + ":" + memory.getKeyIdentifier();
            redisService.setValue(cacheKey, memory, 1, java.util.concurrent.TimeUnit.HOURS);
        }
    }
    
    /**
     * Get cached memory
     */
    public Optional<MemoryItem> getCachedMemory(UUID userId, String keyIdentifier) {
        String cacheKey = "memory:" + userId + ":" + keyIdentifier;
        Object cached = redisService.getValue(cacheKey);
        
        if (cached instanceof MemoryItem) {
            return Optional.of((MemoryItem) cached);
        }
        
        return Optional.empty();
    }
    
    /**
     * Invalidate memory cache for user
     */
    private void invalidateMemoryCache(UUID userId) {
        String pattern = "memory:" + userId + ":*";
        redisService.deleteByPattern(pattern);
    }
    
    // ==================== ANALYTICS ====================
    
    /**
     * Get memory statistics for a user
     */
    public Map<String, Object> getMemoryStats(UUID userId) {
        List<Object[]> typeStats = memoryRepository.getMemoryStatsByType(userId);
        List<Object[]> timeline = memoryRepository.getMemoryCreationTimeline(
            userId, LocalDateTime.now().minusDays(30));
        
        Long totalSize = memoryRepository.getTotalMemorySizeByUser(userId);
        Long activeCount = memoryRepository.countActiveMemoriesByUser(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMemories", activeCount);
        stats.put("totalSizeBytes", totalSize);
        stats.put("typeDistribution", typeStats);
        stats.put("creationTimeline", timeline);
        stats.put("lastCleanup", LocalDateTime.now());
        
        return stats;
    }
}

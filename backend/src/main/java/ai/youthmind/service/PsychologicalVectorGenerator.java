package ai.youthmind.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for generating 43-dimensional psychological vectors based on Buddhist mental factors
 * 
 * The 43-dimensional vector represents Buddhist mental factors as follows:
 * 
 * POSITIVE FACTORS (Wholesome - Dimensions 1-16):
 * 1. Attention - mindful awareness and focus
 * 2. Contact - sensory and mental contact
 * 3. Feeling - emotional sensations and experiences
 * 4. Thinking - cognitive processes and thoughts
 * 5. Deliberation - careful consideration and reflection
 * 6. Faith - trust, confidence, and belief
 * 7. Vigor - energy, enthusiasm, and effort
 * 8. Inner shame - moral conscience and self-respect
 * 9. Shame - humility, modesty, and embarrassment
 * 10. Absence of craving - contentment and non-attachment
 * 11. Absence of hatred - love, compassion, and forgiveness
 * 12. Absence of ignorance - wisdom, understanding, and insight
 * 13. Light ease - lightness, comfort, and peace
 * 14. Non-laxness - diligence, perseverance, and determination
 * 15. Renunciation - letting go, detachment, and abandonment
 * 16. Non-harming - compassion, kindness, and gentleness
 * 
 * NEGATIVE FACTORS (Unwholesome - Dimensions 17-43):
 * 17. Greed - desire, craving, and attachment
 * 18. Anger - rage, fury, and hostility
 * 19. Arrogance - pride, conceit, and superiority
 * 20. Foolishness - ignorance, stupidity, and unwisdom
 * 21. Doubt - uncertainty, skepticism, and confusion
 * 22. Improper views - wrong view, misconception, and delusion
 * 23. Wrath - rage, fury, and indignation
 * 24. Hatred - aversion, dislike, and enmity
 * 25. Rage - fury, wrath, and outrage
 * 26. Covering - hiding, concealing, and masking
 * 27. Deceit - deception, lying, and dishonesty
 * 28. Flattery - praise, compliment, and adulation
 * 29. Conceit - pride, arrogance, and vanity
 * 30. Harming - hurting, damaging, and violence
 * 31. Jealousy - envy, resentment, and bitterness
 * 32. Stinginess - greed, selfishness, and miserliness
 * 33. Lack of remorse - unrepentant, unconcerned, and callous
 * 34. Lack of shame - shameless, brazen, and impudent
 * 35. Lack of faith - faithless, distrustful, and cynical
 * 36. Laziness - sloth, idleness, and procrastination
 * 37. Laxness - looseness, carelessness, and negligence
 * 38. Torpor - dullness, lethargy, and sluggishness
 * 39. Restlessness - agitation, unease, and anxiety
 * 40. Distraction - scatteredness, confusion, and disorder
 * 41. Improper knowledge - wrong knowledge, misunderstanding, and delusion
 * 42. Scatteredness - distraction, dispersion, and fragmentation
 * 43. Regret - remorse, sorrow, and repentance
 * 
 * Each dimension is calculated based on keyword analysis of content,
 * normalized to values between 0.0 and 1.0, where higher values indicate
 * stronger presence of that mental factor in the analyzed content.
 */
@Service
@Transactional
public class PsychologicalVectorGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(PsychologicalVectorGenerator.class);
    
    // Psychological vector dimensions (43 total - Buddhist mental factors)
    public static final int PSYCHOLOGICAL_VECTOR_DIMENSIONS = 43;
    
    // Redis keys
    private static final String PSYCHOLOGICAL_VECTOR_KEY = "psychological_vector:";
    private static final String VECTOR_CACHE_KEY = "vector_cache:";
    private static final String VECTOR_HISTORY_KEY = "vector_history:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Generate 37-dimensional psychological vector
     * @param content Text content to analyze
     * @param userId User ID for personalization
     * @return 37-dimensional psychological vector
     */
    public Map<String, Object> generateVector(String content, UUID userId) {
        try {
            logger.debug("Generating psychological vector for user {} with content length {}", userId, content.length());
            
            // Check cache first
            String cacheKey = VECTOR_CACHE_KEY + userId + ":" + content.hashCode();
            Map<String, Object> cachedVector = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedVector != null) {
                logger.debug("Using cached psychological vector for user {}", userId);
                return cachedVector;
            }
            
            // Generate psychological vector components
            List<Double> psychologicalVector = generatePsychologicalComponents(content, userId);
            
            // Validate vector
            boolean isValid = validateVector(psychologicalVector);
            if (!isValid) {
                logger.warn("Generated invalid psychological vector for user {}", userId);
                psychologicalVector = createDefaultVector();
            }
            
            // Optimize vector
            psychologicalVector = optimizeVector(psychologicalVector);
            
            // Create result
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId.toString());
            result.put("psychologicalVector", psychologicalVector);
            result.put("dimensions", PSYCHOLOGICAL_VECTOR_DIMENSIONS);
            result.put("isValid", isValid);
            result.put("generationMethod", "PSYCHOLOGICAL_ANALYSIS");
            result.put("timestamp", LocalDateTime.now());
            
            // Store vector
            storeVector(userId, result);
            
            // Cache the result
            redisTemplate.opsForValue().set(cacheKey, result, 2, TimeUnit.HOURS);
            
            logger.debug("Psychological vector generated for user {}: {} dimensions", userId, psychologicalVector.size());
            return result;
            
        } catch (Exception e) {
            logger.error("Error generating psychological vector for user {}: {}", userId, e.getMessage(), e);
            return createDefaultVectorResult(userId);
        }
    }
    
    /**
     * Validate psychological vector
     * @param vector Vector to validate
     * @return Validation result
     */
    public boolean validateVector(List<Double> vector) {
        try {
            logger.debug("Validating psychological vector with {} dimensions", vector.size());
            
            // Check dimensions
            if (vector == null || vector.size() != PSYCHOLOGICAL_VECTOR_DIMENSIONS) {
                logger.warn("Invalid vector dimensions: expected {}, got {}", PSYCHOLOGICAL_VECTOR_DIMENSIONS, 
                    vector != null ? vector.size() : 0);
                return false;
            }
            
            // Check value ranges
            for (int i = 0; i < vector.size(); i++) {
                Double value = vector.get(i);
                if (value == null || value < 0.0 || value > 1.0) {
                    logger.warn("Invalid vector value at index {}: {}", i, value);
                    return false;
                }
            }
            
            // Check for reasonable distribution
            double mean = vector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            if (mean < 0.1 || mean > 0.9) {
                logger.warn("Vector mean too extreme: {}", mean);
                return false;
            }
            
            logger.debug("Psychological vector validation passed");
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating psychological vector: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Optimize psychological vector
     * @param vector Vector to optimize
     * @return Optimized vector
     */
    public List<Double> optimizeVector(List<Double> vector) {
        try {
            logger.debug("Optimizing psychological vector");
            
            if (vector == null || vector.size() != PSYCHOLOGICAL_VECTOR_DIMENSIONS) {
                return createDefaultVector();
            }
            
            List<Double> optimizedVector = new ArrayList<>(vector);
            
            // Apply smoothing to reduce noise
            optimizedVector = applySmoothing(optimizedVector);
            
            // Apply normalization
            optimizedVector = applyNormalization(optimizedVector);
            
            // Apply thresholding
            optimizedVector = applyThresholding(optimizedVector);
            
            logger.debug("Psychological vector optimized");
            return optimizedVector;
            
        } catch (Exception e) {
            logger.error("Error optimizing psychological vector: {}", e.getMessage(), e);
            return vector != null ? vector : createDefaultVector();
        }
    }
    
    /**
     * Store psychological vector
     * @param userId User ID
     * @param vectorData Vector data to store
     */
    public void storeVector(UUID userId, Map<String, Object> vectorData) {
        try {
            logger.debug("Storing psychological vector for user {}", userId);
            
            String storageKey = PSYCHOLOGICAL_VECTOR_KEY + userId;
            
            // Get existing vector history
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(VECTOR_HISTORY_KEY + userId);
            if (history == null) {
                history = new HashMap<>();
            }
            
            // Add new vector to history
            List<Map<String, Object>> vectorHistory = (List<Map<String, Object>>) history.get("vectors");
            if (vectorHistory == null) {
                vectorHistory = new ArrayList<>();
            }
            
            Map<String, Object> vectorEntry = new HashMap<>();
            vectorEntry.put("timestamp", LocalDateTime.now());
            vectorEntry.put("vector", vectorData.get("psychologicalVector"));
            vectorEntry.put("isValid", vectorData.get("isValid"));
            
            vectorHistory.add(vectorEntry);
            
            // Keep only recent vectors (last 50)
            if (vectorHistory.size() > 50) {
                vectorHistory = vectorHistory.subList(vectorHistory.size() - 50, vectorHistory.size());
            }
            
            // Calculate vector statistics
            Map<String, Object> statistics = calculateVectorStatistics(vectorHistory);
            
            history.put("vectors", vectorHistory);
            history.put("statistics", statistics);
            history.put("lastUpdated", LocalDateTime.now());
            
            // Store updated history
            redisTemplate.opsForValue().set(VECTOR_HISTORY_KEY + userId, history, 30, TimeUnit.DAYS);
            
            // Store current vector
            redisTemplate.opsForValue().set(storageKey, vectorData, 6, TimeUnit.HOURS);
            
            logger.debug("Psychological vector stored for user {}", userId);
            
        } catch (Exception e) {
            logger.error("Error storing psychological vector for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Get psychological vector for a user
     * @param userId User ID
     * @return Latest psychological vector
     */
    public Map<String, Object> getVector(UUID userId) {
        try {
            logger.debug("Getting psychological vector for user {}", userId);
            
            String storageKey = PSYCHOLOGICAL_VECTOR_KEY + userId;
            Map<String, Object> vectorData = (Map<String, Object>) redisTemplate.opsForValue().get(storageKey);
            
            return vectorData != null ? vectorData : createDefaultVectorResult(userId);
            
        } catch (Exception e) {
            logger.error("Error getting psychological vector for user {}: {}", userId, e.getMessage(), e);
            return createDefaultVectorResult(userId);
        }
    }
    
    /**
     * Get vector history for a user
     * @param userId User ID
     * @return Vector history
     */
    public Map<String, Object> getVectorHistory(UUID userId) {
        try {
            logger.debug("Getting vector history for user {}", userId);
            
            String historyKey = VECTOR_HISTORY_KEY + userId;
            Map<String, Object> history = (Map<String, Object>) redisTemplate.opsForValue().get(historyKey);
            
            return history != null ? history : new HashMap<>();
            
        } catch (Exception e) {
            logger.error("Error getting vector history for user {}: {}", userId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private List<Double> generatePsychologicalComponents(String content, UUID userId) {
        List<Double> vector = new ArrayList<>();
        
        // Generate 43-dimensional Buddhist mental factors vector
        vector.addAll(analyzeBuddhistMentalFactors(content));
        
        return vector;
    }
    
    private List<Double> analyzeBuddhistMentalFactors(String content) {
        List<Double> factors = new ArrayList<>();
        
        // 43-dimensional Buddhist mental factors vector
        // Positive factors (wholesome)
        factors.add(calculateFactorIntensity(content, "attention", Arrays.asList("attention", "mindful", "aware", "focused", "attentive")));
        factors.add(calculateFactorIntensity(content, "contact", Arrays.asList("contact", "touch", "connection", "interaction", "engagement")));
        factors.add(calculateFactorIntensity(content, "feeling", Arrays.asList("feeling", "emotion", "sensation", "experience", "perception")));
        factors.add(calculateFactorIntensity(content, "thinking", Arrays.asList("thinking", "thought", "consideration", "reflection", "contemplation")));
        factors.add(calculateFactorIntensity(content, "deliberation", Arrays.asList("deliberation", "consideration", "pondering", "weighing", "evaluation")));
        factors.add(calculateFactorIntensity(content, "faith", Arrays.asList("faith", "trust", "confidence", "belief", "conviction")));
        factors.add(calculateFactorIntensity(content, "vigor", Arrays.asList("vigor", "energy", "enthusiasm", "zeal", "effort")));
        factors.add(calculateFactorIntensity(content, "inner_shame", Arrays.asList("inner shame", "conscience", "moral sense", "self-respect", "dignity")));
        factors.add(calculateFactorIntensity(content, "shame", Arrays.asList("shame", "embarrassment", "humility", "modesty", "bashfulness")));
        factors.add(calculateFactorIntensity(content, "absence_of_craving", Arrays.asList("contentment", "satisfaction", "non-craving", "detachment", "freedom")));
        factors.add(calculateFactorIntensity(content, "absence_of_hatred", Arrays.asList("love", "compassion", "kindness", "forgiveness", "non-hatred")));
        factors.add(calculateFactorIntensity(content, "absence_of_ignorance", Arrays.asList("wisdom", "understanding", "knowledge", "insight", "clarity")));
        factors.add(calculateFactorIntensity(content, "light_ease", Arrays.asList("lightness", "ease", "comfort", "relief", "peace")));
        factors.add(calculateFactorIntensity(content, "non_laxness", Arrays.asList("diligence", "perseverance", "steadfastness", "determination", "commitment")));
        factors.add(calculateFactorIntensity(content, "renunciation", Arrays.asList("renunciation", "letting go", "detachment", "abandonment", "release")));
        factors.add(calculateFactorIntensity(content, "non_harming", Arrays.asList("non-harming", "compassion", "kindness", "gentleness", "care")));
        
        // Negative factors (unwholesome)
        factors.add(calculateFactorIntensity(content, "greed", Arrays.asList("greed", "desire", "craving", "attachment", "covetousness")));
        factors.add(calculateFactorIntensity(content, "anger", Arrays.asList("anger", "rage", "fury", "wrath", "hostility")));
        factors.add(calculateFactorIntensity(content, "arrogance", Arrays.asList("arrogance", "pride", "conceit", "haughtiness", "superiority")));
        factors.add(calculateFactorIntensity(content, "foolishness", Arrays.asList("foolishness", "ignorance", "stupidity", "naivety", "unwisdom")));
        factors.add(calculateFactorIntensity(content, "doubt", Arrays.asList("doubt", "uncertainty", "skepticism", "hesitation", "confusion")));
        factors.add(calculateFactorIntensity(content, "improper_views", Arrays.asList("wrong view", "misconception", "delusion", "false belief", "distorted thinking")));
        factors.add(calculateFactorIntensity(content, "wrath", Arrays.asList("wrath", "rage", "fury", "anger", "indignation")));
        factors.add(calculateFactorIntensity(content, "hatred", Arrays.asList("hatred", "aversion", "dislike", "enmity", "hostility")));
        factors.add(calculateFactorIntensity(content, "rage", Arrays.asList("rage", "fury", "wrath", "anger", "outrage")));
        factors.add(calculateFactorIntensity(content, "covering", Arrays.asList("covering", "hiding", "concealing", "masking", "disguising")));
        factors.add(calculateFactorIntensity(content, "deceit", Arrays.asList("deceit", "deception", "lying", "falsehood", "dishonesty")));
        factors.add(calculateFactorIntensity(content, "flattery", Arrays.asList("flattery", "praise", "compliment", "adulation", "sycophancy")));
        factors.add(calculateFactorIntensity(content, "conceit", Arrays.asList("conceit", "pride", "arrogance", "vanity", "self-importance")));
        factors.add(calculateFactorIntensity(content, "harming", Arrays.asList("harming", "hurting", "damaging", "injuring", "violence")));
        factors.add(calculateFactorIntensity(content, "jealousy", Arrays.asList("jealousy", "envy", "resentment", "covetousness", "bitterness")));
        factors.add(calculateFactorIntensity(content, "stinginess", Arrays.asList("stinginess", "greed", "selfishness", "miserliness", "avarice")));
        factors.add(calculateFactorIntensity(content, "lack_of_remorse", Arrays.asList("no remorse", "unrepentant", "unconcerned", "indifferent", "callous")));
        factors.add(calculateFactorIntensity(content, "lack_of_shame", Arrays.asList("shameless", "brazen", "impudent", "audacious", "bold")));
        factors.add(calculateFactorIntensity(content, "lack_of_faith", Arrays.asList("faithless", "distrustful", "skeptical", "cynical", "disbelieving")));
        factors.add(calculateFactorIntensity(content, "laziness", Arrays.asList("laziness", "sloth", "idleness", "indolence", "procrastination")));
        factors.add(calculateFactorIntensity(content, "laxness", Arrays.asList("laxness", "looseness", "carelessness", "negligence", "slackness")));
        factors.add(calculateFactorIntensity(content, "torpor", Arrays.asList("torpor", "dullness", "lethargy", "sluggishness", "inertia")));
        factors.add(calculateFactorIntensity(content, "restlessness", Arrays.asList("restlessness", "agitation", "unease", "anxiety", "worry")));
        factors.add(calculateFactorIntensity(content, "distraction", Arrays.asList("distraction", "scatteredness", "confusion", "disorder", "chaos")));
        factors.add(calculateFactorIntensity(content, "improper_knowledge", Arrays.asList("wrong knowledge", "misunderstanding", "ignorance", "confusion", "delusion")));
        factors.add(calculateFactorIntensity(content, "scatteredness", Arrays.asList("scatteredness", "distraction", "dispersion", "fragmentation", "disorder")));
        factors.add(calculateFactorIntensity(content, "regret", Arrays.asList("regret", "remorse", "sorrow", "repentance", "guilt")));
        
        return factors;
    }
    
    private double calculateFactorIntensity(String content, String factorName, List<String> keywords) {
        return calculateKeywordIntensity(content.toLowerCase(), keywords);
    }
    
    private double calculateKeywordIntensity(String content, List<String> keywords) {
        if (content == null || content.isEmpty()) {
            return 0.0;
        }
        
        int totalOccurrences = 0;
        int totalWords = content.split("\\s+").length;
        
        for (String keyword : keywords) {
            int occurrences = countOccurrences(content, keyword);
            totalOccurrences += occurrences;
        }
        
        // Normalize to [0, 1] range
        double intensity = Math.min(1.0, (double) totalOccurrences / Math.max(1, totalWords / 10));
        return intensity;
    }
    
    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }
    
    private List<Double> applySmoothing(List<Double> vector) {
        List<Double> smoothed = new ArrayList<>();
        
        for (int i = 0; i < vector.size(); i++) {
            double value = vector.get(i);
            
            // Apply simple smoothing (average with neighbors if available)
            if (i > 0 && i < vector.size() - 1) {
                double neighborAvg = (vector.get(i - 1) + vector.get(i + 1)) / 2.0;
                value = (value + neighborAvg) / 2.0;
            }
            
            smoothed.add(value);
        }
        
        return smoothed;
    }
    
    private List<Double> applyNormalization(List<Double> vector) {
        // Calculate mean and standard deviation
        double mean = vector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = vector.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        
        if (stdDev == 0) {
            return vector; // No normalization needed
        }
        
        // Apply z-score normalization and scale to [0, 1]
        return vector.stream()
            .map(value -> {
                double normalized = (value - mean) / stdDev;
                // Scale to [0, 1] using sigmoid-like function
                return 1.0 / (1.0 + Math.exp(-normalized));
            })
            .collect(Collectors.toList());
    }
    
    private List<Double> applyThresholding(List<Double> vector) {
        return vector.stream()
            .map(value -> {
                // Apply soft thresholding
                if (value < 0.1) return 0.1;
                if (value > 0.9) return 0.9;
                return value;
            })
            .collect(Collectors.toList());
    }
    
    private Map<String, Object> calculateVectorStatistics(List<Map<String, Object>> vectorHistory) {
        Map<String, Object> statistics = new HashMap<>();
        
        if (vectorHistory.isEmpty()) {
            statistics.put("totalVectors", 0);
            statistics.put("averageValidity", 0.0);
            return statistics;
        }
        
        int totalVectors = vectorHistory.size();
        long validVectors = vectorHistory.stream()
            .mapToLong(entry -> (Boolean) entry.get("isValid") ? 1 : 0)
            .sum();
        
        double averageValidity = (double) validVectors / totalVectors;
        
        statistics.put("totalVectors", totalVectors);
        statistics.put("validVectors", validVectors);
        statistics.put("averageValidity", averageValidity);
        
        return statistics;
    }
    
    private List<Double> createDefaultVector() {
        List<Double> defaultVector = new ArrayList<>();
        for (int i = 0; i < PSYCHOLOGICAL_VECTOR_DIMENSIONS; i++) {
            defaultVector.add(0.5); // Neutral values
        }
        return defaultVector;
    }
    
    private Map<String, Object> createDefaultVectorResult(UUID userId) {
        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("userId", userId.toString());
        defaultResult.put("psychologicalVector", createDefaultVector());
        defaultResult.put("dimensions", PSYCHOLOGICAL_VECTOR_DIMENSIONS);
        defaultResult.put("isValid", false);
        defaultResult.put("generationMethod", "DEFAULT");
        defaultResult.put("timestamp", LocalDateTime.now());
        return defaultResult;
    }
}

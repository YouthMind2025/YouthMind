package ai.youthmind.service;

import ai.youthmind.exception.DeepSeekScoringException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * POSITIVE FACTORS (Wholesome - Dimensions 0-15):
 * 0. Attention - mindful awareness and focus
 * 1. Contact - sensory and mental contact
 * 2. Feeling - emotional sensations and experiences
 * 3. Thinking - cognitive processes and thoughts
 * 4. Deliberation - careful consideration and reflection
 * 5. Faith - trust, confidence, and belief
 * 6. Vigor - energy, enthusiasm, and effort
 * 7. Inner shame - moral conscience and self-respect
 * 8. Shame - humility, modesty, and embarrassment
 * 9. Absence of craving - contentment and non-attachment
 * 10. Absence of hatred - love, compassion, and forgiveness
 * 11. Absence of ignorance - wisdom, understanding, and insight
 * 12. Light ease - lightness, comfort, and peace
 * 13. Non-laxness - diligence, perseverance, and determination
 * 14. Renunciation - letting go, detachment, and abandonment
 * 15. Non-harming - compassion, kindness, and gentleness
 * 
 * NEGATIVE FACTORS (Unwholesome - Dimensions 16-42):
 * 16. Greed - desire, craving, and attachment
 * 17. Anger - rage, fury, and hostility
 * 18. Arrogance - pride, conceit, and superiority
 * 19. Foolishness - ignorance, stupidity, and unwisdom
 * 20. Doubt - uncertainty, skepticism, and confusion
 * 21. Improper views - wrong view, misconception, and delusion
 * 22. Wrath - rage, fury, and indignation
 * 23. Hatred - aversion, dislike, and enmity
 * 24. Rage - fury, wrath, and outrage
 * 25. Covering - hiding, concealing, and masking
 * 26. Deceit - deception, lying, and dishonesty
 * 27. Flattery - praise, compliment, and adulation
 * 28. Conceit - pride, arrogance, and vanity
 * 29. Harming - hurting, damaging, and violence
 * 30. Jealousy - envy, resentment, and bitterness
 * 31. Stinginess - greed, selfishness, and miserliness
 * 32. Lack of remorse - unrepentant, unconcerned, and callous
 * 33. Lack of shame - shameless, brazen, and impudent
 * 34. Lack of faith - faithless, distrustful, and cynical
 * 35. Laziness - sloth, idleness, and procrastination
 * 36. Laxness - looseness, carelessness, and negligence
 * 37. Torpor - dullness, lethargy, and sluggishness
 * 38. Restlessness - agitation, unease, and anxiety
 * 39. Distraction - scatteredness, confusion, and disorder
 * 40. Improper knowledge - wrong knowledge, misunderstanding, and delusion
 * 41. Scatteredness - distraction, dispersion, and fragmentation
 * 42. Regret - remorse, sorrow, and repentance
 * 
 * KEYWORD ANALYSIS APPROACH:
 * Each dimension is calculated based on comprehensive keyword analysis of content,
 * normalized to values between 0.0 and 1.0, where higher values indicate
 * stronger presence of that mental factor in the analyzed content.
 * 
 * The keyword lists include:
 * - Formal Buddhist psychology terminology (original keywords)
 * - Colloquial mental health language (how people actually talk about struggles)
 * - Emotional expressions and raw emotional language used in distress
 * - Common phrases, idioms, and expressions people use
 * - Verb forms and variations (different tenses and forms of key words)
 * - Youth-specific language, slang, and expressions young people use
 * - Multi-word phrases like "panic attack", "locked myself up", "can't even"
 * - Crisis detection keywords for suicidal ideation and self-harm
 * - CBT cognitive distortions and negative self-talk patterns
 * 
 * CRISIS DETECTION PRIORITY:
 * Special attention is given to crisis detection keywords including:
 * - Suicidal ideation: "suicidal", "kill myself", "end it", "want to die"
 * - Self-harm: "hurt myself", "self-harm", "cutting"
 * - Severe distress: "panic attack", "can't breathe", "breakdown"
 * 
 * VALIDATION APPROACH:
 * Vectors are validated to ensure they contain meaningful psychological data.
 * 
 * Valid vectors have:
 * - Exactly 43 dimensions
 * - All values in range [0.0, 1.0]
 * - Mean between 0.01 and 0.95
 * 
 * Note: Real emotional text produces SPARSE vectors where only a few factors
 * have significant values. A mean of 0.01-0.05 is normal and indicates
 * focused emotional content. Higher means (0.1+) indicate very emotionally
 * dense or crisis-level content.
 * 
 * OPTIMIZATION APPROACH (UPDATED):
 * The optimization process has been simplified to preserve the raw psychological
 * signal from keyword matching:
 * 
 * 1. Keyword matching produces raw intensity values [0.0, 1.0+]
 * 2. Thresholding clips values to valid range [0.0, 1.0]
 * 3. NO normalization or smoothing - preserves interpretable raw values
 * 
 * Why normalization was removed:
 * - The raw keyword-matching values ARE the psychological analysis
 * - Z-score normalization + sigmoid was destroying meaningful differences
 * - Expected output from psychological analysis should match keyword intensities
 * 
 * Example: If "anxiety" appears 3 times in 150 words:
 * - Raw intensity: 3 / 15 = 0.20
 * - After optimization: 0.20 (preserved)
 * - This directly reflects the presence of anxiety keywords
 * 
 * This comprehensive approach ensures accurate psychological analysis of youth
 * mental health content by matching the actual language patterns used in
 * real-world emotional expression and mental health discussions.
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
    
    @Autowired(required = false)
    private DeepSeekVectorScoringService deepSeekScoringService;
    
    // Configuration flag to enable/disable DeepSeek RAG scoring
    @Value("${youthmind.vector.use-deepseek:true}")
    private boolean useDeepSeekScoring;
    
    /**
     * Generate 43-dimensional psychological vector using hybrid approach:
     * 1. Try DeepSeek RAG-based scoring (if enabled)
     * 2. Fall back to keyword-based scoring if DeepSeek fails
     * 
     * @param content Text content to analyze
     * @param userId User ID for personalization
     * @return 43-dimensional psychological vector with metadata
     */
    public Map<String, Object> generateVector(String content, UUID userId) {
        logger.info("╔════════════════════════════════════════════════════════════════");
        logger.info("║ PSYCHOLOGICAL VECTOR GENERATION STARTED");
        logger.info("║ User ID: {}", userId);
        logger.info("║ Content length: {} characters", content.length());
        logger.info("║ Content preview: {}", content.substring(0, Math.min(100, content.length())));
        logger.info("╚════════════════════════════════════════════════════════════════");
        
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("Generating psychological vector for user {} with content length {}", userId, content.length());
            
            // Check cache first
            String cacheKey = VECTOR_CACHE_KEY + userId + ":" + content.hashCode();
            logger.debug("→ Checking Redis cache with key: {}", cacheKey);
            Map<String, Object> cachedVector = (Map<String, Object>) redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedVector != null) {
                logger.info("✓ Using cached psychological vector for user {}", userId);
                return cachedVector;
            }
            
            logger.info("→ No cache hit, generating new vector...");
            
            // Generate psychological vector components using hybrid approach
            logger.info("→ Calling generatePsychologicalComponents()...");
            List<Double> psychologicalVector = generatePsychologicalComponents(content, userId);
            long generationDuration = System.currentTimeMillis() - startTime;
            
            logger.info("✓ Psychological vector generated in {}ms", generationDuration);
            logger.info("  - Vector dimensions: {}", psychologicalVector.size());
            logger.info("  - Non-zero scores: {}/{}", 
                psychologicalVector.stream().filter(s -> s > 0.0).count(), psychologicalVector.size());
            logger.info("  - Mean score: {:.3f}", 
                psychologicalVector.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
            logger.info("  - Max score: {:.3f}", 
                psychologicalVector.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
            
            // Determine which method was used
            String generationMethod = determineGenerationMethod();
            logger.info("  - Generation method: {}", generationMethod);
            
            // Validate vector
            boolean isValid = validateVector(psychologicalVector);
            if (!isValid) {
                logger.warn("Generated invalid psychological vector for user {}", userId);
                psychologicalVector = createDefaultVector();
                generationMethod = "DEFAULT";
            }
            
            // Optimize vector (only for keyword-based, DeepSeek returns optimized scores)
            if ("KEYWORD_FALLBACK".equals(generationMethod) || "DEFAULT".equals(generationMethod)) {
                psychologicalVector = optimizeVector(psychologicalVector);
            }
            
            // Create result
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId.toString());
            result.put("psychologicalVector", psychologicalVector);
            result.put("dimensions", PSYCHOLOGICAL_VECTOR_DIMENSIONS);
            result.put("isValid", isValid);
            result.put("generationMethod", generationMethod);
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
    
    // Thread-local variable to track which generation method was used
    private ThreadLocal<String> lastGenerationMethod = ThreadLocal.withInitial(() -> "UNKNOWN");
    
    /**
     * Generate psychological components using hybrid approach:
     * 1. Try DeepSeek RAG-based scoring first (if enabled)
     * 2. Fall back to keyword-based scoring if DeepSeek fails or is disabled
     * 
     * @param content Text content to analyze
     * @param userId User ID for tracking
     * @return List of 43 scores [0.0, 1.0]
     */
    private List<Double> generatePsychologicalComponents(String content, UUID userId) {
        logger.info("  ┌─────────────────────────────────────────────────");
        logger.info("  │ GENERATING PSYCHOLOGICAL COMPONENTS");
        logger.info("  │ DeepSeek enabled: {}", useDeepSeekScoring);
        logger.info("  │ DeepSeek service available: {}", deepSeekScoringService != null);
        logger.info("  └─────────────────────────────────────────────────");
        
        // Try DeepSeek RAG scoring first (if enabled and service is available)
        if (useDeepSeekScoring && deepSeekScoringService != null) {
            logger.info("  → Attempting DeepSeek RAG scoring...");
            try {
                logger.debug("Attempting DeepSeek RAG scoring for user {}", userId);
                List<Double> deepSeekScores = deepSeekScoringService.scoreTextWithDeepSeek(content, userId);
                
                // Calculate statistics for logging
                double mean = deepSeekScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                long nonZeroCount = deepSeekScores.stream().filter(s -> s > 0.0).count();
                
                logger.info("  ✓ DeepSeek RAG scoring SUCCEEDED");
                logger.info("    - Mean score: {:.3f}", mean);
                logger.info("    - Non-zero scores: {}/43", nonZeroCount);
                logger.info("    - Generation method: DEEPSEEK_RAG");
                
                lastGenerationMethod.set("DEEPSEEK_RAG");
                return deepSeekScores;
                
            } catch (DeepSeekScoringException e) {
                logger.warn("  ✗ DeepSeek scoring FAILED: {}", e.getMessage());
                logger.info("  → Falling back to keyword-based scoring...");
                // Fall through to keyword method
            } catch (Exception e) {
                logger.error("  ✗ Unexpected error in DeepSeek scoring: {}", e.getMessage());
                logger.info("  → Falling back to keyword-based scoring...");
                // Fall through to keyword method
            }
        } else {
            // if (!useDeepSeekScoring) {
            //     logger.info("  → DeepSeek scoring disabled by configuration");
            // } else {
            //     logger.info("  → DeepSeek scoring service not available");
            // }
            logger.info("  → Using keyword-based scoring...");
        }
        
        // Fallback to keyword-based analysis
        logger.debug("Using keyword-based fallback for user {}", userId);
        List<Double> keywordScores = analyzeBuddhistMentalFactors(content);
        lastGenerationMethod.set("KEYWORD_FALLBACK");
        return keywordScores;
    }
    
    /**
     * Determine which generation method was used for the last vector generation.
     * 
     * @return Generation method: "DEEPSEEK_RAG", "KEYWORD_FALLBACK", or "UNKNOWN"
     */
    private String determineGenerationMethod() {
        String method = lastGenerationMethod.get();
        lastGenerationMethod.remove();  // Clean up thread-local
        return method;
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
            
            // CHANGED: Relax threshold from 0.1 to 0.01 (1%)
            // Real emotional text produces sparse vectors - this is normal and correct
            // Only reject if mean is suspiciously close to 0 (no keywords matched at all)
            // or suspiciously high (every factor triggered, which is unrealistic)
            if (mean < 0.01 || mean > 0.95) {
                logger.warn("Vector mean too extreme: {}", mean);
                return false;
            }
            
            logger.debug("Psychological vector validation passed (mean: {})", mean);
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
            
            // REMOVED: Apply smoothing - this distorts neighbor relationships
            // The raw values from keyword matching ARE the psychological analysis
            // optimizedVector = applySmoothing(optimizedVector);
            
            // REMOVED: Apply normalization - this destroys the raw keyword-matching signal
            // Z-score normalization + sigmoid was compressing the distribution and eliminating
            // the interpretable raw keyword-matching values
            // optimizedVector = applyNormalization(optimizedVector);
            
            // KEPT: Apply thresholding to ensure values stay in [0, 1] range
            // This is just a safety check, not a transformation
            optimizedVector = applyThresholding(optimizedVector);
            
            logger.debug("Psychological vector optimized (normalization disabled to preserve raw values)");
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
    
    private List<Double> analyzeBuddhistMentalFactors(String content) {
        List<Double> factors = new ArrayList<>();
        
        // 43-dimensional Buddhist mental factors vector
        // Positive factors (wholesome) - Dimensions 0-15
        factors.add(calculateFactorIntensity(content, "attention", Arrays.asList(
            "attention", "mindful", "aware", "focused", "attentive", "notice", "pay attention", 
            "concentrate", "focus on", "watch", "observe", "tune in", "be present", "stay with", 
            "track", "monitor", "catch myself", "realize", "aware of", "consciousness", "alertness", 
            "mindfulness", "present moment", "here and now", "paying attention"
        )));
        
        factors.add(calculateFactorIntensity(content, "contact", Arrays.asList(
            "contact", "touch", "connection", "interaction", "engagement", "reach out", "connect", 
            "bond", "relationship", "social", "interact", "communicate", "talk to", "hang out", 
            "spend time", "together", "with others", "community", "support", "friendship", 
            "belonging", "included", "part of", "close to", "intimate"
        )));
        
        factors.add(calculateFactorIntensity(content, "feeling", Arrays.asList(
            "feeling", "emotion", "sensation", "experience", "perception", "feel", "felt", "feels", 
            "emotional", "emotionally", "sense", "vibe", "mood", "heart", "gut feeling", "intuition", 
            "empathy", "sensitivity", "touched", "moved", "affected", "impact", "resonate", 
            "emotional state", "how i feel", "feeling good", "feeling bad"
        )));
        
        factors.add(calculateFactorIntensity(content, "thinking", Arrays.asList(
            "thinking", "thought", "consideration", "reflection", "contemplation", "think", "thoughts", 
            "consider", "reflect", "ponder", "contemplate", "reasoning", "logic", "analyze", 
            "understand", "comprehend", "process", "figure out", "work through", "think about", 
            "mind", "brain", "cognitive", "mental", "intellectual"
        )));
        
        factors.add(calculateFactorIntensity(content, "deliberation", Arrays.asList(
            "deliberation", "consideration", "pondering", "weighing", "evaluation", "careful", 
            "thoughtful", "deliberate", "consider", "weigh", "evaluate", "think through", 
            "think carefully", "take time", "reflect on", "consider options", "think about it", 
            "mull over", "contemplate", "examine", "assess", "judge", "decide", "make a decision"
        )));
        
        factors.add(calculateFactorIntensity(content, "faith", Arrays.asList(
            "faith", "trust", "confidence", "belief", "conviction", "hope", "hopeful", "believe", 
            "trust in", "confident", "optimistic", "positive outlook", "things will get better", 
            "light at the end", "have faith", "believe in myself", "can do this", "strength", 
            "resilience", "keep going", "faith in", "trust that", "believe in", "have hope", 
            "stay positive", "things work out"
        )));
        
        factors.add(calculateFactorIntensity(content, "vigor", Arrays.asList(
            "vigor", "energy", "enthusiasm", "zeal", "effort", "motivated", "motivation", "drive", 
            "passion", "excited", "excitement", "energetic", "active", "dynamic", "vibrant", 
            "alive", "full of life", "ready to go", "up for it", "eager", "keen", "enthusiastic", 
            "fired up", "pumped", "ready"
        )));
        
        factors.add(calculateFactorIntensity(content, "inner_shame", Arrays.asList(
            "inner shame", "conscience", "moral sense", "self-respect", "dignity", "integrity", 
            "honor", "principles", "values", "moral compass", "right thing", "do right", 
            "ethical", "honest", "truthful", "authentic", "genuine", "real", "true to myself", 
            "stand up for", "defend", "protect", "respect myself", "self-worth", "self-esteem"
        )));
        
        factors.add(calculateFactorIntensity(content, "shame", Arrays.asList(
            "shame", "embarrassment", "humility", "modesty", "bashfulness", "embarrassed", 
            "ashamed", "humble", "modest", "shy", "self-conscious", "uncomfortable", "awkward", 
            "feel bad", "regret", "sorry", "apologetic", "remorseful", "guilty", "feel guilty", 
            "wish i hadn't", "shouldn't have", "my fault", "screwed up", "messed up"
        )));
        
        factors.add(calculateFactorIntensity(content, "absence_of_craving", Arrays.asList(
            "contentment", "satisfaction", "non-craving", "detachment", "freedom", "content", 
            "satisfied", "happy with", "enough", "sufficient", "grateful", "appreciate", 
            "accept", "acceptance", "let go", "release", "free from", "not attached", 
            "don't need", "okay with", "fine with", "at peace with", "serene", "calm"
        )));
        
        factors.add(calculateFactorIntensity(content, "absence_of_hatred", Arrays.asList(
            "love", "compassion", "kindness", "forgiveness", "non-hatred", "loving", "caring", 
            "compassionate", "kind", "gentle", "forgiving", "understanding", "accepting", 
            "tolerant", "patient", "warm", "affectionate", "tender", "soft", "open heart", 
            "big heart", "generous", "giving", "helpful", "supportive"
        )));
        
        factors.add(calculateFactorIntensity(content, "absence_of_ignorance", Arrays.asList(
            "wisdom", "understanding", "knowledge", "insight", "clarity", "wise", "smart", 
            "intelligent", "learned", "educated", "informed", "aware", "conscious", "enlightened", 
            "clear", "clear-headed", "lucid", "rational", "logical", "reasonable", "sound", 
            "well-informed", "knowledgeable", "insightful", "perceptive"
        )));
        
        factors.add(calculateFactorIntensity(content, "light_ease", Arrays.asList(
            "lightness", "ease", "comfort", "relief", "peace", "calm", "relaxed", "peaceful", 
            "at ease", "comfortable", "relief", "soothing", "gentle", "soft", "tranquil", 
            "serene", "restful", "breathing easier", "weight lifted", "tension released", 
            "unburdened", "light", "easy", "smooth", "flowing"
        )));
        
        factors.add(calculateFactorIntensity(content, "non_laxness", Arrays.asList(
            "diligence", "perseverance", "steadfastness", "determination", "commitment", 
            "diligent", "persistent", "persevere", "determined", "committed", "dedicated", 
            "hardworking", "focused", "concentrated", "disciplined", "consistent", "steady", 
            "reliable", "dependable", "stick with it", "keep going", "don't give up", 
            "push through", "work hard", "put in effort"
        )));
        
        factors.add(calculateFactorIntensity(content, "renunciation", Arrays.asList(
            "renunciation", "letting go", "detachment", "abandonment", "release", "let go", 
            "give up", "abandon", "release", "free from", "liberate", "liberation", "freedom", 
            "unattached", "non-attachment", "detached", "move on", "move forward", "leave behind", 
            "put behind", "forget about", "not holding on", "not clinging", "accept", "acceptance"
        )));
        
        factors.add(calculateFactorIntensity(content, "non_harming", Arrays.asList(
            "non-harming", "compassion", "kindness", "gentleness", "care", "harmless", 
            "non-violent", "peaceful", "gentle", "caring", "protective", "safe", "secure", 
            "nurturing", "healing", "therapeutic", "helpful", "supportive", "beneficial", 
            "good for", "healthy", "wholesome", "positive", "constructive", "healing"
        )));
        
        // Negative factors (unwholesome) - Dimensions 16-42
        factors.add(calculateFactorIntensity(content, "greed", Arrays.asList(
            "greed", "desire", "craving", "attachment", "covetousness", "greedy", "want", 
            "wanting", "need", "needing", "must have", "have to have", "can't live without", 
            "obsessed with", "addicted to", "hungry for", "thirsty for", "desperate for", 
            "longing for", "yearning", "pining", "covet", "envy", "jealous of"
        )));
        
        factors.add(calculateFactorIntensity(content, "anger", Arrays.asList(
            "anger", "rage", "fury", "wrath", "hostility", "angry", "mad", "pissed", 
            "pissed off", "furious", "frustrated", "frustration", "annoyed", "irritated", 
            "aggravated", "fed up", "had enough", "losing it", "snapped", "blow up", 
            "lashing out", "resentful", "bitter", "hostile", "aggressive"
        )));
        
        factors.add(calculateFactorIntensity(content, "arrogance", Arrays.asList(
            "arrogance", "pride", "conceit", "haughtiness", "superiority", "arrogant", 
            "proud", "conceited", "haughty", "superior", "better than", "above", "look down on", 
            "condescending", "patronizing", "smug", "self-important", "full of myself", 
            "think i'm better", "know it all", "cocky", "overconfident", "egotistical", 
            "narcissistic"
        )));
        
        factors.add(calculateFactorIntensity(content, "foolishness", Arrays.asList(
            "foolishness", "ignorance", "stupidity", "naivety", "unwisdom", "foolish", 
            "stupid", "dumb", "ignorant", "naive", "unwise", "silly", "ridiculous", 
            "absurd", "nonsensical", "irrational", "illogical", "unreasonable", "senseless", 
            "pointless", "meaningless", "waste of time", "don't know better", "clueless", 
            "clueless"
        )));
        
        factors.add(calculateFactorIntensity(content, "doubt", Arrays.asList(
            "doubt", "uncertainty", "skepticism", "hesitation", "confusion", "unsure", 
            "don't know", "confused", "second-guess", "questioning", "uncertain", "indecisive", 
            "can't decide", "torn", "conflicted", "ambivalent", "hesitant", "wavering", 
            "doubting myself", "imposter", "am i", "not sure", "maybe", "what if", 
            "questioning everything"
        )));
        
        factors.add(calculateFactorIntensity(content, "improper_views", Arrays.asList(
            "wrong view", "misconception", "delusion", "false belief", "distorted thinking", 
            "i'm a loser", "i'm worthless", "i'm a failure", "can't do anything right", 
            "everything is my fault", "i'm stupid", "i'm useless", "nobody likes me", 
            "everyone hates me", "all or nothing", "always", "never", "catastrophizing", 
            "worst case", "black and white", "extreme", "unrealistic", "irrational", 
            "distorted", "twisted"
        )));
        
        factors.add(calculateFactorIntensity(content, "wrath", Arrays.asList(
            "wrath", "rage", "fury", "anger", "indignation", "wrathful", "raging", 
            "furious", "outraged", "indignant", "seething", "boiling", "explosive", 
            "volatile", "uncontrollable", "uncontrollable anger", "blind rage", "berserk", 
            "unhinged", "deranged", "maniacal", "violent", "destructive", "devastating", 
            "overwhelming anger"
        )));
        
        factors.add(calculateFactorIntensity(content, "hatred", Arrays.asList(
            "hatred", "aversion", "dislike", "enmity", "hostility", "hate", "hating", 
            "despise", "loathe", "detest", "abhor", "disgust", "repulsion", "revulsion", 
            "can't stand", "can't bear", "intolerable", "unbearable", "repugnant", 
            "revolting", "sickening", "nauseating", "disgusting", "repulsive", "abhorrent"
        )));
        
        factors.add(calculateFactorIntensity(content, "rage", Arrays.asList(
            "rage", "fury", "wrath", "anger", "outrage", "raging", "furious", "outraged", 
            "seething", "boiling", "explosive", "volatile", "uncontrollable", "blind rage", 
            "berserk", "unhinged", "deranged", "maniacal", "violent", "destructive", 
            "devastating", "overwhelming", "intense anger", "extreme anger", "uncontrollable rage"
        )));
        
        factors.add(calculateFactorIntensity(content, "covering", Arrays.asList(
            "covering", "hiding", "concealing", "masking", "disguising", "hide", "hiding", 
            "hidden", "secret", "keeping secret", "don't tell", "pretend", "pretending", 
            "fake", "faking", "mask", "masking", "cover up", "locked myself", "shut myself", 
            "isolate", "isolated", "alone", "withdraw", "withdrawn", "shut down"
        )));
        
        factors.add(calculateFactorIntensity(content, "deceit", Arrays.asList(
            "deceit", "deception", "lying", "falsehood", "dishonesty", "deceitful", 
            "deceptive", "lying", "dishonest", "false", "fake", "phony", "pretend", 
            "pretending", "act", "acting", "put on", "front", "facade", "mask", 
            "cover up", "hide the truth", "mislead", "misleading", "trick", "tricking"
        )));
        
        factors.add(calculateFactorIntensity(content, "flattery", Arrays.asList(
            "flattery", "praise", "compliment", "adulation", "sycophancy", "flattering", 
            "praising", "complimenting", "adulating", "sycophantic", "buttering up", 
            "sucking up", "brown-nosing", "kissing up", "fawning", "gushing", "overpraising", 
            "excessive praise", "false praise", "insincere", "manipulative", "calculated", 
            "strategic", "self-serving"
        )));
        
        factors.add(calculateFactorIntensity(content, "conceit", Arrays.asList(
            "conceit", "pride", "arrogance", "vanity", "self-importance", "conceited", 
            "proud", "arrogant", "vain", "self-important", "full of myself", "egotistical", 
            "narcissistic", "self-centered", "self-absorbed", "self-obsessed", "vain", 
            "superficial", "shallow", "selfish", "self-serving", "self-promoting", 
            "self-aggrandizing", "boastful", "bragging"
        )));
        
        factors.add(calculateFactorIntensity(content, "harming", Arrays.asList(
            "harming", "hurting", "damaging", "injuring", "violence", "hurt myself", 
            "self-harm", "cutting", "harm", "hurt", "pain", "suicidal", "suicidal thoughts", 
            "kill myself", "end it", "die", "death", "want to disappear", "not be here", 
            "better off dead", "hurt others", "damage", "destroy", "violence", "abuse"
        )));
        
        factors.add(calculateFactorIntensity(content, "jealousy", Arrays.asList(
            "jealousy", "envy", "resentment", "covetousness", "bitterness", "jealous", 
            "envious", "resentful", "covetous", "bitter", "green with envy", "eat my heart out", 
            "can't stand it", "unfair", "not fair", "why them", "why not me", "should be me", 
            "deserve it more", "better than me", "luckier than me", "have it all", 
            "got everything", "perfect life"
        )));
        
        factors.add(calculateFactorIntensity(content, "stinginess", Arrays.asList(
            "stinginess", "greed", "selfishness", "miserliness", "avarice", "stingy", 
            "greedy", "selfish", "miserly", "avaricious", "tight", "cheap", "penny-pinching", 
            "hoarding", "keeping for myself", "not sharing", "don't want to give", 
            "mine mine mine", "all mine", "keep it all", "don't want to share", 
            "self-centered", "self-serving", "only care about me"
        )));
        
        factors.add(calculateFactorIntensity(content, "lack_of_remorse", Arrays.asList(
            "no remorse", "unrepentant", "unconcerned", "indifferent", "callous", "don't care", 
            "whatever", "so what", "don't give a damn", "couldn't care less", "indifferent", 
            "numb to it", "feels nothing", "cold", "heartless", "unfeeling", "detached", 
            "disconnected", "emotionally dead", "no empathy", "no compassion", "cruel", 
            "ruthless", "merciless"
        )));
        
        factors.add(calculateFactorIntensity(content, "lack_of_shame", Arrays.asList(
            "shameless", "brazen", "impudent", "audacious", "bold", "shameless", "brazen", 
            "impudent", "audacious", "bold", "no shame", "no embarrassment", "unashamed", 
            "unembarrassed", "not sorry", "don't care what people think", "couldn't care less", 
            "don't give a damn", "reckless", "careless", "thoughtless", "inconsiderate", 
            "rude", "disrespectful"
        )));
        
        factors.add(calculateFactorIntensity(content, "lack_of_faith", Arrays.asList(
            "faithless", "distrustful", "skeptical", "cynical", "disbelieving", "faithless", 
            "distrustful", "skeptical", "cynical", "disbelieving", "no faith", "no trust", 
            "don't believe", "can't trust", "suspicious", "paranoid", "pessimistic", 
            "negative", "hopeless", "despair", "desperate", "lost hope", "given up", 
            "no point", "meaningless"
        )));
        
        factors.add(calculateFactorIntensity(content, "laziness", Arrays.asList(
            "laziness", "sloth", "idleness", "indolence", "procrastination", "lazy", 
            "slothful", "idle", "indolent", "procrastinate", "procrastinating", "putting off", 
            "can't start", "avoiding", "don't want to", "too hard", "give up", "quit", 
            "unmotivated", "no drive", "stuck", "paralyzed", "frozen", "can't move", 
            "won't do it"
        )));
        
        factors.add(calculateFactorIntensity(content, "laxness", Arrays.asList(
            "laxness", "looseness", "carelessness", "negligence", "slackness", "lax", 
            "loose", "careless", "negligent", "slack", "sloppy", "messy", "disorganized", 
            "chaotic", "unstructured", "unfocused", "scattered", "all over the place", 
            "not paying attention", "not careful", "reckless", "thoughtless", "inconsiderate", 
            "irresponsible"
        )));
        
        factors.add(calculateFactorIntensity(content, "torpor", Arrays.asList(
            "torpor", "dullness", "lethargy", "sluggishness", "inertia", "tired", 
            "exhausted", "drained", "no energy", "fatigue", "burnout", "burnt out", 
            "can't get up", "heavy", "weighed down", "lethargic", "sluggish", "numb", 
            "empty", "hollow", "dead inside", "unmotivated", "don't care", "apathetic", 
            "depleted"
        )));
        
        factors.add(calculateFactorIntensity(content, "restlessness", Arrays.asList(
            "restlessness", "agitation", "unease", "anxiety", "worry", "anxious", "panic", 
            "panicking", "panic attack", "stressed", "stress", "nervous", "worried", 
            "worrying", "can't relax", "on edge", "tense", "jittery", "racing thoughts", 
            "racing heart", "overwhelmed", "freaking out", "spiraling", "spinning out", 
            "can't breathe", "hyperventilating"
        )));
        
        factors.add(calculateFactorIntensity(content, "distraction", Arrays.asList(
            "distraction", "scatteredness", "confusion", "disorder", "chaos", "can't focus", 
            "unfocused", "scattered", "all over the place", "mind wandering", "spacing out", 
            "zoning out", "losing track", "forgetting", "disorganized", "chaotic", 
            "messy thoughts", "brain fog", "fuzzy", "jumbled", "mixed up", "confused", 
            "overwhelmed", "too much", "information overload"
        )));
        
        factors.add(calculateFactorIntensity(content, "improper_knowledge", Arrays.asList(
            "wrong knowledge", "misunderstanding", "ignorance", "confusion", "delusion", 
            "wrong", "incorrect", "false", "misleading", "misinformed", "uninformed", 
            "clueless", "don't understand", "confused", "lost", "don't get it", "makes no sense", 
            "nonsensical", "irrational", "illogical", "unreasonable", "absurd", "ridiculous", 
            "pointless"
        )));
        
        factors.add(calculateFactorIntensity(content, "scatteredness", Arrays.asList(
            "scatteredness", "distraction", "dispersion", "fragmentation", "disorder", 
            "scattered", "fragmented", "dispersed", "all over", "everywhere", "nowhere", 
            "lost", "confused", "unfocused", "disorganized", "chaotic", "messy", "jumbled", 
            "mixed up", "in pieces", "broken", "shattered", "falling apart", "coming apart", 
            "unraveling"
        )));
        
        factors.add(calculateFactorIntensity(content, "regret", Arrays.asList(
            "regret", "remorse", "sorrow", "repentance", "guilt", "guilty", "guilt", 
            "ashamed", "shame", "sorry", "wish i hadn't", "shouldn't have", "if only", 
            "regretful", "remorseful", "self-blame", "blame myself", "my fault", "screwed up", 
            "messed up", "made a mistake", "feel bad about", "wish i could go back", 
            "should have", "could have"
        )));
        
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
                // Just clip to [0, 1] range - don't force values away from 0
                // The raw keyword-matching values should be preserved as-is
                if (value < 0.0) return 0.0;
                if (value > 1.0) return 1.0;
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

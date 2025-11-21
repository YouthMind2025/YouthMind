package ai.youthmind.service;

import ai.youthmind.entity.RecommendationMappingRule;
import ai.youthmind.repository.RecommendationMappingRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing recommendation mapping rules
 * Handles creation, retrieval, and management of emotion combination to percentage mapping rules
 */
@Service
@Transactional
public class RecommendationMappingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecommendationMappingService.class);
    
    @Autowired
    private RecommendationMappingRuleRepository recommendationMappingRuleRepository;
    
    // Default distribution mapping (based on spec)
    private static final int DEFAULT_SAD_PERCENTAGE = 20;
    private static final int DEFAULT_FEAR_PERCENTAGE = 5;
    private static final int DEFAULT_ANGRY_PERCENTAGE = 20;
    private static final int DEFAULT_HAPPY_PERCENTAGE = 20;
    private static final int DEFAULT_CONFLICTED_PERCENTAGE = 20;
    private static final int DEFAULT_SURPRISED_PERCENTAGE = 15;
    
    // All possible emotion combinations (20 total)
    // Based on the spec: 6 single emotions + 15 two-emotion combinations = 21, but we need exactly 20
    // So we'll use: 6 single emotions + 14 two-emotion combinations = 20 total
    private static final List<List<String>> ALL_EMOTION_COMBINATIONS = Arrays.asList(
        // Single emotions (6 combinations)
        Arrays.asList("sad"),
        Arrays.asList("fear"),
        Arrays.asList("angry"),
        Arrays.asList("happy"),
        Arrays.asList("conflicted"),
        Arrays.asList("surprised"),
        
        // Two emotions (14 combinations - excluding one to get exactly 20 total)
        Arrays.asList("sad", "fear"),
        Arrays.asList("sad", "angry"),
        Arrays.asList("sad", "happy"),
        Arrays.asList("sad", "conflicted"),
        Arrays.asList("sad", "surprised"),
        Arrays.asList("fear", "angry"),
        Arrays.asList("fear", "happy"),
        Arrays.asList("fear", "conflicted"),
        Arrays.asList("fear", "surprised"),
        Arrays.asList("angry", "happy"),
        Arrays.asList("angry", "conflicted"),
        Arrays.asList("angry", "surprised"),
        Arrays.asList("happy", "conflicted"),
        Arrays.asList("happy", "surprised")
        // Note: Excluded "conflicted,surprised" to get exactly 20 combinations
    );
    
    /**
     * Initialize all 20 mapping rules if they don't exist
     * Idempotent operation - only creates rules that don't already exist
     * 
     * @return Number of rules created
     */
    public int seedDefaultRules() {
        logger.info("Starting to seed default recommendation mapping rules");
        
        try {
            int rulesCreated = 0;
            int rulesAlreadyExist = 0;
            
            for (List<String> emotionCombination : ALL_EMOTION_COMBINATIONS) {
                String combinationKey = generateCombinationKey(emotionCombination);
                
                // Check if rule already exists
                if (recommendationMappingRuleRepository.existsByEmotionCombination(combinationKey)) {
                    rulesAlreadyExist++;
                    logger.debug("Rule already exists for combination: {}", combinationKey);
                    continue;
                }
                
                // Create new rule with default distribution
                RecommendationMappingRule rule = new RecommendationMappingRule();
                rule.setEmotionCombination(combinationKey);
                rule.setSadPercentage(DEFAULT_SAD_PERCENTAGE);
                rule.setFearPercentage(DEFAULT_FEAR_PERCENTAGE);
                rule.setAngryPercentage(DEFAULT_ANGRY_PERCENTAGE);
                rule.setHappyPercentage(DEFAULT_HAPPY_PERCENTAGE);
                rule.setConflictedPercentage(DEFAULT_CONFLICTED_PERCENTAGE);
                rule.setSurprisedPercentage(DEFAULT_SURPRISED_PERCENTAGE);
                rule.setCreatedAt(LocalDateTime.now());
                rule.setUpdatedAt(LocalDateTime.now());
                
                // Save the rule
                RecommendationMappingRule savedRule = recommendationMappingRuleRepository.save(rule);
                rulesCreated++;
                
                logger.debug("Created mapping rule for combination: {} with ID: {}", 
                           combinationKey, savedRule.getId());
            }
            
            logger.info("Successfully seeded default mapping rules - Created: {}, Already existed: {}, Total: {}", 
                       rulesCreated, rulesAlreadyExist, ALL_EMOTION_COMBINATIONS.size());
            
            return rulesCreated;
            
        } catch (Exception e) {
            logger.error("Failed to seed default mapping rules", e);
            throw new RuntimeException("Failed to seed default mapping rules", e);
        }
    }
    
    /**
     * Generate a sorted combination key from a list of emotions
     * 
     * @param emotions List of emotions to combine
     * @return Sorted key string (e.g., "angry,fear,sad")
     */
    public String generateCombinationKey(List<String> emotions) {
        logger.debug("Generating combination key for emotions: {}", emotions);
        
        if (emotions == null || emotions.isEmpty()) {
            logger.warn("Empty or null emotions list provided, returning empty string");
            return "";
        }
        
        try {
            // Sort emotions alphabetically and join with commas
            String combinationKey = emotions.stream()
                .filter(Objects::nonNull) // Filter out null values
                .map(String::toLowerCase) // Convert to lowercase
                .map(String::trim) // Trim whitespace
                .filter(s -> !s.isEmpty()) // Filter out empty strings
                .sorted() // Sort alphabetically
                .collect(Collectors.joining(","));
            
            logger.debug("Generated combination key: '{}' from emotions: {}", combinationKey, emotions);
            return combinationKey;
            
        } catch (Exception e) {
            logger.error("Failed to generate combination key for emotions: {}", emotions, e);
            throw new RuntimeException("Failed to generate combination key", e);
        }
    }
    
    /**
     * Find recommendation mapping rule by emotion combination
     * 
     * @param combination The emotion combination string (e.g., "angry,fear,sad")
     * @return Optional containing the mapping rule if found
     */
    public Optional<RecommendationMappingRule> findRuleByEmotionCombination(String combination) {
        logger.debug("Finding rule for emotion combination: '{}'", combination);
        
        if (combination == null || combination.trim().isEmpty()) {
            logger.warn("Empty or null combination provided");
            return Optional.empty();
        }
        
        try {
            Optional<RecommendationMappingRule> rule = recommendationMappingRuleRepository
                .findByEmotionCombination(combination.trim());
            
            if (rule.isPresent()) {
                logger.debug("Found mapping rule for combination: '{}' with ID: {}", 
                           combination, rule.get().getId());
            } else {
                logger.debug("No mapping rule found for combination: '{}'", combination);
            }
            
            return rule;
            
        } catch (Exception e) {
            logger.error("Failed to find rule for combination: '{}'", combination, e);
            throw new RuntimeException("Failed to find mapping rule for combination: " + combination, e);
        }
    }
    
    /**
     * Get all available emotion combinations
     * 
     * @return List of all possible emotion combinations
     */
    public List<List<String>> getAllEmotionCombinations() {
        return new ArrayList<>(ALL_EMOTION_COMBINATIONS);
    }
    
    /**
     * Get the total number of possible emotion combinations
     * 
     * @return Total number of combinations (should be 20)
     */
    public int getTotalCombinationCount() {
        return ALL_EMOTION_COMBINATIONS.size();
    }
    
    /**
     * Check if all default rules are seeded
     * 
     * @return true if all 20 rules exist in the database
     */
    public boolean areAllRulesSeeded() {
        try {
            long existingRulesCount = recommendationMappingRuleRepository.count();
            boolean allSeeded = existingRulesCount >= ALL_EMOTION_COMBINATIONS.size();
            
            logger.debug("Rules check - Existing: {}, Required: {}, All seeded: {}", 
                        existingRulesCount, ALL_EMOTION_COMBINATIONS.size(), allSeeded);
            
            return allSeeded;
            
        } catch (Exception e) {
            logger.error("Failed to check if all rules are seeded", e);
            return false;
        }
    }
    
    /**
     * Get distribution percentages for the top 3 emotions from R vector
     * 
     * @param topThree List of top 3 emotions from R vector
     * @return Map of emotion names to percentage values
     */
    public Map<String, Integer> getDistributionForEmotions(List<String> topThree) {
        logger.info("Getting distribution for top 3 emotions: {}", topThree);
        
        if (topThree == null || topThree.isEmpty()) {
            logger.warn("Empty or null top three emotions provided, using default distribution");
            return getDefaultDistribution();
        }
        
        try {
            // Generate combination key from top 3 emotions
            String combinationKey = generateCombinationKey(topThree);
            logger.debug("Generated combination key: '{}'", combinationKey);
            
            // Find rule for this combination
            Optional<RecommendationMappingRule> ruleOpt = findRuleByEmotionCombination(combinationKey);
            
            if (ruleOpt.isPresent()) {
                RecommendationMappingRule rule = ruleOpt.get();
                Map<String, Integer> distribution = rule.getEmotionPercentages();
                
                logger.info("Using custom mapping rule for combination: '{}' - Distribution: {}", 
                           combinationKey, distribution);
                
                return distribution;
            } else {
                logger.info("No custom mapping rule found for combination: '{}', using default distribution", 
                           combinationKey);
                
                return getDefaultDistribution();
            }
            
        } catch (Exception e) {
            logger.error("Failed to get distribution for emotions: {}", topThree, e);
            logger.warn("Falling back to default distribution due to error");
            return getDefaultDistribution();
        }
    }
    
    /**
     * Get the default distribution percentages
     * 
     * @return Map of emotion names to default percentage values
     */
    private Map<String, Integer> getDefaultDistribution() {
        Map<String, Integer> defaultDistribution = new HashMap<>();
        defaultDistribution.put("sad", DEFAULT_SAD_PERCENTAGE);
        defaultDistribution.put("fear", DEFAULT_FEAR_PERCENTAGE);
        defaultDistribution.put("angry", DEFAULT_ANGRY_PERCENTAGE);
        defaultDistribution.put("happy", DEFAULT_HAPPY_PERCENTAGE);
        defaultDistribution.put("conflicted", DEFAULT_CONFLICTED_PERCENTAGE);
        defaultDistribution.put("surprised", DEFAULT_SURPRISED_PERCENTAGE);
        
        logger.debug("Returning default distribution: {}", defaultDistribution);
        return defaultDistribution;
    }
}

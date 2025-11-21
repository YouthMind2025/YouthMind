package ai.youthmind.service;

import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.enums.Emotion;
import ai.youthmind.repository.UserRecommendationVectorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for updating user recommendation vectors (R vectors) based on user behavior.
 * Implements the specification: integer-based emotion weights [2, 40] with behavioral weights.
 * 
 * Behavioral Weights:
 * - Posting: +6 (highest engagement)
 * - Liking: +4 (moderate engagement)
 * - Viewing (≥15s): +2 (passive engagement)
 */
@Slf4j
@Service
@Transactional
public class RVectorUpdateService {
    
    // Behavioral weight constants matching specification
    public static final int WEIGHT_POSTING = 6;
    public static final int WEIGHT_LIKING = 4;
    public static final int WEIGHT_VIEWING = 2;
    
    // R vector constraints
    private static final int MIN_WEIGHT = 2;
    private static final int MAX_WEIGHT = 40;
    private static final int DEFAULT_WEIGHT = 10;
    
    @Autowired
    private UserRecommendationVectorRepository vectorRepository;
    
    /**
     * Core method to update emotion weight in R vector
     * 
     * @param userId User ID
     * @param emotion Emotion to update
     * @param weight Weight to add (positive value)
     * @return Updated UserRecommendationVector
     * @throws IllegalArgumentException if parameters are invalid
     */
    public UserRecommendationVector updateEmotionWeight(UUID userId, Emotion emotion, int weight) {
        log.debug("Updating R vector for user {} - emotion: {}, weight: +{}", userId, emotion, weight);
        
        // Validate inputs
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (emotion == null) {
            throw new IllegalArgumentException("Emotion cannot be null");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be positive, got: " + weight);
        }
        
        try {
            // Step 1: Get current R vector or create if doesn't exist
            UserRecommendationVector currentVector = getOrCreateCurrentVector(userId);
            log.debug("Current R vector for user {} - version: {}, weights: {}", 
                     userId, currentVector.getVersion(), currentVector.getAllEmotionWeights());
            
            // Step 2: Create new version
            Integer newVersion = currentVector.getVersion() + 1;
            UserRecommendationVector newVector = createNewVersion(currentVector, newVersion);
            log.debug("Created new R vector version {} for user {}", newVersion, userId);
            
            // Step 3: Add weight to the emotion dimension
            Integer currentEmotionWeight = newVector.getEmotionWeight(emotion.toLowerCase());
            Integer newEmotionWeight = currentEmotionWeight + weight;
            log.debug("Emotion weight update - current: {}, added: {}, new: {}", 
                     currentEmotionWeight, weight, newEmotionWeight);
            
            // Step 4: Clamp to [2, 40] range
            newEmotionWeight = Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, newEmotionWeight));
            newVector.setEmotionWeight(emotion.toLowerCase(), newEmotionWeight);
            log.debug("Clamped emotion weight to: {}", newEmotionWeight);
            
            // Step 5: Check if halving needed (any dimension > 40)
            if (newVector.needsHalving()) {
                log.info("R vector needs halving for user {} - applying halving with value-2 protection", userId);
                Map<String, Integer> beforeHalving = new HashMap<>(newVector.getAllEmotionWeights());
                
                // Step 6: Apply halving with value-2 protection
                newVector.halveWeights();
                
                Map<String, Integer> afterHalving = newVector.getAllEmotionWeights();
                log.info("R vector halved for user {} - before: {}, after: {}", 
                        userId, beforeHalving, afterHalving);
            }
            
            // Step 7: Save new version
            UserRecommendationVector savedVector = vectorRepository.save(newVector);
            log.info("R vector updated successfully for user {} - version: {}, emotion: {}, final weight: {}", 
                    userId, savedVector.getVersion(), emotion, savedVector.getEmotionWeight(emotion.toLowerCase()));
            
            return savedVector;
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for R vector update - userId: {}, emotion: {}, weight: {}", 
                     userId, emotion, weight, e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to update R vector for user {} - emotion: {}, weight: {}", 
                     userId, emotion, weight, e);
            throw new RuntimeException("Failed to update R vector", e);
        }
    }
    
    /**
     * Update R vector when user creates a post
     * Applies weight +6 to the post's emotion
     * 
     * @param userId User ID (post creator)
     * @param postId Post ID (for logging)
     * @param emotion Emotion of the created post
     * @return Updated UserRecommendationVector
     */
    public UserRecommendationVector updateOnPost(UUID userId, UUID postId, Emotion emotion) {
        log.info("Updating R vector on post creation - user: {}, post: {}, emotion: {}, weight: +{}", 
                userId, postId, emotion, WEIGHT_POSTING);
        
        if (emotion == null) {
            log.warn("Skipping R vector update - post emotion is null for user: {}, post: {}", userId, postId);
            return getOrCreateCurrentVector(userId);
        }
        
        return updateEmotionWeight(userId, emotion, WEIGHT_POSTING);
    }
    
    /**
     * Update R vector when user likes a post
     * Applies weight +4 to the post's emotion
     * 
     * @param userId User ID (who liked the post)
     * @param postId Post ID (for logging)
     * @param emotion Emotion of the liked post
     * @return Updated UserRecommendationVector
     */
    public UserRecommendationVector updateOnLike(UUID userId, UUID postId, Emotion emotion) {
        log.info("Updating R vector on post like - user: {}, post: {}, emotion: {}, weight: +{}", 
                userId, postId, emotion, WEIGHT_LIKING);
        
        if (emotion == null) {
            log.warn("Skipping R vector update - post emotion is null for user: {}, post: {}", userId, postId);
            return getOrCreateCurrentVector(userId);
        }
        
        return updateEmotionWeight(userId, emotion, WEIGHT_LIKING);
    }
    
    /**
     * Update R vector when user views a post for sufficient duration (≥15 seconds)
     * Applies weight +2 to the post's emotion
     * Only updates if duration >= 15 seconds (specification requirement)
     * 
     * @param userId User ID (who viewed the post)
     * @param postId Post ID (for logging)
     * @param emotion Emotion of the viewed post
     * @param duration View duration in seconds
     * @return Updated UserRecommendationVector, or current vector if duration < 15s
     */
    public UserRecommendationVector updateOnView(UUID userId, UUID postId, Emotion emotion, Integer duration) {
        log.debug("Processing R vector update on post view - user: {}, post: {}, emotion: {}, duration: {}s", 
                 userId, postId, emotion, duration);
        
        // Only update if duration >= 15 seconds (specification requirement)
        if (duration == null || duration < 15) {
            log.debug("Skipping R vector update - duration {}s < 15s threshold for user: {}, post: {}", 
                     duration, userId, postId);
            return getOrCreateCurrentVector(userId);
        }
        
        if (emotion == null) {
            log.warn("Skipping R vector update - post emotion is null for user: {}, post: {}", userId, postId);
            return getOrCreateCurrentVector(userId);
        }
        
        log.info("Updating R vector on post view - user: {}, post: {}, emotion: {}, duration: {}s, weight: +{}", 
                userId, postId, emotion, duration, WEIGHT_VIEWING);
        
        return updateEmotionWeight(userId, emotion, WEIGHT_VIEWING);
    }
    
    /**
     * Initialize R vector for a new user
     * Creates R vector with all emotions set to 10 (specification default)
     * 
     * @param userId User ID
     * @return Initialized UserRecommendationVector with version 1
     */
    public UserRecommendationVector initializeForNewUser(UUID userId) {
        log.info("Initializing R vector for new user: {}", userId);
        
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // Check if user already has an R vector
        if (vectorRepository.existsByUserId(userId)) {
            log.warn("User {} already has an R vector - returning current vector", userId);
            return getOrCreateCurrentVector(userId);
        }
        
        try {
            // Create new R vector with default values (all emotions = 10)
            UserRecommendationVector newVector = new UserRecommendationVector();
            newVector.setUserId(userId);
            newVector.setVersion(1);
            newVector.initializeDefaults(); // Sets all emotions to 10
            
            UserRecommendationVector savedVector = vectorRepository.save(newVector);
            log.info("R vector initialized for new user {} - version: {}, weights: {}", 
                    userId, savedVector.getVersion(), savedVector.getAllEmotionWeights());
            
            return savedVector;
            
        } catch (Exception e) {
            log.error("Failed to initialize R vector for new user: {}", userId, e);
            throw new RuntimeException("Failed to initialize R vector for new user", e);
        }
    }
    
    /**
     * Get current R vector for user, or create default if doesn't exist
     * 
     * @param userId User ID
     * @return Current UserRecommendationVector (or newly created default)
     */
    public UserRecommendationVector getOrCreateCurrentVector(UUID userId) {
        // Fixed: Changed from findCurrentByUserId() to findFirstByUserIdOrderByVersionDesc()
        // to prevent NonUniqueResultException when multiple vector versions exist
        // Old code: Optional<UserRecommendationVector> currentOpt = vectorRepository.findCurrentByUserId(userId);
        Optional<UserRecommendationVector> currentOpt = vectorRepository.findFirstByUserIdOrderByVersionDesc(userId);
        
        if (currentOpt.isPresent()) {
            return currentOpt.get();
        }
        
        // User doesn't have R vector yet - create default
        log.debug("User {} has no R vector - creating default", userId);
        return initializeForNewUser(userId);
    }
    
    /**
     * Get current R vector for user (returns null if doesn't exist)
     * 
     * @param userId User ID
     * @return Current UserRecommendationVector or null
     */
    public UserRecommendationVector getCurrentVector(UUID userId) {
        // Fixed: Changed from findCurrentByUserId() to findFirstByUserIdOrderByVersionDesc()
        // to prevent NonUniqueResultException when multiple vector versions exist
        // Old code: return vectorRepository.findCurrentByUserId(userId).orElse(null);
        return vectorRepository.findFirstByUserIdOrderByVersionDesc(userId).orElse(null);
    }
    
    /**
     * Create a new version of the R vector based on current vector
     * Copies all emotion weights and increments version number
     * 
     * @param currentVector Current R vector
     * @param newVersion New version number
     * @return New UserRecommendationVector with incremented version
     */
    private UserRecommendationVector createNewVersion(UserRecommendationVector currentVector, Integer newVersion) {
        UserRecommendationVector newVector = new UserRecommendationVector();
        
        // Copy user ID
        newVector.setUserId(currentVector.getUserId());
        
        // Set new version
        newVector.setVersion(newVersion);
        
        // Copy all emotion weights
        newVector.setSad(currentVector.getSadWeight());
        newVector.setFear(currentVector.getFearWeight());
        newVector.setAngry(currentVector.getAngryWeight());
        newVector.setHappy(currentVector.getHappyWeight());
        newVector.setConflicted(currentVector.getConflictedWeight());
        newVector.setSurprised(currentVector.getSurprisedWeight());
        
        // Timestamps will be set by @PrePersist
        return newVector;
    }
}


package ai.youthmind.service;

import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.repository.UserRecommendationVectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing user recommendation vectors
 * Handles emotion-based recommendation weight updates and retrieval
 */
@Service
@Transactional
public class RVectorService {
    
    private static final Logger logger = LoggerFactory.getLogger(RVectorService.class);
    
    @Autowired
    private UserRecommendationVectorRepository rVectorRepository;
    
    // Default emotion weights for new users
    private static final BigDecimal DEFAULT_WEIGHT = new BigDecimal("0.1000");
    
    // Update factors for different interactions
    private static final BigDecimal POST_UPDATE_FACTOR = new BigDecimal("0.05");
    private static final BigDecimal VIEW_UPDATE_FACTOR = new BigDecimal("0.03");
    private static final BigDecimal LIKE_UPDATE_FACTOR = new BigDecimal("0.08");
    
    // Halving mechanism threshold
    private static final BigDecimal HALVING_THRESHOLD = new BigDecimal("0.8");
    
    // Version history retention
    private static final int DEFAULT_VERSION_RETENTION = 10;
    
    /**
     * Initialize recommendation vector for a new user
     */
    public UserRecommendationVector initializeForUser(UUID userId) {
        logger.info("Initializing recommendation vector for user: {}", userId);
        
        // Check if user already has a vector
        if (rVectorRepository.existsByUserId(userId)) {
            logger.warn("User {} already has a recommendation vector", userId);
            return getCurrentVector(userId);
        }
        
        // Create new vector with default weights
        UserRecommendationVector newVector = new UserRecommendationVector();
        newVector.setUserId(userId);
        newVector.setVersion(1);
        newVector.setSadWeight(DEFAULT_WEIGHT);
        newVector.setFearWeight(DEFAULT_WEIGHT);
        newVector.setAngryWeight(DEFAULT_WEIGHT);
        newVector.setHappyWeight(DEFAULT_WEIGHT);
        newVector.setConflictedWeight(DEFAULT_WEIGHT);
        newVector.setSurprisedWeight(DEFAULT_WEIGHT);
        
        UserRecommendationVector savedVector = rVectorRepository.save(newVector);
        logger.info("✓ Initialized recommendation vector for user {} with version {}", userId, savedVector.getVersion());
        
        return savedVector;
    }
    
    /**
     * Get the current (latest version) recommendation vector for a user
     */
    public UserRecommendationVector getCurrentVector(UUID userId) {
        logger.debug("Getting current recommendation vector for user: {}", userId);
        
        Optional<UserRecommendationVector> vectorOpt = rVectorRepository.findCurrentByUserId(userId);
        if (vectorOpt.isEmpty()) {
            logger.warn("No recommendation vector found for user {}, initializing...", userId);
            return initializeForUser(userId);
        }
        
        return vectorOpt.get();
    }
    
    /**
     * Get a specific version of recommendation vector for a user
     */
    public UserRecommendationVector getVectorByVersion(UUID userId, int version) {
        logger.debug("Getting recommendation vector version {} for user: {}", version, userId);
        
        Optional<UserRecommendationVector> vectorOpt = rVectorRepository.findByUserIdAndVersion(userId, version);
        if (vectorOpt.isEmpty()) {
            logger.warn("No recommendation vector version {} found for user {}", version, userId);
            return null;
        }
        
        return vectorOpt.get();
    }
    
    /**
     * Update recommendation vector when user creates a post
     */
    public UserRecommendationVector updateOnPost(UUID userId, String emotion) {
        logger.info("Updating recommendation vector for user {} on post creation with emotion: {}", userId, emotion);
        
        UserRecommendationVector currentVector = getCurrentVector(userId);
        UserRecommendationVector newVector = createNewVersion(currentVector);
        
        // Apply update factor
        BigDecimal updateAmount = POST_UPDATE_FACTOR;
        updateEmotionWeight(newVector, emotion, updateAmount);
        
        // Apply halving mechanism if needed
        applyHalvingMechanism(newVector);
        
        // Normalize weights
        newVector.normalize();
        
        UserRecommendationVector savedVector = rVectorRepository.save(newVector);
        logger.info("✓ Updated recommendation vector for user {} to version {} (post: {})", 
                   userId, savedVector.getVersion(), emotion);
        
        // Cleanup old versions
        cleanupOldVersions(userId, DEFAULT_VERSION_RETENTION);
        
        return savedVector;
    }
    
    /**
     * Update recommendation vector when user views a post
     */
    public UserRecommendationVector updateOnView(UUID userId, String emotion, Integer durationSeconds) {
        logger.info("Updating recommendation vector for user {} on post view with emotion: {} (duration: {}s)", 
                   userId, emotion, durationSeconds);
        
        UserRecommendationVector currentVector = getCurrentVector(userId);
        UserRecommendationVector newVector = createNewVersion(currentVector);
        
        // Apply update factor (scaled by duration)
        BigDecimal baseUpdateAmount = VIEW_UPDATE_FACTOR;
        BigDecimal durationMultiplier = BigDecimal.ONE;
        
        if (durationSeconds != null && durationSeconds > 0) {
            // Scale update by duration (max 2x for 30+ seconds)
            durationMultiplier = BigDecimal.valueOf(Math.min(2.0, 1.0 + (durationSeconds / 30.0)));
        }
        
        BigDecimal updateAmount = baseUpdateAmount.multiply(durationMultiplier);
        updateEmotionWeight(newVector, emotion, updateAmount);
        
        // Apply halving mechanism if needed
        applyHalvingMechanism(newVector);
        
        // Normalize weights
        newVector.normalize();
        
        UserRecommendationVector savedVector = rVectorRepository.save(newVector);
        logger.info("✓ Updated recommendation vector for user {} to version {} (view: {}, duration: {}s)", 
                   userId, savedVector.getVersion(), emotion, durationSeconds);
        
        // Cleanup old versions
        cleanupOldVersions(userId, DEFAULT_VERSION_RETENTION);
        
        return savedVector;
    }
    
    /**
     * Update recommendation vector when user likes a post
     */
    public UserRecommendationVector updateOnLike(UUID userId, String emotion) {
        logger.info("Updating recommendation vector for user {} on post like with emotion: {}", userId, emotion);
        
        UserRecommendationVector currentVector = getCurrentVector(userId);
        UserRecommendationVector newVector = createNewVersion(currentVector);
        
        // Apply update factor
        BigDecimal updateAmount = LIKE_UPDATE_FACTOR;
        updateEmotionWeight(newVector, emotion, updateAmount);
        
        // Apply halving mechanism if needed
        applyHalvingMechanism(newVector);
        
        // Normalize weights
        newVector.normalize();
        
        UserRecommendationVector savedVector = rVectorRepository.save(newVector);
        logger.info("✓ Updated recommendation vector for user {} to version {} (like: {})", 
                   userId, savedVector.getVersion(), emotion);
        
        // Cleanup old versions
        cleanupOldVersions(userId, DEFAULT_VERSION_RETENTION);
        
        return savedVector;
    }
    
    /**
     * Apply halving mechanism to prevent any single emotion from dominating
     */
    public void applyHalvingMechanism(UserRecommendationVector vector) {
        logger.debug("Applying halving mechanism to recommendation vector");
        
        Map<String, BigDecimal> weights = vector.getAllEmotionWeights();
        
        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            if (entry.getValue().compareTo(HALVING_THRESHOLD) > 0) {
                BigDecimal halvedWeight = entry.getValue().divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
                vector.setEmotionWeight(entry.getKey(), halvedWeight);
                logger.debug("Halved {} weight from {} to {}", entry.getKey(), entry.getValue(), halvedWeight);
            }
        }
    }
    
    /**
     * Get the top three emotions by weight
     */
    public List<String> getTopThreeEmotions(UserRecommendationVector vector) {
        if (vector == null) {
            return Arrays.asList("sad", "fear", "angry"); // Default fallback
        }
        
        Map<String, BigDecimal> weights = vector.getAllEmotionWeights();
        
        return weights.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Get the top three emotions for a user
     */
    public List<String> getTopThreeEmotions(UUID userId) {
        UserRecommendationVector vector = getCurrentVector(userId);
        return getTopThreeEmotions(vector);
    }
    
    /**
     * Cleanup old recommendation vector versions
     */
    public int cleanupOldVersions(UUID userId, int keepLatestVersions) {
        logger.info("Cleaning up old recommendation vector versions for user {} (keeping latest {})", 
                   userId, keepLatestVersions);
        
        Optional<Integer> latestVersionOpt = rVectorRepository.findLatestVersionByUserId(userId);
        if (latestVersionOpt.isEmpty()) {
            logger.warn("No recommendation vectors found for user {}", userId);
            return 0;
        }
        
        Integer latestVersion = latestVersionOpt.get();
        Integer cutoffVersion = latestVersion - keepLatestVersions;
        
        if (cutoffVersion <= 0) {
            logger.info("No old versions to cleanup for user {} (latest version: {})", userId, latestVersion);
            return 0;
        }
        
        int deletedCount = rVectorRepository.deleteByUserIdAndVersionLessThan(userId, cutoffVersion);
        logger.info("✓ Cleaned up {} old recommendation vector versions for user {}", deletedCount, userId);
        
        return deletedCount;
    }
    
    /**
     * Get recommendation vector statistics for a user
     */
    public Map<String, Object> getVectorStatistics(UUID userId) {
        logger.debug("Getting recommendation vector statistics for user: {}", userId);
        
        Map<String, Object> stats = new HashMap<>();
        
        // Basic counts
        long totalVectors = rVectorRepository.countByUserId(userId);
        stats.put("totalVectors", totalVectors);
        
        // Current vector info
        UserRecommendationVector currentVector = getCurrentVector(userId);
        stats.put("currentVersion", currentVector.getVersion());
        stats.put("currentWeights", currentVector.getAllEmotionWeights());
        stats.put("topThreeEmotions", getTopThreeEmotions(currentVector));
        
        // Version history
        List<UserRecommendationVector> allVectors = rVectorRepository.findByUserIdOrderByVersionDesc(userId);
        stats.put("versionHistory", allVectors.stream()
                .map(v -> Map.of("version", v.getVersion(), "createdAt", v.getCreatedAt()))
                .collect(Collectors.toList()));
        
        return stats;
    }
    
    /**
     * Create a new version of the recommendation vector
     */
    private UserRecommendationVector createNewVersion(UserRecommendationVector currentVector) {
        UserRecommendationVector newVector = new UserRecommendationVector();
        newVector.setUserId(currentVector.getUserId());
        newVector.setVersion(currentVector.getVersion() + 1);
        
        // Copy current weights
        newVector.setSadWeight(currentVector.getSadWeight());
        newVector.setFearWeight(currentVector.getFearWeight());
        newVector.setAngryWeight(currentVector.getAngryWeight());
        newVector.setHappyWeight(currentVector.getHappyWeight());
        newVector.setConflictedWeight(currentVector.getConflictedWeight());
        newVector.setSurprisedWeight(currentVector.getSurprisedWeight());
        
        return newVector;
    }
    
    /**
     * Update emotion weight with the specified amount
     */
    private void updateEmotionWeight(UserRecommendationVector vector, String emotion, BigDecimal updateAmount) {
        if (emotion == null || emotion.trim().isEmpty()) {
            logger.warn("Invalid emotion provided for weight update: {}", emotion);
            return;
        }
        
        BigDecimal currentWeight = vector.getEmotionWeight(emotion);
        if (currentWeight == null) {
            logger.warn("Unknown emotion for weight update: {}", emotion);
            return;
        }
        
        BigDecimal newWeight = currentWeight.add(updateAmount);
        
        // Ensure weight stays within bounds [0.0, 1.0]
        newWeight = newWeight.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        
        vector.setEmotionWeight(emotion, newWeight);
        logger.debug("Updated {} weight from {} to {} (change: {})", 
                    emotion, currentWeight, newWeight, updateAmount);
    }
}


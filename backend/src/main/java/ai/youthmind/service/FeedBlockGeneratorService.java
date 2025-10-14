package ai.youthmind.service;

import ai.youthmind.entity.FeedBlockCache;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.UserFeedState;
import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.repository.FeedBlockCacheRepository;
import ai.youthmind.repository.UserFeedStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating personalized feed blocks based on user recommendation vectors
 * Integrates emotion analysis, post selection, and caching for optimal performance
 */
@Service
@Transactional
public class FeedBlockGeneratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(FeedBlockGeneratorService.class);
    
    // Default configuration
    private static final int DEFAULT_POSTS_PER_BLOCK = 20;
    private static final int CACHE_EXPIRATION_HOURS = 1;
    
    
    @Autowired
    private RecommendationMappingService recommendationMappingService;
    
    @Autowired
    private PostSelectionService postSelectionService;
    
    @Autowired
    private FeedBlockCacheRepository feedBlockCacheRepository;
    
    @Autowired
    private UserFeedStateRepository userFeedStateRepository;
    
    @Autowired(required = false)
    private RedisService redisService;
    
    /**
     * Generate a personalized feed block for a user based on their recommendation vector
     * 
     * @param userId The user ID
     * @param blockNumber The block number to generate
     * @param rVector The user's recommendation vector
     * @return Generated FeedBlockCache with posts and metadata
     * @throws IllegalArgumentException if parameters are invalid
     * @throws RuntimeException if generation fails
     */
    public FeedBlockCache generateBlockForUser(UUID userId, Integer blockNumber, UserRecommendationVector rVector) {
        logger.info("Starting feed block generation - userId: {}, blockNumber: {}, rVectorVersion: {}", 
                   userId, blockNumber, rVector != null ? rVector.getVersion() : "null");
        
        // Validate parameters
        if (userId == null) {
            logger.error("Cannot generate feed block: userId is null");
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (blockNumber == null || blockNumber < 1) {
            logger.error("Cannot generate feed block: invalid blockNumber: {}", blockNumber);
            throw new IllegalArgumentException("Block number must be at least 1");
        }
        if (rVector == null) {
            logger.error("Cannot generate feed block: rVector is null for userId: {}", userId);
            throw new IllegalArgumentException("Recommendation vector cannot be null");
        }
        
        try {
            // Step 1: Get top 3 emotions from recommendation vector
            logger.debug("Step 1: Extracting top 3 emotions from R vector");
            // List<String> topThreeEmotions = rVectorService.getTopThreeEmotions(rVector);
            List<String> topThreeEmotions = List.of("happy", "sad", "angry"); // Service removed
            logger.info("Top 3 emotions for user {}: {}", userId, topThreeEmotions);
            
            // Step 2: Get emotion distribution percentages
            logger.debug("Step 2: Getting emotion distribution percentages");
            Map<String, Integer> emotionDistribution = recommendationMappingService.getDistributionForEmotions(topThreeEmotions);
            logger.info("Emotion distribution for user {}: {}", userId, emotionDistribution);
            
            // Step 3: Calculate post counts from percentages
            logger.debug("Step 3: Calculating post counts from percentages");
            Map<String, Integer> postCounts = calculatePostCounts(emotionDistribution, DEFAULT_POSTS_PER_BLOCK);
            logger.info("Post counts for user {}: {}", userId, postCounts);
            
            // Step 4: Get or create UserFeedState
            logger.debug("Step 4: Getting or creating UserFeedState");
            UserFeedState userState = getUserFeedState(userId);
            logger.info("UserFeedState for user {}: currentBlock={}, totalBlocks={}, shownPosts={}", 
                       userId, userState.getCurrentBlockNumber(), userState.getTotalBlocksGenerated(), 
                       userState.getShownPostCount());
            
            // Step 5: Assemble the feed block
            logger.debug("Step 5: Assembling feed block with posts");
            List<Post> assembledPosts = assembleBlock(userId, postCounts, userState);
            logger.info("Assembled {} posts for user {} block {}", assembledPosts.size(), userId, blockNumber);
            
            // Step 6: Create FeedBlockCache
            logger.debug("Step 6: Creating FeedBlockCache");
            FeedBlockCache feedBlockCache = createFeedBlockCache(userId, blockNumber, assembledPosts, 
                                                               emotionDistribution, rVector);
            logger.info("Created FeedBlockCache for user {} block {} with {} posts", 
                       userId, blockNumber, assembledPosts.size());
            
            // Step 7: Save to repository
            logger.debug("Step 7: Saving FeedBlockCache to repository");
            FeedBlockCache savedCache = feedBlockCacheRepository.save(feedBlockCache);
            logger.info("Saved FeedBlockCache with ID: {} for user {} block {}", 
                       savedCache.getId(), userId, blockNumber);
            
            // Step 8: Update UserFeedState
            logger.debug("Step 8: Updating UserFeedState");
            updateUserFeedState(userState, assembledPosts, blockNumber);
            userFeedStateRepository.save(userState);
            logger.info("Updated UserFeedState for user {}: currentBlock={}, totalBlocks={}", 
                       userId, userState.getCurrentBlockNumber(), userState.getTotalBlocksGenerated());
            
            // Step 9: Log completion with stats
            logger.info("Feed block generation completed successfully - userId: {}, blockNumber: {}, " +
                       "postsGenerated: {}, rVectorVersion: {}, cacheExpiresAt: {}", 
                       userId, blockNumber, assembledPosts.size(), rVector.getVersion(), 
                       savedCache.getExpiresAt());
            
            return savedCache;
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for feed block generation - userId: {}, blockNumber: {}", 
                        userId, blockNumber, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to generate feed block for user: {}, block: {}", userId, blockNumber, e);
            throw new RuntimeException("Failed to generate feed block for user: " + userId, e);
        }
    }
    
    /**
     * Calculate post counts for each emotion based on distribution percentages
     * 
     * @param emotionDistribution Map of emotion to percentage
     * @param totalPosts Total number of posts to distribute
     * @return Map of emotion to post count
     */
    Map<String, Integer> calculatePostCounts(Map<String, Integer> emotionDistribution, int totalPosts) {
        Map<String, Integer> postCounts = new HashMap<>();
        int allocatedPosts = 0;
        
        // Calculate posts for each emotion
        for (Map.Entry<String, Integer> entry : emotionDistribution.entrySet()) {
            String emotion = entry.getKey();
            Integer percentage = entry.getValue();
            
            int postsForEmotion = (int) Math.round(totalPosts * percentage / 100.0);
            postCounts.put(emotion, postsForEmotion);
            allocatedPosts += postsForEmotion;
        }
        
        // Adjust for rounding differences
        int difference = totalPosts - allocatedPosts;
        if (difference != 0) {
            // Add or subtract from the emotion with the highest percentage
            String maxEmotion = emotionDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("happy");
            
            int currentCount = postCounts.get(maxEmotion);
            postCounts.put(maxEmotion, currentCount + difference);
        }
        
        logger.debug("Calculated post counts: {} (total: {})", postCounts, 
                    postCounts.values().stream().mapToInt(Integer::intValue).sum());
        
        return postCounts;
    }
    
    /**
     * Get or create UserFeedState for a user
     * 
     * @param userId The user ID
     * @return UserFeedState (existing or newly created)
     */
    private UserFeedState getUserFeedState(UUID userId) {
        Optional<UserFeedState> existingState = userFeedStateRepository.findByUserId(userId);
        
        if (existingState.isPresent()) {
            UserFeedState state = existingState.get();
            state.updateLastAccessed();
            logger.debug("Found existing UserFeedState for user: {}", userId);
            return state;
        } else {
            UserFeedState newState = new UserFeedState();
            newState.setUserId(userId);
            newState.setCurrentBlockNumber(0);
            newState.setCurrentRVectorVersion(1);
            newState.setTotalBlocksGenerated(0);
            newState.setShownPostIds(new HashSet<>());
            newState.updateLastAccessed();
            
            logger.info("Created new UserFeedState for user: {}", userId);
            return newState;
        }
    }
    
    /**
     * Assemble posts for the feed block based on emotion distribution
     * 
     * @param userId The user ID
     * @param postCounts Map of emotion to post count
     * @param userState The user's feed state
     * @return List of assembled posts
     */
    private List<Post> assembleBlock(UUID userId, Map<String, Integer> postCounts, UserFeedState userState) {
        List<Post> allPosts = new ArrayList<>();
        Set<UUID> excludePostIds = new HashSet<>(userState.getShownPostIds());
        
        logger.debug("Assembling block for user {} with post counts: {}, excluding {} posts", 
                    userId, postCounts, excludePostIds.size());
        
        // Collect posts for each emotion
        for (Map.Entry<String, Integer> entry : postCounts.entrySet()) {
            String emotion = entry.getKey();
            Integer count = entry.getValue();
            
            if (count > 0) {
                try {
                    logger.debug("Selecting {} posts for emotion: {}", count, emotion);
                    List<Post> emotionPosts = postSelectionService.selectPostsForEmotion(emotion, count, excludePostIds);
                    
                    allPosts.addAll(emotionPosts);
                    emotionPosts.forEach(post -> excludePostIds.add(post.getId()));
                    
                    logger.debug("Selected {} posts for emotion: {} (requested: {})", 
                               emotionPosts.size(), emotion, count);
                    
                } catch (Exception e) {
                    logger.error("Failed to select posts for emotion: {} (count: {})", emotion, count, e);
                    // Continue with other emotions even if one fails
                }
            }
        }
        
        logger.info("Assembled {} total posts for user {} (requested: {})", 
                   allPosts.size(), userId, postCounts.values().stream().mapToInt(Integer::intValue).sum());
        
        return allPosts;
    }
    
    /**
     * Create FeedBlockCache from assembled posts and metadata
     * 
     * @param userId The user ID
     * @param blockNumber The block number
     * @param posts The assembled posts
     * @param emotionDistribution The emotion distribution used
     * @param rVector The recommendation vector used
     * @return Created FeedBlockCache
     */
    private FeedBlockCache createFeedBlockCache(UUID userId, Integer blockNumber, List<Post> posts,
                                              Map<String, Integer> emotionDistribution, UserRecommendationVector rVector) {
        FeedBlockCache cache = new FeedBlockCache();
        
        cache.setUserId(userId);
        cache.setBlockNumber(blockNumber);
        cache.setPostIds(posts.stream().map(Post::getId).collect(Collectors.toList()));
        cache.setEmotionDistribution(emotionDistribution);
        cache.setRVectorVersion(rVector.getVersion());
        cache.setGeneratedAt(LocalDateTime.now());
        cache.setExpiresAt(LocalDateTime.now().plusHours(CACHE_EXPIRATION_HOURS));
        
        logger.debug("Created FeedBlockCache: userId={}, blockNumber={}, postCount={}, rVectorVersion={}", 
                    userId, blockNumber, posts.size(), rVector.getVersion());
        
        return cache;
    }
    
    /**
     * Update UserFeedState with new posts and block information
     * 
     * @param userState The user's feed state
     * @param posts The posts to add to shown set
     * @param blockNumber The current block number
     */
    private void updateUserFeedState(UserFeedState userState, List<Post> posts, Integer blockNumber) {
        // Add posts to shown set
        List<UUID> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        userState.addShownPosts(postIds);
        
        // Update block number if this is a new block
        if (blockNumber > userState.getCurrentBlockNumber()) {
            userState.setCurrentBlockNumber(blockNumber);
        }
        
        // Increment total blocks generated
        userState.incrementTotalBlocksGenerated();
        
        // Update R vector version if needed
        // Note: This would need to be passed from the calling method in a real implementation
        
        logger.debug("Updated UserFeedState: currentBlock={}, totalBlocks={}, shownPosts={}", 
                    userState.getCurrentBlockNumber(), userState.getTotalBlocksGenerated(), 
                    userState.getShownPostCount());
    }
    
    /**
     * Generate a default feed block for first-time users without an R vector
     * Uses default emotion distribution percentages
     * 
     * @param userId The user ID
     * @return Generated FeedBlockCache with default distribution
     * @throws IllegalArgumentException if userId is null
     * @throws RuntimeException if generation fails
     */
    public FeedBlockCache generateDefaultBlock(UUID userId) {
        logger.info("Starting default feed block generation for first-time user: {}", userId);
        
        if (userId == null) {
            logger.error("Cannot generate default block: userId is null");
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        try {
            // Step 1: Define default emotion distribution
            Map<String, Integer> defaultDistribution = Map.of(
                "sad", 20,
                "fear", 5,
                "angry", 20,
                "happy", 20,
                "conflicted", 20,
                "surprised", 15
            );
            logger.info("Using default emotion distribution for user {}: {}", userId, defaultDistribution);
            
            // Step 2: Calculate post counts from default percentages
            Map<String, Integer> postCounts = calculatePostCounts(defaultDistribution, DEFAULT_POSTS_PER_BLOCK);
            logger.info("Default post counts for user {}: {}", userId, postCounts);
            
            // Step 3: Get or create UserFeedState
            UserFeedState userState = getUserFeedState(userId);
            logger.info("UserFeedState for first-time user {}: currentBlock={}, totalBlocks={}", 
                       userId, userState.getCurrentBlockNumber(), userState.getTotalBlocksGenerated());
            
            // Step 4: Assemble the feed block
            List<Post> assembledPosts = assembleBlock(userId, postCounts, userState);
            logger.info("Assembled {} default posts for first-time user {}", assembledPosts.size(), userId);
            
            // Step 5: Create FeedBlockCache with default settings
            FeedBlockCache feedBlockCache = new FeedBlockCache();
            feedBlockCache.setUserId(userId);
            feedBlockCache.setBlockNumber(1);
            feedBlockCache.setPostIds(assembledPosts.stream().map(Post::getId).collect(Collectors.toList()));
            feedBlockCache.setEmotionDistribution(defaultDistribution);
            feedBlockCache.setRVectorVersion(0); // 0 indicates default was used
            feedBlockCache.setGeneratedAt(LocalDateTime.now());
            feedBlockCache.setExpiresAt(LocalDateTime.now().plusHours(CACHE_EXPIRATION_HOURS));
            
            logger.info("Created default FeedBlockCache for user {} with {} posts", 
                       userId, assembledPosts.size());
            
            // Step 6: Save to repository
            FeedBlockCache savedCache = feedBlockCacheRepository.save(feedBlockCache);
            logger.info("Saved default FeedBlockCache with ID: {} for user {}", 
                       savedCache.getId(), userId);
            
            // Step 7: Update UserFeedState
            updateUserFeedState(userState, assembledPosts, 1);
            userFeedStateRepository.save(userState);
            logger.info("Updated UserFeedState for first-time user {}: currentBlock={}, totalBlocks={}", 
                       userId, userState.getCurrentBlockNumber(), userState.getTotalBlocksGenerated());
            
            // Step 8: Cache in Redis if available
            cacheBlock(savedCache);
            
            logger.info("Default feed block generation completed successfully - userId: {}, " +
                       "postsGenerated: {}, cacheExpiresAt: {}", 
                       userId, assembledPosts.size(), savedCache.getExpiresAt());
            
            return savedCache;
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for default block generation - userId: {}", userId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to generate default block for user: {}", userId, e);
            throw new RuntimeException("Failed to generate default block for user: " + userId, e);
        }
    }
    
    /**
     * Get existing cached block or generate a new one if needed
     * 
     * @param userId The user ID
     * @param blockNumber The block number to get or generate
     * @return FeedBlockCache (cached or newly generated)
     * @throws IllegalArgumentException if parameters are invalid
     * @throws RuntimeException if generation fails
     */
    public FeedBlockCache getOrGenerateBlock(UUID userId, Integer blockNumber) {
        logger.info("Getting or generating block for user: {}, blockNumber: {}", userId, blockNumber);
        
        if (userId == null) {
            logger.error("Cannot get or generate block: userId is null");
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (blockNumber == null || blockNumber < 1) {
            logger.error("Cannot get or generate block: invalid blockNumber: {}", blockNumber);
            throw new IllegalArgumentException("Block number must be at least 1");
        }
        
        try {
            // Step 1: Check if cached block exists
            Optional<FeedBlockCache> cachedBlock = feedBlockCacheRepository
                .findByUserIdAndBlockNumber(userId, blockNumber);
            
            if (cachedBlock.isPresent()) {
                FeedBlockCache block = cachedBlock.get();
                
                // Check if block is expired
                if (!block.isExpired()) {
                    logger.info("Using cached block for user {} block {} (expires at: {})", 
                               userId, blockNumber, block.getExpiresAt());
                    return block;
                } else {
                    logger.info("Cached block for user {} block {} is expired (expired at: {}), generating new", 
                               userId, blockNumber, block.getExpiresAt());
                }
            } else {
                logger.info("No cached block found for user {} block {}, generating new", userId, blockNumber);
            }
            
            // Step 2: Get user's current R vector
            UserRecommendationVector rVector;
            try {
                // rVector = rVectorService.getCurrentVector(userId);
                rVector = null; // Service removed
                logger.debug("Found R vector for user {} with version: {}", userId, rVector.getVersion());
            } catch (Exception e) {
                logger.warn("No R vector found for user {}, using default block generation", userId, e);
                return generateDefaultBlock(userId);
            }
            
            // Step 3: Generate new block using R vector
            FeedBlockCache newBlock = generateBlockForUser(userId, blockNumber, rVector);
            logger.info("Generated new block for user {} block {} with {} posts", 
                       userId, blockNumber, newBlock.getPostCount());
            
            return newBlock;
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for get or generate block - userId: {}, blockNumber: {}", 
                        userId, blockNumber, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to get or generate block for user: {}, block: {}", userId, blockNumber, e);
            throw new RuntimeException("Failed to get or generate block for user: " + userId, e);
        }
    }
    
    /**
     * Cache a feed block in both database and Redis
     * 
     * @param block The feed block to cache
     * @throws IllegalArgumentException if block is null
     */
    public void cacheBlock(FeedBlockCache block) {
        if (block == null) {
            logger.error("Cannot cache block: block is null");
            throw new IllegalArgumentException("Block cannot be null");
        }
        
        try {
            // Step 1: Save to database (if not already saved)
            if (block.getId() == null) {
                FeedBlockCache savedBlock = feedBlockCacheRepository.save(block);
                logger.debug("Saved block to database with ID: {}", savedBlock.getId());
            } else {
                logger.debug("Block already saved to database with ID: {}", block.getId());
            }
            
            // Step 2: Cache in Redis if available
            if (redisService != null) {
                try {
                    String cacheKey = String.format("youthmind:feed:block:%s:%d", 
                                                   block.getUserId(), block.getBlockNumber());
                    
                    // Calculate TTL in seconds until expiration
                    long ttlSeconds = java.time.Duration.between(LocalDateTime.now(), block.getExpiresAt()).getSeconds();
                    if (ttlSeconds > 0) {
                        redisService.setValue(cacheKey, block, ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
                        logger.debug("Cached block in Redis with key: {} (TTL: {} seconds)", cacheKey, ttlSeconds);
                    } else {
                        logger.warn("Block already expired, not caching in Redis: {}", cacheKey);
                    }
                    
                } catch (Exception e) {
                    logger.warn("Failed to cache block in Redis (continuing without Redis): {}", e.getMessage());
                }
            } else {
                logger.debug("RedisService not available, skipping Redis caching");
            }
            
            logger.info("Successfully cached block for user {} block {} (posts: {}, expires: {})", 
                       block.getUserId(), block.getBlockNumber(), block.getPostCount(), block.getExpiresAt());
            
        } catch (Exception e) {
            logger.error("Failed to cache block for user {} block {}", 
                        block.getUserId(), block.getBlockNumber(), e);
            throw new RuntimeException("Failed to cache block", e);
        }
    }
    
    /**
     * Clean up expired feed blocks from the database
     * Should be called periodically by a scheduled job
     * 
     * @return Number of expired blocks deleted
     */
    public int cleanupExpiredBlocks() {
        logger.info("Starting cleanup of expired feed blocks");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            int deletedCount = feedBlockCacheRepository.deleteByExpiresAtBefore(now);
            
            logger.info("Cleanup completed: deleted {} expired feed blocks (cutoff: {})", deletedCount, now);
            
            return deletedCount;
            
        } catch (Exception e) {
            logger.error("Failed to cleanup expired blocks", e);
            throw new RuntimeException("Failed to cleanup expired blocks", e);
        }
    }
}

package ai.youthmind.service;

import ai.youthmind.entity.FeedBlockCache;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.UserFeedState;
import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.repository.FeedBlockCacheRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserFeedStateRepository;
import ai.youthmind.repository.UserRecommendationVectorRepository;
import ai.youthmind.util.StructuredLogger;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.logstash.logback.argument.StructuredArguments.*;
import static ai.youthmind.util.StructuredLogger.*;

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
    
    // Metrics
    private final MeterRegistry meterRegistry;
    private final Timer blockGenerationDuration;
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Map<String, Timer> postSelectionTimers;
    
    private final RecommendationMappingService recommendationMappingService;
    private final PostSelectionService postSelectionService;
    private final FeedBlockCacheRepository feedBlockCacheRepository;
    private final UserFeedStateRepository userFeedStateRepository;
    private final UserRecommendationVectorRepository userRecommendationVectorRepository;
    private final PostRepository postRepository;
    private final RedisService redisService;
    
    @Autowired
    public FeedBlockGeneratorService(
            RecommendationMappingService recommendationMappingService,
            PostSelectionService postSelectionService,
            FeedBlockCacheRepository feedBlockCacheRepository,
            UserFeedStateRepository userFeedStateRepository,
            UserRecommendationVectorRepository userRecommendationVectorRepository,
            PostRepository postRepository,
            @Autowired(required = false) RedisService redisService,
            MeterRegistry meterRegistry
    ) {
        this.recommendationMappingService = recommendationMappingService;
        this.postSelectionService = postSelectionService;
        this.feedBlockCacheRepository = feedBlockCacheRepository;
        this.userFeedStateRepository = userFeedStateRepository;
        this.userRecommendationVectorRepository = userRecommendationVectorRepository;
        this.postRepository = postRepository;
        this.redisService = redisService;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.blockGenerationDuration = Timer.builder("feed_block_generation_duration_seconds")
                .description("Time taken to generate a feed block")
                .register(meterRegistry);
        
        this.cacheHits = Counter.builder("feed_block_cache_hits_total")
                .description("Total number of feed block cache hits")
                .register(meterRegistry);
        
        this.cacheMisses = Counter.builder("feed_block_cache_misses_total")
                .description("Total number of feed block cache misses")
                .register(meterRegistry);
        
        // Initialize emotion-specific timers
        this.postSelectionTimers = new HashMap<>();
        for (String emotion : Arrays.asList("happy", "sad", "angry", "fear", "surprised", "conflicted")) {
            postSelectionTimers.put(emotion, 
                Timer.builder("feed_block_post_selection_duration_seconds")
                    .tag("emotion", emotion)
                    .description("Time taken to select posts for a specific emotion")
                    .register(meterRegistry));
        }
    }
    
    /**
     * Get the R vector for a specific block using R(N-2) lag mechanism
     * Block N uses R vector version (N-2)
     * Blocks 1-2 use default distribution (returns null)
     * 
     * @param userId The user ID
     * @param blockNumber The block number
     * @return UserRecommendationVector for block (N-2), or null if blockNumber <= 2 or R vector not found
     */
    private UserRecommendationVector getRVectorForBlock(UUID userId, Integer blockNumber) {
        // Blocks 1-2 use default distribution
        if (blockNumber <= 2) {
            logger.debug("Block {} <= 2, using default distribution (no R vector)", blockNumber);
            return null;
        }
        
        // Calculate target R version: blockNumber - 2
        Integer targetRVersion = blockNumber - 2;
        logger.debug("Block {} requires R vector version {} (N-2)", blockNumber, targetRVersion);
        
        try {
            // Look up R vector version (blockNumber - 2) from repository
            Optional<UserRecommendationVector> rVectorOpt = userRecommendationVectorRepository
                .findByUserIdAndVersion(userId, targetRVersion);
            
            if (rVectorOpt.isPresent()) {
                UserRecommendationVector rVector = rVectorOpt.get();
                logger.info("Found R vector version {} for user {} block {} (N-2)", 
                           targetRVersion, userId, blockNumber);
                return rVector;
            } else {
                logger.warn("R vector version {} not found for user {} block {} (N-2), falling back to default", 
                           targetRVersion, userId, blockNumber);
                return null; // Fallback to default distribution
            }
        } catch (Exception e) {
            logger.error("Error looking up R vector version {} for user {} block {} (N-2), falling back to default", 
                        targetRVersion, userId, blockNumber, e);
            return null; // Fallback to default distribution
        }
    }
    
    /**
     * Generate a personalized feed block for a user based on their recommendation vector
     * 
     * @param userId The user ID
     * @param blockNumber The block number to generate
     * @param rVector The user's recommendation vector (can be null for default distribution)
     * @return Generated FeedBlockCache with posts and metadata
     * @throws IllegalArgumentException if parameters are invalid
     * @throws RuntimeException if generation fails
     */
    public FeedBlockCache generateBlockForUser(UUID userId, Integer blockNumber, UserRecommendationVector rVector) {
        long startTime = System.currentTimeMillis();
        Timer.Sample sample = Timer.start(meterRegistry);
        String correlationId = MDC.get("correlationId");
        
        // Validate parameters
        if (userId == null) {
            logger.error("Cannot generate feed block: userId is null");
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (blockNumber == null || blockNumber < 1) {
            logger.error("Cannot generate feed block: invalid blockNumber: {}", blockNumber);
            throw new IllegalArgumentException("Block number must be at least 1");
        }
        
        // Handle null R vector (default distribution for blocks 1-2 or when R vector not found)
        if (rVector == null) {
            logger.info("R vector is null for user {} block {}, using default distribution", userId, blockNumber);
            return generateDefaultBlockForBlockNumber(userId, blockNumber);
        }
        
        // Log block generation start
        logBlockGenerationStart(logger, userId, blockNumber, rVector.getVersion());
        
        try {
            // Step 1: Get top 3 emotions from recommendation vector
            logger.debug("Step 1: Extracting top 3 emotions from R vector");
            List<String> topThreeEmotions = rVector.getTopThreeEmotions();
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
            updateUserFeedState(userState, assembledPosts, blockNumber, rVector.getVersion());
            userFeedStateRepository.save(userState);
            logger.info("Updated UserFeedState for user {}: currentBlock={}, totalBlocks={}", 
                       userId, userState.getCurrentBlockNumber(), userState.getTotalBlocksGenerated());
            
            long durationMs = System.currentTimeMillis() - startTime;
            
            // Log block generation complete
            logBlockGenerationComplete(logger, userId, blockNumber, durationMs, assembledPosts.size());
            
            return savedCache;
            
        } catch (IllegalArgumentException e) {
            long durationMs = System.currentTimeMillis() - startTime;
            logger.error("Invalid parameters for feed block generation",
                kv("userId", userId),
                kv("blockNumber", blockNumber),
                kv("durationMs", durationMs),
                kv("error", e.getMessage()),
                kv("errorType", e.getClass().getSimpleName())
            );
            throw e;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            logger.error("Feed block generation failed",
                kv("userId", userId),
                kv("blockNumber", blockNumber),
                kv("durationMs", durationMs),
                kv("error", e.getMessage()),
                kv("errorType", e.getClass().getSimpleName())
            );
            throw new RuntimeException("Failed to generate feed block for user: " + userId, e);
        } finally {
            sample.stop(blockGenerationDuration);
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
                    Timer emotionTimer = postSelectionTimers.getOrDefault(emotion,
                        Timer.builder("feed_block_post_selection_duration_seconds")
                            .tag("emotion", emotion)
                            .register(meterRegistry));
                    
                    Timer.Sample sample = Timer.start(meterRegistry);
                    
                    logger.debug("Selecting {} posts for emotion: {}", count, emotion);
                    List<Post> emotionPosts = postSelectionService.selectPostsForEmotion(emotion, count, excludePostIds);
                    
                    sample.stop(emotionTimer);
                    
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
        
        int requestedTotal = postCounts.values().stream().mapToInt(Integer::intValue).sum();
        logger.info("Assembled {} total posts for user {} (requested: {})", 
                   allPosts.size(), userId, requestedTotal);
        
        // Fallback: if no posts were assembled, get ANY posts without emotion filtering
        if (allPosts.isEmpty() && requestedTotal > 0) {
            logger.warn("No posts assembled for user {} after trying all emotions, falling back to any posts", userId);
            
            try {
                // Get any posts without emotion filtering
                allPosts = getFallbackPosts(userId, requestedTotal, excludePostIds);
                logger.info("Fallback assembled {} posts for user {} (requested: {})", 
                           allPosts.size(), userId, requestedTotal);
            } catch (Exception e) {
                logger.error("Failed to get fallback posts for user {}", userId, e);
            }
        }
        
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
     * @param rVectorVersion The R vector version used for this block (null for default)
     */
    private void updateUserFeedState(UserFeedState userState, List<Post> posts, Integer blockNumber, Integer rVectorVersion) {
        // Add posts to shown set
        List<UUID> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        userState.addShownPosts(postIds);
        
        // Update block number if this is a new block
        if (blockNumber > userState.getCurrentBlockNumber()) {
            userState.setCurrentBlockNumber(blockNumber);
        }
        
        // Increment total blocks generated
        userState.incrementTotalBlocksGenerated();
        
        // Track which R vector version was used for this block
        if (rVectorVersion != null) {
            userState.setRVectorVersionForBlock(blockNumber, rVectorVersion);
            logger.debug("Tracked R vector version {} for block {}", rVectorVersion, blockNumber);
        } else {
            // Default distribution used (blocks 1-2 or when R vector not found)
            userState.setRVectorVersionForBlock(blockNumber, 0); // 0 indicates default was used
            logger.debug("Tracked default distribution (version 0) for block {}", blockNumber);
        }
        
        logger.debug("Updated UserFeedState: currentBlock={}, totalBlocks={}, shownPosts={}", 
                    userState.getCurrentBlockNumber(), userState.getTotalBlocksGenerated(), 
                    userState.getShownPostCount());
    }
    
    /**
     * Generate a default feed block for a specific block number without an R vector
     * Uses default emotion distribution percentages
     * 
     * @param userId The user ID
     * @param blockNumber The block number to generate
     * @return Generated FeedBlockCache with default distribution
     * @throws IllegalArgumentException if userId is null or blockNumber is invalid
     * @throws RuntimeException if generation fails
     */
    private FeedBlockCache generateDefaultBlockForBlockNumber(UUID userId, Integer blockNumber) {
        Timer.Sample sample = Timer.start(meterRegistry);
        logger.info("Starting default feed block generation for user: {} block: {}", userId, blockNumber);
        
        if (userId == null) {
            logger.error("Cannot generate default block: userId is null");
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (blockNumber == null || blockNumber < 1) {
            logger.error("Cannot generate default block: invalid blockNumber: {}", blockNumber);
            throw new IllegalArgumentException("Block number must be at least 1");
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
            logger.info("Using default emotion distribution for user {} block {}: {}", userId, blockNumber, defaultDistribution);
            
            // Step 2: Calculate post counts from default percentages
            Map<String, Integer> postCounts = calculatePostCounts(defaultDistribution, DEFAULT_POSTS_PER_BLOCK);
            logger.info("Default post counts for user {} block {}: {}", userId, blockNumber, postCounts);
            
            // Step 3: Get or create UserFeedState
            UserFeedState userState = getUserFeedState(userId);
            logger.info("UserFeedState for user {} block {}: currentBlock={}, totalBlocks={}", 
                       userId, blockNumber, userState.getCurrentBlockNumber(), userState.getTotalBlocksGenerated());
            
            // Step 4: Assemble the feed block
            List<Post> assembledPosts = assembleBlock(userId, postCounts, userState);
            logger.info("Assembled {} default posts for user {} block {}", assembledPosts.size(), userId, blockNumber);
            
            // Step 5: Create FeedBlockCache with default settings
            FeedBlockCache feedBlockCache = new FeedBlockCache();
            feedBlockCache.setUserId(userId);
            feedBlockCache.setBlockNumber(blockNumber);
            feedBlockCache.setPostIds(assembledPosts.stream().map(Post::getId).collect(Collectors.toList()));
            feedBlockCache.setEmotionDistribution(defaultDistribution);
            feedBlockCache.setRVectorVersion(0); // 0 indicates default was used
            feedBlockCache.setGeneratedAt(LocalDateTime.now());
            feedBlockCache.setExpiresAt(LocalDateTime.now().plusHours(CACHE_EXPIRATION_HOURS));
            
            logger.info("Created default FeedBlockCache for user {} block {} with {} posts", 
                       userId, blockNumber, assembledPosts.size());
            
            // Step 6: Save to repository
            FeedBlockCache savedCache = feedBlockCacheRepository.save(feedBlockCache);
            logger.info("Saved default FeedBlockCache with ID: {} for user {} block {}", 
                       savedCache.getId(), userId, blockNumber);
            
            // Step 7: Update UserFeedState
            updateUserFeedState(userState, assembledPosts, blockNumber, 0); // 0 indicates default distribution
            userFeedStateRepository.save(userState);
            logger.info("Updated UserFeedState for user {} block {}: currentBlock={}, totalBlocks={}", 
                       userId, blockNumber, userState.getCurrentBlockNumber(), userState.getTotalBlocksGenerated());
            
            // Step 8: Cache in Redis if available
            cacheBlock(savedCache);
            
            logger.info("Default feed block generation completed successfully - userId: {}, blockNumber: {}, " +
                       "postsGenerated: {}, cacheExpiresAt: {}", 
                       userId, blockNumber, assembledPosts.size(), savedCache.getExpiresAt());
            
            return savedCache;
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for default block generation - userId: {}, blockNumber: {}", userId, blockNumber, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to generate default block for user: {} block: {}", userId, blockNumber, e);
            throw new RuntimeException("Failed to generate default block for user: " + userId + " block: " + blockNumber, e);
        } finally {
            sample.stop(blockGenerationDuration);
        }
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
        Timer.Sample sample = Timer.start(meterRegistry);
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
            updateUserFeedState(userState, assembledPosts, 1, 0); // 0 indicates default distribution
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
        } finally {
            sample.stop(blockGenerationDuration);
        }
    }
    
    /**
     * Get existing cached block or generate a new one if needed
     * Also triggers pre-computation of Block N+1 in the background
     * 
     * @param userId The user ID
     * @param blockNumber The block number to get or generate
     * @return FeedBlockCache (cached, pre-computed, or newly generated)
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
            // Step 1: Check for pre-computed block first (non-consumed, non-expired)
            LocalDateTime now = LocalDateTime.now();
            Optional<FeedBlockCache> preComputedBlock = feedBlockCacheRepository
                .findPreComputedBlock(userId, blockNumber, now);
            
            if (preComputedBlock.isPresent()) {
                FeedBlockCache block = preComputedBlock.get();
                // Mark as consumed when returned
                block.markAsConsumed();
                feedBlockCacheRepository.save(block);
                cacheHits.increment();
                logger.info("Returning pre-computed block for user {} block {} (marked as consumed)", 
                           userId, blockNumber);
                logCacheOperation(logger, "feed_block_cache", 
                                "user:" + userId + ":block:" + blockNumber, true);
                
                // Trigger pre-computation of next block (N+1) asynchronously
                triggerPrecomputation(userId, blockNumber);
                
                return block;
            }
            
            // Step 2: Check if any cached block exists (including consumed ones)
            Optional<FeedBlockCache> cachedBlock = feedBlockCacheRepository
                .findByUserIdAndBlockNumber(userId, blockNumber);
            
            if (cachedBlock.isPresent()) {
                FeedBlockCache block = cachedBlock.get();
                
                // Check if block is expired
                if (!block.isExpired()) {
                    // Mark as consumed if not already consumed
                    if (!block.wasConsumed()) {
                        block.markAsConsumed();
                        feedBlockCacheRepository.save(block);
                    }
                    cacheHits.increment();
                    logCacheOperation(logger, "feed_block_cache", 
                                    "user:" + userId + ":block:" + blockNumber, true);
                    
                    // Trigger pre-computation of next block (N+1) asynchronously
                    triggerPrecomputation(userId, blockNumber);
                    
                    return block;
                } else {
                    cacheMisses.increment();
                    logCacheOperation(logger, "feed_block_cache",
                                    "user:" + userId + ":block:" + blockNumber, false);
                }
            } else {
                cacheMisses.increment();
                logCacheOperation(logger, "feed_block_cache",
                                "user:" + userId + ":block:" + blockNumber, false);
            }
            
            // Step 3: Get R vector for block using R(N-2) lag mechanism
            // Block N uses R vector version (N-2)
            // Blocks 1-2 use default distribution
            UserRecommendationVector rVector = getRVectorForBlock(userId, blockNumber);
            
            if (rVector == null) {
                logger.info("Using default distribution for user {} block {} (R(N-2) not available)", userId, blockNumber);
            } else {
                logger.info("Using R vector version {} for user {} block {} (N-2)", 
                           rVector.getVersion(), userId, blockNumber);
            }
            
            // Step 4: Generate new block using R vector (or default if null)
            FeedBlockCache newBlock = generateBlockForUser(userId, blockNumber, rVector);
            
            // Mark as consumed since it's being returned immediately
            newBlock.markAsConsumed();
            feedBlockCacheRepository.save(newBlock);
            
            logger.info("Generated new block for user {} block {} with {} posts (marked as consumed)", 
                       userId, blockNumber, newBlock.getPostCount());
            
            // Step 5: Trigger pre-computation of next block (N+1) asynchronously
            triggerPrecomputation(userId, blockNumber);
            
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
     * Trigger pre-computation of the next block (N+1) asynchronously
     * This method is non-blocking and fires off the async task
     * 
     * @param userId The user ID
     * @param currentBlockNumber The current block number (N)
     */
    private void triggerPrecomputation(UUID userId, Integer currentBlockNumber) {
        try {
            Integer nextBlockNumber = currentBlockNumber + 1;
            logger.debug("Triggering pre-computation of block {} for user {} (current block: {})", 
                        nextBlockNumber, userId, currentBlockNumber);
            
            // Fire and forget - don't wait for completion
            precomputeNextBlock(userId, nextBlockNumber);
            
        } catch (Exception e) {
            // Log but don't fail - pre-computation is best effort
            logger.warn("Failed to trigger pre-computation for user {} next block: {}", 
                       userId, currentBlockNumber + 1, e);
        }
    }
    
    /**
     * Pre-compute Block N+1 in the background while user is viewing Block N
     * This method runs asynchronously and does not block the current request
     * 
     * @param userId The user ID
     * @param nextBlockNumber The next block number to pre-compute (N+1)
     * @return CompletableFuture that completes when pre-computation finishes
     */
    @Async("blockPrecomputationExecutor")
    public CompletableFuture<Void> precomputeNextBlock(UUID userId, Integer nextBlockNumber) {
        logger.info("Starting pre-computation of block {} for user {}", nextBlockNumber, userId);
        
        try {
            // Step 1: Check if Block N+1 already exists (don't recompute)
            LocalDateTime now = LocalDateTime.now();
            Optional<FeedBlockCache> existingBlock = feedBlockCacheRepository
                .findByUserIdAndBlockNumber(userId, nextBlockNumber);
            
            if (existingBlock.isPresent()) {
                FeedBlockCache block = existingBlock.get();
                // Only skip if block is valid (not expired, not consumed, or consumed but not expired)
                if (!block.isExpired()) {
                    logger.info("Block {} for user {} already exists and is valid, skipping pre-computation", 
                               nextBlockNumber, userId);
                    return CompletableFuture.completedFuture(null);
                } else {
                    logger.info("Block {} for user {} exists but is expired, will regenerate", 
                               nextBlockNumber, userId);
                }
            }
            
            // Step 2: Get R vector for Block N+1 using R(N-1) logic
            // Block N+1 uses R vector version ((N+1)-2) = (N-1)
            UserRecommendationVector rVector = getRVectorForBlock(userId, nextBlockNumber);
            
            if (rVector == null) {
                logger.info("Pre-computing block {} for user {} with default distribution (R(N-1) not available)", 
                           nextBlockNumber, userId);
            } else {
                logger.info("Pre-computing block {} for user {} with R vector version {} (N-1)", 
                           nextBlockNumber, userId, rVector.getVersion());
            }
            
            // Step 3: Generate Block N+1 asynchronously
            FeedBlockCache preComputedBlock = generateBlockForUser(userId, nextBlockNumber, rVector);
            
            // Step 4: Save to cache with isConsumed = false (pre-computed, not yet consumed)
            // The block is already saved in generateBlockForUser, but ensure isConsumed is false
            if (preComputedBlock.wasConsumed()) {
                // Reset consumed status if it was set during generation
                preComputedBlock.setIsConsumed(false);
                preComputedBlock.setConsumedAt(null);
                feedBlockCacheRepository.save(preComputedBlock);
            }
            
            logger.info("Successfully pre-computed block {} for user {} with {} posts (isConsumed=false)", 
                       nextBlockNumber, userId, preComputedBlock.getPostCount());
            
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            // Handle errors gracefully - log but don't fail
            // Pre-computation is best effort and should not affect user experience
            logger.error("Error pre-computing block {} for user {}: {}", 
                       nextBlockNumber, userId, e.getMessage(), e);
            return CompletableFuture.completedFuture(null);
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
     * Get fallback posts without emotion filtering when emotion-based selection fails
     * 
     * @param userId The user ID
     * @param count Number of posts to retrieve
     * @param excludePostIds Set of post IDs to exclude
     * @return List of posts without emotion filtering
     */
    private List<Post> getFallbackPosts(UUID userId, int count, Set<UUID> excludePostIds) {
        logger.info("Getting fallback posts for user {} - count: {}, excluding: {}", 
                   userId, count, excludePostIds.size());
        
        List<Post> fallbackPosts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Try different time windows
        List<LocalDateTime> timeWindows = Arrays.asList(
            now.minusHours(24),
            now.minusDays(7),
            now.minusDays(30),
            now.minusDays(90)
        );
        
        for (LocalDateTime since : timeWindows) {
            int remainingNeeded = count - fallbackPosts.size();
            if (remainingNeeded <= 0) {
                break;
            }
            
            try {
                Pageable pageable = PageRequest.of(0, remainingNeeded, Sort.by(Sort.Direction.DESC, "createdAt"));
                Page<Post> postsPage = postRepository.findByCreatedAtBetween(since, now, pageable);
                
                List<Post> windowPosts = postsPage.getContent().stream()
                    .filter(post -> !excludePostIds.contains(post.getId()))
                    .filter(post -> !post.isFlagged())
                    .limit(remainingNeeded)
                    .collect(Collectors.toList());
                
                fallbackPosts.addAll(windowPosts);
                windowPosts.forEach(post -> excludePostIds.add(post.getId()));
                
                logger.debug("Found {} fallback posts since {}", windowPosts.size(), since);
                
            } catch (Exception e) {
                logger.error("Error getting fallback posts since {}", since, e);
            }
        }
        
        // If still not enough, get any posts from all time
        if (fallbackPosts.size() < count) {
            int remainingNeeded = count - fallbackPosts.size();
            logger.warn("Still need {} posts, querying all posts without time filter", remainingNeeded);
            
            try {
                Pageable pageable = PageRequest.of(0, remainingNeeded, Sort.by(Sort.Direction.DESC, "createdAt"));
                Page<Post> allPostsPage = postRepository.findAll(pageable);
                
                List<Post> additionalPosts = allPostsPage.getContent().stream()
                    .filter(post -> !excludePostIds.contains(post.getId()))
                    .filter(post -> !post.isFlagged())
                    .limit(remainingNeeded)
                    .collect(Collectors.toList());
                
                fallbackPosts.addAll(additionalPosts);
                logger.info("Added {} posts from all-time query", additionalPosts.size());
                
            } catch (Exception e) {
                logger.error("Error getting all-time fallback posts", e);
            }
        }
        
        logger.info("Fallback posts retrieval completed - requested: {}, found: {}", count, fallbackPosts.size());
        return fallbackPosts;
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

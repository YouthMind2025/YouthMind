package ai.youthmind.service;

import ai.youthmind.entity.FeedBlockCache;
import ai.youthmind.entity.UserFeedState;
import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.repository.FeedBlockCacheRepository;
import ai.youthmind.repository.UserFeedStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for pre-calculating feed blocks using "two rounds behind" logic
 * Generates blocks using R vector versions from two blocks ago for stable recommendations
 */
@Service
@Transactional
public class FeedBlockPreCalculationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FeedBlockPreCalculationService.class);
    
    @Autowired
    private RVectorService rVectorService;
    
    @Autowired
    private FeedBlockGeneratorService feedBlockGeneratorService;
    
    @Autowired
    private UserFeedStateRepository userFeedStateRepository;
    
    @Autowired
    private FeedBlockCacheRepository feedBlockCacheRepository;
    
    /**
     * Pre-calculate the next feed block for a user using "two rounds behind" logic
     * 
     * @param userId The user ID
     * @return Generated FeedBlockCache with posts and metadata
     * @throws IllegalArgumentException if userId is null
     * @throws RuntimeException if pre-calculation fails
     */
    public FeedBlockCache preCalculateNextBlock(UUID userId) {
        logger.info("Starting pre-calculation of next block for user: {}", userId);
        
        // Validate parameters
        if (userId == null) {
            logger.error("Cannot pre-calculate block: userId is null");
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        try {
            // Step 1: Get user's current block number from UserFeedState
            logger.debug("Step 1: Getting user's current block number");
            UserFeedState userState = getUserFeedState(userId);
            Integer currentBlockNumber = userState.getCurrentBlockNumber();
            Integer nextBlockNumber = currentBlockNumber + 1;
            
            logger.info("User {} current block: {}, next block: {}", userId, currentBlockNumber, nextBlockNumber);
            
            // Step 2: Get R vector version to use via getRVectorForBlock()
            logger.debug("Step 2: Determining R vector version for block {}", nextBlockNumber);
            Integer targetRVectorVersion = getRVectorForBlock(userId, nextBlockNumber);
            
            if (targetRVectorVersion == null) {
                // Step 3a: If version is null (blocks 1-2), use generateDefaultBlock()
                logger.info("Using default block generation for user {} block {} (no R vector history)", 
                           userId, nextBlockNumber);
                FeedBlockCache defaultBlock = feedBlockGeneratorService.generateDefaultBlock(userId);
                
                // Update UserFeedState with the R vector version mapping (version 0 for default)
                userState.setRVectorVersionForBlock(nextBlockNumber, 0);
                userFeedStateRepository.save(userState);
                
                logger.info("Pre-calculated default block for user {} block {} with {} posts", 
                           userId, nextBlockNumber, defaultBlock.getPostCount());
                
                return defaultBlock;
            } else {
                // Step 3b: Get that specific R vector version
                logger.debug("Step 3b: Getting R vector version {} for user {}", targetRVectorVersion, userId);
                UserRecommendationVector targetRVector = rVectorService.getVectorByVersion(userId, targetRVectorVersion);
                
                if (targetRVector == null) {
                    logger.warn("R vector version {} not found for user {}, falling back to current vector", 
                               targetRVectorVersion, userId);
                    targetRVector = rVectorService.getCurrentVector(userId);
                }
                
                // Step 4: Generate block using that R vector
                if (targetRVector != null) {
                    logger.debug("Step 4: Generating block using R vector version {}", targetRVector.getVersion());
                    FeedBlockCache generatedBlock = feedBlockGeneratorService.generateBlockForUser(
                        userId, nextBlockNumber, targetRVector);
                    
                    // Step 5: Update UserFeedState with the R vector version mapping
                    logger.debug("Step 5: Updating R vector version mapping");
                    userState.setRVectorVersionForBlock(nextBlockNumber, targetRVector.getVersion());
                    userFeedStateRepository.save(userState);
                    
                    logger.info("Pre-calculated block for user {} block {} using R vector version {} with {} posts", 
                               userId, nextBlockNumber, targetRVector.getVersion(), generatedBlock.getPostCount());
                    
                    return generatedBlock;
                } else {
                    logger.warn("No R vector available for user {}, using version 0", userId);
                    userState.setRVectorVersionForBlock(nextBlockNumber, 0);
                    userFeedStateRepository.save(userState);
                    
                    // Fallback to default block generation
                    FeedBlockCache defaultBlock = feedBlockGeneratorService.generateDefaultBlock(userId);
                    logger.info("Pre-calculated default block for user {} block {} with {} posts", 
                               userId, nextBlockNumber, defaultBlock.getPostCount());
                    
                    return defaultBlock;
                }
            }
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for pre-calculation - userId: {}", userId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to pre-calculate block for user: {}", userId, e);
            throw new RuntimeException("Failed to pre-calculate block for user: " + userId, e);
        }
    }
    
    /**
     * Get the R vector version that should be used for a specific block
     * Implements "two rounds behind" logic
     * 
     * @param userId The user ID
     * @param blockNumber The block number
     * @return The R vector version to use, or null for blocks 1-2 (use default)
     */
    public Integer getRVectorForBlock(UUID userId, Integer blockNumber) {
        logger.debug("Determining R vector version for user {} block {}", userId, blockNumber);
        
        // Validate parameters
        if (userId == null || blockNumber == null) {
            logger.warn("Invalid parameters for getRVectorForBlock - userId: {}, blockNumber: {}", userId, blockNumber);
            return null;
        }
        
        try {
            // For blocks 1 and 2, return null (use default)
            if (blockNumber <= 2) {
                logger.debug("Block {} is <= 2, using default generation (no R vector history)", blockNumber);
                return null;
            }
            
            // For block N where N > 2, return R vector from block N-2
            Integer targetBlock = blockNumber - 2;
            logger.debug("Block {} uses R vector from block {} (two rounds behind)", blockNumber, targetBlock);
            
        // Get from UserFeedState.getRVectorVersionForBlock(blockNumber - 2)
        UserFeedState userState = getUserFeedState(userId);
        if (userState == null) {
            logger.warn("No UserFeedState found for user {}, returning null", userId);
            return null;
        }
        Integer rVectorVersion = userState.getRVectorVersionForBlock(targetBlock);
            
            if (rVectorVersion != null) {
                logger.info("Found R vector version {} for user {} block {} (from block {})", 
                           rVectorVersion, userId, blockNumber, targetBlock);
            } else {
                logger.warn("No R vector version found for user {} block {} (from block {}), will use current vector", 
                           userId, blockNumber, targetBlock);
            }
            
            return rVectorVersion;
            
        } catch (Exception e) {
            logger.error("Failed to get R vector version for user {} block {}", userId, blockNumber, e);
            return null;
        }
    }
    
    /**
     * Get or create UserFeedState for a user
     * 
     * @param userId The user ID
     * @return UserFeedState entity
     */
    private UserFeedState getUserFeedState(UUID userId) {
        Optional<UserFeedState> userStateOpt = userFeedStateRepository.findByUserId(userId);
        
        if (userStateOpt.isPresent()) {
            return userStateOpt.get();
        } else {
            // Create new UserFeedState if it doesn't exist
            UserFeedState newUserState = new UserFeedState();
            newUserState.setUserId(userId);
            newUserState.setCurrentBlockNumber(0);
            newUserState.setTotalBlocksGenerated(0);
            
            UserFeedState savedState = userFeedStateRepository.save(newUserState);
            logger.info("Created new UserFeedState for user: {}", userId);
            return savedState;
        }
    }
    
    /**
     * Handle user scroll event and pre-calculate next blocks asynchronously
     * Triggered when user scrolls to next block
     * 
     * @param userId The user ID
     */
    @Async
    public void handleUserScroll(UUID userId) {
        logger.info("User scroll detected for user: {}, triggering pre-calculation", userId);
        
        try {
            // Get user's current block number
            UserFeedState userState = getUserFeedState(userId);
            Integer currentBlockNumber = userState.getCurrentBlockNumber();
            
            // Pre-calculate block N+2 asynchronously (two blocks ahead)
            Integer targetBlockNumber = currentBlockNumber + 2;
            logger.info("Pre-calculating block {} for user {} (current block: {})", 
                       targetBlockNumber, userId, currentBlockNumber);
            
            // Use preCalculateNextBlock logic but for the target block
            Integer targetRVectorVersion = getRVectorForBlock(userId, targetBlockNumber);
            
            if (targetRVectorVersion == null) {
                // Use default block generation
                feedBlockGeneratorService.generateDefaultBlock(userId);
                logger.info("Pre-calculated default block {} for user {}", targetBlockNumber, userId);
            } else {
                // Get specific R vector version and generate block
                UserRecommendationVector targetRVector = rVectorService.getVectorByVersion(userId, targetRVectorVersion);
                if (targetRVector != null) {
                    feedBlockGeneratorService.generateBlockForUser(userId, targetBlockNumber, targetRVector);
                    logger.info("Pre-calculated block {} for user {} using R vector version {}", 
                               targetBlockNumber, userId, targetRVectorVersion);
                } else {
                    logger.warn("R vector version {} not found for user {}, skipping pre-calculation", 
                               targetRVectorVersion, userId);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to handle user scroll for user: {}", userId, e);
        }
    }
    
    /**
     * Clean up expired blocks for a user
     * 
     * @param userId The user ID
     * @return Number of expired blocks deleted
     */
    @Transactional
    public int cleanupExpiredBlocks(UUID userId) {
        logger.info("Starting cleanup of expired blocks for user: {}", userId);
        
        try {
            // Remove expired blocks for this user
            int deletedCount = feedBlockCacheRepository.deleteByExpiresAtBefore(LocalDateTime.now());
            
            logger.info("Cleaned up {} expired blocks for user: {}", deletedCount, userId);
            return deletedCount;
            
        } catch (Exception e) {
            logger.error("Failed to cleanup expired blocks for user: {}", userId, e);
            return 0;
        }
    }
    
    /**
     * Get or generate a feed block for a user
     * Main entry point for getting blocks - checks cache first, then generates if needed
     * 
     * @param userId The user ID
     * @param blockNumber The block number to get
     * @return FeedBlockCache with posts and metadata
     * @throws IllegalArgumentException if parameters are invalid
     * @throws RuntimeException if block generation fails
     */
    @Transactional
    public FeedBlockCache getOrGenerateBlock(UUID userId, Integer blockNumber) {
        logger.info("Getting or generating block {} for user: {}", blockNumber, userId);
        
        // Validate parameters
        if (userId == null) {
            logger.error("Cannot get/generate block: userId is null");
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (blockNumber == null || blockNumber < 1) {
            logger.error("Cannot get/generate block: invalid blockNumber: {}", blockNumber);
            throw new IllegalArgumentException("Block number must be at least 1");
        }
        
        try {
            // Step 1: Check cache first
            logger.debug("Step 1: Checking cache for user {} block {}", userId, blockNumber);
            Optional<FeedBlockCache> cachedBlock = feedBlockCacheRepository
                .findByUserIdAndBlockNumber(userId, blockNumber);
            
            if (cachedBlock.isPresent()) {
                FeedBlockCache block = cachedBlock.get();
                
                // Check if block is expired
                if (!block.isExpired()) {
                    logger.info("Cache hit for user {} block {} (expires at: {})", 
                               userId, blockNumber, block.getExpiresAt());
                    return block;
                } else {
                    logger.info("Cached block for user {} block {} is expired (expired at: {}), generating new", 
                               userId, blockNumber, block.getExpiresAt());
                }
            } else {
                logger.info("Cache miss for user {} block {}, generating new", userId, blockNumber);
            }
            
            // Step 2: Generate new block using pre-calculation logic
            logger.debug("Step 2: Generating new block using pre-calculation logic");
            
            // Get user's current block number to determine if this is a pre-calculated block
            UserFeedState userState = getUserFeedState(userId);
            Integer currentBlockNumber = userState.getCurrentBlockNumber();
            
            if (blockNumber <= currentBlockNumber + 1) {
                // This is a current or next block, use preCalculateNextBlock
                logger.debug("Block {} is current/next block, using preCalculateNextBlock", blockNumber);
                return preCalculateNextBlock(userId);
            } else {
                // This is a future block, generate directly
                logger.debug("Block {} is future block, generating directly", blockNumber);
                Integer targetRVectorVersion = getRVectorForBlock(userId, blockNumber);
                
                if (targetRVectorVersion == null) {
                    return feedBlockGeneratorService.generateDefaultBlock(userId);
                } else {
                    UserRecommendationVector targetRVector = rVectorService.getVectorByVersion(userId, targetRVectorVersion);
                    if (targetRVector == null) {
                        targetRVector = rVectorService.getCurrentVector(userId);
                    }
                    return feedBlockGeneratorService.generateBlockForUser(userId, blockNumber, targetRVector);
                }
            }
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for getOrGenerateBlock - userId: {}, blockNumber: {}", 
                        userId, blockNumber, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to get or generate block for user: {} block: {}", userId, blockNumber, e);
            throw new RuntimeException("Failed to get or generate block for user: " + userId + " block: " + blockNumber, e);
        }
    }
}

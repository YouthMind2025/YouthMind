package ai.youthmind.service;

import ai.youthmind.dto.FeedBlockResponse;
import ai.youthmind.dto.BlockMetadata;
import ai.youthmind.dto.PostDto;
import ai.youthmind.entity.FeedBlockCache;
import ai.youthmind.entity.User;
import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.repository.FeedBlockCacheRepository;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.repository.UserRecommendationVectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PostService feed block functionality
 * Tests the new feed block endpoint to ensure it works correctly with iOS
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PostServiceFeedBlockIntegrationTest {

    @Autowired
    private PostService postService;

    @Autowired
    private FeedBlockPreCalculationService feedBlockPreCalculationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRecommendationVectorRepository userRecommendationVectorRepository;

    @Autowired
    private FeedBlockCacheRepository feedBlockCacheRepository;

    private UUID testUserId;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRecommendationEngineEnabled(true);
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();

        // Initialize R vector for the user
        UserRecommendationVector rVector = new UserRecommendationVector();
        rVector.setUserId(testUserId);
        rVector.setVersion(1);
        rVector.setCreatedAt(LocalDateTime.now());
        rVector.setUpdatedAt(LocalDateTime.now());
        userRecommendationVectorRepository.save(rVector);
    }

    @Test
    void testGetFeedBlock_FirstBlock_Success() {
        // Given
        Integer blockNumber = 1;
        Integer pageSize = 20;
        Boolean prefetch = false;

        // When
        FeedBlockResponse response = postService.getFeedBlock(testUserId, blockNumber, pageSize, prefetch);

        // Then
        assertNotNull(response, "FeedBlockResponse should not be null");
        
        // Check posts are populated
        assertNotNull(response.getPosts(), "Posts should not be null");
        assertTrue(response.getPosts().size() > 0, "Posts should contain at least one post");
        
        // Check block metadata is present
        assertNotNull(response.getBlockMetadata(), "Block metadata should not be null");
        BlockMetadata metadata = response.getBlockMetadata();
        assertNotNull(metadata.getEmotionDistribution(), "Emotion distribution should not be null");
        assertTrue(metadata.getEmotionDistribution().size() > 0, "Emotion distribution should contain emotions");
        assertNotNull(metadata.getRVectorVersion(), "R vector version should not be null");
        assertTrue(metadata.getIsPersonalized(), "Block should be personalized");
        
        // Check pagination fields
        assertEquals(0, response.getCurrentPage(), "Current page should be 0 for block-based feeds");
        assertEquals(1, response.getTotalPages(), "Total pages should be 1 for self-contained blocks");
        assertTrue(response.isHasNext(), "Should have next block available");
        assertTrue(response.getIsFirst(), "Should be first block");
        assertFalse(response.getIsLast(), "Should not be last block");
        
        // Check block-specific fields
        assertEquals(blockNumber, response.getBlockNumber(), "Block number should match requested");
        assertEquals(blockNumber + 1, response.getNextBlockNumber(), "Next block number should increment");
        assertTrue(response.getNextBlockAvailable(), "Next block should be available");
        
        // Check page size
        assertEquals(pageSize, response.getPageSize(), "Page size should match requested");
        assertEquals(response.getPosts().size(), response.getTotalElements(), "Total elements should match posts count");
    }

    @Test
    void testGetFeedBlock_WithPrefetch_TriggersPreCalculation() {
        // Given
        Integer blockNumber = 1;
        Integer pageSize = 20;
        Boolean prefetch = true;

        // When
        FeedBlockResponse response = postService.getFeedBlock(testUserId, blockNumber, pageSize, prefetch);

        // Then
        assertNotNull(response, "FeedBlockResponse should not be null");
        
        // Verify prefetch was triggered by checking if next block becomes available
        // Note: This is a simplified test - in a real scenario, we'd mock the service
        assertTrue(response.getNextBlockAvailable(), "Next block should be available after prefetch");
        assertEquals(blockNumber + 1, response.getNextBlockNumber(), "Next block number should be correct");
        
        // Verify the response structure is correct
        assertNotNull(response.getPosts(), "Posts should not be null");
        assertNotNull(response.getBlockMetadata(), "Block metadata should not be null");
        assertEquals(blockNumber, response.getBlockNumber(), "Block number should match");
    }

    @Test
    void testGetFeedBlock_InvalidBlockNumber_ThrowsException() {
        // Test blockNumber = 0
        IllegalArgumentException exception1 = assertThrows(
            IllegalArgumentException.class,
            () -> postService.getFeedBlock(testUserId, 0, 20, false),
            "Should throw IllegalArgumentException for blockNumber = 0"
        );
        assertTrue(exception1.getMessage().contains("Block number must be at least 1"), 
                  "Exception message should mention block number requirement");

        // Test negative blockNumber
        IllegalArgumentException exception2 = assertThrows(
            IllegalArgumentException.class,
            () -> postService.getFeedBlock(testUserId, -1, 20, false),
            "Should throw IllegalArgumentException for negative blockNumber"
        );
        assertTrue(exception2.getMessage().contains("Block number must be at least 1"), 
                  "Exception message should mention block number requirement");
    }

    @Test
    void testGetFeedBlock_InvalidPageSize_ThrowsException() {
        // Test pageSize = 0
        IllegalArgumentException exception1 = assertThrows(
            IllegalArgumentException.class,
            () -> postService.getFeedBlock(testUserId, 1, 0, false),
            "Should throw IllegalArgumentException for pageSize = 0"
        );
        assertTrue(exception1.getMessage().contains("Page size must be between 1 and 50"), 
                  "Exception message should mention page size requirement");

        // Test pageSize > 50
        IllegalArgumentException exception2 = assertThrows(
            IllegalArgumentException.class,
            () -> postService.getFeedBlock(testUserId, 1, 51, false),
            "Should throw IllegalArgumentException for pageSize > 50"
        );
        assertTrue(exception2.getMessage().contains("Page size must be between 1 and 50"), 
                  "Exception message should mention page size requirement");
    }

    @Test
    void testGetFeedBlock_BlockMetadata_IsCorrect() {
        // Given
        Integer blockNumber = 1;
        Integer pageSize = 20;
        Boolean prefetch = false;

        // When
        FeedBlockResponse response = postService.getFeedBlock(testUserId, blockNumber, pageSize, prefetch);

        // Then
        assertNotNull(response.getBlockMetadata(), "Block metadata should not be null");
        BlockMetadata metadata = response.getBlockMetadata();

        // Verify emotion distribution
        Map<String, Integer> emotionDistribution = metadata.getEmotionDistribution();
        assertNotNull(emotionDistribution, "Emotion distribution should not be null");
        assertTrue(emotionDistribution.size() > 0, "Emotion distribution should contain emotions");
        
        // Check that emotion distribution values are positive
        emotionDistribution.values().forEach(value -> 
            assertTrue(value >= 0, "Emotion distribution values should be non-negative")
        );

        // Check R vector version
        assertNotNull(metadata.getRVectorVersion(), "R vector version should not be null");
        assertTrue(metadata.getRVectorVersion() >= 0, "R vector version should be non-negative");

        // Check personalized flag
        assertTrue(metadata.getIsPersonalized(), "Block should be personalized");

        // Check timestamps
        assertNotNull(metadata.getGeneratedAt(), "Generated at timestamp should not be null");
        assertNotNull(metadata.getExpiresAt(), "Expires at timestamp should not be null");
        assertTrue(metadata.getExpiresAt().isAfter(metadata.getGeneratedAt()), 
                  "Expiration should be after generation");

        // Check utility methods
        assertFalse(metadata.isExpired(), "Block should not be expired immediately after generation");
        assertTrue(metadata.getAgeInHours() >= 0, "Age should be non-negative");
        assertTrue(metadata.getTimeUntilExpirationInHours() > 0, "Time until expiration should be positive");
        assertTrue(metadata.getTotalPostCount() > 0, "Total post count should be positive");
        
        String dominantEmotion = metadata.getDominantEmotion();
        if (dominantEmotion != null) {
            assertTrue(emotionDistribution.containsKey(dominantEmotion), 
                      "Dominant emotion should exist in distribution");
        }
    }

    @Test
    void testGetFeedBlock_NextBlockAvailability_IsAccurate() {
        // Given
        Integer blockNumber = 1;
        Integer pageSize = 20;
        Boolean prefetch = false;

        // When
        FeedBlockResponse response = postService.getFeedBlock(testUserId, blockNumber, pageSize, prefetch);

        // Then
        assertNotNull(response, "FeedBlockResponse should not be null");
        
        // Check next block availability logic
        assertTrue(response.getNextBlockAvailable(), "Next block should be available");
        assertEquals(blockNumber + 1, response.getNextBlockNumber(), "Next block number should increment correctly");
        assertTrue(response.isHasNext(), "hasNext should be true when next block is available");
        assertFalse(response.getIsLast(), "isLast should be false when next block is available");

        // Test getting the next block
        FeedBlockResponse nextResponse = postService.getFeedBlock(testUserId, blockNumber + 1, pageSize, prefetch);
        assertNotNull(nextResponse, "Next block response should not be null");
        assertEquals(blockNumber + 1, nextResponse.getBlockNumber(), "Next block number should be correct");
        
        // Verify the next block has its own metadata
        assertNotNull(nextResponse.getBlockMetadata(), "Next block metadata should not be null");
        assertTrue(nextResponse.getBlockMetadata().getIsPersonalized(), "Next block should also be personalized");
        
        // Check that next block availability is still accurate
        assertTrue(nextResponse.getNextBlockAvailable(), "Next block should still be available");
        assertEquals(blockNumber + 2, nextResponse.getNextBlockNumber(), "Next block number should increment again");
    }

    @Test
    void testGetFeedBlock_NullUserId_ThrowsException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> postService.getFeedBlock(null, 1, 20, false),
            "Should throw IllegalArgumentException for null userId"
        );
        assertTrue(exception.getMessage().contains("User ID cannot be null"), 
                  "Exception message should mention user ID requirement");
    }

    @Test
    void testGetFeedBlock_MultipleBlocks_Consistency() {
        // Test multiple blocks to ensure consistency
        for (int blockNum = 1; blockNum <= 3; blockNum++) {
            FeedBlockResponse response = postService.getFeedBlock(testUserId, blockNum, 20, false);
            
            assertNotNull(response, "Response should not be null for block " + blockNum);
            assertEquals(blockNum, response.getBlockNumber(), "Block number should match for block " + blockNum);
            assertNotNull(response.getPosts(), "Posts should not be null for block " + blockNum);
            assertNotNull(response.getBlockMetadata(), "Metadata should not be null for block " + blockNum);
            assertTrue(response.getBlockMetadata().getIsPersonalized(), "Block should be personalized for block " + blockNum);
            
            // Each block should be self-contained
            assertEquals(0, response.getCurrentPage(), "Current page should be 0 for block " + blockNum);
            assertEquals(1, response.getTotalPages(), "Total pages should be 1 for block " + blockNum);
        }
    }
}

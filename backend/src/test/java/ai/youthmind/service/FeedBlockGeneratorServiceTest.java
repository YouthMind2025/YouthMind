package ai.youthmind.service;

import ai.youthmind.entity.FeedBlockCache;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.UserFeedState;
import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.repository.FeedBlockCacheRepository;
import ai.youthmind.repository.UserFeedStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FeedBlockGeneratorService
 * Tests all major functionality with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
class FeedBlockGeneratorServiceTest {
    
    @Mock
    private RVectorService rVectorService;
    
    @Mock
    private RecommendationMappingService recommendationMappingService;
    
    @Mock
    private PostSelectionService postSelectionService;
    
    @Mock
    private FeedBlockCacheRepository feedBlockCacheRepository;
    
    @Mock
    private UserFeedStateRepository userFeedStateRepository;
    
    @Mock
    private RedisService redisService;
    
    @InjectMocks
    private FeedBlockGeneratorService feedBlockGeneratorService;
    
    private UUID testUserId;
    private UserRecommendationVector testRVector;
    private UserFeedState testUserState;
    private List<Post> testPosts;
    
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        // Create test R vector
        testRVector = new UserRecommendationVector();
        testRVector.setId(UUID.randomUUID());
        testRVector.setUserId(testUserId);
        testRVector.setSad(15);
        testRVector.setFear(8);
        testRVector.setAngry(12);
        testRVector.setHappy(18);
        testRVector.setConflicted(10);
        testRVector.setSurprised(7);
        testRVector.setVersion(1);
        testRVector.setCreatedAt(LocalDateTime.now());
        testRVector.setUpdatedAt(LocalDateTime.now());
        
        // Create test user state
        testUserState = new UserFeedState();
        testUserState.setId(UUID.randomUUID());
        testUserState.setUserId(testUserId);
        testUserState.setCurrentBlockNumber(0);
        testUserState.setCurrentRVectorVersion(1);
        testUserState.setTotalBlocksGenerated(0);
        testUserState.setShownPostIds(new HashSet<>());
        testUserState.setLastAccessedAt(LocalDateTime.now());
        
        // Create test posts
        testPosts = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Post post = new Post();
            post.setId(UUID.randomUUID());
            post.setEmotion("happy");
            post.setContent("Test post " + i);
            post.setCreatedAt(LocalDateTime.now());
            testPosts.add(post);
        }
    }
    
    @Test
    void testGenerateBlockForUser_Success() {
        // Arrange
        Integer blockNumber = 1;
        List<String> topThreeEmotions = Arrays.asList("happy", "sad", "angry");
        Map<String, Integer> emotionDistribution = Map.of(
            "happy", 30, "sad", 25, "angry", 20, "conflicted", 15, "surprised", 10
        );
        
        when(rVectorService.getTopThreeEmotions(testRVector)).thenReturn(topThreeEmotions);
        when(recommendationMappingService.getDistributionForEmotions(topThreeEmotions)).thenReturn(emotionDistribution);
        when(userFeedStateRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserState));
        when(postSelectionService.selectPostsForEmotion(anyString(), anyInt(), anySet())).thenAnswer(invocation -> {
            Integer count = invocation.getArgument(1);
            return testPosts.subList(0, Math.min(count, testPosts.size()));
        });
        when(feedBlockCacheRepository.save(any(FeedBlockCache.class))).thenAnswer(invocation -> {
            FeedBlockCache cache = invocation.getArgument(0);
            cache.setId(UUID.randomUUID());
            return cache;
        });
        
        // Act
        FeedBlockCache result = feedBlockGeneratorService.generateBlockForUser(testUserId, blockNumber, testRVector);
        
        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(blockNumber, result.getBlockNumber());
        assertEquals(testRVector.getVersion(), result.getRVectorVersion());
        assertEquals(emotionDistribution, result.getEmotionDistribution());
        assertEquals(20, result.getPostCount());
        assertNotNull(result.getGeneratedAt());
        assertNotNull(result.getExpiresAt());
        
        // Verify interactions
        verify(rVectorService).getTopThreeEmotions(testRVector);
        verify(recommendationMappingService).getDistributionForEmotions(topThreeEmotions);
        verify(userFeedStateRepository).findByUserId(testUserId);
        verify(postSelectionService, atLeastOnce()).selectPostsForEmotion(anyString(), anyInt(), anySet());
        verify(feedBlockCacheRepository).save(any(FeedBlockCache.class));
        verify(userFeedStateRepository).save(testUserState);
    }
    
    @Test
    void testGenerateDefaultBlock_Success() {
        // Arrange
        when(userFeedStateRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserState));
        when(postSelectionService.selectPostsForEmotion(anyString(), anyInt(), anySet())).thenAnswer(invocation -> {
            Integer count = invocation.getArgument(1);
            return testPosts.subList(0, Math.min(count, testPosts.size()));
        });
        when(feedBlockCacheRepository.save(any(FeedBlockCache.class))).thenAnswer(invocation -> {
            FeedBlockCache cache = invocation.getArgument(0);
            cache.setId(UUID.randomUUID());
            return cache;
        });
        
        // Act
        FeedBlockCache result = feedBlockGeneratorService.generateDefaultBlock(testUserId);
        
        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(1, result.getBlockNumber());
        assertEquals(0, result.getRVectorVersion()); // Default version
        assertEquals(20, result.getPostCount());
        
        // Verify default distribution
        Map<String, Integer> distribution = result.getEmotionDistribution();
        assertEquals(20, distribution.get("sad"));
        assertEquals(5, distribution.get("fear"));
        assertEquals(20, distribution.get("angry"));
        assertEquals(20, distribution.get("happy"));
        assertEquals(20, distribution.get("conflicted"));
        assertEquals(15, distribution.get("surprised"));
        
        // Verify interactions
        verify(userFeedStateRepository).findByUserId(testUserId);
        verify(postSelectionService, atLeastOnce()).selectPostsForEmotion(anyString(), anyInt(), anySet());
        verify(feedBlockCacheRepository).save(any(FeedBlockCache.class));
        verify(userFeedStateRepository).save(testUserState);
    }
    
    @Test
    void testGetOrGenerateBlock_CachedBlock() {
        // Arrange
        Integer blockNumber = 1;
        FeedBlockCache cachedBlock = new FeedBlockCache();
        cachedBlock.setId(UUID.randomUUID());
        cachedBlock.setUserId(testUserId);
        cachedBlock.setBlockNumber(blockNumber);
        cachedBlock.setExpiresAt(LocalDateTime.now().plusHours(2)); // Not expired
        cachedBlock.setPostIds(Arrays.asList(UUID.randomUUID(), UUID.randomUUID()));
        
        when(feedBlockCacheRepository.findByUserIdAndBlockNumber(testUserId, blockNumber))
            .thenReturn(Optional.of(cachedBlock));
        
        // Act
        FeedBlockCache result = feedBlockGeneratorService.getOrGenerateBlock(testUserId, blockNumber);
        
        // Assert
        assertNotNull(result);
        assertEquals(cachedBlock.getId(), result.getId());
        assertEquals(testUserId, result.getUserId());
        assertEquals(blockNumber, result.getBlockNumber());
        
        // Verify generateBlockForUser is NOT called
        verify(rVectorService, never()).getCurrentVector(any());
        verify(recommendationMappingService, never()).getDistributionForEmotions(any());
        verify(postSelectionService, never()).selectPostsForEmotion(anyString(), anyInt(), anySet());
    }
    
    @Test
    void testGetOrGenerateBlock_ExpiredBlock() {
        // Arrange
        Integer blockNumber = 1;
        FeedBlockCache expiredBlock = new FeedBlockCache();
        expiredBlock.setId(UUID.randomUUID());
        expiredBlock.setUserId(testUserId);
        expiredBlock.setBlockNumber(blockNumber);
        expiredBlock.setExpiresAt(LocalDateTime.now().minusHours(1)); // Expired
        
        when(feedBlockCacheRepository.findByUserIdAndBlockNumber(testUserId, blockNumber))
            .thenReturn(Optional.of(expiredBlock));
        when(rVectorService.getCurrentVector(testUserId)).thenReturn(testRVector);
        when(rVectorService.getTopThreeEmotions(testRVector)).thenReturn(Arrays.asList("happy", "sad", "angry"));
        when(recommendationMappingService.getDistributionForEmotions(any())).thenReturn(Map.of("happy", 30, "sad", 25, "angry", 20));
        when(userFeedStateRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserState));
        when(postSelectionService.selectPostsForEmotion(anyString(), anyInt(), anySet())).thenAnswer(invocation -> {
            Integer count = invocation.getArgument(1);
            return testPosts.subList(0, Math.min(count, testPosts.size()));
        });
        when(feedBlockCacheRepository.save(any(FeedBlockCache.class))).thenAnswer(invocation -> {
            FeedBlockCache cache = invocation.getArgument(0);
            cache.setId(UUID.randomUUID());
            return cache;
        });
        
        // Act
        FeedBlockCache result = feedBlockGeneratorService.getOrGenerateBlock(testUserId, blockNumber);
        
        // Assert
        assertNotNull(result);
        assertNotEquals(expiredBlock.getId(), result.getId()); // New block generated
        
        // Verify generateBlockForUser IS called
        verify(rVectorService).getCurrentVector(testUserId);
        verify(rVectorService).getTopThreeEmotions(testRVector);
        verify(recommendationMappingService).getDistributionForEmotions(any());
        verify(postSelectionService, atLeastOnce()).selectPostsForEmotion(anyString(), anyInt(), anySet());
    }
    
    @Test
    void testGetOrGenerateBlock_NoCache() {
        // Arrange
        Integer blockNumber = 1;
        
        when(feedBlockCacheRepository.findByUserIdAndBlockNumber(testUserId, blockNumber))
            .thenReturn(Optional.empty());
        when(rVectorService.getCurrentVector(testUserId)).thenReturn(testRVector);
        when(rVectorService.getTopThreeEmotions(testRVector)).thenReturn(Arrays.asList("happy", "sad", "angry"));
        when(recommendationMappingService.getDistributionForEmotions(any())).thenReturn(Map.of("happy", 30, "sad", 25, "angry", 20));
        when(userFeedStateRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserState));
        when(postSelectionService.selectPostsForEmotion(anyString(), anyInt(), anySet())).thenAnswer(invocation -> {
            Integer count = invocation.getArgument(1);
            return testPosts.subList(0, Math.min(count, testPosts.size()));
        });
        when(feedBlockCacheRepository.save(any(FeedBlockCache.class))).thenAnswer(invocation -> {
            FeedBlockCache cache = invocation.getArgument(0);
            cache.setId(UUID.randomUUID());
            return cache;
        });
        
        // Act
        FeedBlockCache result = feedBlockGeneratorService.getOrGenerateBlock(testUserId, blockNumber);
        
        // Assert
        assertNotNull(result);
        assertEquals(testUserId, result.getUserId());
        assertEquals(blockNumber, result.getBlockNumber());
        
        // Verify new block is generated
        verify(rVectorService).getCurrentVector(testUserId);
        verify(rVectorService).getTopThreeEmotions(testRVector);
        verify(recommendationMappingService).getDistributionForEmotions(any());
        verify(postSelectionService, atLeastOnce()).selectPostsForEmotion(anyString(), anyInt(), anySet());
    }
    
    @Test
    void testCalculatePostCounts_CorrectDistribution() {
        // Arrange
        Map<String, Integer> emotionDistribution = Map.of(
            "happy", 30, "sad", 25, "angry", 20, "conflicted", 15, "surprised", 10
        );
        int totalPosts = 20;
        
        // Act
        Map<String, Integer> result = feedBlockGeneratorService.calculatePostCounts(emotionDistribution, totalPosts);
        
        // Assert
        assertNotNull(result);
        assertEquals(5, result.size()); // All emotions present
        
        // Verify totals add up correctly
        int totalCalculated = result.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(totalPosts, totalCalculated);
        
        // Verify individual counts (with rounding)
        assertEquals(6, result.get("happy"));   // 30% of 20 = 6
        assertEquals(5, result.get("sad"));       // 25% of 20 = 5
        assertEquals(4, result.get("angry"));    // 20% of 20 = 4
        assertEquals(3, result.get("conflicted")); // 15% of 20 = 3
        assertEquals(2, result.get("surprised")); // 10% of 20 = 2
    }
    
    @Test
    void testCleanupExpiredBlocks_Success() {
        // Arrange
        int expectedDeletedCount = 5;
        when(feedBlockCacheRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
            .thenReturn(expectedDeletedCount);
        
        // Act
        int result = feedBlockGeneratorService.cleanupExpiredBlocks();
        
        // Assert
        assertEquals(expectedDeletedCount, result);
        verify(feedBlockCacheRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }
    
    @Test
    void testGenerateBlockForUser_NullParameters() {
        // Test null userId
        assertThrows(IllegalArgumentException.class, () -> {
            feedBlockGeneratorService.generateBlockForUser(null, 1, testRVector);
        });
        
        // Test null blockNumber
        assertThrows(IllegalArgumentException.class, () -> {
            feedBlockGeneratorService.generateBlockForUser(testUserId, null, testRVector);
        });
        
        // Test invalid blockNumber
        assertThrows(IllegalArgumentException.class, () -> {
            feedBlockGeneratorService.generateBlockForUser(testUserId, 0, testRVector);
        });
        
        // Test null rVector
        assertThrows(IllegalArgumentException.class, () -> {
            feedBlockGeneratorService.generateBlockForUser(testUserId, 1, null);
        });
    }
    
    @Test
    void testGenerateDefaultBlock_NullUserId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            feedBlockGeneratorService.generateDefaultBlock(null);
        });
    }
    
    @Test
    void testGetOrGenerateBlock_NullParameters() {
        // Test null userId
        assertThrows(IllegalArgumentException.class, () -> {
            feedBlockGeneratorService.getOrGenerateBlock(null, 1);
        });
        
        // Test null blockNumber
        assertThrows(IllegalArgumentException.class, () -> {
            feedBlockGeneratorService.getOrGenerateBlock(testUserId, null);
        });
        
        // Test invalid blockNumber
        assertThrows(IllegalArgumentException.class, () -> {
            feedBlockGeneratorService.getOrGenerateBlock(testUserId, 0);
        });
    }
    
    @Test
    void testCacheBlock_Success() {
        // Arrange
        FeedBlockCache block = new FeedBlockCache();
        block.setId(null); // No ID yet - will trigger save
        block.setUserId(testUserId);
        block.setBlockNumber(1);
        block.setExpiresAt(LocalDateTime.now().plusHours(1));
        
        when(feedBlockCacheRepository.save(any(FeedBlockCache.class))).thenReturn(block);
        
        // Act
        feedBlockGeneratorService.cacheBlock(block);
        
        // Assert
        verify(feedBlockCacheRepository).save(block);
        verify(redisService).setValue(anyString(), eq(block), anyLong(), any());
    }
    
    @Test
    void testCacheBlock_NullBlock() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            feedBlockGeneratorService.cacheBlock(null);
        });
    }
    
    @Test
    void testGetOrGenerateBlock_NoRVector_UsesDefault() {
        // Arrange
        Integer blockNumber = 1;
        
        when(feedBlockCacheRepository.findByUserIdAndBlockNumber(testUserId, blockNumber))
            .thenReturn(Optional.empty());
        when(rVectorService.getCurrentVector(testUserId)).thenThrow(new RuntimeException("No R vector"));
        when(userFeedStateRepository.findByUserId(testUserId)).thenReturn(Optional.of(testUserState));
        when(postSelectionService.selectPostsForEmotion(anyString(), anyInt(), anySet())).thenAnswer(invocation -> {
            Integer count = invocation.getArgument(1);
            return testPosts.subList(0, Math.min(count, testPosts.size()));
        });
        when(feedBlockCacheRepository.save(any(FeedBlockCache.class))).thenAnswer(invocation -> {
            FeedBlockCache cache = invocation.getArgument(0);
            cache.setId(UUID.randomUUID());
            return cache;
        });
        
        // Act
        FeedBlockCache result = feedBlockGeneratorService.getOrGenerateBlock(testUserId, blockNumber);
        
        // Assert
        assertNotNull(result);
        assertEquals(0, result.getRVectorVersion()); // Default version
        assertEquals(1, result.getBlockNumber());
        
        // Verify default block generation was called
        verify(rVectorService).getCurrentVector(testUserId);
        verify(userFeedStateRepository).findByUserId(testUserId);
        verify(postSelectionService, atLeastOnce()).selectPostsForEmotion(anyString(), anyInt(), anySet());
    }
}

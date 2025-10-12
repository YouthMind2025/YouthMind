package ai.youthmind.service;

import ai.youthmind.dto.FeedBlockResponse;
import ai.youthmind.dto.BlockMetadata;
import ai.youthmind.dto.PostDto;
import ai.youthmind.entity.FeedBlockCache;
import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PostService feed block functionality
 * Tests the new feed block endpoint logic without requiring full Spring context
 */
@ExtendWith(MockitoExtension.class)
public class PostServiceFeedBlockUnitTest {

    @Mock
    private FeedBlockPreCalculationService feedBlockPreCalculationService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService;

    private UUID testUserId;
    private FeedBlockCache mockBlock;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        // Create mock feed block
        mockBlock = new FeedBlockCache();
        mockBlock.setId(UUID.randomUUID());
        mockBlock.setUserId(testUserId);
        mockBlock.setBlockNumber(1);
        mockBlock.setPostIds(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        mockBlock.setEmotionDistribution(Map.of(
            "happy", 2,
            "sad", 1,
            "angry", 0,
            "conflicted", 0,
            "surprised", 0,
            "fear", 0
        ));
        mockBlock.setRVectorVersion(1);
        mockBlock.setGeneratedAt(LocalDateTime.now());
        mockBlock.setExpiresAt(LocalDateTime.now().plusHours(24));
        mockBlock.setCreatedAt(LocalDateTime.now());
        mockBlock.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testGetFeedBlock_FirstBlock_Success() {
        // Given
        Integer blockNumber = 1;
        Integer pageSize = 20;
        Boolean prefetch = false;

        // Mock the service call
        when(feedBlockPreCalculationService.getOrGenerateBlock(testUserId, blockNumber))
            .thenReturn(mockBlock);

        // Mock post conversion (simplified)
        List<PostDto> mockPosts = List.of(
            createMockPostDto(),
            createMockPostDto(),
            createMockPostDto()
        );
        
        // We need to mock the convertBlockPostsToDtos method, but it's private
        // For this test, we'll focus on the public interface validation
        
        // When
        FeedBlockResponse response = postService.getFeedBlock(testUserId, blockNumber, pageSize, prefetch);

        // Then
        assertNotNull(response, "FeedBlockResponse should not be null");
        assertEquals(blockNumber, response.getBlockNumber(), "Block number should match requested");
        assertNotNull(response.getBlockMetadata(), "Block metadata should not be null");
        
        // Verify service was called
        verify(feedBlockPreCalculationService).getOrGenerateBlock(testUserId, blockNumber);
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
    void testGetFeedBlock_WithPrefetch_TriggersPreCalculation() {
        // Given
        Integer blockNumber = 1;
        Integer pageSize = 20;
        Boolean prefetch = true;

        // Mock the service call to return a block with next block available
        when(feedBlockPreCalculationService.getOrGenerateBlock(testUserId, blockNumber))
            .thenReturn(mockBlock);
        
        // Mock next block availability to return true so prefetch is triggered
        when(feedBlockPreCalculationService.getOrGenerateBlock(testUserId, blockNumber + 1))
            .thenReturn(mockBlock);

        // When
        FeedBlockResponse response = postService.getFeedBlock(testUserId, blockNumber, pageSize, prefetch);

        // Then
        assertNotNull(response, "FeedBlockResponse should not be null");
        
        // Verify prefetch was triggered (only when next block is available)
        verify(feedBlockPreCalculationService).handleUserScroll(testUserId);
    }

    @Test
    void testConvertToBlockMetadata_IsCorrect() {
        // Given
        BlockMetadata metadata = new BlockMetadata();
        metadata.setEmotionDistribution(mockBlock.getEmotionDistribution());
        metadata.setRVectorVersion(mockBlock.getRVectorVersion());
        metadata.setGeneratedAt(mockBlock.getGeneratedAt());
        metadata.setExpiresAt(mockBlock.getExpiresAt());
        metadata.setIsPersonalized(true);

        // When & Then
        assertNotNull(metadata.getEmotionDistribution(), "Emotion distribution should not be null");
        assertTrue(metadata.getEmotionDistribution().size() > 0, "Emotion distribution should contain emotions");
        assertNotNull(metadata.getRVectorVersion(), "R vector version should not be null");
        assertTrue(metadata.getIsPersonalized(), "Block should be personalized");
        assertNotNull(metadata.getGeneratedAt(), "Generated at timestamp should not be null");
        assertNotNull(metadata.getExpiresAt(), "Expires at timestamp should not be null");
        assertTrue(metadata.getExpiresAt().isAfter(metadata.getGeneratedAt()), 
                  "Expiration should be after generation");
        assertFalse(metadata.isExpired(), "Block should not be expired immediately after generation");
        assertTrue(metadata.getTotalPostCount() > 0, "Total post count should be positive");
    }

    @Test
    void testFeedBlockResponse_StructureValidation() {
        // Given
        FeedBlockResponse response = new FeedBlockResponse();
        response.setBlockNumber(1);
        response.setNextBlockAvailable(true);
        response.setNextBlockNumber(2);
        
        BlockMetadata metadata = new BlockMetadata();
        metadata.setEmotionDistribution(Map.of("happy", 2, "sad", 1));
        metadata.setRVectorVersion(1);
        metadata.setGeneratedAt(LocalDateTime.now());
        metadata.setExpiresAt(LocalDateTime.now().plusHours(24));
        metadata.setIsPersonalized(true);
        
        response.setBlockMetadata(metadata);

        // When & Then
        assertEquals(1, response.getBlockNumber(), "Block number should be set correctly");
        assertTrue(response.getNextBlockAvailable(), "Next block should be available");
        assertEquals(2, response.getNextBlockNumber(), "Next block number should increment");
        assertTrue(response.isPersonalized(), "Block should be personalized");
        assertFalse(response.isBlockExpired(), "Block should not be expired");
        assertEquals(1, response.getRVectorVersion(), "R vector version should match");
        assertTrue(response.isFirstBlock(), "Should be first block");
    }

    private PostDto createMockPostDto() {
        PostDto postDto = new PostDto();
        postDto.setId(UUID.randomUUID());
        postDto.setTitle("Test Post");
        postDto.setContent("Test content");
        postDto.setEmotion("happy");
        postDto.setResonance(5);
        postDto.setView(10);
        postDto.setCollect(2);
        postDto.setIsresonanced(false);
        postDto.setCollected(false);
        postDto.setDate(LocalDateTime.now());
        return postDto;
    }
}

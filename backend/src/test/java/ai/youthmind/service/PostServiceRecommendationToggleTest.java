package ai.youthmind.service;

import ai.youthmind.dto.FeedResponse;
import ai.youthmind.entity.FeedBlockCache;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.PostResonanceRepository;
import ai.youthmind.repository.PostViewRepository;
import ai.youthmind.repository.PostCollectionRepository;
import ai.youthmind.repository.CommentRepository;
import ai.youthmind.repository.AIAnalysisRepository;
import ai.youthmind.service.PsychologicalStateCacheService;
import ai.youthmind.service.AIServiceClient;
import ai.youthmind.service.PsychologicalVectorGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Integration test for PostService recommendation engine toggle functionality
 * Tests routing between personalized and chronological feeds based on user preference
 */
@ExtendWith(MockitoExtension.class)
class PostServiceRecommendationToggleTest {

    private static final Logger logger = LoggerFactory.getLogger(PostServiceRecommendationToggleTest.class);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostResonanceRepository postResonanceRepository;

    @Mock
    private PostViewRepository postViewRepository;

    @Mock
    private PostCollectionRepository postCollectionRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AIAnalysisRepository aiAnalysisRepository;

    @Mock
    private PsychologicalStateCacheService psychologicalStateCacheService;

    @Mock
    private AIServiceClient aiServiceClient;

    @Mock
    private PsychologicalVectorGenerator psychologicalVectorGenerator;

    @Mock
    private FeedBlockPreCalculationService feedBlockPreCalculationService;

    @InjectMocks
    private PostService postService;

    private UUID testUserId;
    private User testUser;
    private UserRecommendationVector testRVector;
    private FeedBlockCache testFeedBlock;
    private List<Post> testPosts;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        
        // Setup common mock behaviors - using lenient to avoid unnecessary stubbing errors
        lenient().when(postResonanceRepository.findByUserIdAndPostIdIn(any(UUID.class), any(List.class))).thenReturn(List.of());
        lenient().when(postViewRepository.findByUserId(any(UUID.class))).thenReturn(List.of());
        lenient().when(postCollectionRepository.findByUserIdAndPostIdIn(any(UUID.class), any(List.class))).thenReturn(List.of());
        
        // Setup test user
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setRecommendationEngineEnabled(true); // Default to enabled
        
        // Setup test R vector
        testRVector = new UserRecommendationVector();
        testRVector.setUserId(testUserId);
        testRVector.setVersion(5);
        testRVector.setEmotionValue("happy", 15);
        testRVector.setEmotionValue("sad", 10);
        
        // Setup test feed block
        testFeedBlock = new FeedBlockCache();
        testFeedBlock.setId(UUID.randomUUID());
        testFeedBlock.setUserId(testUserId);
        testFeedBlock.setBlockNumber(1);
        testFeedBlock.setPostIds(List.of(UUID.randomUUID(), UUID.randomUUID()));
        testFeedBlock.setEmotionDistribution(Map.of("happy", 60, "sad", 40));
        testFeedBlock.setRVectorVersion(5);
        testFeedBlock.setGeneratedAt(LocalDateTime.now());
        testFeedBlock.setExpiresAt(LocalDateTime.now().plusHours(1));
        
        // Setup test posts
        testPosts = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Post post = new Post();
            post.setId(UUID.randomUUID());
            post.setTitle("Test Post " + i);
            post.setContent("Test post content " + i);
            post.setEmotion("happy");
            post.setView(10);
            post.setResonance(5);
            post.setCollect(2);
            post.setFlagged(false);
            post.setCreatedAt(LocalDateTime.now().minusHours(i));
            post.setUpdatedAt(LocalDateTime.now().minusHours(i));
            testPosts.add(post);
        }
    }

    /**
     * Helper method to create a mock post with the given ID
     */
    private Post createMockPost(UUID id) {
        Post post = new Post();
        post.setId(id);
        post.setTitle("Mock Post");
        post.setContent("Mock post content");
        post.setEmotion("happy");
        post.setView(10);
        post.setResonance(5);
        post.setCollect(2);
        post.setFlagged(false);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return post;
    }

    @Test
    void testGetFeed_RecommendationEnabled_RoutesToPersonalizedFeed() {
        // Arrange
        testUser.setRecommendationEngineEnabled(true);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(feedBlockPreCalculationService.getOrGenerateBlock(testUserId, 1)).thenReturn(testFeedBlock);
        // Mock the post repository to return posts by IDs
        Map<UUID, Post> postMap = testPosts.stream().collect(Collectors.toMap(Post::getId, post -> post));
        when(postRepository.findAllById(any())).thenAnswer(invocation -> {
            List<UUID> ids = invocation.getArgument(0);
            return ids.stream()
                .map(id -> postMap.getOrDefault(id, createMockPost(id)))
                .collect(Collectors.toList());
        });
        
        // Act
        FeedResponse result = postService.getFeed(0, 20, "newest", null, null, null, testUserId);
        
        // Assert
        assertNotNull(result);
        assertEquals(0, result.getCurrentPage());
        assertEquals(1, result.getTotalPages());
        assertEquals(2, result.getTotalElements());
        assertFalse(result.isHasNext());
        
        // Verify personalized feed was called
        verify(feedBlockPreCalculationService).getOrGenerateBlock(testUserId, 1);
        verify(postRepository).findAllById(testFeedBlock.getPostIds());
        
        // Verify chronological feed was NOT called
        verify(postRepository, never()).findAll(any(org.springframework.data.domain.Pageable.class));
        
        logger.info("✓ Test passed: Recommendation enabled routes to personalized feed");
    }

    @Test
    void testGetFeed_RecommendationDisabled_RoutesToChronologicalFeed() {
        // Arrange
        testUser.setRecommendationEngineEnabled(false);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(postRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new org.springframework.data.domain.PageImpl<>(testPosts));
        
        // Act
        FeedResponse result = postService.getFeed(0, 20, "newest", null, null, null, testUserId);
        
        // Assert
        assertNotNull(result);
        assertEquals(0, result.getCurrentPage());
        assertEquals(2, result.getTotalElements());
        
        // Verify chronological feed was called
        verify(postRepository).findAll(any(org.springframework.data.domain.Pageable.class));
        
        // Verify personalized feed was NOT called
        verify(feedBlockPreCalculationService, never()).getOrGenerateBlock(any(), anyInt());
        
        logger.info("✓ Test passed: Recommendation disabled routes to chronological feed");
    }

    @Test
    void testGetFeed_PersonalizedFeedFails_FallsBackToChronologicalFeed() {
        // Arrange
        testUser.setRecommendationEngineEnabled(true);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(feedBlockPreCalculationService.getOrGenerateBlock(testUserId, 1))
            .thenThrow(new RuntimeException("Personalized feed generation failed"));
        when(postRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new org.springframework.data.domain.PageImpl<>(testPosts));
        
        // Act
        FeedResponse result = postService.getFeed(0, 20, "newest", null, null, null, testUserId);
        
        // Assert
        assertNotNull(result);
        assertEquals(0, result.getCurrentPage());
        assertEquals(2, result.getTotalElements());
        
        // Verify both services were called (personalized failed, chronological succeeded)
        verify(feedBlockPreCalculationService).getOrGenerateBlock(testUserId, 1);
        verify(postRepository).findAll(any(org.springframework.data.domain.Pageable.class));
        
        logger.info("✓ Test passed: Personalized feed failure falls back to chronological feed");
    }

    @Test
    void testGetFeed_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            postService.getFeed(0, 20, "newest", null, null, null, testUserId);
        });
        
        // Verify no other services were called
        verify(feedBlockPreCalculationService, never()).getOrGenerateBlock(any(), anyInt());
        verify(postRepository, never()).findAll(any(org.springframework.data.domain.Pageable.class));
        
        logger.info("✓ Test passed: User not found throws ResourceNotFoundException");
    }

    @Test
    void testGetFeed_RecommendationEnabled_WithEmotionFilter_StillRoutesToPersonalizedFeed() {
        // Arrange
        testUser.setRecommendationEngineEnabled(true);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(feedBlockPreCalculationService.getOrGenerateBlock(testUserId, 1)).thenReturn(testFeedBlock);
        // Mock the post repository to return posts by IDs
        Map<UUID, Post> postMap = testPosts.stream().collect(Collectors.toMap(Post::getId, post -> post));
        when(postRepository.findAllById(any())).thenAnswer(invocation -> {
            List<UUID> ids = invocation.getArgument(0);
            return ids.stream()
                .map(id -> postMap.getOrDefault(id, createMockPost(id)))
                .collect(Collectors.toList());
        });
        
        // Act
        FeedResponse result = postService.getFeed(0, 20, "newest", "happy", null, null, testUserId);
        
        // Assert
        assertNotNull(result);
        assertEquals(0, result.getCurrentPage());
        
        // Verify personalized feed was called (emotion filter ignored for personalized feed)
        verify(feedBlockPreCalculationService).getOrGenerateBlock(testUserId, 1);
        verify(postRepository).findAllById(testFeedBlock.getPostIds());
        
        // Verify chronological feed was NOT called
        verify(postRepository, never()).findByEmotionIgnoreCase(any(), any(org.springframework.data.domain.Pageable.class));
        
        logger.info("✓ Test passed: Emotion filter ignored for personalized feed");
    }

    @Test
    void testGetFeed_RecommendationDisabled_WithEmotionFilter_RoutesToChronologicalFeed() {
        // Arrange
        testUser.setRecommendationEngineEnabled(false);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(postRepository.findByEmotionIgnoreCase(any(), any(org.springframework.data.domain.Pageable.class))).thenReturn(new org.springframework.data.domain.PageImpl<>(testPosts));
        
        // Act
        FeedResponse result = postService.getFeed(0, 20, "newest", "happy", null, null, testUserId);
        
        // Assert
        assertNotNull(result);
        assertEquals(0, result.getCurrentPage());
        
        // Verify chronological feed was called with emotion filter
        verify(postRepository).findByEmotionIgnoreCase(eq("happy"), any(org.springframework.data.domain.Pageable.class));
        
        // Verify personalized feed was NOT called
        verify(feedBlockPreCalculationService, never()).getOrGenerateBlock(any(), anyInt());
        
        logger.info("✓ Test passed: Emotion filter applied to chronological feed");
    }

    @Test
    void testGetFeed_MultiplePages_RecommendationEnabled_GeneratesCorrectBlocks() {
        // Arrange
        testUser.setRecommendationEngineEnabled(true);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(feedBlockPreCalculationService.getOrGenerateBlock(testUserId, 1)).thenReturn(testFeedBlock);
        when(feedBlockPreCalculationService.getOrGenerateBlock(testUserId, 2)).thenReturn(testFeedBlock);
        // Mock the post repository to return posts by IDs
        Map<UUID, Post> postMap = testPosts.stream().collect(Collectors.toMap(Post::getId, post -> post));
        when(postRepository.findAllById(any())).thenAnswer(invocation -> {
            List<UUID> ids = invocation.getArgument(0);
            return ids.stream()
                .map(id -> postMap.getOrDefault(id, createMockPost(id)))
                .collect(Collectors.toList());
        });
        
        // Act - Test page 0
        FeedResponse result1 = postService.getFeed(0, 20, "newest", null, null, null, testUserId);
        
        // Act - Test page 1
        FeedResponse result2 = postService.getFeed(1, 20, "newest", null, null, null, testUserId);
        
        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(0, result1.getCurrentPage());
        assertEquals(1, result2.getCurrentPage());
        
        // Verify correct block numbers were requested
        verify(feedBlockPreCalculationService).getOrGenerateBlock(testUserId, 1);
        verify(feedBlockPreCalculationService).getOrGenerateBlock(testUserId, 2);
        
        logger.info("✓ Test passed: Multiple pages generate correct feed blocks");
    }
}

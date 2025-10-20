package ai.youthmind.service;

import ai.youthmind.dto.ModerationResult;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserModerationStatusRepository;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.repository.WarningHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncModerationServiceBatchTest {

    @Mock
    private AIServiceClient aiServiceClient;

    @Mock
    private DeepSeekAdapterService deepSeekAdapterService;

    @Mock
    private PostModerationService postModerationService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserModerationStatusRepository moderationStatusRepository;

    @Mock
    private WarningHistoryRepository warningHistoryRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AsyncModerationService asyncModerationService;

    private List<UUID> postIds;
    private List<Post> posts;
    private List<ModerationResult> batchResults;

    @BeforeEach
    void setUp() {
        postIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        
        posts = new ArrayList<>();
        for (int i = 0; i < postIds.size(); i++) {
            Post post = new Post();
            post.setId(postIds.get(i));
            post.setContent("Test content " + i);
            post.setUserId(UUID.randomUUID());
            posts.add(post);
        }

        batchResults = new ArrayList<>();
        for (int i = 0; i < posts.size(); i++) {
            ModerationResult result = new ModerationResult();
            result.setSafe(true);
            result.setConfidence(0.9);
            result.setReason("Test moderation result " + i);
            batchResults.add(result);
        }

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null); // No cached results
    }

    @Test
    void testProcessBatchModerationAsync_UsesBatchModeration() {
        // Arrange
        when(postRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(posts.get(0)))
            .thenReturn(Optional.of(posts.get(1)))
            .thenReturn(Optional.of(posts.get(2)));

        when(deepSeekAdapterService.moderateContentBatch(anyList()))
            .thenReturn(batchResults);

        // Act
        asyncModerationService.processBatchModerationAsync(postIds).join();

        // Assert
        verify(deepSeekAdapterService, times(1)).moderateContentBatch(anyList());
        verify(postModerationService, never()).moderatePost(any(Post.class), any(UUID.class));
        verify(aiServiceClient, never()).moderateContent(anyString(), anyString(), any(UUID.class), anyString());
        
        // Verify that posts are updated
        verify(postRepository, atLeastOnce()).save(any(Post.class));
    }

    @Test
    void testProcessBatchModerationAsync_WithCachedResults() {
        // Arrange
        when(postRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(posts.get(0)))
            .thenReturn(Optional.of(posts.get(1)))
            .thenReturn(Optional.of(posts.get(2)));

        // First post is cached
        ModerationResult cachedResult = new ModerationResult();
        cachedResult.setSafe(true);
        cachedResult.setConfidence(0.8);
        cachedResult.setReason("Cached result");
        
        when(valueOperations.get("moderation:post:" + posts.get(0).getId()))
            .thenReturn(cachedResult);
        when(valueOperations.get("moderation:post:" + posts.get(1).getId()))
            .thenReturn(null);
        when(valueOperations.get("moderation:post:" + posts.get(2).getId()))
            .thenReturn(null);

        // Only 2 posts need AI moderation (1 is cached)
        List<ModerationResult> partialBatchResults = Arrays.asList(
            batchResults.get(1), batchResults.get(2)
        );
        when(deepSeekAdapterService.moderateContentBatch(anyList()))
            .thenReturn(partialBatchResults);

        // Act
        asyncModerationService.processBatchModerationAsync(postIds).join();

        // Assert
        verify(deepSeekAdapterService, times(1)).moderateContentBatch(anyList());
        // Verify that the batch request only contains 2 posts (not 3)
        verify(deepSeekAdapterService).moderateContentBatch(argThat(list -> list.size() == 2));
        verify(postModerationService, never()).moderatePost(any(Post.class), any(UUID.class));
    }

    @Test
    void testProcessBatchModerationAsync_AllCached() {
        // Arrange
        when(postRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(posts.get(0)))
            .thenReturn(Optional.of(posts.get(1)))
            .thenReturn(Optional.of(posts.get(2)));

        // All posts are cached
        ModerationResult cachedResult = new ModerationResult();
        cachedResult.setSafe(true);
        cachedResult.setConfidence(0.8);
        cachedResult.setReason("Cached result");
        
        when(valueOperations.get(anyString())).thenReturn(cachedResult);

        // Act
        asyncModerationService.processBatchModerationAsync(postIds).join();

        // Assert
        verify(deepSeekAdapterService, never()).moderateContentBatch(anyList());
        verify(postModerationService, never()).moderatePost(any(Post.class), any(UUID.class));
        verify(aiServiceClient, never()).moderateContent(anyString(), anyString(), any(UUID.class), anyString());
    }

    @Test
    void testProcessBatchModerationAsync_EmptyPostList() {
        // Arrange
        List<UUID> emptyPostIds = Collections.emptyList();

        // Act
        asyncModerationService.processBatchModerationAsync(emptyPostIds).join();

        // Assert
        verify(deepSeekAdapterService, never()).moderateContentBatch(anyList());
        verify(postModerationService, never()).moderatePost(any(Post.class), any(UUID.class));
        verify(aiServiceClient, never()).moderateContent(anyString(), anyString(), any(UUID.class), anyString());
    }

    @Test
    void testProcessModerationAsync_UsesPostModerationService() {
        // Arrange
        UUID postId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String content = "Test content";
        
        Post post = new Post();
        post.setId(postId);
        post.setContent(content);
        post.setUserId(userId);
        
        ModerationResult expectedResult = new ModerationResult();
        expectedResult.setSafe(true);
        expectedResult.setConfidence(0.9);
        expectedResult.setReason("Test result");
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postModerationService.moderatePost(post, userId)).thenReturn(expectedResult);

        // Act
        CompletableFuture<ModerationResult> future = asyncModerationService.processModerationAsync(postId, content, userId);
        ModerationResult result = future.join();

        // Assert
        verify(postModerationService, times(1)).moderatePost(post, userId);
        verify(aiServiceClient, never()).moderateContent(anyString(), anyString(), any(UUID.class), anyString());
        assertEquals(expectedResult, result);
    }
}

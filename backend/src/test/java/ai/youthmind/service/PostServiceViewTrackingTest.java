package ai.youthmind.service;

import ai.youthmind.dto.PostDto;
import ai.youthmind.dto.PostViewStats;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.PostView;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.PostViewRepository;
import ai.youthmind.service.PostService;
import ai.youthmind.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("PostService View Tracking Tests")
class PostServiceViewTrackingTest {
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private PostViewRepository postViewRepository;
    
    @Mock
    private RedisService redisService;
    
    @InjectMocks
    private PostService postService;
    
    private UUID testPostId;
    private UUID testUserId;
    private Post testPost;
    private PostView testPostView;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testPostId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        
        testPost = new Post();
        testPost.setId(testPostId);
        testPost.setUserId(testUserId);
        testPost.setTitle("Test Post");
        testPost.setContent("Test Content");
        testPost.setEmotion("happy");
        testPost.setView(5);
        testPost.setResonance(3);
        testPost.setCollect(1);
        testPost.setCreatedAt(LocalDateTime.now());
        
        testPostView = new PostView(testUserId, testPostId);
        testPostView.setId(UUID.randomUUID());
        testPostView.setViewedAt(LocalDateTime.now());
    }
    
    @Test
    @DisplayName("Should track first-time view and increment count")
    void shouldTrackFirstTimeViewAndIncrementCount() {
        // Arrange
        when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
        when(postViewRepository.existsByUserIdAndPostId(testUserId, testPostId)).thenReturn(false);
        when(postViewRepository.save(any(PostView.class))).thenReturn(testPostView);
        when(postRepository.save(any(Post.class))).thenReturn(testPost);
        
        // Act
        PostDto result = postService.trackPostView(testPostId, testUserId);
        
        // Assert
        assertNotNull(result);
        assertEquals(6, result.getView()); // Should be incremented from 5 to 6
        
        // Verify interactions
        verify(postViewRepository).existsByUserIdAndPostId(testUserId, testPostId);
        verify(postViewRepository).save(any(PostView.class));
        verify(postRepository).save(testPost);
        verify(redisService).invalidateFeedCache();
    }
    
    @Test
    @DisplayName("Should not increment count for repeat views")
    void shouldNotIncrementCountForRepeatViews() {
        // Arrange
        when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
        when(postViewRepository.existsByUserIdAndPostId(testUserId, testPostId)).thenReturn(true);
        
        // Act
        PostDto result = postService.trackPostView(testPostId, testUserId);
        
        // Assert
        assertNotNull(result);
        assertEquals(5, result.getView()); // Should remain unchanged
        
        // Verify interactions
        verify(postViewRepository).existsByUserIdAndPostId(testUserId, testPostId);
        verify(postViewRepository, never()).save(any(PostView.class));
        verify(postRepository, never()).save(any(Post.class));
        verify(redisService, never()).invalidateFeedCache();
    }
    
    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent post")
    void shouldThrowResourceNotFoundExceptionForNonExistentPost() {
        // Arrange
        when(postRepository.findById(testPostId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            postService.trackPostView(testPostId, testUserId);
        });
        
        // Verify no interactions with view repository
        verify(postViewRepository, never()).existsByUserIdAndPostId(any(), any());
        verify(postViewRepository, never()).save(any(PostView.class));
    }
    
    @Test
    @DisplayName("Should get view statistics correctly")
    void shouldGetViewStatisticsCorrectly() {
        // Arrange
        when(postRepository.existsById(testPostId)).thenReturn(true);
        when(postViewRepository.countByPostId(testPostId)).thenReturn(150L);
        when(postViewRepository.countByPostIdAndViewedAtAfter(testPostId, any(LocalDateTime.class))).thenReturn(25L);
        when(postViewRepository.findByPostIdOrderByViewedAtDesc(testPostId)).thenReturn(
            java.util.List.of(testPostView)
        );
        
        // Act
        PostViewStats stats = postService.getPostViewStats(testPostId);
        
        // Assert
        assertNotNull(stats);
        assertEquals(testPostId, stats.getPostId());
        assertEquals(150L, stats.getTotalViews());
        assertEquals(25L, stats.getRecentViews());
        assertEquals(1, stats.getUniqueViewers());
        
        // Verify interactions
        verify(postRepository).existsById(testPostId);
        verify(postViewRepository).countByPostId(testPostId);
        verify(postViewRepository).countByPostIdAndViewedAtAfter(testPostId, any(LocalDateTime.class));
        verify(postViewRepository).findByPostIdOrderByViewedAtDesc(testPostId);
    }
    
    @Test
    @DisplayName("Should throw ResourceNotFoundException for view stats of non-existent post")
    void shouldThrowResourceNotFoundExceptionForViewStatsOfNonExistentPost() {
        // Arrange
        when(postRepository.existsById(testPostId)).thenReturn(false);
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            postService.getPostViewStats(testPostId);
        });
        
        // Verify no interactions with view repository
        verify(postViewRepository, never()).countByPostId(any());
        verify(postViewRepository, never()).countByPostIdAndViewedAtAfter(any(), any());
    }
    
    @Test
    @DisplayName("Should handle zero views correctly in statistics")
    void shouldHandleZeroViewsCorrectlyInStatistics() {
        // Arrange
        when(postRepository.existsById(testPostId)).thenReturn(true);
        when(postViewRepository.countByPostId(testPostId)).thenReturn(0L);
        when(postViewRepository.countByPostIdAndViewedAtAfter(testPostId, any(LocalDateTime.class))).thenReturn(0L);
        when(postViewRepository.findByPostIdOrderByViewedAtDesc(testPostId)).thenReturn(java.util.List.of());
        
        // Act
        PostViewStats stats = postService.getPostViewStats(testPostId);
        
        // Assert
        assertNotNull(stats);
        assertEquals(0L, stats.getTotalViews());
        assertEquals(0L, stats.getRecentViews());
        assertEquals(0, stats.getUniqueViewers());
        assertEquals(0.0, stats.getViewGrowthRate());
        assertFalse(stats.hasHighEngagement());
        assertEquals("Very Low", stats.getEngagementLevel());
    }
    
    @Test
    @DisplayName("Should calculate engagement metrics correctly")
    void shouldCalculateEngagementMetricsCorrectly() {
        // Arrange
        when(postRepository.existsById(testPostId)).thenReturn(true);
        when(postViewRepository.countByPostId(testPostId)).thenReturn(100L);
        when(postViewRepository.countByPostIdAndViewedAtAfter(testPostId, any(LocalDateTime.class))).thenReturn(15L);
        when(postViewRepository.findByPostIdOrderByViewedAtDesc(testPostId)).thenReturn(
            java.util.List.of(testPostView)
        );
        
        // Act
        PostViewStats stats = postService.getPostViewStats(testPostId);
        
        // Assert
        assertNotNull(stats);
        assertEquals(15.0, stats.getViewGrowthRate()); // 15/100 * 100 = 15%
        assertTrue(stats.hasHighEngagement()); // 15% > 10%
        assertEquals("High", stats.getEngagementLevel());
    }
    
    @Test
    @DisplayName("Should handle view tracking with null userId gracefully")
    void shouldHandleViewTrackingWithNullUserIdGracefully() {
        // Arrange
        when(postRepository.findById(testPostId)).thenReturn(Optional.of(testPost));
        when(postViewRepository.existsByUserIdAndPostId(null, testPostId)).thenReturn(false);
        when(postViewRepository.save(any(PostView.class))).thenReturn(testPostView);
        when(postRepository.save(any(Post.class))).thenReturn(testPost);
        
        // Act
        PostDto result = postService.trackPostView(testPostId, null);
        
        // Assert
        assertNotNull(result);
        assertEquals(6, result.getView()); // Should still increment
        
        // Verify interactions
        verify(postViewRepository).existsByUserIdAndPostId(null, testPostId);
        verify(postViewRepository).save(any(PostView.class));
        verify(postRepository).save(testPost);
    }
} 
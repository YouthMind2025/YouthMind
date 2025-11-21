package ai.youthmind.service;

import ai.youthmind.dto.PostDto;
import ai.youthmind.dto.PostUpdateRequest;
import ai.youthmind.entity.Post;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.repository.CommentRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PostService updatePost method
 */
@ExtendWith(MockitoExtension.class)
class PostServiceUpdateTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private PostService postService;

    private UUID postId;
    private UUID userId;
    private Post existingPost;
    private PostUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        existingPost = new Post();
        existingPost.setId(postId);
        existingPost.setUserId(userId);
        existingPost.setTitle("Original Title");
        existingPost.setContent("Original content");
        existingPost.setEmotion("sad");
        existingPost.setPreImg("https://example.com/original.jpg");
        existingPost.setTopics("original, test");
        existingPost.setCreatedAt(LocalDateTime.now().minusDays(1));
        existingPost.setUpdatedAt(LocalDateTime.now().minusDays(1));
        
        updateRequest = new PostUpdateRequest();
        
        // Mock comment count for all tests
        when(commentRepository.countByPostId(any(UUID.class))).thenReturn(0L);
    }

    @Test
    void testUpdatePost_SuccessfulUpdate() {
        // Arrange
        updateRequest.setTitle("Updated Title");
        updateRequest.setContent("Updated content");
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);
        
        // Act
        PostDto result = postService.updatePost(postId, userId, updateRequest);
        
        // Assert
        assertNotNull(result);
        verify(postRepository).findById(postId);
        verify(postRepository).save(any(Post.class));
        verify(redisService).invalidateFeedCache();
        
        // Verify the post was updated
        assertEquals("Updated Title", existingPost.getTitle());
        assertEquals("Updated content", existingPost.getContent());
        assertNotNull(existingPost.getUpdatedAt());
        assertTrue(existingPost.getUpdatedAt().isAfter(existingPost.getCreatedAt()));
    }

    @Test
    void testUpdatePost_PostNotFound() {
        // Arrange
        when(postRepository.findById(postId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            postService.updatePost(postId, userId, updateRequest);
        });
        
        verify(postRepository).findById(postId);
        verify(postRepository, never()).save(any(Post.class));
        verify(redisService, never()).invalidateFeedCache();
    }

    @Test
    void testUpdatePost_UnauthorizedUser() {
        // Arrange
        UUID differentUserId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            postService.updatePost(postId, differentUserId, updateRequest);
        });
        
        verify(postRepository).findById(postId);
        verify(postRepository, never()).save(any(Post.class));
        verify(redisService, never()).invalidateFeedCache();
    }

    @Test
    void testUpdatePost_NoUpdatesProvided() {
        // Arrange
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            postService.updatePost(postId, userId, updateRequest);
        });
        
        verify(postRepository).findById(postId);
        verify(postRepository, never()).save(any(Post.class));
        verify(redisService, never()).invalidateFeedCache();
    }

    @Test
    void testUpdatePost_InvalidEmotion() {
        // Arrange
        updateRequest.setEmotion("invalid_emotion");
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            postService.updatePost(postId, userId, updateRequest);
        });
        
        verify(postRepository).findById(postId);
        verify(postRepository, never()).save(any(Post.class));
        verify(redisService, never()).invalidateFeedCache();
    }

    @Test
    void testUpdatePost_PartialUpdate() {
        // Arrange
        updateRequest.setTitle("Only Title Update");
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);
        
        // Act
        PostDto result = postService.updatePost(postId, userId, updateRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("Only Title Update", existingPost.getTitle());
        assertEquals("Original content", existingPost.getContent()); // Should remain unchanged
        assertEquals("sad", existingPost.getEmotion()); // Should remain unchanged
        verify(postRepository).save(any(Post.class));
        verify(redisService).invalidateFeedCache();
    }

    @Test
    void testUpdatePost_EmotionUpdate() {
        // Arrange
        updateRequest.setEmotion("happy");
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);
        
        // Act
        PostDto result = postService.updatePost(postId, userId, updateRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("happy", existingPost.getEmotion());
        assertEquals("Original Title", existingPost.getTitle()); // Should remain unchanged
        verify(postRepository).save(any(Post.class));
        verify(redisService).invalidateFeedCache();
    }

    @Test
    void testUpdatePost_ImageUpdate() {
        // Arrange
        updateRequest.setPreImg("https://example.com/new-image.jpg");
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);
        
        // Act
        PostDto result = postService.updatePost(postId, userId, updateRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("https://example.com/new-image.jpg", existingPost.getPreImg());
        verify(postRepository).save(any(Post.class));
        verify(redisService).invalidateFeedCache();
    }

    @Test
    void testUpdatePost_TopicsUpdate() {
        // Arrange
        updateRequest.setTopics("new, topics, here");
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);
        
        // Act
        PostDto result = postService.updatePost(postId, userId, updateRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("new, topics, here", existingPost.getTopics());
        verify(postRepository).save(any(Post.class));
        verify(redisService).invalidateFeedCache();
    }

    @Test
    void testUpdatePost_TrimWhitespace() {
        // Arrange
        updateRequest.setTitle("  Title with spaces  ");
        updateRequest.setContent("  Content with spaces  ");
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);
        
        // Act
        PostDto result = postService.updatePost(postId, userId, updateRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("Title with spaces", existingPost.getTitle());
        assertEquals("Content with spaces", existingPost.getContent());
        verify(postRepository).save(any(Post.class));
        verify(redisService).invalidateFeedCache();
    }

    @Test
    void testUpdatePost_EmptyStringHandling() {
        // Arrange
        updateRequest.setPreImg("");
        updateRequest.setTopics("");
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);
        
        // Act
        PostDto result = postService.updatePost(postId, userId, updateRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("", existingPost.getPreImg());
        assertEquals("", existingPost.getTopics());
        verify(postRepository).save(any(Post.class));
        verify(redisService).invalidateFeedCache();
    }
} 
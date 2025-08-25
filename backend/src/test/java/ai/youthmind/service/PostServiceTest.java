package ai.youthmind.service;

import ai.youthmind.dto.PostDto;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.PostResonance;
import ai.youthmind.entity.PostView;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.PostResonanceRepository;
import ai.youthmind.repository.PostViewRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private PostViewRepository postViewRepository;
    
    @Mock
    private PostResonanceRepository postResonanceRepository;
    
    @Mock
    private RedisService redisService;
    
    @InjectMocks
    private PostService postService;
    
    private UUID userId;
    private UUID postId;
    private Post testPost;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        postId = UUID.randomUUID();
        
        testPost = new Post();
        testPost.setId(postId);
        testPost.setUserId(userId);
        testPost.setTitle("Test Post");
        testPost.setContent("Test content");
        testPost.setEmotion("happy");
        testPost.setView(5);
        testPost.setResonance(10);
        testPost.setCollect(2);
        testPost.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void getPostById_WhenPostExists_ShouldReturnPostDto() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(postViewRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);
        when(postViewRepository.save(any(PostView.class))).thenReturn(new PostView(userId, postId));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);
        
        // When
        PostDto result = postService.getPostById(postId, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(postId);
        assertThat(result.getTitle()).isEqualTo("Test Post");
        assertThat(result.getContent()).isEqualTo("Test content");
        assertThat(result.getEmotion()).isEqualTo("happy");
        assertThat(result.getView()).isEqualTo(6); // Should be incremented from 5 to 6
        assertThat(result.getResonance()).isEqualTo(10);
        assertThat(result.getCollect()).isEqualTo(2);
        
        // Verify view tracking was recorded
        verify(postViewRepository).existsByUserIdAndPostId(userId, postId);
        verify(postViewRepository).save(any(PostView.class));
        verify(postRepository).save(any(Post.class));
        verify(redisService).invalidateFeedCache();
    }
    
    @Test
    void getPostById_WhenPostExistsAndUserAlreadyViewed_ShouldNotIncrementViewCount() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(postViewRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(true);
        
        // When
        PostDto result = postService.getPostById(postId, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getView()).isEqualTo(5); // Should remain unchanged
        
        // Verify no view tracking was recorded
        verify(postViewRepository).existsByUserIdAndPostId(userId, postId);
        verify(postViewRepository, never()).save(any(PostView.class));
        verify(postRepository, never()).save(any(Post.class));
        verify(redisService, never()).invalidateFeedCache();
    }
    
    @Test
    void getPostById_WhenPostNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> postService.getPostById(postId, userId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Post not found with id");
        
        // Verify no view tracking was attempted
        verify(postViewRepository, never()).existsByUserIdAndPostId(any(), any());
        verify(postViewRepository, never()).save(any(PostView.class));
        verify(postRepository, never()).save(any(Post.class));
        verify(redisService, never()).invalidateFeedCache();
    }
    
    @Test
    void getPostById_WhenFirstTimeViewing_ShouldCreatePostViewRecord() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(postViewRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);
        when(postViewRepository.save(any(PostView.class))).thenReturn(new PostView(userId, postId));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);
        
        // When
        postService.getPostById(postId, userId);
        
        // Then
        verify(postViewRepository).save(argThat(postView -> 
            postView.getUserId().equals(userId) && 
            postView.getPostId().equals(postId)
        ));
    }
    
    @Test
    void getPostById_WhenViewCountIncremented_ShouldUpdatePostEntity() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(postViewRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);
        when(postViewRepository.save(any(PostView.class))).thenReturn(new PostView(userId, postId));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);
        
        // When
        postService.getPostById(postId, userId);
        
        // Then
        verify(postRepository).save(argThat(post -> 
            post.getView() == 6 // Should be incremented from 5 to 6
        ));
    }

    @Test
    void toggleResonance_ShouldAddResonance_WhenUserHasNotResonanced() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(postResonanceRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);
        
        // When
        PostDto result = postService.toggleResonance(postId, userId);
        
        // Then
        verify(postResonanceRepository).save(any(PostResonance.class));
        verify(postRepository).save(testPost);
        assertThat(testPost.getResonance()).isEqualTo(11); // Resonance count should be incremented
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(postId);
    }
    
    @Test
    void toggleResonance_ShouldRemoveResonance_WhenUserHasAlreadyResonanced() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(postResonanceRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(true);
        
        // When
        PostDto result = postService.toggleResonance(postId, userId);
        
        // Then
        verify(postResonanceRepository).deleteByUserIdAndPostId(userId, postId);
        verify(postRepository).save(testPost);
        assertThat(testPost.getResonance()).isEqualTo(9); // Resonance count should be decremented
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(postId);
    }
    
    @Test
    void toggleResonance_ShouldThrowException_WhenPostNotFound() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> postService.toggleResonance(postId, userId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Post not found with id");
    }
} 
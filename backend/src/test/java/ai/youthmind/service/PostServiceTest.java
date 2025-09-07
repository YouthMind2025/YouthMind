package ai.youthmind.service;

import ai.youthmind.dto.PostDto;
import ai.youthmind.dto.FeedResponse;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.PostCollection;
import ai.youthmind.entity.PostResonance;
import ai.youthmind.entity.PostView;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.repository.CommentRepository;
import ai.youthmind.repository.PostCollectionRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private PostViewRepository postViewRepository;
    
    @Mock
    private PostResonanceRepository postResonanceRepository;
    
    @Mock
    private PostCollectionRepository postCollectionRepository;
    
    @Mock
    private CommentRepository commentRepository;
    
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
        
        // Mock comment count for all tests (lenient to avoid unnecessary stubbing errors)
        lenient().when(commentRepository.countByPostId(any(UUID.class))).thenReturn(0L);
    }
    
    @Test
    void getPostById_WhenPostExists_ShouldReturnPostDto() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(postViewRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);
        when(postViewRepository.save(any(PostView.class))).thenReturn(new PostView(userId, postId));
        
        // When
        PostDto result = postService.getPostById(postId, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(postId);
        assertThat(result.getTitle()).isEqualTo("Test Post");
        assertThat(result.getContent()).isEqualTo("Test content");
        assertThat(result.getEmotion()).isEqualTo("happy");
        assertThat(result.getView()).isEqualTo(5); // Original view count (increment happens async)
        assertThat(result.getResonance()).isEqualTo(10);
        assertThat(result.getCollect()).isEqualTo(2);
        
        // Verify view tracking was recorded
        verify(postViewRepository).existsByUserIdAndPostId(userId, postId);
        verify(postViewRepository).save(any(PostView.class));
        verify(postRepository).incrementViewCount(postId);
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
        
        // When
        postService.getPostById(postId, userId);
        
        // Then
        verify(postRepository).incrementViewCount(postId); // View count incremented via native query
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

    @Test
    void toggleCollection_ShouldAddCollection_WhenUserHasNotCollected() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(postCollectionRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);
        
        // When
        PostDto result = postService.toggleCollection(postId, userId);
        
        // Then
        verify(postCollectionRepository).save(any(PostCollection.class));
        verify(postRepository).save(testPost);
        assertThat(testPost.getCollect()).isEqualTo(3); // Collect count should be incremented from 2 to 3
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(postId);
    }
    
    @Test
    void toggleCollection_ShouldRemoveCollection_WhenUserHasAlreadyCollected() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(postCollectionRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(true);
        
        // When
        PostDto result = postService.toggleCollection(postId, userId);
        
        // Then
        verify(postCollectionRepository).deleteByUserIdAndPostId(userId, postId);
        verify(postRepository).save(testPost);
        assertThat(testPost.getCollect()).isEqualTo(1); // Collect count should be decremented from 2 to 1
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(postId);
    }
    
    @Test
    void toggleCollection_ShouldThrowException_WhenPostNotFound() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> postService.toggleCollection(postId, userId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Post not found with id");
    }

    @Test
    void searchPosts_ShouldReturnResults_WhenValidQueryProvided() {
        // Given
        String query = "mental health";
        String emotion = "happy";
        int page = 0;
        int size = 20;
        String sortBy = "relevance";
        
        Post post1 = createTestPost("Mental Health Tips", "How to maintain good mental health", "happy");
        Post post2 = createTestPost("Wellness Guide", "Mental wellness strategies", "happy");
        List<Post> posts = Arrays.asList(post1, post2);
        Page<Post> postPage = new PageImpl<>(posts, PageRequest.of(page, size), 2);
        
        when(postRepository.searchPostsByEmotion(query, emotion, any(Pageable.class)))
            .thenReturn(postPage);
        when(redisService.getCachedFeed(anyString())).thenReturn(null);
        
        // When
        FeedResponse result = postService.searchPosts(query, emotion, page, size, sortBy, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPosts()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getCurrentPage()).isEqualTo(0);
        
        verify(postRepository).searchPostsByEmotion(query, emotion, any(Pageable.class));
        verify(redisService).cacheFeed(anyString(), any(FeedResponse.class));
    }
    
    @Test
    void searchPosts_ShouldReturnResults_WhenNoEmotionFilter() {
        // Given
        String query = "anxiety";
        String emotion = null;
        int page = 0;
        int size = 20;
        String sortBy = "newest";
        
        Post post1 = createTestPost("Anxiety Help", "Coping with anxiety", "fear");
        Post post2 = createTestPost("Calm Mind", "Reducing anxiety naturally", "calm");
        List<Post> posts = Arrays.asList(post1, post2);
        Page<Post> postPage = new PageImpl<>(posts, PageRequest.of(page, size), 2);
        
        when(postRepository.searchPosts(query, any(Pageable.class)))
            .thenReturn(postPage);
        when(redisService.getCachedFeed(anyString())).thenReturn(null);
        
        // When
        FeedResponse result = postService.searchPosts(query, emotion, page, size, sortBy, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPosts()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        
        verify(postRepository).searchPosts(query, any(Pageable.class));
        verify(postRepository, never()).searchPostsByEmotion(anyString(), anyString(), any(Pageable.class));
    }
    
    @Test
    void searchPosts_ShouldThrowException_WhenEmptyQuery() {
        // Given
        String query = "";
        String emotion = "happy";
        int page = 0;
        int size = 20;
        String sortBy = "relevance";
        
        // When & Then
        assertThatThrownBy(() -> postService.searchPosts(query, emotion, page, size, sortBy, userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Search query cannot be empty");
    }
    
    @Test
    void searchPosts_ShouldThrowException_WhenNullQuery() {
        // Given
        String query = null;
        String emotion = "happy";
        int page = 0;
        int size = 20;
        String sortBy = "relevance";
        
        // When & Then
        assertThatThrownBy(() -> postService.searchPosts(query, emotion, page, size, sortBy, userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Search query cannot be empty");
    }
    
    @Test
    void searchPostsWithRelevance_ShouldReturnRelevanceRankedResults() {
        // Given
        String query = "depression";
        String emotion = "sad";
        int page = 0;
        int size = 20;
        
        Post post1 = createTestPost("Depression Help", "Understanding depression", "sad"); // Title match
        Post post2 = createTestPost("Mental Health", "Depression symptoms and treatment", "sad"); // Content match
        Post post3 = createTestPost("Wellness", "General mental health tips", "sad"); // Topic match
        List<Post> posts = Arrays.asList(post1, post2, post3);
        Page<Post> postPage = new PageImpl<>(posts, PageRequest.of(page, size), 3);
        
        when(postRepository.searchPostsWithRelevance(query, emotion, any(Pageable.class)))
            .thenReturn(postPage);
        when(redisService.getCachedFeed(anyString())).thenReturn(null);
        
        // When
        FeedResponse result = postService.searchPostsWithRelevance(query, emotion, page, size, userId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPosts()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        
        verify(postRepository).searchPostsWithRelevance(query, emotion, any(Pageable.class));
        verify(redisService).cacheFeed(anyString(), any(FeedResponse.class));
    }
    
    @Test
    void searchPosts_ShouldUseCache_WhenResultsCached() {
        // Given
        String query = "stress";
        String emotion = "anxious";
        int page = 0;
        int size = 20;
        String sortBy = "newest";
        
        FeedResponse cachedResponse = new FeedResponse();
        cachedResponse.setPosts(Arrays.asList(createTestPostDto("Stress Management", "How to manage stress", "anxious")));
        cachedResponse.setTotalElements(1);
        cachedResponse.setCurrentPage(0);
        
        when(redisService.getCachedFeed(anyString())).thenReturn(cachedResponse);
        
        // When
        FeedResponse result = postService.searchPosts(query, emotion, page, size, sortBy, userId);
        
        // Then
        assertThat(result).isEqualTo(cachedResponse);
        verify(redisService).getCachedFeed(anyString());
        verify(postRepository, never()).searchPosts(anyString(), any(Pageable.class));
        verify(postRepository, never()).searchPostsByEmotion(anyString(), anyString(), any(Pageable.class));
    }
    
    private Post createTestPost(String title, String content, String emotion) {
        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setTitle(title);
        post.setContent(content);
        post.setEmotion(emotion);
        post.setCreatedAt(LocalDateTime.now());
        post.setResonance(5);
        post.setView(10);
        post.setCollect(2);
        return post;
    }
    
    private PostDto createTestPostDto(String title, String content, String emotion) {
        PostDto dto = new PostDto();
        dto.setId(UUID.randomUUID());
        dto.setTitle(title);
        dto.setContent(content);
        dto.setEmotion(emotion);
        dto.setDate(LocalDateTime.now());
        dto.setResonance(5);
        dto.setView(10);
        dto.setCollect(2);
        dto.setLiked(false);
        dto.setBookmarked(false);
        return dto;
    }
} 
package ai.youthmind.service;

import ai.youthmind.dto.ModerationHistoryResponse;
import ai.youthmind.dto.ModerationStatusResponse;
import ai.youthmind.dto.ModerationSubmissionRequest;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {
    
    @Mock
    private PostService postService;
    
    @Mock
    private PostRepository postRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private ModerationService moderationService;
    
    private UUID moderatorId;
    private UUID postId;
    private UUID userId;
    private Post testPost;
    private User testUser;
    private ModerationSubmissionRequest request;
    
    @BeforeEach
    void setUp() {
        moderatorId = UUID.randomUUID();
        postId = UUID.randomUUID();
        userId = UUID.randomUUID();
        
        testPost = new Post();
        testPost.setId(postId);
        testPost.setUserId(userId);
        testPost.setTitle("Test Post");
        testPost.setContent("Test content");
        testPost.setEmotion("happy");
        testPost.setFlagged(false);
        testPost.setCreatedAt(LocalDateTime.now());
        
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setRole(User.Role.USER);
        
        request = new ModerationSubmissionRequest();
        request.setContentId(postId);
        request.setContentType("post");
        request.setReason("inappropriate_language");
        request.setDetails("Contains explicit language");
        request.setPriority("medium");
    }
    
    @Test
    void submitForModeration_WhenValidPost_ShouldReturnStatusResponse() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        
        // When
        ModerationStatusResponse response = moderationService.submitForModeration(request, moderatorId);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContentId()).isEqualTo(postId);
        assertThat(response.getContentType()).isEqualTo("post");
        assertThat(response.getStatus()).isEqualTo("pending_review");
        assertThat(response.getReason()).isEqualTo("inappropriate_language");
        assertThat(response.getPriority()).isEqualTo("medium");
        assertThat(response.getSubmittedAt()).isNotNull();
        
        // Verify post was flagged
        verify(postService).flagPost(postId, "inappropriate_language - Contains explicit language");
    }
    
    @Test
    void submitForModeration_WhenPostNotFound_ShouldThrowException() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> moderationService.submitForModeration(request, moderatorId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Post not found");
    }
    
    @Test
    void getModerationStatus_WhenPostExists_ShouldReturnStatus() {
        // Given
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        
        // When
        ModerationStatusResponse response = moderationService.getModerationStatus(postId);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContentId()).isEqualTo(postId);
        assertThat(response.getContentType()).isEqualTo("post");
        assertThat(response.getStatus()).isEqualTo("approved"); // Not flagged
    }
    
    @Test
    void getModerationStatus_WhenPostFlagged_ShouldReturnPendingStatus() {
        // Given
        testPost.setFlagged(true);
        testPost.setFlagReason("inappropriate_language");
        testPost.setFlaggedAt(LocalDateTime.now());
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        
        // When
        ModerationStatusResponse response = moderationService.getModerationStatus(postId);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContentId()).isEqualTo(postId);
        assertThat(response.getContentType()).isEqualTo("post");
        assertThat(response.getStatus()).isEqualTo("pending_review");
        assertThat(response.getReason()).isEqualTo("inappropriate_language");
    }
    
    @Test
    void getModerationHistory_WhenUserExists_ShouldReturnHistory() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(postRepository.findByUserIdAndFlaggedTrue(userId)).thenReturn(List.of(testPost));
        
        // When
        ModerationHistoryResponse response = moderationService.getModerationHistory(userId);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getTotalModerationCases()).isEqualTo(1);
        assertThat(response.getModerationEntries()).hasSize(1);
        assertThat(response.getModerationEntries().get(0).getContentId()).isEqualTo(postId);
        assertThat(response.getModerationEntries().get(0).getContentType()).isEqualTo("post");
    }
    
    @Test
    void getModerationHistory_WhenUserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> moderationService.getModerationHistory(userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User not found");
    }
}

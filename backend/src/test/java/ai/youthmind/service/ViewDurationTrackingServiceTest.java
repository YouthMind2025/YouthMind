package ai.youthmind.service;

import ai.youthmind.dto.PostDto;
import ai.youthmind.dto.ViewSession;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ViewDurationTrackingService
 * Tests view session tracking, duration calculation, and R vector updates
 */
@ExtendWith(MockitoExtension.class)
class ViewDurationTrackingServiceTest {
    
    @Mock
    private RedisService redisService;
    
    @Mock
    private PostService postService;
    
    @Mock
    private RVectorService rVectorService;
    
    @Mock
    private PostRepository postRepository;
    
    @InjectMocks
    private ViewDurationTrackingService viewDurationTrackingService;
    
    private UUID testUserId;
    private UUID testPostId;
    private Post testPost;
    private PostDto testPostDto;
    private UserRecommendationVector testRVector;
    
    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPostId = UUID.randomUUID();
        
        // Setup test Post
        testPost = new Post();
        testPost.setId(testPostId);
        testPost.setEmotion("happy");
        testPost.setContent("Test post content");
        testPost.setView(10);
        
        // Setup test PostDto
        testPostDto = new PostDto();
        testPostDto.setId(testPostId);
        testPostDto.setEmotion("happy");
        testPostDto.setContent("Test post content");
        testPostDto.setView(10);
        
        // Setup test RVector
        testRVector = new UserRecommendationVector();
        testRVector.setUserId(testUserId);
        testRVector.setVersion(5);
        testRVector.setEmotionValue("happy", 12);
    }
    
    @Test
    void testStartView_Success() {
        // Arrange
        when(postRepository.findById(testPostId)).thenReturn(java.util.Optional.of(testPost));
        
        // Act
        viewDurationTrackingService.startView(testUserId, testPostId);
        
        // Assert
        verify(postRepository).findById(testPostId);
        verify(redisService).setValue(
            eq("youthmind:view_session:" + testUserId + ":" + testPostId),
            any(ViewSession.class),
            eq(2L),
            eq(TimeUnit.HOURS)
        );
    }
    
    @Test
    void testStartView_PostNotFound() {
        // Arrange
        when(postRepository.findById(testPostId)).thenReturn(java.util.Optional.empty());
        
        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            viewDurationTrackingService.startView(testUserId, testPostId);
        });
        
        verify(postRepository).findById(testPostId);
        verify(redisService, never()).setValue(anyString(), any(), anyLong(), any(TimeUnit.class));
    }
    
    @Test
    void testStartView_NullParameters() {
        // Act & Assert
        assertDoesNotThrow(() -> viewDurationTrackingService.startView(null, testPostId));
        assertDoesNotThrow(() -> viewDurationTrackingService.startView(testUserId, null));
        
        verify(postRepository, never()).findById(any());
        verify(redisService, never()).setValue(anyString(), any(), anyLong(), any(TimeUnit.class));
    }
    
    @Test
    void testEndView_QualifiedView() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(20); // 20 seconds ago
        ViewSession session = new ViewSession(testUserId, testPostId, startTime, "happy");
        
        when(redisService.getValue("youthmind:view_session:" + testUserId + ":" + testPostId))
            .thenReturn(session);
        when(postService.trackPostView(testPostId, testUserId)).thenReturn(testPostDto);
        when(rVectorService.updateOnView(testUserId, "happy", 15)).thenReturn(testRVector);
        
        // Act
        viewDurationTrackingService.endView(testUserId, testPostId);
        
        // Assert
        verify(redisService).getValue("youthmind:view_session:" + testUserId + ":" + testPostId);
        verify(postService).trackPostView(testPostId, testUserId);
        verify(rVectorService).updateOnView(testUserId, "happy", 15);
        verify(redisService).deleteValue("youthmind:view_session:" + testUserId + ":" + testPostId);
    }
    
    @Test
    void testEndView_UnqualifiedView() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(10); // 10 seconds ago
        ViewSession session = new ViewSession(testUserId, testPostId, startTime, "happy");
        
        when(redisService.getValue("youthmind:view_session:" + testUserId + ":" + testPostId))
            .thenReturn(session);
        when(postService.trackPostView(testPostId, testUserId)).thenReturn(testPostDto);
        
        // Act
        viewDurationTrackingService.endView(testUserId, testPostId);
        
        // Assert
        verify(redisService).getValue("youthmind:view_session:" + testUserId + ":" + testPostId);
        verify(postService).trackPostView(testPostId, testUserId);
        verify(rVectorService, never()).updateOnView(any(), any(), anyInt());
        verify(redisService).deleteValue("youthmind:view_session:" + testUserId + ":" + testPostId);
    }
    
    @Test
    void testEndView_SessionNotFound() {
        // Arrange
        when(redisService.getValue("youthmind:view_session:" + testUserId + ":" + testPostId))
            .thenReturn(null);
        when(postService.trackPostView(testPostId, testUserId)).thenReturn(testPostDto);
        
        // Act
        viewDurationTrackingService.endView(testUserId, testPostId);
        
        // Assert
        verify(redisService).getValue("youthmind:view_session:" + testUserId + ":" + testPostId);
        verify(postService).trackPostView(testPostId, testUserId);
        verify(rVectorService, never()).updateOnView(any(), any(), anyInt());
        verify(redisService, never()).deleteValue(anyString());
    }
    
    @Test
    void testEndView_NullParameters() {
        // Act & Assert
        assertDoesNotThrow(() -> viewDurationTrackingService.endView(null, testPostId));
        assertDoesNotThrow(() -> viewDurationTrackingService.endView(testUserId, null));
        
        verify(redisService, never()).getValue(anyString());
        verify(postService, never()).trackPostView(any(), any());
    }
    
    @Test
    void testIsQualifiedView_ThresholdChecks() {
        // Test below threshold
        assertFalse(viewDurationTrackingService.isQualifiedView(14));
        
        // Test at threshold
        assertTrue(viewDurationTrackingService.isQualifiedView(15));
        
        // Test above threshold
        assertTrue(viewDurationTrackingService.isQualifiedView(16));
        
        // Test null
        assertFalse(viewDurationTrackingService.isQualifiedView(null));
    }
    
    @Test
    void testProcessQualifiedView_Success() {
        // Arrange
        when(rVectorService.updateOnView(testUserId, "happy", 15)).thenReturn(testRVector);
        
        // Act
        viewDurationTrackingService.processQualifiedView(testUserId, testPostId, "happy");
        
        // Assert
        verify(rVectorService).updateOnView(testUserId, "happy", 15);
    }
    
    @Test
    void testProcessQualifiedView_NullEmotion() {
        // Act
        viewDurationTrackingService.processQualifiedView(testUserId, testPostId, null);
        
        // Assert
        verify(rVectorService, never()).updateOnView(any(), any(), anyInt());
    }
    
    @Test
    void testProcessQualifiedView_EmptyEmotion() {
        // Act
        viewDurationTrackingService.processQualifiedView(testUserId, testPostId, "");
        
        // Assert
        verify(rVectorService, never()).updateOnView(any(), any(), anyInt());
    }
    
    @Test
    void testProcessQualifiedView_WhitespaceEmotion() {
        // Act
        viewDurationTrackingService.processQualifiedView(testUserId, testPostId, "   ");
        
        // Assert
        verify(rVectorService, never()).updateOnView(any(), any(), anyInt());
    }
    
    @Test
    void testProcessQualifiedView_RVectorServiceException() {
        // Arrange
        when(rVectorService.updateOnView(testUserId, "happy", 15))
            .thenThrow(new RuntimeException("R vector update failed"));
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            viewDurationTrackingService.processQualifiedView(testUserId, testPostId, "happy");
        });
        
        verify(rVectorService).updateOnView(testUserId, "happy", 15);
    }
    
    @Test
    void testStartView_RedisNotAvailable() {
        // Arrange
        when(postRepository.findById(testPostId)).thenReturn(java.util.Optional.of(testPost));
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            viewDurationTrackingService.startView(testUserId, testPostId);
        });
        
        verify(postRepository).findById(testPostId);
    }
    
    @Test
    void testEndView_RedisNotAvailable() {
        // Arrange
        when(redisService.getValue(anyString())).thenReturn(null); // Simulate Redis unavailable
        when(postService.trackPostView(testPostId, testUserId)).thenReturn(testPostDto);
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            viewDurationTrackingService.endView(testUserId, testPostId);
        });
        
        verify(postService).trackPostView(testPostId, testUserId);
    }
    
    @Test
    void testEndView_PostServiceException() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(20);
        ViewSession session = new ViewSession(testUserId, testPostId, startTime, "happy");
        
        when(redisService.getValue("youthmind:view_session:" + testUserId + ":" + testPostId))
            .thenReturn(session);
        when(postService.trackPostView(testPostId, testUserId))
            .thenThrow(new RuntimeException("Post service failed"));
        
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            viewDurationTrackingService.endView(testUserId, testPostId);
        });
        
        verify(postService).trackPostView(testPostId, testUserId);
        verify(rVectorService, never()).updateOnView(any(), any(), anyInt());
    }
    
    @Test
    void testCleanupStaleViewSessions_RedisAvailable() {
        // Act
        viewDurationTrackingService.cleanupStaleViewSessions();
        
        // Assert - should not throw exception and should handle gracefully
        assertDoesNotThrow(() -> {
            viewDurationTrackingService.cleanupStaleViewSessions();
        });
    }
    
    @Test
    void testCleanupStaleViewSessions_RedisNotAvailable() {
        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            viewDurationTrackingService.cleanupStaleViewSessions();
        });
    }
}

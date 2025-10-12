package ai.youthmind.service;

import ai.youthmind.entity.Post;
import ai.youthmind.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PostSelectionService
 * Tests business logic with mocked dependencies for fast execution
 */
@ExtendWith(MockitoExtension.class)
public class PostSelectionServiceTest {

    @InjectMocks
    private PostSelectionService postSelectionService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private RVectorService rVectorService;

    @Mock
    private RecommendationMappingService recommendationMappingService;

    private LocalDateTime testCutoffTime;
    private Set<UUID> testExcludeIds;

    @BeforeEach
    void setUp() {
        testCutoffTime = LocalDateTime.now().minusDays(7);
        testExcludeIds = new HashSet<>();
    }

    @Test
    void testSelectWithPopularityDistribution_CorrectMix() {
        // Arrange
        String emotion = "sad";
        Integer count = 10;
        
        // Create mock posts for different popularity tiers
        List<Post> hotPosts = createMockPosts(4, 150, "sad"); // Hot posts (view >= 100)
        List<Post> normalPosts = createMockPosts(3, 75, "sad"); // Normal posts (50 <= view < 100)
        List<Post> coldPosts = createMockPosts(3, 25, "sad"); // Cold posts (view < 50)
        
        // Mock repository responses
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewGreaterThanEqual(
            eq("sad"), eq(testCutoffTime), eq(100), any(Pageable.class)))
            .thenReturn(new PageImpl<>(hotPosts));
            
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewBetween(
            eq("sad"), eq(testCutoffTime), eq(50), eq(99), any(Pageable.class)))
            .thenReturn(new PageImpl<>(normalPosts));
            
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewLessThan(
            eq("sad"), eq(testCutoffTime), eq(50), any(Pageable.class)))
            .thenReturn(new PageImpl<>(coldPosts));

        // Act
        List<Post> result = postSelectionService.selectWithPopularityDistribution(
            emotion, count, testCutoffTime, testExcludeIds);

        // Assert
        assertNotNull(result);
        assertEquals(10, result.size());
        
        // Verify distribution (40% hot, 30% normal, 30% cold)
        long hotCount = result.stream().filter(p -> p.getView() >= 100).count();
        long normalCount = result.stream().filter(p -> p.getView() >= 50 && p.getView() < 100).count();
        long coldCount = result.stream().filter(p -> p.getView() < 50).count();
        
        assertEquals(4, hotCount, "Should have 4 hot posts (40%)");
        assertEquals(3, normalCount, "Should have 3 normal posts (30%)");
        assertEquals(3, coldCount, "Should have 3 cold posts (30%)");
        
        // Verify repository methods were called
        verify(postRepository).findByEmotionIgnoreCaseAndCreatedAtAfterAndViewGreaterThanEqual(
            eq("sad"), eq(testCutoffTime), eq(100), any(Pageable.class));
        verify(postRepository).findByEmotionIgnoreCaseAndCreatedAtAfterAndViewBetween(
            eq("sad"), eq(testCutoffTime), eq(50), eq(99), any(Pageable.class));
        verify(postRepository).findByEmotionIgnoreCaseAndCreatedAtAfterAndViewLessThan(
            eq("sad"), eq(testCutoffTime), eq(50), any(Pageable.class));
    }

    @Test
    void testSelectWithPopularityDistribution_InsufficientHotPosts() {
        // Arrange
        String emotion = "happy";
        Integer count = 10;
        
        // Create insufficient hot posts (only 1 instead of 4 needed)
        List<Post> hotPosts = createMockPosts(1, 150, "happy");
        List<Post> normalPosts = createMockPosts(5, 75, "happy");
        List<Post> coldPosts = createMockPosts(5, 25, "happy");
        
        // Mock repository responses
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewGreaterThanEqual(
            eq("happy"), eq(testCutoffTime), eq(100), any(Pageable.class)))
            .thenReturn(new PageImpl<>(hotPosts));
            
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewBetween(
            eq("happy"), eq(testCutoffTime), eq(50), eq(99), any(Pageable.class)))
            .thenReturn(new PageImpl<>(normalPosts));
            
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewLessThan(
            eq("happy"), eq(testCutoffTime), eq(50), any(Pageable.class)))
            .thenReturn(new PageImpl<>(coldPosts));
        
        // Note: Compensation query is not needed in this test because we have enough posts
        // from normal and cold tiers to meet the target count

        // Act
        List<Post> result = postSelectionService.selectWithPopularityDistribution(
            emotion, count, testCutoffTime, testExcludeIds);

        // Assert
        assertNotNull(result);
        assertEquals(10, result.size(), "Should compensate for insufficient hot posts");
        
        // Should have 1 hot post (what's available) + compensation from other tiers
        long hotCount = result.stream().filter(p -> p.getView() >= 100).count();
        assertEquals(1, hotCount, "Should have only 1 hot post (insufficient available)");
        
        // With 1 hot + 5 normal + 5 cold = 11 total, but we limit to 10
        // So we should have 1 hot + 3 normal + 3 cold = 7, plus 3 more from compensation
        // But since we have enough from normal/cold tiers, compensation might not be needed
        
        // Verify the result is exactly the requested count
        assertEquals(10, result.size(), "Should return exactly the requested count");
    }

    @Test
    void testSelectWithPopularityDistribution_EmptyResult() {
        // Arrange
        String emotion = "nonexistent";
        Integer count = 5;
        
        // Mock empty responses from all repository methods
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewGreaterThanEqual(
            eq("nonexistent"), eq(testCutoffTime), eq(100), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
            
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewBetween(
            eq("nonexistent"), eq(testCutoffTime), eq(50), eq(99), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
            
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewLessThan(
            eq("nonexistent"), eq(testCutoffTime), eq(50), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        
        // Mock empty compensation query
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfter(
            eq("nonexistent"), eq(testCutoffTime), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        List<Post> result = postSelectionService.selectWithPopularityDistribution(
            emotion, count, testCutoffTime, testExcludeIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty list for nonexistent emotion");
    }

    @Test
    void testSelectWithPopularityDistribution_ExcludesPostIds() {
        // Arrange
        String emotion = "sad";
        Integer count = 5;
        
        // Create posts with some to exclude
        List<Post> allPosts = createMockPosts(10, 80, "sad");
        UUID excludeId = allPosts.get(0).getId();
        testExcludeIds.add(excludeId);
        
        // Mock repository to return all posts
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewGreaterThanEqual(
            anyString(), any(LocalDateTime.class), anyInt(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(allPosts));
            
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewBetween(
            anyString(), any(LocalDateTime.class), anyInt(), anyInt(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(allPosts));
            
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewLessThan(
            anyString(), any(LocalDateTime.class), anyInt(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(allPosts));

        // Act
        List<Post> result = postSelectionService.selectWithPopularityDistribution(
            emotion, count, testCutoffTime, testExcludeIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.stream().noneMatch(p -> p.getId().equals(excludeId)), 
                  "Excluded post should not be in results");
    }

    @Test
    void testSelectWithPopularityDistribution_NullParameters() {
        // Test null emotion
        assertThrows(IllegalArgumentException.class, () -> {
            postSelectionService.selectWithPopularityDistribution(null, 5, testCutoffTime, testExcludeIds);
        });
        
        // Test empty emotion
        assertThrows(IllegalArgumentException.class, () -> {
            postSelectionService.selectWithPopularityDistribution("", 5, testCutoffTime, testExcludeIds);
        });
        
        // Test whitespace-only emotion
        assertThrows(IllegalArgumentException.class, () -> {
            postSelectionService.selectWithPopularityDistribution("   ", 5, testCutoffTime, testExcludeIds);
        });
        
        // Test null count
        assertThrows(IllegalArgumentException.class, () -> {
            postSelectionService.selectWithPopularityDistribution("sad", null, testCutoffTime, testExcludeIds);
        });
        
        // Test zero count
        assertThrows(IllegalArgumentException.class, () -> {
            postSelectionService.selectWithPopularityDistribution("sad", 0, testCutoffTime, testExcludeIds);
        });
        
        // Test negative count
        assertThrows(IllegalArgumentException.class, () -> {
            postSelectionService.selectWithPopularityDistribution("sad", -5, testCutoffTime, testExcludeIds);
        });
        
        // Test null cutoff time
        assertThrows(IllegalArgumentException.class, () -> {
            postSelectionService.selectWithPopularityDistribution("sad", 5, null, testExcludeIds);
        });
    }

    @Test
    void testSelectWithPopularityDistribution_EmotionCaseInsensitive() {
        // Arrange
        String emotion = "HAPPY"; // Uppercase
        Integer count = 5;
        
        List<Post> mockPosts = createMockPosts(5, 80, "happy");
        
        // Mock repository responses
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewGreaterThanEqual(
            eq("happy"), any(LocalDateTime.class), anyInt(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(mockPosts));
            
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewBetween(
            eq("happy"), any(LocalDateTime.class), anyInt(), anyInt(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(mockPosts));
            
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewLessThan(
            eq("happy"), any(LocalDateTime.class), anyInt(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(mockPosts));

        // Act
        List<Post> result = postSelectionService.selectWithPopularityDistribution(
            emotion, count, testCutoffTime, testExcludeIds);

        // Assert
        assertNotNull(result);
        // Verify that the emotion was normalized to lowercase in the query
        verify(postRepository).findByEmotionIgnoreCaseAndCreatedAtAfterAndViewGreaterThanEqual(
            eq("happy"), any(LocalDateTime.class), anyInt(), any(Pageable.class));
    }

    @Test
    void testSelectWithPopularityDistribution_HandlesFlaggedPosts() {
        // Arrange
        String emotion = "sad";
        Integer count = 5;
        
        // Create posts including flagged ones
        List<Post> postsWithFlagged = createMockPosts(10, 80, "sad");
        postsWithFlagged.get(0).setFlagged(true); // Make first post flagged
        
        // Mock repository responses
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewGreaterThanEqual(
            anyString(), any(LocalDateTime.class), anyInt(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(postsWithFlagged));
            
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewBetween(
            anyString(), any(LocalDateTime.class), anyInt(), anyInt(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(postsWithFlagged));
            
        when(postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewLessThan(
            anyString(), any(LocalDateTime.class), anyInt(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(postsWithFlagged));

        // Act
        List<Post> result = postSelectionService.selectWithPopularityDistribution(
            emotion, count, testCutoffTime, testExcludeIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.stream().noneMatch(Post::isFlagged), 
                  "No flagged posts should be included");
    }

    /**
     * Helper method to create mock posts for testing
     */
    private List<Post> createMockPosts(int count, int viewCount, String emotion) {
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Post post = new Post();
            post.setId(UUID.randomUUID());
            post.setTitle("Test Post " + i);
            post.setContent("Test content for post " + i);
            post.setEmotion(emotion);
            post.setView(viewCount);
            post.setResonance(10);
            post.setCollect(5);
            post.setFlagged(false);
            post.setCreatedAt(LocalDateTime.now().minusDays(i));
            posts.add(post);
        }
        return posts;
    }
}
package ai.youthmind.service;

import ai.youthmind.entity.Post;
import ai.youthmind.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for PostSelectionService
 * Tests real database interactions and repository queries
 */
@DataJpaTest
@Import(PostSelectionService.class)
@ActiveProfiles("test")
@Transactional
public class PostSelectionServiceIntegrationTest {

    @Autowired
    private PostSelectionService postSelectionService;

    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private TestEntityManager entityManager;

    private LocalDateTime now;
    private LocalDateTime sixHoursAgo;
    private LocalDateTime twelveHoursAgo;
    private LocalDateTime eighteenHoursAgo;
    private LocalDateTime oneDayAgo;
    private LocalDateTime twoDaysAgo;
    private LocalDateTime fiveDaysAgo;

    @BeforeEach
    void setUp() {
        // Set up time references
        now = LocalDateTime.now();
        sixHoursAgo = now.minusHours(6);
        twelveHoursAgo = now.minusHours(12);
        eighteenHoursAgo = now.minusHours(18);
        oneDayAgo = now.minusDays(1);
        twoDaysAgo = now.minusDays(2);
        fiveDaysAgo = now.minusDays(5);
        
        // Seed test data
        seedTestData();
    }

    private void seedTestData() {
        // Create test posts for "sad" emotion
        createSadPosts();
        
        // Create test posts for "happy" emotion
        createHappyPosts();
        
        // Create some flagged posts that should be excluded
        createFlaggedPosts();
        
        // Create some old posts that should be excluded by cutoff time
        createOldPosts();
    }

    private void createSadPosts() {
        // 5 hot posts (viewCount >= 100) created 6 hours ago
        createPost("sad", 150, sixHoursAgo, false);
        createPost("sad", 200, sixHoursAgo, false);
        createPost("sad", 120, sixHoursAgo, false);
        createPost("sad", 180, sixHoursAgo, false);
        createPost("sad", 110, sixHoursAgo, false);
        
        // 5 normal posts (50 <= viewCount < 100) created 12 hours ago
        createPost("sad", 75, twelveHoursAgo, false);
        createPost("sad", 60, twelveHoursAgo, false);
        createPost("sad", 80, twelveHoursAgo, false);
        createPost("sad", 90, twelveHoursAgo, false);
        createPost("sad", 55, twelveHoursAgo, false);
        
        // 5 cold posts (viewCount < 50) created 18 hours ago
        createPost("sad", 30, eighteenHoursAgo, false);
        createPost("sad", 25, eighteenHoursAgo, false);
        createPost("sad", 40, eighteenHoursAgo, false);
        createPost("sad", 35, eighteenHoursAgo, false);
        createPost("sad", 20, eighteenHoursAgo, false);
    }

    private void createHappyPosts() {
        // 3 hot posts created 1 day ago
        createPost("happy", 180, oneDayAgo, false);
        createPost("happy", 160, oneDayAgo, false);
        createPost("happy", 140, oneDayAgo, false);
        
        // 3 normal posts created 2 days ago
        createPost("happy", 80, twoDaysAgo, false);
        createPost("happy", 70, twoDaysAgo, false);
        createPost("happy", 60, twoDaysAgo, false);
        
        // 4 cold posts created 5 days ago
        createPost("happy", 40, fiveDaysAgo, false);
        createPost("happy", 35, fiveDaysAgo, false);
        createPost("happy", 30, fiveDaysAgo, false);
        createPost("happy", 25, fiveDaysAgo, false);
    }

    private void createFlaggedPosts() {
        // Create flagged posts that should be excluded
        createPost("sad", 100, sixHoursAgo, true);  // Hot but flagged
        createPost("happy", 50, oneDayAgo, true);   // Normal but flagged
        createPost("sad", 20, eighteenHoursAgo, true); // Cold but flagged
    }

    private void createOldPosts() {
        // Create posts older than our typical cutoff (should be excluded)
        LocalDateTime oldTime = now.minusDays(10);
        createPost("sad", 200, oldTime, false);     // Hot but too old
        createPost("happy", 100, oldTime, false);   // Hot but too old
        createPost("sad", 30, oldTime, false);      // Cold but too old
    }

    private Post createPost(String emotion, int viewCount, LocalDateTime createdAt, boolean flagged) {
        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setUserId(UUID.randomUUID());
        post.setTitle("Test Post - " + emotion + " - " + viewCount + " views");
        post.setContent("Test content for " + emotion + " post with " + viewCount + " views");
        post.setEmotion(emotion);
        post.setView(viewCount);
        post.setResonance(10);
        post.setCollect(5);
        post.setFlagged(flagged);
        post.setCreatedAt(createdAt);
        post.setUpdatedAt(createdAt);
        
        return entityManager.persistAndFlush(post);
    }

    @Test
    void testSelectWithPopularityDistribution_RealDatabase() {
        // Arrange
        String emotion = "sad";
        Integer count = 15; // Should get: 6 hot (40%), 4 normal (30%), 5 cold (30%)
        LocalDateTime cutoffTime = now.minusDays(1); // Include posts from last 24 hours
        Set<UUID> excludePostIds = Set.of();

        // Act
        List<Post> result = postSelectionService.selectWithPopularityDistribution(
            emotion, count, cutoffTime, excludePostIds);

        // Assert
        assertNotNull(result);
        assertEquals(15, result.size(), "Should return exactly 15 posts");
        
        // Verify all posts have the correct emotion
        assertTrue(result.stream().allMatch(p -> "sad".equals(p.getEmotion())), 
                  "All posts should have 'sad' emotion");
        
        // Verify no flagged posts
        assertTrue(result.stream().noneMatch(Post::isFlagged), 
                  "No flagged posts should be included");
        
        // Verify popularity distribution
        long hotCount = result.stream().filter(p -> p.getView() >= 100).count();
        long normalCount = result.stream().filter(p -> p.getView() >= 50 && p.getView() < 100).count();
        long coldCount = result.stream().filter(p -> p.getView() < 50).count();
        
        assertEquals(6, hotCount, "Should have 6 hot posts (40%)");
        assertEquals(4, normalCount, "Should have 4 normal posts (30%)");
        assertEquals(5, coldCount, "Should have 5 cold posts (30%)");
        
        // Verify all posts are after cutoff time
        assertTrue(result.stream().allMatch(p -> p.getCreatedAt().isAfter(cutoffTime)), 
                  "All posts should be after cutoff time");
    }

    @Test
    void testPopularityStratification_DatabaseIntegrity() {
        // This test verifies that our custom repository methods actually work
        
        // Test hot posts query
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Post> hotPosts = postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewGreaterThanEqual(
            "sad", now.minusDays(1), 100, pageable);
        
        assertEquals(5, hotPosts.getContent().size(), "Should find 5 hot sad posts");
        assertTrue(hotPosts.getContent().stream().allMatch(p -> p.getView() >= 100), 
                  "All hot posts should have view count >= 100");
        assertTrue(hotPosts.getContent().stream().noneMatch(Post::isFlagged), 
                  "No flagged posts should be in hot results");
        
        // Test normal posts query
        Page<Post> normalPosts = postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewBetween(
            "sad", now.minusDays(1), 50, 99, pageable);
        
        assertEquals(5, normalPosts.getContent().size(), "Should find 5 normal sad posts");
        assertTrue(normalPosts.getContent().stream().allMatch(p -> p.getView() >= 50 && p.getView() < 100), 
                  "All normal posts should have view count between 50-99");
        assertTrue(normalPosts.getContent().stream().noneMatch(Post::isFlagged), 
                  "No flagged posts should be in normal results");
        
        // Test cold posts query
        Page<Post> coldPosts = postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewLessThan(
            "sad", now.minusDays(1), 50, pageable);
        
        assertEquals(5, coldPosts.getContent().size(), "Should find 5 cold sad posts");
        assertTrue(coldPosts.getContent().stream().allMatch(p -> p.getView() < 50), 
                  "All cold posts should have view count < 50");
        assertTrue(coldPosts.getContent().stream().noneMatch(Post::isFlagged), 
                  "No flagged posts should be in cold results");
    }

    @Test
    void testTimeFiltering_ActualTimestamps() {
        // Arrange
        String emotion = "sad";
        Integer count = 20;
        LocalDateTime cutoffTime = now.minusHours(10); // Only include posts from last 10 hours
        Set<UUID> excludePostIds = Set.of();

        // Act
        List<Post> result = postSelectionService.selectWithPopularityDistribution(
            emotion, count, cutoffTime, excludePostIds);

        // Assert
        assertNotNull(result);
        
        // Should only get posts created in the last 10 hours (6 hours ago)
        // This excludes posts from 12 and 18 hours ago
        assertTrue(result.stream().allMatch(p -> p.getCreatedAt().isAfter(cutoffTime)), 
                  "All posts should be after cutoff time");
        
        // Should only get the 5 hot posts from 6 hours ago
        assertEquals(5, result.size(), "Should only return posts from last 10 hours");
        assertTrue(result.stream().allMatch(p -> p.getView() >= 100), 
                  "Should only get hot posts (the ones from 6 hours ago)");
    }

    @Test
    void testExclusionLogic_DatabaseLevel() {
        // Arrange
        String emotion = "sad";
        Integer count = 15;
        LocalDateTime cutoffTime = now.minusDays(1);
        
        // Get one of the hot posts to exclude
        List<Post> allSadPosts = postRepository.findByEmotion("sad");
        Post postToExclude = allSadPosts.stream()
            .filter(p -> p.getView() >= 100 && !p.isFlagged())
            .findFirst()
            .orElseThrow();
        
        Set<UUID> excludePostIds = Set.of(postToExclude.getId());

        // Act
        List<Post> result = postSelectionService.selectWithPopularityDistribution(
            emotion, count, cutoffTime, excludePostIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.stream().noneMatch(p -> p.getId().equals(postToExclude.getId())), 
                  "Excluded post should not be in results");
        
        // Should get 14 posts instead of 15 (one excluded)
        assertEquals(14, result.size(), "Should return 14 posts (one excluded)");
        
        // Verify the excluded post is not in any of the results
        List<UUID> resultIds = result.stream().map(Post::getId).collect(Collectors.toList());
        assertFalse(resultIds.contains(postToExclude.getId()), 
                   "Excluded post ID should not be in result IDs");
    }

    @Test
    void testSelectWithPopularityDistribution_HandlesInsufficientPosts() {
        // Arrange
        String emotion = "happy";
        Integer count = 20; // Request more than available posts (only 10 available)
        LocalDateTime cutoffTime = now.minusDays(1);
        Set<UUID> excludePostIds = Set.of();

        // Act
        List<Post> result = postSelectionService.selectWithPopularityDistribution(
            emotion, count, cutoffTime, excludePostIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.size() <= 10, "Should not return more posts than available");
        assertTrue(result.size() > 0, "Should return some posts if available");
        
        // Verify all posts have correct emotion and are not flagged
        assertTrue(result.stream().allMatch(p -> "happy".equals(p.getEmotion())), 
                  "All posts should have 'happy' emotion");
        assertTrue(result.stream().noneMatch(Post::isFlagged), 
                  "No flagged posts should be included");
        
        // Verify compensation worked - should have mix of all available tiers
        long hotCount = result.stream().filter(p -> p.getView() >= 100).count();
        long normalCount = result.stream().filter(p -> p.getView() >= 50 && p.getView() < 100).count();
        long coldCount = result.stream().filter(p -> p.getView() < 50).count();
        
        assertTrue(hotCount > 0, "Should have some hot posts");
        assertTrue(normalCount > 0, "Should have some normal posts");
        assertTrue(coldCount > 0, "Should have some cold posts");
    }

    @Test
    void testSelectWithPopularityDistribution_EmptyResult() {
        // Arrange
        String emotion = "nonexistent";
        Integer count = 5;
        LocalDateTime cutoffTime = now.minusDays(1);
        Set<UUID> excludePostIds = Set.of();

        // Act
        List<Post> result = postSelectionService.selectWithPopularityDistribution(
            emotion, count, cutoffTime, excludePostIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty list for nonexistent emotion");
    }

    @Test
    void testSelectWithPopularityDistribution_EmotionCaseInsensitive() {
        // Arrange
        String emotion = "SAD"; // Uppercase
        Integer count = 10;
        LocalDateTime cutoffTime = now.minusDays(1);
        Set<UUID> excludePostIds = Set.of();

        // Act
        List<Post> result = postSelectionService.selectWithPopularityDistribution(
            emotion, count, cutoffTime, excludePostIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.size() > 0, "Should return posts for uppercase emotion");
        assertTrue(result.stream().allMatch(p -> "sad".equals(p.getEmotion())), 
                  "All posts should have 'sad' emotion (lowercase)");
    }

    @Test
    void testRepositoryQueries_VerifySQLGeneration() {
        // This test verifies that our custom repository methods generate correct SQL
        
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Test that queries execute without SQL errors
        assertDoesNotThrow(() -> {
            Page<Post> hotPosts = postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewGreaterThanEqual(
                "sad", now.minusDays(1), 100, pageable);
            assertNotNull(hotPosts);
        }, "Hot posts query should execute without errors");
        
        assertDoesNotThrow(() -> {
            Page<Post> normalPosts = postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewBetween(
                "sad", now.minusDays(1), 50, 99, pageable);
            assertNotNull(normalPosts);
        }, "Normal posts query should execute without errors");
        
        assertDoesNotThrow(() -> {
            Page<Post> coldPosts = postRepository.findByEmotionIgnoreCaseAndCreatedAtAfterAndViewLessThan(
                "sad", now.minusDays(1), 50, pageable);
            assertNotNull(coldPosts);
        }, "Cold posts query should execute without errors");
        
        assertDoesNotThrow(() -> {
            Page<Post> allPosts = postRepository.findByEmotionIgnoreCaseAndCreatedAtAfter(
                "sad", now.minusDays(1), pageable);
            assertNotNull(allPosts);
        }, "General posts query should execute without errors");
    }

    @Test
    void testSelectWithPopularityDistribution_InvalidInput() {
        // Test null emotion
        assertThrows(IllegalArgumentException.class, () -> {
            postSelectionService.selectWithPopularityDistribution(null, 5, now.minusDays(1), Set.of());
        });
        
        // Test empty emotion
        assertThrows(IllegalArgumentException.class, () -> {
            postSelectionService.selectWithPopularityDistribution("", 5, now.minusDays(1), Set.of());
        });
        
        // Test null count
        assertThrows(IllegalArgumentException.class, () -> {
            postSelectionService.selectWithPopularityDistribution("sad", null, now.minusDays(1), Set.of());
        });
        
        // Test zero count
        assertThrows(IllegalArgumentException.class, () -> {
            postSelectionService.selectWithPopularityDistribution("sad", 0, now.minusDays(1), Set.of());
        });
        
        // Test null cutoff time
        assertThrows(IllegalArgumentException.class, () -> {
            postSelectionService.selectWithPopularityDistribution("sad", 5, null, Set.of());
        });
    }
}
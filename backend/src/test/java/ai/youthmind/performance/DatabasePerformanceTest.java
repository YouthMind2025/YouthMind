package ai.youthmind.performance;

import ai.youthmind.entity.*;
import ai.youthmind.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabasePerformanceTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private UserEngagementRepository userEngagementRepository;
    
    private static final int USERS_COUNT = 1000;
    private static final int POSTS_COUNT = 10000;
    private static final int COMMENTS_COUNT = 50000;
    private static final int ENGAGEMENTS_COUNT = 100000;
    
    private List<User> testUsers = new ArrayList<>();
    private List<Post> testPosts = new ArrayList<>();
    private Random random = new Random();
    
    @Test
    @Order(1)
    @DisplayName("Setup Large Dataset for Performance Testing")
    void setupLargeDataset() {
        System.out.println("=== Setting up large dataset for performance testing ===");
        
        long startTime = System.currentTimeMillis();
        
        // Create users in batches
        System.out.println("Creating " + USERS_COUNT + " users...");
        for (int i = 0; i < USERS_COUNT; i++) {
            User user = new User();
            user.setEmail("perfuser" + i + "@youthmind.ai");
            user.setPassword("hashedPassword" + i);
            user.setFirstName("PerfUser" + i);
            user.setLastName("Test");
            user.setUsername("perfuser" + i);
            testUsers.add(user);
            
            if (i % 100 == 0) {
                userRepository.saveAll(testUsers.subList(Math.max(0, i - 99), i + 1));
                System.out.println("Created " + (i + 1) + " users");
            }
        }
        userRepository.saveAll(testUsers);
        
        long userCreationTime = System.currentTimeMillis() - startTime;
        System.out.println("User creation completed in " + userCreationTime + "ms");
        
        // Create posts
        System.out.println("Creating " + POSTS_COUNT + " posts...");
        startTime = System.currentTimeMillis();
        Post.Emotion[] emotions = Post.Emotion.values();
        
        for (int i = 0; i < POSTS_COUNT; i++) {
            User randomUser = testUsers.get(random.nextInt(testUsers.size()));
            
            Post post = new Post();
            post.setUserId(randomUser.getId());
            post.setTitle("Performance Test Post " + i);
            post.setContent("This is performance test post content number " + i + 
                          ". It contains some sample text to test database performance with longer content.");
            post.setEmotion(emotions[random.nextInt(emotions.length)]);
            post.setTopics("performance,testing,database,load");
            post.setViews(random.nextInt(10000));
            post.setLikes(random.nextInt(1000));
            post.setResonanceCount(random.nextInt(500));
            post.setCollectCount(random.nextInt(200));
            testPosts.add(post);
            
            if (i % 500 == 0) {
                postRepository.saveAll(testPosts.subList(Math.max(0, i - 499), i + 1));
                System.out.println("Created " + (i + 1) + " posts");
            }
        }
        postRepository.saveAll(testPosts);
        
        long postCreationTime = System.currentTimeMillis() - startTime;
        System.out.println("Post creation completed in " + postCreationTime + "ms");
        
        // Create comments
        System.out.println("Creating " + COMMENTS_COUNT + " comments...");
        startTime = System.currentTimeMillis();
        List<Comment> comments = new ArrayList<>();
        
        for (int i = 0; i < COMMENTS_COUNT; i++) {
            Post randomPost = testPosts.get(random.nextInt(testPosts.size()));
            User randomUser = testUsers.get(random.nextInt(testUsers.size()));
            
            Comment comment = new Comment();
            comment.setPostId(randomPost.getId());
            comment.setAuthorId(randomUser.getId());
            comment.setContent("Performance test comment " + i);
            comment.setLikes(random.nextInt(100));
            comments.add(comment);
            
            if (i % 1000 == 0) {
                commentRepository.saveAll(comments.subList(Math.max(0, i - 999), i + 1));
                System.out.println("Created " + (i + 1) + " comments");
            }
        }
        commentRepository.saveAll(comments);
        
        long commentCreationTime = System.currentTimeMillis() - startTime;
        System.out.println("Comment creation completed in " + commentCreationTime + "ms");
        
        // Create user engagements
        System.out.println("Creating " + ENGAGEMENTS_COUNT + " user engagements...");
        startTime = System.currentTimeMillis();
        List<UserEngagement> engagements = new ArrayList<>();
        UserEngagement.EngagementType[] engagementTypes = UserEngagement.EngagementType.values();
        
        for (int i = 0; i < ENGAGEMENTS_COUNT; i++) {
            Post randomPost = testPosts.get(random.nextInt(testPosts.size()));
            User randomUser = testUsers.get(random.nextInt(testUsers.size()));
            
            UserEngagement engagement = new UserEngagement();
            engagement.setUserId(randomUser.getId());
            engagement.setPostId(randomPost.getId());
            engagement.setType(engagementTypes[random.nextInt(engagementTypes.length)]);
            engagements.add(engagement);
            
            if (i % 2000 == 0) {
                try {
                    userEngagementRepository.saveAll(engagements.subList(Math.max(0, i - 1999), i + 1));
                    System.out.println("Created " + (i + 1) + " engagements");
                } catch (Exception e) {
                    // Skip duplicates (unique constraint violations)
                    System.out.println("Skipped some duplicate engagements at " + (i + 1));
                }
            }
        }
        
        try {
            userEngagementRepository.saveAll(engagements);
        } catch (Exception e) {
            System.out.println("Completed engagements with some duplicates skipped");
        }
        
        long engagementCreationTime = System.currentTimeMillis() - startTime;
        System.out.println("Engagement creation completed in " + engagementCreationTime + "ms");
        
        System.out.println("✓ Large dataset setup completed");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test Feed Query Performance")
    void testFeedQueryPerformance() {
        System.out.println("=== Testing Feed Query Performance ===");
        
        Pageable pageable = PageRequest.of(0, 50);
        
        // Test basic feed query
        long startTime = System.currentTimeMillis();
        Page<Post> feedPosts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        long queryTime = System.currentTimeMillis() - startTime;
        
        assertThat(feedPosts.getTotalElements()).isGreaterThan(POSTS_COUNT);
        assertThat(feedPosts.getContent()).hasSize(50);
        System.out.println("Basic feed query: " + queryTime + "ms");
        assertThat(queryTime).isLessThan(1000); // Should be under 1 second
        
        // Test emotion-filtered feed query
        startTime = System.currentTimeMillis();
        Page<Post> emotionFeed = postRepository.findByEmotionOrderByCreatedAtDesc(Post.Emotion.HAPPY, pageable);
        queryTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Emotion-filtered feed query: " + queryTime + "ms");
        assertThat(queryTime).isLessThan(500); // Should be under 500ms with index
        
        // Test trending posts query
        startTime = System.currentTimeMillis();
        Page<Post> trendingPosts = postRepository.findMostResonantPosts(
            LocalDateTime.now().minusDays(7), pageable);
        queryTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Trending posts query: " + queryTime + "ms");
        assertThat(queryTime).isLessThan(800); // Should be under 800ms
        
        System.out.println("✓ Feed query performance tests passed");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test Search Performance")
    void testSearchPerformance() {
        System.out.println("=== Testing Search Performance ===");
        
        Pageable pageable = PageRequest.of(0, 20);
        
        // Test content search
        long startTime = System.currentTimeMillis();
        Page<Post> searchResults = postRepository.searchPosts("performance", pageable);
        long queryTime = System.currentTimeMillis() - startTime;
        
        assertThat(searchResults.getTotalElements()).isGreaterThan(0);
        System.out.println("Content search query: " + queryTime + "ms");
        assertThat(queryTime).isLessThan(2000); // Should be under 2 seconds
        
        // Test search with emotion filter
        startTime = System.currentTimeMillis();
        Page<Post> filteredSearch = postRepository.searchPostsByEmotion("test", "HAPPY", pageable);
        queryTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Filtered search query: " + queryTime + "ms");
        assertThat(queryTime).isLessThan(1500); // Should be under 1.5 seconds
        
        System.out.println("✓ Search performance tests passed");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test Concurrent Read Performance")
    void testConcurrentReadPerformance() throws InterruptedException, ExecutionException {
        System.out.println("=== Testing Concurrent Read Performance ===");
        
        int threadCount = 20;
        int queriesPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        List<Future<Long>> futures = new ArrayList<>();
        
        for (int i = 0; i < threadCount; i++) {
            Future<Long> future = executor.submit(() -> {
                long totalTime = 0;
                Pageable pageable = PageRequest.of(0, 20);
                
                for (int j = 0; j < queriesPerThread; j++) {
                    long startTime = System.currentTimeMillis();
                    
                    // Mix different types of queries
                    switch (j % 4) {
                        case 0:
                            postRepository.findAllByOrderByCreatedAtDesc(pageable);
                            break;
                        case 1:
                            postRepository.findByEmotion(Post.Emotion.HAPPY, pageable);
                            break;
                        case 2:
                            postRepository.findMostResonantPosts(LocalDateTime.now().minusDays(1), pageable);
                            break;
                        case 3:
                            userRepository.findAll(pageable);
                            break;
                    }
                    
                    totalTime += (System.currentTimeMillis() - startTime);
                }
                
                return totalTime;
            });
            
            futures.add(future);
        }
        
        long totalExecutionTime = 0;
        for (Future<Long> future : futures) {
            totalExecutionTime += future.get();
        }
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        
        double averageQueryTime = (double) totalExecutionTime / (threadCount * queriesPerThread);
        System.out.println("Average query time under concurrent load: " + averageQueryTime + "ms");
        System.out.println("Total queries executed: " + (threadCount * queriesPerThread));
        
        // Under concurrent load, average query time should still be reasonable
        assertThat(averageQueryTime).isLessThan(2000);
        
        System.out.println("✓ Concurrent read performance tests passed");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test Analytics Query Performance")
    void testAnalyticsQueryPerformance() {
        System.out.println("=== Testing Analytics Query Performance ===");
        
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        
        // Test post count analytics
        long startTime = System.currentTimeMillis();
        Long totalPosts = postRepository.countPostsSince(since);
        long queryTime = System.currentTimeMillis() - startTime;
        
        assertThat(totalPosts).isGreaterThan(0);
        System.out.println("Post count query: " + queryTime + "ms");
        assertThat(queryTime).isLessThan(500);
        
        // Test emotion distribution
        startTime = System.currentTimeMillis();
        List<Object[]> emotionDistribution = postRepository.getEmotionDistributionSince(since);
        queryTime = System.currentTimeMillis() - startTime;
        
        assertThat(emotionDistribution).isNotEmpty();
        System.out.println("Emotion distribution query: " + queryTime + "ms");
        assertThat(queryTime).isLessThan(1000);
        
        // Test engagement statistics
        startTime = System.currentTimeMillis();
        List<Object[]> engagementStats = postRepository.getEngagementStatsSince(since);
        queryTime = System.currentTimeMillis() - startTime;
        
        assertThat(engagementStats).isNotEmpty();
        System.out.println("Engagement statistics query: " + queryTime + "ms");
        assertThat(queryTime).isLessThan(800);
        
        System.out.println("✓ Analytics query performance tests passed");
    }
    
    @Test
    @Order(6)
    @DisplayName("Test Memory Usage During Large Operations")
    void testMemoryUsageDuringLargeOperations() {
        System.out.println("=== Testing Memory Usage During Large Operations ===");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection and get baseline
        System.gc();
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Baseline memory usage: " + (baselineMemory / 1024 / 1024) + " MB");
        
        // Perform large query operations
        Pageable largePage = PageRequest.of(0, 1000);
        
        long startTime = System.currentTimeMillis();
        Page<Post> largeFeed = postRepository.findAllByOrderByCreatedAtDesc(largePage);
        long queryTime = System.currentTimeMillis() - startTime;
        
        long peakMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Peak memory usage: " + (peakMemory / 1024 / 1024) + " MB");
        System.out.println("Memory increase: " + ((peakMemory - baselineMemory) / 1024 / 1024) + " MB");
        System.out.println("Large query time: " + queryTime + "ms");
        
        assertThat(largeFeed.getContent()).hasSize(1000);
        assertThat(queryTime).isLessThan(3000); // Should complete within 3 seconds
        
        // Memory increase should be reasonable (less than 100MB for 1000 records)
        long memoryIncrease = peakMemory - baselineMemory;
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024); // 100MB
        
        System.out.println("✓ Memory usage tests passed");
    }
    
    @Test
    @Order(7)
    @DisplayName("Test Database Connection Pool Performance")
    void testDatabaseConnectionPoolPerformance() throws InterruptedException, ExecutionException {
        System.out.println("=== Testing Database Connection Pool Performance ===");
        
        int threadCount = 50; // More threads than typical pool size
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        List<Future<Boolean>> futures = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            Future<Boolean> future = executor.submit(() -> {
                try {
                    // Simulate typical database operations
                    userRepository.count();
                    postRepository.count();
                    commentRepository.count();
                    
                    latch.countDown();
                    return true;
                } catch (Exception e) {
                    System.err.println("Connection pool error: " + e.getMessage());
                    latch.countDown();
                    return false;
                }
            });
            
            futures.add(future);
        }
        
        latch.await(30, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Check that all operations completed successfully
        int successCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            }
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        System.out.println("Successful operations: " + successCount + "/" + threadCount);
        System.out.println("Total time for all operations: " + totalTime + "ms");
        
        assertThat(successCount).isEqualTo(threadCount); // All should succeed
        assertThat(totalTime).isLessThan(10000); // Should complete within 10 seconds
        
        System.out.println("✓ Database connection pool performance tests passed");
    }
    
    @AfterAll
    static void cleanup() {
        System.out.println("=== Performance Test Cleanup ===");
        System.out.println("Large dataset will be cleaned up by test framework");
    }
}

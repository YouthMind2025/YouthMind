package ai.youthmind.stress;

import ai.youthmind.entity.*;
import ai.youthmind.repository.*;
import ai.youthmind.service.EnhancedPostService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseStressTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserEngagementRepository userEngagementRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private EnhancedPostService enhancedPostService;
    
    private static final int STRESS_USERS = 500;
    private static final int STRESS_POSTS = 2000;
    private static final int CONCURRENT_THREADS = 50;
    private static final int OPERATIONS_PER_THREAD = 100;
    
    private List<User> stressUsers = new ArrayList<>();
    private List<Post> stressPosts = new ArrayList<>();
    private Random random = new Random();
    
    @Test
    @Order(1)
    @DisplayName("Setup Stress Test Data")
    void setupStressTestData() {
        System.out.println("=== Setting up stress test data ===");
        
        long startTime = System.currentTimeMillis();
        
        // Create stress test users
        for (int i = 0; i < STRESS_USERS; i++) {
            User user = new User();
            user.setEmail("stress" + i + "@test.com");
            user.setPassword("password" + i);
            user.setFirstName("Stress" + i);
            user.setLastName("User");
            user.setUsername("stress" + i);
            stressUsers.add(user);
        }
        stressUsers = userRepository.saveAll(stressUsers);
        
        // Create stress test posts
        Post.Emotion[] emotions = Post.Emotion.values();
        for (int i = 0; i < STRESS_POSTS; i++) {
            User randomUser = stressUsers.get(random.nextInt(stressUsers.size()));
            
            Post post = new Post();
            post.setUserId(randomUser.getId());
            post.setTitle("Stress Test Post " + i);
            post.setContent("Content for stress test post " + i);
            post.setEmotion(emotions[random.nextInt(emotions.length)]);
            post.setViews(random.nextInt(100));
            post.setLikes(random.nextInt(50));
            post.setResonanceCount(random.nextInt(25));
            stressPosts.add(post);
        }
        stressPosts = postRepository.saveAll(stressPosts);
        
        long setupTime = System.currentTimeMillis() - startTime;
        System.out.println("Stress test data setup completed in " + setupTime + "ms");
        System.out.println("Created " + stressUsers.size() + " users and " + stressPosts.size() + " posts");
    }
    
    @Test
    @Order(2)
    @DisplayName("Concurrent Read Operations Stress Test")
    void testConcurrentReadOperations() throws InterruptedException, ExecutionException {
        System.out.println("=== Testing Concurrent Read Operations ===");
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        List<Future<Void>> futures = new ArrayList<>();
        long testStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            Future<Void> future = executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        long operationStart = System.currentTimeMillis();
                        
                        try {
                            // Mix different read operations
                            switch (j % 5) {
                                case 0:
                                    postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20));
                                    break;
                                case 1:
                                    Post.Emotion randomEmotion = Post.Emotion.values()[random.nextInt(Post.Emotion.values().length)];
                                    postRepository.findByEmotion(randomEmotion, PageRequest.of(0, 10));
                                    break;
                                case 2:
                                    User randomUser = stressUsers.get(random.nextInt(stressUsers.size()));
                                    postRepository.findByUserIdOrderByCreatedAtDesc(randomUser.getId(), PageRequest.of(0, 10));
                                    break;
                                case 3:
                                    postRepository.findMostResonantPosts(LocalDateTime.now().minusDays(7), PageRequest.of(0, 15));
                                    break;
                                case 4:
                                    userRepository.findAll(PageRequest.of(0, 25));
                                    break;
                            }
                            
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.println("Operation failed: " + e.getMessage());
                        }
                        
                        totalResponseTime.addAndGet(System.currentTimeMillis() - operationStart);
                    }
                } finally {
                    latch.countDown();
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Wait for all operations to complete
        latch.await(120, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        
        // Calculate metrics
        int totalOperations = CONCURRENT_THREADS * OPERATIONS_PER_THREAD;
        double averageResponseTime = (double) totalResponseTime.get() / successCount.get();
        double operationsPerSecond = (double) successCount.get() / (testDuration / 1000.0);
        double errorRate = (double) errorCount.get() / totalOperations * 100;
        
        System.out.println("=== Concurrent Read Operations Results ===");
        System.out.println("Total operations attempted: " + totalOperations);
        System.out.println("Successful operations: " + successCount.get());
        System.out.println("Failed operations: " + errorCount.get());
        System.out.println("Error rate: " + String.format("%.2f%%", errorRate));
        System.out.println("Average response time: " + String.format("%.2f ms", averageResponseTime));
        System.out.println("Operations per second: " + String.format("%.2f", operationsPerSecond));
        System.out.println("Total test duration: " + testDuration + "ms");
        
        // Assertions for performance requirements
        assertThat(errorRate).isLessThan(1.0); // Less than 1% error rate
        assertThat(averageResponseTime).isLessThan(1000); // Average response under 1 second
        assertThat(operationsPerSecond).isGreaterThan(50); // At least 50 operations per second
    }
    
    @Test
    @Order(3)
    @DisplayName("Concurrent Write Operations Stress Test")
    void testConcurrentWriteOperations() throws InterruptedException, ExecutionException {
        System.out.println("=== Testing Concurrent Write Operations ===");
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS / 2); // Fewer threads for writes
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS / 2);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        List<Future<Void>> futures = new ArrayList<>();
        long testStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < CONCURRENT_THREADS / 2; i++) {
            Future<Void> future = executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD / 2; j++) { // Fewer write operations
                        long operationStart = System.currentTimeMillis();
                        
                        try {
                            // Mix different write operations
                            switch (j % 4) {
                                case 0:
                                    // Create new post
                                    User randomUser = stressUsers.get(random.nextInt(stressUsers.size()));
                                    Post newPost = new Post();
                                    newPost.setUserId(randomUser.getId());
                                    newPost.setTitle("Concurrent Test Post " + System.currentTimeMillis());
                                    newPost.setContent("Content for concurrent test post");
                                    newPost.setEmotion(Post.Emotion.values()[random.nextInt(Post.Emotion.values().length)]);
                                    postRepository.save(newPost);
                                    break;
                                    
                                case 1:
                                    // Add engagement
                                    Post randomPost = stressPosts.get(random.nextInt(stressPosts.size()));
                                    User engagingUser = stressUsers.get(random.nextInt(stressUsers.size()));
                                    
                                    UserEngagement engagement = new UserEngagement();
                                    engagement.setUserId(engagingUser.getId());
                                    engagement.setPostId(randomPost.getId());
                                    engagement.setType(UserEngagement.EngagementType.RESONANCE);
                                    
                                    try {
                                        userEngagementRepository.save(engagement);
                                    } catch (Exception e) {
                                        // Ignore duplicate key violations
                                        if (!e.getMessage().contains("duplicate key")) {
                                            throw e;
                                        }
                                    }
                                    break;
                                    
                                case 2:
                                    // Create comment
                                    Post commentPost = stressPosts.get(random.nextInt(stressPosts.size()));
                                    User commentUser = stressUsers.get(random.nextInt(stressUsers.size()));
                                    
                                    Comment comment = new Comment();
                                    comment.setPostId(commentPost.getId());
                                    comment.setAuthorId(commentUser.getId());
                                    comment.setContent("Concurrent test comment " + System.currentTimeMillis());
                                    commentRepository.save(comment);
                                    break;
                                    
                                case 3:
                                    // Update post views
                                    Post viewPost = stressPosts.get(random.nextInt(stressPosts.size()));
                                    viewPost.setViews(viewPost.getViews() + 1);
                                    postRepository.save(viewPost);
                                    break;
                            }
                            
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.println("Write operation failed: " + e.getMessage());
                        }
                        
                        totalResponseTime.addAndGet(System.currentTimeMillis() - operationStart);
                    }
                } finally {
                    latch.countDown();
                }
                return null;
            });
            
            futures.add(future);
        }
        
        // Wait for all operations to complete
        latch.await(180, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        
        // Calculate metrics
        int totalOperations = (CONCURRENT_THREADS / 2) * (OPERATIONS_PER_THREAD / 2);
        double averageResponseTime = successCount.get() > 0 ? (double) totalResponseTime.get() / successCount.get() : 0;
        double operationsPerSecond = (double) successCount.get() / (testDuration / 1000.0);
        double errorRate = (double) errorCount.get() / totalOperations * 100;
        
        System.out.println("=== Concurrent Write Operations Results ===");
        System.out.println("Total operations attempted: " + totalOperations);
        System.out.println("Successful operations: " + successCount.get());
        System.out.println("Failed operations: " + errorCount.get());
        System.out.println("Error rate: " + String.format("%.2f%%", errorRate));
        System.out.println("Average response time: " + String.format("%.2f ms", averageResponseTime));
        System.out.println("Operations per second: " + String.format("%.2f", operationsPerSecond));
        System.out.println("Total test duration: " + testDuration + "ms");
        
        // Assertions for performance requirements
        assertThat(errorRate).isLessThan(5.0); // Less than 5% error rate for writes
        assertThat(averageResponseTime).isLessThan(2000); // Average response under 2 seconds
        assertThat(operationsPerSecond).isGreaterThan(20); // At least 20 write operations per second
    }
    
    @Test
    @Order(4)
    @DisplayName("Memory Usage Under Load Test")
    void testMemoryUsageUnderLoad() throws InterruptedException {
        System.out.println("=== Testing Memory Usage Under Load ===");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection and get baseline
        System.gc();
        Thread.sleep(1000);
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Baseline memory usage: " + (baselineMemory / 1024 / 1024) + " MB");
        
        // Perform intensive operations
        ExecutorService executor = Executors.newFixedThreadPool(20);
        List<Future<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < 20; i++) {
            Future<Void> future = executor.submit(() -> {
                for (int j = 0; j < 50; j++) {
                    // Perform memory-intensive operations
                    postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100));
                    userRepository.findAll(PageRequest.of(0, 100));
                }
                return null;
            });
            futures.add(future);
        }
        
        // Wait for completion
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                System.err.println("Memory test operation failed: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        
        // Check peak memory usage
        long peakMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = peakMemory - baselineMemory;
        
        System.out.println("Peak memory usage: " + (peakMemory / 1024 / 1024) + " MB");
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        
        // Force garbage collection
        System.gc();
        Thread.sleep(2000);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryAfterGC = finalMemory - baselineMemory;
        
        System.out.println("Memory after GC: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("Memory retained after GC: " + (memoryAfterGC / 1024 / 1024) + " MB");
        
        // Memory should not increase by more than 200MB under load
        assertThat(memoryIncrease).isLessThan(200 * 1024 * 1024);
        
        // Memory should be mostly released after GC (less than 50MB retained)
        assertThat(memoryAfterGC).isLessThan(50 * 1024 * 1024);
    }
    
    @Test
    @Order(5)
    @DisplayName("Database Connection Pool Stress Test")
    void testDatabaseConnectionPoolStress() throws InterruptedException, ExecutionException {
        System.out.println("=== Testing Database Connection Pool Under Stress ===");
        
        int threadCount = 100; // More threads than typical pool size
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // Each thread performs multiple database operations
                    for (int j = 0; j < 10; j++) {
                        userRepository.count();
                        postRepository.count();
                        commentRepository.count();
                        Thread.sleep(10); // Small delay to simulate real work
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Connection pool stress error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(60, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        System.out.println("Connection pool stress test results:");
        System.out.println("Successful threads: " + successCount.get() + "/" + threadCount);
        System.out.println("Failed threads: " + errorCount.get());
        System.out.println("Total time: " + totalTime + "ms");
        
        // All threads should complete successfully
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(errorCount.get()).isEqualTo(0);
    }
    
    @AfterAll
    static void cleanup() {
        System.out.println("=== Stress Test Cleanup ===");
        System.out.println("Stress test data will be cleaned up by test framework");
    }
}

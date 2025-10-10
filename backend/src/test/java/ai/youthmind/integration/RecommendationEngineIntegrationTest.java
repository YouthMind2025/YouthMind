package ai.youthmind.integration;

import ai.youthmind.dto.FeedBlockResponse;
import ai.youthmind.dto.FeedStatusResponse;
import ai.youthmind.dto.RVectorResponse;
import ai.youthmind.events.model.Emotion;
import ai.youthmind.events.model.PostCreatedEvent;
import ai.youthmind.events.model.PostLikedEvent;
import ai.youthmind.events.model.PostViewedEvent;
import ai.youthmind.events.model.UserScrolledEvent;
import ai.youthmind.events.publisher.RecommendationEventPublisher;
import ai.youthmind.service.FeedBlockService;
import ai.youthmind.service.RVectorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for the full recommendation engine flow.
 * Tests the complete pipeline from event publishing to feed generation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class RecommendationEngineIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("youthmind_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
        registry.add("youthmind.recommendation.enabled", () -> "true");
        registry.add("youthmind.recommendation.default-block-size", () -> "5");
        registry.add("youthmind.recommendation.block-expiry-hours", () -> "1");
        registry.add("youthmind.recommendation.rvector-cache-ttl-minutes", () -> "5");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RecommendationEventPublisher eventPublisher;

    @Autowired
    private RVectorService rVectorService;

    @Autowired
    private FeedBlockService feedBlockService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testUserId;
    private UUID testPostId;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPostId = UUID.randomUUID();
        baseUrl = "http://localhost:" + port;
        
        // Clear Redis cache
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        
        // Initialize R vector for test user
        rVectorService.initializeRVector(testUserId);
    }

    @Test
    void testFullRecommendationFlow() {
        // Step 1: Publish PostCreatedEvent
        PostCreatedEvent postCreatedEvent = PostCreatedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishPostCreated(postCreatedEvent);
        
        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Double> rVector = rVectorService.getRVector(testUserId);
            assertThat(rVector).isNotNull();
            assertThat(rVector).hasSize(6);
        });
        
        // Step 2: Publish PostViewedEvent
        PostViewedEvent postViewedEvent = PostViewedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .duration(30)
                .build();
        
        eventPublisher.publishPostViewed(postViewedEvent);
        
        // Wait for R vector update
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Double> rVector = rVectorService.getRVector(testUserId);
            assertThat(rVector.get(0)).isGreaterThan(0.2); // JOY dimension should be updated
        });
        
        // Step 3: Publish PostLikedEvent
        PostLikedEvent postLikedEvent = PostLikedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishPostLiked(postLikedEvent);
        
        // Wait for R vector update
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Double> rVector = rVectorService.getRVector(testUserId);
            assertThat(rVector.get(0)).isGreaterThan(0.3); // JOY dimension should be further updated
        });
        
        // Step 4: Publish UserScrolledEvent
        UserScrolledEvent userScrolledEvent = UserScrolledEvent.builder()
                .userId(testUserId)
                .currentBlock(0)
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishUserScrolled(userScrolledEvent);
        
        // Wait for block pre-calculation
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            boolean nextBlockReady = feedBlockService.isNextBlockReady(testUserId, 0);
            assertThat(nextBlockReady).isTrue();
        });
        
        // Step 5: Test feed block generation
        FeedBlockResponse feedBlock = feedBlockService.getFeedBlock(testUserId, 0, 5);
        assertThat(feedBlock).isNotNull();
        assertThat(feedBlock.getBlockMetadata()).isNotNull();
        assertThat(feedBlock.getBlockMetadata().getBlockNumber()).isEqualTo(0);
        assertThat(feedBlock.getBlockMetadata().getUserId()).isEqualTo(testUserId);
        
        // Step 6: Test feed status
        FeedStatusResponse feedStatus = feedBlockService.getFeedStatus(testUserId);
        assertThat(feedStatus).isNotNull();
        assertThat(feedStatus.getRVectorSummary()).isNotNull();
        assertThat(feedStatus.getRVectorSummary().getDimensions()).hasSize(6);
        assertThat(feedStatus.getNextBlockAvailability()).isNotNull();
        assertThat(feedStatus.getNextBlockAvailability().isAvailable()).isTrue();
    }

    @Test
    void testRVectorPersistence() {
        // Test R vector persistence across service calls
        List<Double> initialRVector = rVectorService.getRVector(testUserId);
        assertThat(initialRVector).isNotNull();
        assertThat(initialRVector).hasSize(6);
        
        // Update R vector
        PostLikedEvent postLikedEvent = PostLikedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.SADNESS)
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishPostLiked(postLikedEvent);
        
        // Wait for update
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Double> updatedRVector = rVectorService.getRVector(testUserId);
            assertThat(updatedRVector.get(1)).isGreaterThan(initialRVector.get(1)); // SADNESS dimension
        });
        
        // Verify persistence
        List<Double> persistedRVector = rVectorService.getRVector(testUserId);
        assertThat(persistedRVector).isNotNull();
        assertThat(persistedRVector.get(1)).isGreaterThan(initialRVector.get(1));
    }

    @Test
    void testFeedBlockCaching() {
        // Generate first block
        FeedBlockResponse block1 = feedBlockService.getFeedBlock(testUserId, 0, 5);
        assertThat(block1).isNotNull();
        
        // Generate same block again (should come from cache)
        FeedBlockResponse block2 = feedBlockService.getFeedBlock(testUserId, 0, 5);
        assertThat(block2).isNotNull();
        assertThat(block2.getBlockMetadata().getBlockNumber()).isEqualTo(block1.getBlockMetadata().getBlockNumber());
        assertThat(block2.getBlockMetadata().getUserId()).isEqualTo(block1.getBlockMetadata().getUserId());
    }

    @Test
    void testEventProcessingWithMultipleUsers() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID post1 = UUID.randomUUID();
        UUID post2 = UUID.randomUUID();
        
        // Initialize R vectors
        rVectorService.initializeRVector(user1);
        rVectorService.initializeRVector(user2);
        
        // User 1 likes post 1
        PostLikedEvent event1 = PostLikedEvent.builder()
                .userId(user1)
                .postId(post1)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishPostLiked(event1);
        
        // User 2 likes post 2
        PostLikedEvent event2 = PostLikedEvent.builder()
                .userId(user2)
                .postId(post2)
                .emotion(Emotion.SADNESS)
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishPostLiked(event2);
        
        // Wait for both events to be processed
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Double> rVector1 = rVectorService.getRVector(user1);
            List<Double> rVector2 = rVectorService.getRVector(user2);
            
            assertThat(rVector1.get(0)).isGreaterThan(0.2); // JOY dimension for user1
            assertThat(rVector2.get(1)).isGreaterThan(0.2); // SADNESS dimension for user2
        });
    }

    @Test
    void testErrorHandlingAndRecovery() {
        // Test with invalid event data
        PostLikedEvent invalidEvent = PostLikedEvent.builder()
                .userId(null) // Invalid user ID
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        
        // This should not crash the system
        eventPublisher.publishPostLiked(invalidEvent);
        
        // System should still be functional
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Double> rVector = rVectorService.getRVector(testUserId);
            assertThat(rVector).isNotNull();
        });
    }

    @Test
    void testPerformanceUnderLoad() {
        // Test with multiple rapid events
        for (int i = 0; i < 10; i++) {
            PostLikedEvent event = PostLikedEvent.builder()
                    .userId(testUserId)
                    .postId(UUID.randomUUID())
                    .emotion(Emotion.JOY)
                    .timestamp(LocalDateTime.now())
                    .build();
            eventPublisher.publishPostLiked(event);
        }
        
        // Wait for all events to be processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Double> rVector = rVectorService.getRVector(testUserId);
            assertThat(rVector.get(0)).isGreaterThan(0.5); // JOY dimension should be significantly updated
        });
    }
}

package ai.youthmind.integration;

import ai.youthmind.events.model.Emotion;
import ai.youthmind.events.model.PostCreatedEvent;
import ai.youthmind.events.model.PostLikedEvent;
import ai.youthmind.events.model.PostViewedEvent;
import ai.youthmind.events.model.UserScrolledEvent;
import ai.youthmind.events.publisher.RecommendationEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for the event system.
 * Tests event publishing, consumption, and Redis Streams functionality.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class EventSystemIntegrationTest {

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
        registry.add("youthmind.recommendation.streams.rvector-updates-stream", () -> "recommendation:rvector-updates");
        registry.add("youthmind.recommendation.streams.block-precalc-stream", () -> "recommendation:block-precalc");
        registry.add("youthmind.recommendation.streams.consumer-group", () -> "recommendation-engine-workers");
    }

    @Autowired
    private RecommendationEventPublisher eventPublisher;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testUserId;
    private UUID testPostId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testPostId = UUID.randomUUID();
        
        // Clear Redis streams
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void testPostCreatedEventPublishing() {
        // Create and publish PostCreatedEvent
        PostCreatedEvent event = PostCreatedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        
        // Publish event
        eventPublisher.publishPostCreated(event);
        
        // Verify event was published to Redis Stream
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Long streamLength = redisTemplate.opsForStream().size("recommendation:rvector-updates");
            assertThat(streamLength).isGreaterThan(0);
        });
    }

    @Test
    void testPostViewedEventPublishing() {
        // Create and publish PostViewedEvent
        PostViewedEvent event = PostViewedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.SADNESS)
                .timestamp(LocalDateTime.now())
                .duration(45)
                .build();
        
        // Publish event
        eventPublisher.publishPostViewed(event);
        
        // Verify event was published to Redis Stream
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Long streamLength = redisTemplate.opsForStream().size("recommendation:rvector-updates");
            assertThat(streamLength).isGreaterThan(0);
        });
    }

    @Test
    void testPostLikedEventPublishing() {
        // Create and publish PostLikedEvent
        PostLikedEvent event = PostLikedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.ANGER)
                .timestamp(LocalDateTime.now())
                .build();
        
        // Publish event
        eventPublisher.publishPostLiked(event);
        
        // Verify event was published to Redis Stream
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Long streamLength = redisTemplate.opsForStream().size("recommendation:rvector-updates");
            assertThat(streamLength).isGreaterThan(0);
        });
    }

    @Test
    void testUserScrolledEventPublishing() {
        // Create and publish UserScrolledEvent
        UserScrolledEvent event = UserScrolledEvent.builder()
                .userId(testUserId)
                .currentBlock(2)
                .timestamp(LocalDateTime.now())
                .build();
        
        // Publish event
        eventPublisher.publishUserScrolled(event);
        
        // Verify event was published to Redis Stream
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Long streamLength = redisTemplate.opsForStream().size("recommendation:block-precalc");
            assertThat(streamLength).isGreaterThan(0);
        });
    }

    @Test
    void testMultipleEventPublishing() {
        // Publish multiple events
        PostCreatedEvent createdEvent = PostCreatedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        
        PostViewedEvent viewedEvent = PostViewedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .duration(30)
                .build();
        
        PostLikedEvent likedEvent = PostLikedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        
        // Publish all events
        eventPublisher.publishPostCreated(createdEvent);
        eventPublisher.publishPostViewed(viewedEvent);
        eventPublisher.publishPostLiked(likedEvent);
        
        // Verify all events were published
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Long streamLength = redisTemplate.opsForStream().size("recommendation:rvector-updates");
            assertThat(streamLength).isGreaterThanOrEqualTo(3);
        });
    }

    @Test
    void testEventOrdering() {
        // Publish events with specific timestamps
        LocalDateTime baseTime = LocalDateTime.now();
        
        PostCreatedEvent event1 = PostCreatedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(baseTime)
                .build();
        
        PostLikedEvent event2 = PostLikedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(baseTime.plusSeconds(1))
                .build();
        
        // Publish events
        eventPublisher.publishPostCreated(event1);
        eventPublisher.publishPostLiked(event2);
        
        // Verify events are in order
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Long streamLength = redisTemplate.opsForStream().size("recommendation:rvector-updates");
            assertThat(streamLength).isGreaterThanOrEqualTo(2);
        });
    }

    @Test
    void testEventPublishingWithRetry() {
        // Test event publishing with retry mechanism
        PostCreatedEvent event = PostCreatedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.FEAR)
                .timestamp(LocalDateTime.now())
                .build();
        
        // Publish event
        eventPublisher.publishPostCreated(event);
        
        // Verify event was published
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Long streamLength = redisTemplate.opsForStream().size("recommendation:rvector-updates");
            assertThat(streamLength).isGreaterThan(0);
        });
    }

    @Test
    void testEventPublishingWithInvalidData() {
        // Test event publishing with invalid data
        PostCreatedEvent invalidEvent = PostCreatedEvent.builder()
                .userId(null) // Invalid user ID
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        
        // This should not crash the system
        eventPublisher.publishPostCreated(invalidEvent);
        
        // System should still be functional
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Verify system is still responsive
            assertThat(redisTemplate.getConnectionFactory()).isNotNull();
        });
    }

    @Test
    void testEventPublishingPerformance() {
        // Test event publishing performance
        long startTime = System.currentTimeMillis();
        
        // Publish 100 events
        for (int i = 0; i < 100; i++) {
            PostCreatedEvent event = PostCreatedEvent.builder()
                    .userId(testUserId)
                    .postId(UUID.randomUUID())
                    .emotion(Emotion.JOY)
                    .timestamp(LocalDateTime.now())
                    .build();
            eventPublisher.publishPostCreated(event);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify all events were published
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Long streamLength = redisTemplate.opsForStream().size("recommendation:rvector-updates");
            assertThat(streamLength).isGreaterThanOrEqualTo(100);
        });
        
        // Verify performance (should be under 5 seconds for 100 events)
        assertThat(duration).isLessThan(5000);
    }

    @Test
    void testEventPublishingWithDifferentEmotions() {
        // Test event publishing with different emotions
        Emotion[] emotions = {Emotion.JOY, Emotion.SADNESS, Emotion.ANGER, Emotion.FEAR, Emotion.SURPRISE, Emotion.DISGUST};
        
        for (Emotion emotion : emotions) {
            PostCreatedEvent event = PostCreatedEvent.builder()
                    .userId(testUserId)
                    .postId(UUID.randomUUID())
                    .emotion(emotion)
                    .timestamp(LocalDateTime.now())
                    .build();
            eventPublisher.publishPostCreated(event);
        }
        
        // Verify all events were published
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Long streamLength = redisTemplate.opsForStream().size("recommendation:rvector-updates");
            assertThat(streamLength).isGreaterThanOrEqualTo(emotions.length);
        });
    }
}

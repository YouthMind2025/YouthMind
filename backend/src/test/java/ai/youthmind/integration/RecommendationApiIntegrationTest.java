package ai.youthmind.integration;

import ai.youthmind.dto.FeedBlockResponse;
import ai.youthmind.dto.FeedStatusResponse;
import ai.youthmind.dto.RVectorResponse;
import ai.youthmind.events.model.Emotion;
import ai.youthmind.events.model.PostCreatedEvent;
import ai.youthmind.events.publisher.RecommendationEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for recommendation API endpoints.
 * Tests all recommendation-related REST endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class RecommendationApiIntegrationTest {

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
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RecommendationEventPublisher eventPublisher;

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
    }

    @Test
    void testFeedBlocksEndpoint() {
        // Publish some events to generate R vector
        PostCreatedEvent event = PostCreatedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishPostCreated(event);
        
        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Verify R vector is updated
            assertThat(redisTemplate.hasKey("recommendation:rvector:" + testUserId)).isTrue();
        });
        
        // Test feed blocks endpoint
        String url = baseUrl + "/v1/feed/blocks?userId=" + testUserId + "&blockNumber=0&size=5";
        ResponseEntity<FeedBlockResponse> response = restTemplate.getForEntity(url, FeedBlockResponse.class);
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getBlockMetadata()).isNotNull();
        assertThat(response.getBody().getBlockMetadata().getBlockNumber()).isEqualTo(0);
        assertThat(response.getBody().getBlockMetadata().getUserId()).isEqualTo(testUserId);
    }

    @Test
    void testFeedScrollEndpoint() {
        // Test feed scroll endpoint
        String url = baseUrl + "/v1/feed/scroll";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        
        String requestBody = "{\"userId\":\"" + testUserId + "\",\"currentBlock\":0}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testFeedStatusEndpoint() {
        // Publish some events to generate R vector
        PostCreatedEvent event = PostCreatedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishPostCreated(event);
        
        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(redisTemplate.hasKey("recommendation:rvector:" + testUserId)).isTrue();
        });
        
        // Test feed status endpoint
        String url = baseUrl + "/v1/feed/status?userId=" + testUserId;
        ResponseEntity<FeedStatusResponse> response = restTemplate.getForEntity(url, FeedStatusResponse.class);
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRVectorSummary()).isNotNull();
        assertThat(response.getBody().getRVectorSummary().getDimensions()).hasSize(6);
        assertThat(response.getBody().getNextBlockAvailability()).isNotNull();
        assertThat(response.getBody().getUserActivityStatus()).isNotNull();
    }

    @Test
    void testRVectorEndpoint() {
        // Publish some events to generate R vector
        PostCreatedEvent event = PostCreatedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishPostCreated(event);
        
        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(redisTemplate.hasKey("recommendation:rvector:" + testUserId)).isTrue();
        });
        
        // Test R vector endpoint
        String url = baseUrl + "/v1/recommendation/rvector?userId=" + testUserId;
        ResponseEntity<RVectorResponse> response = restTemplate.getForEntity(url, RVectorResponse.class);
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo(testUserId);
        assertThat(response.getBody().getDimensions()).hasSize(6);
        assertThat(response.getBody().getTop3Emotions()).isNotNull();
        assertThat(response.getBody().getStatus()).isNotNull();
    }

    @Test
    void testRVectorHistoryEndpoint() {
        // Publish multiple events to generate history
        for (int i = 0; i < 3; i++) {
            PostCreatedEvent event = PostCreatedEvent.builder()
                    .userId(testUserId)
                    .postId(UUID.randomUUID())
                    .emotion(Emotion.JOY)
                    .timestamp(LocalDateTime.now())
                    .build();
            eventPublisher.publishPostCreated(event);
        }
        
        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(redisTemplate.hasKey("recommendation:rvector:" + testUserId)).isTrue();
        });
        
        // Test R vector history endpoint
        String url = baseUrl + "/v1/recommendation/rvector/history?userId=" + testUserId;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testRVectorResetEndpoint() {
        // Publish some events to generate R vector
        PostCreatedEvent event = PostCreatedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishPostCreated(event);
        
        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(redisTemplate.hasKey("recommendation:rvector:" + testUserId)).isTrue();
        });
        
        // Test R vector reset endpoint
        String url = baseUrl + "/v1/recommendation/rvector/reset";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        
        String requestBody = "{\"userId\":\"" + testUserId + "\"}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testRecommendationPerformanceEndpoint() {
        // Test recommendation performance endpoint
        String url = baseUrl + "/v1/analytics/recommendation/performance";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testUserRecommendationAnalyticsEndpoint() {
        // Publish some events to generate analytics data
        PostCreatedEvent event = PostCreatedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishPostCreated(event);
        
        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(redisTemplate.hasKey("recommendation:rvector:" + testUserId)).isTrue();
        });
        
        // Test user recommendation analytics endpoint
        String url = baseUrl + "/v1/analytics/recommendation/user/" + testUserId;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testEmotionDistributionEndpoint() {
        // Publish events with different emotions
        Emotion[] emotions = {Emotion.JOY, Emotion.SADNESS, Emotion.ANGER};
        for (Emotion emotion : emotions) {
            PostCreatedEvent event = PostCreatedEvent.builder()
                    .userId(testUserId)
                    .postId(UUID.randomUUID())
                    .emotion(emotion)
                    .timestamp(LocalDateTime.now())
                    .build();
            eventPublisher.publishPostCreated(event);
        }
        
        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(redisTemplate.hasKey("recommendation:rvector:" + testUserId)).isTrue();
        });
        
        // Test emotion distribution endpoint
        String url = baseUrl + "/v1/analytics/recommendation/emotion-distribution";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testAdminRecommendationRulesEndpoint() {
        // Test admin recommendation rules endpoint
        String url = baseUrl + "/v1/admin/recommendation/rules";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testAdminRecommendationRecalculateEndpoint() {
        // Test admin recommendation recalculate endpoint
        String url = baseUrl + "/v1/admin/recommendation/recalculate-all";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        
        String requestBody = "{\"happyPercentage\":0.3,\"sadPercentage\":0.2,\"angryPercentage\":0.2,\"fearPercentage\":0.1,\"surprisedPercentage\":0.1,\"conflictedPercentage\":0.1}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void testErrorHandling() {
        // Test with invalid user ID
        String url = baseUrl + "/v1/feed/blocks?userId=invalid-uuid&blockNumber=0&size=5";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should return error response
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void testRateLimiting() {
        // Test rate limiting by making multiple rapid requests
        String url = baseUrl + "/v1/feed/status?userId=" + testUserId;
        
        // Make 10 rapid requests
        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            // Should not be rate limited for this number of requests
            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        }
    }
}

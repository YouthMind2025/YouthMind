package ai.youthmind.benchmark;

import ai.youthmind.dto.FeedBlockResponse;
import ai.youthmind.dto.FeedStatusResponse;
import ai.youthmind.events.model.Emotion;
import ai.youthmind.events.model.PostCreatedEvent;
import ai.youthmind.events.model.PostLikedEvent;
import ai.youthmind.events.model.PostViewedEvent;
import ai.youthmind.events.publisher.RecommendationEventPublisher;
import ai.youthmind.service.FeedBlockService;
import ai.youthmind.service.RVectorService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for recommendation engine key operations.
 * Measures performance of critical paths in the recommendation system.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
public class RecommendationEngineBenchmark {

    private ConfigurableApplicationContext context;
    private RecommendationEventPublisher eventPublisher;
    private RVectorService rVectorService;
    private FeedBlockService feedBlockService;
    
    private UUID testUserId;
    private UUID testPostId;

    @Setup(Level.Trial)
    public void setup() {
        // Start Spring context
        context = SpringApplication.run(TestApplication.class);
        
        // Get beans
        eventPublisher = context.getBean(RecommendationEventPublisher.class);
        rVectorService = context.getBean(RVectorService.class);
        feedBlockService = context.getBean(FeedBlockService.class);
        
        // Initialize test data
        testUserId = UUID.randomUUID();
        testPostId = UUID.randomUUID();
        rVectorService.initializeRVector(testUserId);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Benchmark
    public void benchmarkRVectorUpdate() {
        // Benchmark R vector update operation
        PostLikedEvent event = PostLikedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishPostLiked(event);
    }

    @Benchmark
    public void benchmarkRVectorRetrieval() {
        // Benchmark R vector retrieval operation
        List<Double> rVector = rVectorService.getRVector(testUserId);
        assert rVector != null && rVector.size() == 6;
    }

    @Benchmark
    public void benchmarkFeedBlockGeneration() {
        // Benchmark feed block generation
        FeedBlockResponse feedBlock = feedBlockService.getFeedBlock(testUserId, 0, 20);
        assert feedBlock != null;
    }

    @Benchmark
    public void benchmarkFeedStatusRetrieval() {
        // Benchmark feed status retrieval
        FeedStatusResponse feedStatus = feedBlockService.getFeedStatus(testUserId);
        assert feedStatus != null;
    }

    @Benchmark
    public void benchmarkEventPublishing() {
        // Benchmark event publishing
        PostCreatedEvent event = PostCreatedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishPostCreated(event);
    }

    @Benchmark
    public void benchmarkMultipleEventPublishing() {
        // Benchmark multiple event publishing
        for (int i = 0; i < 10; i++) {
            PostViewedEvent event = PostViewedEvent.builder()
                    .userId(testUserId)
                    .postId(UUID.randomUUID())
                    .emotion(Emotion.JOY)
                    .timestamp(LocalDateTime.now())
                    .duration(30)
                    .build();
            
            eventPublisher.publishPostViewed(event);
        }
    }

    @Benchmark
    public void benchmarkRVectorUpdateWithDifferentEmotions() {
        // Benchmark R vector update with different emotions
        Emotion[] emotions = {Emotion.JOY, Emotion.SADNESS, Emotion.ANGER, Emotion.FEAR, Emotion.SURPRISE, Emotion.DISGUST};
        Emotion emotion = emotions[System.nanoTime() % emotions.length];
        
        PostLikedEvent event = PostLikedEvent.builder()
                .userId(testUserId)
                .postId(testPostId)
                .emotion(emotion)
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishPostLiked(event);
    }

    @Benchmark
    public void benchmarkFeedBlockGenerationWithDifferentSizes() {
        // Benchmark feed block generation with different sizes
        int[] sizes = {5, 10, 20, 50};
        int size = sizes[System.nanoTime() % sizes.length];
        
        FeedBlockResponse feedBlock = feedBlockService.getFeedBlock(testUserId, 0, size);
        assert feedBlock != null;
    }

    @Benchmark
    public void benchmarkConcurrentRVectorUpdates() {
        // Benchmark concurrent R vector updates
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        
        rVectorService.initializeRVector(userId1);
        rVectorService.initializeRVector(userId2);
        
        PostLikedEvent event1 = PostLikedEvent.builder()
                .userId(userId1)
                .postId(testPostId)
                .emotion(Emotion.JOY)
                .timestamp(LocalDateTime.now())
                .build();
        
        PostLikedEvent event2 = PostLikedEvent.builder()
                .userId(userId2)
                .postId(testPostId)
                .emotion(Emotion.SADNESS)
                .timestamp(LocalDateTime.now())
                .build();
        
        eventPublisher.publishPostLiked(event1);
        eventPublisher.publishPostLiked(event2);
    }

    @Benchmark
    public void benchmarkFeedBlockGenerationWithCaching() {
        // Benchmark feed block generation with caching
        // First call - should generate and cache
        FeedBlockResponse feedBlock1 = feedBlockService.getFeedBlock(testUserId, 0, 20);
        assert feedBlock1 != null;
        
        // Second call - should come from cache
        FeedBlockResponse feedBlock2 = feedBlockService.getFeedBlock(testUserId, 0, 20);
        assert feedBlock2 != null;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(RecommendationEngineBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    @ComponentScan(basePackages = "ai.youthmind")
    public static class TestApplication {
        public static void main(String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }
    }
}

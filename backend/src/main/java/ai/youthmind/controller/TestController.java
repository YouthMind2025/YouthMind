package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.FeedResponse;
import ai.youthmind.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Test Endpoints", description = "Test and health check endpoints for development and monitoring")
public class TestController {

    @Autowired
    private RedisService redisService;

    @GetMapping("/hello")
    @Operation(
        summary = "Hello endpoint",
        description = "Simple greeting endpoint to verify the API is running"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully returned greeting message")
    })
    public ApiResponse<String> hello() {
        return ApiResponse.success("Hello, YouthMind!", 200);
    }

    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Health check endpoint to verify the backend service is running"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Backend is healthy and running")
    })
    public ApiResponse<String> health() {
        return ApiResponse.success("YouthMind Backend is running successfully!", 200);
    }

    @GetMapping("/redis")
    @Operation(
        summary = "Redis test",
        description = "Test Redis connectivity by setting and retrieving a test value"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Redis test completed successfully"),
        @ApiResponse(responseCode = "500", description = "Redis connection failed or test failed")
    })
    public ApiResponse<String> testRedis() {
        String key = "youthmind:test:greeting";
        String value = "Hello from Redis!";
        try {
            redisService.setValue(key, value, 1, TimeUnit.MINUTES);
            String retrievedValue = (String) redisService.getValue(key);
            if (value.equals(retrievedValue)) {
                String message = "OK: Successfully set and retrieved a value from Redis. Got: '" + retrievedValue + "'";
                return ApiResponse.success(message, 200);
            } else {
                String message = "FAIL: Retrieved a different value from Redis. Expected: '" + value + "', Got: '" + retrievedValue + "'";
                return ApiResponse.error(message, 500);
            }
        } catch (Exception e) {
            String message = "FAIL: An exception occurred while interacting with Redis: " + e.getMessage();
            return ApiResponse.error(message, 500);
        }
    }

    @GetMapping("/redis/cache")
    @Operation(
        summary = "Test Redis caching",
        description = "Test Redis caching with feed simulation"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cache test completed successfully")
    })
    public ApiResponse<Map<String, Object>> testRedisCache() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test feed caching
            String cacheKey = "latest:0:20:newest";
            FeedResponse testFeed = new FeedResponse();
            testFeed.setCurrentPage(0);
            testFeed.setTotalPages(1);
            testFeed.setTotalElements(0);
            testFeed.setPageSize(20);
            testFeed.setHasNext(false);
            testFeed.setFirst(true);
            testFeed.setLast(true);
            
            // Cache the feed
            redisService.cacheFeed(cacheKey, testFeed);
            
            // Retrieve from cache
            FeedResponse cachedFeed = redisService.getCachedFeed(cacheKey);
            
            result.put("cache_test", cachedFeed != null ? "SUCCESS" : "FAILED");
            result.put("cached_page", cachedFeed != null ? cachedFeed.getCurrentPage() : "null");
            result.put("cached_total", cachedFeed != null ? cachedFeed.getTotalElements() : "null");
            
            // Test cache invalidation
            redisService.invalidateFeedCache();
            FeedResponse afterInvalidation = redisService.getCachedFeed(cacheKey);
            result.put("invalidation_test", afterInvalidation == null ? "SUCCESS" : "FAILED");
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return ApiResponse.success(result, 200);
    }

    @PostMapping("/redis/pubsub")
    @Operation(
        summary = "Test Redis pub/sub",
        description = "Test Redis pub/sub messaging by publishing test events"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pub/sub test completed successfully")
    })
    public ApiResponse<Map<String, String>> testRedisPubSub(
        @RequestParam(defaultValue = "test_post_123") String postId,
        @RequestParam(defaultValue = "test_user_456") String userId,
        @RequestParam(defaultValue = "happy") String emotion
    ) {
        Map<String, String> result = new HashMap<>();
        
        try {
            // Test structured event publishing
            redisService.publishPostCreatedEvent(postId, userId, emotion);
            result.put("post_created_event", "Published successfully");
            
            // Test AI processed event
            redisService.publishAiProcessedEvent(postId, "{\"sentiment\":\"positive\",\"confidence\":0.85}");
            result.put("ai_processed_event", "Published successfully");
            
            // Test legacy publishing
            redisService.publishPostCreated("Legacy test message");
            result.put("legacy_event", "Published successfully");
            
            result.put("status", "All pub/sub tests completed");
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("status", "Pub/sub test failed");
        }
        
        return ApiResponse.success(result, 200);
    }

    @GetMapping("/redis/hot-posts")
    @Operation(
        summary = "Test hot posts scoring",
        description = "Test Redis hot posts scoring system"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Hot posts test completed successfully")
    })
    public ApiResponse<Map<String, Object>> testHotPosts() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Add some test posts to hot posts
            redisService.updateHotPostScore("post_1", 1.5);
            redisService.updateHotPostScore("post_2", 2.0);
            redisService.updateHotPostScore("post_3", 0.8);
            
            // Get hot posts
            var hotPosts = redisService.getHotPosts(10);
            result.put("hot_posts", hotPosts);
            result.put("hot_posts_count", hotPosts.size());
            result.put("status", "Hot posts test completed");
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("status", "Hot posts test failed");
        }
        
        return ApiResponse.success(result, 200);
    }
} 
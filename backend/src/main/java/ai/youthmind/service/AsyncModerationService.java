package ai.youthmind.service;

import ai.youthmind.dto.ModerationResult;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.entity.UserModerationStatus;
import ai.youthmind.entity.WarningHistory;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserModerationStatusRepository;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.repository.WarningHistoryRepository;
import ai.youthmind.util.ModerationUtils;
import ai.youthmind.util.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * Async moderation service for processing content moderation in background
 * Handles high-volume moderation requests with queue-based processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncModerationService {

    private final AIServiceClient aiServiceClient;
    private final DeepSeekAdapterService deepSeekAdapterService;
    private final PostModerationService postModerationService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserModerationStatusRepository moderationStatusRepository;
    private final WarningHistoryRepository warningHistoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Thread pool for async moderation processing
    private final ThreadPoolExecutor moderationExecutor = new ThreadPoolExecutor(
        10, // core pool size
        50, // maximum pool size
        60L, TimeUnit.SECONDS, // keep alive time
        new LinkedBlockingQueue<>(1000), // queue capacity
        r -> {
            Thread t = new Thread(r, "moderation-worker-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        }
    );

    /**
     * Process moderation asynchronously for a post using the layered PostModerationService
     */
    @Async("moderationExecutor")
    public CompletableFuture<ModerationResult> processModerationAsync(UUID postId, String content, UUID userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PerformanceMonitor.start("moderation_processing");
                
                // Check if already processed
                String cacheKey = "moderation:post:" + postId;
                ModerationResult cached = (ModerationResult) redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("Returning cached moderation result for post {}", postId);
                    return cached;
                }

                // Get the post entity for comprehensive moderation
                Post post = postRepository.findById(postId).orElse(null);
                if (post == null) {
                    log.warn("Post not found for moderation: {}", postId);
                    return ModerationResult.safe("Post not found");
                }

                // Use PostModerationService for layered moderation (Local → Context → AI)
                ModerationResult result = postModerationService.moderatePost(post, userId);
                
                // Cache result for 1 hour
                redisTemplate.opsForValue().set(cacheKey, result, 3600, TimeUnit.SECONDS);
                
                // Update post with moderation result (PostModerationService already handles this)
                // But we still need to update user moderation status
                updateUserModerationStatus(userId, result);
                
                PerformanceMonitor.end("moderation_processing");
                PerformanceMonitor.MethodStats stats = PerformanceMonitor.getStats("moderation_processing");
                log.info("Async layered moderation completed for post {} in {}ms", 
                    postId, stats != null ? stats.getAverageExecutionTime() / 1_000_000 : 0);
                
                return result;
                
            } catch (Exception e) {
                log.error("Error in async moderation for post {}: {}", postId, e.getMessage(), e);
                return new ModerationResult();
            }
        }, moderationExecutor);
    }

    /**
     * Process batch moderation for multiple posts using optimized batch AI calls
     */
    @Async("moderationExecutor")
    public CompletableFuture<Void> processBatchModerationAsync(java.util.List<UUID> postIds) {
        return CompletableFuture.runAsync(() -> {
            try {
                PerformanceMonitor.start("batch_moderation_processing");
                
                log.info("Starting optimized batch moderation for {} posts", postIds.size());
                
                // Fetch all posts first
                List<Post> posts = postIds.stream()
                    .map(postRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
                
                if (posts.isEmpty()) {
                    log.warn("No valid posts found for batch moderation");
                    return;
                }
                
                // Check cache for already processed posts
                List<Post> postsToModerate = new ArrayList<>();
                Map<UUID, ModerationResult> cachedResults = new HashMap<>();
                
                for (Post post : posts) {
                    String cacheKey = "moderation:post:" + post.getId();
                    ModerationResult cached = (ModerationResult) redisTemplate.opsForValue().get(cacheKey);
                    if (cached != null) {
                        log.debug("Using cached result for post {}", post.getId());
                        cachedResults.put(post.getId(), cached);
                        // Note: Cached results don't need post status update as they were already processed
                    } else {
                        postsToModerate.add(post);
                    }
                }
                
                if (postsToModerate.isEmpty()) {
                    log.info("All posts already cached, skipping AI moderation");
                    return;
                }
                
                // Create batch moderation requests for efficient AI processing
                List<DeepSeekAdapterService.BatchModerationRequest> batchRequests = postsToModerate.stream()
                    .map(post -> new DeepSeekAdapterService.BatchModerationRequest(
                        post.getContent(), 
                        "post", 
                        post.getUserId(), 
                        "batch_moderation_" + System.currentTimeMillis()
                    ))
                    .collect(Collectors.toList());
                
                // Process batch moderation using DeepSeekAdapterService
                List<ModerationResult> batchResults = deepSeekAdapterService.moderateContentBatch(batchRequests);
                
                // Process results and update posts
                for (int i = 0; i < postsToModerate.size() && i < batchResults.size(); i++) {
                    Post post = postsToModerate.get(i);
                    ModerationResult result = batchResults.get(i);
                    
                    try {
                        // Cache result for 1 hour
                        String cacheKey = "moderation:post:" + post.getId();
                        redisTemplate.opsForValue().set(cacheKey, result, 3600, TimeUnit.SECONDS);
                        
                        // Update post with moderation result
                        updatePostModerationStatus(post.getId(), result);
                        
                        // Update user moderation status if needed
                        updateUserModerationStatus(post.getUserId(), result);
                        
                    } catch (Exception e) {
                        log.error("Error processing moderation result for post {}: {}", post.getId(), e.getMessage());
                    }
                }
                
                PerformanceMonitor.end("batch_moderation_processing");
                PerformanceMonitor.MethodStats stats = PerformanceMonitor.getStats("batch_moderation_processing");
                log.info("Optimized batch moderation completed for {} posts ({} new, {} cached) in {}ms", 
                    posts.size(), postsToModerate.size(), cachedResults.size(),
                    stats != null ? stats.getAverageExecutionTime() / 1_000_000 : 0);
                    
            } catch (Exception e) {
                log.error("Error in optimized batch moderation: {}", e.getMessage(), e);
            }
        }, moderationExecutor);
    }

    @Transactional
    private void updatePostModerationStatus(UUID postId, ModerationResult result) {
        try {
            Post post = postRepository.findById(postId).orElse(null);
            if (post != null) {
                post.setFlagged(!result.isSafe());
                post.setFlagReason(result.getReason());
                if (!result.isSafe()) {
                    post.setFlaggedAt(LocalDateTime.now());
                }
                postRepository.save(post);
            }
        } catch (Exception e) {
            log.error("Error updating post moderation status for post {}: {}", postId, e.getMessage());
        }
    }

    @Transactional
    private void updateUserModerationStatus(UUID userId, ModerationResult result) {
        try {
            if (!result.isSafe() && result.getConfidence() > 0.8) {
                // Find or create user moderation status
                UserModerationStatus status = moderationStatusRepository.findByUserId(userId)
                    .orElse(new UserModerationStatus());
                
                // Set the User entity by fetching from database
                if (status.getUser() == null) {
                    User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                    status.setUser(user);
                    status.setUserId(userId); // Set userId field explicitly
                }
                
                status.setStatus(ai.youthmind.entity.UserModerationStatus.ModerationStatus.WARNED);
                status.setWarningLevel(status.getWarningLevel() + 1);
                status.setLastWarningAt(LocalDateTime.now());
                status.setTotalWarnings(status.getTotalWarnings() + 1);
                status.setUpdatedAt(LocalDateTime.now());
                
                moderationStatusRepository.save(status);
                
                // Create warning history
                WarningHistory warning = new WarningHistory();
                // Set the User entity by fetching from database
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                warning.setUser(user);
                warning.setWarningType(ai.youthmind.entity.WarningHistory.WarningType.INAPPROPRIATE_CONTENT);
                warning.setReason(result.getReason());
                warning.setSeverity(ai.youthmind.entity.WarningHistory.Severity.MEDIUM);
                warning.setCreatedAt(LocalDateTime.now());
                warning.setExpiresAt(LocalDateTime.now().plusDays(30));
                
                warningHistoryRepository.save(warning);
            }
        } catch (Exception e) {
            log.error("Error updating user moderation status for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Get moderation queue status
     */
    public String getModerationQueueStatus() {
        return String.format("Active: %d, Queue: %d, Completed: %d", 
            moderationExecutor.getActiveCount(),
            moderationExecutor.getQueue().size(),
            moderationExecutor.getCompletedTaskCount());
    }

    /**
     * Shutdown moderation executor gracefully
     */
    public void shutdown() {
        moderationExecutor.shutdown();
        try {
            if (!moderationExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                moderationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            moderationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

package ai.youthmind.service;

import ai.youthmind.dto.ModerationResult;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.entity.UserModerationStatus;
import ai.youthmind.entity.WarningHistory;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserModerationStatusRepository;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Async moderation service for processing content moderation in background
 * Handles high-volume moderation requests with queue-based processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncModerationService {

    private final AIServiceClient aiServiceClient;
    private final PostRepository postRepository;
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
     * Process moderation asynchronously for a post
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

                // Process moderation with AI
                ModerationResult result = aiServiceClient.moderateContent(content, "text", java.util.UUID.randomUUID(), "moderation");
                
                // Cache result for 1 hour
                redisTemplate.opsForValue().set(cacheKey, result, 3600, TimeUnit.SECONDS);
                
                // Update post with moderation result
                updatePostModerationStatus(postId, result);
                
                // Update user moderation status if needed
                updateUserModerationStatus(userId, result);
                
                PerformanceMonitor.end("moderation_processing");
                PerformanceMonitor.MethodStats stats = PerformanceMonitor.getStats("moderation_processing");
                log.info("Async moderation completed for post {} in {}ms", 
                    postId, stats != null ? stats.getAverageExecutionTime() / 1_000_000 : 0);
                
                return result;
                
            } catch (Exception e) {
                log.error("Error in async moderation for post {}: {}", postId, e.getMessage(), e);
                return new ModerationResult();
            }
        }, moderationExecutor);
    }

    /**
     * Process batch moderation for multiple posts
     */
    @Async("moderationExecutor")
    public CompletableFuture<Void> processBatchModerationAsync(java.util.List<UUID> postIds) {
        return CompletableFuture.runAsync(() -> {
            try {
                PerformanceMonitor.start("batch_moderation_processing");
                
                log.info("Starting batch moderation for {} posts", postIds.size());
                
                // Process in parallel with controlled concurrency
                postIds.parallelStream()
                    .limit(20) // Limit concurrent processing
                    .forEach(postId -> {
                        try {
                            Post post = postRepository.findById(postId).orElse(null);
                            if (post != null) {
                                processModerationAsync(postId, post.getContent(), post.getUserId()).join();
                            }
                        } catch (Exception e) {
                            log.error("Error processing post {} in batch: {}", postId, e.getMessage());
                        }
                    });
                
                PerformanceMonitor.end("batch_moderation_processing");
                PerformanceMonitor.MethodStats stats = PerformanceMonitor.getStats("batch_moderation_processing");
                log.info("Batch moderation completed for {} posts in {}ms", 
                    postIds.size(), stats != null ? stats.getAverageExecutionTime() / 1_000_000 : 0);
                    
            } catch (Exception e) {
                log.error("Error in batch moderation: {}", e.getMessage(), e);
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
                UserModerationStatus status = moderationStatusRepository.findByUserId(userId)
                    .orElse(new UserModerationStatus());
                
                status.setUserId(userId);
                status.setStatus(ai.youthmind.entity.UserModerationStatus.ModerationStatus.WARNED);
                status.setWarningLevel(status.getWarningLevel() + 1);
                status.setLastWarningAt(LocalDateTime.now());
                status.setTotalWarnings(status.getTotalWarnings() + 1);
                status.setUpdatedAt(LocalDateTime.now());
                
                moderationStatusRepository.save(status);
                
                // Create warning history
                WarningHistory warning = new WarningHistory();
                warning.setUserId(userId);
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

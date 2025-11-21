package ai.youthmind.service;

import ai.youthmind.entity.Post;
import ai.youthmind.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Background job for backfilling emotion data for existing posts
 * Processes posts without emotion classifications and assigns emotions using AI
 * 
 * Features:
 * - Batch processing (100 posts at a time)
 * - Rate limiting to avoid overwhelming AI service
 * - Progress tracking and resumability
 * - Graceful error handling
 * - Detailed logging
 */
@Service
@Slf4j
public class BackfillPostEmotionsJob {
    
    private static final int BATCH_SIZE = 100;
    private static final int RATE_LIMIT_DELAY_MS = 1000; // 1 second between batches
    private static final String BACKFILL_SOURCE = "ai_classified_backfill";
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired(required = false)
    private EmotionClassificationService emotionClassificationService;
    
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    
    private volatile boolean isRunning = false;
    private volatile boolean shouldStop = false;
    
    /**
     * Execute the backfill job asynchronously
     * Processes all posts without emotion data
     * 
     * @return CompletableFuture with job statistics
     */
    @Async
    public CompletableFuture<BackfillResult> executeBackfillJob() {
        if (isRunning) {
            log.warn("Backfill job is already running. Skipping duplicate execution.");
            return CompletableFuture.completedFuture(BackfillResult.alreadyRunning());
        }
        
        if (emotionClassificationService == null) {
            log.error("EmotionClassificationService is not available. Cannot run backfill job.");
            return CompletableFuture.completedFuture(BackfillResult.serviceUnavailable());
        }
        
        LocalDateTime startTime = LocalDateTime.now();
        isRunning = true;
        shouldStop = false;
        
        // Reset counters
        processedCount.set(0);
        successCount.set(0);
        errorCount.set(0);
        totalProcessingTime.set(0);
        
        log.info("Starting emotion backfill job...");
        
        try {
            // Count total posts without emotion data
            long totalPosts = countPostsWithoutEmotionData();
            log.info("Found {} posts without emotion data to process", totalPosts);
            
            if (totalPosts == 0) {
                log.info("No posts require emotion backfilling. Job complete.");
                return CompletableFuture.completedFuture(BackfillResult.noPostsToProcess());
            }
            
            // Process in batches
            int batchNumber = 0;
            while (!shouldStop) {
                List<Post> batch = fetchNextBatch(batchNumber);
                
                if (batch.isEmpty()) {
                    log.info("No more posts to process. Backfill complete.");
                    break;
                }
                
                log.info("Processing batch {} ({} posts)", batchNumber + 1, batch.size());
                processBatch(batch);
                
                // Rate limiting: wait between batches
                if (!shouldStop) {
                    Thread.sleep(RATE_LIMIT_DELAY_MS);
                }
                
                batchNumber++;
                
                // Log progress every 10 batches
                if (batchNumber % 10 == 0) {
                    logProgress(totalPosts);
                }
            }
            
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            
            BackfillResult result = BackfillResult.completed(
                processedCount.get(),
                successCount.get(),
                errorCount.get(),
                duration
            );
            
            log.info("Emotion backfill job completed: {}", result);
            return CompletableFuture.completedFuture(result);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Backfill job was interrupted", e);
            return CompletableFuture.completedFuture(BackfillResult.interrupted());
        } catch (Exception e) {
            log.error("Backfill job failed with exception", e);
            return CompletableFuture.completedFuture(BackfillResult.failed(e.getMessage()));
        } finally {
            isRunning = false;
        }
    }
    
    /**
     * Stop the currently running backfill job
     */
    public void stopBackfillJob() {
        if (isRunning) {
            log.info("Stopping backfill job...");
            shouldStop = true;
        } else {
            log.info("No backfill job is currently running");
        }
    }
    
    /**
     * Count posts without emotion data
     */
    private long countPostsWithoutEmotionData() {
        return postRepository.count();
    }
    
    /**
     * Fetch the next batch of posts to process
     */
    private List<Post> fetchNextBatch(int batchNumber) {
        Pageable pageable = PageRequest.of(batchNumber, BATCH_SIZE);
        
        // Find posts where emotion is null or emotion_source is not 'ai_classified_backfill'
        // This allows resumability - we skip posts we've already processed
        return postRepository.findAll(pageable).getContent().stream()
            .filter(post -> post.getEmotion() == null || 
                           (post.getEmotionSource() != null && 
                            !post.getEmotionSource().equals(BACKFILL_SOURCE) &&
                            !post.getEmotionSource().equals("ai_classified")))
            .limit(BATCH_SIZE)
            .toList();
    }
    
    /**
     * Process a batch of posts
     */
    @Transactional
    protected void processBatch(List<Post> posts) {
        for (Post post : posts) {
            if (shouldStop) {
                log.info("Stopping batch processing due to stop signal");
                break;
            }
            
            processPost(post);
        }
    }
    
    /**
     * Process a single post
     */
    private void processPost(Post post) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Classify emotion using AI service
            String content = post.getContent();
            if (content == null || content.trim().isEmpty()) {
                log.warn("Post {} has no content, skipping emotion classification", post.getId());
                errorCount.incrementAndGet();
                return;
            }
            
            // Call emotion classification service
            var classificationResult = emotionClassificationService.classifyEmotion(content);
            
            if (classificationResult != null && classificationResult.getEmotion() != null) {
                // Update post with emotion data
                post.setEmotion(classificationResult.getEmotion());
                post.setEmotionConfidence(classificationResult.getConfidence());
                post.setEmotionSource(BACKFILL_SOURCE);
                
                postRepository.save(post);
                
                successCount.incrementAndGet();
                log.debug("Successfully classified post {} as {} (confidence: {})", 
                    post.getId(), post.getEmotion(), post.getEmotionConfidence());
            } else {
                log.warn("Failed to classify emotion for post {}: null result", post.getId());
                errorCount.incrementAndGet();
            }
            
        } catch (Exception e) {
            log.error("Error processing post {}: {}", post.getId(), e.getMessage(), e);
            errorCount.incrementAndGet();
        } finally {
            processedCount.incrementAndGet();
            long processingTime = System.currentTimeMillis() - startTime;
            totalProcessingTime.addAndGet(processingTime);
        }
    }
    
    /**
     * Log progress
     */
    private void logProgress(long totalPosts) {
        int processed = processedCount.get();
        int success = successCount.get();
        int errors = errorCount.get();
        double percentComplete = (processed / (double) totalPosts) * 100;
        double avgProcessingTime = totalProcessingTime.get() / (double) processed;
        
        log.info("Progress: {}/{} posts ({:.1f}%) | Success: {} | Errors: {} | Avg time: {:.0f}ms",
            processed, totalPosts, percentComplete, success, errors, avgProcessingTime);
    }
    
    /**
     * Get current job status
     */
    public JobStatus getStatus() {
        return new JobStatus(
            isRunning,
            processedCount.get(),
            successCount.get(),
            errorCount.get(),
            totalProcessingTime.get()
        );
    }
    
    /**
     * Result of backfill job execution
     */
    public record BackfillResult(
        boolean success,
        String message,
        int processedCount,
        int successCount,
        int errorCount,
        Duration duration
    ) {
        public static BackfillResult completed(int processed, int success, int errors, Duration duration) {
            return new BackfillResult(true, "Completed successfully", processed, success, errors, duration);
        }
        
        public static BackfillResult alreadyRunning() {
            return new BackfillResult(false, "Job already running", 0, 0, 0, Duration.ZERO);
        }
        
        public static BackfillResult serviceUnavailable() {
            return new BackfillResult(false, "Emotion classification service unavailable", 0, 0, 0, Duration.ZERO);
        }
        
        public static BackfillResult noPostsToProcess() {
            return new BackfillResult(true, "No posts require processing", 0, 0, 0, Duration.ZERO);
        }
        
        public static BackfillResult interrupted() {
            return new BackfillResult(false, "Job was interrupted", 0, 0, 0, Duration.ZERO);
        }
        
        public static BackfillResult failed(String reason) {
            return new BackfillResult(false, "Job failed: " + reason, 0, 0, 0, Duration.ZERO);
        }
    }
    
    /**
     * Current status of backfill job
     */
    public record JobStatus(
        boolean isRunning,
        int processedCount,
        int successCount,
        int errorCount,
        long totalProcessingTimeMs
    ) {}
    
    /**
     * Placeholder for emotion classification result
     * Replace with actual service response class
     */
    public static class EmotionClassificationResult {
        private String emotion;
        private Double confidence;
        
        public String getEmotion() {
            return emotion;
        }
        
        public void setEmotion(String emotion) {
            this.emotion = emotion;
        }
        
        public Double getConfidence() {
            return confidence;
        }
        
        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }
    }
    
    /**
     * Placeholder for emotion classification service
     * Replace with actual service implementation
     */
    public interface EmotionClassificationService {
        EmotionClassificationResult classifyEmotion(String content);
    }
}


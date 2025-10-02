package ai.youthmind.service;

import ai.youthmind.dto.BatchRequest;
import ai.youthmind.dto.BatchResponse;
import ai.youthmind.dto.PostDto;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.util.PerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling batch API requests with high performance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchProcessingService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final AsyncModerationService asyncModerationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Executor batchProcessingExecutor;

    // Cache for batch processing status
    private final Map<String, BatchResponse<?>> batchStatusCache = new ConcurrentHashMap<>();

    /**
     * Process batch request asynchronously
     */
    @Async("batchProcessingExecutor")
    public <T> CompletableFuture<BatchResponse<T>> processBatchAsync(BatchRequest<T> request) {
        return CompletableFuture.supplyAsync(() -> {
            String batchId = request.getBatchId();
            LocalDateTime startTime = LocalDateTime.now();
            
            try {
                PerformanceMonitor.start("batch_processing");
                
                // Initialize response
                BatchResponse.BatchResponseBuilder<T> responseBuilder = BatchResponse.<T>builder()
                    .batchId(batchId)
                    .status(BatchResponse.BatchStatus.PROCESSING)
                    .totalOperations(request.getOperations().size())
                    .startTime(startTime)
                    .results(new ArrayList<>());

                // Cache initial status
                batchStatusCache.put(batchId, responseBuilder.build());

                // Process operations
                List<BatchResponse.BatchOperationResult<T>> results = new ArrayList<>();
                int successfulCount = 0;
                int failedCount = 0;

                // Check if this is a batch of posts that can use batch moderation
                boolean isPostBatch = request.getOperations().stream()
                    .allMatch(op -> "CREATE_POST".equals(op.getOperationType()));
                
                if (isPostBatch && request.getOperations().size() > 1) {
                    // Use batch moderation for multiple posts
                    log.info("Using batch moderation for {} posts", request.getOperations().size());
                    results = processPostBatchWithModeration(request);
                    successfulCount = (int) results.stream().filter(BatchResponse.BatchOperationResult::isSuccess).count();
                    failedCount = results.size() - successfulCount;
                } else {
                    // Process operations individually
                    for (BatchRequest.BatchOperation<T> operation : request.getOperations()) {
                        try {
                            BatchResponse.BatchOperationResult<T> result = processOperation(operation);
                            results.add(result);
                            
                            if (result.isSuccess()) {
                                successfulCount++;
                            } else {
                                failedCount++;
                                if (request.isFailFast()) {
                                    break; // Stop on first failure if failFast is true
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error processing operation {}: {}", operation.getOperationId(), e.getMessage());
                            results.add(BatchResponse.BatchOperationResult.<T>builder()
                                .operationId(operation.getOperationId())
                                .success(false)
                                .errorMessage(e.getMessage())
                                .errorCode("PROCESSING_ERROR")
                                .build());
                            failedCount++;
                            
                            if (request.isFailFast()) {
                                break;
                            }
                        }
                    }
                }

                // Build final response
                LocalDateTime endTime = LocalDateTime.now();
                BatchResponse<T> response = responseBuilder
                    .status(determineBatchStatus(successfulCount, failedCount, request.getOperations().size()))
                    .successfulOperations(successfulCount)
                    .failedOperations(failedCount)
                    .endTime(endTime)
                    .processingTimeMs(java.time.Duration.between(startTime, endTime).toMillis())
                    .results(results)
                    .build();

                // Update cache
                batchStatusCache.put(batchId, response);
                
                // Cache result for 1 hour
                redisTemplate.opsForValue().set("batch:result:" + batchId, response, 3600, TimeUnit.SECONDS);
                
                PerformanceMonitor.end("batch_processing");
                log.info("Batch processing completed for batch {}: {} successful, {} failed in {}ms", 
                    batchId, successfulCount, failedCount, response.getProcessingTimeMs());

                return response;

            } catch (Exception e) {
                log.error("Error in batch processing for batch {}: {}", batchId, e.getMessage(), e);
                
                BatchResponse<T> errorResponse = BatchResponse.<T>builder()
                    .batchId(batchId)
                    .status(BatchResponse.BatchStatus.FAILED)
                    .totalOperations(request.getOperations().size())
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .processingTimeMs(java.time.Duration.between(startTime, LocalDateTime.now()).toMillis())
                    .results(new ArrayList<>())
                    .build();
                
                batchStatusCache.put(batchId, errorResponse);
                return errorResponse;
            }
        }, batchProcessingExecutor);
    }

    /**
     * Process individual operation
     */
    private <T> BatchResponse.BatchOperationResult<T> processOperation(BatchRequest.BatchOperation<T> operation) {
        long operationStartTime = System.currentTimeMillis();
        
        try {
            switch (operation.getOperationType().toUpperCase()) {
                case "CREATE_POST":
                    return processCreatePost(operation);
                case "UPDATE_POST":
                    return processUpdatePost(operation);
                case "DELETE_POST":
                    return processDeletePost(operation);
                case "GET_POST":
                    return processGetPost(operation);
                case "BATCH_MODERATION":
                    return processBatchModeration(operation);
                default:
                    throw new IllegalArgumentException("Unsupported operation type: " + operation.getOperationType());
            }
        } catch (Exception e) {
            return BatchResponse.BatchOperationResult.<T>builder()
                .operationId(operation.getOperationId())
                .success(false)
                .errorMessage(e.getMessage())
                .errorCode("OPERATION_ERROR")
                .processingTimeMs(System.currentTimeMillis() - operationStartTime)
                .build();
        }
    }

    /**
     * Process a batch of posts with batch moderation
     */
    @SuppressWarnings("unchecked")
    private <T> List<BatchResponse.BatchOperationResult<T>> processPostBatchWithModeration(BatchRequest<T> request) {
        List<BatchResponse.BatchOperationResult<T>> results = new ArrayList<>();
        List<Post> createdPosts = new ArrayList<>();
        
        try {
            // First, create all posts without moderation
            for (BatchRequest.BatchOperation<T> operation : request.getOperations()) {
                try {
                    PostDto postDto = convertOperationToPostDto(operation);
                    Post post = convertToPost(postDto);
                    Post savedPost = postRepository.save(post);
                    createdPosts.add(savedPost);
                    
                    results.add(BatchResponse.BatchOperationResult.<T>builder()
                        .operationId(operation.getOperationId())
                        .success(true)
                        .data((T) convertToPostDto(savedPost))
                        .build());
                } catch (Exception e) {
                    log.error("Failed to create post in batch: {}", e.getMessage(), e);
                    results.add(BatchResponse.BatchOperationResult.<T>builder()
                        .operationId(operation.getOperationId())
                        .success(false)
                        .errorMessage(e.getMessage())
                        .errorCode("POST_CREATION_ERROR")
                        .build());
                }
            }
            
            // Now run batch moderation on all created posts
            if (!createdPosts.isEmpty()) {
                List<UUID> postIds = createdPosts.stream()
                    .map(Post::getId)
                    .collect(Collectors.toList());
                
                log.info("Running batch moderation for {} posts", postIds.size());
                asyncModerationService.processBatchModerationAsync(postIds);
            }
            
        } catch (Exception e) {
            log.error("Error in batch post processing: {}", e.getMessage(), e);
            // Mark all operations as failed
            for (int i = 0; i < request.getOperations().size(); i++) {
                if (i >= results.size()) {
                    results.add(BatchResponse.BatchOperationResult.<T>builder()
                        .operationId(request.getOperations().get(i).getOperationId())
                        .success(false)
                        .errorMessage("Batch processing failed: " + e.getMessage())
                        .errorCode("BATCH_PROCESSING_ERROR")
                        .build());
                }
            }
        }
        
        return results;
    }

    /**
     * Convert operation to PostDto
     */
    @SuppressWarnings("unchecked")
    private PostDto convertOperationToPostDto(BatchRequest.BatchOperation<?> operation) {
        if (operation.getData() instanceof PostDto) {
            return (PostDto) operation.getData();
        } else if (operation.getData() instanceof java.util.Map) {
            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) operation.getData();
            return convertMapToPostDto(dataMap);
        } else {
            throw new IllegalArgumentException("Invalid data type for CREATE_POST operation: " + operation.getData().getClass());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> BatchResponse.BatchOperationResult<T> processCreatePost(BatchRequest.BatchOperation<T> operation) {
        try {
            // Handle the case where data might be a LinkedHashMap instead of PostDto
            PostDto postDto;
            if (operation.getData() instanceof PostDto) {
                postDto = (PostDto) operation.getData();
            } else if (operation.getData() instanceof java.util.Map) {
                // Convert LinkedHashMap to PostDto
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) operation.getData();
                postDto = convertMapToPostDto(dataMap);
            } else {
                throw new IllegalArgumentException("Invalid data type for CREATE_POST operation: " + operation.getData().getClass());
            }
            
            log.debug("Creating post with data: userId={}, title={}, content={}", 
                postDto.getUserId(), postDto.getTitle(), postDto.getContent());
            
            Post post = convertToPost(postDto);
            log.debug("Converted post: id={}, userId={}, title={}, content={}", 
                post.getId(), post.getUserId(), post.getTitle(), post.getContent());
            
            Post savedPost = postRepository.save(post);
            log.debug("Saved post with ID: {}", savedPost.getId());
            
            // Trigger async moderation
            asyncModerationService.processModerationAsync(savedPost.getId(), savedPost.getContent(), savedPost.getUserId());
            
            return BatchResponse.BatchOperationResult.<T>builder()
                .operationId(operation.getOperationId())
                .success(true)
                .data((T) convertToPostDto(savedPost))
                .build();
        } catch (Exception e) {
            log.error("Failed to create post: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create post: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> BatchResponse.BatchOperationResult<T> processUpdatePost(BatchRequest.BatchOperation<T> operation) {
        try {
            Long postId = Long.valueOf(operation.getResourceId());
            UUID uuidPostId = UUID.fromString(postId.toString());
            PostDto postDto = (PostDto) operation.getData();
            
            Post existingPost = postRepository.findById(uuidPostId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
            
            updatePostFromDto(existingPost, postDto);
            Post savedPost = postRepository.save(existingPost);
            
            return BatchResponse.BatchOperationResult.<T>builder()
                .operationId(operation.getOperationId())
                .success(true)
                .data((T) convertToPostDto(savedPost))
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update post: " + e.getMessage(), e);
        }
    }

    private <T> BatchResponse.BatchOperationResult<T> processDeletePost(BatchRequest.BatchOperation<T> operation) {
        try {
            Long postId = Long.valueOf(operation.getResourceId());
            UUID uuidPostId = UUID.fromString(postId.toString());
            postRepository.deleteById(uuidPostId);
            
            return BatchResponse.BatchOperationResult.<T>builder()
                .operationId(operation.getOperationId())
                .success(true)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete post: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> BatchResponse.BatchOperationResult<T> processGetPost(BatchRequest.BatchOperation<T> operation) {
        try {
            Long postId = Long.valueOf(operation.getResourceId());
            UUID uuidPostId = UUID.fromString(postId.toString());
            Post post = postRepository.findById(uuidPostId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));
            
            return BatchResponse.BatchOperationResult.<T>builder()
                .operationId(operation.getOperationId())
                .success(true)
                .data((T) convertToPostDto(post))
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get post: " + e.getMessage(), e);
        }
    }

    private <T> BatchResponse.BatchOperationResult<T> processBatchModeration(BatchRequest.BatchOperation<T> operation) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> postIds = (List<Long>) operation.getData();
            List<UUID> uuidPostIds = postIds.stream()
                .map(id -> java.util.UUID.fromString(id.toString()))
                .collect(java.util.stream.Collectors.toList());
            asyncModerationService.processBatchModerationAsync(uuidPostIds);
            
            return BatchResponse.BatchOperationResult.<T>builder()
                .operationId(operation.getOperationId())
                .success(true)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to process batch moderation: " + e.getMessage(), e);
        }
    }

    /**
     * Get batch processing status
     */
    @SuppressWarnings("unchecked")
    public <T> BatchResponse<T> getBatchStatus(String batchId) {
        // Check cache first
        BatchResponse<T> cached = (BatchResponse<T>) batchStatusCache.get(batchId);
        if (cached != null) {
            return cached;
        }
        
        // Check Redis cache
        return (BatchResponse<T>) redisTemplate.opsForValue().get("batch:result:" + batchId);
    }

    /**
     * Get all active batch processing statuses
     */
    public Map<String, BatchResponse<?>> getAllBatchStatuses() {
        return new HashMap<>(batchStatusCache);
    }

    private BatchResponse.BatchStatus determineBatchStatus(int successful, int failed, int total) {
        if (failed == 0) {
            return BatchResponse.BatchStatus.COMPLETED;
        } else if (successful == 0) {
            return BatchResponse.BatchStatus.FAILED;
        } else {
            return BatchResponse.BatchStatus.PARTIALLY_COMPLETED;
        }
    }

    // Helper methods for conversion
    private PostDto convertMapToPostDto(java.util.Map<String, Object> dataMap) {
        return PostDto.builder()
            .id(dataMap.get("id") != null ? UUID.fromString(dataMap.get("id").toString()) : null)
            .userId(dataMap.get("userId") != null ? UUID.fromString(dataMap.get("userId").toString()) : null)
            .title((String) dataMap.get("title"))
            .content((String) dataMap.get("content"))
            .emotion((String) dataMap.get("emotion"))
            .preImg((String) dataMap.get("preImg"))
            .topics((String) dataMap.get("topics"))
            .resonance(dataMap.get("resonance") != null ? (Integer) dataMap.get("resonance") : 0)
            .view(dataMap.get("view") != null ? (Integer) dataMap.get("view") : 0)
            .collect(dataMap.get("collect") != null ? (Integer) dataMap.get("collect") : 0)
            .isresonanced(dataMap.get("isresonanced") != null ? (Boolean) dataMap.get("isresonanced") : false)
            .isCollected(dataMap.get("isCollected") != null ? (Boolean) dataMap.get("isCollected") : false)
            .flagged(dataMap.get("flagged") != null ? (Boolean) dataMap.get("flagged") : false)
            .flagReason((String) dataMap.get("flagReason"))
            .flaggedAt(dataMap.get("flaggedAt") != null ? 
                java.time.LocalDateTime.parse(dataMap.get("flaggedAt").toString()) : null)
            .build();
    }

    private Post convertToPost(PostDto dto) {
        // Validate required fields
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Content is required");
        }
        
        Post post = new Post();
        post.setUserId(dto.getUserId());
        post.setTitle(dto.getTitle().trim());
        post.setContent(dto.getContent().trim());
        post.setEmotion(dto.getEmotion());
        post.setPreImg(dto.getPreImg());
        post.setTopics(dto.getTopics());
        
        // Set required fields with default values
        post.setResonance(dto.getResonance());
        post.setView(dto.getView());
        post.setCollect(dto.getCollect());
        post.setFlagged(dto.isFlagged());
        post.setFlagReason(dto.getFlagReason());
        post.setFlaggedAt(dto.getFlaggedAt());
        
        // Set timestamps - @PrePersist will handle this, but set explicitly for safety
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        
        return post;
    }

    private PostDto convertToPostDto(Post post) {
        return PostDto.builder()
            .id(post.getId())
            .userId(post.getUserId())
            .title(post.getTitle())
            .content(post.getContent())
            .emotion(post.getEmotion())
            .preImg(post.getPreImg())
            .resonance(post.getResonance())
            .view(post.getView())
            .collect(post.getCollect())
            .topics(post.getTopics())
            .date(post.getCreatedAt())
            .flaggedAt(post.getFlaggedAt())
            .build();
    }

    private void updatePostFromDto(Post post, PostDto dto) {
        if (dto.getTitle() != null) post.setTitle(dto.getTitle());
        if (dto.getContent() != null) post.setContent(dto.getContent());
        if (dto.getEmotion() != null) post.setEmotion(dto.getEmotion());
        if (dto.getPreImg() != null) post.setPreImg(dto.getPreImg());
        if (dto.getTopics() != null) post.setTopics(dto.getTopics());
    }
}

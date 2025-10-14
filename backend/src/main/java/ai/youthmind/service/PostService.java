package ai.youthmind.service;

import ai.youthmind.dto.EmotionClassificationResult;
import ai.youthmind.dto.ModerationResult;
import ai.youthmind.dto.PostCreateRequest;
import ai.youthmind.dto.PostDto;
import ai.youthmind.dto.PostDtoBuilder;
import ai.youthmind.dto.FeedResponse;
import ai.youthmind.dto.FeedBlockResponse;
import ai.youthmind.dto.BlockMetadata;
import ai.youthmind.dto.FlaggedPostDto;
import ai.youthmind.dto.PostViewStats;
import ai.youthmind.dto.PostUpdateRequest;
import ai.youthmind.dto.PsychologicalAnalysisRequest;
import ai.youthmind.dto.PsychologicalAnalysisResponse;
import ai.youthmind.util.ModerationUtils;
import ai.youthmind.entity.AIAnalysis;
import ai.youthmind.entity.Post;
import ai.youthmind.events.model.Emotion;
import ai.youthmind.entity.PostCollection;
import ai.youthmind.entity.PostResonance;
import ai.youthmind.entity.PostView;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.repository.AIAnalysisRepository;
import ai.youthmind.repository.CommentRepository;
import ai.youthmind.repository.PostCollectionRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.PostResonanceRepository;
import ai.youthmind.repository.PostViewRepository;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.service.RedisService;
import ai.youthmind.entity.User;
import ai.youthmind.entity.FeedBlockCache;
import ai.youthmind.util.PerformanceMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class PostService {
    
    private static final Logger logger = LoggerFactory.getLogger(PostService.class);
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private AIAnalysisRepository aiAnalysisRepository;
    
    @Autowired
    private PostViewRepository postViewRepository;
    
    @Autowired
    private PostResonanceRepository postResonanceRepository;
    
    @Autowired
    private PostCollectionRepository postCollectionRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    //@Autowired
    //private RedisService redisService;
    
    @Autowired
    private PsychologicalStateCacheService psychologicalStateCacheService;
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    @Autowired
    private PsychologicalVectorGenerator psychologicalVectorGenerator;
    
    @Autowired
    private UserRepository userRepository;
    
    
    /**
     * Update or insert AI analysis result for a post
     */
    public AIAnalysis updateOrInsertAnalysisResult(UUID postId, 
                                                 BigDecimal sentimentScore, 
                                                 BigDecimal empathyScore, 
                                                 List<String> generatedQuestions, 
                                                 Integer processingTimeMs) {
        
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found with id: " + postId);
        }
        
        Post post = postOpt.get();
        
        // Check if analysis already exists
        Optional<AIAnalysis> existingAnalysis = aiAnalysisRepository.findById(postId);
        
        AIAnalysis analysis;
        if (existingAnalysis.isPresent()) {
            // Update existing analysis
            analysis = existingAnalysis.get();
            analysis.setSentimentScore(sentimentScore);
            analysis.setEmpathyScore(empathyScore);
            analysis.setGeneratedQuestions(generatedQuestions);
            analysis.setProcessingTimeMs(processingTimeMs);
        } else {
            // Create new analysis
            analysis = new AIAnalysis();
            analysis.setPost(post);
            analysis.setSentimentScore(sentimentScore);
            analysis.setEmpathyScore(empathyScore);
            analysis.setGeneratedQuestions(generatedQuestions);
            analysis.setProcessingTimeMs(processingTimeMs);
        }
        
        return aiAnalysisRepository.save(analysis);
    }

    @Autowired
    private PostModerationService postModerationService;
    
    @Autowired
    private ModerationLoggingService moderationLoggingService;
    
    
    @Autowired
    private AIEmotionClassificationService aiEmotionClassificationService;
    
    public Post createPost(UUID userId, PostCreateRequest request) {
        logger.info("========================================");
        logger.info("POST CREATION STARTED");
        logger.info("User ID: {}", userId);
        logger.info("Post title: {}", request.getTitle());
        logger.info("Post content length: {} characters", request.getContent().length());
        logger.info("Post emotion: {}", request.getEmotion());
        logger.info("========================================");
        
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setEmotion(request.getEmotion());
        post.setPreImg(request.getPreImg());
        post.setResonance(0); // New post starts with 0 resonance
        post.setView(0); // New post starts with 0 views
        post.setCollect(0); // New post starts with 0 collections
        
        // AI Moderation Integration - Pre-publish content screening
        try {
            // Check if user should be exempt from moderation
            if (!postModerationService.isUserExemptFromModeration(userId)) {
                // Perform AI moderation
                ModerationResult moderationResult = postModerationService.moderatePost(post, userId);
                
                // Log moderation result
                moderationLoggingService.logPostModeration(post, userId, moderationResult, "post_creation");
                
                // Apply moderation actions (flagging, warnings, etc.)
                if (!moderationResult.isSafe()) {
                    // Content flagged - post will be saved but flagged
                    post.setFlagged(true);
                    post.setFlagReason(moderationResult.getReason());
                    post.setFlaggedAt(LocalDateTime.now());
                    
                    // Log warning issuance if applicable
                    if (ModerationUtils.shouldIssueWarning(moderationResult)) {
                        moderationLoggingService.logWarningIssuance(
                            userId, 
                            post.getId(), 
                            ModerationUtils.mapCategoryToWarningTypeString(ModerationUtils.getPrimaryCategory(moderationResult)),
                            ModerationUtils.getSeverityAsString(moderationResult),
                            "AI-detected harmful content: " + moderationResult.getReason()
                        );
                    }
                }
            } else {
                // User is exempt from moderation
                post.setFlagged(false);
                post.setFlagReason(null);
                post.setFlaggedAt(null);
            }
        } catch (Exception e) {
            // Log moderation error but don't block post creation
            moderationLoggingService.logModerationError(userId, null, e, "post_creation_moderation");
            
            // Default to safe on error to avoid blocking legitimate content
            post.setFlagged(false);
            post.setFlagReason(null);
            post.setFlaggedAt(null);
        }
        
        Post savedPost = postRepository.save(post);
        logger.info("✓ Post saved to database with ID: {}", savedPost.getId());
        
        // AI Emotion Classification
        try {
            String userEmotion = request.getEmotion();
            boolean userProvidedEmotion = userEmotion != null && !userEmotion.trim().isEmpty();
            
            if (userProvidedEmotion) {
                // User provided emotion - set emotionSource to "user"
                savedPost.setEmotionSource("user");
                logger.info("→ User provided emotion: {} - setting emotionSource to 'user'", userEmotion);
            } else {
                // No user emotion - classify with AI
                logger.info("→ No user emotion provided - calling AI emotion classification");
                EmotionClassificationResult aiResult = aiEmotionClassificationService.classifyEmotion(savedPost.getContent());
                
                if (aiResult != null && aiResult.isValid()) {
                    // Set AI classification results
                    savedPost.setAiEmotion(aiResult.getEmotion());
                    savedPost.setEmotionConfidence(aiResult.getConfidence());
                    savedPost.setEmotionSource(aiResult.getSource());
                    
                    // Also set the main emotion field for R vector updates
                    savedPost.setEmotion(aiResult.getEmotion());
                    
                    logger.info("✓ AI emotion classification completed: {} (confidence: {})", 
                               aiResult.getEmotion(), aiResult.getConfidence());
                } else {
                    // AI classification failed - set fallback values
                    savedPost.setEmotionSource("ai_fallback");
                    savedPost.setEmotion("neutral");
                    savedPost.setAiEmotion("neutral");
                    savedPost.setEmotionConfidence(0.0);
                    
                    logger.warn("✗ AI emotion classification failed - using fallback: neutral");
                }
            }
            
            // Save the post again with AI classification data
            savedPost = postRepository.save(savedPost);
            logger.info("✓ Post updated with emotion classification data");
            
        } catch (Exception e) {
            // Log error but don't fail post creation
            logger.error("✗ Failed to perform AI emotion classification for post: {} - {}", 
                        savedPost.getId(), e.getMessage(), e);
            
            // Set fallback values to ensure post has valid emotion data
            if (savedPost.getEmotion() == null || savedPost.getEmotion().trim().isEmpty()) {
                savedPost.setEmotion("neutral");
                savedPost.setEmotionSource("ai_fallback");
                savedPost.setAiEmotion("neutral");
                savedPost.setEmotionConfidence(0.0);
                savedPost = postRepository.save(savedPost);
                logger.info("✓ Post updated with fallback emotion data");
            }
        }
        
        // Update R vector based on post emotion
        try {
            String emotion = savedPost.getEmotion();
            if (emotion != null && !emotion.trim().isEmpty()) {
                logger.info("→ R vector service removed - skipping R vector update for user: {} with emotion: {}", userId, emotion);
                // rVectorService.updateOnPost(userId, savedPost.getId(), Emotion.valueOf(emotion.toUpperCase()));
                logger.info("✓ R vector update skipped - service removed");
            } else {
                logger.warn("→ Skipping R vector update - post emotion is null or empty");
            }
        } catch (Exception e) {
            // Log error but don't fail post creation
            logger.error("✗ Failed to update R vector for user: {} with emotion: {} - {}", 
                        userId, savedPost.getEmotion(), e.getMessage(), e);
        }
        
        // Trigger psychological analysis for the post content
        logger.info("→ Starting psychological analysis for post: {}", savedPost.getId());
        try {
            // Perform psychological analysis using our new RAG DeepSeek system
            logger.info("→ Calling PsychologicalVectorGenerator.generateVector() (RAG DeepSeek system)...");
            long analysisStartTime = System.currentTimeMillis();
            Map<String, Object> vectorResult = psychologicalVectorGenerator.generateVector(savedPost.getContent(), userId);
            long analysisDuration = System.currentTimeMillis() - analysisStartTime;
            logger.info("✓ RAG DeepSeek vector generation completed in {}ms", analysisDuration);
            
            // Store psychological analysis results in post metadata
            String aiMetadataJson = savedPost.getAiMetadata();
            Map<String, Object> aiMetadata = new HashMap<>();
            if (aiMetadataJson != null && !aiMetadataJson.isEmpty()) {
                try {
                    // Parse existing JSON metadata
                    ObjectMapper mapper = new ObjectMapper();
                    aiMetadata = mapper.readValue(aiMetadataJson, Map.class);
                } catch (Exception e) {
                    // If parsing fails, start with empty map
                    aiMetadata = new HashMap<>();
                }
            }
            
            // Process psychological analysis results from RAG DeepSeek system
            logger.info("→ Processing RAG DeepSeek vector results...");
            
            // Extract vector from result
            @SuppressWarnings("unchecked")
            List<Double> psychologicalVector = (List<Double>) vectorResult.get("psychologicalVector");
            String generationMethod = (String) vectorResult.get("generationMethod");
            Boolean isValid = (Boolean) vectorResult.get("isValid");
            
            if (psychologicalVector != null) {
                logger.info("  ✓ Psychological vector received: {} dimensions", psychologicalVector.size());
                logger.info("  ✓ Generation method: {}", generationMethod);
                logger.info("  ✓ Vector valid: {}", isValid);
                
                // Store in metadata
                aiMetadata.put("psychological_vector", psychologicalVector);
                aiMetadata.put("generation_method", generationMethod);
                aiMetadata.put("vector_valid", isValid);
                
                // Calculate confidence score (mean of non-zero values)
                double confidence = psychologicalVector.stream()
                    .filter(s -> s > 0.0)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
                aiMetadata.put("psychological_confidence", confidence);
                
            } else {
                logger.warn("  ✗ No psychological vector in RAG result");
            }
            
            aiMetadata.put("analysis_timestamp", LocalDateTime.now());
            aiMetadata.put("analysis_version", "1.0");
            
            // Convert metadata back to JSON string
            ObjectMapper mapper2 = new ObjectMapper();
            String aiMetadataJsonFinal = mapper2.writeValueAsString(aiMetadata);
            
            // Update post with psychological metadata
            logger.info("→ Updating post with psychological metadata...");
            savedPost.setAiMetadata(aiMetadataJsonFinal);
            postRepository.save(savedPost);
            logger.info("✓ Post updated with psychological metadata");
            
            // Update user's psychological state based on post content
            logger.info("→ Warming up psychological state cache for user: {}", userId);
            psychologicalStateCacheService.warmUpCache(userId);
            logger.info("✓ Psychological state cache warmed up");
            
        } catch (Exception e) {
            // Log error but don't fail post creation
            logger.error("✗ Failed to perform psychological analysis for post {}: {}", 
                savedPost.getId(), e.getMessage(), e);
        }
        
        // Invalidate feed cache after creating a post
        // // redisService.invalidateFeedCache();
        
        logger.info("========================================");
        logger.info("POST CREATION COMPLETED");
        logger.info("Post ID: {}", savedPost.getId());
        logger.info("========================================");
        
        return savedPost;
    }
    
    
    /**
     * Get feed with recommendation engine toggle support
     * Routes to personalized feed if enabled, chronological feed if disabled
     */
    public FeedResponse getFeed(int page, int size, String sortBy, String emotion, 
                               LocalDateTime startDate, LocalDateTime endDate, UUID userId) {
        PerformanceMonitor.start("PostService.getFeed");
        try {
            // Get user preferences
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
            
            boolean useRecommendations = user.getRecommendationEngineEnabled();
            logger.info("Feed request for user {}: recommendations={}, page={}, size={}", 
                       userId, useRecommendations, page, size);
            
            if (useRecommendations) {
                // Route to personalized recommendation feed
                logger.debug("Using personalized feed for user {}", userId);
                return getPersonalizedFeed(page, size, userId);
            } else {
                // Route to chronological feed
                logger.debug("Using chronological feed for user {}", userId);
                return getChronologicalFeed(page, size, sortBy, emotion, startDate, endDate, userId);
            }
        } finally {
            PerformanceMonitor.end("PostService.getFeed");
        }
    }
    
    /**
     * Get personalized feed using recommendation engine
     * Falls back to chronological feed if personalized generation fails
     */
    private FeedResponse getPersonalizedFeed(int page, int size, UUID userId) {
        try {
            logger.debug("Generating personalized feed for user {} page {}", userId, page);
            
            // Get personalized feed block
            // FeedBlockCache block = feedBlockPreCalculationService.getOrGenerateBlock(userId, page + 1);
            FeedBlockCache block = null; // Service removed
            
            // Convert block posts to DTOs
            List<PostDto> postDtos = convertBlockPostsToDtos(block.getPostIds(), userId);
            
            // Build response
            FeedResponse feedResponse = new FeedResponse();
            feedResponse.setPosts(postDtos);
            feedResponse.setCurrentPage(page);
            feedResponse.setTotalPages(1); // Feed blocks are self-contained
            feedResponse.setTotalElements(postDtos.size());
            feedResponse.setHasNext(false); // Each block is complete
            feedResponse.setPageSize(size);
            feedResponse.setFirst(page == 0);
            feedResponse.setLast(true);
            
            logger.info("Successfully generated personalized feed for user {}: {} posts", userId, postDtos.size());
            return feedResponse;
            
        } catch (Exception e) {
            logger.warn("Failed to generate personalized feed for user {}, falling back to chronological feed: {}", 
                       userId, e.getMessage(), e);
            
            // Fallback to chronological feed
            return getChronologicalFeed(page, size, "newest", null, null, null, userId);
        }
    }
    
    /**
     * Get chronological feed with filtering and sorting
     * This is the fallback/default behavior for when recommendations are disabled
     */
    private FeedResponse getChronologicalFeed(int page, int size, String sortBy, String emotion, 
                                            LocalDateTime startDate, LocalDateTime endDate, UUID userId) {
        try {
            // Try to get from cache first
            String cacheKey = String.format("feed:page:%d:size:%d:sort:%s:emotion:%s:start:%s:end:%s:user:%s",
                page, size, sortBy, emotion, startDate, endDate, userId);
            
            FeedResponse cachedFeed = null; // redisService.getCachedFeed(cacheKey);
            if (cachedFeed != null) {
                return cachedFeed;
            }

            // Create pageable with sorting
            Pageable pageable = PageRequest.of(page, size, getSortFromString(sortBy));

            // Get posts based on filters
            Page<Post> postsPage;
            if ("popular".equals(sortBy)) {
                // Use popularity sorting with time decay
                postsPage = postRepository.findPostsByPopularity(emotion, pageable);
            } else if (emotion != null && startDate != null && endDate != null) {
                postsPage = postRepository.findByEmotionIgnoreCaseAndCreatedAtBetween(emotion, startDate, endDate, pageable);
            } else if (emotion != null) {
                postsPage = postRepository.findByEmotionIgnoreCase(emotion, pageable);
            } else if (startDate != null && endDate != null) {
                postsPage = postRepository.findByCreatedAtBetween(startDate, endDate, pageable);
            } else {
                postsPage = postRepository.findAll(pageable);
            }
            
            // Convert to DTOs with batch user engagement lookup
            List<PostDto> postDtos = convertPostsToDtosWithBatchEngagement(postsPage.getContent(), userId);
        
            // Build response
            FeedResponse feedResponse = new FeedResponse();
            feedResponse.setPosts(postDtos);
            feedResponse.setCurrentPage(page);
            feedResponse.setTotalPages(postsPage.getTotalPages());
            feedResponse.setTotalElements(postsPage.getTotalElements());
            feedResponse.setHasNext(postsPage.hasNext());
            feedResponse.setPageSize(postsPage.getSize());
            feedResponse.setFirst(postsPage.isFirst());
            feedResponse.setLast(postsPage.isLast());
            
            // Cache for 2 minutes - temporarily disabled to fix Redis connection issue
            // redisService.cacheFeed(cacheKey, feedResponse);
            
            logger.debug("Generated chronological feed for user {}: {} posts", userId, postDtos.size());
            return feedResponse;
            
        } catch (Exception e) {
            logger.error("Failed to generate chronological feed for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate chronological feed", e);
        }
    }
    
    /**
     * Convert feed block post IDs to PostDto list with engagement data
     * Maintains order from the postIds list
     */
    private List<PostDto> convertBlockPostsToDtos(List<UUID> postIds, UUID userId) {
        if (postIds == null || postIds.isEmpty()) {
            logger.warn("Empty post IDs list for user {}", userId);
            return List.of();
        }
        
        try {
            // Fetch posts by IDs in the order specified
            List<Post> posts = postRepository.findAllById(postIds);
            
            // Create a map for quick lookup
            Map<UUID, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, post -> post));
            
            // Convert to DTOs maintaining the original order
            List<PostDto> postDtos = postIds.stream()
                .map(postId -> {
                    Post post = postMap.get(postId);
                    if (post != null) {
                        return convertToPostDto(post, userId);
                    } else {
                        logger.warn("Post {} not found for user {}", postId, userId);
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
            
            logger.debug("Converted {} posts to DTOs for user {} (requested: {})", 
                        postDtos.size(), userId, postIds.size());
            
            return postDtos;
            
        } catch (Exception e) {
            logger.error("Failed to convert block posts to DTOs for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to convert block posts to DTOs", e);
        }
    }
    
    /**
     * Convert posts to DTOs with batch user engagement lookup for better performance
     */
    private List<PostDto> convertPostsToDtosWithBatchEngagement(List<Post> posts, UUID userId) {
        if (posts.isEmpty()) {
            return List.of();
        }
        
        // Batch lookup user engagement status
        List<UUID> postIds = posts.stream().map(Post::getId).toList();
        Set<UUID> resonatedPostIds = postResonanceRepository.findByUserIdAndPostIdIn(userId, postIds)
            .stream().map(PostResonance::getPostId).collect(Collectors.toSet());
        Set<UUID> collectedPostIds = postCollectionRepository.findByUserIdAndPostIdIn(userId, postIds)
            .stream().map(PostCollection::getPostId).collect(Collectors.toSet());
        
        // Convert to DTOs with engagement status
        return posts.stream()
            .map(post -> {
                PostDto dto = convertToPostDto(post, null); // Don't track view here
                dto.setLiked(resonatedPostIds.contains(post.getId()));
                dto.setBookmarked(collectedPostIds.contains(post.getId()));
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Get posts by emotion
     */
    public List<PostDto> getPostsByEmotion(String emotion, UUID currentUserId) {
        List<Post> posts = postRepository.findByEmotion(emotion);
        return posts.stream()
            .map(post -> convertToPostDto(post, currentUserId))
            .collect(Collectors.toList());
    }
    
    /**
     * Get posts by user
     */
    public List<PostDto> getPostsByUser(UUID userId, UUID currentUserId) {
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return posts.stream()
            .map(post -> convertToPostDto(post, currentUserId))
            .collect(Collectors.toList());
    }

    /**
     * Get liked-posts by user
     */
    public List<PostDto> getLikedPostsByUser(UUID userId) {
        List<PostResonance> resonances = postResonanceRepository.findByUserId(userId);
        return resonances.stream()
                .map(resonance -> getPostById(resonance.getPostId(), userId))
                .collect(Collectors.toList());
    }

    /**
     * Get bookmarked posts by user
     */
    public List<PostDto> getBookmarkedPostsByUser(UUID userId) {
        List<PostCollection> collections = postCollectionRepository.findByUserId(userId);
        return collections.stream()
                .map(collection -> getPostById(collection.getPostId(), userId))
                .collect(Collectors.toList());
    }

    /**
     * Get all posts that have been flagged for moderation
     */
    public List<FlaggedPostDto> getFlaggedPosts() {
        List<Post> flaggedPosts = postRepository.findByFlaggedTrue();
        return flaggedPosts.stream()
                .map(this::convertToFlaggedPostDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get post by ID for internal use (returns Optional<Post>)
     */
    public Optional<Post> getPostById(UUID postId) {
        return postRepository.findById(postId);
    }
    
    /**
     * Get post by ID with view tracking and user engagement status
     * Optimized with caching and batch operations
     */
    public PostDto getPostById(UUID postId, UUID userId) {
        // Try to get from cache first
        String cacheKey = "post:" + postId + ":user:" + userId;
        PostDto cachedPost = null; // (PostDto) // redisService.getCachedPost(postId.toString());
        if (cachedPost != null) {
            return cachedPost;
        }
        
        // Get post from database
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        
        // Track view asynchronously to avoid blocking the response
        trackViewAsync(postId, userId);
        
        // Convert to DTO
        PostDto postDto = convertToPostDto(post, userId);
        
        // Cache the result for 5 minutes
        // // redisService.cachePost(postId.toString(), postDto);
        
        return postDto;
    }
    
    /**
     * Asynchronously track post view to improve response time
     */
    private void trackViewAsync(UUID postId, UUID userId) {
        // Use a separate thread or async method to track view
        // This prevents blocking the main response
        try {
            if (!postViewRepository.existsByUserIdAndPostId(userId, postId)) {
                postViewRepository.save(new PostView(userId, postId));
                // Update view count in background
                postRepository.incrementViewCount(postId);
            }
        } catch (Exception e) {
            // Log error but don't fail the main request
            // In production, use proper logging framework
            System.err.println("Failed to track view: " + e.getMessage());
        }
    }

    /**
     * Toggle resonance (like/unlike) for a post
     * 
     * @param postId The post ID
     * @param userId The user ID
     * @return PostDto with updated resonance information
     */
    @Transactional
    public PostDto toggleResonance(UUID postId, UUID userId) {
        // Find the post
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        
        // Check if user has already resonanced this post
        boolean hasResonanced = postResonanceRepository.existsByUserIdAndPostId(userId, postId);
        
        if (hasResonanced) {
            // User already resonanced, so remove the resonance (unlike)
            postResonanceRepository.deleteByUserIdAndPostId(userId, postId);
            
            // Decrement resonance count
            post.setResonance(post.getResonance() - 1);
        } else {
            // User hasn't resonanced, so add the resonance (like)
            PostResonance resonance = new PostResonance(userId, postId);
            postResonanceRepository.save(resonance);
            
            // Increment resonance count
            post.setResonance(post.getResonance() + 1);
            
            // Update R vector based on post emotion (only on like, not unlike)
            try {
                String emotion = post.getEmotion();
                if (emotion != null && !emotion.trim().isEmpty()) {
                    logger.info("→ Updating R vector for user: {} with emotion: {} (like)", userId, emotion);
                    // rVectorService.updateOnLike(userId, post.getId(), Emotion.valueOf(emotion.toUpperCase()));
                    logger.info("R vector service removed - skipping like update");
                    logger.info("✓ R vector updated successfully - emotion: {} (like)", emotion);
                } else {
                    logger.warn("→ Skipping R vector update - post emotion is null or empty (like)");
                }
            } catch (Exception e) {
                // Log error but don't fail the like operation
                logger.error("✗ Failed to update R vector for user: {} with emotion: {} (like) - {}", 
                            userId, post.getEmotion(), e.getMessage(), e);
            }
        }
        
        // Save the updated post
        postRepository.save(post);
        
        // Invalidate cache
        // redisService.invalidateFeedCache();
        
        // Return updated post DTO
        return convertToPostDto(post, userId);
    }

    /**
     * Toggle collection (save/unsave) for a post
     * 
     * @param postId The post ID
     * @param userId The user ID
     * @return PostDto with updated collection information
     */
    @Transactional
    public PostDto toggleCollection(UUID postId, UUID userId) {
        // Find the post
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        
        // Check if user has already collected this post
        boolean hasCollected = postCollectionRepository.existsByUserIdAndPostId(userId, postId);
        
        if (hasCollected) {
            // User already collected, so remove the collection (unsave)
            postCollectionRepository.deleteByUserIdAndPostId(userId, postId);
            
            // Decrement collection count
            post.setCollect(post.getCollect() - 1);
        } else {
            // User hasn't collected, so add the collection (save)
            PostCollection collection = new PostCollection(userId, postId);
            postCollectionRepository.save(collection);
            
            // Increment collection count
            post.setCollect(post.getCollect() + 1);
        }
        
        // Save the updated post
        postRepository.save(post);
        
        // Invalidate cache
        // redisService.invalidateFeedCache();
        
        // Return updated post DTO
        return convertToPostDto(post, userId);
    }
    
    /**
     * Track a post view for a specific user
     * Increments view count only for first-time views per user
     * 
     * @param postId The post ID
     * @param userId The user ID
     * @return PostDto with updated view information
     */
    @Transactional
    public PostDto trackPostView(UUID postId, UUID userId) {
        // Find the post
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        
        // Check if current user has already viewed this post
        boolean hasUserViewed = postViewRepository.existsByUserIdAndPostId(userId, postId);
        
        // If first time viewing, record the view
        if (!hasUserViewed) {
            PostView newView = new PostView(userId, postId);
            postViewRepository.save(newView);
            
            // Update post view count
            post.setView(post.getView() + 1);
            postRepository.save(post);
            
            // Invalidate cache if needed
            // redisService.invalidateFeedCache();
        }
        
        // Convert to DTO and return
        return convertToPostDto(post, userId);
    }
    
    /**
     * Track post view with duration and update R vector if duration >= 15 seconds
     * 
     * @param postId The post ID
     * @param userId The user ID
     * @param durationSeconds Duration in seconds the user viewed the post
     * @return PostDto with updated view information
     */
    @Transactional
    public PostDto trackPostViewWithDuration(UUID postId, UUID userId, Integer durationSeconds) {
        // Use existing trackPostView logic
        PostDto postDto = trackPostView(postId, userId);
        
        // Check if duration is sufficient for R vector update (>= 15 seconds)
        if (durationSeconds != null && durationSeconds >= 15) {
            try {
                // Get the post to extract emotion
                Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
                
                String emotion = post.getEmotion();
                if (emotion != null && !emotion.trim().isEmpty()) {
                    logger.info("→ Updating R vector for user: {} with emotion: {} (view, {}s)", 
                               userId, emotion, durationSeconds);
                    // rVectorService.updateOnView(userId, post.getId(), Emotion.valueOf(emotion.toUpperCase()), durationSeconds);
                    logger.info("R vector service removed - skipping view update");
                    logger.info("✓ R vector updated successfully - emotion: {}, duration: {}s (view)", 
                               emotion, durationSeconds);
                } else {
                    logger.warn("→ Skipping R vector update - post emotion is null or empty (view, {}s)", durationSeconds);
                }
            } catch (Exception e) {
                // Log error but don't fail the view tracking
                logger.error("✗ Failed to update R vector for user: {} with duration: {}s (view) - {}", 
                            userId, durationSeconds, e.getMessage(), e);
            }
        } else {
            logger.debug("→ Skipping R vector update - duration {}s < 15 seconds threshold", durationSeconds);
        }
        
        return postDto;
    }
    
    /**
     * Get post view statistics for analytics
     * 
     * @param postId The post ID
     * @return PostViewStats with view count and unique viewers
     */
    public PostViewStats getPostViewStats(UUID postId) {
        // Verify post exists
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }
        
        // Get total unique views
        long totalViews = postViewRepository.countByPostId(postId);
        
        // Get recent views (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentViews = postViewRepository.countByPostIdAndViewedAtAfter(postId, thirtyDaysAgo);
        
        // Get unique viewers list (limited to prevent memory issues)
        List<PostView> recentViewers = postViewRepository.findByPostIdOrderByViewedAtDesc(postId);
        
        return new PostViewStats(postId, totalViews, recentViews, recentViewers.size());
    }

    /**
     * Search posts with text query and optional emotion filter
     * 
     * @param query The search query text
     * @param emotion Optional emotion filter (can be null)
     * @param page Page number (0-based)
     * @param size Page size
     * @param sortBy Sort order (newest, oldest, relevance)
     * @param currentUserId Current user ID for personalization
     * @return FeedResponse with search results
     */
    public FeedResponse searchPosts(String query, String emotion, int page, int size, String sortBy, UUID currentUserId) {
        // Validate search query
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        
        // Check cache first
        String cacheKey = String.format("search:%s:%s:%d:%d:%s", 
            query.toLowerCase(), 
            emotion != null ? emotion.toLowerCase() : "all", 
            page, size, sortBy);
        // FeedResponse cachedResults = redisService.getCachedFeed(cacheKey);
        FeedResponse cachedResults = null; // Redis disabled, no cached results
        if (cachedResults != null) {
            return cachedResults;
        }

        // Create pageable with sorting
        Sort sort = getSearchSortFromString(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Execute search based on parameters
        Page<Post> postPage;
        if (emotion != null && !emotion.trim().isEmpty()) {
            // Search with emotion filter
            postPage = postRepository.searchPostsByEmotion(query.trim(), emotion.trim(), pageable);
        } else {
            // Search without emotion filter
            postPage = postRepository.searchPosts(query.trim(), pageable);
        }
        
        // Convert to DTOs
        List<PostDto> postDtos = postPage.getContent().stream()
            .map(post -> convertToPostDto(post, currentUserId))
            .collect(Collectors.toList());
        
        // Build response
        FeedResponse response = new FeedResponse();
        response.setPosts(postDtos);
        response.setCurrentPage(postPage.getNumber());
        response.setTotalPages(postPage.getTotalPages());
        response.setTotalElements(postPage.getTotalElements());
        response.setPageSize(postPage.getSize());
        response.setHasNext(postPage.hasNext());
        response.setFirst(postPage.isFirst());
        response.setLast(postPage.isLast());
        
        // Cache the search results (shorter TTL for search results)
        // redisService.cacheFeed(cacheKey, response);
        
        return response;
    }

    /**
     * Advanced search with relevance scoring and multiple filters
     * 
     * @param query The search query text
     * @param emotion Optional emotion filter
     * @param page Page number (0-based)
     * @param size Page size
     * @param currentUserId Current user ID for personalization
     * @return FeedResponse with relevance-ranked search results
     */
    public FeedResponse searchPostsWithRelevance(String query, String emotion, int page, int size, UUID currentUserId) {
        // Validate search query
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        
        // Check cache first
        String cacheKey = String.format("search_relevance:%s:%s:%d:%d", 
            query.toLowerCase(), 
            emotion != null ? emotion.toLowerCase() : "all", 
            page, size);
        // FeedResponse cachedResults = redisService.getCachedFeed(cacheKey);
        FeedResponse cachedResults = null; // Redis disabled, no cached results
        if (cachedResults != null) {
            return cachedResults;
        }

        // Create pageable
        Pageable pageable = PageRequest.of(page, size);

        // Execute relevance-based search
        Page<Post> postPage = postRepository.searchPostsWithRelevance(query.trim(), emotion, pageable);
        
        // Convert to DTOs
        List<PostDto> postDtos = postPage.getContent().stream()
            .map(post -> convertToPostDto(post, currentUserId))
            .collect(Collectors.toList());
        
        // Build response
        FeedResponse response = new FeedResponse();
        response.setPosts(postDtos);
        response.setCurrentPage(postPage.getNumber());
        response.setTotalPages(postPage.getTotalPages());
        response.setTotalElements(postPage.getTotalElements());
        response.setPageSize(postPage.getSize());
        response.setHasNext(postPage.hasNext());
        response.setFirst(postPage.isFirst());
        response.setLast(postPage.isLast());
        
        // Cache the relevance search results
        // redisService.cacheFeed(cacheKey, response);
        
        return response;
    }

    /**
     * Get posts with tag and term filtering
     * 
     * @param page Page number (0-based)
     * @param size Page size
     * @param tag Optional tag filter
     * @param term Optional search term
     * @param currentUserId Current user ID for personalization
     * @return FeedResponse with filtered posts
     */
    public FeedResponse getPostsWithTagFiltering(int page, int size, String tag, String term, UUID currentUserId) {
        // Validate parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100;

        // Create pageable with sorting by newest first
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Post> postPage;
        
        if (tag != null && term != null) {
            // Both tag and term filtering
            postPage = postRepository.findByTopicsContainingIgnoreCaseAndContentContainingIgnoreCaseOrTitleContainingIgnoreCase(
                tag, term, pageable);
        } else if (tag != null) {
            // Only tag filtering
            postPage = postRepository.findByTopicsContainingIgnoreCase(tag, pageable);
        } else if (term != null) {
            // Only term filtering (search in title, content, and topics)
            postPage = postRepository.searchPosts(term, pageable);
        } else {
            // No filtering, return all posts
            postPage = postRepository.findAll(pageable);
        }

        // Convert to DTOs with batch user engagement lookup
        List<PostDto> postDtos = convertPostsToDtosWithBatchEngagement(postPage.getContent(), currentUserId);

        // Build response
        FeedResponse response = new FeedResponse();
        response.setPosts(postDtos);
        response.setCurrentPage(postPage.getNumber());
        response.setTotalPages(postPage.getTotalPages());
        response.setTotalElements(postPage.getTotalElements());
        response.setPageSize(postPage.getSize());
        response.setHasNext(postPage.hasNext());
        response.setFirst(postPage.isFirst());
        response.setLast(postPage.isLast());

        return response;
    }

    private FlaggedPostDto convertToFlaggedPostDto(Post post) {
        FlaggedPostDto dto = new FlaggedPostDto();
        dto.setPostId(post.getId());
        dto.setFlagReason(post.getFlagReason());
        dto.setFlaggedAt(post.getFlaggedAt());
        dto.setAiConfidence(1.0);
        String content = post.getContent();
        dto.setContentPreview(content != null && content.length() > 100 ? content.substring(0, 100) + "..." : content);
        dto.setStatus("pending_review");
        return dto;
    }
    
    private PostDto convertToPostDto(Post post, UUID currentUserId) {
        // Get actual comment count for this post
        long commentCount = commentRepository.countByPostId(post.getId());
        
        // Use the builder pattern for cleaner, more maintainable code
        PostDtoBuilder builder = PostDtoBuilder.create()
            .id(post.getId())
            .title(post.getTitle())
            .content(post.getContent())
            .preImg(post.getPreImg())
            .emotion(post.getEmotion())
            .date(post.getCreatedAt())
            .resonance(post.getResonance())
            .view(post.getView())
            .collect(post.getCollect())
            .userId(post.getUserId())
            .topics(post.getTopics())
            .aiMetadata(post.getAiMetadata())
            .flagged(post.isFlagged())
            .flagReason(post.getFlagReason())
            .flaggedAt(post.getFlaggedAt())
            .commentsCount((int) commentCount);
        
        // Check if current user has resonanced this post
        if (currentUserId != null) {
            boolean isResonanced = postResonanceRepository.existsByUserIdAndPostId(currentUserId, post.getId());
            builder.isresonanced(isResonanced);
            
            // Check if current user has collected this post
            boolean isCollected = postCollectionRepository.existsByUserIdAndPostId(currentUserId, post.getId());
            builder.isCollected(isCollected);
        } else {
            builder.isresonanced(false).isCollected(false);
        }
        
        return builder.build();
    }
    
    /**
     * Save a post entity
     */
    public Post savePost(Post post) {
        return postRepository.save(post);
    }
    
    /**
     * Delete a post by ID
     */
    public void deletePost(UUID postId) {
        postRepository.deleteById(postId);
        // Invalidate cache if needed
        // redisService.invalidateFeedCache();
    }
    
    /**
     * Update an existing post
     * 
     * @param postId The post ID to update
     * @param userId The user ID requesting the update
     * @param updateRequest PostUpdateRequest containing fields to update
     * @return Updated PostDto
     */
    @Transactional
    public PostDto updatePost(UUID postId, UUID userId, PostUpdateRequest updateRequest) {
        // Find the post
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        
        // Verify user owns the post
        if (!post.getUserId().equals(userId)) {
            throw new IllegalStateException("User not authorized to update this post");
        }
        
        // Validate update request
        if (!updateRequest.hasUpdates()) {
            throw new IllegalArgumentException("No fields provided for update");
        }
        
        if (!updateRequest.isValidEmotion()) {
            throw new IllegalArgumentException("Invalid emotion value provided");
        }
        
        // Update allowed fields
        if (updateRequest.getTitle() != null) {
            post.setTitle(updateRequest.getTitle().trim());
        }
        
        if (updateRequest.getContent() != null) {
            post.setContent(updateRequest.getContent().trim());
        }
        
        if (updateRequest.getEmotion() != null) {
            post.setEmotion(updateRequest.getEmotion().trim());
        }
        
        if (updateRequest.getPreImg() != null) {
            post.setPreImg(updateRequest.getPreImg().trim());
        }
        
        if (updateRequest.getTopics() != null) {
            post.setTopics(updateRequest.getTopics().trim());
        }
        
        // Update timestamp
        post.setUpdatedAt(LocalDateTime.now());
        
        // Save the updated post
        Post savedPost = postRepository.save(post);
        
        // Invalidate cache
        // redisService.invalidateFeedCache();
        
        // Return updated post DTO
        return convertToPostDto(savedPost, userId);
    }
    
    private Sort getSortFromString(String sortBy) {
        return switch (sortBy) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "popular" -> Sort.by(Sort.Direction.DESC, "createdAt"); // Popularity sorting handled in repository query
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private Sort getSearchSortFromString(String sortBy) {
        return switch (sortBy) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "relevance" -> Sort.by(Sort.Direction.DESC, "createdAt"); // Relevance is handled in query
            case "popular" -> Sort.by(Sort.Direction.DESC, "resonance");
            case "most_viewed" -> Sort.by(Sort.Direction.DESC, "view");
            case "most_collected" -> Sort.by(Sort.Direction.DESC, "collect");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
    
    /**
     * Flag a post for moderation
     * 
     * @param postId The post ID to flag
     * @param reason The reason for flagging (optional)
     */
    @Transactional
    public void flagPost(UUID postId, String reason) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        
        post.setFlagged(true);
        post.setFlagReason(reason != null ? reason : "Flagged by admin");
        post.setFlaggedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        
        postRepository.save(post);
        
        // Invalidate cache if needed
        // redisService.invalidateFeedCache();
    }
    
    /**
     * Unflag a post (remove flag)
     * 
     * @param postId The post ID to unflag
     */
    @Transactional
    public void unflagPost(UUID postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        
        post.setFlagged(false);
        post.setFlagReason(null);
        post.setFlaggedAt(null);
        post.setUpdatedAt(LocalDateTime.now());
        
        postRepository.save(post);
        
        // Invalidate cache if needed
        // redisService.invalidateFeedCache();
    }
    
    /**
     * Get feed block with personalized recommendations
     * 
     * @param userId The user ID requesting the block
     * @param blockNumber The block number to retrieve (1-based)
     * @param pageSize The number of posts per block
     * @param prefetch Whether to trigger prefetch of next block
     * @return FeedBlockResponse with posts and block metadata
     */
    @Transactional
    public FeedBlockResponse getFeedBlock(UUID userId, Integer blockNumber, Integer pageSize, Boolean prefetch) {
        PerformanceMonitor.start("PostService.getFeedBlock");
        logger.info("Getting feed block for user: {} block: {} pageSize: {} prefetch: {}", 
                   userId, blockNumber, pageSize, prefetch);
        
        try {
            // Validate parameters
            if (userId == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }
            if (blockNumber == null || blockNumber < 1) {
                throw new IllegalArgumentException("Block number must be at least 1");
            }
            if (pageSize == null || pageSize < 1 || pageSize > 50) {
                throw new IllegalArgumentException("Page size must be between 1 and 50");
            }
            
            // Step 1: Get or generate feed block
            logger.debug("Step 1: Getting or generating block {} for user {}", blockNumber, userId);
            // FeedBlockCache block = feedBlockPreCalculationService.getOrGenerateBlock(userId, blockNumber);
            FeedBlockCache block = null; // Service removed
            
            if (block == null) {
                logger.error("Failed to get or generate block {} for user {}", blockNumber, userId);
                throw new RuntimeException("Failed to generate feed block");
            }
            
            logger.info("Retrieved block {} for user {} with {} posts", 
                       blockNumber, userId, block.getPostCount());
            
            // Step 2: Convert block postIds to PostDto list
            logger.debug("Step 2: Converting {} post IDs to DTOs", block.getPostIds().size());
            List<PostDto> postDtos = convertBlockPostsToDtos(block.getPostIds(), userId);
            
            logger.info("Converted {} posts to DTOs for user {}", postDtos.size(), userId);
            
            // Step 3: Create FeedBlockResponse
            logger.debug("Step 3: Creating FeedBlockResponse");
            FeedBlockResponse feedBlockResponse = new FeedBlockResponse();
            
            // Set posts
            feedBlockResponse.setPosts(postDtos);
            
            // Set block number
            feedBlockResponse.setBlockNumber(blockNumber);
            
            // Create and set block metadata
            BlockMetadata blockMetadata = convertToBlockMetadata(block);
            feedBlockResponse.setBlockMetadata(blockMetadata);
            
            // Set pagination fields
            feedBlockResponse.setCurrentPage(0); // Block-based, not page-based
            feedBlockResponse.setTotalPages(1); // Self-contained block
            feedBlockResponse.setTotalElements(postDtos.size());
            feedBlockResponse.setPageSize(pageSize);
            feedBlockResponse.setFirst(blockNumber == 1);
            
            // Check if next block is available
            boolean nextBlockAvailable = checkNextBlockAvailable(userId, blockNumber + 1);
            feedBlockResponse.setNextBlockAvailable(nextBlockAvailable);
            feedBlockResponse.setNextBlockNumber(blockNumber + 1);
            feedBlockResponse.setHasNext(nextBlockAvailable);
            feedBlockResponse.setLast(!nextBlockAvailable);
            
            logger.info("Feed block response created for user {} block {} with {} posts, nextBlockAvailable: {}", 
                       userId, blockNumber, postDtos.size(), nextBlockAvailable);
            
            // Step 4: Trigger prefetch if requested
            if (Boolean.TRUE.equals(prefetch) && nextBlockAvailable) {
                logger.info("Triggering prefetch for user {} next block {}", userId, blockNumber + 1);
                try {
                    // Trigger prefetch asynchronously
                    // feedBlockPreCalculationService.handleUserScroll(userId);
                    logger.info("Feed block pre-calculation service removed - skipping scroll handling");
                    logger.debug("Prefetch triggered successfully for user {}", userId);
                } catch (Exception e) {
                    logger.warn("Failed to trigger prefetch for user {}: {}", userId, e.getMessage(), e);
                    // Don't fail the main request if prefetch fails
                }
            }
            
            logger.info("Successfully generated feed block response for user {} block {}", userId, blockNumber);
            return feedBlockResponse;
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters for getFeedBlock - userId: {} blockNumber: {} pageSize: {}", 
                        userId, blockNumber, pageSize, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to get feed block for user: {} block: {}", userId, blockNumber, e);
            throw new RuntimeException("Failed to get feed block for user: " + userId + " block: " + blockNumber, e);
        } finally {
            PerformanceMonitor.end("PostService.getFeedBlock");
        }
    }
    
    /**
     * Convert FeedBlockCache to BlockMetadata
     * 
     * @param block The FeedBlockCache to convert
     * @return BlockMetadata with all relevant information
     */
    private BlockMetadata convertToBlockMetadata(FeedBlockCache block) {
        logger.debug("Converting FeedBlockCache to BlockMetadata for block {}", block.getBlockNumber());
        
        BlockMetadata metadata = new BlockMetadata();
        
        // Map emotion distribution
        metadata.setEmotionDistribution(block.getEmotionDistribution());
        
        // Map R vector version
        metadata.setRVectorVersion(block.getRVectorVersion());
        
        // Map timestamps
        metadata.setGeneratedAt(block.getGeneratedAt());
        metadata.setExpiresAt(block.getExpiresAt());
        
        // Set personalized flag (always true for recommendation engine)
        metadata.setIsPersonalized(true);
        
        logger.debug("Converted block {} metadata: {} emotions, R vector v{}, generated: {}, expires: {}", 
                    block.getBlockNumber(), 
                    metadata.getEmotionDistribution() != null ? metadata.getEmotionDistribution().size() : 0,
                    metadata.getRVectorVersion(),
                    metadata.getGeneratedAt(),
                    metadata.getExpiresAt());
        
        return metadata;
    }
    
    /**
     * Check if the next block is available for a user
     * 
     * @param userId The user ID
     * @param nextBlockNumber The next block number to check
     * @return true if next block is available or can be generated
     */
    private boolean checkNextBlockAvailable(UUID userId, Integer nextBlockNumber) {
        try {
            logger.debug("Checking if next block {} is available for user {}", nextBlockNumber, userId);
            
            // Check if block exists in cache by trying to get it
            try {
                // FeedBlockCache cachedBlock = feedBlockPreCalculationService.getOrGenerateBlock(userId, nextBlockNumber);
                FeedBlockCache cachedBlock = null; // Service removed
                boolean available = cachedBlock != null && !cachedBlock.isExpired();
                logger.debug("Next block {} for user {} exists, available: {}", 
                           nextBlockNumber, userId, available);
                return available;
            } catch (Exception e) {
                logger.debug("Next block {} for user {} not available: {}", nextBlockNumber, userId, e.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            logger.warn("Failed to check next block availability for user {} block {}: {}", 
                       userId, nextBlockNumber, e.getMessage(), e);
            return false;
        }
    }
} 

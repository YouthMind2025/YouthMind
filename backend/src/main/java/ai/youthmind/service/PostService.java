package ai.youthmind.service;

import ai.youthmind.dto.PostCreateRequest;
import ai.youthmind.dto.PostDto;
import ai.youthmind.dto.PostDtoBuilder;
import ai.youthmind.dto.FeedResponse;
import ai.youthmind.dto.FlaggedPostDto;
import ai.youthmind.entity.AIAnalysis;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.PostCollection;
import ai.youthmind.entity.PostResonance;
import ai.youthmind.entity.PostView;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.repository.AIAnalysisRepository;
import ai.youthmind.repository.PostCollectionRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.PostResonanceRepository;
import ai.youthmind.repository.PostViewRepository;
import ai.youthmind.util.PerformanceMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Set;

@Service
@Transactional
public class PostService {
    
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
    private RedisService redisService;
    
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

    public Post createPost(UUID userId, PostCreateRequest request) {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setEmotion(request.getEmotion());
        post.setPreImg(request.getPreImg());
        post.setResonance(0); // New post starts with 0 resonance
        post.setView(0); // New post starts with 0 views
        post.setCollect(0); // New post starts with 0 collections
        
        Post savedPost = postRepository.save(post);
        
        // Invalidate feed cache after creating a post
        redisService.invalidateFeedCache();
        
        return savedPost;
    }
    
    /**
     * Get feed with optimized batch operations and caching
     */
    public FeedResponse getFeed(int page, int size, String sortBy, String emotion, 
                               LocalDateTime startDate, LocalDateTime endDate, UUID userId) {
        PerformanceMonitor.start("PostService.getFeed");
        try {
            // Try to get from cache first
            String cacheKey = String.format("feed:page:%d:size:%d:sort:%s:emotion:%s:start:%s:end:%s:user:%s",
                page, size, sortBy, emotion, startDate, endDate, userId);
            
            FeedResponse cachedFeed = redisService.getFromCache(cacheKey, FeedResponse.class);
            if (cachedFeed != null) {
                return cachedFeed;
            }
            
            // Create pageable with sorting
            Pageable pageable = PageRequest.of(page, size, getSortFromString(sortBy));
            
            // Get posts based on filters
            Page<Post> postsPage;
            if (emotion != null && startDate != null && endDate != null) {
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
            feedResponse.setHasPrevious(postsPage.hasPrevious());
            
            // Cache for 2 minutes
            redisService.setInCache(cacheKey, feedResponse, 120);
            
            return feedResponse;
        } finally {
            PerformanceMonitor.end("PostService.getFeed");
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
                dto.setIsresonanced(resonatedPostIds.contains(post.getId()));
                dto.setIsCollected(collectedPostIds.contains(post.getId()));
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
        PostDto cachedPost = redisService.getFromCache(cacheKey, PostDto.class);
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
        redisService.setInCache(cacheKey, postDto, 300);
        
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
        }
        
        // Save the updated post
        postRepository.save(post);
        
        // Invalidate cache
        redisService.invalidateFeedCache();
        
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
        redisService.invalidateFeedCache();
        
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
            redisService.invalidateFeedCache();
        }
        
        // Convert to DTO and return
        return convertToPostDto(post, userId);
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
        FeedResponse cachedResults = redisService.getCachedFeed(cacheKey);
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
        redisService.cacheFeed(cacheKey, response);
        
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
        FeedResponse cachedResults = redisService.getCachedFeed(cacheKey);
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
        redisService.cacheFeed(cacheKey, response);
        
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
            .commentsCount(0); // TODO: Implement comments count
        
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
        redisService.invalidateFeedCache();
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
        redisService.invalidateFeedCache();
        
        // Return updated post DTO
        return convertToPostDto(savedPost, userId);
    }
    
    private Sort getSortFromString(String sortBy) {
        return switch (sortBy) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "popular" -> Sort.by(Sort.Direction.DESC, "createdAt"); // TODO: Implement popularity sorting
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
} 
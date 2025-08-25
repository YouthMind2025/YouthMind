package ai.youthmind.service;

import ai.youthmind.dto.PostCreateRequest;
import ai.youthmind.dto.PostDto;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
     * Get paginated feed of posts
     */
    public FeedResponse getFeed(int page, int size, String sortBy, String emotion, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, UUID currentUserId) {
        // Check cache first
        String cacheKey = String.format("feed:latest:%d:%d:%s:%s:%s:%s", page, size, sortBy,
                emotion == null ? "" : emotion,
                startDate == null ? "" : startDate.toString(),
                endDate == null ? "" : endDate.toString());
        FeedResponse cachedFeed = redisService.getCachedFeed(cacheKey);
        if (cachedFeed != null) {
            return cachedFeed;
        }

        // Create pageable with sorting
        Sort sort = getSortFromString(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // Determine appropriate query based on filters
        Page<Post> postPage;
        boolean hasEmotion = emotion != null && !emotion.isBlank();
        boolean hasDateRange = startDate != null && endDate != null;

        if (hasEmotion && hasDateRange) {
            postPage = postRepository.findByEmotionIgnoreCaseAndCreatedAtBetween(emotion, startDate, endDate, pageable);
        } else if (hasEmotion) {
            postPage = postRepository.findByEmotionIgnoreCase(emotion, pageable);
        } else if (hasDateRange) {
            postPage = postRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        } else {
            postPage = postRepository.findAll(pageable);
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
        
        // Cache the response
        redisService.cacheFeed(cacheKey, response);
        
        return response;
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
     * Get post by ID with view tracking
     * Increments view count only for first-time views per user
     */
    public PostDto getPostById(UUID postId, UUID currentUserId) {
        // Find the post
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        
        // Check if current user has already viewed this post
        boolean hasUserViewed = postViewRepository.existsByUserIdAndPostId(currentUserId, postId);
        
        // If first time viewing, record the view
        if (!hasUserViewed) {
            PostView newView = new PostView(currentUserId, postId);
            postViewRepository.save(newView);
            
            // Update post view count
            post.setView(post.getView() + 1);
            postRepository.save(post);
            
            // Invalidate cache if needed
            redisService.invalidateFeedCache();
        }
        
        // Convert to DTO and return
        return convertToPostDto(post, currentUserId);
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
        PostDto dto = new PostDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setPreImg(post.getPreImg());
        dto.setEmotion(post.getEmotion());
        dto.setDate(post.getCreatedAt());
        dto.setResonance(post.getResonance());
        dto.setView(post.getView());
        dto.setCollect(post.getCollect());
        
        // Check if current user has resonanced this post
        boolean isResonanced = postResonanceRepository.existsByUserIdAndPostId(currentUserId, post.getId());
        dto.setIsresonanced(isResonanced);
        
        // Check if current user has collected this post
        boolean isCollected = postCollectionRepository.existsByUserIdAndPostId(currentUserId, post.getId());
        dto.setIsCollected(isCollected);
        
        return dto;
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
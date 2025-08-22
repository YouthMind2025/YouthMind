package ai.youthmind.service;

import ai.youthmind.dto.PostCreateRequest;
import ai.youthmind.dto.PostDto;
import ai.youthmind.dto.FeedResponse;
import ai.youthmind.dto.FlaggedPostDto;
import ai.youthmind.entity.AIAnalysis;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.PostView;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.repository.AIAnalysisRepository;
import ai.youthmind.repository.PostRepository;
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
        post.setViewCount(0); // New post starts with 0 views
        post.setCollectCount(0); // New post starts with 0 collections
        
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
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);
            
            // Invalidate cache if needed
            redisService.invalidateFeedCache();
        }
        
        // Convert to DTO and return
        return convertToPostDto(post, currentUserId);
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
        dto.setView(post.getViewCount());
        dto.setCollect(post.getCollectCount());
        
        // TODO: Implement user reaction tracking - for now set defaults
        dto.setIsresonanced(false);
        dto.setIsCollected(false);
        
        return dto;
    }
    
    private Sort getSortFromString(String sortBy) {
        return switch (sortBy) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "popular" -> Sort.by(Sort.Direction.DESC, "createdAt"); // TODO: Implement popularity sorting
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
} 
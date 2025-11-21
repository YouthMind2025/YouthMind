package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.FeedResponse;
import ai.youthmind.dto.FeedBlockResponse;
import ai.youthmind.dto.PostCreateRequest;
import ai.youthmind.dto.PostCreateResponse;
import ai.youthmind.dto.PostDto;
import ai.youthmind.dto.PostDtoBuilder;
import ai.youthmind.dto.PostUpdateRequest;
import ai.youthmind.dto.PostViewDurationRequest;
import ai.youthmind.dto.PostViewStats;
import ai.youthmind.dto.PsychologicalAnalysisRequest;
import ai.youthmind.dto.PsychologicalAnalysisResponse;
import ai.youthmind.entity.Post;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.service.PostService;
import ai.youthmind.service.RedisService;
import ai.youthmind.service.PsychologicalStateCacheService;
import ai.youthmind.service.AIServiceClient;
import ai.youthmind.util.AuthenticationUtils;
import ai.youthmind.util.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Post Controller - Unified endpoint for all post-related operations
 * 
 * This controller consolidates all post management functionality including:
 * - Post CRUD operations (create, read, update, delete)
 * - Post engagement (resonance/likes, collection/saves, views)
 * - Post discovery (feed, search, filtering)
 * - Post analytics (view statistics)
 * 
 * Key Features:
 * - JWT-based authentication for all endpoints
 * - Comprehensive input validation and error handling
 * - Optimized caching and batch operations
 * - RESTful API design with consistent response formats
 * - Swagger/OpenAPI documentation for all endpoints
 * 
 * Performance Optimizations:
 * - Redis caching for frequently accessed data
 * - Batch database operations for engagement status
 * - Asynchronous view tracking to improve response times
 * - Efficient pagination and filtering
 * 
 * Security Features:
 * - User ownership verification for modifications
 * - Input sanitization and validation
 * - Rate limiting considerations (implemented at service layer)
 * 
 * @author YouthMind Development Team
 * @version 2.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/v1/posts")
@Tag(name = "Posts", description = "Post creation and management operations")
@SecurityRequirement(name = "bearer-jwt")
public class PostController {

    @Autowired
    private PostService postService;

    //@Autowired
    //private RedisService redisService;
    
    @Autowired
    private PsychologicalStateCacheService psychologicalStateCacheService;
    
    @Autowired
    private AIServiceClient aiServiceClient;
    

    @PostMapping("/create")
    @Operation(
        summary = "Create a new post",
        description = "Create a new post with content, emotion tag, and optional AI metadata"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Post created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<PostCreateResponse>> createPost(@Valid @RequestBody PostCreateRequest request) {
        // Extract user ID from JWT token
        UUID userId = AuthenticationUtils.getCurrentUserId();
        
        // Create the post
        Post savedPost = postService.createPost(userId, request);
        
        // Build full PostDto for response using PostDtoBuilder
        PostDto postDto = PostDtoBuilder.create()
            .id(savedPost.getId())
            .title(savedPost.getTitle())
            .content(savedPost.getContent())
            .preImg(savedPost.getPreImg())
            .emotion(savedPost.getEmotion())
            .date(savedPost.getCreatedAt())
            .resonance(savedPost.getResonance())
            .view(savedPost.getView())
            .collect(savedPost.getCollect())
            .isresonanced(false) // New post, user hasn't resonated yet
            .isCollected(false)  // New post, user hasn't collected yet
            .build();
        
        // Build response with full post data
        PostCreateResponse response = new PostCreateResponse(postDto, "Post created successfully");

        // Trigger psychological analysis for the post content
        try {
            // Create psychological analysis request
            PsychologicalAnalysisRequest analysisRequest = new PsychologicalAnalysisRequest();
            analysisRequest.setUserId(userId);
            analysisRequest.setContent(savedPost.getContent());
            analysisRequest.setContentType("post");
            analysisRequest.setTimestamp(LocalDateTime.now());
            
            // Set analysis configuration
            PsychologicalAnalysisRequest.AnalysisConfig config = new PsychologicalAnalysisRequest.AnalysisConfig();
            config.setGeneratePsychologicalVector(true);
            config.setAnalyzeBuddhistFactors(true);
            config.setCalculateIntensityFactor(true);
            config.setProcessStateVector(true);
            config.setIncludeDetailedAnalysis(true);
            config.setIncludeRecommendations(false); // Skip recommendations for post creation
            config.setMaxAnalysisDepth(3);
            config.setAnalysisMode("standard");
            analysisRequest.setAnalysisConfig(config);
            
            // Perform psychological analysis asynchronously
            aiServiceClient.performPsychologicalAnalysis(analysisRequest);
            
            // Update user's psychological state based on post content (simplified)
            psychologicalStateCacheService.warmUpCache(userId);
            
        } catch (Exception e) {
            // Log error but don't fail post creation
            System.err.println("Failed to trigger psychological analysis for post: " + e.getMessage());
        }

        // Publish enhanced event with structured data
        // redisService.publishPostCreatedEvent(
        //     savedPost.getId().toString(), 
        //     userId.toString(), 
        //     savedPost.getEmotion()
        // );
        
        return ResponseBuilder.created(response, "Post created successfully");
    }

    @GetMapping
    @Operation(
        summary = "Get all posts",
        description = "Retrieve all posts with pagination and optional tag/term filtering"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Posts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<FeedResponse>> getAllPosts(
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "Number of posts per page", example = "20")
        @RequestParam(defaultValue = "20") int size,
        
        @Parameter(description = "Filter by tag", example = "mental-health")
        @RequestParam(required = false) String tag,
        
        @Parameter(description = "Search term", example = "anxiety")
        @RequestParam(required = false) String term
    ) {
        // Extract user ID from JWT token
        UUID userId = AuthenticationUtils.getCurrentUserId();
        
        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100; // Limit max page size
        
        // Get posts with optional tag/term filtering
        FeedResponse feedResponse;
        if (tag != null || term != null) {
            feedResponse = postService.getPostsWithTagFiltering(page, size, tag, term, userId);
        } else {
            feedResponse = postService.getFeed(page, size, "newest", null, null, null, userId);
        }
        
        return ResponseBuilder.success(feedResponse);
    }

    @GetMapping("/feed")
    @Operation(
        summary = "Get paginated feed of posts",
        description = "Retrieve a paginated list of posts with optional filters and caching support"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Feed retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<FeedResponse>> getFeed(
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "Number of posts per page", example = "20")
        @RequestParam(defaultValue = "20") int size,
        
        @Parameter(description = "Sort order", example = "newest")
        @RequestParam(defaultValue = "newest") String sortBy,

        @Parameter(description = "Filter by emotion", example = "happy")
        @RequestParam(required = false) String emotion,

        @Parameter(description = "Filter by start date (ISO 8601)", example = "2023-01-01T00:00:00")
        @RequestParam(required = false) String startDate,

        @Parameter(description = "Filter by end date (ISO 8601)", example = "2023-12-31T23:59:59")
        @RequestParam(required = false) String endDate
    ) {
        // Extract user ID from JWT token
        UUID userId = AuthenticationUtils.getCurrentUserId();
        
        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100; // Limit max page size
        
        // Parse date filters if provided
        LocalDateTime startDt = null;
        LocalDateTime endDt = null;
        try {
            if (startDate != null && !startDate.isBlank()) {
                startDt = LocalDateTime.parse(startDate);
            }
            if (endDate != null && !endDate.isBlank()) {
                endDt = LocalDateTime.parse(endDate);
            }
        } catch (Exception e) {
            // Ignore parse errors and treat as null - alternatively handle validation
        }

        // Get feed from service (includes caching)
        FeedResponse feedResponse = postService.getFeed(page, size, sortBy, emotion, startDt, endDt, userId);
        
        return ResponseBuilder.success(feedResponse);
    }

    @GetMapping("/feed/blocks")
    @Operation(
        summary = "Get personalized feed block",
        description = "Retrieve a personalized feed block with recommendation engine integration and block metadata"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Feed block retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ApiResponse<FeedBlockResponse>> getFeedBlock(
        @Parameter(description = "Block number (1-based)", example = "1")
        @RequestParam(defaultValue = "1") int blockNumber,
        
        @Parameter(description = "Number of posts per block", example = "20")
        @RequestParam(defaultValue = "20") int pageSize,
        
        @Parameter(description = "Whether to trigger prefetch of next block", example = "false")
        @RequestParam(defaultValue = "false") boolean prefetch
    ) {
        try {
            // Extract user ID from JWT token
            UUID userId = AuthenticationUtils.getCurrentUserId();
            
            // Validate parameters
            if (blockNumber < 1) {
                return ResponseBuilder.badRequest("Block number must be at least 1");
            }
            if (pageSize < 1 || pageSize > 50) {
                return ResponseBuilder.badRequest("Page size must be between 1 and 50");
            }
            
            // Get feed block from service
            FeedBlockResponse feedBlockResponse = postService.getFeedBlock(userId, blockNumber, pageSize, prefetch);
            
            return ResponseBuilder.success(feedBlockResponse, "Feed block retrieved successfully");
            
        } catch (IllegalArgumentException e) {
            return ResponseBuilder.badRequest(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseBuilder.notFound(e.getMessage());
        } catch (Exception e) {
            return ResponseBuilder.internalServerError("Failed to retrieve feed block: " + e.getMessage());
        }
    }


    @GetMapping("/by-user/{userId}")
    @Operation(
        summary = "Get posts by user",
        description = "Retrieve posts created by a specific user"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Posts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<List<PostDto>>> getPostsByUser(
        @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID userId
    ) {
        // Extract current user ID from JWT token
        UUID currentUserId = AuthenticationUtils.getCurrentUserId();
        
        List<PostDto> posts = postService.getPostsByUser(userId, currentUserId);
        
        return ResponseBuilder.success(posts);
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get post details by ID",
        description = "Retrieve detailed information about a specific post with view tracking"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<ApiResponse<PostDto>> getPostById(
        @Parameter(description = "Post ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        // Extract user ID from JWT token
        UUID userId = AuthenticationUtils.getCurrentUserId();
        
        // Get post with view tracking
        PostDto post = postService.getPostById(id, userId);
        
        return ResponseBuilder.success(post);
    }
    
    @PutMapping("/{id}/resonance")
    @Operation(
        summary = "Toggle post resonance (like/unlike)",
        description = "Like or unlike a post. If the user has already liked the post, this will unlike it, otherwise it will like it."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Resonance toggled successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<ApiResponse<PostDto>> toggleResonance(
        @Parameter(description = "Post ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        // Extract user ID from JWT token
        UUID userId = AuthenticationUtils.getCurrentUserId();
        
        // Toggle resonance
        PostDto updatedPost = postService.toggleResonance(id, userId);
        
        return ResponseBuilder.success(updatedPost);
    }
    
    @PutMapping("/{id}/collect")
    @Operation(
        summary = "Toggle post collection (save/unsave)",
        description = "Save or unsave a post. If the user has already saved the post, this will unsave it, otherwise it will save it."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Collection toggled successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<ApiResponse<PostDto>> toggleCollection(
        @Parameter(description = "Post ID", example = "123d4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        // Extract user ID from JWT token
        UUID userId = AuthenticationUtils.getCurrentUserId();
        
        // Toggle collection
        PostDto updatedPost = postService.toggleCollection(id, userId);
        
        return ResponseBuilder.success(updatedPost);
    }
    
    
    
    @PutMapping("/{id}")
    @Operation(
        summary = "Update post",
        description = "Update an existing post. Only the post owner can update their posts."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to update this post"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<PostDto>> updatePost(
        @Parameter(description = "Post ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id,
        @Valid @RequestBody PostUpdateRequest updateRequest
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            // Validate update request
            if (!updateRequest.hasUpdates()) {
                return ResponseBuilder.badRequest("No fields provided for update");
            }

            if (!updateRequest.isValidEmotion()) {
                return ResponseBuilder.badRequest("Invalid emotion value provided");
            }

            // Update the post using PostService
            PostDto updatedPost = postService.updatePost(id, currentUserId, updateRequest);
            
            return ResponseBuilder.success(updatedPost, "Post updated successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseBuilder.notFound(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseBuilder.badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseBuilder.forbidden(e.getMessage());
        } catch (Exception e) {
            return ResponseBuilder.internalServerError("Failed to update post");
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete post",
        description = "Delete a post. Only the post owner can delete their posts."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to delete this post"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> deletePost(
        @Parameter(description = "Post ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();

            Optional<Post> postOpt = postService.getPostById(id);
            if (postOpt.isEmpty()) {
                return ResponseBuilder.notFound("Post not found");
            }

            Post post = postOpt.get();
            // Check if user owns the post
            if (!post.getUserId().equals(currentUserId)) {
                return ResponseBuilder.forbidden("Not authorized to delete this post");
            }

            // Delete the post
            postService.deletePost(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Post deleted successfully");

            return ResponseBuilder.success(response, "Post deleted successfully");
        } catch (Exception e) {
            return ResponseBuilder.internalServerError("Failed to delete post");
        }
    }
    
    @PostMapping("/{id}/view")
    @Operation(
        summary = "Track post view",
        description = "Track a view for a post by the current user. Only increments count for first-time views per user."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "View tracked successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<PostDto>> trackPostView(
        @Parameter(description = "Post ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            
            // Track the view using PostService
            PostDto updatedPost = postService.trackPostView(id, currentUserId);
            
            return ResponseBuilder.success(updatedPost, "View tracked successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseBuilder.notFound(e.getMessage());
        } catch (Exception e) {
            return ResponseBuilder.internalServerError("Failed to track view");
        }
    }
    
    @PostMapping("/{id}/view/duration")
    @Operation(
        summary = "Track post view with duration",
        description = "Track a post view with duration and update R vector if duration >= 15 seconds"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "View with duration tracked successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    public ResponseEntity<ApiResponse<PostDto>> trackPostViewWithDuration(
        @Parameter(description = "Post ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "View duration request")
        @Valid @RequestBody PostViewDurationRequest request
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            
            // Track the view with duration using PostService
            PostDto updatedPost = postService.trackPostViewWithDuration(id, currentUserId, request.getDurationSeconds());
            
            return ResponseBuilder.success(updatedPost, "View with duration tracked successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseBuilder.notFound(e.getMessage());
        } catch (Exception e) {
            return ResponseBuilder.internalServerError("Failed to track view with duration");
        }
    }
    
    @PostMapping("/{id}/view/start")
    @Operation(
        summary = "Start tracking post view session",
        description = "Start tracking a post view session for duration-based analytics and R vector updates"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "View session started successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<String>> startPostView(
        @Parameter(description = "Post ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            
            // Start view session tracking
            // viewDurationTrackingService.startView(currentUserId, id);
            System.out.println("View duration tracking service removed - skipping start view");
            
            return ResponseBuilder.success("View session started", "View tracking started successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseBuilder.notFound(e.getMessage());
        } catch (Exception e) {
            return ResponseBuilder.internalServerError("Failed to start view session");
        }
    }
    
    @PostMapping("/{id}/view/end")
    @Operation(
        summary = "End tracking post view session",
        description = "End a post view session and process duration-based analytics and R vector updates"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "View session ended successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<String>> endPostView(
        @Parameter(description = "Post ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            
            // End view session tracking
            // viewDurationTrackingService.endView(currentUserId, id);
            System.out.println("View duration tracking service removed - skipping end view");
            
            return ResponseBuilder.success("View session ended", "View tracking completed successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseBuilder.notFound(e.getMessage());
        } catch (Exception e) {
            return ResponseBuilder.internalServerError("Failed to end view session");
        }
    }
    
    @GetMapping("/{id}/view-stats")
    @Operation(
        summary = "Get post view statistics",
        description = "Retrieve analytics data about post views including total views, recent views, and engagement metrics"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "View statistics retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<PostViewStats>> getPostViewStats(
        @Parameter(description = "Post ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        try {
            // Extract user from authentication
            UUID userId = AuthenticationUtils.getCurrentUserId();
            
            // Get view statistics using PostService
            PostViewStats viewStats = postService.getPostViewStats(id);
            
            return ResponseBuilder.success(viewStats, "View statistics retrieved successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseBuilder.notFound(e.getMessage());
        } catch (Exception e) {
            return ResponseBuilder.internalServerError("Failed to retrieve view statistics");
        }
    }
    
    // ===== PSYCHOLOGICAL ANALYSIS ENDPOINTS =====
    
    @PostMapping("/{id}/analyze-psychological")
    @Operation(
        summary = "Analyze post psychological content",
        description = "Perform psychological analysis on a specific post's content"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Psychological analysis completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<PsychologicalAnalysisResponse>> analyzePostPsychological(
            @Parameter(description = "Post ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @RequestBody(required = false) @Parameter(description = "Optional analysis configuration") Map<String, Object> config) {
        
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            
            // Get the post
            Optional<Post> postOpt = postService.getPostById(id);
            if (postOpt.isEmpty()) {
                return ResponseBuilder.notFound("Post not found");
            }
            
            Post post = postOpt.get();
            
            // Create psychological analysis request
            PsychologicalAnalysisRequest analysisRequest = new PsychologicalAnalysisRequest();
            analysisRequest.setUserId(post.getUserId());
            analysisRequest.setContent(post.getContent());
            analysisRequest.setContentType("post");
            analysisRequest.setTimestamp(LocalDateTime.now());
            
            // Set analysis configuration
            PsychologicalAnalysisRequest.AnalysisConfig analysisConfig = new PsychologicalAnalysisRequest.AnalysisConfig();
            analysisConfig.setGeneratePsychologicalVector(true);
            analysisConfig.setAnalyzeBuddhistFactors(true);
            analysisConfig.setCalculateIntensityFactor(true);
            analysisConfig.setProcessStateVector(true);
            analysisConfig.setIncludeDetailedAnalysis(true);
            analysisConfig.setIncludeRecommendations(true);
            analysisConfig.setMaxAnalysisDepth(4);
            analysisConfig.setAnalysisMode("comprehensive");
            analysisRequest.setAnalysisConfig(analysisConfig);
            
            // Perform psychological analysis
            PsychologicalAnalysisResponse response = aiServiceClient.performPsychologicalAnalysis(analysisRequest);
            
            return ResponseBuilder.success(response, "Psychological analysis completed successfully");
            
        } catch (Exception e) {
            return ResponseBuilder.internalServerError("Failed to perform psychological analysis");
        }
    }
    
    @GetMapping("/{id}/psychological-metadata")
    @Operation(
        summary = "Get post psychological metadata",
        description = "Retrieve psychological analysis metadata stored with the post"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Psychological metadata retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPostPsychologicalMetadata(
            @Parameter(description = "Post ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        
        try {
            // Extract user from authentication
            UUID currentUserId = AuthenticationUtils.getCurrentUserId();
            
            // Get the post
            Optional<Post> postOpt = postService.getPostById(id);
            if (postOpt.isEmpty()) {
                return ResponseBuilder.notFound("Post not found");
            }
            
            Post post = postOpt.get();
            
            // Extract psychological metadata from AI metadata
            Map<String, Object> psychologicalMetadata = new HashMap<>();
            if (post.getAiMetadata() != null && !post.getAiMetadata().isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> aiMetadata = mapper.readValue(post.getAiMetadata(), Map.class);
                    psychologicalMetadata.put("psychological_vector", aiMetadata.get("psychological_vector"));
                    psychologicalMetadata.put("intensity_factor", aiMetadata.get("intensity_factor"));
                    psychologicalMetadata.put("buddhist_factors", aiMetadata.get("buddhist_factors"));
                    psychologicalMetadata.put("analysis_timestamp", aiMetadata.get("analysis_timestamp"));
                } catch (Exception e) {
                    psychologicalMetadata.put("message", "Failed to parse psychological metadata");
                }
            } else {
                psychologicalMetadata.put("message", "No psychological metadata available");
            }
            
            return ResponseBuilder.success(psychologicalMetadata, "Psychological metadata retrieved successfully");
            
        } catch (Exception e) {
            return ResponseBuilder.internalServerError("Failed to retrieve psychological metadata");
        }
    }
}

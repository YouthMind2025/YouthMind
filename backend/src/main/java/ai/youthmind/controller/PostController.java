package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.FeedResponse;
import ai.youthmind.dto.PostCreateRequest;
import ai.youthmind.dto.PostCreateResponse;
import ai.youthmind.dto.PostDto;
import ai.youthmind.entity.Post;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.service.PostService;
import ai.youthmind.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/posts")
@Tag(name = "Posts", description = "Post creation and management operations")
@SecurityRequirement(name = "bearer-jwt")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private RedisService redisService;

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = (UUID) authentication.getPrincipal();
        
        // Create the post
        Post savedPost = postService.createPost(userId, request);
        
        // Build full PostDto for response
        PostDto postDto = new PostDto();
        postDto.setId(savedPost.getId());
        postDto.setTitle(savedPost.getTitle());
        postDto.setContent(savedPost.getContent());
        postDto.setPreImg(savedPost.getPreImg());
        postDto.setEmotion(savedPost.getEmotion());
        postDto.setDate(savedPost.getCreatedAt());
        postDto.setResonance(savedPost.getResonance());
        postDto.setView(savedPost.getView());
        postDto.setCollect(savedPost.getCollect());
        postDto.setIsresonanced(false); // New post, user hasn't resonated yet
        postDto.setIsCollected(false);  // New post, user hasn't collected yet
        
        // Build response with full post data
        PostCreateResponse response = new PostCreateResponse(postDto, "Post created successfully");

        // Publish enhanced event with structured data
        redisService.publishPostCreatedEvent(
            savedPost.getId().toString(), 
            userId.toString(), 
            savedPost.getEmotion()
        );
        
        ApiResponse<PostCreateResponse> apiResponse = ApiResponse.success(response, 201);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = (UUID) authentication.getPrincipal();
        
        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size < 1) size = 20;
        if (size > 100) size = 100; // Limit max page size
        
        // Parse date filters if provided
        java.time.LocalDateTime startDt = null;
        java.time.LocalDateTime endDt = null;
        try {
            if (startDate != null && !startDate.isBlank()) {
                startDt = java.time.LocalDateTime.parse(startDate);
            }
            if (endDate != null && !endDate.isBlank()) {
                endDt = java.time.LocalDateTime.parse(endDate);
            }
        } catch (Exception e) {
            // Ignore parse errors and treat as null - alternatively handle validation
        }

        // Get feed from service (includes caching)
        FeedResponse feedResponse = postService.getFeed(page, size, sortBy, emotion, startDt, endDt, userId);
        
        ApiResponse<FeedResponse> apiResponse = ApiResponse.success(feedResponse, 200);
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/by-emotion/{emotion}")
    @Operation(
        summary = "Get posts by emotion",
        description = "Retrieve posts filtered by specific emotion"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Posts retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ApiResponse<List<PostDto>>> getPostsByEmotion(
        @Parameter(description = "Emotion to filter by", example = "happy")
        @PathVariable String emotion
    ) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = (UUID) authentication.getPrincipal();
        
        List<PostDto> posts = postService.getPostsByEmotion(emotion, userId);
        
        ApiResponse<List<PostDto>> apiResponse = ApiResponse.success(posts, 200);
        return ResponseEntity.ok(apiResponse);
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID currentUserId = (UUID) authentication.getPrincipal();
        
        List<PostDto> posts = postService.getPostsByUser(userId, currentUserId);
        
        ApiResponse<List<PostDto>> apiResponse = ApiResponse.success(posts, 200);
        return ResponseEntity.ok(apiResponse);
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = (UUID) authentication.getPrincipal();
        
        // Get post with view tracking
        PostDto post = postService.getPostById(id, userId);
        
        ApiResponse<PostDto> apiResponse = ApiResponse.success(post, 200);
        return ResponseEntity.ok(apiResponse);
    }
} 
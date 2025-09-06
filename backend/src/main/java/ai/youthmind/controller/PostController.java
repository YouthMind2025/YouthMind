package ai.youthmind.controller;

import ai.youthmind.dto.FeedResponse;
import ai.youthmind.dto.PostCreateRequest;
import ai.youthmind.dto.PostCreateResponse;
import ai.youthmind.dto.PostDto;
import ai.youthmind.entity.Post;
import ai.youthmind.service.PostService;
import ai.youthmind.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
        @ApiResponse(responseCode = "201", description = "Post created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<ai.youthmind.dto.ApiResponse<PostCreateResponse>> createPost(@Valid @RequestBody PostCreateRequest request) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = (UUID) authentication.getPrincipal();
        
        // Create the post
        Post savedPost = postService.createPost(userId, request);
        
        // Build response according to API specification
        PostCreateResponse response = new PostCreateResponse();
        response.setId(savedPost.getId());
        response.setAuthorId(savedPost.getUserId());
        response.setAuthorName("John Doe"); // TODO: Get actual author name
        response.setTitle(savedPost.getTitle());
        response.setContent(savedPost.getContent());
        response.setEmotion(savedPost.getEmotionTag());
        response.setPreImg(savedPost.getPreImg());
        response.setTopics(savedPost.getTopics() != null ? savedPost.getTopics().split(",") : new String[0]);
        response.setResonance(savedPost.getLikes() != null ? savedPost.getLikes() : 0);
        response.setViews(savedPost.getViews() != null ? savedPost.getViews() : 0);
        response.setCollects(savedPost.getBookmarks() != null ? savedPost.getBookmarks() : 0);
        response.setIsResonanced(false);
        response.setIsCollected(false);
        response.setCreatedAt(savedPost.getCreatedAt());
        response.setUpdatedAt(savedPost.getUpdatedAt());

        // Publish enhanced event with structured data
        redisService.publishPostCreatedEvent(
            savedPost.getId().toString(), 
            userId.toString(), 
            savedPost.getEmotionTag()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ai.youthmind.dto.ApiResponse.success(response, 201));
    }

    @GetMapping("/feed")
    @Operation(
        summary = "Get paginated feed of posts",
        description = "Retrieve a paginated list of posts with optional filters and caching support"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Feed retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<FeedResponse> getFeed(
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        
        @Parameter(description = "Number of posts per page", example = "20")
        @RequestParam(defaultValue = "20") int size,
        
        @Parameter(description = "Sort order", example = "newest")
        @RequestParam(defaultValue = "newest") String sortBy,

        @Parameter(description = "Filter by emotion tag", example = "joy")
        @RequestParam(required = false) String emotionTag,

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
        FeedResponse feedResponse = postService.getFeed(page, size, sortBy, emotionTag, startDt, endDt, userId);
        
        return ResponseEntity.ok(feedResponse);
    }

    @GetMapping("/by-emotion/{emotionTag}")
    @Operation(
        summary = "Get posts by emotion tag",
        description = "Retrieve posts filtered by specific emotion tag"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Posts retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<List<PostDto>> getPostsByEmotion(
        @Parameter(description = "Emotion tag to filter by", example = "stress")
        @PathVariable String emotionTag
    ) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = (UUID) authentication.getPrincipal();
        
        List<PostDto> posts = postService.getPostsByEmotionTag(emotionTag, userId);
        
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/by-user/{userId}")
    @Operation(
        summary = "Get posts by user",
        description = "Retrieve posts created by a specific user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Posts retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token")
    })
    public ResponseEntity<List<PostDto>> getPostsByUser(
        @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID userId
    ) {
        // Extract current user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID currentUserId = (UUID) authentication.getPrincipal();
        
        List<PostDto> posts = postService.getPostsByUser(userId, currentUserId);
        
        return ResponseEntity.ok(posts);
    }
} 
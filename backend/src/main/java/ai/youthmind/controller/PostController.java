package ai.youthmind.controller;

import ai.youthmind.dto.PostCreateRequest;
import ai.youthmind.dto.PostCreateResponse;
import ai.youthmind.entity.Post;
import ai.youthmind.service.PostService;
import ai.youthmind.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<PostCreateResponse> createPost(@Valid @RequestBody PostCreateRequest request) {
        // Extract user ID from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = (UUID) authentication.getPrincipal();
        
        // Create the post
        Post savedPost = postService.createPost(userId, request);
        
        // Build response
        PostCreateResponse response = new PostCreateResponse(savedPost.getId(), savedPost.getCreatedAt());

        redisService.publishPostCreated("Post " + response.getPostId() + " created");
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
} 
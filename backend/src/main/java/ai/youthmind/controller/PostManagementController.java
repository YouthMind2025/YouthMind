package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.PostDto;
import ai.youthmind.dto.PostEngagementRequest;
import ai.youthmind.dto.PostEngagementResponse;
import ai.youthmind.entity.Post;
import ai.youthmind.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/posts")
@Tag(name = "Post Management", description = "Post management operations")
@SecurityRequirement(name = "bearer-jwt")
public class PostManagementController {

    @Autowired
    private PostService postService;

    @GetMapping("/{postId}")
    @Operation(
        summary = "Get single post",
        description = "Retrieve a single post by ID"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<ApiResponse<PostDto>> getPost(@PathVariable UUID postId) {
        try {
            Optional<Post> postOpt = postService.getPostById(postId);
            if (postOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, HttpStatus.NOT_FOUND.value()));
            }

            Post post = postOpt.get();
            // Convert to PostDto (simplified for now)
            PostDto postDto = new PostDto();
            // Set fields manually since there are compilation issues
            
            return ResponseEntity.ok(ApiResponse.success(postDto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PutMapping("/{postId}")
    @Operation(
        summary = "Update post",
        description = "Update an existing post"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to update this post")
    })
    public ResponseEntity<ApiResponse<PostDto>> updatePost(
        @PathVariable UUID postId,
        @RequestBody Map<String, Object> updateRequest
    ) {
        try {
            // Extract user from authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID currentUserId = (UUID) authentication.getPrincipal();

            Optional<Post> postOpt = postService.getPostById(postId);
            if (postOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, HttpStatus.NOT_FOUND.value()));
            }

            Post post = postOpt.get();
            // Check if user owns the post
            if (!post.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(null, HttpStatus.FORBIDDEN.value()));
            }

            // Update post fields (simplified)
            // In a real implementation, you would update the fields from updateRequest

            PostDto postDto = new PostDto();
            // Set fields manually
            
            return ResponseEntity.ok(ApiResponse.success(postDto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/{postId}")
    @Operation(
        summary = "Delete post",
        description = "Delete a post"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to delete this post")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> deletePost(@PathVariable UUID postId) {
        try {
            // Extract user from authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID currentUserId = (UUID) authentication.getPrincipal();

            Optional<Post> postOpt = postService.getPostById(postId);
            if (postOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, HttpStatus.NOT_FOUND.value()));
            }

            Post post = postOpt.get();
            // Check if user owns the post
            if (!post.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(null, HttpStatus.FORBIDDEN.value()));
            }

            // Delete the post
            postService.deletePost(postId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Post deleted successfully");

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{postId}/resonance")
    @Operation(
        summary = "Like/Unlike post",
        description = "Toggle like (resonance) on a post"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Resonance toggled successfully")
    })
    public ResponseEntity<ApiResponse<PostEngagementResponse>> toggleResonance(
        @PathVariable UUID postId,
        @RequestBody PostEngagementRequest request
    ) {
        try {
            // Extract user from authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Toggle resonance logic would go here
            // For now, return a mock response
            PostEngagementResponse response = new PostEngagementResponse();
            response.setPostId(postId);
            response.setResonance(42); // Mock value
            response.setResonanced(request.isLiked());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{postId}/collect")
    @Operation(
        summary = "Bookmark/Unbookmark post",
        description = "Toggle bookmark (collect) on a post"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Collection toggled successfully")
    })
    public ResponseEntity<ApiResponse<PostEngagementResponse>> toggleCollect(
        @PathVariable UUID postId,
        @RequestBody PostEngagementRequest request
    ) {
        try {
            // Extract user from authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Toggle collect logic would go here
            // For now, return a mock response
            PostEngagementResponse response = new PostEngagementResponse();
            response.setPostId(postId);
            response.setCollects(8); // Mock value
            response.setCollected(request.isCollected());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/{postId}/view")
    @Operation(
        summary = "Increment view count",
        description = "Increment the view count for a post"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "View count incremented successfully")
    })
    public ResponseEntity<ApiResponse<PostEngagementResponse>> incrementView(@PathVariable UUID postId) {
        try {
            // Extract user from authentication (optional for views)
            UUID currentUserId = null;
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof UUID) {
                    currentUserId = (UUID) authentication.getPrincipal();
                }
            } catch (Exception e) {
                // Views can work without authentication
            }

            // Increment view logic would go here
            // For now, return a mock response
            PostEngagementResponse response = new PostEngagementResponse();
            response.setPostId(postId);
            response.setViews(157); // Mock value

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}

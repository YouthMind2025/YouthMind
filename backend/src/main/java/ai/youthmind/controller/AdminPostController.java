package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.FlaggedPostDto;
import ai.youthmind.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Administrative endpoints for post moderation.
 */
@RestController
@RequestMapping("/admin/v1/posts")
@Tag(name = "Admin Posts", description = "Administrative operations on posts")
@SecurityRequirement(name = "bearer-jwt")
public class AdminPostController {

    @Autowired
    private PostService postService;

    @GetMapping("/flagged")
    @Operation(summary = "Get all flagged posts", description = "Returns a list of posts that have been flagged for moderation")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Flagged posts retrieved successfully")
    })
    public ResponseEntity<Map<String, Object>> getFlaggedPosts() {
        List<FlaggedPostDto> flaggedPosts = postService.getFlaggedPosts();
        Map<String, Object> response = new HashMap<>();
        response.put("flagged_posts", flaggedPosts);
        response.put("total_pending", flaggedPosts.size());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(
        summary = "Delete post (admin)",
        description = "Delete any post by ID. Admin-only endpoint that bypasses ownership checks."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin access required")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> deletePost(
        @Parameter(description = "Post ID", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID id
    ) {
        try {
            // Check if post exists
            if (!postService.getPostById(id).isPresent()) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", "Post not found");
                return ResponseEntity.status(404).body(ApiResponse.error(errorData, 404));
            }

            // Delete the post (admin can delete any post)
            postService.deletePost(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Post deleted successfully by admin");
            response.put("deletedPostId", id.toString());

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", "Failed to delete post");
            errorData.put("details", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error(errorData, 500));
        }
    }
}
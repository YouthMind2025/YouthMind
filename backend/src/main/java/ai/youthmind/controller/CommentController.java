package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.CommentCreateRequest;
import ai.youthmind.dto.CommentDto;
import ai.youthmind.dto.CommentEngagementRequest;
import ai.youthmind.dto.CommentEngagementResponse;
import ai.youthmind.dto.CommentListResponse;
import ai.youthmind.dto.PaginationDto;
import ai.youthmind.entity.Comment;
import ai.youthmind.entity.CommentEngagement;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.service.CommentService;
import ai.youthmind.service.PostService;
import ai.youthmind.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@Tag(name = "Comments", description = "Comment management operations")
@SecurityRequirement(name = "bearer-jwt")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @GetMapping("/posts/{postId}/comments")
    @Operation(
        summary = "Get comments for a post",
        description = "Retrieve paginated comments for a specific post"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comments retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<ApiResponse<CommentListResponse>> getComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        
        try {
            // Check if post exists
            Optional<Post> postOpt = postService.getPostById(postId);
            if (postOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, 404));
            }

            // Get current user for engagement status
            UUID currentUserId = null;
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof UUID) {
                    currentUserId = (UUID) authentication.getPrincipal();
                }
            } catch (Exception e) {
                // User not authenticated, continue without engagement status
            }

            // Get comments with pagination
            Pageable pageable = PageRequest.of(page - 1, limit);
            CommentListResponse response = commentService.getCommentsByPostId(postId, pageable);
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @PostMapping("/posts/{postId}/comments")
    @Operation(
        summary = "Add comment to a post",
        description = "Create a new comment on a specific post"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Comment created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<CommentDto>> addComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CommentCreateRequest request) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Check if post exists
            Optional<Post> postOpt = postService.getPostById(postId);
            if (postOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, 404));
            }

            // Create comment
            Comment comment = new Comment();
            comment.setPostId(postId);
            comment.setAuthorId(currentUserId);
            comment.setContent(request.getContent());
            comment.setParentId(request.getParentId());
            comment.setLikes(0);
            comment.setCreatedAt(LocalDateTime.now());

            Comment savedComment = commentService.saveComment(comment);
            
            // Convert to DTO
            CommentDto commentDto = convertToCommentDto(savedComment, currentUserId);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(commentDto, 201));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(
        summary = "Delete comment",
        description = "Delete a specific comment"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comment deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Not authorized to delete this comment")
    })
    public ResponseEntity<ApiResponse<Object>> deleteComment(@PathVariable UUID commentId) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Check if comment exists
            Optional<Comment> commentOpt = commentService.getCommentById(commentId);
            if (commentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, 404));
            }

            Comment comment = commentOpt.get();
            
            // Check if user owns the comment
            if (!comment.getAuthorId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(null, 403));
            }

            // Delete comment
            commentService.deleteComment(commentId, currentUserId);

            return ResponseEntity.ok(ApiResponse.success(
                Map.of("success", true, "message", "Comment deleted successfully")
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    @PostMapping("/comments/{commentId}/like")
    @Operation(
        summary = "Like/Unlike comment",
        description = "Toggle like status for a comment"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Like status updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<CommentEngagementResponse>> likeComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentEngagementRequest request) {
        
        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(null, 401));
            }
            UUID currentUserId = (UUID) authentication.getPrincipal();

            // Check if comment exists
            Optional<Comment> commentOpt = commentService.getCommentById(commentId);
            if (commentOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(null, 404));
            }

            // Toggle like
            CommentEngagementResponse response = commentService.toggleCommentLike(commentId, request, currentUserId);
            
            return ResponseEntity.ok(ApiResponse.success(response));        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(null, 500));
        }
    }

    private CommentDto convertToCommentDto(Comment comment, UUID currentUserId) {
        // Get author info
        Optional<User> authorOpt = userService.getUserById(comment.getAuthorId());
        String authorName = authorOpt.map(user -> 
            (user.getFirstName() + " " + user.getLastName()).trim()).orElse("Unknown User");

        // Check if current user liked this comment
        boolean isLiked = false;
        if (currentUserId != null) {
            isLiked = commentService.hasUserLikedComment(comment.getId(), currentUserId);
        }

        // Get replies if this is a parent comment
        List<CommentDto> replies = null;
        if (comment.getParentId() == null) {
            replies = commentService.getRepliesByCommentId(comment.getId());
        }

        return new CommentDto(
            comment.getId(),
            comment.getPostId(),
            comment.getAuthorId(),
            authorName,
            comment.getContent(),
            comment.getLikes(),
            isLiked,
            comment.getParentId(),
            replies,
            comment.getCreatedAt()
        );
    }
}

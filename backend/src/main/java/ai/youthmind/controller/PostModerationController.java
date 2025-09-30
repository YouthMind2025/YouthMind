package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.ModerationResult;
import ai.youthmind.entity.Post;
import ai.youthmind.service.PostModerationService;
import ai.youthmind.service.ModerationLoggingService;
import ai.youthmind.service.PostService;
import ai.youthmind.service.WarningService;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.util.ModerationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for post moderation operations.
 * Provides endpoints for manual moderation, flagging management, and moderation analytics.
 */
@RestController
@RequestMapping("/api/moderation/posts")
@Tag(name = "Post Moderation", description = "Endpoints for post moderation and content management")
public class PostModerationController {
    
    @Autowired
    private PostModerationService postModerationService;
    
    @Autowired
    private ModerationLoggingService moderationLoggingService;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private WarningService warningService;
    
    @Autowired
    private PostRepository postRepository;
    
    /**
     * Manually moderate a specific post.
     */
    @PostMapping("/{postId}/moderate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Operation(
        summary = "Manually moderate a post",
        description = "Perform AI moderation on a specific post. Admin/Moderator only."
    )
    public ResponseEntity<ApiResponse<ModerationResult>> moderatePost(
            @PathVariable @Parameter(description = "Post ID", required = true) UUID postId) {
        
        try {
            Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
            
            // Perform AI moderation
            ModerationResult result = postModerationService.moderatePost(post, post.getUserId());
            
            // Log moderation result
            moderationLoggingService.logPostModeration(post, post.getUserId(), result, "manual_moderation");
            
            // Apply moderation actions
            if (!result.isSafe()) {
                post.setFlagged(true);
                post.setFlagReason(result.getReason());
                post.setFlaggedAt(LocalDateTime.now());
                
                // Issue warning if high confidence
                if (ModerationUtils.shouldIssueWarning(result)) {
                    warningService.issueWarning(
                        post.getUserId(),
                        ModerationUtils.mapCategoryToWarningType(ModerationUtils.getPrimaryCategory(result)),
                        ModerationUtils.mapConfidenceToSeverity(result.getConfidence()),
                        "Manual moderation - AI-detected harmful content: " + result.getReason(),
                        null, // System-generated warning
                        postId,
                        null
                    );
                }
            } else {
                post.setFlagged(false);
                post.setFlagReason(null);
                post.setFlaggedAt(null);
            }
            
            postRepository.save(post);
            
            return ResponseEntity.ok(ApiResponse.<ModerationResult>builder()
                .success(true)
                .message("Post moderated successfully")
                .data(result)
                .build());
                
        } catch (Exception e) {
            moderationLoggingService.logModerationError(null, postId, e, "manual_post_moderation");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<ModerationResult>builder()
                    .success(false)
                    .message("Failed to moderate post: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Get all flagged posts.
     */
    @GetMapping("/flagged")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Operation(
        summary = "Get flagged posts",
        description = "Retrieve all posts that have been flagged for review. Admin/Moderator only."
    )
    public ResponseEntity<ApiResponse<List<Post>>> getFlaggedPosts() {
        
        try {
            List<Post> flaggedPosts = postRepository.findByFlaggedTrue();
            
            return ResponseEntity.ok(ApiResponse.<List<Post>>builder()
                .success(true)
                .message("Flagged posts retrieved successfully")
                .data(flaggedPosts)
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<List<Post>>builder()
                    .success(false)
                    .message("Failed to retrieve flagged posts: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Unflag a post (mark as safe).
     */
    @PostMapping("/{postId}/unflag")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Operation(
        summary = "Unflag a post",
        description = "Mark a flagged post as safe and remove flag status. Admin/Moderator only."
    )
    public ResponseEntity<ApiResponse<Void>> unflagPost(
            @PathVariable @Parameter(description = "Post ID", required = true) UUID postId,
            @RequestParam @Parameter(description = "Reason for unflagging", required = true) String reason) {
        
        try {
            Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
            
            post.setFlagged(false);
            post.setFlagReason(null);
            post.setFlaggedAt(null);
            
            postRepository.save(post);
            
            // Log unflagging action
            moderationLoggingService.logPostFlagging(postId, post.getUserId(), false, reason, null);
            
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Post unflagged successfully")
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to unflag post: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Flag a post manually.
     */
    @PostMapping("/{postId}/flag")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Operation(
        summary = "Flag a post",
        description = "Manually flag a post for review. Admin/Moderator only."
    )
    public ResponseEntity<ApiResponse<Void>> flagPost(
            @PathVariable @Parameter(description = "Post ID", required = true) UUID postId,
            @RequestParam @Parameter(description = "Reason for flagging", required = true) String reason) {
        
        try {
            Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
            
            post.setFlagged(true);
            post.setFlagReason(reason);
            post.setFlaggedAt(LocalDateTime.now());
            
            postRepository.save(post);
            
            // Log flagging action
            moderationLoggingService.logPostFlagging(postId, post.getUserId(), true, reason, null);
            
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Post flagged successfully")
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to flag post: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Get moderation statistics.
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
    @Operation(
        summary = "Get moderation statistics",
        description = "Retrieve moderation statistics and analytics. Admin/Moderator only."
    )
    public ResponseEntity<ApiResponse<ModerationStatistics>> getModerationStatistics() {
        
        try {
            long totalPosts = postRepository.count();
            long flaggedPosts = postRepository.countByFlaggedTrue();
            long unflaggedPosts = totalPosts - flaggedPosts;
            
            // Calculate flagging rate
            double flaggingRate = totalPosts > 0 ? (double) flaggedPosts / totalPosts * 100 : 0.0;
            
            ModerationStatistics stats = new ModerationStatistics();
            stats.setTotalPosts(totalPosts);
            stats.setFlaggedPosts(flaggedPosts);
            stats.setUnflaggedPosts(unflaggedPosts);
            stats.setFlaggingRate(flaggingRate);
            
            // Log statistics
            moderationLoggingService.logModerationStatistics(
                (int) totalPosts, 
                (int) flaggedPosts, 
                0, // warnings issued - would need to query warning service
                0.0, // average confidence - would need to calculate from logs
                "current"
            );
            
            return ResponseEntity.ok(ApiResponse.<ModerationStatistics>builder()
                .success(true)
                .message("Moderation statistics retrieved successfully")
                .data(stats)
                .build());
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<ModerationStatistics>builder()
                    .success(false)
                    .message("Failed to retrieve moderation statistics: " + e.getMessage())
                    .build());
        }
    }
    
    
    /**
     * Inner class for moderation statistics.
     */
    public static class ModerationStatistics {
        private long totalPosts;
        private long flaggedPosts;
        private long unflaggedPosts;
        private double flaggingRate;
        
        // Getters and setters
        public long getTotalPosts() {
            return totalPosts;
        }
        
        public void setTotalPosts(long totalPosts) {
            this.totalPosts = totalPosts;
        }
        
        public long getFlaggedPosts() {
            return flaggedPosts;
        }
        
        public void setFlaggedPosts(long flaggedPosts) {
            this.flaggedPosts = flaggedPosts;
        }
        
        public long getUnflaggedPosts() {
            return unflaggedPosts;
        }
        
        public void setUnflaggedPosts(long unflaggedPosts) {
            this.unflaggedPosts = unflaggedPosts;
        }
        
        public double getFlaggingRate() {
            return flaggingRate;
        }
        
        public void setFlaggingRate(double flaggingRate) {
            this.flaggingRate = flaggingRate;
        }
    }
}

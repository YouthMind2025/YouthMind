package ai.youthmind.service;

import ai.youthmind.dto.*;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling moderation operations.
 * Provides methods for submitting content for moderation, checking status, and retrieving history.
 */
@Service
@Transactional
public class ModerationService {
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Submit content for moderation review
     * 
     * @param request The moderation submission request
     * @param moderatorId ID of the admin submitting the request
     * @return ModerationStatusResponse with initial status
     */
    public ModerationStatusResponse submitForModeration(ModerationSubmissionRequest request, UUID moderatorId) {
        // Validate request
        if (!request.isValidContentType()) {
            throw new IllegalArgumentException("Invalid content type: " + request.getContentType());
        }
        
        if (!request.isValidReason()) {
            throw new IllegalArgumentException("Invalid reason: " + request.getReason());
        }
        
        if (!request.isValidPriority()) {
            throw new IllegalArgumentException("Invalid priority: " + request.getPriority());
        }
        
        // Verify content exists
        if ("post".equals(request.getContentType())) {
            Optional<Post> post = postRepository.findById(request.getContentId());
            if (post.isEmpty()) {
                throw new IllegalArgumentException("Post not found with ID: " + request.getContentId());
            }
            
            // Flag the post using existing PostService method
            postService.flagPost(request.getContentId(), request.getReason() + 
                (request.getDetails() != null ? " - " + request.getDetails() : ""));
        } else if ("user".equals(request.getContentType())) {
            Optional<User> user = userRepository.findById(request.getContentId());
            if (user.isEmpty()) {
                throw new IllegalArgumentException("User not found with ID: " + request.getContentId());
            }
            // For user moderation, we would implement user flagging logic here
            // For now, we'll just create a status response
        }
        
        // Create and return status response
        ModerationStatusResponse response = new ModerationStatusResponse();
        response.setContentId(request.getContentId());
        response.setContentType(request.getContentType());
        response.setStatus("pending_review");
        response.setReason(request.getReason());
        response.setPriority(request.getPriority());
        response.setSubmittedAt(LocalDateTime.now());
        response.setEscalationLevel(0);
        response.setModeratorId(moderatorId);
        
        return response;
    }
    
    /**
     * Check moderation status for specific content
     * 
     * @param contentId ID of the content to check
     * @return ModerationStatusResponse with current status
     */
    public ModerationStatusResponse getModerationStatus(UUID contentId) {
        // Check if it's a post
        Optional<Post> post = postRepository.findById(contentId);
        if (post.isPresent()) {
            Post postEntity = post.get();
            
            ModerationStatusResponse response = new ModerationStatusResponse();
            response.setContentId(contentId);
            response.setContentType("post");
            response.setStatus(postEntity.isFlagged() ? "pending_review" : "approved");
            response.setReason(postEntity.getFlagReason());
            response.setSubmittedAt(postEntity.getFlaggedAt());
            response.setEscalationLevel(0);
            
            // If not flagged, it's considered approved
            if (!postEntity.isFlagged()) {
                response.setStatus("approved");
                response.setReviewedAt(postEntity.getUpdatedAt());
            }
            
            return response;
        }
        
        // Check if it's a user (placeholder implementation)
        Optional<User> user = userRepository.findById(contentId);
        if (user.isPresent()) {
            ModerationStatusResponse response = new ModerationStatusResponse();
            response.setContentId(contentId);
            response.setContentType("user");
            response.setStatus("active"); // Default status for users
            response.setEscalationLevel(0);
            return response;
        }
        
        throw new IllegalArgumentException("Content not found with ID: " + contentId);
    }
    
    /**
     * Get moderation history for a specific user
     * 
     * @param userId ID of the user to get history for
     * @return ModerationHistoryResponse with user's moderation history
     */
    public ModerationHistoryResponse getModerationHistory(UUID userId) {
        // Verify user exists
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        // Get all flagged posts by this user
        List<Post> flaggedPosts = postRepository.findByUserIdAndFlaggedTrue(userId);
        
        // Convert to moderation history entries
        List<ModerationHistoryResponse.ModerationHistoryEntry> entries = new ArrayList<>();
        for (Post post : flaggedPosts) {
            ModerationHistoryResponse.ModerationHistoryEntry entry = 
                new ModerationHistoryResponse.ModerationHistoryEntry();
            entry.setContentId(post.getId());
            entry.setContentType("post");
            entry.setReason(post.getFlagReason());
            entry.setStatus(post.isFlagged() ? "pending_review" : "resolved");
            entry.setActionTaken(post.isFlagged() ? "content_hidden" : "none");
            entry.setModeratedAt(post.getFlaggedAt());
            entries.add(entry);
        }
        
        // Create response
        ModerationHistoryResponse response = new ModerationHistoryResponse(userId, entries);
        response.setTotalModerationCases(entries.size());
        response.setPendingCases((int) entries.stream().filter(e -> "pending_review".equals(e.getStatus())).count());
        response.setResolvedCases((int) entries.stream().filter(e -> "resolved".equals(e.getStatus())).count());
        response.setEscalatedCases(0); // Not implemented yet
        response.setUserStatus("active"); // Default status
        response.setRiskScore(calculateRiskScore(entries));
        
        return response;
    }
    
    /**
     * Calculate risk score for a user based on moderation history
     * 
     * @param entries List of moderation history entries
     * @return Risk score from 0-100
     */
    private Integer calculateRiskScore(List<ModerationHistoryResponse.ModerationHistoryEntry> entries) {
        if (entries.isEmpty()) {
            return 0;
        }
        
        int score = 0;
        for (ModerationHistoryResponse.ModerationHistoryEntry entry : entries) {
            // Base score for any moderation action
            score += 10;
            
            // Additional points for severity
            if ("harassment".equals(entry.getReason()) || "hate_speech".equals(entry.getReason())) {
                score += 20;
            } else if ("violence".equals(entry.getReason())) {
                score += 30;
            } else if ("spam".equals(entry.getReason())) {
                score += 5;
            }
            
            // Points for recent activity (within last 30 days)
            if (entry.getModeratedAt() != null && 
                entry.getModeratedAt().isAfter(LocalDateTime.now().minusDays(30))) {
                score += 15;
            }
        }
        
        return Math.min(score, 100); // Cap at 100
    }

    /**
     * Get moderation queue with pagination and filtering
     *
     * @param status Filter by moderation status
     * @param contentType Filter by content type
     * @param pageable Pagination information
     * @return ModerationQueueResponse with paginated results
     */
    public ModerationQueueResponse getModerationQueue(String status, String contentType, Pageable pageable) {
        // Since Post entity doesn't have moderation status field, we'll use flagged status
        Page<Post> flaggedPosts;

        // For now, we'll only handle posts as that's what our entity supports
        if ("post".equals(contentType) || contentType == null) {
            if ("pending".equals(status) || status == null) {
                // Get all flagged posts (pending moderation)
                flaggedPosts = postRepository.findByFlaggedTrue(pageable);
            } else if ("reviewed".equals(status)) {
                // Get posts that were previously flagged but are no longer flagged
                // This would require additional logic or a history table
                // For now, return empty as we don't have this data
                flaggedPosts = new PageImpl<>(new ArrayList<>(), pageable, 0);
            } else {
                // Invalid status
                flaggedPosts = new PageImpl<>(new ArrayList<>(), pageable, 0);
            }
        } else {
            // Only posts are supported for now
            flaggedPosts = new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        // Convert to response
        ModerationQueueResponse response = new ModerationQueueResponse();
        response.setItems(convertToQueueItems(flaggedPosts.getContent()));
        response.setTotalItems(flaggedPosts.getTotalElements());
        response.setTotalPages(flaggedPosts.getTotalPages());
        response.setCurrentPage(flaggedPosts.getNumber());
        response.setPageSize(flaggedPosts.getSize());

        return response;
    }

    /**
     * Update moderation status for content
     *
     * @param request The moderation update request
     * @param moderatorId ID of the admin performing the update
     * @return ModerationStatusResponse with updated status
     */
    public ModerationStatusResponse updateModerationStatus(ModerationUpdateRequest request, UUID moderatorId) {
        // Validate content exists
        Optional<Post> postOpt = postRepository.findById(request.getContentId());
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Content not found with ID: " + request.getContentId());
        }

        Post post = postOpt.get();

        // Update moderation status based on action
        if ("approve".equals(request.getAction())) {
            post.setFlagged(false);
            post.setFlagReason(null);
        } else if ("reject".equals(request.getAction())) {
            // For rejection, we might want to keep it flagged or handle differently
            // For now, we'll just keep it flagged with the rejection reason
            post.setFlagReason("Rejected: " + request.getReason());
        } else if ("delete".equals(request.getAction())) {
            postRepository.delete(post);
        }
        // Add more actions as needed

        // Update the post
        postRepository.save(post);

        // Create response
        ModerationStatusResponse response = new ModerationStatusResponse();
        response.setContentId(post.getId());
        response.setContentType("post");
        response.setStatus(post.isFlagged() ? "pending" : "approved");
        response.setReason(post.getFlagReason());
        response.setReviewedAt(LocalDateTime.now());
        response.setModeratorId(moderatorId);

        return response;
    }

    /**
     * Get moderation statistics
     *
     * @param timeframe Optional timeframe filter
     * @return ModerationStatsResponse with statistics
     */
    public ModerationStatsResponse getModerationStats(String timeframe) {
        ModerationStatsResponse response = new ModerationStatsResponse();

        // Calculate timeframe
        LocalDateTime startDate = calculateStartDate(timeframe);

        // Get statistics based on flagged status
        long totalFlagged = postRepository.countByFlaggedTrueAndFlaggedAtAfter(startDate);
        long totalPosts = postRepository.countByCreatedAtAfter(startDate);

        response.setTotalPending(totalFlagged);
        response.setTotalReviewed(totalPosts - totalFlagged); // Approximation
        response.setTotalApproved(totalPosts - totalFlagged); // Approximation
        response.setTotalRejected(0L); // We don't track this separately
        response.setTotalEscalated(0L); // We don't track this

        // For now, we only have posts
        ContentTypeStats postStats = new ContentTypeStats();
        postStats.setTotal(totalPosts);
        postStats.setPending(totalFlagged);
        postStats.setApproved(totalPosts - totalFlagged);
        postStats.setRejected(0L);

        response.setPostStats(postStats);
        response.setCommentStats(new ContentTypeStats()); // Empty for now
        response.setUserStats(new ContentTypeStats()); // Empty for now

        response.setTimeframe(timeframe != null ? timeframe : "all");
        response.setGeneratedAt(LocalDateTime.now());

        return response;
    }

    /**
     * Convert posts to moderation queue items
     *
     * @param posts List of posts
     * @return List of moderation queue items
     */
    private List<ModerationQueueItem> convertToQueueItems(List<Post> posts) {
        List<ModerationQueueItem> items = new ArrayList<>();

        for (Post post : posts) {
            ModerationQueueItem item = new ModerationQueueItem();
            item.setContentId(post.getId());
            item.setContentType("post");
            item.setTitle(post.getTitle());
            item.setContentPreview(post.getContent().length() > 100 ?
                    post.getContent().substring(0, 100) + "..." : post.getContent());
            item.setAuthorId(post.getUserId());
            item.setStatus(post.isFlagged() ? "pending" : "approved");
            item.setPriority("medium"); // Default priority
            item.setSubmittedAt(post.getFlaggedAt());
            item.setFlagReason(post.getFlagReason());

            items.add(item);
        }

        return items;
    }

    /**
     * Calculate start date based on timeframe
     *
     * @param timeframe Timeframe string
     * @return Start date for filtering
     */
    private LocalDateTime calculateStartDate(String timeframe) {
        if (timeframe == null) {
            return LocalDateTime.of(1970, 1, 1, 0, 0); // Very old date to get all records
        }

        return switch (timeframe) {
            case "24h" -> LocalDateTime.now().minusHours(24);
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusDays(30);
            default -> LocalDateTime.of(1970, 1, 1, 0, 0);
        };
    }

}

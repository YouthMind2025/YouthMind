package ai.youthmind.service;

import ai.youthmind.dto.ModerationHistoryResponse;
import ai.youthmind.dto.ModerationStatusResponse;
import ai.youthmind.dto.ModerationSubmissionRequest;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
}

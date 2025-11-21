package ai.youthmind.service;

import ai.youthmind.dto.*;
import ai.youthmind.entity.Comment;
import ai.youthmind.entity.CommentEngagement;
import ai.youthmind.entity.CommentReport;
import ai.youthmind.entity.User;
import ai.youthmind.repository.CommentEngagementRepository;
import ai.youthmind.repository.CommentReportRepository;
import ai.youthmind.repository.CommentRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentEngagementRepository commentEngagementRepository;

    @Autowired
    private CommentReportRepository commentReportRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentModerationService commentModerationService;

    /**
     * Get comments for a post with pagination
     */
    public CommentListResponse getCommentsByPostId(UUID postId, Pageable pageable, UUID currentUserId) {
        // Get all comments for the post with pagination
        Page<Comment> commentsPage = commentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable);
        
        // Convert to DTOs with replies
        List<CommentDto> commentDtos = commentsPage.getContent().stream()
            .map(comment -> convertToCommentDto(comment, currentUserId))
            .collect(Collectors.toList());

        // Create pagination info
        PaginationDto pagination = new PaginationDto();
        pagination.setPage(pageable.getPageNumber() + 1);
        pagination.setLimit(pageable.getPageSize());
        pagination.setTotal((int) commentsPage.getTotalElements());
        pagination.setTotalItems((int) commentsPage.getTotalElements());
        pagination.setTotalPages(commentsPage.getTotalPages());
        pagination.setHasNext(commentsPage.hasNext());
        pagination.setHasPrevious(commentsPage.hasPrevious());

        CommentListResponse response = new CommentListResponse();
        response.setComments(commentDtos);
        response.setPagination(pagination);

        return response;
    }

    /**
     * Save a comment with moderation
     */
    public Comment saveComment(Comment comment) {
        try {
            logger.info("Saving comment by user: {}", comment.getAuthorId());
            
            // Check if user should be allowed to post comments
            if (!commentModerationService.shouldAllowComment(comment.getAuthorId())) {
                logger.warn("User {} blocked from posting comments due to moderation history", comment.getAuthorId());
                throw new RuntimeException("User is temporarily blocked from posting comments due to moderation history");
            }
            
            // Perform moderation before saving
            ai.youthmind.dto.ModerationResult moderationResult = commentModerationService.moderateComment(comment, comment.getAuthorId());
            
            // If moderation result is not safe, handle accordingly
            if (!moderationResult.isSafe()) {
                logger.warn("Comment flagged as unsafe: {}", moderationResult.getReason());
                
                // For now, we'll still save the comment but log the flag
                // In a real implementation, you might:
                // - Save with a flag
                // - Queue for human review
                // - Block the comment entirely
                // - Send warning to user
            }
            
            // Save the comment
            Comment savedComment = commentRepository.save(comment);
            
            logger.info("Comment saved successfully with moderation result - Safe: {}, Confidence: {}", 
                moderationResult.isSafe(), moderationResult.getConfidence());
            
            return savedComment;
            
        } catch (Exception e) {
            logger.error("Error saving comment with moderation: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Save a comment without moderation (for internal use)
     */
    public Comment saveCommentWithoutModeration(Comment comment) {
        return commentRepository.save(comment);
    }

    /**
     * Get comment by ID
     */
    public Optional<Comment> getCommentById(UUID commentId) {
        return commentRepository.findById(commentId);
    }

    /**
     * Check if user has liked a comment
     */
    public boolean hasUserLikedComment(UUID userId, UUID commentId) {
        Optional<CommentEngagement> engagement = commentEngagementRepository
            .findByUserIdAndCommentIdAndType(userId, commentId, CommentEngagement.EngagementType.LIKE);
        return engagement.isPresent();
    }

    /**
     * Get replies by comment ID
     */
    public List<CommentDto> getRepliesByCommentId(UUID commentId, UUID currentUserId) {
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
        return replies.stream()
            .map(reply -> convertToCommentDto(reply, currentUserId))
            .collect(Collectors.toList());
    }

    /**
     * Get comments for a post with pagination
     */
    public CommentListResponse getComments(UUID postId, int page, int limit, UUID currentUserId) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        
        // Get top-level comments
        List<Comment> topLevelComments = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(postId);
        
        // Convert to DTOs with replies
        List<CommentDto> commentDtos = topLevelComments.stream()
            .map(comment -> convertToCommentDto(comment, currentUserId))
            .collect(Collectors.toList());

        // Create pagination info
        PaginationDto pagination = new PaginationDto();
        pagination.setPage(page);
        pagination.setLimit(limit);
        pagination.setTotalItems((int) commentRepository.countByPostId(postId));
        pagination.setTotal((int) commentRepository.countByPostId(postId));
        pagination.setTotalPages((int) Math.ceil((double) commentRepository.countByPostId(postId) / limit));
        pagination.setHasNext(page * limit < commentRepository.countByPostId(postId));
        pagination.setHasPrevious(page > 1);

        CommentListResponse response = new CommentListResponse();
        response.setComments(commentDtos);
        response.setPagination(pagination);

        return response;
    }

    /**
     * Create a new comment
     */
    public CommentCreateResponse createComment(UUID postId, CommentCreateRequest request, UUID currentUserId) {
        // Verify post exists
        if (!postRepository.existsById(postId)) {
            throw new RuntimeException("Post not found");
        }

        // Verify parent comment exists if provided
        if (request.getParentId() != null && !commentRepository.existsById(request.getParentId())) {
            throw new RuntimeException("Parent comment not found");
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setAuthorId(currentUserId);
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        comment.setLikes(0);

        Comment savedComment = commentRepository.save(comment);

        return convertToCommentCreateResponse(savedComment);
    }

    /**
     * Delete a comment
     */
    public void deleteComment(UUID commentId, UUID currentUserId) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            throw new RuntimeException("Comment not found");
        }

        Comment comment = commentOpt.get();
        if (!comment.getAuthorId().equals(currentUserId)) {
            throw new RuntimeException("Not authorized to delete this comment");
        }

        // Delete all replies first
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
        commentRepository.deleteAll(replies);

        // Delete comment engagements
        commentEngagementRepository.deleteByCommentId(commentId);

        // Delete the comment
        commentRepository.delete(comment);
    }

    /**
     * Like/unlike a comment
     */
    public CommentEngagementResponse toggleCommentLike(UUID commentId, CommentEngagementRequest request, UUID currentUserId) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            throw new RuntimeException("Comment not found");
        }

        Comment comment = commentOpt.get();
        boolean liked = request.isLiked();

        if (liked) {
            // Check if already liked
            Optional<CommentEngagement> existingEngagement = commentEngagementRepository
                .findByUserIdAndCommentIdAndType(currentUserId, commentId, CommentEngagement.EngagementType.LIKE);

            if (existingEngagement.isEmpty()) {
                // Create new like
                CommentEngagement engagement = new CommentEngagement();
                engagement.setUserId(currentUserId);
                engagement.setCommentId(commentId);
                engagement.setType(CommentEngagement.EngagementType.LIKE);
                commentEngagementRepository.save(engagement);

                // Update comment like count
                comment.setLikes(comment.getLikes() + 1);
                commentRepository.save(comment);
            }
        } else {
            // Remove like
            Optional<CommentEngagement> existingEngagement = commentEngagementRepository
                .findByUserIdAndCommentIdAndType(currentUserId, commentId, CommentEngagement.EngagementType.LIKE);

            if (existingEngagement.isPresent()) {
                commentEngagementRepository.delete(existingEngagement.get());

                // Update comment like count
                comment.setLikes(Math.max(0, comment.getLikes() - 1));
                commentRepository.save(comment);
            }
        }

        CommentEngagementResponse response = new CommentEngagementResponse();
        response.setCommentId(commentId);
        response.setLikes(comment.getLikes());
        response.setLiked(liked);

        return response;
    }

    // Helper methods
    private CommentDto convertToCommentDto(Comment comment, UUID currentUserId) {
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPostId());
        dto.setAuthorId(comment.getAuthorId());
        dto.setContent(comment.getContent());
        dto.setLikes(comment.getLikes());
        dto.setParentId(comment.getParentId());
        dto.setCreatedAt(comment.getCreatedAt());

        // Set author information
        userRepository.findById(comment.getAuthorId()).ifPresent(user -> {
            dto.setAuthor(user.getFirstName() + " " + user.getLastName());
        });

        // Set engagement flags
        Optional<CommentEngagement> userLike = commentEngagementRepository
            .findByUserIdAndCommentIdAndType(currentUserId, comment.getId(), CommentEngagement.EngagementType.LIKE);
        dto.setLiked(userLike.isPresent());

        // Get replies
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(comment.getId());
        List<CommentDto> replyDtos = replies.stream()
            .map(reply -> convertToCommentDto(reply, currentUserId))
            .collect(Collectors.toList());
        dto.setReplies(replyDtos);

        return dto;
    }

    private CommentCreateResponse convertToCommentCreateResponse(Comment comment) {
        CommentCreateResponse response = new CommentCreateResponse();
        response.setId(comment.getId());
        response.setPostId(comment.getPostId());
        response.setAuthorId(comment.getAuthorId());
        response.setContent(comment.getContent());
        response.setLikes(comment.getLikes());
        response.setLiked(false);
        response.setParentId(comment.getParentId());
        response.setCreatedAt(comment.getCreatedAt());

        // Set author name
        userRepository.findById(comment.getAuthorId()).ifPresent(user -> {
            response.setAuthorName(user.getFirstName() + " " + user.getLastName());
        });

        return response;
    }

    /**
     * Report a comment for inappropriate content
     */
    public CommentReportResponse reportComment(UUID commentId, CommentReportRequest request, UUID reporterId) {
        // Verify comment exists
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            throw new RuntimeException("Comment not found");
        }

        // Check if user has already reported this comment
        if (commentReportRepository.existsByCommentIdAndReporterId(commentId, reporterId)) {
            throw new RuntimeException("You have already reported this comment");
        }

        // Create report
        CommentReport report = new CommentReport();
        report.setCommentId(commentId);
        report.setReporterId(reporterId);
        report.setReportReason(request.getReportReason());
        report.setDescription(request.getDescription());
        report.setStatus(CommentReport.ReportStatus.PENDING);

        CommentReport savedReport = commentReportRepository.save(report);
        return new CommentReportResponse(savedReport);
    }
}

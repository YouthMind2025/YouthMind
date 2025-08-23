package ai.youthmind.service;

import ai.youthmind.dto.*;
import ai.youthmind.entity.Comment;
import ai.youthmind.entity.CommentEngagement;
import ai.youthmind.entity.User;
import ai.youthmind.repository.CommentEngagementRepository;
import ai.youthmind.repository.CommentRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentEngagementRepository commentEngagementRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get comments for a post with pagination
     */
    public CommentListResponse getCommentsByPostId(UUID postId, Pageable pageable) {
        // Get all comments for the post with pagination
        Page<Comment> commentsPage = commentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable);
        
        // Convert to DTOs with replies
        List<CommentDto> commentDtos = commentsPage.getContent().stream()
            .map(comment -> convertToCommentDto(comment, null)) // TODO: Pass actual currentUserId
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
     * Save a comment
     */
    public Comment saveComment(Comment comment) {
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
    public List<CommentDto> getRepliesByCommentId(UUID commentId) {
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId);
        return replies.stream()
            .map(reply -> convertToCommentDto(reply, null)) // TODO: Pass actual currentUserId
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
            dto.setAuthorName(user.getFirstName() + " " + user.getLastName());
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
}

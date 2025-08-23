package ai.youthmind.repository;

import ai.youthmind.entity.CommentEngagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentEngagementRepository extends JpaRepository<CommentEngagement, UUID> {
    
    Optional<CommentEngagement> findByUserIdAndCommentIdAndType(UUID userId, UUID commentId, CommentEngagement.EngagementType type);
    
    List<CommentEngagement> findByUserIdAndType(UUID userId, CommentEngagement.EngagementType type);
    
    List<CommentEngagement> findByCommentIdAndType(UUID commentId, CommentEngagement.EngagementType type);
    
    @Query("SELECT COUNT(ce) FROM CommentEngagement ce WHERE ce.commentId = :commentId AND ce.type = :type")
    long countByCommentIdAndType(@Param("commentId") UUID commentId, @Param("type") CommentEngagement.EngagementType type);
    
    @Query("SELECT COUNT(ce) FROM CommentEngagement ce WHERE ce.userId = :userId AND ce.type = :type")
    long countByUserIdAndType(@Param("userId") UUID userId, @Param("type") CommentEngagement.EngagementType type);
    
    void deleteByUserIdAndCommentIdAndType(UUID userId, UUID commentId, CommentEngagement.EngagementType type);
    
    void deleteByCommentId(UUID commentId);
    
    void deleteByUserId(UUID userId);
}

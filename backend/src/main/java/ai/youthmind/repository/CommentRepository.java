package ai.youthmind.repository;

import ai.youthmind.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    
    Page<Comment> findByPostIdOrderByCreatedAtDesc(UUID postId, Pageable pageable);
    
    List<Comment> findByPostIdAndParentIdIsNullOrderByCreatedAtDesc(UUID postId);

    List<Comment> findByAuthorIdAndParentIdIsNullOrderByCreatedAtDesc(UUID autherId);
    
    List<Comment> findByParentIdOrderByCreatedAtAsc(UUID parentId);
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.postId = :postId")
    long countByPostId(@Param("postId") UUID postId);
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.authorId = :authorId")
    long countByAuthorId(@Param("authorId") UUID authorId);
    
    void deleteByPostId(UUID postId);
    
    void deleteByAuthorId(UUID authorId);
    
    /**
     * Find comments by multiple post IDs ordered by creation date
     */
    List<Comment> findByPostIdInOrderByCreatedAtDesc(List<UUID> postIds);
}

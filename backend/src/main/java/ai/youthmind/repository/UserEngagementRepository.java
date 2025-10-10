package ai.youthmind.repository;

import ai.youthmind.entity.UserEngagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserEngagementRepository extends JpaRepository<UserEngagement, UUID> {
    
    Optional<UserEngagement> findByUserIdAndPostIdAndType(UUID userId, UUID postId, UserEngagement.EngagementType type);
    
    List<UserEngagement> findByUserIdAndType(UUID userId, UserEngagement.EngagementType type);
    
    List<UserEngagement> findByPostIdAndType(UUID postId, UserEngagement.EngagementType type);
    
    @Query("SELECT COUNT(ue) FROM UserEngagement ue WHERE ue.postId = :postId AND ue.type = :type")
    long countByPostIdAndType(@Param("postId") UUID postId, @Param("type") UserEngagement.EngagementType type);
    
    @Query("SELECT COUNT(ue) FROM UserEngagement ue WHERE ue.userId = :userId AND ue.type = :type")
    long countByUserIdAndType(@Param("userId") UUID userId, @Param("type") UserEngagement.EngagementType type);
    
    void deleteByUserIdAndPostIdAndType(UUID userId, UUID postId, UserEngagement.EngagementType type);
    
    void deleteByPostId(UUID postId);
    
    void deleteByUserId(UUID userId);
}

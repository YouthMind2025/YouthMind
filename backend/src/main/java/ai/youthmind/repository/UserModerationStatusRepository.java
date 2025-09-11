package ai.youthmind.repository;

import ai.youthmind.entity.UserModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing user moderation status records.
 */
@Repository
public interface UserModerationStatusRepository extends JpaRepository<UserModerationStatus, UUID> {
    
    /**
     * Find moderation status by user ID.
     */
    @Query("SELECT ums FROM UserModerationStatus ums WHERE ums.user.id = :userId")
    Optional<UserModerationStatus> findByUserId(@Param("userId") UUID userId);
    
    /**
     * Find users by moderation status.
     */
    List<UserModerationStatus> findByStatusOrderByUpdatedAtDesc(UserModerationStatus.ModerationStatus status);
    
    /**
     * Find users by warning level.
     */
    List<UserModerationStatus> findByWarningLevelOrderByUpdatedAtDesc(Integer warningLevel);
    
    /**
     * Find users requiring manual review.
     */
    List<UserModerationStatus> findByManualReviewRequiredTrueOrderByUpdatedAtDesc();
    
    /**
     * Find users with auto-moderation disabled.
     */
    List<UserModerationStatus> findByAutoModerationEnabledFalseOrderByUpdatedAtDesc();
    
    /**
     * Find users by status and warning level.
     */
    List<UserModerationStatus> findByStatusAndWarningLevelOrderByUpdatedAtDesc(
        UserModerationStatus.ModerationStatus status, Integer warningLevel);
    
    /**
     * Count users by moderation status.
     */
    long countByStatus(UserModerationStatus.ModerationStatus status);
    
    /**
     * Count users by warning level.
     */
    long countByWarningLevel(Integer warningLevel);
    
    /**
     * Count users requiring manual review.
     */
    long countByManualReviewRequiredTrue();
    
    /**
     * Count users with auto-moderation disabled.
     */
    long countByAutoModerationEnabledFalse();
    
    /**
     * Find users with warnings in a specific date range.
     */
    @Query("SELECT ums FROM UserModerationStatus ums WHERE ums.lastWarningAt BETWEEN :startDate AND :endDate ORDER BY ums.lastWarningAt DESC")
    List<UserModerationStatus> findUsersWithWarningsInDateRange(@Param("startDate") LocalDateTime startDate, 
                                                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find users with moderation activity in a specific date range.
     */
    @Query("SELECT ums FROM UserModerationStatus ums WHERE ums.lastModerationAt BETWEEN :startDate AND :endDate ORDER BY ums.lastModerationAt DESC")
    List<UserModerationStatus> findUsersWithModerationActivityInDateRange(@Param("startDate") LocalDateTime startDate, 
                                                                         @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find users with high warning counts (top 10).
     */
    @Query("SELECT ums FROM UserModerationStatus ums ORDER BY ums.totalWarnings DESC, ums.updatedAt DESC")
    List<UserModerationStatus> findTopUsersByWarningCount(@Param("limit") int limit);
    
    /**
     * Find users with high ban counts (top 10).
     */
    @Query("SELECT ums FROM UserModerationStatus ums ORDER BY ums.totalBans DESC, ums.updatedAt DESC")
    List<UserModerationStatus> findTopUsersByBanCount(@Param("limit") int limit);
    
    /**
     * Find users who haven't been moderated recently (potential cleanup candidates).
     */
    @Query("SELECT ums FROM UserModerationStatus ums WHERE ums.lastModerationAt < :cutoffDate ORDER BY ums.lastModerationAt ASC")
    List<UserModerationStatus> findUsersNotModeratedRecently(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find users with specific warning level range.
     */
    @Query("SELECT ums FROM UserModerationStatus ums WHERE ums.warningLevel BETWEEN :minLevel AND :maxLevel ORDER BY ums.warningLevel DESC, ums.updatedAt DESC")
    List<UserModerationStatus> findUsersByWarningLevelRange(@Param("minLevel") Integer minLevel, 
                                                           @Param("maxLevel") Integer maxLevel);
    
    /**
     * Check if user exists in moderation status table.
     */
    @Query("SELECT COUNT(ums) > 0 FROM UserModerationStatus ums WHERE ums.user.id = :userId")
    boolean existsByUserId(@Param("userId") UUID userId);
}

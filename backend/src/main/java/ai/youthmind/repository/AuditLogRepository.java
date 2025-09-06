package ai.youthmind.repository;

import ai.youthmind.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);
    
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);
    
    List<AuditLog> findByUserId(UUID userId);
    
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);
    
    List<AuditLog> findByAction(AuditLog.Action action);
    
    Page<AuditLog> findByAction(AuditLog.Action action, Pageable pageable);
    
    @Query("SELECT al FROM AuditLog al WHERE al.createdAt BETWEEN :startDate AND :endDate ORDER BY al.createdAt DESC")
    List<AuditLog> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT al FROM AuditLog al WHERE al.createdAt BETWEEN :startDate AND :endDate ORDER BY al.createdAt DESC")
    Page<AuditLog> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate, 
                                         Pageable pageable);
    
    @Query("SELECT al FROM AuditLog al WHERE al.userId = :userId AND al.createdAt >= :since ORDER BY al.createdAt DESC")
    List<AuditLog> findRecentActivityByUser(@Param("userId") UUID userId, 
                                           @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.entityType = :entityType AND al.action = :action AND al.createdAt >= :since")
    Long countByEntityTypeAndActionSince(@Param("entityType") String entityType, 
                                        @Param("action") AuditLog.Action action, 
                                        @Param("since") LocalDateTime since);
    
    // Clean up old audit logs (for data retention)
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}

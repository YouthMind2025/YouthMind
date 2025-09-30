package ai.youthmind.repository;

import ai.youthmind.entity.UserConsent;
import ai.youthmind.entity.UserConsent.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserConsent entity operations.
 */
@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {
    
    /**
     * Find consent requests by user ID and status
     */
    List<UserConsent> findByUserIdAndStatus(UUID userId, ConsentStatus status);
    
    /**
     * Find pending consent requests for a user that haven't expired
     */
    List<UserConsent> findByUserIdAndStatusAndExpiresAtAfter(UUID userId, ConsentStatus status, LocalDateTime expiresAt);
    
    /**
     * Find consent requests by user ID ordered by requested date (descending)
     */
    List<UserConsent> findByUserIdOrderByRequestedAtDesc(UUID userId);
    
    /**
     * Find consent for specific user and content
     */
    Optional<UserConsent> findByUserIdAndContentId(UUID userId, UUID contentId);
    
    /**
     * Find all consent requests for a user
     */
    List<UserConsent> findByUserId(UUID userId);
    
    /**
     * Find expired consent requests with specific status
     */
    List<UserConsent> findByExpiresAtBeforeAndStatus(LocalDateTime expiresAt, ConsentStatus status);
    
    /**
     * Count consent requests by user and status
     */
    long countByUserIdAndStatus(UUID userId, ConsentStatus status);
    
    /**
     * Find consent requests by content ID
     */
    List<UserConsent> findByContentId(UUID contentId);
    
    /**
     * Find consent requests by content type and status
     */
    List<UserConsent> findByContentTypeAndStatus(UserConsent.ConsentType contentType, ConsentStatus status);
    
    /**
     * Find consent requests that will expire soon (within specified hours)
     */
    @Query("SELECT uc FROM UserConsent uc WHERE uc.status = :status AND uc.expiresAt BETWEEN :now AND :expiryTime")
    List<UserConsent> findExpiringSoon(@Param("status") ConsentStatus status, 
                                      @Param("now") LocalDateTime now, 
                                      @Param("expiryTime") LocalDateTime expiryTime);
    
    /**
     * Find consent requests created within a date range
     */
    @Query("SELECT uc FROM UserConsent uc WHERE uc.createdAt BETWEEN :startDate AND :endDate ORDER BY uc.createdAt DESC")
    List<UserConsent> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get consent statistics for a user
     */
    @Query("SELECT " +
           "COUNT(uc) as totalRequests, " +
           "SUM(CASE WHEN uc.status = 'ACCEPTED' THEN 1 ELSE 0 END) as acceptedRequests, " +
           "SUM(CASE WHEN uc.status = 'REJECTED' THEN 1 ELSE 0 END) as rejectedRequests, " +
           "SUM(CASE WHEN uc.status = 'MODIFIED' THEN 1 ELSE 0 END) as modifiedRequests, " +
           "SUM(CASE WHEN uc.status = 'EXPIRED' THEN 1 ELSE 0 END) as expiredRequests, " +
           "SUM(CASE WHEN uc.status = 'PENDING' THEN 1 ELSE 0 END) as pendingRequests " +
           "FROM UserConsent uc WHERE uc.userId = :userId")
    Object[] getConsentStatistics(@Param("userId") UUID userId);
}

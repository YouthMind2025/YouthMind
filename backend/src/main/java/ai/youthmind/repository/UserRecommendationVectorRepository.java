package ai.youthmind.repository;

import ai.youthmind.entity.UserRecommendationVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserRecommendationVector entities
 * Provides data access methods for recommendation vector management
 */
@Repository
public interface UserRecommendationVectorRepository extends JpaRepository<UserRecommendationVector, UUID> {
    
    /**
     * Find the current (latest version) recommendation vector for a user
     * Uses Spring Data JPA's findFirstBy pattern to automatically limit to 1 result
     * This prevents NonUniqueResultException when multiple vectors exist for the same user
     */
    Optional<UserRecommendationVector> findFirstByUserIdOrderByVersionDesc(UUID userId);
    
    /**
     * @deprecated Use findFirstByUserIdOrderByVersionDesc instead. This method is kept for backward compatibility
     * but may throw NonUniqueResultException if multiple vectors exist.
     */
    @Deprecated
    @Query("SELECT rv FROM UserRecommendationVector rv WHERE rv.userId = :userId ORDER BY rv.version DESC")
    Optional<UserRecommendationVector> findCurrentByUserId(@Param("userId") UUID userId);
    
    /**
     * Find all recommendation vectors for a user, ordered by version descending
     */
    List<UserRecommendationVector> findByUserIdOrderByVersionDesc(UUID userId);
    
    /**
     * Find recommendation vector by user ID and specific version
     */
    Optional<UserRecommendationVector> findByUserIdAndVersion(UUID userId, Integer version);
    
    /**
     * Find the latest version number for a user
     */
    @Query("SELECT MAX(rv.version) FROM UserRecommendationVector rv WHERE rv.userId = :userId")
    Optional<Integer> findLatestVersionByUserId(@Param("userId") UUID userId);
    
    /**
     * Count total recommendation vectors for a user
     */
    long countByUserId(UUID userId);
    
    /**
     * Find recommendation vectors older than specified date for cleanup
     */
    @Query("SELECT rv FROM UserRecommendationVector rv WHERE rv.createdAt < :cutoffDate ORDER BY rv.createdAt ASC")
    List<UserRecommendationVector> findOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find recommendation vectors for a user older than specified date
     */
    @Query("SELECT rv FROM UserRecommendationVector rv WHERE rv.userId = :userId AND rv.createdAt < :cutoffDate ORDER BY rv.createdAt ASC")
    List<UserRecommendationVector> findByUserIdAndOlderThan(@Param("userId") UUID userId, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find recommendation vectors for a user with version less than specified version
     */
    @Query("SELECT rv FROM UserRecommendationVector rv WHERE rv.userId = :userId AND rv.version < :version ORDER BY rv.version ASC")
    List<UserRecommendationVector> findByUserIdAndVersionLessThan(@Param("userId") UUID userId, @Param("version") Integer version);
    
    /**
     * Delete recommendation vectors older than specified date
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserRecommendationVector rv WHERE rv.createdAt < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Delete recommendation vectors for a user older than specified date
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserRecommendationVector rv WHERE rv.userId = :userId AND rv.createdAt < :cutoffDate")
    int deleteByUserIdAndOlderThan(@Param("userId") UUID userId, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Delete recommendation vectors for a user with version less than specified version
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserRecommendationVector rv WHERE rv.userId = :userId AND rv.version < :version")
    int deleteByUserIdAndVersionLessThan(@Param("userId") UUID userId, @Param("version") Integer version);
    
    /**
     * Delete all recommendation vectors for a user
     */
    @Modifying
    @Transactional
    void deleteByUserId(UUID userId);
    
    /**
     * Check if a user has any recommendation vectors
     */
    boolean existsByUserId(UUID userId);
    
    /**
     * Find recommendation vectors created between two dates
     */
    @Query("SELECT rv FROM UserRecommendationVector rv WHERE rv.createdAt BETWEEN :startDate AND :endDate ORDER BY rv.createdAt DESC")
    List<UserRecommendationVector> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find recommendation vectors for a user created between two dates
     */
    @Query("SELECT rv FROM UserRecommendationVector rv WHERE rv.userId = :userId AND rv.createdAt BETWEEN :startDate AND :endDate ORDER BY rv.createdAt DESC")
    List<UserRecommendationVector> findByUserIdAndCreatedAtBetween(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get statistics about recommendation vectors
     */
    @Query("SELECT COUNT(rv), AVG(rv.version), MIN(rv.createdAt), MAX(rv.createdAt) FROM UserRecommendationVector rv WHERE rv.userId = :userId")
    Object[] getStatisticsByUserId(@Param("userId") UUID userId);
}

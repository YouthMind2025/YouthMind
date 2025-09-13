package ai.youthmind.repository;

import ai.youthmind.entity.AiApiUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for AI API usage tracking and analytics.
 */
@Repository
public interface AiApiUsageRepository extends JpaRepository<AiApiUsage, UUID> {

    /**
     * Find usage records by user ID.
     */
    List<AiApiUsage> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find usage records by user ID with pagination.
     */
    Page<AiApiUsage> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find usage records by request type.
     */
    List<AiApiUsage> findByRequestTypeOrderByCreatedAtDesc(AiApiUsage.RequestType requestType);

    /**
     * Find usage records by model name.
     */
    List<AiApiUsage> findByModelNameOrderByCreatedAtDesc(String modelName);

    /**
     * Find usage records by success status.
     */
    List<AiApiUsage> findBySuccessOrderByCreatedAtDesc(Boolean success);

    /**
     * Find usage records by session ID.
     */
    List<AiApiUsage> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * Find usage records within date range.
     */
    List<AiApiUsage> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find usage records by user and date range.
     */
    List<AiApiUsage> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find usage records by request type and date range.
     */
    List<AiApiUsage> findByRequestTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
            AiApiUsage.RequestType requestType, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find usage records by model and date range.
     */
    List<AiApiUsage> findByModelNameAndCreatedAtBetweenOrderByCreatedAtDesc(
            String modelName, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Calculate total cost for a user.
     */
    @Query("SELECT COALESCE(SUM(a.totalCost), 0) FROM AiApiUsage a WHERE a.userId = :userId")
    BigDecimal calculateTotalCostByUserId(@Param("userId") UUID userId);

    /**
     * Calculate total cost for a user within date range.
     */
    @Query("SELECT COALESCE(SUM(a.totalCost), 0) FROM AiApiUsage a WHERE a.userId = :userId AND a.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalCostByUserIdAndDateRange(
            @Param("userId") UUID userId, 
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total cost for a request type.
     */
    @Query("SELECT COALESCE(SUM(a.totalCost), 0) FROM AiApiUsage a WHERE a.requestType = :requestType")
    BigDecimal calculateTotalCostByRequestType(@Param("requestType") AiApiUsage.RequestType requestType);

    /**
     * Calculate total cost for a model.
     */
    @Query("SELECT COALESCE(SUM(a.totalCost), 0) FROM AiApiUsage a WHERE a.modelName = :modelName")
    BigDecimal calculateTotalCostByModelName(@Param("modelName") String modelName);

    /**
     * Calculate total cost within date range.
     */
    @Query("SELECT COALESCE(SUM(a.totalCost), 0) FROM AiApiUsage a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalCostByDateRange(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count total requests for a user.
     */
    @Query("SELECT COUNT(a) FROM AiApiUsage a WHERE a.userId = :userId")
    Long countRequestsByUserId(@Param("userId") UUID userId);

    /**
     * Count total requests for a user within date range.
     */
    @Query("SELECT COUNT(a) FROM AiApiUsage a WHERE a.userId = :userId AND a.createdAt BETWEEN :startDate AND :endDate")
    Long countRequestsByUserIdAndDateRange(
            @Param("userId") UUID userId, 
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count total requests by request type.
     */
    @Query("SELECT COUNT(a) FROM AiApiUsage a WHERE a.requestType = :requestType")
    Long countRequestsByRequestType(@Param("requestType") AiApiUsage.RequestType requestType);

    /**
     * Count total requests by model.
     */
    @Query("SELECT COUNT(a) FROM AiApiUsage a WHERE a.modelName = :modelName")
    Long countRequestsByModelName(@Param("modelName") String modelName);

    /**
     * Count successful requests for a user.
     */
    @Query("SELECT COUNT(a) FROM AiApiUsage a WHERE a.userId = :userId AND a.success = true")
    Long countSuccessfulRequestsByUserId(@Param("userId") UUID userId);

    /**
     * Count failed requests for a user.
     */
    @Query("SELECT COUNT(a) FROM AiApiUsage a WHERE a.userId = :userId AND a.success = false")
    Long countFailedRequestsByUserId(@Param("userId") UUID userId);

    /**
     * Calculate average request duration for a user.
     */
    @Query("SELECT AVG(a.requestDurationMs) FROM AiApiUsage a WHERE a.userId = :userId")
    Double calculateAverageRequestDurationByUserId(@Param("userId") UUID userId);

    /**
     * Calculate average request duration by request type.
     */
    @Query("SELECT AVG(a.requestDurationMs) FROM AiApiUsage a WHERE a.requestType = :requestType")
    Double calculateAverageRequestDurationByRequestType(@Param("requestType") AiApiUsage.RequestType requestType);

    /**
     * Calculate total tokens used by a user.
     */
    @Query("SELECT COALESCE(SUM(a.totalTokens), 0) FROM AiApiUsage a WHERE a.userId = :userId")
    Long calculateTotalTokensByUserId(@Param("userId") UUID userId);

    /**
     * Calculate total tokens used by a user within date range.
     */
    @Query("SELECT COALESCE(SUM(a.totalTokens), 0) FROM AiApiUsage a WHERE a.userId = :userId AND a.createdAt BETWEEN :startDate AND :endDate")
    Long calculateTotalTokensByUserIdAndDateRange(
            @Param("userId") UUID userId, 
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get top users by cost.
     */
    @Query("SELECT a.userId, SUM(a.totalCost) as totalCost FROM AiApiUsage a WHERE a.userId IS NOT NULL GROUP BY a.userId ORDER BY totalCost DESC")
    List<Object[]> findTopUsersByCost(Pageable pageable);

    /**
     * Get top users by token usage.
     */
    @Query("SELECT a.userId, SUM(a.totalTokens) as totalTokens FROM AiApiUsage a WHERE a.userId IS NOT NULL GROUP BY a.userId ORDER BY totalTokens DESC")
    List<Object[]> findTopUsersByTokens(Pageable pageable);

    /**
     * Get usage statistics by request type.
     */
    @Query("SELECT a.requestType, COUNT(a) as requestCount, SUM(a.totalCost) as totalCost, AVG(a.requestDurationMs) as avgDuration FROM AiApiUsage a GROUP BY a.requestType")
    List<Object[]> getUsageStatisticsByRequestType();

    /**
     * Get usage statistics by model.
     */
    @Query("SELECT a.modelName, COUNT(a) as requestCount, SUM(a.totalCost) as totalCost, AVG(a.requestDurationMs) as avgDuration FROM AiApiUsage a GROUP BY a.modelName")
    List<Object[]> getUsageStatisticsByModel();

    /**
     * Get daily usage statistics.
     */
    @Query("SELECT DATE(a.createdAt) as date, COUNT(a) as requestCount, SUM(a.totalCost) as totalCost, SUM(a.totalTokens) as totalTokens FROM AiApiUsage a WHERE a.createdAt BETWEEN :startDate AND :endDate GROUP BY DATE(a.createdAt) ORDER BY date")
    List<Object[]> getDailyUsageStatistics(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get hourly usage statistics for a specific day.
     */
    @Query("SELECT HOUR(a.createdAt) as hour, COUNT(a) as requestCount, SUM(a.totalCost) as totalCost FROM AiApiUsage a WHERE DATE(a.createdAt) = DATE(:date) GROUP BY HOUR(a.createdAt) ORDER BY hour")
    List<Object[]> getHourlyUsageStatistics(@Param("date") LocalDateTime date);
}

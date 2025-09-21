package ai.youthmind.repository;

import ai.youthmind.entity.VectorEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for VectorEmbedding entity.
 * Provides advanced queries for vector embedding management and analytics.
 */
@Repository
public interface VectorEmbeddingRepository extends JpaRepository<VectorEmbedding, UUID> {

    // Basic queries
    Optional<VectorEmbedding> findByQdrantPointId(String qdrantPointId);
    List<VectorEmbedding> findByContentIdAndContentType(UUID contentId, VectorEmbedding.ContentType contentType);
    List<VectorEmbedding> findByCollectionName(String collectionName);
    List<VectorEmbedding> findByModel(String model);
    
    // Delete methods
    void deleteByContentIdAndContentType(UUID contentId, VectorEmbedding.ContentType contentType);

    // Active embeddings
    List<VectorEmbedding> findByIsActiveTrue();
    List<VectorEmbedding> findByIsActiveTrueAndContentType(VectorEmbedding.ContentType contentType);
    List<VectorEmbedding> findByIsActiveTrueAndCollectionName(String collectionName);

    // Expired embeddings
    List<VectorEmbedding> findByExpiresAtBefore(LocalDateTime now);
    List<VectorEmbedding> findByIsActiveTrueAndExpiresAtBefore(LocalDateTime now);

    // Quality-based queries
    List<VectorEmbedding> findByQualityScoreGreaterThan(Double minQualityScore);
    List<VectorEmbedding> findByQualityScoreLessThan(Double maxQualityScore);
    List<VectorEmbedding> findByQualityScoreBetween(Double minQualityScore, Double maxQualityScore);

    // Time-based queries
    List<VectorEmbedding> findByCreatedAtAfter(LocalDateTime after);
    List<VectorEmbedding> findByCreatedAtBefore(LocalDateTime before);
    List<VectorEmbedding> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Complex queries for analytics
    @Query("SELECT v FROM VectorEmbedding v WHERE v.contentType = :contentType AND v.isActive = true ORDER BY v.createdAt DESC")
    List<VectorEmbedding> findRecentActiveByContentType(@Param("contentType") VectorEmbedding.ContentType contentType);

    @Query("SELECT v FROM VectorEmbedding v WHERE v.collectionName = :collectionName AND v.isActive = true AND v.qualityScore >= :minQuality ORDER BY v.qualityScore DESC")
    List<VectorEmbedding> findHighQualityByCollection(@Param("collectionName") String collectionName, @Param("minQuality") Double minQuality);

    @Query("SELECT COUNT(v) FROM VectorEmbedding v WHERE v.contentType = :contentType AND v.isActive = true")
    Long countActiveByContentType(@Param("contentType") VectorEmbedding.ContentType contentType);

    @Query("SELECT COUNT(v) FROM VectorEmbedding v WHERE v.collectionName = :collectionName AND v.isActive = true")
    Long countActiveByCollection(@Param("collectionName") String collectionName);

    @Query("SELECT AVG(v.qualityScore) FROM VectorEmbedding v WHERE v.contentType = :contentType AND v.isActive = true")
    Double getAverageQualityByContentType(@Param("contentType") VectorEmbedding.ContentType contentType);

    @Query("SELECT AVG(v.processingTimeMs) FROM VectorEmbedding v WHERE v.model = :model AND v.isActive = true")
    Double getAverageProcessingTimeByModel(@Param("model") String model);

    // Statistics queries
    @Query("SELECT v.model, COUNT(v) FROM VectorEmbedding v WHERE v.isActive = true GROUP BY v.model")
    List<Object[]> getCountByModel();

    @Query("SELECT v.contentType, COUNT(v) FROM VectorEmbedding v WHERE v.isActive = true GROUP BY v.contentType")
    List<Object[]> getCountByContentType();

    @Query("SELECT v.collectionName, COUNT(v) FROM VectorEmbedding v WHERE v.isActive = true GROUP BY v.collectionName")
    List<Object[]> getCountByCollection();

    // Cleanup queries
    @Query("DELETE FROM VectorEmbedding v WHERE v.expiresAt < :now AND v.isActive = false")
    int deleteExpiredInactive(@Param("now") LocalDateTime now);

    @Query("UPDATE VectorEmbedding v SET v.isActive = false WHERE v.expiresAt < :now")
    int deactivateExpired(@Param("now") LocalDateTime now);

    // Performance queries
    @Query("SELECT v FROM VectorEmbedding v WHERE v.contentId = :contentId AND v.contentType = :contentType AND v.isActive = true ORDER BY v.createdAt DESC LIMIT 1")
    Optional<VectorEmbedding> findLatestActiveByContent(@Param("contentId") UUID contentId, @Param("contentType") VectorEmbedding.ContentType contentType);

    @Query("SELECT v FROM VectorEmbedding v WHERE v.qdrantPointId IN :pointIds AND v.isActive = true")
    List<VectorEmbedding> findByQdrantPointIds(@Param("pointIds") List<String> pointIds);

    // Health check queries
    @Query("SELECT COUNT(v) FROM VectorEmbedding v WHERE v.isActive = true AND v.createdAt > :since")
    Long countActiveSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(v) FROM VectorEmbedding v WHERE v.isActive = true AND v.qualityScore IS NULL")
    Long countActiveWithoutQualityScore();

    @Query("SELECT COUNT(v) FROM VectorEmbedding v WHERE v.isActive = true AND v.qdrantPointId IS NULL")
    Long countActiveWithoutQdrantPointId();
}

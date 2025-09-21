package ai.youthmind.repository;

import ai.youthmind.entity.MemoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemoryItemRepository extends JpaRepository<MemoryItem, UUID> {
    
    // Basic queries
    List<MemoryItem> findByUserId(UUID userId);
    
    List<MemoryItem> findByUserIdAndMemoryType(UUID userId, MemoryItem.MemoryType memoryType);
    
    List<MemoryItem> findByUserIdAndArchivedAtIsNull(UUID userId);
    
    Optional<MemoryItem> findByUserIdAndKeyIdentifier(UUID userId, String keyIdentifier);
    
    // Weight-based queries for retrieval
    @Query("SELECT m FROM MemoryItem m WHERE m.userId = :userId AND m.archivedAt IS NULL " +
           "ORDER BY m.weightScore DESC, m.lastAccessedAt DESC")
    List<MemoryItem> findByUserIdOrderByWeightDesc(@Param("userId") UUID userId);
    
    @Query("SELECT m FROM MemoryItem m WHERE m.userId = :userId AND m.archivedAt IS NULL " +
           "ORDER BY m.weightScore DESC, m.lastAccessedAt DESC")
    Page<MemoryItem> findByUserIdOrderByWeightDesc(@Param("userId") UUID userId, Pageable pageable);
    
    // Memory type specific queries
    @Query("SELECT m FROM MemoryItem m WHERE m.userId = :userId AND m.memoryType = :type " +
           "AND m.archivedAt IS NULL ORDER BY m.weightScore DESC")
    List<MemoryItem> findActiveMemoriesByType(@Param("userId") UUID userId, 
                                             @Param("type") MemoryItem.MemoryType type);
    
    // Context-based retrieval
    @Query("SELECT m FROM MemoryItem m WHERE m.userId = :userId AND m.archivedAt IS NULL " +
           "AND (m.content ILIKE %:context% OR m.contextTags::text ILIKE %:context%) " +
           "ORDER BY m.weightScore DESC")
    List<MemoryItem> findByUserIdAndContext(@Param("userId") UUID userId, 
                                           @Param("context") String context);
    
    // Time-based queries
    @Query("SELECT m FROM MemoryItem m WHERE m.userId = :userId AND m.lastAccessedAt >= :since " +
           "AND m.archivedAt IS NULL ORDER BY m.lastAccessedAt DESC")
    List<MemoryItem> findRecentlyAccessedMemories(@Param("userId") UUID userId, 
                                                 @Param("since") LocalDateTime since);
    
    @Query("SELECT m FROM MemoryItem m WHERE m.userId = :userId AND m.createdAt >= :since " +
           "AND m.archivedAt IS NULL ORDER BY m.createdAt DESC")
    List<MemoryItem> findRecentMemories(@Param("userId") UUID userId, 
                                       @Param("since") LocalDateTime since);
    
    // Importance-based queries
    @Query("SELECT m FROM MemoryItem m WHERE m.userId = :userId AND m.importanceLevel = :level " +
           "AND m.archivedAt IS NULL ORDER BY m.weightScore DESC")
    List<MemoryItem> findByImportanceLevel(@Param("userId") UUID userId, 
                                          @Param("level") MemoryItem.ImportanceLevel level);
    
    // High-value memories for context building
    @Query("SELECT m FROM MemoryItem m WHERE m.userId = :userId AND m.archivedAt IS NULL " +
           "AND (m.importanceLevel = 'CRITICAL' OR m.importanceLevel = 'HIGH' OR m.weightScore > :minWeight) " +
           "ORDER BY m.weightScore DESC")
    List<MemoryItem> findHighValueMemories(@Param("userId") UUID userId, 
                                          @Param("minWeight") BigDecimal minWeight);
    
    // Memory management queries
    @Query("SELECT COUNT(m) FROM MemoryItem m WHERE m.userId = :userId AND m.archivedAt IS NULL")
    Long countActiveMemoriesByUser(@Param("userId") UUID userId);
    
    @Query("SELECT SUM(LENGTH(m.content)) FROM MemoryItem m WHERE m.userId = :userId AND m.archivedAt IS NULL")
    Long getTotalMemorySizeByUser(@Param("userId") UUID userId);
    
    // Cleanup and archival
    @Query("SELECT m FROM MemoryItem m WHERE m.expiresAt < :now AND m.archivedAt IS NULL")
    List<MemoryItem> findExpiredMemories(@Param("now") LocalDateTime now);
    
    @Query("SELECT m FROM MemoryItem m WHERE m.userId = :userId AND m.accessCount = 0 " +
           "AND m.createdAt < :cutoffDate AND m.archivedAt IS NULL")
    List<MemoryItem> findUnusedOldMemories(@Param("userId") UUID userId, 
                                          @Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT m FROM MemoryItem m WHERE m.userId = :userId AND m.weightScore < :minWeight " +
           "AND m.archivedAt IS NULL ORDER BY m.weightScore ASC")
    List<MemoryItem> findLowWeightMemories(@Param("userId") UUID userId, 
                                          @Param("minWeight") BigDecimal minWeight);
    
    // Update operations
    @Modifying
    @Transactional
    @Query("UPDATE MemoryItem m SET m.accessCount = m.accessCount + 1, m.lastAccessedAt = :now " +
           "WHERE m.id = :memoryId")
    void incrementAccessCount(@Param("memoryId") UUID memoryId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Transactional
    @Query("UPDATE MemoryItem m SET m.weightScore = :newWeight WHERE m.id = :memoryId")
    void updateWeightScore(@Param("memoryId") UUID memoryId, @Param("newWeight") BigDecimal newWeight);
    
    @Modifying
    @Transactional
    @Query("UPDATE MemoryItem m SET m.archivedAt = :now WHERE m.id = :memoryId")
    void archiveMemory(@Param("memoryId") UUID memoryId, @Param("now") LocalDateTime now);
    
    @Modifying
    @Transactional
    @Query("UPDATE MemoryItem m SET m.archivedAt = :now WHERE m.expiresAt < :now AND m.archivedAt IS NULL")
    int archiveExpiredMemories(@Param("now") LocalDateTime now);
    
    // Semantic search (if vector extension is available)
    @Query(value = "SELECT * FROM memory_items m WHERE m.user_id = :userId AND m.archived_at IS NULL " +
                   "ORDER BY m.embedding_vector <-> cast(:queryVector as vector) LIMIT :limit", 
           nativeQuery = true)
    List<MemoryItem> findSimilarMemories(@Param("userId") UUID userId, 
                                        @Param("queryVector") String queryVector, 
                                        @Param("limit") int limit);
    
    // Analytics queries
    @Query("SELECT m.memoryType, COUNT(m), AVG(m.weightScore) FROM MemoryItem m " +
           "WHERE m.userId = :userId AND m.archivedAt IS NULL GROUP BY m.memoryType")
    List<Object[]> getMemoryStatsByType(@Param("userId") UUID userId);
    
    @Query("SELECT DATE(m.createdAt), COUNT(m) FROM MemoryItem m " +
           "WHERE m.userId = :userId AND m.createdAt >= :since GROUP BY DATE(m.createdAt) " +
           "ORDER BY DATE(m.createdAt)")
    List<Object[]> getMemoryCreationTimeline(@Param("userId") UUID userId, 
                                            @Param("since") LocalDateTime since);
}

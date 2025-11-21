package ai.youthmind.repository;

import ai.youthmind.entity.RecommendationMappingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for recommendation mapping rules
 * Provides database access for emotion combination to percentage mapping rules
 */
@Repository
public interface RecommendationMappingRuleRepository extends JpaRepository<RecommendationMappingRule, UUID> {
    
    /**
     * Find recommendation mapping rule by emotion combination
     * 
     * @param emotionCombination The emotion combination string (e.g., "angry,fear,sad")
     * @return Optional containing the mapping rule if found
     */
    Optional<RecommendationMappingRule> findByEmotionCombination(String emotionCombination);
    
    /**
     * Check if a recommendation mapping rule exists for the given emotion combination
     * 
     * @param emotionCombination The emotion combination string to check
     * @return true if a mapping rule exists for this combination
     */
    boolean existsByEmotionCombination(String emotionCombination);
    
    /**
     * Delete recommendation mapping rule by emotion combination
     * 
     * @param emotionCombination The emotion combination string to delete
     * @return Number of deleted rules
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RecommendationMappingRule rmr WHERE rmr.emotionCombination = :emotionCombination")
    int deleteByEmotionCombination(@Param("emotionCombination") String emotionCombination);
    
    /**
     * Find all recommendation mapping rules ordered by emotion combination ascending
     * Useful for admin interfaces to display rules in alphabetical order
     * 
     * @return List of all mapping rules ordered by emotion combination
     */
    @Query("SELECT rmr FROM RecommendationMappingRule rmr ORDER BY rmr.emotionCombination ASC")
    List<RecommendationMappingRule> findAllByOrderByEmotionCombinationAsc();
    
    /**
     * Count total number of recommendation mapping rules
     * Inherited from JpaRepository but explicitly listed for clarity
     * 
     * @return Total count of mapping rules
     */
    @Override
    long count();
    
    /**
     * Find recommendation mapping rules by emotion combination containing specific emotion
     * Useful for finding all rules that include a particular emotion
     * 
     * @param emotion The emotion to search for (e.g., "sad", "happy")
     * @return List of mapping rules containing the specified emotion
     */
    @Query("SELECT rmr FROM RecommendationMappingRule rmr WHERE rmr.emotionCombination LIKE %:emotion%")
    List<RecommendationMappingRule> findByEmotionCombinationContaining(@Param("emotion") String emotion);
    
    /**
     * Find recommendation mapping rules with percentage above threshold for specific emotion
     * Useful for finding rules that heavily weight a particular emotion
     * 
     * @param emotion The emotion to check (sad, fear, angry, happy, conflicted, surprised)
     * @param threshold The percentage threshold
     * @return List of mapping rules with percentage above threshold
     */
    @Query("SELECT rmr FROM RecommendationMappingRule rmr WHERE " +
           "(:emotion = 'sad' AND rmr.sadPercentage >= :threshold) OR " +
           "(:emotion = 'fear' AND rmr.fearPercentage >= :threshold) OR " +
           "(:emotion = 'angry' AND rmr.angryPercentage >= :threshold) OR " +
           "(:emotion = 'happy' AND rmr.happyPercentage >= :threshold) OR " +
           "(:emotion = 'conflicted' AND rmr.conflictedPercentage >= :threshold) OR " +
           "(:emotion = 'surprised' AND rmr.surprisedPercentage >= :threshold)")
    List<RecommendationMappingRule> findByEmotionPercentageAboveThreshold(@Param("emotion") String emotion, 
                                                                         @Param("threshold") Integer threshold);
    
    /**
     * Find recommendation mapping rules created after a specific date
     * Useful for finding recently added rules
     * 
     * @param date The date to filter by
     * @return List of mapping rules created after the specified date
     */
    @Query("SELECT rmr FROM RecommendationMappingRule rmr WHERE rmr.createdAt >= :date ORDER BY rmr.createdAt DESC")
    List<RecommendationMappingRule> findByCreatedAtAfter(@Param("date") java.time.LocalDateTime date);
    
    /**
     * Find recommendation mapping rules updated after a specific date
     * Useful for finding recently modified rules
     * 
     * @param date The date to filter by
     * @return List of mapping rules updated after the specified date
     */
    @Query("SELECT rmr FROM RecommendationMappingRule rmr WHERE rmr.updatedAt >= :date ORDER BY rmr.updatedAt DESC")
    List<RecommendationMappingRule> findByUpdatedAtAfter(@Param("date") java.time.LocalDateTime date);
}





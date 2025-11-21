package ai.youthmind.repository;

import ai.youthmind.entity.BuddhistPsychologyFactor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Buddhist psychology factors.
 * Provides queries for finding by category, positive/negative factors, and modern psychology equivalents.
 */
@Repository
public interface BuddhistPsychologyFactorRepository extends JpaRepository<BuddhistPsychologyFactor, Long> {

    // Basic queries by category
    List<BuddhistPsychologyFactor> findByCategory(BuddhistPsychologyFactor.Category category);
    
    Page<BuddhistPsychologyFactor> findByCategory(BuddhistPsychologyFactor.Category category, Pageable pageable);
    
    List<BuddhistPsychologyFactor> findByCategoryOrderByDimensionIndex(BuddhistPsychologyFactor.Category category);
    
    // Queries by polarity
    List<BuddhistPsychologyFactor> findByPolarity(BuddhistPsychologyFactor.Polarity polarity);
    
    Page<BuddhistPsychologyFactor> findByPolarity(BuddhistPsychologyFactor.Polarity polarity, Pageable pageable);
    
    List<BuddhistPsychologyFactor> findByPolarityOrderByDimensionIndex(BuddhistPsychologyFactor.Polarity polarity);
    
    // Combined category and polarity queries
    List<BuddhistPsychologyFactor> findByCategoryAndPolarity(BuddhistPsychologyFactor.Category category, BuddhistPsychologyFactor.Polarity polarity);
    
    Page<BuddhistPsychologyFactor> findByCategoryAndPolarity(BuddhistPsychologyFactor.Category category, BuddhistPsychologyFactor.Polarity polarity, Pageable pageable);
    
    List<BuddhistPsychologyFactor> findByCategoryAndPolarityOrderByDimensionIndex(BuddhistPsychologyFactor.Category category, BuddhistPsychologyFactor.Polarity polarity);
    
    // Core factors queries
    List<BuddhistPsychologyFactor> findByIsCoreFactorTrue();
    
    List<BuddhistPsychologyFactor> findByIsCoreFactorTrueOrderByDimensionIndex();
    
    List<BuddhistPsychologyFactor> findByIsCoreFactorFalse();
    
    List<BuddhistPsychologyFactor> findByIsCoreFactorFalseOrderByDimensionIndex();
    
    // Secondary afflictions queries
    List<BuddhistPsychologyFactor> findByIsSecondaryAfflictionTrue();
    
    List<BuddhistPsychologyFactor> findByIsSecondaryAfflictionTrueOrderByDimensionIndex();
    
    List<BuddhistPsychologyFactor> findByIsSecondaryAfflictionFalse();
    
    List<BuddhistPsychologyFactor> findByIsSecondaryAfflictionFalseOrderByDimensionIndex();
    
    // Modern psychology mapping queries
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.modernPsychologyMapping ILIKE %:mapping%")
    List<BuddhistPsychologyFactor> findByModernPsychologyMappingContaining(@Param("mapping") String mapping);
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.modernPsychologyMapping ILIKE %:mapping%")
    Page<BuddhistPsychologyFactor> findByModernPsychologyMappingContaining(@Param("mapping") String mapping, Pageable pageable);
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.modernPsychologyMapping = :mapping")
    List<BuddhistPsychologyFactor> findByModernPsychologyMapping(@Param("mapping") String mapping);
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.modernPsychologyMapping ILIKE :pattern")
    List<BuddhistPsychologyFactor> findByModernPsychologyMappingLike(@Param("pattern") String pattern);
    
    // Factor name queries
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.factorName ILIKE %:name%")
    List<BuddhistPsychologyFactor> findByFactorNameContaining(@Param("name") String name);
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.factorName ILIKE %:name%")
    Page<BuddhistPsychologyFactor> findByFactorNameContaining(@Param("name") String name, Pageable pageable);
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.factorName = :name")
    Optional<BuddhistPsychologyFactor> findByFactorName(@Param("name") String name);
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.factorName ILIKE %:name%")
    List<BuddhistPsychologyFactor> findByEnglishNameContaining(@Param("name") String name);
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.factorNameChinese ILIKE %:name%")
    List<BuddhistPsychologyFactor> findByChineseNameContaining(@Param("name") String name);
    
    // Description queries
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.description ILIKE %:description%")
    List<BuddhistPsychologyFactor> findByDescriptionContaining(@Param("description") String description);
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.description ILIKE %:description%")
    Page<BuddhistPsychologyFactor> findByDescriptionContaining(@Param("description") String description, Pageable pageable);
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.therapeuticImplications ILIKE %:implications%")
    List<BuddhistPsychologyFactor> findByTherapeuticImplicationsContaining(@Param("implications") String implications);
    
    // Dimension index queries
    List<BuddhistPsychologyFactor> findByDimensionIndexBetween(Integer startIndex, Integer endIndex);
    
    List<BuddhistPsychologyFactor> findByDimensionIndexBetweenOrderByDimensionIndex(Integer startIndex, Integer endIndex);
    
    Optional<BuddhistPsychologyFactor> findByDimensionIndex(Integer dimensionIndex);
    
    List<BuddhistPsychologyFactor> findByDimensionIndexLessThan(Integer maxIndex);
    
    List<BuddhistPsychologyFactor> findByDimensionIndexGreaterThan(Integer minIndex);
    
    // Universal factors (dimensions 0-4)
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.dimensionIndex BETWEEN 0 AND 4 ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findUniversalFactors();
    
    // Specific factors (dimensions 5-42)
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.dimensionIndex BETWEEN 5 AND 42 ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findSpecificFactors();
    
    // Positive factors
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.polarity = 'POSITIVE' ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findPositiveFactors();
    
    // Negative factors
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.polarity = 'NEGATIVE' ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findNegativeFactors();
    
    // Neutral factors
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.polarity = 'NEUTRAL' ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findNeutralFactors();
    
    // Category-specific queries
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.category = 'UNIVERSAL' ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findUniversalCategoryFactors();
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.category = 'SPECIFIC' ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findSpecificCategoryFactors();
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.category = 'POSITIVE' ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findPositiveCategoryFactors();
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.category = 'NEGATIVE' ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findNegativeCategoryFactors();
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.category = 'SECONDARY_AFFLICTIONS' ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findSecondaryAfflictionsCategoryFactors();
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.category = 'VARIABLE' ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findVariableCategoryFactors();
    
    // Search by multiple categories
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.category IN :categories ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findByCategoryIn(@Param("categories") List<BuddhistPsychologyFactor.Category> categories);
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.category IN :categories ORDER BY bpf.dimensionIndex")
    Page<BuddhistPsychologyFactor> findByCategoryIn(@Param("categories") List<BuddhistPsychologyFactor.Category> categories, Pageable pageable);
    
    // Search by multiple polarities
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.polarity IN :polarities ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findByPolarityIn(@Param("polarities") List<BuddhistPsychologyFactor.Polarity> polarities);
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.polarity IN :polarities ORDER BY bpf.dimensionIndex")
    Page<BuddhistPsychologyFactor> findByPolarityIn(@Param("polarities") List<BuddhistPsychologyFactor.Polarity> polarities, Pageable pageable);
    
    // Complex search queries
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE " +
           "bpf.category = :category AND bpf.polarity = :polarity AND bpf.isCoreFactor = :isCore " +
           "ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findByCategoryAndPolarityAndIsCoreFactor(@Param("category") BuddhistPsychologyFactor.Category category,
                                                                           @Param("polarity") BuddhistPsychologyFactor.Polarity polarity,
                                                                           @Param("isCore") Boolean isCore);
    
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE " +
           "bpf.category = :category AND bpf.polarity = :polarity AND bpf.isSecondaryAffliction = :isSecondary " +
           "ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findByCategoryAndPolarityAndIsSecondaryAffliction(@Param("category") BuddhistPsychologyFactor.Category category,
                                                                                     @Param("polarity") BuddhistPsychologyFactor.Polarity polarity,
                                                                                     @Param("isSecondary") Boolean isSecondary);
    
    // Statistical queries
    @Query("SELECT COUNT(bpf) FROM BuddhistPsychologyFactor bpf WHERE bpf.category = :category")
    Long countByCategory(@Param("category") BuddhistPsychologyFactor.Category category);
    
    @Query("SELECT COUNT(bpf) FROM BuddhistPsychologyFactor bpf WHERE bpf.polarity = :polarity")
    Long countByPolarity(@Param("polarity") BuddhistPsychologyFactor.Polarity polarity);
    
    @Query("SELECT COUNT(bpf) FROM BuddhistPsychologyFactor bpf WHERE bpf.isCoreFactor = true")
    Long countCoreFactors();
    
    @Query("SELECT COUNT(bpf) FROM BuddhistPsychologyFactor bpf WHERE bpf.isSecondaryAffliction = true")
    Long countSecondaryAfflictions();
    
    // All factors ordered by dimension index
    List<BuddhistPsychologyFactor> findAllByOrderByDimensionIndex();
    
    // Existence checks
    boolean existsByFactorName(String factorName);
    
    boolean existsByDimensionIndex(Integer dimensionIndex);
    
    boolean existsByModernPsychologyMapping(String mapping);
    
    // Range queries for dimension indices
    @Query("SELECT bpf FROM BuddhistPsychologyFactor bpf WHERE bpf.dimensionIndex >= :startIndex AND bpf.dimensionIndex <= :endIndex ORDER BY bpf.dimensionIndex")
    List<BuddhistPsychologyFactor> findByDimensionIndexRange(@Param("startIndex") Integer startIndex, @Param("endIndex") Integer endIndex);
    
    // Get all unique categories
    @Query("SELECT DISTINCT bpf.category FROM BuddhistPsychologyFactor bpf ORDER BY bpf.category")
    List<BuddhistPsychologyFactor.Category> findDistinctCategories();
    
    // Get all unique polarities
    @Query("SELECT DISTINCT bpf.polarity FROM BuddhistPsychologyFactor bpf ORDER BY bpf.polarity")
    List<BuddhistPsychologyFactor.Polarity> findDistinctPolarities();
    
    // Get all unique modern psychology mappings
    @Query("SELECT DISTINCT bpf.modernPsychologyMapping FROM BuddhistPsychologyFactor bpf WHERE bpf.modernPsychologyMapping IS NOT NULL ORDER BY bpf.modernPsychologyMapping")
    List<String> findDistinctModernPsychologyMappings();
}

package ai.youthmind.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity for personality computation results
 * Implements dual personality system (active + subconscious)
 */
@Entity
@Table(name = "personality_computations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalityComputation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "computation_period", nullable = false)
    @NotNull(message = "Computation period is required")
    private ComputationPeriod computationPeriod;
    
    @Column(name = "computation_date", nullable = false)
    @NotNull(message = "Computation date is required")
    private LocalDate computationDate;
    
    // Active personality (μ_post) - from user's own posts
    @Column(name = "active_personality_vector", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @NotNull(message = "Active personality vector is required")
    @Size(min = 43, max = 43, message = "Active personality vector must contain exactly 43 dimensions")
    private List<Double> activePersonalityVector;
    
    @Column(name = "active_personality_confidence", nullable = false, precision = 5, scale = 4)
    @NotNull(message = "Active personality confidence is required")
    @DecimalMin(value = "0.0", message = "Active personality confidence must be >= 0")
    @DecimalMax(value = "1.0", message = "Active personality confidence must be <= 1")
    private BigDecimal activePersonalityConfidence;
    
    @Column(name = "active_posts_count", nullable = false)
    @Min(value = 0, message = "Active posts count must be >= 0")
    private Integer activePostsCount = 0;
    
    @Column(name = "active_posts_weighted_sum", nullable = false, precision = 12, scale = 6)
    @DecimalMin(value = "0.0", message = "Active posts weighted sum must be >= 0")
    private BigDecimal activePostsWeightedSum = BigDecimal.ZERO;
    
    // Subconscious personality (V_shadow) - from interaction patterns
    @Column(name = "subconscious_personality_vector", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @NotNull(message = "Subconscious personality vector is required")
    @Size(min = 43, max = 43, message = "Subconscious personality vector must contain exactly 43 dimensions")
    private List<Double> subconsciousPersonalityVector;
    
    @Column(name = "subconscious_personality_confidence", nullable = false, precision = 5, scale = 4)
    @NotNull(message = "Subconscious personality confidence is required")
    @DecimalMin(value = "0.0", message = "Subconscious personality confidence must be >= 0")
    @DecimalMax(value = "1.0", message = "Subconscious personality confidence must be <= 1")
    private BigDecimal subconsciousPersonalityConfidence;
    
    @Column(name = "subconscious_interactions_count", nullable = false)
    @Min(value = 0, message = "Subconscious interactions count must be >= 0")
    private Integer subconsciousInteractionsCount = 0;
    
    @Column(name = "top_k_dimensions", nullable = false)
    @Min(value = 1, message = "Top K dimensions must be >= 1")
    private Integer topKDimensions = 10;
    
    @Column(name = "resonance_dimensions", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Integer> resonanceDimensions;
    
    // Final personality (μ) - combined personality
    @Column(name = "final_personality_vector", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @NotNull(message = "Final personality vector is required")
    @Size(min = 43, max = 43, message = "Final personality vector must contain exactly 43 dimensions")
    private List<Double> finalPersonalityVector;
    
    @Column(name = "lambda1", nullable = false, precision = 5, scale = 4)
    @NotNull(message = "Lambda1 is required")
    @DecimalMin(value = "0.0", message = "Lambda1 must be >= 0")
    @DecimalMax(value = "1.0", message = "Lambda1 must be <= 1")
    private BigDecimal lambda1 = new BigDecimal("0.7");
    
    @Column(name = "lambda2", nullable = false, precision = 5, scale = 4)
    @NotNull(message = "Lambda2 is required")
    @DecimalMin(value = "0.0", message = "Lambda2 must be >= 0")
    @DecimalMax(value = "1.0", message = "Lambda2 must be <= 1")
    private BigDecimal lambda2 = new BigDecimal("0.3");
    
    @Column(name = "final_confidence", nullable = false, precision = 5, scale = 4)
    @NotNull(message = "Final confidence is required")
    @DecimalMin(value = "0.0", message = "Final confidence must be >= 0")
    @DecimalMax(value = "1.0", message = "Final confidence must be <= 1")
    private BigDecimal finalConfidence;
    
    // Computation metadata
    @Column(name = "computation_duration_ms")
    @Min(value = 0, message = "Computation duration must be >= 0")
    private Integer computationDurationMs;
    
    @Column(name = "posts_analyzed", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<UUID> postsAnalyzed;
    
    @Column(name = "interactions_analyzed", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<UUID> interactionsAnalyzed;
    
    @Column(name = "computation_parameters", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String computationParameters;
    
    @Column(name = "computation_version", length = 20)
    @Size(max = 20, message = "Computation version must not exceed 20 characters")
    private String computationVersion = "1.0";
    
    // Status and validation
    @Column(name = "is_valid", nullable = false)
    private Boolean isValid = true;
    
    @Column(name = "validation_errors", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> validationErrors;
    
    @Column(name = "needs_recomputation", nullable = false)
    private Boolean needsRecomputation = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Computation period enumeration
    public enum ComputationPeriod {
        WEEKLY("Weekly personality computation"),
        MONTHLY("Monthly personality computation"),
        QUARTERLY("Quarterly personality computation"),
        YEARLY("Yearly personality computation");
        
        private final String description;
        
        ComputationPeriod(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        validateComputation();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateComputation();
    }
    
    /**
     * Validates the personality computation data
     */
    private void validateComputation() {
        // Validate vector dimensions
        if (activePersonalityVector == null || activePersonalityVector.size() != 43) {
            throw new IllegalArgumentException("Active personality vector must contain exactly 43 dimensions");
        }
        if (subconsciousPersonalityVector == null || subconsciousPersonalityVector.size() != 43) {
            throw new IllegalArgumentException("Subconscious personality vector must contain exactly 43 dimensions");
        }
        if (finalPersonalityVector == null || finalPersonalityVector.size() != 43) {
            throw new IllegalArgumentException("Final personality vector must contain exactly 43 dimensions");
        }
        
        // Validate lambda weights sum to approximately 1
        if (lambda1 != null && lambda2 != null) {
            BigDecimal sum = lambda1.add(lambda2);
            if (sum.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.001")) > 0) {
                throw new IllegalArgumentException("Lambda1 + Lambda2 must sum to approximately 1.0");
            }
        }
        
        // Calculate final confidence if not set
        if (finalConfidence == null && activePersonalityConfidence != null && subconsciousPersonalityConfidence != null) {
            finalConfidence = activePersonalityConfidence.add(subconsciousPersonalityConfidence)
                    .divide(new BigDecimal("2"), 4, BigDecimal.ROUND_HALF_UP);
        }
    }
    
    /**
     * Get a specific dimension value from the active personality vector
     * @param dimensionIndex The index of the dimension (0-42)
     * @return The value at the specified dimension
     */
    public Double getActivePersonalityDimension(int dimensionIndex) {
        if (dimensionIndex < 0 || dimensionIndex >= 43) {
            throw new IllegalArgumentException("Dimension index must be between 0 and 42");
        }
        return activePersonalityVector.get(dimensionIndex);
    }
    
    /**
     * Get a specific dimension value from the subconscious personality vector
     * @param dimensionIndex The index of the dimension (0-42)
     * @return The value at the specified dimension
     */
    public Double getSubconsciousPersonalityDimension(int dimensionIndex) {
        if (dimensionIndex < 0 || dimensionIndex >= 43) {
            throw new IllegalArgumentException("Dimension index must be between 0 and 42");
        }
        return subconsciousPersonalityVector.get(dimensionIndex);
    }
    
    /**
     * Get a specific dimension value from the final personality vector
     * @param dimensionIndex The index of the dimension (0-42)
     * @return The value at the specified dimension
     */
    public Double getFinalPersonalityDimension(int dimensionIndex) {
        if (dimensionIndex < 0 || dimensionIndex >= 43) {
            throw new IllegalArgumentException("Dimension index must be between 0 and 42");
        }
        return finalPersonalityVector.get(dimensionIndex);
    }
    
    /**
     * Calculate the overall personality stability
     * @return Stability score based on confidence levels
     */
    public BigDecimal getPersonalityStability() {
        if (activePersonalityConfidence == null || subconsciousPersonalityConfidence == null) {
            return BigDecimal.ZERO;
        }
        return activePersonalityConfidence.add(subconsciousPersonalityConfidence)
                .divide(new BigDecimal("2"), 4, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Check if this computation has sufficient data
     * @return true if both active and subconscious personalities have enough data
     */
    public boolean hasSufficientData() {
        return activePostsCount != null && activePostsCount >= 5 &&
               subconsciousInteractionsCount != null && subconsciousInteractionsCount >= 10;
    }
    
    /**
     * Get the computation quality score
     * @return Quality score based on data sufficiency and confidence
     */
    public BigDecimal getComputationQuality() {
        BigDecimal dataQuality = BigDecimal.ZERO;
        if (hasSufficientData()) {
            dataQuality = new BigDecimal("0.5");
        }
        
        BigDecimal confidenceQuality = getPersonalityStability().multiply(new BigDecimal("0.5"));
        
        return dataQuality.add(confidenceQuality);
    }
}

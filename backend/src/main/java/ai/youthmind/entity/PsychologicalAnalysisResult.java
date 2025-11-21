package ai.youthmind.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JPA Entity for storing psychological analysis results
 * Contains analysis outputs from AI processing of posts and interactions
 */
@Entity
@Table(name = "psychological_analysis_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PsychologicalAnalysisResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "post_id", nullable = false)
    @NotNull(message = "Post ID is required")
    private UUID postId;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @Column(name = "raw_vector", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @NotNull(message = "Raw vector is required")
    @Size(min = 43, max = 43, message = "Raw vector must contain exactly 43 dimensions")
    private List<Double> rawVector;
    
    @Column(name = "intensity_factor", nullable = false, precision = 5, scale = 4)
    @NotNull(message = "Intensity factor is required")
    @DecimalMin(value = "0.0", message = "Intensity factor must be >= 0")
    @DecimalMax(value = "1.0", message = "Intensity factor must be <= 1")
    private BigDecimal intensityFactor;
    
    @Column(name = "buddhist_factors", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Double> buddhistFactors;
    
    @Column(name = "analysis_metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String analysisMetadata;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false)
    @NotNull(message = "Analysis type is required")
    private AnalysisType analysisType;
    
    @Column(name = "model_version", length = 50)
    @Size(max = 50, message = "Model version must not exceed 50 characters")
    private String modelVersion;
    
    @Column(name = "processing_time_ms")
    @Min(value = 0, message = "Processing time must be >= 0")
    private Long processingTimeMs;
    
    @Column(name = "confidence_score", precision = 5, scale = 4)
    @DecimalMin(value = "0.0", message = "Confidence score must be >= 0")
    @DecimalMax(value = "1.0", message = "Confidence score must be <= 1")
    private BigDecimal confidenceScore;
    
    @Column(name = "quality_score", precision = 5, scale = 4)
    @DecimalMin(value = "0.0", message = "Quality score must be >= 0")
    @DecimalMax(value = "1.0", message = "Quality score must be <= 1")
    private BigDecimal qualityScore;
    
    @Column(name = "is_valid", nullable = false)
    private Boolean isValid = true;
    
    @Column(name = "validation_errors", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> validationErrors;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Analysis type enumeration
    public enum AnalysisType {
        POST_ANALYSIS("Analysis of user post content"),
        INTERACTION_ANALYSIS("Analysis of user interaction"),
        PERSONALITY_ANALYSIS("Personality trait analysis"),
        EMOTIONAL_ANALYSIS("Emotional state analysis"),
        BEHAVIORAL_ANALYSIS("Behavioral pattern analysis"),
        THERAPEUTIC_ANALYSIS("Therapeutic intervention analysis");
        
        private final String description;
        
        AnalysisType(String description) {
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
        validateAnalysisData();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateAnalysisData();
    }
    
    /**
     * Validates the analysis result data
     */
    private void validateAnalysisData() {
        // Validate vector dimensions
        if (rawVector == null || rawVector.size() != 43) {
            throw new IllegalArgumentException("Raw vector must contain exactly 43 dimensions");
        }
        
        // Validate intensity factor
        if (intensityFactor == null || intensityFactor.compareTo(BigDecimal.ZERO) < 0 || intensityFactor.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Intensity factor must be between 0 and 1");
        }
        
        // Validate Buddhist factors if present
        if (buddhistFactors != null) {
            for (Map.Entry<String, Double> entry : buddhistFactors.entrySet()) {
                if (entry.getValue() == null || entry.getValue() < 0.0 || entry.getValue() > 1.0) {
                    throw new IllegalArgumentException("Buddhist factor values must be between 0 and 1");
                }
            }
        }
    }
    
    /**
     * Get a specific dimension value from the raw vector
     * @param dimensionIndex The index of the dimension (0-42)
     * @return The value at the specified dimension
     */
    public Double getRawVectorDimension(int dimensionIndex) {
        if (dimensionIndex < 0 || dimensionIndex >= 43) {
            throw new IllegalArgumentException("Dimension index must be between 0 and 42");
        }
        return rawVector.get(dimensionIndex);
    }
    
    /**
     * Get the value of a specific Buddhist factor
     * @param factorName The name of the Buddhist factor
     * @return The factor value, or null if not present
     */
    public Double getBuddhistFactorValue(String factorName) {
        return buddhistFactors != null ? buddhistFactors.get(factorName) : null;
    }
    
    /**
     * Get the top N Buddhist factors by value
     * @param n The number of top factors to return
     * @return Map of factor names to values, sorted by value descending
     */
    public Map<String, Double> getTopBuddhistFactors(int n) {
        if (buddhistFactors == null || buddhistFactors.isEmpty()) {
            return Map.of();
        }
        
        return buddhistFactors.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(n)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        java.util.LinkedHashMap::new
                ));
    }
    
    /**
     * Calculate the magnitude of the raw vector
     * @return The magnitude of the vector
     */
    public double getVectorMagnitude() {
        return rawVector.stream()
                .mapToDouble(value -> value * value)
                .sum();
    }
    
    /**
     * Get the analysis quality level
     * @return Quality level description
     */
    public String getQualityLevel() {
        if (qualityScore == null) return "Unknown";
        
        if (qualityScore.compareTo(new BigDecimal("0.9")) >= 0) {
            return "Excellent";
        } else if (qualityScore.compareTo(new BigDecimal("0.7")) >= 0) {
            return "Good";
        } else if (qualityScore.compareTo(new BigDecimal("0.5")) >= 0) {
            return "Fair";
        } else if (qualityScore.compareTo(new BigDecimal("0.3")) >= 0) {
            return "Poor";
        } else {
            return "Very Poor";
        }
    }
    
    /**
     * Get the confidence level
     * @return Confidence level description
     */
    public String getConfidenceLevel() {
        if (confidenceScore == null) return "Unknown";
        
        if (confidenceScore.compareTo(new BigDecimal("0.9")) >= 0) {
            return "Very High";
        } else if (confidenceScore.compareTo(new BigDecimal("0.7")) >= 0) {
            return "High";
        } else if (confidenceScore.compareTo(new BigDecimal("0.5")) >= 0) {
            return "Medium";
        } else if (confidenceScore.compareTo(new BigDecimal("0.3")) >= 0) {
            return "Low";
        } else {
            return "Very Low";
        }
    }
    
    /**
     * Check if this analysis result is reliable
     * @return true if both quality and confidence scores are high
     */
    public boolean isReliable() {
        return qualityScore != null && qualityScore.compareTo(new BigDecimal("0.7")) >= 0 &&
               confidenceScore != null && confidenceScore.compareTo(new BigDecimal("0.7")) >= 0;
    }
    
    /**
     * Get the intensity level description
     * @return Intensity level description
     */
    public String getIntensityLevel() {
        if (intensityFactor == null) return "Unknown";
        
        if (intensityFactor.compareTo(new BigDecimal("0.8")) >= 0) {
            return "Very High";
        } else if (intensityFactor.compareTo(new BigDecimal("0.6")) >= 0) {
            return "High";
        } else if (intensityFactor.compareTo(new BigDecimal("0.4")) >= 0) {
            return "Medium";
        } else if (intensityFactor.compareTo(new BigDecimal("0.2")) >= 0) {
            return "Low";
        } else {
            return "Very Low";
        }
    }
    
    /**
     * Check if this analysis has high intensity
     * @return true if intensity factor >= 0.6
     */
    public boolean isHighIntensity() {
        return intensityFactor != null && intensityFactor.compareTo(new BigDecimal("0.6")) >= 0;
    }
    
    /**
     * Get the processing efficiency score
     * @return Processing efficiency (lower is better)
     */
    public Double getProcessingEfficiency() {
        if (processingTimeMs == null || processingTimeMs == 0) return null;
        
        // Calculate efficiency based on processing time
        // Lower processing time = higher efficiency
        return 1000.0 / processingTimeMs;
    }
}

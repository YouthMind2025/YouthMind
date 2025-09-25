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
import java.util.UUID;

/**
 * JPA Entity for storing 43-dimensional psychological state vectors
 * Based on Buddhist mental factors for real-time psychological analysis
 */
@Entity
@Table(name = "psychological_state_vectors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PsychologicalStateVector {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @Column(name = "vector_data", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @NotNull(message = "Vector data is required")
    @Size(min = 43, max = 43, message = "Vector must contain exactly 43 dimensions")
    private List<Double> vectorData;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vector_type", nullable = false)
    @NotNull(message = "Vector type is required")
    private VectorType vectorType;
    
    @Column(name = "intensity_factor", precision = 5, scale = 4)
    @DecimalMin(value = "0.0", message = "Intensity factor must be >= 0")
    @DecimalMax(value = "1.0", message = "Intensity factor must be <= 1")
    private BigDecimal intensityFactor;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    @NotNull(message = "Source type is required")
    private SourceType sourceType;
    
    @Column(name = "source_id")
    private UUID sourceId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Vector type enumeration
    public enum VectorType {
        V_RAW("Raw vector from posts"),
        S_CURRENT("Current user psychological state"),
        R_RAW("Raw vector from interactions"),
        B_BALANCE("System balance vector");
        
        private final String description;
        
        VectorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // Source type enumeration
    public enum SourceType {
        POST("Generated from user post"),
        INTERACTION("Generated from user interaction"),
        SYSTEM("System-generated vector"),
        COMPUTED("Computed from other vectors");
        
        private final String description;
        
        SourceType(String description) {
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
        validateVectorData();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateVectorData();
    }
    
    /**
     * Validates that the vector data contains exactly 43 dimensions
     * and that intensity factor is provided for V_RAW vectors
     */
    private void validateVectorData() {
        if (vectorData == null || vectorData.size() != 43) {
            throw new IllegalArgumentException("Vector data must contain exactly 43 dimensions");
        }
        
        // V_RAW vectors must have intensity factor
        if (vectorType == VectorType.V_RAW && intensityFactor == null) {
            throw new IllegalArgumentException("V_RAW vectors must have an intensity factor");
        }
        
        // Non-V_RAW vectors should not have intensity factor
        if (vectorType != VectorType.V_RAW && intensityFactor != null) {
            throw new IllegalArgumentException("Only V_RAW vectors should have intensity factor");
        }
    }
    
    /**
     * Get a specific dimension value from the vector
     * @param dimensionIndex The index of the dimension (0-42)
     * @return The value at the specified dimension
     */
    public Double getDimensionValue(int dimensionIndex) {
        if (dimensionIndex < 0 || dimensionIndex >= 43) {
            throw new IllegalArgumentException("Dimension index must be between 0 and 42");
        }
        return vectorData.get(dimensionIndex);
    }
    
    /**
     * Set a specific dimension value in the vector
     * @param dimensionIndex The index of the dimension (0-42)
     * @param value The value to set
     */
    public void setDimensionValue(int dimensionIndex, Double value) {
        if (dimensionIndex < 0 || dimensionIndex >= 43) {
            throw new IllegalArgumentException("Dimension index must be between 0 and 42");
        }
        vectorData.set(dimensionIndex, value);
    }
    
    /**
     * Calculate the magnitude (Euclidean norm) of the vector
     * @return The magnitude of the vector
     */
    public double calculateMagnitude() {
        return vectorData.stream()
                .mapToDouble(value -> value * value)
                .sum();
    }
    
    /**
     * Calculate the dot product with another vector
     * @param other The other vector
     * @return The dot product
     */
    public double dotProduct(PsychologicalStateVector other) {
        if (other == null || other.vectorData == null || other.vectorData.size() != 43) {
            throw new IllegalArgumentException("Other vector must have 43 dimensions");
        }
        
        double dotProduct = 0.0;
        for (int i = 0; i < 43; i++) {
            dotProduct += vectorData.get(i) * other.vectorData.get(i);
        }
        return dotProduct;
    }
    
    /**
     * Calculate cosine similarity with another vector
     * @param other The other vector
     * @return The cosine similarity (-1 to 1)
     */
    public double cosineSimilarity(PsychologicalStateVector other) {
        double dotProduct = dotProduct(other);
        double magnitude1 = calculateMagnitude();
        double magnitude2 = other.calculateMagnitude();
        
        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }
}

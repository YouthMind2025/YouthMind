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
 * JPA Entity for tracking psychological state update history
 * Provides complete audit trail of all state changes and computations
 */
@Entity
@Table(name = "state_update_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StateUpdateHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "update_type", nullable = false)
    @NotNull(message = "Update type is required")
    private UpdateType updateType;
    
    @Column(name = "previous_state", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Double> previousState;
    
    @Column(name = "new_state", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @NotNull(message = "New state is required")
    @Size(min = 43, max = 43, message = "New state must contain exactly 43 dimensions")
    private List<Double> newState;
    
    @Column(name = "alpha_used", precision = 5, scale = 4)
    @DecimalMin(value = "0.0", message = "Alpha used must be >= 0")
    @DecimalMax(value = "1.0", message = "Alpha used must be <= 1")
    private BigDecimal alphaUsed;
    
    @Column(name = "theta_factor", precision = 5, scale = 4)
    @DecimalMin(value = "0.0", message = "Theta factor must be >= 0")
    @DecimalMax(value = "1.0", message = "Theta factor must be <= 1")
    private BigDecimal thetaFactor;
    
    @Column(name = "beta_factor", precision = 5, scale = 4)
    @DecimalMin(value = "0.0", message = "Beta factor must be >= 0")
    @DecimalMax(value = "1.0", message = "Beta factor must be <= 1")
    private BigDecimal betaFactor;
    
    @Column(name = "mu_factor", precision = 5, scale = 4)
    @DecimalMin(value = "0.0", message = "Mu factor must be >= 0")
    @DecimalMax(value = "1.0", message = "Mu factor must be <= 1")
    private BigDecimal muFactor;
    
    @Column(name = "ws_weight", precision = 5, scale = 4)
    @DecimalMin(value = "0.0", message = "Ws weight must be >= 0")
    @DecimalMax(value = "1.0", message = "Ws weight must be <= 1")
    private BigDecimal wsWeight;
    
    @Column(name = "source_id")
    private UUID sourceId;
    
    @Column(name = "computation_metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String computationMetadata;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Update type enumeration
    public enum UpdateType {
        POSTING("Update from user posting"),
        INTERACTION("Update from user interaction"),
        PERSONALITY_COMPUTATION("Update from personality computation"),
        SYSTEM_UPDATE("System-generated update");
        
        private final String description;
        
        UpdateType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        validateUpdateData();
    }
    
    /**
     * Validates the state update data based on update type
     */
    private void validateUpdateData() {
        // Validate vector dimensions
        if (previousState != null && previousState.size() != 43) {
            throw new IllegalArgumentException("Previous state must contain exactly 43 dimensions");
        }
        if (newState == null || newState.size() != 43) {
            throw new IllegalArgumentException("New state must contain exactly 43 dimensions");
        }
        
        // Validate parameters based on update type
        switch (updateType) {
            case POSTING:
                if (alphaUsed == null || thetaFactor == null) {
                    throw new IllegalArgumentException("POSTING updates must have alpha_used and theta_factor");
                }
                break;
            case INTERACTION:
                if (betaFactor == null || muFactor == null || wsWeight == null) {
                    throw new IllegalArgumentException("INTERACTION updates must have beta_factor, mu_factor, and ws_weight");
                }
                break;
            case PERSONALITY_COMPUTATION:
                // Personality computation updates may not have specific parameters
                break;
            case SYSTEM_UPDATE:
                // System updates may have various parameters
                break;
        }
    }
    
    /**
     * Get a specific dimension value from the previous state
     * @param dimensionIndex The index of the dimension (0-42)
     * @return The value at the specified dimension, or null if no previous state
     */
    public Double getPreviousStateDimension(int dimensionIndex) {
        if (previousState == null) return null;
        if (dimensionIndex < 0 || dimensionIndex >= 43) {
            throw new IllegalArgumentException("Dimension index must be between 0 and 42");
        }
        return previousState.get(dimensionIndex);
    }
    
    /**
     * Get a specific dimension value from the new state
     * @param dimensionIndex The index of the dimension (0-42)
     * @return The value at the specified dimension
     */
    public Double getNewStateDimension(int dimensionIndex) {
        if (dimensionIndex < 0 || dimensionIndex >= 43) {
            throw new IllegalArgumentException("Dimension index must be between 0 and 42");
        }
        return newState.get(dimensionIndex);
    }
    
    /**
     * Calculate the magnitude of change between previous and new state
     * @return The magnitude of change, or null if no previous state
     */
    public Double getChangeMagnitude() {
        if (previousState == null) return null;
        
        double sumSquaredDiffs = 0.0;
        for (int i = 0; i < 43; i++) {
            double diff = newState.get(i) - previousState.get(i);
            sumSquaredDiffs += diff * diff;
        }
        return Math.sqrt(sumSquaredDiffs);
    }
    
    /**
     * Calculate the change in a specific dimension
     * @param dimensionIndex The index of the dimension (0-42)
     * @return The change in the dimension, or null if no previous state
     */
    public Double getDimensionChange(int dimensionIndex) {
        if (previousState == null) return null;
        if (dimensionIndex < 0 || dimensionIndex >= 43) {
            throw new IllegalArgumentException("Dimension index must be between 0 and 42");
        }
        return newState.get(dimensionIndex) - previousState.get(dimensionIndex);
    }
    
    /**
     * Get the largest change in any dimension
     * @return The largest absolute change, or null if no previous state
     */
    public Double getMaxDimensionChange() {
        if (previousState == null) return null;
        
        double maxChange = 0.0;
        for (int i = 0; i < 43; i++) {
            double change = Math.abs(newState.get(i) - previousState.get(i));
            maxChange = Math.max(maxChange, change);
        }
        return maxChange;
    }
    
    /**
     * Check if this update represents a significant change
     * @param threshold The threshold for significant change
     * @return true if the change magnitude exceeds the threshold
     */
    public boolean isSignificantChange(double threshold) {
        Double changeMagnitude = getChangeMagnitude();
        return changeMagnitude != null && changeMagnitude > threshold;
    }
    
    /**
     * Get the update impact level based on change magnitude
     * @return Impact level description
     */
    public String getImpactLevel() {
        Double changeMagnitude = getChangeMagnitude();
        if (changeMagnitude == null) return "No Previous State";
        
        if (changeMagnitude > 2.0) {
            return "Major Change";
        } else if (changeMagnitude > 1.0) {
            return "Significant Change";
        } else if (changeMagnitude > 0.5) {
            return "Moderate Change";
        } else if (changeMagnitude > 0.1) {
            return "Minor Change";
        } else {
            return "Minimal Change";
        }
    }
    
    /**
     * Check if this is a posting update
     * @return true if update type is POSTING
     */
    public boolean isPostingUpdate() {
        return updateType == UpdateType.POSTING;
    }
    
    /**
     * Check if this is an interaction update
     * @return true if update type is INTERACTION
     */
    public boolean isInteractionUpdate() {
        return updateType == UpdateType.INTERACTION;
    }
    
    /**
     * Check if this is a personality computation update
     * @return true if update type is PERSONALITY_COMPUTATION
     */
    public boolean isPersonalityComputationUpdate() {
        return updateType == UpdateType.PERSONALITY_COMPUTATION;
    }
    
    /**
     * Check if this is a system update
     * @return true if update type is SYSTEM_UPDATE
     */
    public boolean isSystemUpdate() {
        return updateType == UpdateType.SYSTEM_UPDATE;
    }
}

package ai.youthmind.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for tracking user interaction modulation parameters
 * Stores β (beta) and μ (mu) parameters for psychological state updates
 */
@Entity
@Table(name = "user_interaction_modulations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInteractionModulation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false)
    @NotNull(message = "Interaction type is required")
    private InteractionType interactionType;
    
    @Column(name = "beta_factor", nullable = false, precision = 8, scale = 6)
    @NotNull(message = "Beta factor is required")
    @DecimalMin(value = "0.0", message = "Beta factor must be >= 0")
    @DecimalMax(value = "1.0", message = "Beta factor must be <= 1")
    private BigDecimal betaFactor;
    
    @Column(name = "mu_factor", nullable = false, precision = 8, scale = 6)
    @NotNull(message = "Mu factor is required")
    @DecimalMin(value = "0.0", message = "Mu factor must be >= 0")
    @DecimalMax(value = "1.0", message = "Mu factor must be <= 1")
    private BigDecimal muFactor;
    
    @Column(name = "ws_weight", nullable = false, precision = 8, scale = 6)
    @NotNull(message = "Ws weight is required")
    @DecimalMin(value = "0.0", message = "Ws weight must be >= 0")
    @DecimalMax(value = "1.0", message = "Ws weight must be <= 1")
    private BigDecimal wsWeight;
    
    @Column(name = "lower_bound", nullable = false, precision = 8, scale = 6)
    @NotNull(message = "Lower bound is required")
    private BigDecimal lowerBound;
    
    @Column(name = "upper_bound", nullable = false, precision = 8, scale = 6)
    @NotNull(message = "Upper bound is required")
    private BigDecimal upperBound;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Interaction type enumeration
    public enum InteractionType {
        RESONANCE("High-impact user resonance with content"),
        LINGER("Medium-impact time spent viewing content"),
        VIEW("Low-impact brief viewing of content"),
        LIKE("Medium-high impact liking of content"),
        COMMENT("High impact commenting on content"),
        SHARE("Highest impact sharing of content");
        
        private final String description;
        
        InteractionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * Get the default Ws weight for this interaction type
         * @return Default Ws weight
         */
        public BigDecimal getDefaultWsWeight() {
            return switch (this) {
                case RESONANCE -> new BigDecimal("0.20");
                case LINGER -> new BigDecimal("0.01");
                case VIEW -> new BigDecimal("0.005");
                case LIKE -> new BigDecimal("0.15");
                case COMMENT -> new BigDecimal("0.25");
                case SHARE -> new BigDecimal("0.30");
            };
        }
        
        /**
         * Get the default beta factor for this interaction type
         * @return Default beta factor
         */
        public BigDecimal getDefaultBetaFactor() {
            return switch (this) {
                case RESONANCE -> new BigDecimal("0.15");
                case LINGER -> new BigDecimal("0.10");
                case VIEW -> new BigDecimal("0.05");
                case LIKE -> new BigDecimal("0.12");
                case COMMENT -> new BigDecimal("0.18");
                case SHARE -> new BigDecimal("0.20");
            };
        }
        
        /**
         * Get the default mu factor for this interaction type
         * @return Default mu factor
         */
        public BigDecimal getDefaultMuFactor() {
            return switch (this) {
                case RESONANCE -> new BigDecimal("0.08");
                case LINGER -> new BigDecimal("0.05");
                case VIEW -> new BigDecimal("0.02");
                case LIKE -> new BigDecimal("0.06");
                case COMMENT -> new BigDecimal("0.10");
                case SHARE -> new BigDecimal("0.12");
            };
        }
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        validateBounds();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateBounds();
    }
    
    /**
     * Validates that lower bound is less than upper bound
     */
    private void validateBounds() {
        if (lowerBound != null && upperBound != null && lowerBound.compareTo(upperBound) >= 0) {
            throw new IllegalArgumentException("Lower bound must be less than upper bound");
        }
    }
    
    /**
     * Check if the modulation parameters are within valid ranges
     * @return true if all parameters are valid
     */
    public boolean isValid() {
        return betaFactor != null && betaFactor.compareTo(BigDecimal.ZERO) >= 0 && betaFactor.compareTo(BigDecimal.ONE) <= 0 &&
               muFactor != null && muFactor.compareTo(BigDecimal.ZERO) >= 0 && muFactor.compareTo(BigDecimal.ONE) <= 0 &&
               wsWeight != null && wsWeight.compareTo(BigDecimal.ZERO) >= 0 && wsWeight.compareTo(BigDecimal.ONE) <= 0 &&
               lowerBound != null && upperBound != null && lowerBound.compareTo(upperBound) < 0;
    }
    
    /**
     * Get the interaction impact level based on Ws weight
     * @return Impact level description
     */
    public String getImpactLevel() {
        if (wsWeight == null) return "Unknown";
        
        if (wsWeight.compareTo(new BigDecimal("0.20")) >= 0) {
            return "High Impact";
        } else if (wsWeight.compareTo(new BigDecimal("0.10")) >= 0) {
            return "Medium-High Impact";
        } else if (wsWeight.compareTo(new BigDecimal("0.05")) >= 0) {
            return "Medium Impact";
        } else if (wsWeight.compareTo(new BigDecimal("0.01")) >= 0) {
            return "Low-Medium Impact";
        } else {
            return "Low Impact";
        }
    }
    
    /**
     * Calculate the effective modulation strength
     * @return Combined strength of beta and mu factors
     */
    public BigDecimal getModulationStrength() {
        if (betaFactor == null || muFactor == null) {
            return BigDecimal.ZERO;
        }
        return betaFactor.add(muFactor).divide(new BigDecimal("2"), 6, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Check if this modulation is for a high-impact interaction
     * @return true if Ws weight >= 0.20
     */
    public boolean isHighImpact() {
        return wsWeight != null && wsWeight.compareTo(new BigDecimal("0.20")) >= 0;
    }
    
    /**
     * Check if this modulation is for a low-impact interaction
     * @return true if Ws weight < 0.01
     */
    public boolean isLowImpact() {
        return wsWeight != null && wsWeight.compareTo(new BigDecimal("0.01")) < 0;
    }
}

package ai.youthmind.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA Entity for Buddhist psychology factors reference table
 * Contains the 43 mental factors used in psychological analysis
 */
@Entity
@Table(name = "buddhist_psychology_factors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuddhistPsychologyFactor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "factor_name", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Factor name is required")
    @Size(max = 100, message = "Factor name must not exceed 100 characters")
    private String factorName;
    
    @Column(name = "factor_name_chinese", length = 100)
    @Size(max = 100, message = "Chinese factor name must not exceed 100 characters")
    private String factorNameChinese;
    
    @Column(name = "dimension_index", nullable = false, unique = true)
    @Min(value = 0, message = "Dimension index must be >= 0")
    @Max(value = 42, message = "Dimension index must be <= 42")
    private Integer dimensionIndex;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    @NotNull(message = "Category is required")
    private Category category;
    
    @Column(name = "subcategory", length = 50)
    @Size(max = 50, message = "Subcategory must not exceed 50 characters")
    private String subcategory;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "polarity", nullable = false)
    @NotNull(message = "Polarity is required")
    private Polarity polarity;
    
    @Column(name = "modern_psychology_mapping", columnDefinition = "TEXT")
    private String modernPsychologyMapping;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "therapeutic_implications", columnDefinition = "TEXT")
    private String therapeuticImplications;
    
    @Column(name = "is_core_factor", nullable = false)
    private Boolean isCoreFactor = false;
    
    @Column(name = "is_secondary_affliction", nullable = false)
    private Boolean isSecondaryAffliction = false;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Category enumeration
    public enum Category {
        UNIVERSAL("Universal mental factors present in all consciousness"),
        SPECIFIC("Factors specific to certain mental states"),
        POSITIVE("Wholesome mental factors"),
        NEGATIVE("Unwholesome mental factors"),
        SECONDARY_AFFLICTIONS("Derived from primary afflictions"),
        VARIABLE("Context-dependent factors");
        
        private final String description;
        
        Category(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // Polarity enumeration
    public enum Polarity {
        POSITIVE("Generally positive mental factor"),
        NEGATIVE("Generally negative mental factor"),
        NEUTRAL("Neutral mental factor");
        
        private final String description;
        
        Polarity(String description) {
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
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if this factor is one of the 5 core universal factors
     * @return true if this is a core factor
     */
    public boolean isCoreFactor() {
        return isCoreFactor != null && isCoreFactor;
    }
    
    /**
     * Check if this factor is a secondary affliction
     * @return true if this is a secondary affliction
     */
    public boolean isSecondaryAffliction() {
        return isSecondaryAffliction != null && isSecondaryAffliction;
    }
    
    /**
     * Check if this factor is positive
     * @return true if this factor has positive polarity
     */
    public boolean isPositive() {
        return polarity == Polarity.POSITIVE;
    }
    
    /**
     * Check if this factor is negative
     * @return true if this factor has negative polarity
     */
    public boolean isNegative() {
        return polarity == Polarity.NEGATIVE;
    }
    
    /**
     * Check if this factor is neutral
     * @return true if this factor has neutral polarity
     */
    public boolean isNeutral() {
        return polarity == Polarity.NEUTRAL;
    }
    
    /**
     * Get the factor name in the preferred language
     * @param preferChinese Whether to prefer Chinese name if available
     * @return The factor name
     */
    public String getDisplayName(boolean preferChinese) {
        if (preferChinese && factorNameChinese != null && !factorNameChinese.trim().isEmpty()) {
            return factorNameChinese;
        }
        return factorName;
    }
    
    /**
     * Get a brief description of the factor's therapeutic implications
     * @return Therapeutic implications or description if not available
     */
    public String getTherapeuticDescription() {
        if (therapeuticImplications != null && !therapeuticImplications.trim().isEmpty()) {
            return therapeuticImplications;
        }
        return description;
    }
}

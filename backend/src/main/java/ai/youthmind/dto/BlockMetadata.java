package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Metadata for feed blocks containing emotion distribution and generation information
 * Used to provide analytics and block-specific information to iOS clients
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Metadata for feed blocks containing emotion distribution and generation information")
public class BlockMetadata {
    
    @Schema(description = "Distribution of emotions in this feed block", 
            example = "{\"happy\": 8, \"sad\": 5, \"angry\": 3, \"conflicted\": 2, \"surprised\": 1, \"fear\": 1}")
    private Map<String, Integer> emotionDistribution;
    
    @Schema(description = "R vector version used to generate this block", example = "15")
    private Integer rVectorVersion;
    
    @Schema(description = "Timestamp when this block was generated", example = "2024-01-15T10:30:00")
    private LocalDateTime generatedAt;
    
    @Schema(description = "Timestamp when this block expires", example = "2024-01-16T10:30:00")
    private LocalDateTime expiresAt;
    
    @Schema(description = "Whether this block was generated using personalized recommendations", example = "true")
    private Boolean isPersonalized;
    
    /**
     * Check if this block has expired
     * 
     * @return true if current time is after expiresAt
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Get the age of this block in hours
     * 
     * @return hours since generation
     */
    public long getAgeInHours() {
        if (generatedAt == null) {
            return 0;
        }
        return java.time.Duration.between(generatedAt, LocalDateTime.now()).toHours();
    }
    
    /**
     * Get time until expiration in hours
     * 
     * @return hours until expiration (negative if expired)
     */
    public long getTimeUntilExpirationInHours() {
        if (expiresAt == null) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toHours();
    }
    
    /**
     * Get the total number of posts in this block based on emotion distribution
     * 
     * @return total post count
     */
    public int getTotalPostCount() {
        if (emotionDistribution == null) {
            return 0;
        }
        return emotionDistribution.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Get the dominant emotion in this block
     * 
     * @return emotion with highest count, or null if no emotions
     */
    public String getDominantEmotion() {
        if (emotionDistribution == null || emotionDistribution.isEmpty()) {
            return null;
        }
        
        return emotionDistribution.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Get emotion distribution as a percentage map
     * 
     * @return map of emotion -> percentage (0.0 to 1.0)
     */
    public Map<String, Double> getEmotionDistributionPercentages() {
        if (emotionDistribution == null || emotionDistribution.isEmpty()) {
            return Map.of();
        }
        
        int totalPosts = getTotalPostCount();
        if (totalPosts == 0) {
            return Map.of();
        }
        
        return emotionDistribution.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> (double) entry.getValue() / totalPosts
            ));
    }
}

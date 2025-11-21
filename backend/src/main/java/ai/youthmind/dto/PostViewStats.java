package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Post View Statistics Data Transfer Object
 * 
 * Provides analytics data about post views including:
 * - Total unique views
 * - Recent views (last 30 days)
 * - Unique viewer count
 * 
 * This DTO is designed for analytics and reporting purposes,
 * providing insights into post engagement and reach.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Post view statistics for analytics and reporting")
public class PostViewStats {
    
    @Schema(description = "Post ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID postId;
    
    @Schema(description = "Total unique views for this post", example = "1250")
    private long totalViews;
    
    @Schema(description = "Recent views in the last 30 days", example = "89")
    private long recentViews;
    
    @Schema(description = "Number of unique viewers", example = "89")
    private int uniqueViewers;
    
    /**
     * Calculate view growth rate (recent vs total)
     * @return Growth rate as a percentage
     */
    public double getViewGrowthRate() {
        if (totalViews == 0) return 0.0;
        return ((double) recentViews / totalViews) * 100.0;
    }
    
    /**
     * Check if post has high engagement
     * @return True if recent views are more than 10% of total views
     */
    public boolean hasHighEngagement() {
        return getViewGrowthRate() > 10.0;
    }
    
    /**
     * Get engagement level description
     * @return Human-readable engagement level
     */
    public String getEngagementLevel() {
        double rate = getViewGrowthRate();
        if (rate > 20.0) return "Very High";
        if (rate > 10.0) return "High";
        if (rate > 5.0) return "Moderate";
        if (rate > 1.0) return "Low";
        return "Very Low";
    }
} 
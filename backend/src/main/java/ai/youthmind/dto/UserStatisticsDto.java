package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsDto {
    
    // Basic user info
    private String username;
    private String email;
    private LocalDateTime memberSince;
    private String userStatus;
    
    // Post statistics
    private int totalPosts;
    private int totalViews; // Total views across all user's posts
    private int totalResonance; // Total resonances (likes) received - renamed to match frontend
    private int totalSaves; // Alias for totalCollections to match frontend
    private int totalCollections; // Total collections (saves) received
    private int totalComments; // Total comments received on user's posts
    
    // Level and progress (required by frontend)
    private int resonanceLevel; // User's current level based on resonance
    private double currentLevelProgress; // Progress to next level (0.0-1.0)
    private int nextLevelThreshold; // Resonance needed for next level
    private LocalDateTime joinDate; // Alias for memberSince
    
    // Engagement statistics (user's actions)
    private int postsResonated; // Posts user has resonated with
    private int postsCollected; // Posts user has collected/saved
    private int commentsMade; // Comments user has made
    private int commentsLiked; // Comments user has liked
    
    // Activity patterns
    private Map<String, Integer> emotionDistribution; // Distribution of emotions in user's posts
    private Map<String, Integer> activityByDayOfWeek; // Activity by day of week
    private Map<String, Integer> activityByHour; // Activity by hour of day
    
    // Performance metrics
    private double averageViewsPerPost;
    private double averageResonancesPerPost;
    private double averageCollectionsPerPost;
    private double engagementRate; // (resonances + collections) / views
    
    // Recent activity (last 30 days)
    private int recentPosts;
    private int recentResonances;
    private int recentCollections;
    private int recentComments;
    
    // Achievement-like metrics
    private int streakDays; // Consecutive days with activity
    private String mostUsedEmotion;
    private String mostActiveDay;
    private String mostActiveHour;
    
    // Social metrics
    private int uniqueEngagers; // Unique users who engaged with user's content
    private double communityImpact; // Calculated impact score
}

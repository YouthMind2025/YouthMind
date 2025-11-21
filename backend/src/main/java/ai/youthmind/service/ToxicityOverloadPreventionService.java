package ai.youthmind.service;

import ai.youthmind.entity.User;
import ai.youthmind.entity.UserModerationStatus;
import ai.youthmind.repository.UserModerationStatusRepository;
import ai.youthmind.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service for preventing toxicity overload by implementing progressive moderation strategies.
 * Monitors user behavior patterns and adjusts moderation sensitivity accordingly.
 */
@Service
public class ToxicityOverloadPreventionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ToxicityOverloadPreventionService.class);
    
    @Autowired
    private UserModerationStatusRepository moderationStatusRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Toxicity thresholds
    private static final int MAX_DAILY_FLAGGED_CONTENT = 5;
    private static final int MAX_WEEKLY_FLAGGED_CONTENT = 15;
    private static final int MAX_MONTHLY_FLAGGED_CONTENT = 50;
    
    // Time windows for analysis
    private static final int DAILY_WINDOW_HOURS = 24;
    private static final int WEEKLY_WINDOW_DAYS = 7;
    private static final int MONTHLY_WINDOW_DAYS = 30;
    
    // Progressive moderation levels
    public enum ToxicityLevel {
        LOW(0.3, "Low toxicity detected - standard moderation"),
        MODERATE(0.5, "Moderate toxicity detected - increased monitoring"),
        HIGH(0.7, "High toxicity detected - strict moderation"),
        CRITICAL(0.9, "Critical toxicity detected - maximum moderation");
        
        private final double threshold;
        private final String description;
        
        ToxicityLevel(double threshold, String description) {
            this.threshold = threshold;
            this.description = description;
        }
        
        public double getThreshold() { return threshold; }
        public String getDescription() { return description; }
    }
    
    /**
     * Analyze user's toxicity patterns and determine appropriate moderation level
     * 
     * @param userId The user ID to analyze
     * @return ToxicityAnalysis with recommended moderation level
     */
    public ToxicityAnalysis analyzeUserToxicity(UUID userId) {
        logger.debug("Analyzing toxicity patterns for user {}", userId);
        
        try {
            UserModerationStatus status = moderationStatusRepository.findByUserId(userId).orElse(null);
            if (status == null) {
                return new ToxicityAnalysis(ToxicityLevel.LOW, 0.0, "New user - no toxicity history");
            }
            
            // Calculate toxicity score based on various factors
            double toxicityScore = calculateToxicityScore(status);
            
            // Determine toxicity level
            ToxicityLevel level = determineToxicityLevel(toxicityScore);
            
            // Generate recommendations
            List<String> recommendations = generateRecommendations(level, status);
            
            return new ToxicityAnalysis(level, toxicityScore, String.join("; ", recommendations));
            
        } catch (Exception e) {
            logger.error("Error analyzing user toxicity for user {}: {}", userId, e.getMessage(), e);
            return new ToxicityAnalysis(ToxicityLevel.LOW, 0.0, "Error in analysis - defaulting to low toxicity");
        }
    }
    
    /**
     * Calculate toxicity score based on user's moderation history
     */
    private double calculateToxicityScore(UserModerationStatus status) {
        double score = 0.0;
        
        // Base score from warning level
        score += status.getWarningLevel() * 0.1;
        
        // Score from total warnings
        score += Math.min(status.getTotalWarnings() * 0.05, 0.3);
        
        // Score from total bans
        score += Math.min(status.getTotalBans() * 0.2, 0.4);
        
        // Time-based decay for recent warnings
        if (status.getLastWarningAt() != null) {
            long hoursSinceLastWarning = ChronoUnit.HOURS.between(status.getLastWarningAt(), LocalDateTime.now());
            if (hoursSinceLastWarning < 24) {
                score += 0.2; // Recent warning penalty
            } else if (hoursSinceLastWarning < 168) { // 1 week
                score += 0.1; // Recent warning penalty
            }
        }
        
        // Status-based adjustments
        switch (status.getStatus()) {
            case CLEAN -> score *= 0.5; // Reduce score for clean users
            case WATCHED -> score += 0.1;
            case WARNED -> score += 0.2;
            case RESTRICTED -> score += 0.4;
            case BANNED -> score += 0.6;
        }
        
        // Manual review requirement increases score
        if (status.getManualReviewRequired()) {
            score += 0.2;
        }
        
        return Math.min(score, 1.0); // Cap at 1.0
    }
    
    /**
     * Determine toxicity level based on score
     */
    private ToxicityLevel determineToxicityLevel(double score) {
        if (score >= ToxicityLevel.CRITICAL.getThreshold()) {
            return ToxicityLevel.CRITICAL;
        } else if (score >= ToxicityLevel.HIGH.getThreshold()) {
            return ToxicityLevel.HIGH;
        } else if (score >= ToxicityLevel.MODERATE.getThreshold()) {
            return ToxicityLevel.MODERATE;
        } else {
            return ToxicityLevel.LOW;
        }
    }
    
    /**
     * Generate recommendations based on toxicity level
     */
    private List<String> generateRecommendations(ToxicityLevel level, UserModerationStatus status) {
        return switch (level) {
            case LOW -> List.of(
                "Continue standard moderation",
                "Monitor for any pattern changes"
            );
            case MODERATE -> List.of(
                "Increase monitoring frequency",
                "Consider gentle reminders about community guidelines",
                "Track content patterns more closely"
            );
            case HIGH -> List.of(
                "Implement stricter moderation",
                "Require manual review for borderline content",
                "Consider temporary restrictions",
                "Provide educational resources about appropriate language"
            );
            case CRITICAL -> List.of(
                "Maximum moderation level",
                "All content requires manual review",
                "Consider temporary suspension",
                "Mandatory educational intervention",
                "Escalate to human moderators"
            );
        };
    }
    
    /**
     * Check if user should be exempt from progressive moderation
     * 
     * @param userId The user ID to check
     * @return true if user should be exempt
     */
    public boolean shouldExemptFromProgressiveModeration(UUID userId) {
        ToxicityAnalysis analysis = analyzeUserToxicity(userId);
        
        // Exempt users with low toxicity and clean history
        return analysis.getLevel() == ToxicityLevel.LOW && 
               analysis.getScore() < 0.2;
    }
    
    /**
     * Get recommended moderation threshold for user
     * 
     * @param userId The user ID
     * @return Recommended threshold (0.0 to 1.0)
     */
    public double getRecommendedModerationThreshold(UUID userId) {
        ToxicityAnalysis analysis = analyzeUserToxicity(userId);
        
        // Lower threshold for higher toxicity users (stricter moderation)
        return switch (analysis.getLevel()) {
            case LOW -> 0.7; // Standard threshold
            case MODERATE -> 0.6; // Slightly stricter
            case HIGH -> 0.5; // Stricter
            case CRITICAL -> 0.3; // Very strict
        };
    }
    
    /**
     * Check if user has exceeded toxicity limits
     * 
     * @param userId The user ID to check
     * @return ToxicityLimitCheck result
     */
    public ToxicityLimitCheck checkToxicityLimits(UUID userId) {
        logger.debug("Checking toxicity limits for user {}", userId);
        
        try {
            UserModerationStatus status = moderationStatusRepository.findByUserId(userId).orElse(null);
            if (status == null) {
                return new ToxicityLimitCheck(false, "New user - no limits exceeded");
            }
            
            LocalDateTime now = LocalDateTime.now();
            boolean limitsExceeded = false;
            StringBuilder reasons = new StringBuilder();
            
            // Check daily limits
            if (status.getLastWarningAt() != null && 
                ChronoUnit.HOURS.between(status.getLastWarningAt(), now) < DAILY_WINDOW_HOURS) {
                if (status.getTotalWarnings() >= MAX_DAILY_FLAGGED_CONTENT) {
                    limitsExceeded = true;
                    reasons.append("Daily warning limit exceeded; ");
                }
            }
            
            // Check weekly limits
            if (status.getTotalWarnings() >= MAX_WEEKLY_FLAGGED_CONTENT) {
                limitsExceeded = true;
                reasons.append("Weekly warning limit exceeded; ");
            }
            
            // Check monthly limits
            if (status.getTotalWarnings() >= MAX_MONTHLY_FLAGGED_CONTENT) {
                limitsExceeded = true;
                reasons.append("Monthly warning limit exceeded; ");
            }
            
            // Check status-based limits
            if (status.getStatus() == UserModerationStatus.ModerationStatus.BANNED) {
                limitsExceeded = true;
                reasons.append("User is currently banned; ");
            }
            
            return new ToxicityLimitCheck(limitsExceeded, reasons.toString());
            
        } catch (Exception e) {
            logger.error("Error checking toxicity limits for user {}: {}", userId, e.getMessage(), e);
            return new ToxicityLimitCheck(false, "Error in limit check - defaulting to no limits exceeded");
        }
    }
    
    /**
     * Update user's toxicity status based on new moderation result
     * 
     * @param userId The user ID
     * @param wasFlagged Whether content was flagged
     * @param severity The severity of the flagged content
     */
    public void updateToxicityStatus(UUID userId, boolean wasFlagged, double severity) {
        logger.debug("Updating toxicity status for user {} - flagged: {}, severity: {}", userId, wasFlagged, severity);
        
        try {
            UserModerationStatus status = moderationStatusRepository.findByUserId(userId).orElse(null);
            if (status == null) {
                // Fetch user entity - required for JPA relationship
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    logger.warn("User not found with ID: {}, cannot update toxicity status", userId);
                    return; // Exit if user doesn't exist
                }
                
                // Create new status for user
                status = new UserModerationStatus();
                status.setUser(user);  // Set User entity (not userId)
                status.setStatus(UserModerationStatus.ModerationStatus.CLEAN);
                status.setWarningLevel(0);
                status.setTotalWarnings(0);
                status.setTotalBans(0);
                status.setAutoModerationEnabled(true);
                status.setManualReviewRequired(false);
                status.setCreatedAt(LocalDateTime.now());
                status.setUpdatedAt(LocalDateTime.now());
            }
            
            if (wasFlagged) {
                // Update warning level based on severity
                int severityLevel = (int) (severity * 10); // Convert to 0-10 scale
                status.setWarningLevel(Math.min(status.getWarningLevel() + severityLevel, 10));
                
                // Update total warnings
                status.setTotalWarnings(status.getTotalWarnings() + 1);
                
                // Update last warning time
                status.setLastWarningAt(LocalDateTime.now());
                
                // Update status based on warning level
                if (status.getWarningLevel() >= 8) {
                    status.setStatus(UserModerationStatus.ModerationStatus.RESTRICTED);
                    status.setManualReviewRequired(true);
                } else if (status.getWarningLevel() >= 5) {
                    status.setStatus(UserModerationStatus.ModerationStatus.WARNED);
                } else if (status.getWarningLevel() >= 3) {
                    status.setStatus(UserModerationStatus.ModerationStatus.WATCHED);
                }
            } else {
                // Positive behavior - reduce warning level slightly
                if (status.getWarningLevel() > 0) {
                    status.setWarningLevel(Math.max(status.getWarningLevel() - 1, 0));
                }
            }
            
            status.setUpdatedAt(LocalDateTime.now());
            moderationStatusRepository.save(status);
            
            logger.info("Updated toxicity status for user {} - warning level: {}, total warnings: {}", 
                       userId, status.getWarningLevel(), status.getTotalWarnings());
            
        } catch (Exception e) {
            logger.error("Error updating toxicity status for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * Result of toxicity analysis
     */
    public static class ToxicityAnalysis {
        private final ToxicityLevel level;
        private final double score;
        private final String recommendations;
        
        public ToxicityAnalysis(ToxicityLevel level, double score, String recommendations) {
            this.level = level;
            this.score = score;
            this.recommendations = recommendations;
        }
        
        // Getters
        public ToxicityLevel getLevel() { return level; }
        public double getScore() { return score; }
        public String getRecommendations() { return recommendations; }
        
        public boolean isHighToxicity() { return level == ToxicityLevel.HIGH || level == ToxicityLevel.CRITICAL; }
        public boolean needsStrictModeration() { return level == ToxicityLevel.CRITICAL; }
    }
    
    /**
     * Result of toxicity limit check
     */
    public static class ToxicityLimitCheck {
        private final boolean limitsExceeded;
        private final String reason;
        
        public ToxicityLimitCheck(boolean limitsExceeded, String reason) {
            this.limitsExceeded = limitsExceeded;
            this.reason = reason;
        }
        
        // Getters
        public boolean isLimitsExceeded() { return limitsExceeded; }
        public String getReason() { return reason; }
    }
}

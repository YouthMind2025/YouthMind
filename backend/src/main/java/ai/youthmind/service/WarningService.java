package ai.youthmind.service;

import ai.youthmind.entity.User;
import ai.youthmind.entity.UserModerationStatus;
import ai.youthmind.entity.WarningHistory;
import ai.youthmind.entity.WarningHistory.WarningType;
import ai.youthmind.entity.WarningHistory.Severity;
import ai.youthmind.entity.UserModerationStatus.ModerationStatus;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.repository.UserModerationStatusRepository;
import ai.youthmind.repository.WarningHistoryRepository;
import ai.youthmind.exception.WarningSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user warnings and progressive discipline system.
 * Implements the business logic for warning increments, bans, and user status management.
 */
@Service
@Transactional
public class WarningService {
    
    private static final Logger logger = LoggerFactory.getLogger(WarningService.class);
    
    // Warning thresholds
    private static final int MAX_WARNINGS_BEFORE_BAN = 3;
    private static final int WARNING_LEVEL_1_THRESHOLD = 1;
    private static final int WARNING_LEVEL_2_THRESHOLD = 2;
    private static final int WARNING_LEVEL_3_THRESHOLD = 3;
    
    // Ban durations (in days)
    private static final int FIRST_BAN_DAYS = 7;
    private static final int SECOND_BAN_DAYS = 30;
    private static final int PERMANENT_BAN_THRESHOLD = 3;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserModerationStatusRepository moderationStatusRepository;
    
    @Autowired
    private WarningHistoryRepository warningHistoryRepository;
    
    /**
     * Issues a warning to a user and handles progressive discipline.
     * 
     * @param userId The user to warn
     * @param warningType The type of warning
     * @param severity The severity level
     * @param reason Detailed reason for the warning
     * @param moderatorId The moderator issuing the warning
     * @param postId Optional post ID if warning is post-related
     * @param commentId Optional comment ID if warning is comment-related
     * @return The created warning history record
     * @throws WarningSystemException if user not found or system error
     */
    public WarningHistory issueWarning(UUID userId, WarningType warningType, Severity severity, 
                                     String reason, UUID moderatorId, UUID postId, UUID commentId) {
        logger.info("Issuing warning to user {}: {} - {}", userId, warningType, reason);
        
        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> WarningSystemException.userNotFound(userId));
        
        // Check if user is already banned
        if (user.getUserStatus() == User.UserStatus.BANNED) {
            throw WarningSystemException.userAlreadyBanned(userId);
        }
        
        // Create warning history record
        WarningHistory warning = new WarningHistory();
        warning.setUser(user); // Set the JPA relationship
        warning.setModeratorId(moderatorId);
        warning.setPostId(postId);
        warning.setCommentId(commentId);
        warning.setWarningType(warningType);
        warning.setSeverity(severity);
        warning.setReason(reason);
        warning.setCreatedAt(LocalDateTime.now());
        
        // Set expiration based on severity
        LocalDateTime expiresAt = calculateWarningExpiration(severity);
        warning.setExpiresAt(expiresAt);
        
        // Save warning
        warning = warningHistoryRepository.save(warning);
        
        // Update user warning count and status
        updateUserWarningStatus(user, warning);
        
        // Update moderation status
        updateModerationStatus(user, warning);
        
        logger.info("Warning issued successfully. User {} now has {} warnings", 
                   userId, user.getWarningsCount());
        
        return warning;
    }
    
    /**
     * Updates user's warning count and applies progressive discipline.
     */
    private void updateUserWarningStatus(User user, WarningHistory warning) {
        int currentWarnings = user.getWarningsCount();
        int newWarningCount = currentWarnings + 1;
        
        user.setWarningsCount(newWarningCount);
        
        // Apply progressive discipline based on warning count
        if (newWarningCount >= MAX_WARNINGS_BEFORE_BAN) {
            // User should be banned
            banUser(user, calculateBanDuration(newWarningCount), 
                   "Automatic ban after " + newWarningCount + " warnings");
            logger.warn("User {} automatically banned after {} warnings", 
                       user.getId(), newWarningCount);
        } else if (newWarningCount >= WARNING_LEVEL_2_THRESHOLD) {
            // User is in warning level 2 - marked as WARNED
            user.setUserStatus(User.UserStatus.WARNED);
            logger.info("User {} status changed to WARNED ({} warnings)", 
                       user.getId(), newWarningCount);
        } else if (newWarningCount >= WARNING_LEVEL_1_THRESHOLD) {
            // User is in warning level 1 - still ACTIVE but flagged
            user.setUserStatus(User.UserStatus.ACTIVE);
            logger.info("User {} has {} warning(s) - monitoring increased", 
                       user.getId(), newWarningCount);
        }
        
        userRepository.save(user);
    }
    
    /**
     * Updates the user's moderation status based on warning history.
     */
    private void updateModerationStatus(User user, WarningHistory warning) {
        UserModerationStatus status = moderationStatusRepository.findByUserId(user.getId())
            .orElseGet(() -> {
                UserModerationStatus newStatus = new UserModerationStatus();
                newStatus.setUser(user); // Set the JPA relationship
                newStatus.setStatus(ModerationStatus.CLEAN);
                newStatus.setWarningLevel(0);
                newStatus.setTotalWarnings(0);
                newStatus.setTotalBans(0);
                newStatus.setAutoModerationEnabled(true);
                newStatus.setCreatedAt(LocalDateTime.now());
                return newStatus;
            });
        
        // Update warning statistics
        status.setTotalWarnings(status.getTotalWarnings() + 1);
        status.setLastWarningAt(LocalDateTime.now());
        status.setLastModerationAt(LocalDateTime.now());
        status.setUpdatedAt(LocalDateTime.now());
        
        // Update warning level based on current warning count
        int warningLevel = calculateWarningLevel(user.getWarningsCount());
        status.setWarningLevel(warningLevel);
        
        // Update moderation status
        ModerationStatus newModerationStatus = calculateModerationStatus(user.getWarningsCount(), 
                                                                        user.getUserStatus());
        status.setStatus(newModerationStatus);
        
        // Set manual review requirement for high-severity warnings
        if (warning.getSeverity() == Severity.HIGH || warning.getSeverity() == Severity.CRITICAL) {
            status.setManualReviewRequired(true);
        }
        
        moderationStatusRepository.save(status);
    }
    
    /**
     * Bans a user for a specified duration.
     */
    public void banUser(UUID userId, int banDurationDays, String reason) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> WarningSystemException.userNotFound(userId));
        
        banUser(user, banDurationDays, reason);
    }
    
    /**
     * Bans a user for a specified duration.
     */
    private void banUser(User user, int banDurationDays, String reason) {
        LocalDateTime banExpiresAt = LocalDateTime.now().plusDays(banDurationDays);
        
        user.setUserStatus(User.UserStatus.BANNED);
        user.setBanExpiresAt(banExpiresAt);
        
        userRepository.save(user);
        
        // Update moderation status
        updateModerationStatusForBan(user, banDurationDays);
        
        logger.warn("User {} banned until {} - Reason: {}", 
                   user.getId(), banExpiresAt, reason);
    }
    
    /**
     * Unbans a user (removes ban status).
     */
    public void unbanUser(UUID userId, String reason) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> WarningSystemException.userNotFound(userId));
        
        if (user.getUserStatus() != User.UserStatus.BANNED) {
            throw WarningSystemException.userNotBanned(userId);
        }
        
        user.setUserStatus(User.UserStatus.ACTIVE);
        user.setBanExpiresAt(null);
        
        userRepository.save(user);
        
        // Update moderation status
        updateModerationStatusForUnban(user);
        
        logger.info("User {} unbanned - Reason: {}", user.getId(), reason);
    }
    
    /**
     * Checks if a user is currently banned.
     */
    public boolean isUserBanned(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        
        if (user.getUserStatus() == User.UserStatus.BANNED) {
            // Check if ban has expired
            if (user.getBanExpiresAt() != null && user.getBanExpiresAt().isBefore(LocalDateTime.now())) {
                // Ban has expired, auto-unban
                unbanUser(userId, "Ban expired automatically");
                return false;
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets warning history for a user.
     */
    public List<WarningHistory> getUserWarningHistory(UUID userId) {
        return warningHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Gets active warnings for a user (not expired).
     */
    public List<WarningHistory> getActiveWarnings(UUID userId) {
        return warningHistoryRepository.findByUserIdAndExpiresAtAfterOrderByCreatedAtDesc(
            userId, LocalDateTime.now());
    }
    
    /**
     * Acknowledges a warning (marks it as acknowledged by the user).
     */
    public void acknowledgeWarning(UUID warningId, UUID userId) {
        WarningHistory warning = warningHistoryRepository.findById(warningId)
            .orElseThrow(() -> WarningSystemException.warningNotFound(warningId));
        
        if (!warning.getUserId().equals(userId)) {
            throw WarningSystemException.warningNotOwnedByUser(warningId, userId);
        }
        
        warning.setAcknowledgedAt(LocalDateTime.now());
        User acknowledgingUser = userRepository.findById(userId).orElse(null);
        warning.setAcknowledgedBy(acknowledgingUser);
        
        warningHistoryRepository.save(warning);
        
        logger.info("Warning {} acknowledged by user {}", warningId, userId);
    }
    
    /**
     * Calculates warning expiration based on severity.
     */
    private LocalDateTime calculateWarningExpiration(Severity severity) {
        int daysToExpire;
        switch (severity) {
            case LOW:
                daysToExpire = 30; // 30 days
                break;
            case MEDIUM:
                daysToExpire = 60; // 60 days
                break;
            case HIGH:
                daysToExpire = 90; // 90 days
                break;
            case CRITICAL:
                daysToExpire = 180; // 6 months
                break;
            default:
                daysToExpire = 30;
        }
        return LocalDateTime.now().plusDays(daysToExpire);
    }
    
    /**
     * Calculates ban duration based on warning count.
     */
    private int calculateBanDuration(int warningCount) {
        if (warningCount >= PERMANENT_BAN_THRESHOLD) {
            return 365; // 1 year (effectively permanent for most cases)
        } else if (warningCount >= 2) {
            return SECOND_BAN_DAYS; // 30 days
        } else {
            return FIRST_BAN_DAYS; // 7 days
        }
    }
    
    /**
     * Calculates warning level based on warning count.
     */
    private int calculateWarningLevel(int warningCount) {
        if (warningCount >= 5) return 5;
        if (warningCount >= 4) return 4;
        if (warningCount >= 3) return 3;
        if (warningCount >= 2) return 2;
        if (warningCount >= 1) return 1;
        return 0;
    }
    
    /**
     * Calculates moderation status based on warning count and user status.
     */
    private ModerationStatus calculateModerationStatus(int warningCount, User.UserStatus userStatus) {
        if (userStatus == User.UserStatus.BANNED) {
            return ModerationStatus.BANNED;
        }
        
        if (warningCount >= 4) return ModerationStatus.RESTRICTED;
        if (warningCount >= 2) return ModerationStatus.WARNED;
        if (warningCount >= 1) return ModerationStatus.WATCHED;
        
        return ModerationStatus.CLEAN;
    }
    
    /**
     * Updates moderation status for ban.
     */
    private void updateModerationStatusForBan(User user, int banDurationDays) {
        UserModerationStatus status = moderationStatusRepository.findByUserId(user.getId())
            .orElseGet(() -> {
                UserModerationStatus newStatus = new UserModerationStatus();
                newStatus.setUser(user); // Set the JPA relationship
                newStatus.setCreatedAt(LocalDateTime.now());
                return newStatus;
            });
        
        status.setStatus(ModerationStatus.BANNED);
        status.setTotalBans(status.getTotalBans() + 1);
        status.setLastModerationAt(LocalDateTime.now());
        status.setUpdatedAt(LocalDateTime.now());
        
        moderationStatusRepository.save(status);
    }
    
    /**
     * Updates moderation status for unban.
     */
    private void updateModerationStatusForUnban(User user) {
        UserModerationStatus status = moderationStatusRepository.findByUserId(user.getId())
            .orElse(null);
        
        if (status != null) {
            ModerationStatus newStatus = calculateModerationStatus(user.getWarningsCount(), user.getUserStatus());
            status.setStatus(newStatus);
            status.setLastModerationAt(LocalDateTime.now());
            status.setUpdatedAt(LocalDateTime.now());
            
            moderationStatusRepository.save(status);
        }
    }
}

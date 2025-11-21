package ai.youthmind.service;

import ai.youthmind.dto.UserStatisticsDto;
import ai.youthmind.entity.User;
import ai.youthmind.entity.UserEngagement;
import ai.youthmind.entity.CommentEngagement;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.Comment;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.DraftRepository;
import ai.youthmind.repository.ConversationRepository;
import ai.youthmind.repository.UserEngagementRepository;
import ai.youthmind.repository.CommentEngagementRepository;
import ai.youthmind.repository.PostResonanceRepository;
import ai.youthmind.repository.PostCollectionRepository;
import ai.youthmind.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private DraftRepository draftRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private UserEngagementRepository userEngagementRepository;
    
    @Autowired
    private CommentEngagementRepository commentEngagementRepository;
    
    @Autowired
    private PostResonanceRepository postResonanceRepository;
    
    @Autowired
    private PostCollectionRepository postCollectionRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private PsychologicalStateCacheService psychologicalStateCacheService;
    
    @Autowired
    private UserPersonalityService userPersonalityService;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public Optional<User> getUserById(UUID id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public User saveUser(User user) {
        User savedUser = userRepository.save(user);
        
        // Initialize psychological state for new users
        if (savedUser.getCreatedAt().equals(savedUser.getUpdatedAt())) {
            initializePsychologicalState(savedUser.getId());
        }
        
        return savedUser;
    }
    
    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    /**
     * Comprehensive account deletion that removes all user-related data
     * This method handles deletion in the correct order to avoid foreign key constraint violations
     */
    @Transactional
    public void deleteAccount(UUID userId) {
        // 1. Delete user engagements (resonances, collections, views)
        userEngagementRepository.deleteByUserId(userId);
        
        // 2. Delete post resonances
        postResonanceRepository.deleteByUserId(userId);
        
        // 3. Delete post collections
        postCollectionRepository.deleteByUserId(userId);
        
        // 4. Delete comment engagements (likes)
        commentEngagementRepository.deleteByUserId(userId);
        
        // 5. Delete user's comments
        commentRepository.deleteByAuthorId(userId);
        
        // 6. Delete user's drafts
        draftRepository.deleteByUserId(userId);
        
        // 7. Delete user's conversations and messages
        conversationRepository.deleteByUserId(userId);
        
        // 8. Delete user's posts
        postRepository.deleteByUserId(userId);
        
        // 9. Finally, delete the user account
        userRepository.deleteById(userId);
    }
    
    /**
     * Get comprehensive user statistics
     * This method calculates various metrics about user activity and engagement
     */
    public UserStatisticsDto getUserStatistics(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        UserStatisticsDto stats = new UserStatisticsDto();
        
        // Basic user info
        stats.setUsername(user.getUsername());
        stats.setEmail(user.getEmail());
        stats.setMemberSince(user.getCreatedAt());
        stats.setUserStatus(user.getUserStatus().toString());
        
        // Get user's posts
        List<Post> userPosts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        stats.setTotalPosts(userPosts.size());
        
        // Calculate post-related statistics
        int totalViews = userPosts.stream().mapToInt(Post::getView).sum();
        int totalResonances = userPosts.stream().mapToInt(Post::getResonance).sum();
        int totalCollections = userPosts.stream().mapToInt(Post::getCollect).sum();
        stats.setTotalViews(totalViews);
        stats.setTotalResonance(totalResonances);
        stats.setTotalSaves(totalCollections); // Set alias for frontend compatibility
        stats.setTotalCollections(totalCollections);
        
        // Calculate level and progress based on total resonance
        int level = calculateResonanceLevel(totalResonances);
        double progress = calculateLevelProgress(totalResonances, level);
        int nextThreshold = calculateNextLevelThreshold(level);
        
        stats.setResonanceLevel(level);
        stats.setCurrentLevelProgress(progress);
        stats.setNextLevelThreshold(nextThreshold);
        stats.setJoinDate(user.getCreatedAt()); // Set alias for memberSince
        
        // Get comments on user's posts
        int totalComments = 0;
        for (Post post : userPosts) {
            totalComments += commentRepository.countByPostId(post.getId());
        }
        stats.setTotalComments(totalComments);
        
        // User's engagement actions
        List<UserEngagement> userResonances = userEngagementRepository
            .findByUserIdAndType(userId, UserEngagement.EngagementType.RESONANCE);
        List<UserEngagement> userCollections = userEngagementRepository
            .findByUserIdAndType(userId, UserEngagement.EngagementType.COLLECT);
        long userCommentsCount = commentRepository.countByAuthorId(userId);
        List<CommentEngagement> userCommentLikes = commentEngagementRepository
            .findByUserIdAndType(userId, CommentEngagement.EngagementType.LIKE);
        
        stats.setPostsResonated(userResonances.size());
        stats.setPostsCollected(userCollections.size());
        stats.setCommentsMade((int) userCommentsCount);
        stats.setCommentsLiked(userCommentLikes.size());
        
        // Calculate emotion distribution
        Map<String, Integer> emotionDistribution = userPosts.stream()
            .collect(Collectors.groupingBy(
                Post::getEmotion,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        stats.setEmotionDistribution(emotionDistribution);
        
        // Calculate activity patterns
        Map<String, Integer> activityByDayOfWeek = new HashMap<>();
        Map<String, Integer> activityByHour = new HashMap<>();
        
        // Analyze post creation patterns
        for (Post post : userPosts) {
            String dayOfWeek = post.getCreatedAt().getDayOfWeek().toString();
            String hour = String.valueOf(post.getCreatedAt().getHour());
            
            activityByDayOfWeek.merge(dayOfWeek, 1, Integer::sum);
            activityByHour.merge(hour, 1, Integer::sum);
        }
        
        // Analyze engagement patterns
        for (UserEngagement engagement : userResonances) {
            String dayOfWeek = engagement.getCreatedAt().getDayOfWeek().toString();
            String hour = String.valueOf(engagement.getCreatedAt().getHour());
            
            activityByDayOfWeek.merge(dayOfWeek, 1, Integer::sum);
            activityByHour.merge(hour, 1, Integer::sum);
        }
        
        stats.setActivityByDayOfWeek(activityByDayOfWeek);
        stats.setActivityByHour(activityByHour);
        
        // Calculate performance metrics
        if (stats.getTotalPosts() > 0) {
            stats.setAverageViewsPerPost((double) totalViews / stats.getTotalPosts());
            stats.setAverageResonancesPerPost((double) totalResonances / stats.getTotalPosts());
            stats.setAverageCollectionsPerPost((double) totalCollections / stats.getTotalPosts());
        } else {
            stats.setAverageViewsPerPost(0.0);
            stats.setAverageResonancesPerPost(0.0);
            stats.setAverageCollectionsPerPost(0.0);
        }
        
        // Calculate engagement rate
        if (totalViews > 0) {
            stats.setEngagementRate((double) (totalResonances + totalCollections) / totalViews);
        } else {
            stats.setEngagementRate(0.0);
        }
        
        // Recent activity (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int recentPosts = (int) userPosts.stream()
            .filter(post -> post.getCreatedAt().isAfter(thirtyDaysAgo))
            .count();
        int recentResonances = (int) userResonances.stream()
            .filter(engagement -> engagement.getCreatedAt().isAfter(thirtyDaysAgo))
            .count();
        int recentCollections = (int) userCollections.stream()
            .filter(engagement -> engagement.getCreatedAt().isAfter(thirtyDaysAgo))
            .count();
        // For recent comments, we'll use a simplified approach since we don't have the full comment list
        int recentComments = 0; // This would need a more complex query to get recent comments by user
        
        stats.setRecentPosts(recentPosts);
        stats.setRecentResonances(recentResonances);
        stats.setRecentCollections(recentCollections);
        stats.setRecentComments(recentComments);
        
        // Find most used emotion
        stats.setMostUsedEmotion(emotionDistribution.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("None"));
        
        // Find most active day and hour
        stats.setMostActiveDay(activityByDayOfWeek.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("None"));
        
        stats.setMostActiveHour(activityByHour.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("None"));
        
        // Calculate unique engagers (users who resonated with or collected user's posts)
        Set<UUID> uniqueEngagers = new HashSet<>();
        for (Post post : userPosts) {
            List<UserEngagement> postResonances = userEngagementRepository
                .findByPostIdAndType(post.getId(), UserEngagement.EngagementType.RESONANCE);
            List<UserEngagement> postCollections = userEngagementRepository
                .findByPostIdAndType(post.getId(), UserEngagement.EngagementType.COLLECT);
            
            uniqueEngagers.addAll(postResonances.stream()
                .map(UserEngagement::getUserId)
                .collect(Collectors.toSet()));
            uniqueEngagers.addAll(postCollections.stream()
                .map(UserEngagement::getUserId)
                .collect(Collectors.toSet()));
        }
        stats.setUniqueEngagers(uniqueEngagers.size());
        
        // Calculate community impact score (simplified)
        double communityImpact = (totalResonances * 2.0 + totalCollections * 3.0 + totalComments * 1.0) / 
            Math.max(stats.getTotalPosts(), 1);
        stats.setCommunityImpact(communityImpact);
        
        // Calculate streak days (simplified - consecutive days with any activity)
        stats.setStreakDays(calculateStreakDays(userId));
        
        return stats;
    }
    
    /**
     * Calculate user's resonance level based on total resonance count
     */
    private int calculateResonanceLevel(int totalResonance) {
        if (totalResonance < 10) return 1;
        if (totalResonance < 50) return 2;
        if (totalResonance < 150) return 3;
        if (totalResonance < 300) return 4;
        if (totalResonance < 500) return 5;
        if (totalResonance < 800) return 6;
        if (totalResonance < 1200) return 7;
        if (totalResonance < 1800) return 8;
        if (totalResonance < 2500) return 9;
        return 10; // Max level
    }
    
    /**
     * Calculate progress to next level (0.0-1.0)
     */
    private double calculateLevelProgress(int totalResonance, int currentLevel) {
        int currentThreshold = getLevelThreshold(currentLevel);
        int nextThreshold = getLevelThreshold(currentLevel + 1);
        
        if (currentLevel >= 10) return 1.0; // Max level reached
        
        int progressInLevel = totalResonance - currentThreshold;
        int levelRange = nextThreshold - currentThreshold;
        
        return Math.min(1.0, Math.max(0.0, (double) progressInLevel / levelRange));
    }
    
    /**
     * Calculate threshold for next level
     */
    private int calculateNextLevelThreshold(int currentLevel) {
        return getLevelThreshold(currentLevel + 1);
    }
    
    /**
     * Get resonance threshold for a specific level
     */
    private int getLevelThreshold(int level) {
        switch (level) {
            case 1: return 0;
            case 2: return 10;
            case 3: return 50;
            case 4: return 150;
            case 5: return 300;
            case 6: return 500;
            case 7: return 800;
            case 8: return 1200;
            case 9: return 1800;
            case 10: return 2500;
            default: return 2500; // Max threshold
        }
    }
    
    /**
     * Calculate consecutive days with activity
     */
    private int calculateStreakDays(UUID userId) {
        // This is a simplified implementation
        // In a real application, you might want to track this more precisely
        List<Post> recentPosts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (recentPosts.isEmpty()) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        int streakDays = 0;
        LocalDateTime currentDate = now.toLocalDate().atStartOfDay();
        
        for (Post post : recentPosts) {
            LocalDateTime postDate = post.getCreatedAt().toLocalDate().atStartOfDay();
            long daysBetween = ChronoUnit.DAYS.between(postDate, currentDate);
            
            if (daysBetween == streakDays) {
                streakDays++;
                currentDate = currentDate.minusDays(1);
            } else if (daysBetween > streakDays) {
                break;
            }
        }
        
        return streakDays;
    }
    
    // ===== PSYCHOLOGICAL STATE INITIALIZATION =====
    
    /**
     * Initialize psychological state for a new user
     * Creates default state vectors and personality parameters
     */
    public void initializePsychologicalState(UUID userId) {
        try {
            // Create default psychological state
            Map<String, Object> defaultState = new HashMap<>();
            
            // Initialize 43-dimensional psychological vector with neutral values
            List<Double> defaultVector = new ArrayList<>();
            for (int i = 0; i < 43; i++) {
                defaultVector.add(0.5); // Neutral value for all dimensions
            }
            
            defaultState.put("psychological_vector", defaultVector);
            defaultState.put("intensity_factor", 0.5);
            defaultState.put("base_alpha", 0.5);
            defaultState.put("updated_at", LocalDateTime.now());
            defaultState.put("initialized", true);
            
            // Initialize Buddhist factors with neutral values
            Map<String, Double> buddhistFactors = new HashMap<>();
            String[] positiveFactors = {
                "attention", "contact", "feeling", "thinking", "deliberation", "faith", "vigor",
                "inner_shame", "shame", "absence_of_craving", "absence_of_hatred", "absence_of_ignorance",
                "light_ease", "non_laxness", "renunciation", "non_harming"
            };
            String[] negativeFactors = {
                "greed", "anger", "arrogance", "foolishness", "doubt", "improper_views", "wrath",
                "hatred", "rage", "covering", "deceit", "flattery", "conceit", "harming", "jealousy",
                "stinginess", "lack_of_remorse", "lack_of_shame", "lack_of_faith", "laziness",
                "laxness", "torpor", "restlessness", "distraction", "improper_knowledge",
                "scatteredness", "regret"
            };
            
            // Set neutral values for all factors
            for (String factor : positiveFactors) {
                buddhistFactors.put(factor, 0.5);
            }
            for (String factor : negativeFactors) {
                buddhistFactors.put(factor, 0.5);
            }
            
            defaultState.put("buddhist_factors", buddhistFactors);
            
            // Store initial psychological state
            psychologicalStateCacheService.cachePsychologicalState(userId, defaultVector, buddhistFactors);
            
            // Initialize personality profile (this will create a default profile if none exists)
            userPersonalityService.getUserPersonalityProfile(userId);
            
        } catch (Exception e) {
            System.err.println("Failed to initialize psychological state for user " + userId + ": " + e.getMessage());
        }
    }
    
    /**
     * Get user psychological insights for analytics
     */
    public Map<String, Object> getUserPsychologicalInsights(UUID userId) {
        try {
            Map<String, Object> psychologicalState = psychologicalStateCacheService.getPsychologicalState(userId);
            if (psychologicalState == null) {
                return new HashMap<>();
            }
            
            Map<String, Object> insights = new HashMap<>();
            insights.put("psychological_state", psychologicalState);
            insights.put("last_updated", psychologicalState.get("updated_at"));
            insights.put("has_analysis", psychologicalState.containsKey("psychological_vector"));
            
            return insights;
            
        } catch (Exception e) {
            System.err.println("Failed to get psychological insights for user " + userId + ": " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Update user psychological state based on content
     */
    public void updateUserPsychologicalState(UUID userId, String content) {
        try {
            // For now, we'll just warm up the cache since we don't have an update method
            psychologicalStateCacheService.warmUpCache(userId);
        } catch (Exception e) {
            System.err.println("Failed to update psychological state for user " + userId + ": " + e.getMessage());
        }
    }
} 
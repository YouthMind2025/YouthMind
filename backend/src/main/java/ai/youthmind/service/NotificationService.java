package ai.youthmind.service;

import ai.youthmind.dto.NotificationItem;
import ai.youthmind.entity.User;
import ai.youthmind.entity.Post;
import ai.youthmind.entity.PostResonance;
import ai.youthmind.entity.PostCollection;
import ai.youthmind.entity.Comment;
import ai.youthmind.repository.UserRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.PostResonanceRepository;
import ai.youthmind.repository.PostCollectionRepository;
import ai.youthmind.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user notifications
 */
@Service
public class NotificationService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private PostResonanceRepository postResonanceRepository;
    
    @Autowired
    private PostCollectionRepository postCollectionRepository;
    
    @Autowired
    private CommentRepository commentRepository;

    /**
     * Get user notifications with optional type filtering
     */
    public List<NotificationItem> getUserNotifications(UUID userId, int page, int limit, String type) {
        List<NotificationItem> notifications = new ArrayList<>();
        
        if (type == null || "SYSTEM".equalsIgnoreCase(type)) {
            // Add system notifications
            notifications.addAll(generateSystemNotifications(userId));
        }
        
        if (type == null || "COMMENT".equalsIgnoreCase(type)) {
            // Add comment notifications (comments on user's posts)
            notifications.addAll(generateCommentNotifications(userId));
        }
        
        if (type == null || "RESONANCE".equalsIgnoreCase(type)) {
            // Add resonance notifications (resonances on user's posts)
            notifications.addAll(generateResonanceNotifications(userId));
        }
        
        if (type == null || "SAVE".equalsIgnoreCase(type) || "BOOKMARK".equalsIgnoreCase(type)) {
            // Add save/bookmark notifications (saves on user's posts)
            notifications.addAll(generateSaveNotifications(userId));
        }
        
        // Sort by creation date (newest first)
        notifications.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        // Apply pagination
        int startIndex = (page - 1) * limit;
        int endIndex = Math.min(startIndex + limit, notifications.size());
        
        if (startIndex >= notifications.size()) {
            return new ArrayList<>();
        }
        
        return notifications.subList(startIndex, endIndex);
    }

    /**
     * Generate system notifications for a user
     */
    private List<NotificationItem> generateSystemNotifications(UUID userId) {
        List<NotificationItem> systemNotifications = new ArrayList<>();
        
        // Example system notifications
        NotificationItem welcomeNotification = new NotificationItem();
        welcomeNotification.setId(UUID.randomUUID());
        welcomeNotification.setNotificationType("SYSTEM");
        welcomeNotification.setTitle("欢迎来到 SaySomething");
        welcomeNotification.setMessage("感谢您加入我们的情感交流社区！开始分享您的想法和感受吧。");
        welcomeNotification.setActionUrl("/feed");
        welcomeNotification.setRead(false);
        welcomeNotification.setCreatedAt(LocalDateTime.now().minusDays(1));
        systemNotifications.add(welcomeNotification);
        
        NotificationItem tipsNotification = new NotificationItem();
        tipsNotification.setId(UUID.randomUUID());
        tipsNotification.setNotificationType("SYSTEM");
        tipsNotification.setTitle("使用小贴士");
        tipsNotification.setMessage("您可以通过选择不同的情感标签来更好地表达您的心情。");
        tipsNotification.setActionUrl("/create-post");
        tipsNotification.setRead(false);
        tipsNotification.setCreatedAt(LocalDateTime.now().minusHours(12));
        systemNotifications.add(tipsNotification);
        
        return systemNotifications;
    }

    /**
     * Generate comment notifications for a user (comments on their posts)
     */
    private List<NotificationItem> generateCommentNotifications(UUID userId) {
        List<NotificationItem> commentNotifications = new ArrayList<>();
        
        // Get user's posts
        List<Post> userPosts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<UUID> userPostIds = userPosts.stream().map(Post::getId).collect(Collectors.toList());
        
        if (userPostIds.isEmpty()) {
            return commentNotifications;
        }
        
        // Get recent comments on user's posts
        List<Comment> recentComments = commentRepository.findByPostIdInOrderByCreatedAtDesc(userPostIds)
            .stream()
            .filter(comment -> !comment.getAuthorId().equals(userId)) // Exclude user's own comments
            .limit(10) // Limit to recent 10 comments
            .collect(Collectors.toList());
        
        for (Comment comment : recentComments) {
            try {
                // Get the commenter
                User commenter = userRepository.findById(comment.getAuthorId()).orElse(null);
                if (commenter == null) continue;
                
                // Get the post
                Post post = userPosts.stream()
                    .filter(p -> p.getId().equals(comment.getPostId()))
                    .findFirst()
                    .orElse(null);
                if (post == null) continue;
                
                NotificationItem notification = new NotificationItem();
                notification.setId(UUID.randomUUID());
                notification.setNotificationType("COMMENT");
                notification.setTitle("新评论");
                notification.setMessage(commenter.getUsername() + " 评论了您的帖子: " + 
                    (comment.getContent().length() > 50 ? 
                        comment.getContent().substring(0, 50) + "..." : 
                        comment.getContent()));
                notification.setActionUrl("/posts/" + post.getId());
                notification.setRead(false);
                notification.setFromUserId(commenter.getId());
                notification.setFromUserName(commenter.getUsername());
                notification.setFromUserAvatar(commenter.getAvatar());
                notification.setRelatedPostId(post.getId());
                notification.setRelatedPostTitle(post.getTitle());
                notification.setCreatedAt(comment.getCreatedAt());
                
                commentNotifications.add(notification);
            } catch (Exception e) {
                // Log error but continue processing
                System.err.println("Error processing comment notification: " + e.getMessage());
            }
        }
        
        return commentNotifications;
    }

    /**
     * Generate resonance notifications for a user (resonances on their posts)
     */
    private List<NotificationItem> generateResonanceNotifications(UUID userId) {
        List<NotificationItem> resonanceNotifications = new ArrayList<>();
        
        // Get user's posts
        List<Post> userPosts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<UUID> userPostIds = userPosts.stream().map(Post::getId).collect(Collectors.toList());
        
        if (userPostIds.isEmpty()) {
            return resonanceNotifications;
        }
        
        // Get recent resonances on user's posts
        List<PostResonance> recentResonances = postResonanceRepository.findByPostIdInOrderByResonancedAtDesc(userPostIds)
            .stream()
            .filter(resonance -> !resonance.getUserId().equals(userId)) // Exclude user's own resonances
            .limit(10) // Limit to recent 10 resonances
            .collect(Collectors.toList());
        
        for (PostResonance resonance : recentResonances) {
            try {
                // Get the user who made the resonance
                User resonanceUser = userRepository.findById(resonance.getUserId()).orElse(null);
                if (resonanceUser == null) continue;
                
                // Get the post
                Post post = userPosts.stream()
                    .filter(p -> p.getId().equals(resonance.getPostId()))
                    .findFirst()
                    .orElse(null);
                if (post == null) continue;
                
                NotificationItem notification = new NotificationItem();
                notification.setId(UUID.randomUUID());
                notification.setNotificationType("RESONANCE");
                notification.setTitle("新共鸣");
                notification.setMessage(resonanceUser.getUsername() + " 对您的帖子产生了共鸣");
                notification.setActionUrl("/posts/" + post.getId());
                notification.setRead(false);
                notification.setFromUserId(resonanceUser.getId());
                notification.setFromUserName(resonanceUser.getUsername());
                notification.setFromUserAvatar(resonanceUser.getAvatar());
                notification.setRelatedPostId(post.getId());
                notification.setRelatedPostTitle(post.getTitle());
                notification.setCreatedAt(resonance.getResonancedAt());
                
                resonanceNotifications.add(notification);
            } catch (Exception e) {
                // Log error but continue processing
                System.err.println("Error processing resonance notification: " + e.getMessage());
            }
        }
        
        return resonanceNotifications;
    }

    /**
     * Generate save/bookmark notifications for a user (saves on their posts)
     */
    private List<NotificationItem> generateSaveNotifications(UUID userId) {
        List<NotificationItem> saveNotifications = new ArrayList<>();
        
        // Get user's posts
        List<Post> userPosts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<UUID> userPostIds = userPosts.stream().map(Post::getId).collect(Collectors.toList());
        
        if (userPostIds.isEmpty()) {
            return saveNotifications;
        }
        
        // Get recent saves on user's posts
        List<PostCollection> recentSaves = postCollectionRepository.findByPostIdInOrderByCollectedAtDesc(userPostIds)
            .stream()
            .filter(collection -> !collection.getUserId().equals(userId)) // Exclude user's own saves
            .limit(10) // Limit to recent 10 saves
            .collect(Collectors.toList());
        
        for (PostCollection collection : recentSaves) {
            try {
                // Get the user who saved the post
                User saveUser = userRepository.findById(collection.getUserId()).orElse(null);
                if (saveUser == null) continue;
                
                // Get the post
                Post post = userPosts.stream()
                    .filter(p -> p.getId().equals(collection.getPostId()))
                    .findFirst()
                    .orElse(null);
                if (post == null) continue;
                
                NotificationItem notification = new NotificationItem();
                notification.setId(UUID.randomUUID());
                notification.setNotificationType("SAVE");
                notification.setTitle("新收藏");
                notification.setMessage(saveUser.getUsername() + " 收藏了您的帖子: \"" + 
                    (post.getTitle() != null && post.getTitle().length() > 30 ? 
                        post.getTitle().substring(0, 30) + "..." : 
                        post.getTitle() != null ? post.getTitle() : "无标题"));
                notification.setActionUrl("/posts/" + post.getId());
                notification.setRead(false);
                notification.setFromUserId(saveUser.getId());
                notification.setFromUserName(saveUser.getUsername());
                notification.setFromUserAvatar(saveUser.getAvatar());
                notification.setRelatedPostId(post.getId());
                notification.setRelatedPostTitle(post.getTitle());
                notification.setCreatedAt(collection.getCollectedAt());
                
                saveNotifications.add(notification);
            } catch (Exception e) {
                // Log error but continue processing
                System.err.println("Error processing save notification: " + e.getMessage());
            }
        }
        
        return saveNotifications;
    }

    /**
     * Mark a notification as read (placeholder implementation)
     */
    public void markAsRead(UUID notificationId, UUID userId) {
        // In a real implementation, this would update the notification's read status in the database
        // For now, this is a placeholder since we're generating notifications dynamically
        System.out.println("Marking notification " + notificationId + " as read for user " + userId);
    }

    /**
     * Mark all notifications as read for a user (placeholder implementation)
     */
    public void markAllAsRead(UUID userId) {
        // In a real implementation, this would update all notifications' read status in the database
        // For now, this is a placeholder since we're generating notifications dynamically
        System.out.println("Marking all notifications as read for user " + userId);
    }
}

package ai.youthmind.service;

import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.entity.UserEngagement;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserEngagementRepository;
import ai.youthmind.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EnhancedPostService {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserEngagementRepository userEngagementRepository;
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private AuditLogService auditLogService;
    
    // Feed Operations with Caching
    @Cacheable(value = "feed", key = "'general:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<Post> getFeed(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    @Cacheable(value = "feed", key = "'emotion:' + #emotion + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<Post> getFeedByEmotion(Post.Emotion emotion, Pageable pageable) {
        return postRepository.findByEmotionOrderByCreatedAtDesc(emotion, pageable);
    }
    
    @Cacheable(value = "trending", key = "'posts:' + #days + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<Post> getTrendingPosts(int days, Pageable pageable) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return postRepository.findMostResonantPosts(since, pageable);
    }
    
    // Post Operations
    @CachePut(value = "posts", key = "#result.id")
    public Post createPost(Post post) {
        // Validate and set defaults
        if (post.getEmotion() == null) {
            post.setEmotion(Post.Emotion.CONTENT);
        }
        
        Post savedPost = postRepository.save(post);
        
        // Update user post count
        updateUserPostCount(post.getUserId(), 1);
        
        // Invalidate related caches
        invalidateFeedCaches();
        
        // Log the creation
        auditLogService.logPostCreation(savedPost);
        
        return savedPost;
    }
    
    @Cacheable(value = "posts", key = "#id")
    public Optional<Post> getPostById(UUID id) {
        return postRepository.findById(id);
    }
    
    @CachePut(value = "posts", key = "#post.id")
    @CacheEvict(value = "feed", allEntries = true)
    public Post updatePost(Post post) {
        Post existingPost = postRepository.findById(post.getId())
            .orElseThrow(() -> new RuntimeException("Post not found"));
        
        // Update fields
        existingPost.setTitle(post.getTitle());
        existingPost.setContent(post.getContent());
        existingPost.setEmotion(post.getEmotion());
        existingPost.setTopics(post.getTopics());
        existingPost.setLastModifiedBy(post.getLastModifiedBy());
        
        Post updatedPost = postRepository.save(existingPost);
        
        // Log the update
        auditLogService.logPostUpdate(existingPost, updatedPost);
        
        return updatedPost;
    }
    
    @CacheEvict(value = {"posts", "feed", "trending"}, allEntries = true)
    public void deletePost(UUID postId, UUID userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));
        
        // Verify ownership
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("User not authorized to delete this post");
        }
        
        // Soft delete
        postRepository.softDeleteById(postId);
        
        // Update user post count
        updateUserPostCount(userId, -1);
        
        // Log the deletion
        auditLogService.logPostDeletion(post);
    }
    
    // Engagement Operations
    @Transactional
    public void addEngagement(UUID userId, UUID postId, UserEngagement.EngagementType type) {
        // Check if engagement already exists
        boolean exists = userEngagementRepository.existsByUserIdAndPostIdAndType(userId, postId, type);
        if (exists) {
            throw new RuntimeException("Engagement already exists");
        }
        
        // Create engagement
        UserEngagement engagement = new UserEngagement();
        engagement.setUserId(userId);
        engagement.setPostId(postId);
        engagement.setType(type);
        userEngagementRepository.save(engagement);
        
        // Update post counts
        updatePostEngagementCount(postId, type, 1);
        
        // Invalidate caches
        evictPostCache(postId);
        invalidateTrendingCaches();
    }
    
    @Transactional
    public void removeEngagement(UUID userId, UUID postId, UserEngagement.EngagementType type) {
        UserEngagement engagement = userEngagementRepository
            .findByUserIdAndPostIdAndType(userId, postId, type)
            .orElseThrow(() -> new RuntimeException("Engagement not found"));
        
        userEngagementRepository.delete(engagement);
        
        // Update post counts
        updatePostEngagementCount(postId, type, -1);
        
        // Invalidate caches
        evictPostCache(postId);
        invalidateTrendingCaches();
    }
    
    @Cacheable(value = "posts", key = "'user-engagements:' + #userId + ':' + #type")
    public List<UserEngagement> getUserEngagements(UUID userId, UserEngagement.EngagementType type) {
        return userEngagementRepository.findByUserIdAndType(userId, type);
    }
    
    // View Tracking
    @Transactional
    public void incrementViews(UUID postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));
        
        post.setViews(post.getViews() + 1);
        postRepository.save(post);
        
        // Update cache
        redisService.setValue("post:views:" + postId, post.getViews());
        evictPostCache(postId);
    }
    
    // Search Operations
    @Cacheable(value = "search", key = "'posts:' + #query + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<Post> searchPosts(String query, Pageable pageable) {
        return postRepository.searchPosts(query, pageable);
    }
    
    @Cacheable(value = "search", key = "'posts-emotion:' + #query + ':' + #emotion + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<Post> searchPostsByEmotion(String query, String emotion, Pageable pageable) {
        return postRepository.searchPostsByEmotion(query, emotion, pageable);
    }
    
    // Analytics Operations
    @Cacheable(value = "analytics", key = "'post-count:' + #days")
    public Long getPostCountSince(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return postRepository.countPostsSince(since);
    }
    
    @Cacheable(value = "analytics", key = "'user-post-count:' + #userId + ':' + #days")
    public Long getUserPostCountSince(UUID userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return postRepository.countUserPostsSince(userId, since);
    }
    
    @Cacheable(value = "analytics", key = "'emotion-distribution:' + #days")
    public List<Object[]> getEmotionDistribution(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return postRepository.getEmotionDistributionSince(since);
    }
    
    @Cacheable(value = "analytics", key = "'engagement-stats:' + #days")
    public List<Object[]> getEngagementStats(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return postRepository.getEngagementStatsSince(since);
    }
    
    // Moderation Operations
    @CacheEvict(value = {"posts", "feed"}, allEntries = true)
    public void flagPost(UUID postId, String reason, UUID moderatorId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));
        
        post.setFlagged(true);
        post.setFlagReason(reason);
        post.setFlaggedAt(LocalDateTime.now());
        post.setLastModifiedBy(moderatorId);
        
        postRepository.save(post);
        
        // Log the flagging
        auditLogService.logPostFlagged(post, reason, moderatorId);
    }
    
    @CacheEvict(value = {"posts", "feed"}, allEntries = true)
    public void unflagPost(UUID postId, UUID moderatorId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));
        
        post.setFlagged(false);
        post.setFlagReason(null);
        post.setFlaggedAt(null);
        post.setLastModifiedBy(moderatorId);
        
        postRepository.save(post);
        
        // Log the unflagging
        auditLogService.logPostUnflagged(post, moderatorId);
    }
    
    public Page<Post> getFlaggedPosts(Pageable pageable) {
        return postRepository.findFlaggedPosts(pageable);
    }
    
    // Utility Methods
    private void updatePostEngagementCount(UUID postId, UserEngagement.EngagementType type, int delta) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Post not found"));
        
        switch (type) {
            case RESONANCE:
                post.setResonanceCount(Math.max(0, post.getResonanceCount() + delta));
                break;
            case COLLECT:
                post.setCollectCount(Math.max(0, post.getCollectCount() + delta));
                break;
            case VIEW:
                post.setViews(Math.max(0, post.getViews() + delta));
                break;
        }
        
        postRepository.save(post);
    }
    
    private void updateUserPostCount(UUID userId, int delta) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setPostsCount(Math.max(0, user.getPostsCount() + delta));
        userRepository.save(user);
        
        // Invalidate user cache
        redisService.deleteValue("user:" + userId);
    }
    
    @CacheEvict(value = "posts", key = "#postId")
    private void evictPostCache(UUID postId) {
        // Cache eviction is handled by annotation
    }
    
    @CacheEvict(value = "feed", allEntries = true)
    private void invalidateFeedCaches() {
        // Cache eviction is handled by annotation
    }
    
    @CacheEvict(value = "trending", allEntries = true)
    private void invalidateTrendingCaches() {
        // Cache eviction is handled by annotation
    }
    
    // Batch Operations for Performance
    @Transactional
    public List<Post> createPostsBatch(List<Post> posts) {
        // Validate all posts
        posts.forEach(post -> {
            if (post.getEmotion() == null) {
                post.setEmotion(Post.Emotion.CONTENT);
            }
        });
        
        List<Post> savedPosts = postRepository.saveAll(posts);
        
        // Update user post counts in batch
        posts.stream()
            .collect(java.util.stream.Collectors.groupingBy(Post::getUserId))
            .forEach((userId, userPosts) -> updateUserPostCount(userId, userPosts.size()));
        
        // Invalidate caches
        invalidateFeedCaches();
        
        return savedPosts;
    }
    
    // Health Check Methods
    public boolean isHealthy() {
        try {
            // Test basic database connectivity
            postRepository.count();
            
            // Test cache connectivity
            redisService.setValue("health:posts", "ok");
            String healthValue = (String) redisService.getValue("health:posts");
            
            return "ok".equals(healthValue);
        } catch (Exception e) {
            return false;
        }
    }
}

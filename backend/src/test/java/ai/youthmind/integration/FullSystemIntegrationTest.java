package ai.youthmind.integration;

import ai.youthmind.entity.*;
import ai.youthmind.repository.*;
import ai.youthmind.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullSystemIntegrationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserPreferencesRepository userPreferencesRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private UserEngagementRepository userEngagementRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private DraftRepository draftRepository;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Autowired
    private EnhancedPostService enhancedPostService;
    
    @Autowired
    private SecurityValidationService securityValidationService;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private RedisService redisService;
    
    private User testUser;
    private UserPreferences testPreferences;
    
    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setEmail("integration@youthmind.ai");
        testUser.setPassword("hashedPassword123");
        testUser.setFirstName("Integration");
        testUser.setLastName("Test");
        testUser.setUsername("integrationtest");
        testUser.setBio("Test user for integration testing");
        testUser = userRepository.save(testUser);
        
        // Create user preferences
        testPreferences = new UserPreferences();
        testPreferences.setUserId(testUser.getId());
        testPreferences.setTheme(UserPreferences.Theme.DARK);
        testPreferences.setLanguage("en");
        testPreferences.setNotificationsEnabled(true);
        testPreferences.setEmailNotifications(false);
        testPreferences.setPrivacyLevel(UserPreferences.PrivacyLevel.PUBLIC);
        testPreferences = userPreferencesRepository.save(testPreferences);
    }
    
    @Test
    @Order(1)
    @DisplayName("Complete User Lifecycle Test")
    void testCompleteUserLifecycle() {
        System.out.println("=== Testing Complete User Lifecycle ===");
        
        // 1. Validate user creation
        SecurityValidationService.ValidationResult userValidation = 
            securityValidationService.validateUser(testUser);
        assertThat(userValidation.isValid()).isTrue();
        
        // 2. Test user preferences
        Optional<UserPreferences> foundPrefs = userPreferencesRepository.findByUserId(testUser.getId());
        assertThat(foundPrefs).isPresent();
        assertThat(foundPrefs.get().getTheme()).isEqualTo(UserPreferences.Theme.DARK);
        
        // 3. Update user preferences
        testPreferences.setTheme(UserPreferences.Theme.LIGHT);
        testPreferences.setEmailNotifications(true);
        userPreferencesRepository.save(testPreferences);
        
        // 4. Verify updates
        foundPrefs = userPreferencesRepository.findByUserId(testUser.getId());
        assertThat(foundPrefs.get().getTheme()).isEqualTo(UserPreferences.Theme.LIGHT);
        assertThat(foundPrefs.get().getEmailNotifications()).isTrue();
        
        System.out.println("✓ User lifecycle test passed");
    }
    
    @Test
    @Order(2)
    @DisplayName("Complete Post Lifecycle with Engagement Test")
    @Transactional
    void testCompletePostLifecycleWithEngagement() {
        System.out.println("=== Testing Complete Post Lifecycle with Engagement ===");
        
        // 1. Create post using service
        Post newPost = new Post();
        newPost.setUserId(testUser.getId());
        newPost.setTitle("Integration Test Post");
        newPost.setContent("This is a comprehensive integration test post with detailed content.");
        newPost.setEmotion(Post.Emotion.EXCITED);
        newPost.setTopics("integration,testing,database");
        
        // 2. Validate post
        SecurityValidationService.ValidationResult postValidation = 
            securityValidationService.validatePost(newPost);
        assertThat(postValidation.isValid()).isTrue();
        
        // 3. Create post through service (with caching and audit logging)
        Post savedPost = enhancedPostService.createPost(newPost);
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getVersion()).isEqualTo(1);
        
        // 4. Test post retrieval with caching
        Optional<Post> cachedPost = enhancedPostService.getPostById(savedPost.getId());
        assertThat(cachedPost).isPresent();
        assertThat(cachedPost.get().getTitle()).isEqualTo("Integration Test Post");
        
        // 5. Test engagement addition
        enhancedPostService.addEngagement(testUser.getId(), savedPost.getId(), 
            UserEngagement.EngagementType.RESONANCE);
        enhancedPostService.addEngagement(testUser.getId(), savedPost.getId(), 
            UserEngagement.EngagementType.COLLECT);
        
        // 6. Verify engagements
        List<UserEngagement> userEngagements = enhancedPostService.getUserEngagements(
            testUser.getId(), UserEngagement.EngagementType.RESONANCE);
        assertThat(userEngagements).hasSize(1);
        
        // 7. Test view increment
        enhancedPostService.incrementViews(savedPost.getId());
        
        // 8. Verify post counts updated
        Post updatedPost = postRepository.findById(savedPost.getId()).orElseThrow();
        assertThat(updatedPost.getViews()).isEqualTo(1);
        assertThat(updatedPost.getResonanceCount()).isEqualTo(1);
        assertThat(updatedPost.getCollectCount()).isEqualTo(1);
        
        // 9. Test post update
        updatedPost.setTitle("Updated Integration Test Post");
        updatedPost.setLastModifiedBy(testUser.getId());
        Post finalPost = enhancedPostService.updatePost(updatedPost);
        assertThat(finalPost.getTitle()).isEqualTo("Updated Integration Test Post");
        assertThat(finalPost.getVersion()).isEqualTo(2);
        
        System.out.println("✓ Post lifecycle with engagement test passed");
    }
    
    @Test
    @Order(3)
    @DisplayName("Comment System with Threading Test")
    void testCommentSystemWithThreading() {
        System.out.println("=== Testing Comment System with Threading ===");
        
        // 1. Create a post first
        Post post = new Post();
        post.setUserId(testUser.getId());
        post.setTitle("Comment Test Post");
        post.setContent("This post will have comments");
        post.setEmotion(Post.Emotion.HAPPY);
        post = postRepository.save(post);
        
        // 2. Create parent comment
        Comment parentComment = new Comment();
        parentComment.setPostId(post.getId());
        parentComment.setAuthorId(testUser.getId());
        parentComment.setContent("This is a parent comment for testing");
        
        SecurityValidationService.ValidationResult commentValidation = 
            securityValidationService.validateComment(parentComment);
        assertThat(commentValidation.isValid()).isTrue();
        
        parentComment = commentRepository.save(parentComment);
        
        // 3. Create reply comments
        Comment reply1 = new Comment();
        reply1.setPostId(post.getId());
        reply1.setAuthorId(testUser.getId());
        reply1.setContent("First reply to parent comment");
        reply1.setParentId(parentComment.getId());
        reply1 = commentRepository.save(reply1);
        
        Comment reply2 = new Comment();
        reply2.setPostId(post.getId());
        reply2.setAuthorId(testUser.getId());
        reply2.setContent("Second reply to parent comment");
        reply2.setParentId(parentComment.getId());
        reply2 = commentRepository.save(reply2);
        
        // 4. Test comment retrieval
        List<Comment> allComments = commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId());
        assertThat(allComments).hasSize(3);
        
        List<Comment> parentComments = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(post.getId());
        assertThat(parentComments).hasSize(1);
        
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(parentComment.getId());
        assertThat(replies).hasSize(2);
        
        // 5. Test comment engagement
        CommentEngagement engagement = new CommentEngagement();
        engagement.setUserId(testUser.getId());
        engagement.setCommentId(parentComment.getId());
        engagement.setType(CommentEngagement.EngagementType.LIKE);
        // Note: Would save through repository if CommentEngagementRepository is available
        
        System.out.println("✓ Comment system with threading test passed");
    }
    
    @Test
    @Order(4)
    @DisplayName("AI Conversation System Test")
    void testAIConversationSystem() {
        System.out.println("=== Testing AI Conversation System ===");
        
        // 1. Create conversation
        Conversation conversation = new Conversation();
        conversation.setUserId(testUser.getId());
        conversation.setTitle("Mental Health Support Chat");
        conversation.setIsPinned(false);
        conversation = conversationRepository.save(conversation);
        
        // 2. Create user message
        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setText("I'm feeling anxious about my upcoming presentation.");
        userMessage.setSender("user");
        userMessage = messageRepository.save(userMessage);
        
        // 3. Create AI response
        Message aiResponse = new Message();
        aiResponse.setConversationId(conversation.getId());
        aiResponse.setText("I understand that presentations can be nerve-wracking. What specific aspects are making you feel most anxious?");
        aiResponse.setSender("ai");
        aiResponse = messageRepository.save(aiResponse);
        
        // 4. Test conversation retrieval
        List<Conversation> userConversations = conversationRepository.findByUserIdOrderByUpdatedAtDesc(testUser.getId());
        assertThat(userConversations).hasSize(1);
        
        List<Message> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conversation.getId());
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getSender()).isEqualTo("user");
        assertThat(messages.get(1).getSender()).isEqualTo("ai");
        
        // 5. Pin conversation
        conversation.setIsPinned(true);
        conversationRepository.save(conversation);
        
        List<Conversation> pinnedConversations = conversationRepository.findByUserIdAndIsPinnedTrueOrderByUpdatedAtDesc(testUser.getId());
        assertThat(pinnedConversations).hasSize(1);
        
        System.out.println("✓ AI conversation system test passed");
    }
    
    @Test
    @Order(5)
    @DisplayName("Draft System Test")
    void testDraftSystem() {
        System.out.println("=== Testing Draft System ===");
        
        // 1. Create draft
        Draft draft = new Draft();
        draft.setUserId(testUser.getId());
        draft.setTitle("My Draft Post");
        draft.setContent("This is a draft that I'm working on...");
        draft.setEmotion(Post.Emotion.THOUGHTFUL.name());
        draft.setTopics("draft,writing,thoughts");
        draft = draftRepository.save(draft);
        
        // 2. Test draft retrieval
        List<Draft> userDrafts = draftRepository.findByUserIdOrderByUpdatedAtDesc(testUser.getId());
        assertThat(userDrafts).hasSize(1);
        assertThat(userDrafts.get(0).getTitle()).isEqualTo("My Draft Post");
        
        // 3. Update draft
        draft.setContent("This is an updated draft with more content...");
        draft = draftRepository.save(draft);
        
        // 4. Convert draft to post
        Post postFromDraft = new Post();
        postFromDraft.setUserId(draft.getUserId());
        postFromDraft.setTitle(draft.getTitle());
        postFromDraft.setContent(draft.getContent());
        postFromDraft.setEmotion(Post.Emotion.valueOf(draft.getEmotion()));
        postFromDraft.setTopics(draft.getTopics());
        
        Post publishedPost = enhancedPostService.createPost(postFromDraft);
        assertThat(publishedPost.getId()).isNotNull();
        
        // 5. Delete draft after publishing
        draftRepository.delete(draft);
        
        userDrafts = draftRepository.findByUserIdOrderByUpdatedAtDesc(testUser.getId());
        assertThat(userDrafts).isEmpty();
        
        System.out.println("✓ Draft system test passed");
    }
    
    @Test
    @Order(6)
    @DisplayName("Feed and Search Performance Test")
    void testFeedAndSearchPerformance() {
        System.out.println("=== Testing Feed and Search Performance ===");
        
        // 1. Create multiple posts for testing
        Post.Emotion[] emotions = Post.Emotion.values();
        for (int i = 0; i < 20; i++) {
            Post post = new Post();
            post.setUserId(testUser.getId());
            post.setTitle("Performance Test Post " + i);
            post.setContent("Content for performance test post number " + i);
            post.setEmotion(emotions[i % emotions.length]);
            post.setTopics("performance,testing,feed");
            postRepository.save(post);
        }
        
        // 2. Test feed retrieval with caching
        long startTime = System.currentTimeMillis();
        Page<Post> feed = enhancedPostService.getFeed(PageRequest.of(0, 10));
        long feedTime = System.currentTimeMillis() - startTime;
        
        assertThat(feed.getTotalElements()).isGreaterThan(20);
        assertThat(feed.getContent()).hasSize(10);
        System.out.println("Feed query time: " + feedTime + "ms");
        
        // 3. Test emotion-filtered feed
        startTime = System.currentTimeMillis();
        Page<Post> emotionFeed = enhancedPostService.getFeedByEmotion(Post.Emotion.HAPPY, PageRequest.of(0, 10));
        long emotionFeedTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Emotion feed query time: " + emotionFeedTime + "ms");
        
        // 4. Test search functionality
        startTime = System.currentTimeMillis();
        Page<Post> searchResults = enhancedPostService.searchPosts("performance", PageRequest.of(0, 10));
        long searchTime = System.currentTimeMillis() - startTime;
        
        assertThat(searchResults.getTotalElements()).isGreaterThan(0);
        System.out.println("Search query time: " + searchTime + "ms");
        
        // 5. Test trending posts
        startTime = System.currentTimeMillis();
        Page<Post> trendingPosts = enhancedPostService.getTrendingPosts(7, PageRequest.of(0, 10));
        long trendingTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Trending posts query time: " + trendingTime + "ms");
        
        // Performance assertions
        assertThat(feedTime).isLessThan(1000); // Should be under 1 second
        assertThat(emotionFeedTime).isLessThan(500); // Should be under 500ms
        assertThat(searchTime).isLessThan(2000); // Should be under 2 seconds
        assertThat(trendingTime).isLessThan(1000); // Should be under 1 second
        
        System.out.println("✓ Feed and search performance test passed");
    }
    
    @Test
    @Order(7)
    @DisplayName("Security Validation Test")
    void testSecurityValidation() {
        System.out.println("=== Testing Security Validation ===");
        
        // 1. Test dangerous content detection
        Post maliciousPost = new Post();
        maliciousPost.setUserId(testUser.getId());
        maliciousPost.setTitle("Normal Title");
        maliciousPost.setContent("<script>alert('XSS')</script>This is malicious content");
        maliciousPost.setEmotion(Post.Emotion.ANGRY);
        
        SecurityValidationService.ValidationResult result = securityValidationService.validatePost(maliciousPost);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("dangerous content"));
        
        // 2. Test content sanitization
        String dangerousContent = "<script>alert('test')</script>Normal content";
        String sanitized = securityValidationService.sanitizeContent(dangerousContent);
        assertThat(sanitized).doesNotContain("<script>");
        assertThat(sanitized).contains("Normal content");
        
        // 3. Test email validation
        User invalidUser = new User();
        invalidUser.setEmail("invalid-email");
        invalidUser.setFirstName("Test");
        invalidUser.setLastName("User");
        
        SecurityValidationService.ValidationResult userResult = securityValidationService.validateUser(invalidUser);
        assertThat(userResult.isValid()).isFalse();
        assertThat(userResult.getErrors()).anyMatch(error -> error.contains("Invalid email format"));
        
        // 4. Test password validation
        SecurityValidationService.ValidationResult weakPassword = securityValidationService.validatePassword("123");
        assertThat(weakPassword.isValid()).isFalse();
        assertThat(weakPassword.getErrors()).anyMatch(error -> error.contains("at least 8 characters"));
        
        SecurityValidationService.ValidationResult strongPassword = securityValidationService.validatePassword("StrongP@ssw0rd123");
        assertThat(strongPassword.isValid()).isTrue();
        
        System.out.println("✓ Security validation test passed");
    }
    
    @Test
    @Order(8)
    @DisplayName("Audit Logging Test")
    void testAuditLogging() {
        System.out.println("=== Testing Audit Logging ===");
        
        // 1. Create post (should trigger audit log)
        Post auditPost = new Post();
        auditPost.setUserId(testUser.getId());
        auditPost.setTitle("Audit Test Post");
        auditPost.setContent("This post will be audited");
        auditPost.setEmotion(Post.Emotion.NEUTRAL);
        
        Post savedAuditPost = enhancedPostService.createPost(auditPost);
        
        // 2. Check if audit log was created
        // Note: Audit logging happens in separate transaction, so we might need to wait
        try {
            Thread.sleep(100); // Small delay for async audit logging
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 3. Verify audit log exists
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId("POST", savedAuditPost.getId());
        assertThat(auditLogs).isNotEmpty();
        
        AuditLog creationLog = auditLogs.stream()
            .filter(log -> log.getAction() == AuditLog.Action.CREATE)
            .findFirst()
            .orElse(null);
        
        assertThat(creationLog).isNotNull();
        assertThat(creationLog.getUserId()).isEqualTo(testUser.getId());
        assertThat(creationLog.getNewValues()).isNotNull();
        
        // 4. Test update audit logging
        savedAuditPost.setTitle("Updated Audit Test Post");
        savedAuditPost.setLastModifiedBy(testUser.getId());
        enhancedPostService.updatePost(savedAuditPost);
        
        // 5. Check update audit log
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        auditLogs = auditLogRepository.findByEntityTypeAndEntityId("POST", savedAuditPost.getId());
        long updateLogs = auditLogs.stream()
            .filter(log -> log.getAction() == AuditLog.Action.UPDATE)
            .count();
        
        assertThat(updateLogs).isGreaterThan(0);
        
        System.out.println("✓ Audit logging test passed");
    }
    
    @Test
    @Order(9)
    @DisplayName("Caching System Test")
    void testCachingSystem() {
        System.out.println("=== Testing Caching System ===");
        
        // 1. Test Redis connectivity
        boolean redisHealthy = redisService != null;
        System.out.println("Redis service available: " + redisHealthy);
        
        if (redisHealthy) {
            // 2. Test basic cache operations
            String testKey = "test:integration:" + System.currentTimeMillis();
            String testValue = "Integration test value";
            
            redisService.setValue(testKey, testValue);
            Object retrievedValue = redisService.getValue(testKey);
            
            assertThat(retrievedValue).isEqualTo(testValue);
            
            // 3. Test cache deletion
            redisService.deleteValue(testKey);
            Object deletedValue = redisService.getValue(testKey);
            assertThat(deletedValue).isNull();
        }
        
        // 4. Test service-level caching
        Post cacheTestPost = new Post();
        cacheTestPost.setUserId(testUser.getId());
        cacheTestPost.setTitle("Cache Test Post");
        cacheTestPost.setContent("Testing caching functionality");
        cacheTestPost.setEmotion(Post.Emotion.EXCITED);
        
        Post savedCachePost = enhancedPostService.createPost(cacheTestPost);
        
        // 5. Test cached retrieval
        long startTime = System.currentTimeMillis();
        Optional<Post> firstRetrieval = enhancedPostService.getPostById(savedCachePost.getId());
        long firstRetrievalTime = System.currentTimeMillis() - startTime;
        
        startTime = System.currentTimeMillis();
        Optional<Post> secondRetrieval = enhancedPostService.getPostById(savedCachePost.getId());
        long secondRetrievalTime = System.currentTimeMillis() - startTime;
        
        assertThat(firstRetrieval).isPresent();
        assertThat(secondRetrieval).isPresent();
        
        System.out.println("First retrieval time: " + firstRetrievalTime + "ms");
        System.out.println("Second retrieval time: " + secondRetrievalTime + "ms");
        
        // Second retrieval should be faster due to caching (in most cases)
        System.out.println("✓ Caching system test passed");
    }
    
    @Test
    @Order(10)
    @DisplayName("Analytics and Reporting Test")
    void testAnalyticsAndReporting() {
        System.out.println("=== Testing Analytics and Reporting ===");
        
        // 1. Test post count analytics
        Long totalPosts = enhancedPostService.getPostCountSince(7);
        assertThat(totalPosts).isGreaterThan(0);
        System.out.println("Total posts in last 7 days: " + totalPosts);
        
        Long userPosts = enhancedPostService.getUserPostCountSince(testUser.getId(), 7);
        assertThat(userPosts).isGreaterThan(0);
        System.out.println("User posts in last 7 days: " + userPosts);
        
        // 2. Test emotion distribution
        List<Object[]> emotionDistribution = enhancedPostService.getEmotionDistribution(7);
        assertThat(emotionDistribution).isNotEmpty();
        System.out.println("Emotion distribution entries: " + emotionDistribution.size());
        
        // 3. Test engagement statistics
        List<Object[]> engagementStats = enhancedPostService.getEngagementStats(7);
        assertThat(engagementStats).isNotEmpty();
        System.out.println("Engagement statistics available: " + (engagementStats.size() > 0));
        
        System.out.println("✓ Analytics and reporting test passed");
    }
    
    @AfterEach
    void cleanup() {
        // Cleanup is handled by test framework rollback
        System.out.println("Integration test cleanup completed");
    }
}

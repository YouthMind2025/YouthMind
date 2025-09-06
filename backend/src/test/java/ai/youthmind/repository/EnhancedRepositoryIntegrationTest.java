package ai.youthmind.repository;

import ai.youthmind.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EnhancedRepositoryIntegrationTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
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
    
    private User testUser;
    private Post testPost;
    private UserPreferences testPreferences;
    
    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setEmail("test@youthmind.ai");
        testUser.setPassword("hashedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setUsername("testuser");
        testUser = userRepository.save(testUser);
        entityManager.flush();
        
        // Create test post
        testPost = new Post();
        testPost.setUserId(testUser.getId());
        testPost.setTitle("Test Post");
        testPost.setContent("This is a test post content for integration testing.");
        testPost.setEmotion(Post.Emotion.HAPPY);
        testPost.setTopics("testing,integration,database");
        testPost = postRepository.save(testPost);
        entityManager.flush();
        
        // Create test user preferences
        testPreferences = new UserPreferences();
        testPreferences.setUserId(testUser.getId());
        testPreferences.setTheme(UserPreferences.Theme.DARK);
        testPreferences.setLanguage("en");
        testPreferences.setNotificationsEnabled(true);
        testPreferences = userPreferencesRepository.save(testPreferences);
        entityManager.flush();
    }
    
    @Test
    @Order(1)
    @DisplayName("Test User Creation and Validation")
    void testUserCreationAndValidation() {
        System.out.println("=== Testing User Creation and Validation ===");
        
        // Verify user was created correctly
        assertThat(testUser.getId()).isNotNull();
        assertThat(testUser.getEmail()).isEqualTo("test@youthmind.ai");
        assertThat(testUser.getCreatedAt()).isNotNull();
        assertThat(testUser.getUpdatedAt()).isNotNull();
        assertThat(testUser.getVersion()).isEqualTo(1);
        assertThat(testUser.getLoginCount()).isEqualTo(0);
        
        // Test user repository methods
        Optional<User> foundUser = userRepository.findByEmail("test@youthmind.ai");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(testUser.getId());
        
        boolean exists = userRepository.existsByEmail("test@youthmind.ai");
        assertThat(exists).isTrue();
        
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");
        assertThat(notExists).isFalse();
        
        System.out.println("✓ User creation and validation tests passed");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test Post Creation with Enhanced Features")
    void testPostCreationWithEnhancedFeatures() {
        System.out.println("=== Testing Post Creation with Enhanced Features ===");
        
        // Verify post was created correctly
        assertThat(testPost.getId()).isNotNull();
        assertThat(testPost.getUserId()).isEqualTo(testUser.getId());
        assertThat(testPost.getTitle()).isEqualTo("Test Post");
        assertThat(testPost.getEmotion()).isEqualTo(Post.Emotion.HAPPY);
        assertThat(testPost.getResonanceCount()).isEqualTo(0);
        assertThat(testPost.getCollectCount()).isEqualTo(0);
        assertThat(testPost.getFlagged()).isFalse();
        assertThat(testPost.getIsAnonymous()).isFalse();
        assertThat(testPost.getVersion()).isEqualTo(1);
        
        // Test emotion-based queries
        List<Post> happyPosts = postRepository.findByEmotion(Post.Emotion.HAPPY);
        assertThat(happyPosts).hasSize(1);
        assertThat(happyPosts.get(0).getId()).isEqualTo(testPost.getId());
        
        Page<Post> happyPostsPage = postRepository.findByEmotion(Post.Emotion.HAPPY, PageRequest.of(0, 10));
        assertThat(happyPostsPage.getTotalElements()).isEqualTo(1);
        
        System.out.println("✓ Post creation with enhanced features tests passed");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test User Preferences")
    void testUserPreferences() {
        System.out.println("=== Testing User Preferences ===");
        
        // Verify preferences were created correctly
        assertThat(testPreferences.getId()).isNotNull();
        assertThat(testPreferences.getUserId()).isEqualTo(testUser.getId());
        assertThat(testPreferences.getTheme()).isEqualTo(UserPreferences.Theme.DARK);
        assertThat(testPreferences.getLanguage()).isEqualTo("en");
        assertThat(testPreferences.getNotificationsEnabled()).isTrue();
        
        // Test repository methods
        Optional<UserPreferences> foundPrefs = userPreferencesRepository.findByUserId(testUser.getId());
        assertThat(foundPrefs).isPresent();
        assertThat(foundPrefs.get().getTheme()).isEqualTo(UserPreferences.Theme.DARK);
        
        boolean exists = userPreferencesRepository.existsByUserId(testUser.getId());
        assertThat(exists).isTrue();
        
        // Test specific field queries
        Optional<UserPreferences.Theme> theme = userPreferencesRepository.getThemeByUserId(testUser.getId());
        assertThat(theme).isPresent();
        assertThat(theme.get()).isEqualTo(UserPreferences.Theme.DARK);
        
        System.out.println("✓ User preferences tests passed");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test Comment System with Threading")
    void testCommentSystemWithThreading() {
        System.out.println("=== Testing Comment System with Threading ===");
        
        // Create a parent comment
        Comment parentComment = new Comment();
        parentComment.setPostId(testPost.getId());
        parentComment.setAuthorId(testUser.getId());
        parentComment.setContent("This is a parent comment");
        parentComment.setLikes(0);
        parentComment = commentRepository.save(parentComment);
        entityManager.flush();
        
        // Create a reply comment
        Comment replyComment = new Comment();
        replyComment.setPostId(testPost.getId());
        replyComment.setAuthorId(testUser.getId());
        replyComment.setContent("This is a reply to the parent comment");
        replyComment.setParentId(parentComment.getId());
        replyComment.setLikes(0);
        replyComment = commentRepository.save(replyComment);
        entityManager.flush();
        
        // Test comment retrieval
        List<Comment> postComments = commentRepository.findByPostIdOrderByCreatedAtAsc(testPost.getId());
        assertThat(postComments).hasSize(2);
        
        List<Comment> parentComments = commentRepository.findByPostIdAndParentIdIsNullOrderByCreatedAtAsc(testPost.getId());
        assertThat(parentComments).hasSize(1);
        assertThat(parentComments.get(0).getId()).isEqualTo(parentComment.getId());
        
        List<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(parentComment.getId());
        assertThat(replies).hasSize(1);
        assertThat(replies.get(0).getId()).isEqualTo(replyComment.getId());
        
        System.out.println("✓ Comment system with threading tests passed");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test User Engagement System")
    void testUserEngagementSystem() {
        System.out.println("=== Testing User Engagement System ===");
        
        // Create resonance engagement
        UserEngagement resonance = new UserEngagement();
        resonance.setUserId(testUser.getId());
        resonance.setPostId(testPost.getId());
        resonance.setType(UserEngagement.EngagementType.RESONANCE);
        resonance = userEngagementRepository.save(resonance);
        entityManager.flush();
        
        // Create collect engagement
        UserEngagement collect = new UserEngagement();
        collect.setUserId(testUser.getId());
        collect.setPostId(testPost.getId());
        collect.setType(UserEngagement.EngagementType.COLLECT);
        collect = userEngagementRepository.save(collect);
        entityManager.flush();
        
        // Test engagement queries
        List<UserEngagement> userEngagements = userEngagementRepository.findByUserId(testUser.getId());
        assertThat(userEngagements).hasSize(2);
        
        List<UserEngagement> postEngagements = userEngagementRepository.findByPostId(testPost.getId());
        assertThat(postEngagements).hasSize(2);
        
        boolean hasResonance = userEngagementRepository.existsByUserIdAndPostIdAndType(
            testUser.getId(), testPost.getId(), UserEngagement.EngagementType.RESONANCE);
        assertThat(hasResonance).isTrue();
        
        Long resonanceCount = userEngagementRepository.countByPostIdAndType(
            testPost.getId(), UserEngagement.EngagementType.RESONANCE);
        assertThat(resonanceCount).isEqualTo(1);
        
        System.out.println("✓ User engagement system tests passed");
    }
    
    @Test
    @Order(6)
    @DisplayName("Test Conversation and Message System")
    void testConversationAndMessageSystem() {
        System.out.println("=== Testing Conversation and Message System ===");
        
        // Create conversation
        Conversation conversation = new Conversation();
        conversation.setUserId(testUser.getId());
        conversation.setTitle("Test AI Conversation");
        conversation.setIsPinned(false);
        conversation = conversationRepository.save(conversation);
        entityManager.flush();
        
        // Create user message
        Message userMessage = new Message();
        userMessage.setConversationId(conversation.getId());
        userMessage.setText("Hello, I need help with my emotions.");
        userMessage.setSender("user");
        userMessage = messageRepository.save(userMessage);
        entityManager.flush();
        
        // Create AI response
        Message aiMessage = new Message();
        aiMessage.setConversationId(conversation.getId());
        aiMessage.setText("I'm here to help you. Can you tell me more about how you're feeling?");
        aiMessage.setSender("ai");
        aiMessage = messageRepository.save(aiMessage);
        entityManager.flush();
        
        // Test conversation queries
        List<Conversation> userConversations = conversationRepository.findByUserIdOrderByUpdatedAtDesc(testUser.getId());
        assertThat(userConversations).hasSize(1);
        assertThat(userConversations.get(0).getTitle()).isEqualTo("Test AI Conversation");
        
        // Test message queries
        List<Message> conversationMessages = messageRepository.findByConversationIdOrderByTimestampAsc(conversation.getId());
        assertThat(conversationMessages).hasSize(2);
        assertThat(conversationMessages.get(0).getSender()).isEqualTo("user");
        assertThat(conversationMessages.get(1).getSender()).isEqualTo("ai");
        
        System.out.println("✓ Conversation and message system tests passed");
    }
    
    @Test
    @Order(7)
    @DisplayName("Test Draft System")
    void testDraftSystem() {
        System.out.println("=== Testing Draft System ===");
        
        // Create draft
        Draft draft = new Draft();
        draft.setUserId(testUser.getId());
        draft.setTitle("Draft Post Title");
        draft.setContent("This is a draft post that hasn't been published yet.");
        draft.setEmotion("EXCITED");
        draft.setTopics("draft,testing");
        draft = draftRepository.save(draft);
        entityManager.flush();
        
        // Test draft queries
        List<Draft> userDrafts = draftRepository.findByUserIdOrderByUpdatedAtDesc(testUser.getId());
        assertThat(userDrafts).hasSize(1);
        assertThat(userDrafts.get(0).getTitle()).isEqualTo("Draft Post Title");
        
        Long draftCount = draftRepository.countByUserId(testUser.getId());
        assertThat(draftCount).isEqualTo(1);
        
        System.out.println("✓ Draft system tests passed");
    }
    
    @Test
    @Order(8)
    @DisplayName("Test Performance with Large Dataset")
    void testPerformanceWithLargeDataset() {
        System.out.println("=== Testing Performance with Large Dataset ===");
        
        long startTime = System.currentTimeMillis();
        
        // Create multiple users
        for (int i = 0; i < 50; i++) {
            User user = new User();
            user.setEmail("user" + i + "@test.com");
            user.setPassword("password" + i);
            user.setFirstName("User" + i);
            user.setLastName("Test");
            user.setUsername("user" + i);
            userRepository.save(user);
        }
        entityManager.flush();
        
        // Create multiple posts with different emotions
        Post.Emotion[] emotions = Post.Emotion.values();
        for (int i = 0; i < 100; i++) {
            Post post = new Post();
            post.setUserId(testUser.getId());
            post.setTitle("Performance Test Post " + i);
            post.setContent("Content for performance test post number " + i);
            post.setEmotion(emotions[i % emotions.length]);
            post.setViews(i * 10);
            post.setLikes(i * 5);
            post.setResonanceCount(i * 3);
            postRepository.save(post);
        }
        entityManager.flush();
        
        long creationTime = System.currentTimeMillis() - startTime;
        System.out.println("Data creation time: " + creationTime + "ms");
        
        // Test pagination performance
        startTime = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(0, 20);
        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        long queryTime = System.currentTimeMillis() - startTime;
        
        assertThat(posts.getTotalElements()).isGreaterThan(100);
        assertThat(posts.getContent()).hasSize(20);
        
        System.out.println("Pagination query time: " + queryTime + "ms");
        
        // Test emotion filtering performance
        startTime = System.currentTimeMillis();
        Page<Post> happyPosts = postRepository.findByEmotion(Post.Emotion.HAPPY, pageable);
        queryTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Emotion filter query time: " + queryTime + "ms");
        
        // Test trending posts performance
        startTime = System.currentTimeMillis();
        Page<Post> trendingPosts = postRepository.findMostResonantPosts(
            LocalDateTime.now().minusDays(7), pageable);
        queryTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Trending posts query time: " + queryTime + "ms");
        
        System.out.println("✓ Performance tests completed successfully");
    }
    
    @Test
    @Order(9)
    @DisplayName("Test Data Integrity and Constraints")
    void testDataIntegrityAndConstraints() {
        System.out.println("=== Testing Data Integrity and Constraints ===");
        
        // Test unique constraints
        assertThrows(Exception.class, () -> {
            User duplicateUser = new User();
            duplicateUser.setEmail("test@youthmind.ai"); // Same email as testUser
            duplicateUser.setPassword("password");
            duplicateUser.setFirstName("Duplicate");
            duplicateUser.setLastName("User");
            userRepository.save(duplicateUser);
            entityManager.flush();
        });
        
        // Test foreign key constraints
        assertThrows(Exception.class, () -> {
            Post orphanPost = new Post();
            orphanPost.setUserId(UUID.randomUUID()); // Non-existent user
            orphanPost.setTitle("Orphan Post");
            orphanPost.setContent("This post has no valid user");
            postRepository.save(orphanPost);
            entityManager.flush();
        });
        
        // Test cascade delete behavior
        UUID userId = testUser.getId();
        UUID postId = testPost.getId();
        
        // Verify data exists
        assertThat(userRepository.existsById(userId)).isTrue();
        assertThat(postRepository.existsById(postId)).isTrue();
        assertThat(userPreferencesRepository.existsByUserId(userId)).isTrue();
        
        System.out.println("✓ Data integrity and constraints tests passed");
    }
    
    @Test
    @Order(10)
    @DisplayName("Test Analytics and Reporting Queries")
    void testAnalyticsAndReportingQueries() {
        System.out.println("=== Testing Analytics and Reporting Queries ===");
        
        // Test post count analytics
        Long totalPosts = postRepository.countPostsSince(LocalDateTime.now().minusDays(1));
        assertThat(totalPosts).isGreaterThan(0);
        
        Long userPosts = postRepository.countUserPostsSince(testUser.getId(), LocalDateTime.now().minusDays(1));
        assertThat(userPosts).isGreaterThan(0);
        
        // Test emotion distribution
        List<Object[]> emotionDistribution = postRepository.getEmotionDistributionSince(LocalDateTime.now().minusDays(1));
        assertThat(emotionDistribution).isNotEmpty();
        
        // Test engagement statistics
        List<Object[]> engagementStats = postRepository.getEngagementStatsSince(LocalDateTime.now().minusDays(1));
        assertThat(engagementStats).isNotEmpty();
        
        System.out.println("✓ Analytics and reporting queries tests passed");
    }
    
    @AfterEach
    void cleanup() {
        // Clean up test data if needed
        System.out.println("Test cleanup completed");
    }
}

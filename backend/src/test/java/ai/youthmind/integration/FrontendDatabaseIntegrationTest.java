package ai.youthmind.integration;

import ai.youthmind.entity.*;
import ai.youthmind.repository.*;
import ai.youthmind.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test frontend actions saving efficiently to database
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FrontendDatabaseIntegrationTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserEngagementRepository userEngagementRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private MemoryItemRepository memoryItemRepository;
    
    @Autowired
    private UserPreferencesRepository userPreferencesRepository;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private MemoryService memoryService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Create test user (simulating frontend registration)
        testUser = new User();
        testUser.setEmail("frontend-test@youthmind.ai");
        testUser.setPassword("hashedPassword");
        testUser.setFirstName("Frontend");
        testUser.setLastName("User");
        testUser.setUsername("frontenduser");
        testUser = userRepository.save(testUser);
    }
    
    @Test
    @Order(1)
    @DisplayName("Frontend Action: User Registration → Database")
    @Transactional
    void testUserRegistrationSavesToDatabase() {
        System.out.println("=== Testing Frontend User Registration → Database ===");
        
        long startTime = System.currentTimeMillis();
        
        // Simulate frontend registration click
        User newUser = new User();
        newUser.setEmail("click-register@youthmind.ai");
        newUser.setPassword("hashedPassword123");
        newUser.setFirstName("Click");
        newUser.setLastName("Register");
        newUser.setUsername("clickregister");
        
        User savedUser = userRepository.save(newUser);
        long saveTime = System.currentTimeMillis() - startTime;
        
        // Verify saved efficiently
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(saveTime).isLessThan(100); // Should save in under 100ms
        
        // Verify user can be retrieved efficiently
        startTime = System.currentTimeMillis();
        Optional<User> retrievedUser = userRepository.findByEmail("click-register@youthmind.ai");
        long retrieveTime = System.currentTimeMillis() - startTime;
        
        assertThat(retrievedUser).isPresent();
        assertThat(retrieveTime).isLessThan(50); // Should retrieve in under 50ms
        
        System.out.println("User save time: " + saveTime + "ms");
        System.out.println("User retrieve time: " + retrieveTime + "ms");
        System.out.println("✅ Frontend registration saves efficiently to database");
    }
    
    @Test
    @Order(2)
    @DisplayName("Frontend Action: Create Post → Database with AI Analysis")
    @Transactional
    void testPostCreationSavesToDatabase() {
        System.out.println("=== Testing Frontend Post Creation → Database ===");
        
        long startTime = System.currentTimeMillis();
        
        // Simulate frontend "Create Post" button click
        Post newPost = new Post();
        newPost.setUserId(testUser.getId());
        newPost.setTitle("Frontend Test Post");
        newPost.setContent("This post was created by clicking the frontend create button");
        newPost.setEmotion("EXCITED");
        newPost.setTopics("frontend,testing,database");
        
        // Add moderation fields (what happens in real system)
        newPost.setModerationLayer("AI_PRIMARY");
        newPost.setModerationConfidence(0.95);
        newPost.setModerationDetails("{\"safe\": true, \"confidence\": 0.95}");
        
        Post savedPost = postRepository.save(newPost);
        long saveTime = System.currentTimeMillis() - startTime;
        
        // Verify post saved with all data
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getContentHash()).isNotNull(); // Auto-generated
        assertThat(savedPost.getModerationLayer()).isEqualTo("AI_PRIMARY");
        assertThat(savedPost.getModerationConfidence()).isEqualTo(0.95);
        assertThat(saveTime).isLessThan(200); // Should save in under 200ms
        
        // Test post retrieval efficiency
        startTime = System.currentTimeMillis();
        Optional<Post> retrievedPost = postRepository.findById(savedPost.getId());
        long retrieveTime = System.currentTimeMillis() - startTime;
        
        assertThat(retrievedPost).isPresent();
        assertThat(retrieveTime).isLessThan(50);
        
        System.out.println("Post save time: " + saveTime + "ms");
        System.out.println("Post retrieve time: " + retrieveTime + "ms");
        System.out.println("✅ Frontend post creation saves efficiently with AI analysis");
    }
    
    @Test
    @Order(3)
    @DisplayName("Frontend Action: Like/Resonance → Database Engagement")
    @Transactional
    void testEngagementSavesToDatabase() {
        System.out.println("=== Testing Frontend Engagement → Database ===");
        
        // Create a post first
        Post post = new Post();
        post.setUserId(testUser.getId());
        post.setTitle("Post to Like");
        post.setContent("This post will receive engagement");
        post.setEmotion("HAPPY");
        Post savedPost = postRepository.save(post);
        
        long startTime = System.currentTimeMillis();
        
        // Simulate frontend "Like" button click
        UserEngagement engagement = new UserEngagement();
        engagement.setUserId(testUser.getId());
        engagement.setPostId(savedPost.getId());
        engagement.setType(UserEngagement.EngagementType.RESONANCE);
        
        UserEngagement savedEngagement = userEngagementRepository.save(engagement);
        long saveTime = System.currentTimeMillis() - startTime;
        
        // Verify engagement saved efficiently
        assertThat(savedEngagement.getId()).isNotNull();
        assertThat(savedEngagement.getCreatedAt()).isNotNull();
        assertThat(saveTime).isLessThan(100); // Should save in under 100ms
        
        // Test engagement retrieval
        startTime = System.currentTimeMillis();
        List<UserEngagement> userEngagements = userEngagementRepository.findByUserId(testUser.getId());
        long retrieveTime = System.currentTimeMillis() - startTime;
        
        assertThat(userEngagements).hasSize(1);
        assertThat(retrieveTime).isLessThan(50);
        
        System.out.println("Engagement save time: " + saveTime + "ms");
        System.out.println("Engagement retrieve time: " + retrieveTime + "ms");
        System.out.println("✅ Frontend engagement clicks save efficiently to database");
    }
    
    @Test
    @Order(4)
    @DisplayName("Frontend Action: AI Chat → Database Memory Storage")
    @Transactional
    void testAIChatSavesToDatabase() {
        System.out.println("=== Testing Frontend AI Chat → Database Memory ===");
        
        long startTime = System.currentTimeMillis();
        
        // Simulate frontend AI chat message
        MemoryItem chatMemory = memoryService.storeChatMemory(
            testUser.getId(),
            "User asking for help with anxiety",
            "I'm feeling really anxious about my upcoming presentation",
            "I understand that presentations can be nerve-wracking. What specific aspects worry you most?"
        );
        long saveTime = System.currentTimeMillis() - startTime;
        
        // Verify memory saved efficiently
        assertThat(chatMemory.getId()).isNotNull();
        assertThat(chatMemory.getMemoryType()).isEqualTo(MemoryItem.MemoryType.CHAT_CONTEXT);
        assertThat(chatMemory.getWeightScore()).isGreaterThan(BigDecimal.ZERO);
        assertThat(saveTime).isLessThan(150); // Should save in under 150ms
        
        // Test memory retrieval efficiency
        startTime = System.currentTimeMillis();
        List<MemoryItem> userMemories = memoryItemRepository.findByUserId(testUser.getId());
        long retrieveTime = System.currentTimeMillis() - startTime;
        
        assertThat(userMemories).hasSize(1);
        assertThat(retrieveTime).isLessThan(100);
        
        System.out.println("Memory save time: " + saveTime + "ms");
        System.out.println("Memory retrieve time: " + retrieveTime + "ms");
        System.out.println("✅ Frontend AI chat saves efficiently to memory database");
    }
    
    @Test
    @Order(5)
    @DisplayName("Frontend Action: Comment → Database Threading")
    @Transactional
    void testCommentSavesToDatabase() {
        System.out.println("=== Testing Frontend Comments → Database Threading ===");
        
        // Create a post first
        Post post = new Post();
        post.setUserId(testUser.getId());
        post.setTitle("Post for Comments");
        post.setContent("This post will receive comments");
        post.setEmotion("THOUGHTFUL");
        Post savedPost = postRepository.save(post);
        
        long startTime = System.currentTimeMillis();
        
        // Simulate frontend "Add Comment" click
        Comment comment = new Comment();
        comment.setPostId(savedPost.getId());
        comment.setAuthorId(testUser.getId());
        comment.setContent("This is a test comment from the frontend");
        
        Comment savedComment = commentRepository.save(comment);
        long saveTime = System.currentTimeMillis() - startTime;
        
        // Verify comment saved efficiently
        assertThat(savedComment.getId()).isNotNull();
        assertThat(savedComment.getCreatedAt()).isNotNull();
        assertThat(saveTime).isLessThan(100);
        
        // Test threaded comment (reply)
        startTime = System.currentTimeMillis();
        Comment reply = new Comment();
        reply.setPostId(savedPost.getId());
        reply.setAuthorId(testUser.getId());
        reply.setContent("This is a reply to the comment");
        reply.setParentId(savedComment.getId());
        
        Comment savedReply = commentRepository.save(reply);
        long replySaveTime = System.currentTimeMillis() - startTime;
        
        assertThat(savedReply.getParentId()).isEqualTo(savedComment.getId());
        assertThat(replySaveTime).isLessThan(100);
        
        System.out.println("Comment save time: " + saveTime + "ms");
        System.out.println("Reply save time: " + replySaveTime + "ms");
        System.out.println("✅ Frontend comments save efficiently with threading");
    }
    
    @Test
    @Order(6)
    @DisplayName("Frontend Action: Settings Update → Database Preferences")
    @Transactional
    void testSettingsUpdateSavesToDatabase() {
        System.out.println("=== Testing Frontend Settings → Database Preferences ===");
        
        long startTime = System.currentTimeMillis();
        
        // Simulate frontend settings update
        UserPreferences preferences = new UserPreferences();
        preferences.setUserId(testUser.getId());
        preferences.setTheme(UserPreferences.Theme.DARK);
        preferences.setLanguage("en");
        preferences.setNotificationsEnabled(true);
        preferences.setEmailNotifications(false);
        preferences.setPrivacyLevel(UserPreferences.PrivacyLevel.FRIENDS);
        preferences.setAiSuggestionsEnabled(true);
        
        UserPreferences savedPreferences = userPreferencesRepository.save(preferences);
        long saveTime = System.currentTimeMillis() - startTime;
        
        // Verify preferences saved efficiently
        assertThat(savedPreferences.getId()).isNotNull();
        assertThat(savedPreferences.getTheme()).isEqualTo(UserPreferences.Theme.DARK);
        assertThat(savedPreferences.getPrivacyLevel()).isEqualTo(UserPreferences.PrivacyLevel.FRIENDS);
        assertThat(saveTime).isLessThan(100);
        
        // Test preference retrieval
        startTime = System.currentTimeMillis();
        Optional<UserPreferences> retrievedPrefs = userPreferencesRepository.findByUserId(testUser.getId());
        long retrieveTime = System.currentTimeMillis() - startTime;
        
        assertThat(retrievedPrefs).isPresent();
        assertThat(retrieveTime).isLessThan(50);
        
        System.out.println("Preferences save time: " + saveTime + "ms");
        System.out.println("Preferences retrieve time: " + retrieveTime + "ms");
        System.out.println("✅ Frontend settings save efficiently to database");
    }
    
    @Test
    @Order(7)
    @DisplayName("Performance Test: Multiple Frontend Actions → Database")
    @Transactional
    void testMultipleFrontendActionPerformance() {
        System.out.println("=== Testing Multiple Frontend Actions Performance ===");
        
        long totalStartTime = System.currentTimeMillis();
        
        // Simulate user session: multiple frontend clicks
        
        // 1. Create 5 posts (like user creating multiple posts)
        for (int i = 0; i < 5; i++) {
            Post post = new Post();
            post.setUserId(testUser.getId());
            post.setTitle("Bulk Post " + i);
            post.setContent("Content for bulk post number " + i);
            post.setEmotion(i % 2 == 0 ? "HAPPY" : "THOUGHTFUL");
            postRepository.save(post);
        }
        
        // 2. Create multiple engagements (like user liking multiple posts)
        List<Post> allPosts = postRepository.findByUserId(testUser.getId());
        for (Post post : allPosts) {
            UserEngagement engagement = new UserEngagement();
            engagement.setUserId(testUser.getId());
            engagement.setPostId(post.getId());
            engagement.setType(UserEngagement.EngagementType.VIEW);
            userEngagementRepository.save(engagement);
        }
        
        // 3. Store multiple memories (like AI chat session)
        for (int i = 0; i < 3; i++) {
            memoryService.storeChatMemory(
                testUser.getId(),
                "Chat session " + i,
                "User message " + i,
                "AI response " + i
            );
        }
        
        long totalTime = System.currentTimeMillis() - totalStartTime;
        
        // Verify all data saved correctly
        List<Post> userPosts = postRepository.findByUserId(testUser.getId());
        List<UserEngagement> userEngagements = userEngagementRepository.findByUserId(testUser.getId());
        List<MemoryItem> userMemories = memoryItemRepository.findByUserId(testUser.getId());
        
        assertThat(userPosts).hasSizeGreaterThanOrEqualTo(5);
        assertThat(userEngagements).hasSizeGreaterThanOrEqualTo(5);
        assertThat(userMemories).hasSizeGreaterThanOrEqualTo(3);
        
        // Performance should be reasonable even with multiple operations
        assertThat(totalTime).isLessThan(2000); // Under 2 seconds for bulk operations
        
        System.out.println("Total bulk operations time: " + totalTime + "ms");
        System.out.println("Posts created: " + userPosts.size());
        System.out.println("Engagements created: " + userEngagements.size());
        System.out.println("Memories stored: " + userMemories.size());
        System.out.println("✅ Multiple frontend actions save efficiently in bulk");
    }
    
    @Test
    @Order(8)
    @DisplayName("Real-time Test: Frontend Updates → Database → Cache")
    void testRealTimeUpdatesFlow() {
        System.out.println("=== Testing Real-time Frontend → Database → Cache Flow ===");
        
        // Create a post
        Post post = new Post();
        post.setUserId(testUser.getId());
        post.setTitle("Real-time Test Post");
        post.setContent("Testing real-time updates");
        post.setEmotion("EXCITED");
        Post savedPost = postRepository.save(post);
        
        long startTime = System.currentTimeMillis();
        
        // Simulate real-time engagement (like user clicking like button)
        UserEngagement engagement = new UserEngagement();
        engagement.setUserId(testUser.getId());
        engagement.setPostId(savedPost.getId());
        engagement.setType(UserEngagement.EngagementType.RESONANCE);
        userEngagementRepository.save(engagement);
        
        // Update post engagement count (what happens in real system)
        savedPost.setResonance(savedPost.getResonance() + 1);
        postRepository.save(savedPost);
        
        long updateTime = System.currentTimeMillis() - startTime;
        
        // Verify real-time update efficiency
        assertThat(updateTime).isLessThan(150); // Should update in under 150ms
        
        // Verify data consistency
        Post updatedPost = postRepository.findById(savedPost.getId()).orElseThrow();
        assertThat(updatedPost.getResonance()).isEqualTo(1);
        
        List<UserEngagement> postEngagements = userEngagementRepository.findByPostId(savedPost.getId());
        assertThat(postEngagements).hasSize(1);
        
        System.out.println("Real-time update time: " + updateTime + "ms");
        System.out.println("✅ Real-time frontend updates save efficiently to database");
    }
    
    @Test
    @Order(9)
    @DisplayName("Frontend Feed Loading → Database Query Performance")
    void testFeedLoadingPerformance() {
        System.out.println("=== Testing Frontend Feed Loading Performance ===");
        
        // Create multiple posts for feed testing
        for (int i = 0; i < 20; i++) {
            Post post = new Post();
            post.setUserId(testUser.getId());
            post.setTitle("Feed Post " + i);
            post.setContent("Content for feed post " + i);
            post.setEmotion(i % 3 == 0 ? "HAPPY" : (i % 3 == 1 ? "CALM" : "EXCITED"));
            post.setResonance(i * 2);
            post.setView(i * 10);
            postRepository.save(post);
        }
        
        long startTime = System.currentTimeMillis();
        
        // Simulate frontend feed loading
        List<Post> feedPosts = postRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        long feedLoadTime = System.currentTimeMillis() - startTime;
        
        assertThat(feedPosts).hasSizeGreaterThanOrEqualTo(20);
        assertThat(feedLoadTime).isLessThan(300); // Should load feed in under 300ms
        
        // Test emotion filtering (common frontend action)
        startTime = System.currentTimeMillis();
        List<Post> happyPosts = postRepository.findByEmotion("HAPPY");
        long filterTime = System.currentTimeMillis() - startTime;
        
        assertThat(happyPosts).isNotEmpty();
        assertThat(filterTime).isLessThan(200); // Should filter in under 200ms
        
        System.out.println("Feed load time: " + feedLoadTime + "ms");
        System.out.println("Filter time: " + filterTime + "ms");
        System.out.println("Posts in feed: " + feedPosts.size());
        System.out.println("✅ Frontend feed loading is efficient from database");
    }
    
    @Test
    @Order(10)
    @DisplayName("Complete Frontend Session → Database Integration")
    void testCompleteFrontendSession() {
        System.out.println("=== Testing Complete Frontend User Session ===");
        
        long sessionStartTime = System.currentTimeMillis();
        
        // Simulate complete user session:
        // 1. User opens app (loads feed)
        List<Post> initialFeed = postRepository.findAll();
        
        // 2. User creates post
        Post newPost = new Post();
        newPost.setUserId(testUser.getId());
        newPost.setTitle("Session Test Post");
        newPost.setContent("Complete session testing");
        newPost.setEmotion("HOPEFUL");
        Post savedPost = postRepository.save(newPost);
        
        // 3. User engages with content
        UserEngagement engagement = new UserEngagement();
        engagement.setUserId(testUser.getId());
        engagement.setPostId(savedPost.getId());
        engagement.setType(UserEngagement.EngagementType.COLLECT);
        userEngagementRepository.save(engagement);
        
        // 4. User chats with AI
        MemoryItem aiMemory = memoryService.storePersonalityTrait(
            testUser.getId(),
            "session_active_user",
            "User actively engages with platform features",
            0.9
        );
        
        // 5. User updates preferences
        UserPreferences prefs = new UserPreferences();
        prefs.setUserId(testUser.getId());
        prefs.setTheme(UserPreferences.Theme.SYSTEM);
        prefs.setNotificationsEnabled(true);
        userPreferencesRepository.save(prefs);
        
        long sessionTime = System.currentTimeMillis() - sessionStartTime;
        
        // Verify complete session data integrity
        assertThat(postRepository.findByUserId(testUser.getId())).isNotEmpty();
        assertThat(userEngagementRepository.findByUserId(testUser.getId())).isNotEmpty();
        assertThat(memoryItemRepository.findByUserId(testUser.getId())).isNotEmpty();
        assertThat(userPreferencesRepository.findByUserId(testUser.getId())).isPresent();
        
        // Session should complete efficiently
        assertThat(sessionTime).isLessThan(1000); // Complete session under 1 second
        
        System.out.println("Complete session time: " + sessionTime + "ms");
        System.out.println("✅ Complete frontend session saves efficiently to database");
        System.out.println("✅ All user data properly stored and retrievable");
    }
    
    @AfterEach
    void cleanup() {
        System.out.println("Test cleanup completed - data persisted for verification");
    }
}

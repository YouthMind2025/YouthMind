package ai.youthmind.config;

import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * DataInitializer - Loads sample data for development and testing
 * 
 * This component runs after application startup and creates sample users and posts
 * for development and testing purposes. Only runs when no existing data is found.
 * 
 * @author YouthMind Development Team
 */
@Component
@Profile("!test") // Skip in test environment
public class DataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            // Debug PasswordEncoder injection
            logger.debug("PasswordEncoder instance: {}", passwordEncoder != null ? passwordEncoder.getClass().getSimpleName() : "NULL");
            if (passwordEncoder != null) {
                logger.debug("PasswordEncoder test encode: {}", passwordEncoder.encode("test"));
            }
            
            // Ensure admin user exists
            if (!userRepository.existsByEmail("admin@youthmind.ai")) {
                logger.info("Creating admin user...");
                User adminUser = createAdminUser();
                userRepository.save(adminUser);
                logger.info("Admin user created: {}", adminUser.getEmail());
            } else {
                logger.info("Admin user already exists");
            }
            
            // Only initialize sample data if no other data exists
            if (userRepository.count() <= 1) { // Only admin user exists
                logger.info("Initializing sample data...");
                initializeSampleData();
                logger.info("Sample data initialization completed");
            } else {
                logger.info("Sample data already exists, skipping initialization");
            }
        } catch (Exception e) {
            logger.error("Error during data initialization: {}", e.getMessage(), e);
            throw e; // Re-throw to fail fast
        }
    }
    
    private void initializeSampleData() {
        // Create sample users
        List<User> sampleUsers = createSampleUsers();
        userRepository.saveAll(sampleUsers);
        
        // Create sample posts
        List<Post> samplePosts = createSamplePosts(sampleUsers);
        postRepository.saveAll(samplePosts);
        
        logger.info("Created {} sample users and {} sample posts", 
                   sampleUsers.size(), samplePosts.size());
    }
    
    private User createAdminUser() {
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setEmail("admin@youthmind.ai");
        
        // Encode password and verify it's not null
        String encodedPassword = passwordEncoder.encode("admin123");
        logger.debug("Encoded password length: {}", encodedPassword != null ? encodedPassword.length() : "NULL");
        admin.setPassword(encodedPassword);
        
        // Verify password was set correctly
        logger.debug("Admin password after setting: {}", admin.getPassword() != null ? "SET" : "NULL");
        
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setUsername("admin");
        admin.setRole(User.Role.ADMIN); // Set admin role
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        
        logger.debug("Admin user created: email={}, password={}", 
                    admin.getEmail(), admin.getPassword() != null ? "SET" : "NULL");
        
        return admin;
    }

    private List<User> createSampleUsers() {
        return Arrays.asList(
            createUser("alice@youthmind.ai", "Alice Johnson"),
            createUser("bob@youthmind.ai", "Bob Smith"),
            createUser("charlie@youthmind.ai", "Charlie Brown"),
            createUser("diana@youthmind.ai", "Diana Prince"),
            createUser("eve@youthmind.ai", "Eve Thompson")
        );
    }
    
    private User createUser(String email, String fullName) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123")); // Default password for testing
        
        // Split full name into first and last name
        String[] nameParts = fullName.split(" ", 2);
        user.setFirstName(nameParts[0]);
        if (nameParts.length > 1) {
            user.setLastName(nameParts[1]);
        }
        
        user.setUsername(email.split("@")[0]); // Use email prefix as username
        user.setCreatedAt(LocalDateTime.now().minusDays((int)(Math.random() * 30)));
        user.setUpdatedAt(user.getCreatedAt());
        return user;
    }
    
    private List<Post> createSamplePosts(List<User> users) {
        return Arrays.asList(
            // Alice's posts
            createPost(users.get(0), "Feeling Anxious About Tomorrow", 
                      "I have a big presentation tomorrow and I can't stop thinking about all the things that could go wrong. " +
                      "Any tips for managing presentation anxiety?", "anxious"),
            
            createPost(users.get(0), "Small Wins Matter", 
                      "Today I managed to complete my morning routine without rushing. Sometimes it's the small things that count!", "happy"),
            
            // Bob's posts
            createPost(users.get(1), "Struggling with Sleep", 
                      "Been having trouble falling asleep lately. My mind just won't quiet down when I hit the pillow. " +
                      "What works for you?", "tired"),
            
            createPost(users.get(1), "Found a Great Mindfulness App", 
                      "Just discovered this amazing meditation app that's helping me stay centered throughout the day. " +
                      "Five minutes of breathing exercises can make such a difference!", "calm"),
            
            // Charlie's posts
            createPost(users.get(2), "Family Stress is Real", 
                      "Dealing with family expectations and pressure is exhausting. Sometimes I feel like I'm living someone else's life.", "stressed"),
            
            createPost(users.get(2), "Grateful for Friends", 
                      "Had an amazing hangout with my closest friends today. We just talked and laughed for hours. " +
                      "Reminded me how important genuine connections are.", "grateful"),
            
            // Diana's posts
            createPost(users.get(3), "Rainy Day Vibes", 
                      "There's something peaceful about rainy days. Perfect time for reflection and cozy indoor activities.", "peaceful"),
            
            createPost(users.get(3), "Job Interview Nerves", 
                      "Big job interview next week. Excited but terrified. Any advice for managing interview anxiety?", "nervous"),
            
            createPost(users.get(3), "Nature Walk Healing", 
                      "Spent the afternoon walking in the forest. Nature has this incredible way of putting things in perspective. " +
                      "Feeling so much more grounded now.", "peaceful"),
            
            createPost(users.get(4), "Learning to Say No", 
                      "Finally learned to set boundaries and say no to things that drain my energy. " +
                      "It's hard but necessary for mental health.", "empowered")
        );
    }
    
    private Post createPost(User user, String title, String content, String emotion) {
        Post post = new Post();
        post.setId(UUID.randomUUID());
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setEmotion(emotion);
        
        // Random engagement metrics for realistic data
        post.setResonance((int)(Math.random() * 50));
        post.setView((int)(Math.random() * 200) + 10);
        post.setCollect((int)(Math.random() * 20));
        
        // Random creation time within last 30 days
        post.setCreatedAt(LocalDateTime.now().minusDays((int)(Math.random() * 30)));
        post.setUpdatedAt(post.getCreatedAt());
        
        // Sample topics (comma-separated)
        post.setTopics(generateTopicsForEmotion(emotion));
        
        // Set JSONB fields to null to avoid type mismatch
        post.setModerationDetails(null);
        post.setAiMetadata(null);
        
        return post;
    }
    
    private String generateTopicsForEmotion(String emotion) {
        switch (emotion.toLowerCase()) {
            case "anxious":
                return "anxiety,stress,coping,mental health";
            case "happy":
                return "happiness,positive,joy,celebration";
            case "tired":
                return "sleep,rest,fatigue,wellness";
            case "calm":
                return "peace,mindfulness,meditation,relaxation";
            case "stressed":
                return "stress,pressure,overwhelm,support";
            case "grateful":
                return "gratitude,appreciation,thankfulness,positive";
            case "relieved":
                return "relief,peace,stress relief,recovery";
            case "hopeful":
                return "hope,optimism,therapy,growth";
            case "frustrated":
                return "frustration,creativity,blocks,challenge";
            case "energetic":
                return "energy,exercise,fitness,motivation";
            case "content":
                return "contentment,happiness,cooking,therapy";
            case "nervous":
                return "nervousness,anxiety,job,interview";
            case "peaceful":
                return "peace,nature,healing,mindfulness";
            case "empowered":
                return "empowerment,boundaries,self-care,growth";
            default:
                return "general,mood,feelings,support";
        }
    }
}

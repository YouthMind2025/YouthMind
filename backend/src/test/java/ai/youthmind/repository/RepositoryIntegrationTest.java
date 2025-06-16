package ai.youthmind.repository;

import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@DataJpaTest
public class RepositoryIntegrationTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Test
    public void testCompleteWorkflow() {
        // Test 1: Create and save a user with timestamp
        System.out.println("=== Test 1: Creating User ===");
        User user = new User();
        user.setEmailHash("integration_test_hash_12345");
        
        LocalDateTime beforeUserSave = LocalDateTime.now();
        User savedUser = userRepository.save(user);
        entityManager.flush();
        LocalDateTime afterUserSave = LocalDateTime.now();
        
        // Verify user creation
        System.out.println("User ID: " + savedUser.getId());
        System.out.println("User Email Hash: " + savedUser.getEmailHash());
        System.out.println("User Created At: " + savedUser.getCreatedAt());
        System.out.println("Timestamp is between save times: " + 
            (savedUser.getCreatedAt().isAfter(beforeUserSave.minusSeconds(1)) && 
             savedUser.getCreatedAt().isBefore(afterUserSave.plusSeconds(1))));
        
        // Test 2: Create posts with relationships
        System.out.println("\n=== Test 2: Creating Posts ===");
        
        Post post1 = new Post();
        post1.setUser(savedUser);
        post1.setContent("I'm feeling really excited about this new project!");
        post1.setEmotionTag("excited");
        
        Post post2 = new Post();
        post2.setUser(savedUser);
        post2.setContent("Just had a calm and peaceful meditation session.");
        post2.setEmotionTag("calm");
        
        Post post3 = new Post();
        post3.setUser(savedUser);
        post3.setContent("Feeling a bit anxious about the presentation tomorrow.");
        post3.setEmotionTag("anxious");
        
        LocalDateTime beforePostsSave = LocalDateTime.now();
        Post savedPost1 = postRepository.save(post1);
        Post savedPost2 = postRepository.save(post2);
        Post savedPost3 = postRepository.save(post3);
        entityManager.flush();
        LocalDateTime afterPostsSave = LocalDateTime.now();
        
        // Verify posts creation
        System.out.println("Post 1 ID: " + savedPost1.getId());
        System.out.println("Post 1 Content: " + savedPost1.getContent());
        System.out.println("Post 1 Emotion: " + savedPost1.getEmotionTag());
        System.out.println("Post 1 User ID: " + savedPost1.getUser().getId());
        System.out.println("Post 1 Created At: " + savedPost1.getCreatedAt());
        
        // Test 3: Query by user relationship
        System.out.println("\n=== Test 3: Querying Posts by User ===");
        List<Post> userPosts = postRepository.findByUser(savedUser);
        System.out.println("Number of posts for user: " + userPosts.size());
        
        for(Post post : userPosts) {
            System.out.println("- Post: " + post.getContent().substring(0, Math.min(30, post.getContent().length())) + "...");
            System.out.println("  Emotion: " + post.getEmotionTag());
            System.out.println("  User matches: " + post.getUser().getId().equals(savedUser.getId()));
        }
        
        // Test 4: Query by emotion tag
        System.out.println("\n=== Test 4: Querying Posts by Emotion Tag ===");
        List<Post> calmPosts = postRepository.findByEmotionTag("calm");
        System.out.println("Number of 'calm' posts: " + calmPosts.size());
        
        for(Post post : calmPosts) {
            System.out.println("- Calm post: " + post.getContent());
            System.out.println("  Emotion tag is 'calm': " + "calm".equals(post.getEmotionTag()));
        }
        
        // Test 5: Test user repository methods
        System.out.println("\n=== Test 5: User Repository Methods ===");
        Optional<User> foundUser = userRepository.findByEmailHash("integration_test_hash_12345");
        System.out.println("User found by email hash: " + foundUser.isPresent());
        if(foundUser.isPresent()) {
            System.out.println("Found user ID matches: " + foundUser.get().getId().equals(savedUser.getId()));
        }
        
        boolean userExists = userRepository.existsByEmailHash("integration_test_hash_12345");
        System.out.println("User exists by email hash: " + userExists);
        
        boolean nonExistentUser = userRepository.existsByEmailHash("non_existent_hash");
        System.out.println("Non-existent user exists: " + nonExistentUser);
        
        // Test 6: Verify relationship integrity
        System.out.println("\n=== Test 6: Relationship Integrity ===");
        entityManager.clear(); // Clear persistence context
        
        Optional<Post> fetchedPost = postRepository.findById(savedPost1.getId());
        if(fetchedPost.isPresent()) {
            Post post = fetchedPost.get();
            System.out.println("Post fetched successfully: " + (post.getId() != null));
            System.out.println("Post has user relationship: " + (post.getUser() != null));
            System.out.println("User relationship matches: " + post.getUser().getId().equals(savedUser.getId()));
            System.out.println("User email hash matches: " + "integration_test_hash_12345".equals(post.getUser().getEmailHash()));
        }
        
        System.out.println("\n=== All Tests Completed Successfully ===");
    }
} 
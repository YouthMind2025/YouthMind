package ai.youthmind.repository;

import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class PostRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    public void testSaveAndFindPost() {
        // Given
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password123");
        user.setFirstName("Test");
        user.setLastName("User");
        User savedUser = userRepository.save(user);
        
        Post post = new Post();
        post.setUserId(savedUser.getId());
        post.setContent("This is a test post content");
        post.setEmotion("happy");
        
        // When
        Post savedPost = postRepository.save(post);
        entityManager.flush();
        
        // Then
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getContent()).isEqualTo("This is a test post content");
        assertThat(savedPost.getEmotion()).isEqualTo("happy");
        assertThat(savedPost.getUserId()).isNotNull();
        assertThat(savedPost.getUserId()).isEqualTo(savedUser.getId());
        assertThat(savedPost.getCreatedAt()).isNotNull();
        assertThat(savedPost.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
        assertThat(savedPost.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
    }
    
    @Test
    public void testFindByUserId() {
        // Given
        User user = new User();
        user.setEmail("finduser@example.com");
        user.setPassword("password456");
        user.setFirstName("Find");
        user.setLastName("User");
        User savedUser = userRepository.save(user);
        
        Post post1 = new Post();
        post1.setUserId(savedUser.getId());
        post1.setContent("First post");
        post1.setEmotion("excited");
        postRepository.save(post1);
        
        Post post2 = new Post();
        post2.setUserId(savedUser.getId());
        post2.setContent("Second post");
        post2.setEmotion("calm");
        postRepository.save(post2);
        
        entityManager.flush();
        
        // When
        List<Post> posts = postRepository.findByUserId(savedUser.getId());
        
        // Then
        assertThat(posts).hasSize(2);
        assertThat(posts).extracting(Post::getContent).containsExactlyInAnyOrder("First post", "Second post");
    }
    
    @Test
    public void testFindByEmotion() {
        // Given
        User user = new User();
        user.setEmail("emotion@example.com");
        user.setPassword("password789");
        user.setFirstName("Emotion");
        user.setLastName("User");
        User savedUser = userRepository.save(user);
        
        Post post1 = new Post();
        post1.setUserId(savedUser.getId());
        post1.setContent("Happy post");
        post1.setEmotion("happy");
        postRepository.save(post1);
        
        Post post2 = new Post();
        post2.setUserId(savedUser.getId());
        post2.setContent("Another happy post");
        post2.setEmotion("happy");
        postRepository.save(post2);
        
        Post post3 = new Post();
        post3.setUserId(savedUser.getId());
        post3.setContent("Sad post");
        post3.setEmotion("sad");
        postRepository.save(post3);
        
        entityManager.flush();
        
        // When
        List<Post> happyPosts = postRepository.findByEmotion("happy");
        
        // Then
        assertThat(happyPosts).hasSize(2);
        assertThat(happyPosts).extracting(Post::getEmotion).containsOnly("happy");
    }
    
    @Test
    public void testUserIdIsSavedCorrectly() {
        // Given
        User user = new User();
        user.setEmail("relationship@example.com");
        user.setPassword("passwordrel");
        user.setFirstName("Relationship");
        user.setLastName("Test");
        User savedUser = userRepository.save(user);
        
        Post post = new Post();
        post.setUserId(savedUser.getId());
        post.setContent("Testing user ID relationship");
        post.setEmotion("neutral");
        
        // When
        Post savedPost = postRepository.save(post);
        entityManager.flush();
        entityManager.clear();  // Clear the persistence context
        
        // Then - fetch from database to verify userId is saved correctly
        Post fetchedPost = postRepository.findById(savedPost.getId()).orElse(null);
        assertThat(fetchedPost).isNotNull();
        assertThat(fetchedPost.getUserId()).isNotNull();
        assertThat(fetchedPost.getUserId()).isEqualTo(savedUser.getId());
    }
    
    @Test
    public void testTimestampIsAutomaticallySet() {
        // Given
        User user = new User();
        user.setEmail("timestamp@example.com");
        user.setPassword("passwordtime");
        user.setFirstName("Timestamp");
        user.setLastName("Post");
        User savedUser = userRepository.save(user);
        
        LocalDateTime beforeSave = LocalDateTime.now();
        Post post = new Post();
        post.setUserId(savedUser.getId());
        post.setContent("Timestamp test post");
        
        // When
        Post savedPost = postRepository.save(post);
        entityManager.flush();
        LocalDateTime afterSave = LocalDateTime.now();
        
        // Then
        assertThat(savedPost.getCreatedAt()).isNotNull();
        assertThat(savedPost.getCreatedAt()).isAfterOrEqualTo(beforeSave);
        assertThat(savedPost.getCreatedAt()).isBeforeOrEqualTo(afterSave);
    }
} 
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
        user.setEmailHash("test_user_hash");
        User savedUser = userRepository.save(user);
        
        Post post = new Post();
        post.setUser(savedUser);
        post.setContent("This is a test post content");
        post.setEmotionTag("happy");
        
        // When
        Post savedPost = postRepository.save(post);
        entityManager.flush();
        
        // Then
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getContent()).isEqualTo("This is a test post content");
        assertThat(savedPost.getEmotionTag()).isEqualTo("happy");
        assertThat(savedPost.getUser()).isNotNull();
        assertThat(savedPost.getUser().getEmailHash()).isEqualTo("test_user_hash");
        assertThat(savedPost.getCreatedAt()).isNotNull();
        assertThat(savedPost.getCreatedAt()).isBefore(LocalDateTime.now().plusSeconds(1));
        assertThat(savedPost.getCreatedAt()).isAfter(LocalDateTime.now().minusSeconds(10));
    }
    
    @Test
    public void testFindByUser() {
        // Given
        User user = new User();
        user.setEmailHash("find_user_hash");
        User savedUser = userRepository.save(user);
        
        Post post1 = new Post();
        post1.setUser(savedUser);
        post1.setContent("First post");
        post1.setEmotionTag("excited");
        postRepository.save(post1);
        
        Post post2 = new Post();
        post2.setUser(savedUser);
        post2.setContent("Second post");
        post2.setEmotionTag("calm");
        postRepository.save(post2);
        
        entityManager.flush();
        
        // When
        List<Post> posts = postRepository.findByUser(savedUser);
        
        // Then
        assertThat(posts).hasSize(2);
        assertThat(posts).extracting(Post::getContent).containsExactlyInAnyOrder("First post", "Second post");
    }
    
    @Test
    public void testFindByEmotionTag() {
        // Given
        User user = new User();
        user.setEmailHash("emotion_user_hash");
        User savedUser = userRepository.save(user);
        
        Post post1 = new Post();
        post1.setUser(savedUser);
        post1.setContent("Happy post");
        post1.setEmotionTag("happy");
        postRepository.save(post1);
        
        Post post2 = new Post();
        post2.setUser(savedUser);
        post2.setContent("Another happy post");
        post2.setEmotionTag("happy");
        postRepository.save(post2);
        
        Post post3 = new Post();
        post3.setUser(savedUser);
        post3.setContent("Sad post");
        post3.setEmotionTag("sad");
        postRepository.save(post3);
        
        entityManager.flush();
        
        // When
        List<Post> happyPosts = postRepository.findByEmotionTag("happy");
        
        // Then
        assertThat(happyPosts).hasSize(2);
        assertThat(happyPosts).extracting(Post::getEmotionTag).containsOnly("happy");
    }
    
    @Test
    public void testRelationshipIsSavedCorrectly() {
        // Given
        User user = new User();
        user.setEmailHash("relationship_test_hash");
        User savedUser = userRepository.save(user);
        
        Post post = new Post();
        post.setUser(savedUser);
        post.setContent("Testing relationship");
        post.setEmotionTag("neutral");
        
        // When
        Post savedPost = postRepository.save(post);
        entityManager.flush();
        entityManager.clear();  // Clear the persistence context
        
        // Then - fetch from database to verify relationship
        Post fetchedPost = postRepository.findById(savedPost.getId()).orElse(null);
        assertThat(fetchedPost).isNotNull();
        assertThat(fetchedPost.getUser()).isNotNull();
        assertThat(fetchedPost.getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(fetchedPost.getUser().getEmailHash()).isEqualTo("relationship_test_hash");
    }
    
    @Test
    public void testTimestampIsAutomaticallySet() {
        // Given
        User user = new User();
        user.setEmailHash("timestamp_post_hash");
        User savedUser = userRepository.save(user);
        
        LocalDateTime beforeSave = LocalDateTime.now();
        Post post = new Post();
        post.setUser(savedUser);
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
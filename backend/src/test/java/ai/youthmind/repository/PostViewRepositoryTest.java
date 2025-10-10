package ai.youthmind.repository;

import ai.youthmind.entity.Post;
import ai.youthmind.entity.PostView;
import ai.youthmind.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
public class PostViewRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private PostViewRepository postViewRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    private Post testPost;
    private UUID userId;
    private UUID postId;
    
    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser = userRepository.save(testUser);
        userId = testUser.getId();
        
        // Create test post
        testPost = new Post();
        testPost.setUserId(userId);
        testPost.setTitle("Test Post");
        testPost.setContent("Test content");
        testPost.setEmotion("happy");
        testPost.setView(0);
        testPost.setResonance(0);
        testPost.setCollect(0);
        testPost = postRepository.save(testPost);
        postId = testPost.getId();
    }
    
    @Test
    void testSaveAndFindPostView() {
        // Given
        PostView postView = new PostView(userId, postId);
        
        // When
        PostView savedPostView = postViewRepository.save(postView);
        entityManager.flush();
        
        // Then
        assertThat(savedPostView.getId()).isNotNull();
        assertThat(savedPostView.getUserId()).isEqualTo(userId);
        assertThat(savedPostView.getPostId()).isEqualTo(postId);
        assertThat(savedPostView.getViewedAt()).isNotNull();
    }
    
    @Test
    void testExistsByUserIdAndPostId_WhenViewExists_ShouldReturnTrue() {
        // Given
        PostView postView = new PostView(userId, postId);
        postViewRepository.save(postView);
        entityManager.flush();
        
        // When
        boolean exists = postViewRepository.existsByUserIdAndPostId(userId, postId);
        
        // Then
        assertThat(exists).isTrue();
    }
    
    @Test
    void testExistsByUserIdAndPostId_WhenViewDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = postViewRepository.existsByUserIdAndPostId(userId, postId);
        
        // Then
        assertThat(exists).isFalse();
    }
    
    @Test
    void testCountByPostId_WhenMultipleViews_ShouldReturnCorrectCount() {
        // Given
        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPassword("password456");
        user2.setFirstName("User");
        user2.setLastName("Two");
        user2 = userRepository.save(user2);
        
        PostView view1 = new PostView(userId, postId);
        PostView view2 = new PostView(user2.getId(), postId);
        postViewRepository.save(view1);
        postViewRepository.save(view2);
        entityManager.flush();
        
        // When
        long count = postViewRepository.countByPostId(postId);
        
        // Then
        assertThat(count).isEqualTo(2);
    }
    
    @Test
    void testFindByPostId_WhenMultipleViews_ShouldReturnAllViews() {
        // Given
        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPassword("password456");
        user2.setFirstName("User");
        user2.setLastName("Two");
        user2 = userRepository.save(user2);
        
        PostView view1 = new PostView(userId, postId);
        PostView view2 = new PostView(user2.getId(), postId);
        postViewRepository.save(view1);
        postViewRepository.save(view2);
        entityManager.flush();
        
        // When
        List<PostView> views = postViewRepository.findByPostId(postId);
        
        // Then
        assertThat(views).hasSize(2);
        assertThat(views).extracting(PostView::getUserId).containsExactlyInAnyOrder(userId, user2.getId());
    }
    
    @Test
    void testFindByUserId_WhenUserViewedMultiplePosts_ShouldReturnAllViews() {
        // Given
        Post post2 = new Post();
        post2.setUserId(userId);
        post2.setTitle("Test Post 2");
        post2.setContent("Test content 2");
        post2.setEmotion("sad");
        post2.setView(0);
        post2.setResonance(0);
        post2.setCollect(0);
        post2 = postRepository.save(post2);
        
        PostView view1 = new PostView(userId, postId);
        PostView view2 = new PostView(userId, post2.getId());
        postViewRepository.save(view1);
        postViewRepository.save(view2);
        entityManager.flush();
        
        // When
        List<PostView> views = postViewRepository.findByUserId(userId);
        
        // Then
        assertThat(views).hasSize(2);
        assertThat(views).extracting(PostView::getPostId).containsExactlyInAnyOrder(postId, post2.getId());
    }
    
    @Test
    void testUniqueConstraint_WhenDuplicateViewAttempted_ShouldFail() {
        // Given
        PostView postView = new PostView(userId, postId);
        postViewRepository.save(postView);
        entityManager.flush();
        
        // When & Then - Attempting to save the same user-post combination should fail
        PostView duplicateView = new PostView(userId, postId);
        
        // This should throw an exception due to unique constraint violation
        assertThatThrownBy(() -> {
            postViewRepository.save(duplicateView);
            entityManager.flush();
        }).isInstanceOf(Exception.class);
    }
} 
package ai.youthmind.repository;

import ai.youthmind.entity.Post;
import ai.youthmind.entity.PostCollection;
import ai.youthmind.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class PostCollectionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PostCollectionRepository postCollectionRepository;

    @Test
    void existsByUserIdAndPostId_ShouldReturnTrue_WhenCollectionExists() {
        // Given
        User user = createAndPersistUser();
        Post post = createAndPersistPost(user.getId());
        PostCollection collection = createAndPersistCollection(user.getId(), post.getId());

        // When
        boolean exists = postCollectionRepository.existsByUserIdAndPostId(user.getId(), post.getId());

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByUserIdAndPostId_ShouldReturnFalse_WhenCollectionDoesNotExist() {
        // Given
        User user = createAndPersistUser();
        Post post = createAndPersistPost(user.getId());
        UUID randomUserId = UUID.randomUUID();

        // When
        boolean exists = postCollectionRepository.existsByUserIdAndPostId(randomUserId, post.getId());

        // Then
        assertFalse(exists);
    }

    @Test
    void findByUserIdAndPostId_ShouldReturnCollection_WhenExists() {
        // Given
        User user = createAndPersistUser();
        Post post = createAndPersistPost(user.getId());
        PostCollection collection = createAndPersistCollection(user.getId(), post.getId());

        // When
        Optional<PostCollection> found = postCollectionRepository.findByUserIdAndPostId(user.getId(), post.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getUserId());
        assertEquals(post.getId(), found.get().getPostId());
    }

    @Test
    void countByPostId_ShouldReturnCorrectCount() {
        // Given
        User user1 = createAndPersistUser();
        User user2 = createAndPersistUser();
        User user3 = createAndPersistUser();
        Post post = createAndPersistPost(user1.getId());

        createAndPersistCollection(user1.getId(), post.getId());
        createAndPersistCollection(user2.getId(), post.getId());
        createAndPersistCollection(user3.getId(), post.getId());

        // When
        long count = postCollectionRepository.countByPostId(post.getId());

        // Then
        assertEquals(3, count);
    }

    @Test
    void findByPostId_ShouldReturnAllCollections() {
        // Given
        User user1 = createAndPersistUser();
        User user2 = createAndPersistUser();
        Post post = createAndPersistPost(user1.getId());

        createAndPersistCollection(user1.getId(), post.getId());
        createAndPersistCollection(user2.getId(), post.getId());

        // When
        List<PostCollection> collections = postCollectionRepository.findByPostId(post.getId());

        // Then
        assertEquals(2, collections.size());
        assertTrue(collections.stream().anyMatch(c -> c.getUserId().equals(user1.getId())));
        assertTrue(collections.stream().anyMatch(c -> c.getUserId().equals(user2.getId())));
    }

    @Test
    void findByPostId_WithPagination_ShouldReturnPagedCollections() {
        // Given
        User user1 = createAndPersistUser();
        User user2 = createAndPersistUser();
        User user3 = createAndPersistUser();
        Post post = createAndPersistPost(user1.getId());

        createAndPersistCollection(user1.getId(), post.getId());
        createAndPersistCollection(user2.getId(), post.getId());
        createAndPersistCollection(user3.getId(), post.getId());

        // When
        Page<PostCollection> page = postCollectionRepository.findByPostId(post.getId(), PageRequest.of(0, 2));

        // Then
        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void findByUserId_ShouldReturnAllUserCollections() {
        // Given
        User user = createAndPersistUser();
        Post post1 = createAndPersistPost(user.getId());
        Post post2 = createAndPersistPost(user.getId());
        Post post3 = createAndPersistPost(user.getId());

        createAndPersistCollection(user.getId(), post1.getId());
        createAndPersistCollection(user.getId(), post2.getId());
        createAndPersistCollection(user.getId(), post3.getId());

        // When
        List<PostCollection> collections = postCollectionRepository.findByUserId(user.getId());

        // Then
        assertEquals(3, collections.size());
        assertTrue(collections.stream().anyMatch(c -> c.getPostId().equals(post1.getId())));
        assertTrue(collections.stream().anyMatch(c -> c.getPostId().equals(post2.getId())));
        assertTrue(collections.stream().anyMatch(c -> c.getPostId().equals(post3.getId())));
    }

    @Test
    void findByUserIdOrderByCollectedAtDesc_ShouldReturnOrderedCollections() {
        // Given
        User user = createAndPersistUser();
        Post post1 = createAndPersistPost(user.getId());
        Post post2 = createAndPersistPost(user.getId());
        Post post3 = createAndPersistPost(user.getId());

        // Create collections with explicit timestamps to ensure order
        PostCollection collection1 = new PostCollection(user.getId(), post1.getId());
        collection1.setCollectedAt(LocalDateTime.now().minusDays(2));
        entityManager.persist(collection1);

        PostCollection collection2 = new PostCollection(user.getId(), post2.getId());
        collection2.setCollectedAt(LocalDateTime.now().minusDays(1));
        entityManager.persist(collection2);

        PostCollection collection3 = new PostCollection(user.getId(), post3.getId());
        collection3.setCollectedAt(LocalDateTime.now());
        entityManager.persist(collection3);

        entityManager.flush();

        // When
        List<PostCollection> collections = postCollectionRepository.findByUserIdOrderByCollectedAtDesc(user.getId());

        // Then
        assertEquals(3, collections.size());
        assertEquals(post3.getId(), collections.get(0).getPostId());
        assertEquals(post2.getId(), collections.get(1).getPostId());
        assertEquals(post1.getId(), collections.get(2).getPostId());
    }

    @Test
    void deleteByUserIdAndPostId_ShouldRemoveCollection() {
        // Given
        User user = createAndPersistUser();
        Post post = createAndPersistPost(user.getId());
        PostCollection collection = createAndPersistCollection(user.getId(), post.getId());

        assertTrue(postCollectionRepository.existsByUserIdAndPostId(user.getId(), post.getId()));

        // When
        long deleted = postCollectionRepository.deleteByUserIdAndPostId(user.getId(), post.getId());

        // Then
        assertEquals(1, deleted);
        assertFalse(postCollectionRepository.existsByUserIdAndPostId(user.getId(), post.getId()));
    }

    @Test
    void uniqueConstraint_ShouldPreventDuplicateCollections() {
        // Given
        User user = createAndPersistUser();
        Post post = createAndPersistPost(user.getId());
        PostCollection collection = createAndPersistCollection(user.getId(), post.getId());

        // When & Then
        PostCollection duplicateCollection = new PostCollection(user.getId(), post.getId());
        assertThrows(Exception.class, () -> {
            entityManager.persist(duplicateCollection);
            entityManager.flush();
        });
    }

    private User createAndPersistUser() {
        User user = new User();
        user.setEmail("user" + UUID.randomUUID() + "@example.com");
        user.setPassword("password");
        user.setUsername("user" + UUID.randomUUID());
        user.setCreatedAt(LocalDateTime.now());
        return entityManager.persist(user);
    }

    private Post createAndPersistPost(UUID userId) {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle("Test Post");
        post.setContent("Test content");
        post.setEmotion("happy");
        post.setCreatedAt(LocalDateTime.now());
        post.setResonance(0);
        post.setView(0);
        post.setCollect(0);
        return entityManager.persist(post);
    }

    private PostCollection createAndPersistCollection(UUID userId, UUID postId) {
        PostCollection collection = new PostCollection(userId, postId);
        return entityManager.persist(collection);
    }
} 
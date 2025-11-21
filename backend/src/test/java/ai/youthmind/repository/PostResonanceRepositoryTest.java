package ai.youthmind.repository;

import ai.youthmind.entity.Post;
import ai.youthmind.entity.PostResonance;
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
public class PostResonanceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PostResonanceRepository postResonanceRepository;

    @Test
    void existsByUserIdAndPostId_ShouldReturnTrue_WhenResonanceExists() {
        // Given
        User user = createAndPersistUser();
        Post post = createAndPersistPost(user.getId());
        PostResonance resonance = createAndPersistResonance(user.getId(), post.getId());

        // When
        boolean exists = postResonanceRepository.existsByUserIdAndPostId(user.getId(), post.getId());

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByUserIdAndPostId_ShouldReturnFalse_WhenResonanceDoesNotExist() {
        // Given
        User user = createAndPersistUser();
        Post post = createAndPersistPost(user.getId());
        UUID randomUserId = UUID.randomUUID();

        // When
        boolean exists = postResonanceRepository.existsByUserIdAndPostId(randomUserId, post.getId());

        // Then
        assertFalse(exists);
    }

    @Test
    void findByUserIdAndPostId_ShouldReturnResonance_WhenExists() {
        // Given
        User user = createAndPersistUser();
        Post post = createAndPersistPost(user.getId());
        PostResonance resonance = createAndPersistResonance(user.getId(), post.getId());

        // When
        Optional<PostResonance> found = postResonanceRepository.findByUserIdAndPostId(user.getId(), post.getId());

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

        createAndPersistResonance(user1.getId(), post.getId());
        createAndPersistResonance(user2.getId(), post.getId());
        createAndPersistResonance(user3.getId(), post.getId());

        // When
        long count = postResonanceRepository.countByPostId(post.getId());

        // Then
        assertEquals(3, count);
    }

    @Test
    void findByPostId_ShouldReturnAllResonances() {
        // Given
        User user1 = createAndPersistUser();
        User user2 = createAndPersistUser();
        Post post = createAndPersistPost(user1.getId());

        createAndPersistResonance(user1.getId(), post.getId());
        createAndPersistResonance(user2.getId(), post.getId());

        // When
        List<PostResonance> resonances = postResonanceRepository.findByPostId(post.getId());

        // Then
        assertEquals(2, resonances.size());
        assertTrue(resonances.stream().anyMatch(r -> r.getUserId().equals(user1.getId())));
        assertTrue(resonances.stream().anyMatch(r -> r.getUserId().equals(user2.getId())));
    }

    @Test
    void findByPostId_WithPagination_ShouldReturnPagedResonances() {
        // Given
        User user1 = createAndPersistUser();
        User user2 = createAndPersistUser();
        User user3 = createAndPersistUser();
        Post post = createAndPersistPost(user1.getId());

        createAndPersistResonance(user1.getId(), post.getId());
        createAndPersistResonance(user2.getId(), post.getId());
        createAndPersistResonance(user3.getId(), post.getId());

        // When
        Page<PostResonance> page = postResonanceRepository.findByPostId(post.getId(), PageRequest.of(0, 2));

        // Then
        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void findByUserId_ShouldReturnAllUserResonances() {
        // Given
        User user = createAndPersistUser();
        Post post1 = createAndPersistPost(user.getId());
        Post post2 = createAndPersistPost(user.getId());
        Post post3 = createAndPersistPost(user.getId());

        createAndPersistResonance(user.getId(), post1.getId());
        createAndPersistResonance(user.getId(), post2.getId());
        createAndPersistResonance(user.getId(), post3.getId());

        // When
        List<PostResonance> resonances = postResonanceRepository.findByUserId(user.getId());

        // Then
        assertEquals(3, resonances.size());
        assertTrue(resonances.stream().anyMatch(r -> r.getPostId().equals(post1.getId())));
        assertTrue(resonances.stream().anyMatch(r -> r.getPostId().equals(post2.getId())));
        assertTrue(resonances.stream().anyMatch(r -> r.getPostId().equals(post3.getId())));
    }

    @Test
    void deleteByUserIdAndPostId_ShouldRemoveResonance() {
        // Given
        User user = createAndPersistUser();
        Post post = createAndPersistPost(user.getId());
        PostResonance resonance = createAndPersistResonance(user.getId(), post.getId());

        assertTrue(postResonanceRepository.existsByUserIdAndPostId(user.getId(), post.getId()));

        // When
        long deleted = postResonanceRepository.deleteByUserIdAndPostId(user.getId(), post.getId());

        // Then
        assertEquals(1, deleted);
        assertFalse(postResonanceRepository.existsByUserIdAndPostId(user.getId(), post.getId()));
    }

    @Test
    void uniqueConstraint_ShouldPreventDuplicateResonances() {
        // Given
        User user = createAndPersistUser();
        Post post = createAndPersistPost(user.getId());
        PostResonance resonance = createAndPersistResonance(user.getId(), post.getId());

        // When & Then
        PostResonance duplicateResonance = new PostResonance(user.getId(), post.getId());
        assertThrows(Exception.class, () -> {
            entityManager.persist(duplicateResonance);
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

    private PostResonance createAndPersistResonance(UUID userId, UUID postId) {
        PostResonance resonance = new PostResonance(userId, postId);
        return entityManager.persist(resonance);
    }
} 
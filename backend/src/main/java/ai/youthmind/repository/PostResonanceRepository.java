package ai.youthmind.repository;

import ai.youthmind.entity.PostResonance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing post resonances (likes).
 */
@Repository
public interface PostResonanceRepository extends JpaRepository<PostResonance, UUID> {

    /**
     * Check if a user has already resonanced a specific post.
     *
     * @param userId The user ID
     * @param postId The post ID
     * @return true if the user has resonanced the post, false otherwise
     */
    boolean existsByUserIdAndPostId(UUID userId, UUID postId);

    /**
     * Find a specific resonance by user and post IDs.
     *
     * @param userId The user ID
     * @param postId The post ID
     * @return The resonance if found
     */
    Optional<PostResonance> findByUserIdAndPostId(UUID userId, UUID postId);

    /**
     * Count the total number of resonances for a post.
     *
     * @param postId The post ID
     * @return The count of resonances
     */
    long countByPostId(UUID postId);

    /**
     * Find all resonances for a specific post.
     *
     * @param postId The post ID
     * @return List of resonances
     */
    List<PostResonance> findByPostId(UUID postId);

    /**
     * Find all resonances for a specific post with pagination.
     *
     * @param postId The post ID
     * @param pageable Pagination information
     * @return Page of resonances
     */
    Page<PostResonance> findByPostId(UUID postId, Pageable pageable);

    /**
     * Find all resonances by a specific user.
     *
     * @param userId The user ID
     * @return List of resonances
     */
    List<PostResonance> findByUserId(UUID userId);

    /**
     * Find all resonances by a specific user with pagination.
     *
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Page of resonances
     */
    Page<PostResonance> findByUserId(UUID userId, Pageable pageable);

    /**
     * Delete a resonance by user and post IDs.
     *
     * @param userId The user ID
     * @param postId The post ID
     * @return The number of resonances deleted
     */
    long deleteByUserIdAndPostId(UUID userId, UUID postId);
} 
package ai.youthmind.repository;

import ai.youthmind.entity.PostCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing post collections (saves).
 */
@Repository
public interface PostCollectionRepository extends JpaRepository<PostCollection, UUID> {

    /**
     * Check if a user has already collected a specific post.
     *
     * @param userId The user ID
     * @param postId The post ID
     * @return true if the user has collected the post, false otherwise
     */
    boolean existsByUserIdAndPostId(UUID userId, UUID postId);

    /**
     * Find a specific collection by user and post IDs.
     *
     * @param userId The user ID
     * @param postId The post ID
     * @return The collection if found
     */
    Optional<PostCollection> findByUserIdAndPostId(UUID userId, UUID postId);

    /**
     * Count the total number of collections for a post.
     *
     * @param postId The post ID
     * @return The count of collections
     */
    long countByPostId(UUID postId);

    /**
     * Find all collections for a specific post.
     *
     * @param postId The post ID
     * @return List of collections
     */
    List<PostCollection> findByPostId(UUID postId);

    /**
     * Find all collections for a specific post with pagination.
     *
     * @param postId The post ID
     * @param pageable Pagination information
     * @return Page of collections
     */
    Page<PostCollection> findByPostId(UUID postId, Pageable pageable);

    /**
     * Find all collections by a specific user.
     *
     * @param userId The user ID
     * @return List of collections
     */
    List<PostCollection> findByUserId(UUID userId);

    /**
     * Find all collections by a specific user with pagination.
     *
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Page of collections
     */
    Page<PostCollection> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find all collections by a specific user ordered by collection date.
     *
     * @param userId The user ID
     * @return List of collections ordered by collection date (most recent first)
     */
    List<PostCollection> findByUserIdOrderByCollectedAtDesc(UUID userId);

    /**
     * Delete a collection by user and post IDs.
     *
     * @param userId The user ID
     * @param postId The post ID
     * @return The number of collections deleted
     */
    long deleteByUserIdAndPostId(UUID userId, UUID postId);
    
    /**
     * Find all collections by a user for multiple posts (batch operation).
     * This method is optimized for bulk operations to reduce database queries.
     *
     * @param userId The user ID
     * @param postIds List of post IDs to check
     * @return List of collections for the specified posts
     */
    List<PostCollection> findByUserIdAndPostIdIn(UUID userId, List<UUID> postIds);
    
    /**
     * Find all collections for multiple posts ordered by collection date.
     * This method is used for generating notifications about who saved user's posts.
     *
     * @param postIds List of post IDs
     * @return List of collections ordered by collection date (most recent first)
     */
    List<PostCollection> findByPostIdInOrderByCollectedAtDesc(List<UUID> postIds);
    
    /**
     * Delete all collections by a specific user.
     * This method is used when deleting a user account.
     *
     * @param userId The user ID
     */
    void deleteByUserId(UUID userId);
} 
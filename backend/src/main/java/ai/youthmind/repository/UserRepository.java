package ai.youthmind.repository;

import ai.youthmind.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
    
    @Query("SELECT u.postsCount FROM User u WHERE u.id = :userId")
    Integer getPostsCountByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT u.likesCount FROM User u WHERE u.id = :userId")
    Integer getLikesCountByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT u.bookmarksCount FROM User u WHERE u.id = :userId")
    Integer getBookmarksCountByUserId(@Param("userId") UUID userId);
    
    /**
     * Find users who don't have a recommendation vector yet
     */
    @Query("SELECT u FROM User u WHERE NOT EXISTS " +
           "(SELECT 1 FROM UserRecommendationVector rv WHERE rv.userId = u.id)")
    List<User> findUsersWithoutRecommendationVectors();

    /**
     * Find users who don't have a feed state yet
     */
    @Query("SELECT u FROM User u WHERE NOT EXISTS " +
           "(SELECT 1 FROM UserFeedState fs WHERE fs.userId = u.id)")
    List<User> findUsersWithoutFeedState();
} 

package ai.youthmind.interests.repository;

import ai.youthmind.interests.entity.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserInterestRepository extends JpaRepository<UserInterest, UUID> {
    Optional<UserInterest> findByUserIdAndTopic(UUID userId, String topic);
    List<UserInterest> findByUserIdOrderByScoreDesc(UUID userId);
}

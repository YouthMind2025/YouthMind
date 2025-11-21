
package ai.youthmind.memory.repository;

import ai.youthmind.memory.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {
    Optional<UserPreference> findByUserIdAndPrefKey(UUID userId, String prefKey);
    List<UserPreference> findByUserId(UUID userId);
}


package ai.youthmind.analytics.repository;

import ai.youthmind.analytics.entity.UserBehaviorAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserBehaviorAnalyticsRepository extends JpaRepository<UserBehaviorAnalytics, UUID> {
    List<UserBehaviorAnalytics> findByUserIdAndCreatedAtBetween(UUID userId, Instant from, Instant to);
    List<UserBehaviorAnalytics> findByUserId(UUID userId);
}

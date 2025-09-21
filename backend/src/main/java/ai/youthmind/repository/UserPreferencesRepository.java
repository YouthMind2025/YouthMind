package ai.youthmind.repository;

import ai.youthmind.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {
    
    Optional<UserPreferences> findByUserId(UUID userId);
    
    boolean existsByUserId(UUID userId);
    
    void deleteByUserId(UUID userId);
    
    @Query("SELECT up.theme FROM UserPreferences up WHERE up.userId = :userId")
    Optional<UserPreferences.Theme> getThemeByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT up.privacyLevel FROM UserPreferences up WHERE up.userId = :userId")
    Optional<UserPreferences.PrivacyLevel> getPrivacyLevelByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT up.notificationsEnabled FROM UserPreferences up WHERE up.userId = :userId")
    Optional<Boolean> getNotificationsEnabledByUserId(@Param("userId") UUID userId);
}
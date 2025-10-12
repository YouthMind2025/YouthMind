package ai.youthmind.service;

import ai.youthmind.entity.User;
import ai.youthmind.entity.UserFeedState;
import ai.youthmind.entity.UserRecommendationVector;
import ai.youthmind.repository.UserFeedStateRepository;
import ai.youthmind.repository.UserRecommendationVectorRepository;
import ai.youthmind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * Service for data migration tasks related to recommendation engine
 * Used for initializing default data for existing users
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataMigrationService {
    
    private final UserRepository userRepository;
    private final UserRecommendationVectorRepository vectorRepository;
    private final UserFeedStateRepository feedStateRepository;
    
    /**
     * Create default R vectors for all users who don't have one
     * Default vector: all emotions = 0.1, version = 1
     * 
     * @return MigrationResult with counts
     */
    @Transactional
    public MigrationResult createDefaultRVectorsForExistingUsers() {
        log.info("Starting R vector migration for existing users...");
        
        int batchSize = 1000;
        int created = 0;
        int skipped = 0;
        int errors = 0;
        
        try {
            // Find users without R vectors
            List<User> usersWithoutVectors = userRepository.findUsersWithoutRecommendationVectors();
            log.info("Found {} users without R vectors", usersWithoutVectors.size());
            
            // Process in batches
            for (int i = 0; i < usersWithoutVectors.size(); i += batchSize) {
                List<User> batch = usersWithoutVectors.subList(
                    i, Math.min(i + batchSize, usersWithoutVectors.size())
                );
                
                for (User user : batch) {
                    try {
                        // Check if vector was created by another process
                        if (vectorRepository.existsByUserId(user.getId())) {
                            skipped++;
                            continue;
                        }
                        
                        // Create default R vector (all emotions = 0.1)
                        UserRecommendationVector defaultVector = new UserRecommendationVector();
                        defaultVector.setUserId(user.getId());
                        defaultVector.setVersion(1);
                        defaultVector.setSadWeight(BigDecimal.valueOf(0.1));
                        defaultVector.setFearWeight(BigDecimal.valueOf(0.1));
                        defaultVector.setAngryWeight(BigDecimal.valueOf(0.1));
                        defaultVector.setHappyWeight(BigDecimal.valueOf(0.1));
                        defaultVector.setConflictedWeight(BigDecimal.valueOf(0.1));
                        defaultVector.setSurprisedWeight(BigDecimal.valueOf(0.1));
                        
                        vectorRepository.save(defaultVector);
                        created++;
                        
                        if (created % 100 == 0) {
                            log.info("Created {} R vectors...", created);
                        }
                    } catch (Exception e) {
                        log.error("Error creating R vector for user {}: {}", user.getId(), e.getMessage());
                        errors++;
                    }
                }
                
                // Flush to database after each batch
                vectorRepository.flush();
                log.info("Completed batch {}/{}", Math.min(i + batchSize, usersWithoutVectors.size()), usersWithoutVectors.size());
            }
            
            log.info("R vector migration complete. Created: {}, Skipped: {}, Errors: {}", created, skipped, errors);
            
        } catch (Exception e) {
            log.error("Fatal error during R vector migration: {}", e.getMessage(), e);
            throw new RuntimeException("R vector migration failed", e);
        }
        
        return new MigrationResult(created, skipped, errors);
    }
    
    /**
     * Create default feed state for all users who don't have one
     * Default state: block=0, version=1, no posts shown
     * 
     * @return MigrationResult with counts
     */
    @Transactional
    public MigrationResult createFeedStateForExistingUsers() {
        log.info("Starting feed state migration for existing users...");
        
        int created = 0;
        int skipped = 0;
        int errors = 0;
        
        try {
            // Find users without feed state
            List<User> usersWithoutFeedState = userRepository.findUsersWithoutFeedState();
            log.info("Found {} users without feed state", usersWithoutFeedState.size());
            
            for (User user : usersWithoutFeedState) {
                try {
                    // Check if feed state was created by another process
                    if (feedStateRepository.existsByUserId(user.getId())) {
                        skipped++;
                        continue;
                    }
                    
                    // Create default feed state
                    UserFeedState feedState = new UserFeedState();
                    feedState.setUserId(user.getId());
                    feedState.setCurrentBlockNumber(0);
                    feedState.setCurrentRVectorVersion(1);
                    feedState.setTotalBlocksGenerated(0);
                    feedState.setShownPostIds(new HashSet<>());
                    feedState.setLastAccessedAt(LocalDateTime.now());
                    
                    feedStateRepository.save(feedState);
                    created++;
                    
                    if (created % 100 == 0) {
                        log.info("Created {} feed states...", created);
                    }
                } catch (Exception e) {
                    log.error("Error creating feed state for user {}: {}", user.getId(), e.getMessage());
                    errors++;
                }
            }
            
            log.info("Feed state migration complete. Created: {}, Skipped: {}, Errors: {}", created, skipped, errors);
            
        } catch (Exception e) {
            log.error("Fatal error during feed state migration: {}", e.getMessage(), e);
            throw new RuntimeException("Feed state migration failed", e);
        }
        
        return new MigrationResult(created, skipped, errors);
    }
    
    /**
     * Run both migrations: R vectors and feed states
     * 
     * @return Combined MigrationResult
     */
    @Transactional
    public MigrationResult runAllMigrations() {
        log.info("Starting all recommendation engine data migrations...");
        
        MigrationResult vectorsResult = createDefaultRVectorsForExistingUsers();
        MigrationResult feedStateResult = createFeedStateForExistingUsers();
        
        int totalCreated = vectorsResult.created() + feedStateResult.created();
        int totalSkipped = vectorsResult.skipped() + feedStateResult.skipped();
        int totalErrors = vectorsResult.errors() + feedStateResult.errors();
        
        log.info("All migrations complete. Total: Created={}, Skipped={}, Errors={}", 
                 totalCreated, totalSkipped, totalErrors);
        
        return new MigrationResult(totalCreated, totalSkipped, totalErrors);
    }
    
    /**
     * Result record for migration operations
     */
    public record MigrationResult(int created, int skipped, int errors) {
        public boolean hasErrors() {
            return errors > 0;
        }
        
        public int total() {
            return created + skipped + errors;
        }
        
        public boolean isSuccessful() {
            return created > 0 && errors == 0;
        }
    }
}


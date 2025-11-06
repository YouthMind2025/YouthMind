package ai.youthmind.config;

import ai.youthmind.interests.entity.UserInterest;
import ai.youthmind.interests.repository.UserInterestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Initialize mock data for testing purposes
 */
@Component
public class MockDataInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(MockDataInitializer.class);
    
    @Autowired
    private UserInterestRepository userInterestRepository;
    
    // User ID from your request: 5673c094-2cee-49f2-9a3c-f6ff4e28b722
    private static final UUID TEST_USER_ID = UUID.fromString("5673c094-2cee-49f2-9a3c-f6ff4e28b722");
    
    @Override
    public void run(String... args) {
        initializeMockUserInterests();
    }
    
    private void initializeMockUserInterests() {
        try {
            // Check if data already exists
            List<UserInterest> existing = userInterestRepository.findByUserIdOrderByScoreDesc(TEST_USER_ID);
            if (!existing.isEmpty()) {
                logger.info("Mock user interests already exist for user {}, skipping initialization", TEST_USER_ID);
                return;
            }
            
            logger.info("Initializing mock user interests for user {}", TEST_USER_ID);
            
            // Create mock interests
            List<MockInterest> mockInterests = Arrays.asList(
                new MockInterest("Mental Health", 0.95),
                new MockInterest("Anxiety Support", 0.88),
                new MockInterest("Mindfulness", 0.82),
                new MockInterest("Depression Awareness", 0.79),
                new MockInterest("Self-Care", 0.75),
                new MockInterest("Stress Management", 0.72),
                new MockInterest("Therapy Resources", 0.68),
                new MockInterest("Emotional Wellness", 0.65),
                new MockInterest("Meditation", 0.60),
                new MockInterest("Youth Support", 0.55),
                new MockInterest("Coping Strategies", 0.52),
                new MockInterest("Mental Health Education", 0.48)
            );
            
            for (MockInterest mock : mockInterests) {
                UserInterest interest = new UserInterest();
                interest.setUserId(TEST_USER_ID);
                interest.setTopic(mock.topic);
                interest.setScore(mock.score);
                interest.setUpdatedAt(Instant.now());
                userInterestRepository.save(interest);
            }
            
            logger.info("Successfully initialized {} mock user interests", mockInterests.size());
            
        } catch (Exception e) {
            logger.error("Error initializing mock user interests: {}", e.getMessage(), e);
        }
    }
    
    private static class MockInterest {
        String topic;
        double score;
        
        MockInterest(String topic, double score) {
            this.topic = topic;
            this.score = score;
        }
    }
}




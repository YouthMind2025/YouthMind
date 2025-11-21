package ai.youthmind.service;

import ai.youthmind.dto.UserPersonalityProfile;
import ai.youthmind.entity.User;
import ai.youthmind.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing user personality profiles
 */
@Service
public class UserPersonalityService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserPersonalityService.class);
    
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public UserPersonalityService(
            UserRepository userRepository,
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Get or create user personality profile
     */
    public UserPersonalityProfile getUserPersonalityProfile(UUID userId) {
        String cacheKey = "user_personality:" + userId;
        
        try {
            // Try to get from cache first
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.convertValue(cached, UserPersonalityProfile.class);
            }
        } catch (Exception e) {
            logger.warn("Failed to get personality profile from cache: {}", e.getMessage());
        }
        
        // Get from database or create new
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        
        User user = userOpt.get();
        
        // Create default personality profile
        UserPersonalityProfile profile = new UserPersonalityProfile();
        profile.setUserId(userId.toString());
        profile.setPreferredPersonalityType("supportive");
        profile.setCommunicationStyle("gentle");
        profile.setInterests(new ArrayList<>());
        profile.setTriggers(new ArrayList<>());
        profile.setPreferences(new HashMap<>());
        profile.setEmotionalState("neutral");
        profile.setPreferredLanguage("en");
        profile.setConversationCount(0);
        profile.setLastUpdated(LocalDateTime.now());
        profile.setTopicEngagement(new HashMap<>());
        profile.setPreferredResponseLength(Arrays.asList("medium"));
        profile.setEnableHumor(true);
        profile.setEnableEmpathy(true);
        profile.setTimezone("UTC");
        profile.setCulturalContext("general");
        
        // Cache the profile
        try {
            redisTemplate.opsForValue().set(cacheKey, profile, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            logger.warn("Failed to cache personality profile: {}", e.getMessage());
        }
        
        return profile;
    }
    
    /**
     * Update user personality profile
     */
    public UserPersonalityProfile updateUserPersonalityProfile(UUID userId, UserPersonalityProfile profile) {
        try {
            // Update the profile
            profile.setUserId(userId.toString());
            profile.setLastUpdated(LocalDateTime.now());
            
            // Cache the updated profile
            String cacheKey = "user_personality:" + userId;
            redisTemplate.opsForValue().set(cacheKey, profile, 1, TimeUnit.HOURS);
            
            logger.info("Updated personality profile for user: {}", userId);
            return profile;
            
        } catch (Exception e) {
            logger.error("Error updating personality profile: {}", e.getMessage(), e);
            return getUserPersonalityProfile(userId); // Return existing profile on error
        }
    }
}

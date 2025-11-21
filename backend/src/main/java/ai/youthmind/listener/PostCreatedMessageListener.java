package ai.youthmind.listener;

import ai.youthmind.service.AIPostAnalysisService;
import ai.youthmind.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

//@Component
public class PostCreatedMessageListener implements MessageListener {
    
    private static final Logger log = LoggerFactory.getLogger(PostCreatedMessageListener.class);
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private AIPostAnalysisService aiPostAnalysisService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(pattern);
        String messageBody = new String(message.getBody());
        
        log.info("Received message on channel: {}", channel);
        log.info("Message: {}", messageBody);
        
        try {
            // Try to parse as structured event
            RedisService.PostCreatedEvent event = objectMapper.readValue(messageBody, RedisService.PostCreatedEvent.class);
            handlePostCreatedEvent(event);
        } catch (JsonProcessingException e) {
            // Handle as plain text message (backward compatibility)
            log.warn("Failed to parse structured event, handling as plain text: {}", messageBody);
            handlePlainTextMessage(messageBody);
        }
    }
    
    private void handlePostCreatedEvent(RedisService.PostCreatedEvent event) {
        log.info("Processing structured post created event:");
        log.info("  Post ID: {}", event.getPostId());
        log.info("  User ID: {}", event.getUserId());
        log.info("  Emotion: {}", event.getEmotion());
        log.info("  Timestamp: {}", event.getTimestamp());
        
        // Invalidate feed cache when new post is created
        redisService.invalidateFeedCache();
        
        // Update hot post scoring if needed
        if (event.getEmotion() != null) {
            double score = calculateHotPostScore(event.getEmotion(), event.getTimestamp());
            redisService.updateHotPostScore(event.getPostId(), score);
        }
        
        // Trigger AI analysis request
        try {
            log.info("Triggering AI analysis for post: {}", event.getPostId());
            aiPostAnalysisService.analyzePostAsync(UUID.fromString(event.getPostId()));
            log.info("AI analysis request submitted for post: {}", event.getPostId());
        } catch (Exception e) {
            log.error("Error triggering AI analysis for post {}: {}", event.getPostId(), e.getMessage(), e);
        }
        
        log.info("Post created event processed successfully");
    }
    
    private void handlePlainTextMessage(String message) {
        log.info("Processing plain text message: {}", message);
        
        // For backward compatibility, still invalidate cache
        redisService.invalidateFeedCache();
    }
    
    private double calculateHotPostScore(String emotion, long timestamp) {
        // Simple scoring algorithm - can be enhanced by Harley
        double baseScore = 1.0;
        
        // Boost score for certain emotions
        switch (emotion.toLowerCase()) {
            case "excited":
            case "happy":
            case "grateful":
                baseScore += 0.5;
                break;
            case "anxious":
            case "stressed":
            case "sad":
                baseScore += 0.3;
                break;
            default:
                baseScore += 0.1;
                break;
        }
        
        // Time decay - newer posts get higher scores
        long currentTime = System.currentTimeMillis();
        long ageInHours = (currentTime - timestamp) / (1000 * 60 * 60);
        double timeDecay = Math.max(0.1, 1.0 - (ageInHours * 0.1));
        
        return baseScore * timeDecay;
    }
}

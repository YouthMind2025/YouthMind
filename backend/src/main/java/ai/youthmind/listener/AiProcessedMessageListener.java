package ai.youthmind.listener;

import ai.youthmind.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

//@Component
public class AiProcessedMessageListener implements MessageListener {
    
    private static final Logger log = LoggerFactory.getLogger(AiProcessedMessageListener.class);
    
    @Autowired
    private RedisService redisService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(pattern);
        String messageBody = new String(message.getBody());
        
        log.info("Received AI processed message on channel: {}", channel);
        log.info("Message: {}", messageBody);
        
        try {
            // Try to parse as structured event
            RedisService.AiProcessedEvent event = objectMapper.readValue(messageBody, RedisService.AiProcessedEvent.class);
            handleAiProcessedEvent(event);
        } catch (JsonProcessingException e) {
            // Handle as plain text message (backward compatibility)
            log.warn("Failed to parse structured AI event, handling as plain text: {}", messageBody);
            handlePlainTextMessage(messageBody);
        }
    }
    
    private void handleAiProcessedEvent(RedisService.AiProcessedEvent event) {
        log.info("Processing structured AI processed event:");
        log.info("  Post ID: {}", event.getPostId());
        log.info("  Analysis Result: {}", event.getAnalysisResult());
        log.info("  Timestamp: {}", event.getTimestamp());
        
        // Invalidate post cache to reflect new AI analysis
        redisService.invalidatePostCache(event.getPostId());
        
        // Invalidate feed cache to show updated AI metadata
        redisService.invalidateFeedCache();
        
        // Update hot post scoring based on AI analysis
        updateHotPostScoreFromAI(event.getPostId(), event.getAnalysisResult());
        
        log.info("AI processed event handled successfully");
    }
    
    private void handlePlainTextMessage(String message) {
        log.info("Processing plain text AI message: {}", message);
        
        // For backward compatibility, invalidate relevant caches
        redisService.invalidateFeedCache();
    }
    
    private void updateHotPostScoreFromAI(String postId, String analysisResult) {
        try {
            // Parse AI analysis result (assuming JSON format)
            // This is a simplified example - actual implementation depends on AI service format
            if (analysisResult.contains("\"sentiment\":\"positive\"")) {
                redisService.updateHotPostScore(postId, 1.5);
            } else if (analysisResult.contains("\"sentiment\":\"negative\"")) {
                redisService.updateHotPostScore(postId, 1.2);
            } else {
                redisService.updateHotPostScore(postId, 1.0);
            }
        } catch (Exception e) {
            log.warn("Failed to update hot post score from AI analysis: {}", e.getMessage());
        }
    }
}

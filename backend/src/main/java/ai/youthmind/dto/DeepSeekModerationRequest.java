package ai.youthmind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for DeepSeek API moderation calls
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekModerationRequest {
    
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("messages")
    private List<Message> messages;
    
    @JsonProperty("temperature")
    private Double temperature = 0.1; // Low temperature for consistent moderation decisions
    
    @JsonProperty("max_tokens")
    private Integer maxTokens = 100; // Short responses for yes/no decisions
    
    @JsonProperty("stream")
    private Boolean stream = false;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        @JsonProperty("role")
        private String role; // "system", "user", "assistant"
        
        @JsonProperty("content")
        private String content;
    }
}

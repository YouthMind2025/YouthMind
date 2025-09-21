package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIChatSuggestionsRequest {
    
    @NotBlank(message = "Message text is required")
    private String message;
    
    @NotNull(message = "Conversation ID is required")
    private String conversationId;
    
    private String personalityType; // "supportive", "professional", "casual", "empathetic"
    private String context; // Additional context for the AI
    private List<String> topics; // Topics to focus on
    private Map<String, Object> userPreferences; // User-specific preferences
    private boolean enableSafetyFiltering = true;
    private boolean enablePersonalityAdaptation = true;
    private String emotionContext; // Current emotional state of user
    private String previousMessageContext; // Context from previous messages
    private int maxSuggestions = 3; // Maximum number of suggestions to generate
    private String suggestionType; // "questions", "responses", "topics", "actions"
}

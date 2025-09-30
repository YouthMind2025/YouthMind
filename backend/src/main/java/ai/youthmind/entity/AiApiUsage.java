package ai.youthmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking AI API usage, costs, and performance metrics.
 * Used for billing, monitoring, and analytics purposes.
 */
@Entity
@Table(name = "ai_api_usage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiApiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private UUID userId;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 50)
    private RequestType requestType;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "prompt_tokens", nullable = false)
    @Builder.Default
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens", nullable = false)
    @Builder.Default
    private Integer completionTokens = 0;

    @Column(name = "total_tokens", nullable = false)
    @Builder.Default
    private Integer totalTokens = 0;

    @Column(name = "cost_per_token", nullable = false, precision = 10, scale = 8)
    @Builder.Default
    private BigDecimal costPerToken = BigDecimal.ZERO;

    @Column(name = "total_cost", nullable = false, precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "request_duration_ms", nullable = false)
    @Builder.Default
    private Integer requestDurationMs = 0;

    @Column(name = "success", nullable = false)
    @Builder.Default
    private Boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "request_payload_size")
    @Builder.Default
    private Integer requestPayloadSize = 0;

    @Column(name = "response_payload_size")
    @Builder.Default
    private Integer responsePayloadSize = 0;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enum for different types of AI API requests.
     */
    public enum RequestType {
        MODERATION("Content Moderation"),
        CONTENT_GENERATION("Content Generation"),
        SENTIMENT_ANALYSIS("Sentiment Analysis"),
        LANGUAGE_DETECTION("Language Detection"),
        TEXT_SUMMARIZATION("Text Summarization"),
        TRANSLATION("Translation"),
        OTHER("Other");

        private final String description;

        RequestType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Calculate total tokens and cost before persisting or updating.
     */
    @PrePersist
    @PreUpdate
    public void calculateMetrics() {
        // Calculate total tokens
        this.totalTokens = (this.promptTokens != null ? this.promptTokens : 0) + 
                          (this.completionTokens != null ? this.completionTokens : 0);
        
        // Calculate total cost
        if (this.totalTokens != null && this.costPerToken != null) {
            this.totalCost = this.costPerToken.multiply(new BigDecimal(this.totalTokens));
        }
    }
}

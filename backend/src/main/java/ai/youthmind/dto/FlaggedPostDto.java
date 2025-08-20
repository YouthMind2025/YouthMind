package ai.youthmind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a post that has been flagged for moderation.
 * Matches the structure expected by the admin-panel front-end.
 */
public class FlaggedPostDto {

    @JsonProperty("post_id")
    @Schema(description = "Unique identifier of the flagged post")
    private UUID postId;

    @JsonProperty("flag_reason")
    @Schema(description = "Reason why the post was flagged")
    private String flagReason;

    @JsonProperty("flagged_at")
    @Schema(description = "Timestamp when the post was flagged")
    private LocalDateTime flaggedAt;

    @JsonProperty("ai_confidence")
    @Schema(description = "AI model confidence that the post violates policy", example = "0.92")
    private Double aiConfidence;

    @JsonProperty("content_preview")
    @Schema(description = "Truncated preview of the post content")
    private String contentPreview;

    @JsonProperty("status")
    @Schema(description = "Current moderation status", example = "pending_review")
    private String status;

    // Getters and setters
    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }

    public String getFlagReason() { return flagReason; }
    public void setFlagReason(String flagReason) { this.flagReason = flagReason; }

    public LocalDateTime getFlaggedAt() { return flaggedAt; }
    public void setFlaggedAt(LocalDateTime flaggedAt) { this.flaggedAt = flaggedAt; }

    public Double getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(Double aiConfidence) { this.aiConfidence = aiConfidence; }

    public String getContentPreview() { return contentPreview; }
    public void setContentPreview(String contentPreview) { this.contentPreview = contentPreview; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
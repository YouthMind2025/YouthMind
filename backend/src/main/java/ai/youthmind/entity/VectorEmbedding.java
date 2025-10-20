package ai.youthmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity for storing vector embeddings in PostgreSQL.
 * This serves as a reference table while actual vectors are stored in Qdrant.
 */
@Entity
@Table(name = "vector_embeddings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType;

    @Column(name = "qdrant_point_id", nullable = false, unique = true)
    private String qdrantPointId;

    @Column(name = "collection_name", nullable = false)
    private String collectionName;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "vector_dimension", nullable = false)
    private Integer vectorDimension;

    @Column(name = "embedding_text")
    private String embeddingText;

    @Column(name = "metadata")
    private String metadata; // JSON string for additional metadata

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public enum ContentType {
        POST, COMMENT, MODERATION_RESULT, USER_PROFILE, ARTICLE, AI_RESPONSE
    }

    // Helper methods for metadata handling
    public void setMetadataMap(Map<String, Object> metadataMap) {
        if (metadataMap != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                this.metadata = mapper.writeValueAsString(metadataMap);
            } catch (Exception e) {
                this.metadata = "{}";
            }
        }
    }

    public Map<String, Object> getMetadataMap() {
        if (metadata != null && !metadata.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(metadata, Map.class);
            } catch (Exception e) {
                return Map.of();
            }
        }
        return Map.of();
    }

    // Helper method to check if embedding is expired
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    // Helper method to check if embedding is valid
    public boolean isValid() {
        return isActive && !isExpired() && qualityScore != null && qualityScore > 0.5;
    }
}

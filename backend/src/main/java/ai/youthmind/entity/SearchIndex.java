package ai.youthmind.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "search_index", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"entity_type", "entity_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchIndex {
    
    public enum EntityType {
        POST, USER, COMMENT
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    @NotNull(message = "Entity type is required")
    private EntityType entityType;
    
    @Column(name = "entity_id", nullable = false)
    @NotNull(message = "Entity ID is required")
    private UUID entityId;
    
    @Column(name = "search_vector", columnDefinition = "tsvector")
    private String searchVector;
    
    @Column(name = "content_hash", length = 64)
    private String contentHash;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Explicit getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public EntityType getEntityType() {
        return entityType;
    }
    
    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }
    
    public UUID getEntityId() {
        return entityId;
    }
    
    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }
    
    public String getSearchVector() {
        return searchVector;
    }
    
    public void setSearchVector(String searchVector) {
        this.searchVector = searchVector;
    }
    
    public String getContentHash() {
        return contentHash;
    }
    
    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

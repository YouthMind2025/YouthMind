package ai.youthmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User entity representing a registered user in the YouthMind platform")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier for the user", example = "1")
    private Long id;
    
    @Column(unique = true, nullable = false)
    @Schema(description = "User's email address (used for login)", example = "user@example.com")
    private String email;
    
    @Column(nullable = false)
    @Schema(description = "User's encrypted password", example = "encrypted_password_hash")
    private String password;
    
    @Column(name = "first_name")
    @Schema(description = "User's first name", example = "John")
    private String firstName;
    
    @Column(name = "last_name")
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;
    
    @Column(name = "created_at")
    @Schema(description = "Timestamp when the user account was created", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @Schema(description = "Timestamp when the user account was last updated", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 
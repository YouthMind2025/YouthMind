package ai.youthmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpCode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String code;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private boolean used = false;
    
    @Column(nullable = false)
    private int attempts = 0;
    
    @Column(nullable = false)
    private boolean expired = false;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    public boolean isExpired() {
        return expired || LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !used && !isExpired() && attempts < 3;
    }
}

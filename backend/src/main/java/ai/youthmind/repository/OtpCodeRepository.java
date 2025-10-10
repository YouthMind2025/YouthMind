package ai.youthmind.repository;

import ai.youthmind.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, String> {
    
    Optional<OtpCode> findByEmailAndCodeAndUsedFalse(String email, String code);
    
    List<OtpCode> findByEmailAndUsedFalseOrderByCreatedAtDesc(String email);
    
    @Modifying
    @Query("UPDATE OtpCode o SET o.expired = true WHERE o.expiresAt < :now AND o.expired = false")
    void expireOldCodes(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.createdAt < :cutoff")
    void deleteOldCodes(@Param("cutoff") LocalDateTime cutoff);
    
    @Query("SELECT COUNT(o) FROM OtpCode o WHERE o.email = :email AND o.createdAt > :since")
    int countRecentCodesByEmail(@Param("email") String email, @Param("since") LocalDateTime since);
}

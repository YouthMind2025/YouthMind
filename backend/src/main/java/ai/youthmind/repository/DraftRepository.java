package ai.youthmind.repository;

import ai.youthmind.entity.Draft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DraftRepository extends JpaRepository<Draft, UUID> {
    
    Page<Draft> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);
    
    void deleteByUserId(UUID userId);
}

package ai.youthmind.repository;

import ai.youthmind.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    
    Page<Conversation> findByUserIdOrderByIsPinnedDescUpdatedAtDesc(UUID userId, Pageable pageable);
    
    void deleteByUserId(UUID userId);
}

package ai.youthmind.repository;

import ai.youthmind.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    Page<Message> findByConversationIdOrderByTimestampAsc(UUID conversationId, Pageable pageable);
    
    List<Message> findByConversationIdOrderByTimestampAsc(UUID conversationId);
    
    List<Message> findByConversationIdOrderByTimestampDesc(UUID conversationId);
    
    void deleteByConversationId(UUID conversationId);
}

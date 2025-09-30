
package ai.youthmind.memory.repository;

import ai.youthmind.memory.entity.AiMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiMemoryRepository extends JpaRepository<AiMemory, UUID> {
    List<AiMemory> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);
}

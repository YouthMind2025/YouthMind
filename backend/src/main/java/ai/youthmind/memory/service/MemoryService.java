
package ai.youthmind.memory.service;

import ai.youthmind.memory.dto.ContextResponse;
import ai.youthmind.memory.entity.AiMemory;
import ai.youthmind.memory.entity.UserPreference;
import ai.youthmind.memory.repository.AiMemoryRepository;
import ai.youthmind.memory.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service("memoryPackageService")
public class MemoryService {
    private final AiMemoryRepository memRepo;
    private final UserPreferenceRepository prefRepo;

    public MemoryService(AiMemoryRepository memRepo, UserPreferenceRepository prefRepo) {
        this.memRepo = memRepo;
        this.prefRepo = prefRepo;
    }

    @Transactional
    public AiMemory append(UUID userId, String label, String content) {
        AiMemory m = new AiMemory();
        m.setUserId(userId);
        m.setLabel(label);
        m.setContent(content);
        return memRepo.save(m);
    }

    @Transactional
    public UserPreference setPref(UUID userId, String key, String value) {
        return prefRepo.findByUserIdAndPrefKey(userId, key).map(p -> {
            p.setPrefValue(value);
            p.setUpdatedAt(java.time.Instant.now());
            return prefRepo.save(p);
        }).orElseGet(() -> {
            UserPreference p = new UserPreference();
            p.setUserId(userId);
            p.setPrefKey(key);
            p.setPrefValue(value);
            return prefRepo.save(p);
        });
    }

    public ContextResponse context(UUID userId) {
        var memories = memRepo.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId().toString());
            map.put("label", m.getLabel());
            map.put("content", m.getContent());
            map.put("createdAt", m.getCreatedAt().toString());
            return map;
        }).collect(Collectors.toList());

        Map<String, String> prefs = prefRepo.findByUserId(userId).stream().collect(
                Collectors.toMap(UserPreference::getPrefKey, UserPreference::getPrefValue));

        return new ContextResponse(memories, prefs);
    }
}


package ai.youthmind.memory.dto;

import java.util.List;
import java.util.Map;

public record ContextResponse(
        List<Map<String, Object>> recentMemories,
        Map<String, String> preferences
) {}

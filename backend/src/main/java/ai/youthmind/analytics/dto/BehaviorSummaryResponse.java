
package ai.youthmind.analytics.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record BehaviorSummaryResponse(
        UUID userId,
        long totalEvents,
        long totalViewSeconds,
        double avgViewSeconds,
        Map<String, Long> interactions,
        Map<String, Long> eventsByDayOfWeek,
        Instant generatedAt
) {}

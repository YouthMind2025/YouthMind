
package ai.youthmind.analytics.service;

import ai.youthmind.analytics.dto.BehaviorSummaryResponse;
import ai.youthmind.analytics.dto.InteractionEventRequest;
import ai.youthmind.analytics.dto.ViewEventRequest;
import ai.youthmind.analytics.entity.UserBehaviorAnalytics;
import ai.youthmind.analytics.repository.UserBehaviorAnalyticsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BehaviorAnalyticsService {
    private final UserBehaviorAnalyticsRepository repo;
    private final ObjectMapper mapper;

    public BehaviorAnalyticsService(UserBehaviorAnalyticsRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public void trackView(ViewEventRequest req) {
        UserBehaviorAnalytics e = new UserBehaviorAnalytics(
                req.getUserId(),
                req.getPostId(),
                Math.max(0, req.getViewDurationSeconds()),
                null,
                req.getMetadataJson()
        );
        repo.save(e);
    }

    public void logInteraction(InteractionEventRequest req) {
        String t = req.getType() == null ? "UNKNOWN" : req.getType().toUpperCase(Locale.ROOT);
        UserBehaviorAnalytics e = new UserBehaviorAnalytics(
                req.getUserId(),
                req.getPostId(),
                null,
                t,
                req.getMetadataJson()
        );
        repo.save(e);
    }

    public BehaviorSummaryResponse summarize(UUID userId, int days) {
        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofDays(days));
        List<UserBehaviorAnalytics> list = repo.findByUserIdAndCreatedAtBetween(userId, from, to);

        long totalView = list.stream().map(UserBehaviorAnalytics::getViewDurationSeconds)
                .filter(Objects::nonNull).mapToLong(Long::longValue).sum();
        long viewCount = list.stream().map(UserBehaviorAnalytics::getViewDurationSeconds)
                .filter(Objects::nonNull).count();
        double avgView = viewCount == 0 ? 0.0 : (double) totalView / viewCount;

        Map<String, Long> interactions = list.stream()
                .map(UserBehaviorAnalytics::getInteractionType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        Map<String, Long> byDow = new LinkedHashMap<>();
        for (DayOfWeek d : DayOfWeek.values()) byDow.put(d.name(), 0L);
        for (UserBehaviorAnalytics e : list) {
            String key = e.getCreatedAt().atZone(ZoneOffset.UTC).getDayOfWeek().name();
            byDow.put(key, byDow.getOrDefault(key, 0L) + 1);
        }

        return new BehaviorSummaryResponse(userId, list.size(), totalView, avgView, interactions, byDow, Instant.now());
    }
}

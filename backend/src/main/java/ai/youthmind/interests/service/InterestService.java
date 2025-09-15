
package ai.youthmind.interests.service;

import ai.youthmind.interests.entity.UserInterest;
import ai.youthmind.interests.repository.UserInterestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InterestService {
    private final UserInterestRepository repo;

    public InterestService(UserInterestRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public List<UserInterest> analyzeAndUpdate(UUID userId, String text) {
        if (text == null) text = "";
        String[] words = text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        Map<String, Long> counts = Arrays.stream(words)
                .filter(w -> w.length() > 2)
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        for (Map.Entry<String, Long> e : counts.entrySet()) {
            String topic = e.getKey();
            double inc = e.getValue();
            repo.findByUserIdAndTopic(userId, topic).ifPresentOrElse(ui -> {
                ui.setScore(ui.getScore() + inc);
                ui.setUpdatedAt(Instant.now());
                repo.save(ui);
            }, () -> {
                UserInterest ui = new UserInterest();
                ui.setUserId(userId);
                ui.setTopic(topic);
                ui.setScore(inc);
                ui.setUpdatedAt(Instant.now());
                repo.save(ui);
            });
        }
        return repo.findByUserIdOrderByScoreDesc(userId);
    }

    public List<UserInterest> top(UUID userId, int limit) {
        List<UserInterest> list = repo.findByUserIdOrderByScoreDesc(userId);
        return list.subList(0, Math.min(limit, list.size()));
    }
}

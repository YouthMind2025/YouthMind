package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngagementPatternsDto {
    private Integer resonancesCount;
    private Long commentsCount;
    private Integer collectionsCount;
    private Double engagementRate;
    private List<Long> responseTimes;
    private List<String> mostEngagedTopic;
    private Map<Integer, Long> resonanceActivityHour;
    private Map<Integer, Long> resonanceActivityDay;
    private Integer resonanceMostActiveHour;
    private Integer resonanceMostActiveDay;
    private Map<Integer, Long> commentActivityHour;
    private Map<Integer, Long> commentActivityDay;
    private Integer commentMostActiveHour;
    private Integer commentMostActiveDay;
    private Map<Integer, Long> collectionActivityHour;
    private Map<Integer, Long> collectionActivityDay;
    private Integer collectionMostActiveHour;
    private Integer collectionMostActiveDay;
    private List<UUID> mostResonancePosts;
    private List<UUID> mostCommentPosts;
    private List<UUID> mostViewPosts;

    public void setResonanceActivityHour(Map<Integer, Long> resonanceActivityHour) {
        this.resonanceActivityHour = resonanceActivityHour;
        this.resonanceMostActiveHour = resonanceActivityHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public void setResonanceActivityDay(Map<Integer, Long> resonanceActivityDay) {
        this.resonanceActivityDay = resonanceActivityDay;
        this.resonanceMostActiveDay = resonanceActivityDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public void setCommentActivityHour(Map<Integer, Long> commentActivityHour) {
        this.commentActivityHour = commentActivityHour;
        this.commentMostActiveHour = commentActivityHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public void setCommentActivityDay(Map<Integer, Long> commentActivityDay) {
        this.commentActivityDay = commentActivityDay;
        this.commentMostActiveDay = commentActivityDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public void setCollectionActivityHour(Map<Integer, Long> collectionActivityHour) {
        this.collectionActivityHour = collectionActivityHour;
        this.collectionMostActiveHour = collectionActivityHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public void setCollectionActivityDay(Map<Integer, Long> collectionActivityDay) {
        this.collectionActivityDay = collectionActivityDay;
        this.collectionMostActiveDay = collectionActivityDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}

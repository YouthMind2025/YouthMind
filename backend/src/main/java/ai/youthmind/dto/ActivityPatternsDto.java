package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.util.Pair;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityPatternsDto {
    private Integer postsCount;
    private Double postFrequencyDay;
    private Double postFrequencyWeek;
    private Double postFrequencyMonth;
    private Integer postMostActiveHour;
    private Integer postMostActiveDay;
    private Map<Integer, Integer> postActivityHour;
    private Map<Integer, Integer> postActivityDay;
    private Map<Pair<Integer, Integer>, Integer> postMonthlyTrend;
    private Map<Pair<Integer, Integer>, Integer> postSeasonalTrend;
    private Double postAverageResonances;
    private Double postAverageComments;
    private Double postAverageViews;
    private Map<String, Integer> postEmotionDistribution;
    private Double postAverageWords;
    private Double postAverageCharacters;

    // Additional setter methods for individual frequency fields
    public void setPostFrequencyDay(double postFrequencyDay) {
        this.postFrequencyDay = postFrequencyDay;
    }
    
    public void setPostFrequencyWeek(double postFrequencyWeek) {
        this.postFrequencyWeek = postFrequencyWeek;
    }
    
    public void setPostFrequencyMonth(double postFrequencyMonth) {
        this.postFrequencyMonth = postFrequencyMonth;
    }
    
    public void setPostFrequencies(double postFrequencyDay) {
        setPostFrequencyDay(postFrequencyDay);
        setPostFrequencyWeek(7 * postFrequencyDay);
        setPostFrequencyMonth((365 / 12) * postFrequencyDay);
    }

    public void setPostActivityHour(Map<Integer, Integer> postActivityHour) {
        this.postActivityHour = postActivityHour;
        this.postMostActiveHour = postActivityHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public void setPostActivityDay(Map<Integer, Integer> postActivityDay) {
        this.postActivityDay = postActivityDay;
        this.postMostActiveDay = postActivityDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}

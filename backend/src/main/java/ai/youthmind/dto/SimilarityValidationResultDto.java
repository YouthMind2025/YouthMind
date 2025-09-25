package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarityValidationResultDto {
    private List<SimilarityTestResult> similarityTests;
    private List<SimilarityTestResult> dissimilarityTests;
    private double similarityThreshold;
    private long validationTimestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimilarityTestResult {
        private String text1;
        private String text2;
        private double similarity;
        private boolean passed;
    }
}

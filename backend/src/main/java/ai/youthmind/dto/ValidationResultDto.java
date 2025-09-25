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
public class ValidationResultDto {
    private String text;
    private List<ValidationCheck> checks;
    private boolean overallPassed;
    private int embeddingDimension;
    private long validationTimestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationCheck {
        private String checkName;
        private boolean passed;
        private double actualValue;
        private String expectedRange;
        private String message;
    }
}

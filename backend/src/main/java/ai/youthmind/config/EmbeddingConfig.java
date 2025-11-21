package ai.youthmind.config;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "embedding")
@Validated
public class EmbeddingConfig {

    @NotNull
    private EmbeddingConfig.VersioningConfig versioningConfig = new VersioningConfig();

    @NotEmpty
    private Map<String, ModelConfig> models = new HashMap<>();

    @NotNull
    private ValidationConfig validationConfig = new ValidationConfig();

    @Data
    public static class VersioningConfig {
        private boolean enabled = true;
        private String defaultVersion = "v1";
        private boolean autoMigration = false;
        private int maxVersions = 5;
        private String versionHeader = "X-Model-Version";
        private String versionParam = "modelVersion";
    }

    @Data
    public static class ModelConfig {
        @NotNull
        private String path;
        private String name;
        private boolean enabled = true;
        private int maxLength = 512;
        private boolean normalize = true;
        @NotNull
        private String tokenizerConfig;
        @NotNull
        private String vocabFile;
    }

    @Data
    public static class ValidationConfig {

        @NotNull
        private QualityThresholds qualityThresholds = new QualityThresholds();

        @NotNull
        private TestCases testCases = new TestCases();

        private boolean enabled = true;
        private boolean preInferenceValidation = true;
        private boolean postInferenceValidation = true;

        @Data
        public static class QualityThresholds {
            @DecimalMin("0.1")
            @DecimalMax("1.0")
            private double minCosineSimilarity = 0.7;

            @DecimalMin("0.0")
            private double maxZeroRatio = 0.3;

            @DecimalMin("0.0")
            private double minNorm = 0.8;

            @DecimalMin("0.0")
            private double maxNorm = 1.2;

            @DecimalMin("0.0")
            private double maxNanRatio = 0.0;

            @DecimalMin("0.0")
            private double maxDuplicateRatio = 0.1;
        }

        @Data
        public static class TestCases {
            private List<String> similarityPairs = List.of(
                    "cat|kitten",
                    "car|vehicle",
                    "happy|joyful"
            );

            private List<String> dissimilarityPairs = List.of(
                    "cat|computer",
                    "happy|sad",
                    "hot|cold"
            );

            private List<String> duplicateTexts = List.of(
                    "The quick brown fox",
                    "The quick brown fox",
                    "Pancakes made by granny"
            );
        }
    }
}

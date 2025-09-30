package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for AI API usage analytics and statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiUsageAnalytics {

    // Overall statistics
    private Long totalRequests;
    private Long successfulRequests;
    private Long failedRequests;
    private BigDecimal totalCost;
    private Long totalTokens;
    private Double averageRequestDuration;
    private Double successRate;

    // Time-based statistics
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private List<DailyUsage> dailyUsage;
    private List<HourlyUsage> hourlyUsage;

    // User statistics
    private List<UserUsage> topUsersByCost;
    private List<UserUsage> topUsersByTokens;

    // Request type statistics
    private List<RequestTypeUsage> requestTypeUsage;

    // Model statistics
    private List<ModelUsage> modelUsage;

    // Error statistics
    private List<ErrorStatistic> errorStatistics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyUsage {
        private String date;
        private Long requestCount;
        private BigDecimal totalCost;
        private Long totalTokens;
        private Double averageDuration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyUsage {
        private Integer hour;
        private Long requestCount;
        private BigDecimal totalCost;
        private Double averageDuration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserUsage {
        private String userId;
        private String username;
        private Long requestCount;
        private BigDecimal totalCost;
        private Long totalTokens;
        private Double averageDuration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestTypeUsage {
        private String requestType;
        private Long requestCount;
        private BigDecimal totalCost;
        private Long totalTokens;
        private Double averageDuration;
        private Double successRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelUsage {
        private String modelName;
        private Long requestCount;
        private BigDecimal totalCost;
        private Long totalTokens;
        private Double averageDuration;
        private Double successRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorStatistic {
        private String errorMessage;
        private Long occurrenceCount;
        private Double percentage;
    }
}

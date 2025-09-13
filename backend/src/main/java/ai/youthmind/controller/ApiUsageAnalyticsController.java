package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.ApiUsageAnalytics;
import ai.youthmind.dto.ApiUsageRequest;
import ai.youthmind.dto.ApiUsageResponse;
import ai.youthmind.entity.AiApiUsage;
import ai.youthmind.service.ApiCostCalculationService;
import ai.youthmind.service.ApiUsageMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for AI API usage analytics and cost tracking.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "API Usage Analytics", description = "AI API usage analytics and cost tracking endpoints")
public class ApiUsageAnalyticsController {

    private final ApiUsageMonitoringService usageMonitoringService;
    private final ApiCostCalculationService costCalculationService;

    /**
     * Record AI API usage for tracking and billing.
     */
    @PostMapping("/usage")
    @Operation(summary = "Record AI API usage", description = "Record AI API usage for tracking and billing purposes")
    public ResponseEntity<ApiResponse<ApiUsageResponse>> recordUsage(@Valid @RequestBody ApiUsageRequest request) {
        log.info("Recording API usage: {} - {}", request.getRequestType(), request.getModelName());
        
        ApiUsageResponse response = usageMonitoringService.recordUsage(request);
        
        return ResponseEntity.ok(ApiResponse.<ApiUsageResponse>builder()
                .success(true)
                .message("API usage recorded successfully")
                .data(response)
                .build());
    }

    /**
     * Get user-specific analytics.
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #userId == authentication.principal.id)")
    @Operation(summary = "Get user analytics", description = "Get AI API usage analytics for a specific user")
    public ResponseEntity<ApiResponse<ApiUsageAnalytics>> getUserAnalytics(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Start date (ISO format)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Getting analytics for user: {} from {} to {}", userId, startDate, endDate);
        
        ApiUsageAnalytics analytics = usageMonitoringService.getUserAnalytics(userId, startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.<ApiUsageAnalytics>builder()
                .success(true)
                .message("User analytics retrieved successfully")
                .data(analytics)
                .build());
    }

    /**
     * Get system-wide analytics.
     */
    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get system analytics", description = "Get overall AI API usage analytics for the entire system")
    public ResponseEntity<ApiResponse<ApiUsageAnalytics>> getSystemAnalytics(
            @Parameter(description = "Start date (ISO format)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Getting system analytics from {} to {}", startDate, endDate);
        
        ApiUsageAnalytics analytics = usageMonitoringService.getSystemAnalytics(startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.<ApiUsageAnalytics>builder()
                .success(true)
                .message("System analytics retrieved successfully")
                .data(analytics)
                .build());
    }

    /**
     * Get analytics by request type.
     */
    @GetMapping("/request-type/{requestType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get analytics by request type", description = "Get AI API usage analytics filtered by request type")
    public ResponseEntity<ApiResponse<ApiUsageAnalytics>> getAnalyticsByRequestType(
            @Parameter(description = "Request type") @PathVariable AiApiUsage.RequestType requestType,
            @Parameter(description = "Start date (ISO format)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Getting analytics for request type: {} from {} to {}", requestType, startDate, endDate);
        
        ApiUsageAnalytics analytics = usageMonitoringService.getAnalyticsByRequestType(requestType, startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.<ApiUsageAnalytics>builder()
                .success(true)
                .message("Request type analytics retrieved successfully")
                .data(analytics)
                .build());
    }

    /**
     * Get analytics by model.
     */
    @GetMapping("/model/{modelName}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get analytics by model", description = "Get AI API usage analytics filtered by model name")
    public ResponseEntity<ApiResponse<ApiUsageAnalytics>> getAnalyticsByModel(
            @Parameter(description = "Model name") @PathVariable String modelName,
            @Parameter(description = "Start date (ISO format)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Getting analytics for model: {} from {} to {}", modelName, startDate, endDate);
        
        ApiUsageAnalytics analytics = usageMonitoringService.getAnalyticsByModel(modelName, startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.<ApiUsageAnalytics>builder()
                .success(true)
                .message("Model analytics retrieved successfully")
                .data(analytics)
                .build());
    }

    /**
     * Get user usage history with pagination.
     */
    @GetMapping("/user/{userId}/history")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #userId == authentication.principal.id)")
    @Operation(summary = "Get user usage history", description = "Get paginated AI API usage history for a specific user")
    public ResponseEntity<ApiResponse<Page<ApiUsageResponse>>> getUserUsageHistory(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting usage history for user: {} (page: {}, size: {})", userId, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ApiUsageResponse> history = usageMonitoringService.getUserUsageHistory(userId, pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<ApiUsageResponse>>builder()
                .success(true)
                .message("User usage history retrieved successfully")
                .data(history)
                .build());
    }

    /**
     * Get top users by cost.
     */
    @GetMapping("/top-users/cost")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get top users by cost", description = "Get users ranked by total AI API costs")
    public ResponseEntity<ApiResponse<List<ApiUsageAnalytics.UserUsage>>> getTopUsersByCost(
            @Parameter(description = "Number of top users to return") @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Getting top {} users by cost", limit);
        
        List<ApiUsageAnalytics.UserUsage> topUsers = usageMonitoringService.getTopUsersByCost(limit);
        
        return ResponseEntity.ok(ApiResponse.<List<ApiUsageAnalytics.UserUsage>>builder()
                .success(true)
                .message("Top users by cost retrieved successfully")
                .data(topUsers)
                .build());
    }

    /**
     * Get top users by token usage.
     */
    @GetMapping("/top-users/tokens")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get top users by tokens", description = "Get users ranked by total token usage")
    public ResponseEntity<ApiResponse<List<ApiUsageAnalytics.UserUsage>>> getTopUsersByTokens(
            @Parameter(description = "Number of top users to return") @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Getting top {} users by tokens", limit);
        
        List<ApiUsageAnalytics.UserUsage> topUsers = usageMonitoringService.getTopUsersByTokens(limit);
        
        return ResponseEntity.ok(ApiResponse.<List<ApiUsageAnalytics.UserUsage>>builder()
                .success(true)
                .message("Top users by tokens retrieved successfully")
                .data(topUsers)
                .build());
    }

    /**
     * Calculate user total cost.
     */
    @GetMapping("/user/{userId}/total-cost")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #userId == authentication.principal.id)")
    @Operation(summary = "Calculate user total cost", description = "Calculate total AI API costs for a specific user")
    public ResponseEntity<ApiResponse<BigDecimal>> calculateUserTotalCost(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Start date (ISO format)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Calculating total cost for user: {} from {} to {}", userId, startDate, endDate);
        
        BigDecimal totalCost;
        if (startDate != null && endDate != null) {
            totalCost = usageMonitoringService.calculateUserTotalCost(userId, startDate, endDate);
        } else {
            totalCost = usageMonitoringService.calculateUserTotalCost(userId);
        }
        
        return ResponseEntity.ok(ApiResponse.<BigDecimal>builder()
                .success(true)
                .message("User total cost calculated successfully")
                .data(totalCost)
                .build());
    }

    /**
     * Get usage statistics by request type.
     */
    @GetMapping("/statistics/request-types")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get request type statistics", description = "Get usage statistics grouped by request type")
    public ResponseEntity<ApiResponse<List<ApiUsageAnalytics.RequestTypeUsage>>> getRequestTypeStatistics() {
        log.info("Getting request type statistics");
        
        List<ApiUsageAnalytics.RequestTypeUsage> statistics = usageMonitoringService.getRequestTypeStatistics();
        
        return ResponseEntity.ok(ApiResponse.<List<ApiUsageAnalytics.RequestTypeUsage>>builder()
                .success(true)
                .message("Request type statistics retrieved successfully")
                .data(statistics)
                .build());
    }

    /**
     * Get usage statistics by model.
     */
    @GetMapping("/statistics/models")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get model statistics", description = "Get usage statistics grouped by model")
    public ResponseEntity<ApiResponse<List<ApiUsageAnalytics.ModelUsage>>> getModelStatistics() {
        log.info("Getting model statistics");
        
        List<ApiUsageAnalytics.ModelUsage> statistics = usageMonitoringService.getModelStatistics();
        
        return ResponseEntity.ok(ApiResponse.<List<ApiUsageAnalytics.ModelUsage>>builder()
                .success(true)
                .message("Model statistics retrieved successfully")
                .data(statistics)
                .build());
    }

    /**
     * Get daily usage statistics.
     */
    @GetMapping("/statistics/daily")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get daily usage statistics", description = "Get daily AI API usage statistics")
    public ResponseEntity<ApiResponse<List<ApiUsageAnalytics.DailyUsage>>> getDailyUsageStatistics(
            @Parameter(description = "Start date (ISO format)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        log.info("Getting daily usage statistics from {} to {}", startDate, endDate);
        
        List<ApiUsageAnalytics.DailyUsage> statistics = usageMonitoringService.getDailyUsageStatistics(startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.<List<ApiUsageAnalytics.DailyUsage>>builder()
                .success(true)
                .message("Daily usage statistics retrieved successfully")
                .data(statistics)
                .build());
    }

    /**
     * Get hourly usage statistics for a specific day.
     */
    @GetMapping("/statistics/hourly")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get hourly usage statistics", description = "Get hourly AI API usage statistics for a specific day")
    public ResponseEntity<ApiResponse<List<ApiUsageAnalytics.HourlyUsage>>> getHourlyUsageStatistics(
            @Parameter(description = "Date (ISO format)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        
        log.info("Getting hourly usage statistics for date: {}", date);
        
        List<ApiUsageAnalytics.HourlyUsage> statistics = usageMonitoringService.getHourlyUsageStatistics(date);
        
        return ResponseEntity.ok(ApiResponse.<List<ApiUsageAnalytics.HourlyUsage>>builder()
                .success(true)
                .message("Hourly usage statistics retrieved successfully")
                .data(statistics)
                .build());
    }

    /**
     * Get model pricing information.
     */
    @GetMapping("/pricing/models")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get model pricing", description = "Get pricing information for all supported AI models")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getModelPricing() {
        log.info("Getting model pricing information");
        
        Map<String, BigDecimal> pricing = costCalculationService.getAllModelPricing();
        
        return ResponseEntity.ok(ApiResponse.<Map<String, BigDecimal>>builder()
                .success(true)
                .message("Model pricing retrieved successfully")
                .data(pricing)
                .build());
    }

    /**
     * Calculate cost for specific usage.
     */
    @PostMapping("/cost/calculate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Calculate cost", description = "Calculate cost for specific model and token usage")
    public ResponseEntity<ApiResponse<BigDecimal>> calculateCost(
            @Parameter(description = "Model name") @RequestParam String modelName,
            @Parameter(description = "Prompt tokens") @RequestParam int promptTokens,
            @Parameter(description = "Completion tokens") @RequestParam int completionTokens) {
        
        log.info("Calculating cost for model: {}, prompt tokens: {}, completion tokens: {}", 
                modelName, promptTokens, completionTokens);
        
        BigDecimal cost = costCalculationService.calculateCost(modelName, promptTokens, completionTokens);
        
        return ResponseEntity.ok(ApiResponse.<BigDecimal>builder()
                .success(true)
                .message("Cost calculated successfully")
                .data(cost)
                .build());
    }

    /**
     * Get cost breakdown by request type.
     */
    @GetMapping("/cost/breakdown")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get cost breakdown", description = "Get cost breakdown by request type")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getCostBreakdown() {
        log.info("Getting cost breakdown by request type");
        
        Map<String, BigDecimal> breakdown = costCalculationService.getCostBreakdownByRequestType();
        
        return ResponseEntity.ok(ApiResponse.<Map<String, BigDecimal>>builder()
                .success(true)
                .message("Cost breakdown retrieved successfully")
                .data(breakdown)
                .build());
    }
}

package ai.youthmind.service;

import ai.youthmind.dto.ApiUsageAnalytics;
import ai.youthmind.dto.ApiUsageRequest;
import ai.youthmind.dto.ApiUsageResponse;
import ai.youthmind.entity.AiApiUsage;
import ai.youthmind.entity.User;
import ai.youthmind.repository.AiApiUsageRepository;
import ai.youthmind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for monitoring and tracking AI API usage, costs, and performance metrics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApiUsageMonitoringService {

    private final AiApiUsageRepository aiApiUsageRepository;
    private final UserRepository userRepository;

    /**
     * Record AI API usage for tracking and billing.
     */
    public ApiUsageResponse recordUsage(ApiUsageRequest request) {
        log.debug("Recording API usage: {} - {}", request.getRequestType(), request.getModelName());

        // Get user if userId is provided
        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElse(null);
        }

        // Create AI API usage record
        AiApiUsage usage = AiApiUsage.builder()
                .user(user)
                .userId(request.getUserId())
                .sessionId(request.getSessionId())
                .requestType(request.getRequestType())
                .modelName(request.getModelName())
                .promptTokens(request.getPromptTokens())
                .completionTokens(request.getCompletionTokens())
                .costPerToken(request.getCostPerToken())
                .requestDurationMs(request.getRequestDurationMs())
                .success(request.getSuccess())
                .errorMessage(request.getErrorMessage())
                .requestPayloadSize(request.getRequestPayloadSize())
                .responsePayloadSize(request.getResponsePayloadSize())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .build();

        // Save the usage record
        AiApiUsage savedUsage = aiApiUsageRepository.save(usage);

        log.info("Recorded API usage: ID={}, User={}, Type={}, Model={}, Cost={}, Duration={}ms",
                savedUsage.getId(), savedUsage.getUserId(), savedUsage.getRequestType(),
                savedUsage.getModelName(), savedUsage.getTotalCost(), savedUsage.getRequestDurationMs());

        return mapToResponse(savedUsage);
    }

    /**
     * Get usage analytics for a specific user.
     */
    @Transactional(readOnly = true)
    public ApiUsageAnalytics getUserAnalytics(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting user analytics for user: {} from {} to {}", userId, startDate, endDate);

        // Get user information
        User user = userRepository.findById(userId).orElse(null);
        String username = user != null ? user.getUsername() : "Unknown User";

        // Get usage records
        List<AiApiUsage> usageRecords = aiApiUsageRepository
                .findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startDate, endDate);

        return buildAnalytics(usageRecords, startDate, endDate, username);
    }

    /**
     * Get overall system analytics.
     */
    @Transactional(readOnly = true)
    public ApiUsageAnalytics getSystemAnalytics(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting system analytics from {} to {}", startDate, endDate);

        // Get all usage records in date range
        List<AiApiUsage> usageRecords = aiApiUsageRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(startDate, endDate);

        return buildAnalytics(usageRecords, startDate, endDate, "System");
    }

    /**
     * Get analytics by request type.
     */
    @Transactional(readOnly = true)
    public ApiUsageAnalytics getAnalyticsByRequestType(AiApiUsage.RequestType requestType, 
                                                       LocalDateTime startDate, 
                                                       LocalDateTime endDate) {
        log.debug("Getting analytics for request type: {} from {} to {}", requestType, startDate, endDate);

        List<AiApiUsage> usageRecords = aiApiUsageRepository
                .findByRequestTypeAndCreatedAtBetweenOrderByCreatedAtDesc(requestType, startDate, endDate);

        return buildAnalytics(usageRecords, startDate, endDate, requestType.getDescription());
    }

    /**
     * Get analytics by model.
     */
    @Transactional(readOnly = true)
    public ApiUsageAnalytics getAnalyticsByModel(String modelName, 
                                                LocalDateTime startDate, 
                                                LocalDateTime endDate) {
        log.debug("Getting analytics for model: {} from {} to {}", modelName, startDate, endDate);

        List<AiApiUsage> usageRecords = aiApiUsageRepository
                .findByModelNameAndCreatedAtBetweenOrderByCreatedAtDesc(modelName, startDate, endDate);

        return buildAnalytics(usageRecords, startDate, endDate, modelName);
    }

    /**
     * Get user usage history with pagination.
     */
    @Transactional(readOnly = true)
    public Page<ApiUsageResponse> getUserUsageHistory(UUID userId, Pageable pageable) {
        log.debug("Getting usage history for user: {} with pagination", userId);

        Page<AiApiUsage> usagePage = aiApiUsageRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return usagePage.map(this::mapToResponse);
    }

    /**
     * Get top users by cost.
     */
    @Transactional(readOnly = true)
    public List<ApiUsageAnalytics.UserUsage> getTopUsersByCost(int limit) {
        log.debug("Getting top {} users by cost", limit);

        List<Object[]> results = aiApiUsageRepository.findTopUsersByCost(PageRequest.of(0, limit));
        return results.stream()
                .map(this::mapToUserUsage)
                .collect(Collectors.toList());
    }

    /**
     * Get top users by token usage.
     */
    @Transactional(readOnly = true)
    public List<ApiUsageAnalytics.UserUsage> getTopUsersByTokens(int limit) {
        log.debug("Getting top {} users by tokens", limit);

        List<Object[]> results = aiApiUsageRepository.findTopUsersByTokens(PageRequest.of(0, limit));
        return results.stream()
                .map(this::mapToUserUsage)
                .collect(Collectors.toList());
    }

    /**
     * Calculate total cost for a user.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateUserTotalCost(UUID userId) {
        return aiApiUsageRepository.calculateTotalCostByUserId(userId);
    }

    /**
     * Calculate total cost for a user within date range.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateUserTotalCost(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        return aiApiUsageRepository.calculateTotalCostByUserIdAndDateRange(userId, startDate, endDate);
    }

    /**
     * Get usage statistics by request type.
     */
    @Transactional(readOnly = true)
    public List<ApiUsageAnalytics.RequestTypeUsage> getRequestTypeStatistics() {
        List<Object[]> results = aiApiUsageRepository.getUsageStatisticsByRequestType();
        return results.stream()
                .map(this::mapToRequestTypeUsage)
                .collect(Collectors.toList());
    }

    /**
     * Get usage statistics by model.
     */
    @Transactional(readOnly = true)
    public List<ApiUsageAnalytics.ModelUsage> getModelStatistics() {
        List<Object[]> results = aiApiUsageRepository.getUsageStatisticsByModel();
        return results.stream()
                .map(this::mapToModelUsage)
                .collect(Collectors.toList());
    }

    /**
     * Get daily usage statistics.
     */
    @Transactional(readOnly = true)
    public List<ApiUsageAnalytics.DailyUsage> getDailyUsageStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = aiApiUsageRepository.getDailyUsageStatistics(startDate, endDate);
        return results.stream()
                .map(this::mapToDailyUsage)
                .collect(Collectors.toList());
    }

    /**
     * Get hourly usage statistics for a specific day.
     */
    @Transactional(readOnly = true)
    public List<ApiUsageAnalytics.HourlyUsage> getHourlyUsageStatistics(LocalDateTime date) {
        List<Object[]> results = aiApiUsageRepository.getHourlyUsageStatistics(date);
        return results.stream()
                .map(this::mapToHourlyUsage)
                .collect(Collectors.toList());
    }

    // Private helper methods

    private ApiUsageAnalytics buildAnalytics(List<AiApiUsage> usageRecords, 
                                           LocalDateTime startDate, 
                                           LocalDateTime endDate, 
                                           String context) {
        if (usageRecords.isEmpty()) {
            return ApiUsageAnalytics.builder()
                    .totalRequests(0L)
                    .successfulRequests(0L)
                    .failedRequests(0L)
                    .totalCost(BigDecimal.ZERO)
                    .totalTokens(0L)
                    .averageRequestDuration(0.0)
                    .successRate(0.0)
                    .periodStart(startDate)
                    .periodEnd(endDate)
                    .build();
        }

        long totalRequests = usageRecords.size();
        long successfulRequests = usageRecords.stream().mapToLong(r -> r.getSuccess() ? 1 : 0).sum();
        long failedRequests = totalRequests - successfulRequests;
        BigDecimal totalCost = usageRecords.stream()
                .map(AiApiUsage::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalTokens = usageRecords.stream()
                .mapToLong(r -> r.getTotalTokens() != null ? r.getTotalTokens() : 0)
                .sum();
        double averageRequestDuration = usageRecords.stream()
                .mapToInt(r -> r.getRequestDurationMs() != null ? r.getRequestDurationMs() : 0)
                .average()
                .orElse(0.0);
        double successRate = totalRequests > 0 ? (double) successfulRequests / totalRequests * 100 : 0.0;

        return ApiUsageAnalytics.builder()
                .totalRequests(totalRequests)
                .successfulRequests(successfulRequests)
                .failedRequests(failedRequests)
                .totalCost(totalCost)
                .totalTokens(totalTokens)
                .averageRequestDuration(averageRequestDuration)
                .successRate(successRate)
                .periodStart(startDate)
                .periodEnd(endDate)
                .dailyUsage(getDailyUsageStatistics(startDate, endDate))
                .requestTypeUsage(getRequestTypeStatistics())
                .modelUsage(getModelStatistics())
                .build();
    }

    private ApiUsageResponse mapToResponse(AiApiUsage usage) {
        return ApiUsageResponse.builder()
                .id(usage.getId())
                .userId(usage.getUserId())
                .sessionId(usage.getSessionId())
                .requestType(usage.getRequestType())
                .modelName(usage.getModelName())
                .promptTokens(usage.getPromptTokens())
                .completionTokens(usage.getCompletionTokens())
                .totalTokens(usage.getTotalTokens())
                .costPerToken(usage.getCostPerToken())
                .totalCost(usage.getTotalCost())
                .requestDurationMs(usage.getRequestDurationMs())
                .success(usage.getSuccess())
                .errorMessage(usage.getErrorMessage())
                .requestPayloadSize(usage.getRequestPayloadSize())
                .responsePayloadSize(usage.getResponsePayloadSize())
                .ipAddress(usage.getIpAddress())
                .userAgent(usage.getUserAgent())
                .createdAt(usage.getCreatedAt())
                .updatedAt(usage.getUpdatedAt())
                .build();
    }

    private ApiUsageAnalytics.UserUsage mapToUserUsage(Object[] result) {
        UUID userId = (UUID) result[0];
        BigDecimal totalCost = (BigDecimal) result[1];
        
        // Get user info
        User user = userRepository.findById(userId).orElse(null);
        String username = user != null ? user.getUsername() : "Unknown User";
        
        // Calculate additional metrics
        Long requestCount = aiApiUsageRepository.countRequestsByUserId(userId);
        Long totalTokens = aiApiUsageRepository.calculateTotalTokensByUserId(userId);
        Double averageDuration = aiApiUsageRepository.calculateAverageRequestDurationByUserId(userId);

        return ApiUsageAnalytics.UserUsage.builder()
                .userId(userId.toString())
                .username(username)
                .requestCount(requestCount)
                .totalCost(totalCost)
                .totalTokens(totalTokens)
                .averageDuration(averageDuration != null ? averageDuration : 0.0)
                .build();
    }

    private ApiUsageAnalytics.RequestTypeUsage mapToRequestTypeUsage(Object[] result) {
        AiApiUsage.RequestType requestType = (AiApiUsage.RequestType) result[0];
        Long requestCount = (Long) result[1];
        BigDecimal totalCost = (BigDecimal) result[2];
        Double averageDuration = (Double) result[3];

        // Calculate success rate
        Long successfulRequests = aiApiUsageRepository.countSuccessfulRequestsByUserId(null);
        double successRate = requestCount > 0 ? (double) successfulRequests / requestCount * 100 : 0.0;

        return ApiUsageAnalytics.RequestTypeUsage.builder()
                .requestType(requestType.toString())
                .requestCount(requestCount)
                .totalCost(totalCost)
                .totalTokens(0L) // Would need additional query
                .averageDuration(averageDuration != null ? averageDuration : 0.0)
                .successRate(successRate)
                .build();
    }

    private ApiUsageAnalytics.ModelUsage mapToModelUsage(Object[] result) {
        String modelName = (String) result[0];
        Long requestCount = (Long) result[1];
        BigDecimal totalCost = (BigDecimal) result[2];
        Double averageDuration = (Double) result[3];

        return ApiUsageAnalytics.ModelUsage.builder()
                .modelName(modelName)
                .requestCount(requestCount)
                .totalCost(totalCost)
                .totalTokens(0L) // Would need additional query
                .averageDuration(averageDuration != null ? averageDuration : 0.0)
                .successRate(0.0) // Would need additional query
                .build();
    }

    private ApiUsageAnalytics.DailyUsage mapToDailyUsage(Object[] result) {
        String date = result[0].toString();
        Long requestCount = (Long) result[1];
        BigDecimal totalCost = (BigDecimal) result[2];
        Long totalTokens = (Long) result[3];

        return ApiUsageAnalytics.DailyUsage.builder()
                .date(date)
                .requestCount(requestCount)
                .totalCost(totalCost)
                .totalTokens(totalTokens)
                .averageDuration(0.0) // Would need additional query
                .build();
    }

    private ApiUsageAnalytics.HourlyUsage mapToHourlyUsage(Object[] result) {
        Integer hour = (Integer) result[0];
        Long requestCount = (Long) result[1];
        BigDecimal totalCost = (BigDecimal) result[2];

        return ApiUsageAnalytics.HourlyUsage.builder()
                .hour(hour)
                .requestCount(requestCount)
                .totalCost(totalCost)
                .averageDuration(0.0) // Would need additional query
                .build();
    }
}

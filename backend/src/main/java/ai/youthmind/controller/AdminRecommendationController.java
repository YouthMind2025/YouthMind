package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.RecommendationRecalculateResponse;
import ai.youthmind.dto.RecommendationRuleDto;
import ai.youthmind.dto.RecommendationRuleUpdateRequest;
import ai.youthmind.service.RecommendationRuleService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Admin controller for recommendation engine management.
 * Requires ADMIN role for all endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/v1/admin/recommendation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Recommendation Management", description = "Administrative endpoints for recommendation engine management")
@SecurityRequirement(name = "bearer-jwt")
public class AdminRecommendationController {

    private final RecommendationRuleService recommendationRuleService;
    private final MeterRegistry meterRegistry;

    // Metrics
    private final Counter ruleRequests;
    private final Counter ruleUpdates;
    private final Counter ruleToggles;
    private final Counter recalculationRequests;
    private final Timer adminResponseLatency;

    public AdminRecommendationController(RecommendationRuleService recommendationRuleService,
                                       MeterRegistry meterRegistry) {
        this.recommendationRuleService = recommendationRuleService;
        this.meterRegistry = meterRegistry;

        // Initialize metrics
        this.ruleRequests = Counter.builder("recommendation.admin.rule_requests")
                .register(meterRegistry);
        this.ruleUpdates = Counter.builder("recommendation.admin.rule_updates")
                .register(meterRegistry);
        this.ruleToggles = Counter.builder("recommendation.admin.rule_toggles")
                .register(meterRegistry);
        this.recalculationRequests = Counter.builder("recommendation.admin.recalculation_requests")
                .register(meterRegistry);
        this.adminResponseLatency = Timer.builder("recommendation.admin.response_latency")
                .register(meterRegistry);
    }

    /**
     * Get all recommendation rules
     * GET /v1/admin/recommendation/rules
     */
    @GetMapping("/rules")
    @Operation(
        summary = "Get all recommendation rules",
        description = "Retrieve all recommendation mapping rules for the system"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rules retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin role required")
    })
    public ResponseEntity<ApiResponse<List<RecommendationRuleDto>>> getAllRules() {
        return Timer.Sample.start(meterRegistry)
                .stop(adminResponseLatency, () -> {
                    try {
                        log.info("Admin requesting all recommendation rules");
                        
                        List<RecommendationRuleDto> rules = recommendationRuleService.getAllRules();
                        
                        ruleRequests.increment();
                        
                        return ResponseEntity.ok(ApiResponse.<List<RecommendationRuleDto>>builder()
                                .success(true)
                                .data(rules)
                                .message("Rules retrieved successfully")
                                .build());
                        
                    } catch (Exception e) {
                        log.error("Failed to get recommendation rules: {}", e.getMessage(), e);
                        return ResponseEntity.internalServerError()
                                .body(ApiResponse.<List<RecommendationRuleDto>>builder()
                                        .success(false)
                                        .message("Failed to retrieve rules")
                                        .build());
                    }
                });
    }

    /**
     * Update a recommendation rule
     * PUT /v1/admin/recommendation/rules/{id}
     */
    @PutMapping("/rules/{id}")
    @Operation(
        summary = "Update recommendation rule",
        description = "Update an existing recommendation mapping rule"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rule updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<ApiResponse<RecommendationRuleDto>> updateRule(
            @Parameter(description = "Rule ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @Valid @RequestBody RecommendationRuleUpdateRequest request) {
        
        return Timer.Sample.start(meterRegistry)
                .stop(adminResponseLatency, () -> {
                    try {
                        log.info("Admin updating recommendation rule {} with request: {}", id, request);
                        
                        // Validate request
                        if (!request.isValid()) {
                            return ResponseEntity.badRequest()
                                    .body(ApiResponse.<RecommendationRuleDto>builder()
                                            .success(false)
                                            .message("Emotion percentages must sum to 100")
                                            .build());
                        }
                        
                        RecommendationRuleDto updatedRule = recommendationRuleService.updateRule(id, request);
                        
                        ruleUpdates.increment();
                        
                        return ResponseEntity.ok(ApiResponse.<RecommendationRuleDto>builder()
                                .success(true)
                                .data(updatedRule)
                                .message("Rule updated successfully")
                                .build());
                        
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid rule update request: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.<RecommendationRuleDto>builder()
                                        .success(false)
                                        .message(e.getMessage())
                                        .build());
                    } catch (Exception e) {
                        log.error("Failed to update recommendation rule: {}", e.getMessage(), e);
                        return ResponseEntity.internalServerError()
                                .body(ApiResponse.<RecommendationRuleDto>builder()
                                        .success(false)
                                        .message("Failed to update rule")
                                        .build());
                    }
                });
    }

    /**
     * Toggle rule active status
     * POST /v1/admin/recommendation/rules/{id}/toggle
     */
    @PostMapping("/rules/{id}/toggle")
    @Operation(
        summary = "Toggle rule active status",
        description = "Activate or deactivate a recommendation rule"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rule status toggled successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public ResponseEntity<ApiResponse<RecommendationRuleDto>> toggleRule(
            @Parameter(description = "Rule ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        
        return Timer.Sample.start(meterRegistry)
                .stop(adminResponseLatency, () -> {
                    try {
                        log.info("Admin toggling recommendation rule: {}", id);
                        
                        RecommendationRuleDto toggledRule = recommendationRuleService.toggleRule(id);
                        
                        ruleToggles.increment();
                        
                        return ResponseEntity.ok(ApiResponse.<RecommendationRuleDto>builder()
                                .success(true)
                                .data(toggledRule)
                                .message("Rule status toggled successfully")
                                .build());
                        
                    } catch (Exception e) {
                        log.error("Failed to toggle recommendation rule: {}", e.getMessage(), e);
                        return ResponseEntity.internalServerError()
                                .body(ApiResponse.<RecommendationRuleDto>builder()
                                        .success(false)
                                        .message("Failed to toggle rule")
                                        .build());
                    }
                });
    }

    /**
     * Recalculate all user R vectors
     * POST /v1/admin/recommendation/recalculate-all
     */
    @PostMapping("/recalculate-all")
    @Operation(
        summary = "Recalculate all user R vectors",
        description = "Force recalculation of all user R vectors based on current rules and interaction history"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recalculation completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - admin role required")
    })
    public ResponseEntity<ApiResponse<RecommendationRecalculateResponse>> recalculateAll(
            @RequestParam(value = "validate", defaultValue = "true") Boolean validate) {
        
        return Timer.Sample.start(meterRegistry)
                .stop(adminResponseLatency, () -> {
                    try {
                        log.info("Admin requesting recalculation of all user R vectors (validate: {})", validate);
                        
                        // Validate rules if requested
                        if (validate && !recommendationRuleService.validateAllRules()) {
                            return ResponseEntity.badRequest()
                                    .body(ApiResponse.<RecommendationRecalculateResponse>builder()
                                            .success(false)
                                            .message("Rule validation failed - percentages must sum to 100")
                                            .build());
                        }
                        
                        LocalDateTime startTime = LocalDateTime.now();
                        
                        // Perform recalculation
                        recommendationRuleService.recalculateAllUserRVectors();
                        
                        LocalDateTime endTime = LocalDateTime.now();
                        long processingTime = java.time.Duration.between(startTime, endTime).toMillis();
                        
                        RecommendationRecalculateResponse response = new RecommendationRecalculateResponse(
                                true,
                                startTime,
                                0, // TODO: Get actual user count
                                0, // TODO: Get actual updated count
                                processingTime,
                                null, // TODO: Get actual changes summary
                                null
                        );
                        
                        recalculationRequests.increment();
                        
                        return ResponseEntity.ok(ApiResponse.<RecommendationRecalculateResponse>builder()
                                .success(true)
                                .data(response)
                                .message("Recalculation completed successfully")
                                .build());
                        
                    } catch (Exception e) {
                        log.error("Failed to recalculate user R vectors: {}", e.getMessage(), e);
                        
                        RecommendationRecalculateResponse errorResponse = new RecommendationRecalculateResponse(
                                false,
                                LocalDateTime.now(),
                                0,
                                0,
                                0L,
                                null,
                                e.getMessage()
                        );
                        
                        return ResponseEntity.internalServerError()
                                .body(ApiResponse.<RecommendationRecalculateResponse>builder()
                                        .success(false)
                                        .data(errorResponse)
                                        .message("Recalculation failed")
                                        .build());
                    }
                });
    }
}

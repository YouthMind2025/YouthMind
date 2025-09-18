package ai.youthmind.controller;

import ai.youthmind.dto.*;
import ai.youthmind.entity.User;
import ai.youthmind.service.InsightsService;
import ai.youthmind.service.RedisService;
import ai.youthmind.service.UserService;
import ai.youthmind.util.AuthenticationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping
@Tag(name = "User Insights")
@SecurityRequirement(name = "bearer-jwt")
public class InsightsController {

    private static final String INSIGHTS_KEY_PREFIX = "youthmind:insights:";
    private static final String TRAITS_KEY_PREFIX = "youthmind:traits:";

    @Autowired
    private UserService userService;

    @Autowired
    private InsightsService insightsService;

    @Autowired
    private RedisService redisService;

    @PostMapping("/api/insights/profile/{userId}")
    @Operation(
            summary = "Full Profile Insights Update",
            description = "Update full user activity summary"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile insights updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserActivitySummaryDto>> updateProfileInsights(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
            @PathVariable UUID userId,
            @Valid @RequestBody UserActivitySummaryDto request
    ) {
        try {
            Optional<User> userOpt = userService.getUserById(userId);

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(new UserActivitySummaryDto(), 400));
            }

            String cacheKey = INSIGHTS_KEY_PREFIX + userId + request.getTimeRange();
            redisService.setValue(cacheKey, request);

            return ResponseEntity.ok(ApiResponse.success(request));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(new UserActivitySummaryDto(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(new UserActivitySummaryDto(), 500));
        }
    }

    @GetMapping("/v1/profile/{userId}")
    @Operation(
            summary = "Get User Profile Insights",
            description = "Retrieve user activity summary and insights for a specific user"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile insights retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserActivitySummaryDto>> getProfileInsights(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
            @PathVariable UUID userId
    ) {
        try {
            // Verify user exists
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(new UserActivitySummaryDto(), 404));
            }

            // Check cache first
            String cacheKey = INSIGHTS_KEY_PREFIX + userId;
            if (redisService.hasKey(cacheKey)) {
                UserActivitySummaryDto cachedInsights = (UserActivitySummaryDto) redisService.getValue(cacheKey);
                return ResponseEntity.ok(ApiResponse.success(cachedInsights));
            }

            // Generate insights for the last 30 days if not cached
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(30);
            Pair<LocalDateTime, LocalDateTime> timeRange = Pair.of(startDate, endDate);
            
            UserActivitySummaryDto insights = insightsService.getUserActivitySummary(userId, timeRange);
            
            // Cache the insights for 1 hour
            redisService.setValue(cacheKey, insights, 3600);

            return ResponseEntity.ok(ApiResponse.success(insights));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(new UserActivitySummaryDto(), 500));
        }
    }

    @PostMapping("/v1/compute/insights")
    @Operation(
            summary = "Compute Insights",
            description = "Get full user activity summary"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile insights computed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserActivitySummaryDto>> updateProfileInsights(
            @Valid @RequestBody InsightsRequest request
    ) {
        try {
            UUID userId = AuthenticationUtils.getCurrentUserId();
            Optional<User> userOpt = userService.getUserById(userId);

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(new UserActivitySummaryDto(), 400));
            }

            String cacheKey = INSIGHTS_KEY_PREFIX + userId + request;
            if (redisService.hasKey(cacheKey)) {
                UserActivitySummaryDto profileInsights = (UserActivitySummaryDto) redisService.getValue(cacheKey);
                return ResponseEntity.ok(ApiResponse.success(profileInsights));
            }

            Pair<LocalDateTime, LocalDateTime> timeRange = Pair.of(request.getStart(), request.getEnd());
            UserActivitySummaryDto profileInsights = insightsService.getUserActivitySummary(userId, timeRange);

            return ResponseEntity.ok(ApiResponse.success(profileInsights));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(new UserActivitySummaryDto(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(new UserActivitySummaryDto(), 500));
        }
    }

    @GetMapping("/api/insights/emotions/{userId}")
    @Operation(
            summary = "Emotional analysis over time",
            description = "Get user emotional analysis between requested time range"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Emotional analysis get successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<EmotionalAnalysisResponse>> getEmotionalAnalysis(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
            @PathVariable UUID userId,
            @Valid @RequestBody InsightsRequest request
    ) {
        try {
            EmotionalAnalysisResponse response = new EmotionalAnalysisResponse();
            response.setStart(request.getStart());
            response.setEnd(request.getEnd());

            Optional<User> userOpt = userService.getUserById(userId);

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(response, 400));
            }

            Pair<LocalDateTime, LocalDateTime> timeRange = Pair.of(request.getStart(), request.getEnd());
            List<String> emotionalTrends = insightsService.calculateEmotionalTrends(userId, timeRange);
            Map<String, Integer> postEmotionDistribution = insightsService.calculatePostEmotionDistribution(userId, timeRange);

            response.setEmotionalTrends(emotionalTrends);
            response.setPostEmotionDistribution(postEmotionDistribution);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(new EmotionalAnalysisResponse(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(new EmotionalAnalysisResponse(), 500));
        }
    }

    @GetMapping("/api/insights/personality/{userId}")
    @Operation(
            summary = "Personality traits",
            description = "Get user personality traits between requested time range"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Personality traits get successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<PersonalityTraitsResponse>> getPersonalityTraits(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
            @PathVariable UUID userId,
            @Valid @RequestBody InsightsRequest request
    ) {
        try {
            PersonalityTraitsResponse response = new PersonalityTraitsResponse();
            response.setStart(request.getStart());
            response.setEnd(request.getEnd());

            Optional<User> userOpt = userService.getUserById(userId);

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(response, 400));
            }

            Pair<LocalDateTime, LocalDateTime> timeRange = Pair.of(request.getStart(), request.getEnd());
            List<String> mostUsedWords = insightsService.calculateMostUsedWords(userId, timeRange);
            List<String> topicPreferences = insightsService.calculateTopicPreferences(userId, timeRange);
            List<String> coveredTopics = insightsService.calculateCoveredTopics(userId, timeRange);
            List<String> mostEngagedTopic = insightsService.calculateMostEngagedTopics(userId, timeRange);

            response.setPostMostUsedWords(mostUsedWords);
            response.setTopicPreferences(topicPreferences);
            response.setCoveredTopics(coveredTopics);
            response.setMostEngagedTopic(mostEngagedTopic);

            List<PersonalityTraitsResponse> traits = new ArrayList<>();
            String cacheKey = TRAITS_KEY_PREFIX + userId;
            if (redisService.hasKey(cacheKey)) {
                traits = (List<PersonalityTraitsResponse>) redisService.getValue(cacheKey);
            }
            traits.add(response);
            redisService.setValue(cacheKey, traits);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(new PersonalityTraitsResponse(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(new PersonalityTraitsResponse(), 500));
        }
    }

    @GetMapping("/v1/traits/{userId}/history")
    @Operation(
            summary = "Personality traits",
            description = "Get user personality traits between requested time range"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Personality traits get successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<List<PersonalityTraitsResponse>>> getTraitsHistory(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
            @PathVariable UUID userId
    ) {
        try {
            Optional<User> userOpt = userService.getUserById(userId);

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(new ArrayList<>(), 400));
            }

            List<PersonalityTraitsResponse> traits = new ArrayList<>();
            String cacheKey = TRAITS_KEY_PREFIX + userId;
            if (redisService.hasKey(cacheKey)) {
                traits = (List<PersonalityTraitsResponse>) redisService.getValue(cacheKey);
                return ResponseEntity.ok(ApiResponse.success(traits));
            }

            return ResponseEntity.ok(ApiResponse.success(new ArrayList<>()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(new ArrayList<>(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(new ArrayList<>(), 500));
        }
    }

}

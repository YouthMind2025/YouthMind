package ai.youthmind.controller;

import ai.youthmind.dto.*;
import ai.youthmind.entity.User;
import ai.youthmind.service.InsightsService;
import ai.youthmind.service.UserService;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/insights")
@Tag(name = "User Insights")
@SecurityRequirement(name = "bearer-jwt")
public class InsightsController {

    @Autowired
    private UserService userService;

    @Autowired
    private InsightsService insightsService;

    @GetMapping("/profile/{userId}")
    @Operation(
            summary = "Full Profile Insights",
            description = "Get full user activity summary between requested time range"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile insights get successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<UserActivitySummaryDto>> getProfileInsights(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
            @PathVariable UUID userId,
            @Valid @RequestBody InsightsRequest request
    ) {
        try {
            Optional<User> userOpt = userService.getUserById(userId);

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(new UserActivitySummaryDto(), 400));
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

    @GetMapping("/emotions/{userId}")
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

    @GetMapping("/personality/{userId}")
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

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(new PersonalityTraitsResponse(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(new PersonalityTraitsResponse(), 500));
        }
    }

}

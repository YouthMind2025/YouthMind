
package ai.youthmind.analytics.controller;

import ai.youthmind.analytics.dto.BehaviorSummaryResponse;
import ai.youthmind.analytics.dto.InteractionEventRequest;
import ai.youthmind.analytics.dto.ViewEventRequest;
import ai.youthmind.analytics.service.BehaviorAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/behavior")
@Tag(name = "Behavior", description = "User behavior tracking")
public class BehaviorAnalyticsController {

    private final BehaviorAnalyticsService service;

    public BehaviorAnalyticsController(BehaviorAnalyticsService service) {
        this.service = service;
    }

    @PostMapping("/view")
    @Operation(summary = "Track view event with optional duration and metadata")
    public ResponseEntity<Void> view(@Valid @RequestBody ViewEventRequest req) {
        service.trackView(req);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/interaction")
    @Operation(summary = "Track user interaction: LIKE, COMMENT, SAVE, SHARE")
    public ResponseEntity<Void> interaction(@Valid @RequestBody InteractionEventRequest req) {
        service.logInteraction(req);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/summary/{userId}")
    @Operation(summary = "Behavior summary for user")
    public ResponseEntity<BehaviorSummaryResponse> summary(@PathVariable UUID userId,
                                                           @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(service.summarize(userId, Math.max(1, Math.min(days, 365))));
    }
}

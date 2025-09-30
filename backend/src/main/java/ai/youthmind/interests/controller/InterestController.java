
package ai.youthmind.interests.controller;

import ai.youthmind.interests.entity.UserInterest;
import ai.youthmind.interests.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interests")
@Tag(name = "Interests", description = "User interest detection")
public class InterestController {

    private final InterestService service;

    public InterestController(InterestService service) {
        this.service = service;
    }

    @PostMapping("/analyze")
    @Operation(summary = "Analyze text and update user interests")
    public ResponseEntity<List<UserInterest>> analyze(@RequestParam UUID userId,
                                                      @RequestBody String text) {
        return ResponseEntity.ok(service.analyzeAndUpdate(userId, text));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get top interests for user")
    public ResponseEntity<List<UserInterest>> top(@PathVariable UUID userId,
                                                  @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(service.top(userId, Math.max(1, Math.min(limit, 100))));
    }
}

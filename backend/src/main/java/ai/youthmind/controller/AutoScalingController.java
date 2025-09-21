package ai.youthmind.controller;

import ai.youthmind.service.AutoScalingService;
import ai.youthmind.util.AuthenticationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for auto-scaling and resource management
 */
@RestController
@RequestMapping("/v1/scaling")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auto Scaling", description = "Auto-scaling and resource management APIs")
public class AutoScalingController {

    private final AutoScalingService autoScalingService;
    private final AuthenticationUtils authenticationUtils;

    @GetMapping("/status")
    @Operation(summary = "Get scaling status", description = "Get current auto-scaling status and metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AutoScalingService.ScalingStatus> getScalingStatus() {
        
        AutoScalingService.ScalingStatus status = autoScalingService.getScalingStatus();
        
        return ResponseEntity.ok(status);
    }

    @PostMapping("/manual-scale/{targetLevel}")
    @Operation(summary = "Manual scaling", description = "Manually trigger scaling to target level")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<Boolean>> manualScale(@PathVariable int targetLevel) {
        
        log.info("Manual scaling requested to level {}", targetLevel);
        
        CompletableFuture<Boolean> future = autoScalingService.manualScale(targetLevel);
        
        return ResponseEntity.accepted().body(future);
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get scaling metrics", description = "Get detailed scaling metrics and history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> getScalingMetrics() {
        
        AutoScalingService.ScalingStatus status = autoScalingService.getScalingStatus();
        
        return ResponseEntity.ok(status);
    }
}

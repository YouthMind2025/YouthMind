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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    public ResponseEntity<Map<String, Object>> manualScale(@PathVariable int targetLevel) {
        log.info("Manual scaling requested to level {}", targetLevel);
        
        // Controller-level validation (consistent with service validation)
        if (targetLevel < 1 || targetLevel > 10) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Target scale level must be between 1 and 10");
            error.put("targetLevel", targetLevel);
            error.put("validRange", "1-10");
            return ResponseEntity.badRequest().body(error);
        }
        
        try {
            CompletableFuture<Boolean> future = autoScalingService.manualScale(targetLevel);
            
            // Add timeout to prevent indefinite blocking (safety measure)
            Boolean result;
            try {
                result = future.get(10, TimeUnit.SECONDS);  // 10 second timeout
            } catch (TimeoutException e) {
                log.error("Manual scaling operation timed out after 10 seconds for targetLevel: {}", targetLevel);
                Map<String, Object> error = new HashMap<>();
                error.put("message", "Scaling operation timed out. Please check system status.");
                error.put("targetLevel", targetLevel);
                error.put("timeout", 10);
                return ResponseEntity.status(org.springframework.http.HttpStatus.REQUEST_TIMEOUT).body(error);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result);
            response.put("targetLevel", targetLevel);
            response.put("message", result ? 
                "Scaling initiated successfully" : "Scaling failed");
            
            return ResponseEntity.accepted().body(response);
        } catch (IllegalArgumentException e) {
            // Fallback if service throws IllegalArgumentException
            Map<String, Object> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error in manual scaling: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("message", "Error initiating scaling: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get scaling metrics", description = "Get detailed scaling metrics and history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> getScalingMetrics() {
        
        AutoScalingService.ScalingStatus status = autoScalingService.getScalingStatus();
        
        return ResponseEntity.ok(status);
    }
}

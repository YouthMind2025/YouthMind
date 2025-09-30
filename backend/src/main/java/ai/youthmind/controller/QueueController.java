package ai.youthmind.controller;

import ai.youthmind.dto.QueueRequest;
import ai.youthmind.dto.QueueResponse;
import ai.youthmind.service.RequestQueueService;
import ai.youthmind.util.AuthenticationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for request queuing and load balancing
 */
@RestController
@RequestMapping("/v1/queue")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Request Queue", description = "Request queuing and load balancing APIs")
public class QueueController {

    private final RequestQueueService requestQueueService;
    private final AuthenticationUtils authenticationUtils;

    @PostMapping("/submit")
    @Operation(summary = "Submit request to queue", description = "Submit a request to the processing queue")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<QueueResponse>> submitRequest(
            @Valid @RequestBody QueueRequest request) {
        
        log.info("Submitting request to queue: operation='{}', priority='{}'", 
            request.getOperation(), request.getPriority());
        
        // Convert to queue item
        RequestQueueService.RequestQueueItem queueItem = RequestQueueService.RequestQueueItem.builder()
            .userId(authenticationUtils.getCurrentUserId().toString())
            .operation(request.getOperation())
            .data(request.getData())
            .priority(RequestQueueService.RequestPriority.valueOf(request.getPriority()))
            .userTier(RequestQueueService.UserTier.valueOf(request.getUserTier()))
            .retryCount(0)
            .build();
        
        CompletableFuture<QueueResponse> future = requestQueueService.queueRequest(queueItem)
            .thenApply(requestId -> QueueResponse.builder()
                .requestId(requestId)
                .status("QUEUED")
                .message("Request submitted to queue successfully")
                .build());
        
        return ResponseEntity.accepted().body(future);
    }

    @GetMapping("/status/{requestId}")
    @Operation(summary = "Get request status", description = "Get the current status of a queued request")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RequestQueueService.RequestQueueItem> getRequestStatus(
            @PathVariable String requestId) {
        
        RequestQueueService.RequestQueueItem request = requestQueueService.getRequest(requestId);
        
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(request);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get queue statistics", description = "Get comprehensive queue statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RequestQueueService.QueueStatistics> getQueueStatistics() {
        
        RequestQueueService.QueueStatistics statistics = requestQueueService.getQueueStatistics();
        
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/processing-times")
    @Operation(summary = "Get processing time statistics", description = "Get detailed processing time metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RequestQueueService.ProcessingTimeStats> getProcessingTimeStats() {
        
        RequestQueueService.ProcessingTimeStats stats = requestQueueService.getProcessingTimeStats();
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/load-balance")
    @Operation(summary = "Load balance request", description = "Submit request with load balancing")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<QueueResponse>> loadBalanceRequest(
            @Valid @RequestBody QueueRequest request) {
        
        log.info("Load balancing request: operation='{}'", request.getOperation());
        
        // Convert to queue item
        RequestQueueService.RequestQueueItem queueItem = RequestQueueService.RequestQueueItem.builder()
            .userId(authenticationUtils.getCurrentUserId().toString())
            .operation(request.getOperation())
            .data(request.getData())
            .priority(RequestQueueService.RequestPriority.valueOf(request.getPriority()))
            .userTier(RequestQueueService.UserTier.valueOf(request.getUserTier()))
            .retryCount(0)
            .build();
        
        CompletableFuture<QueueResponse> future = requestQueueService.loadBalanceRequest(queueItem)
            .thenApply(requestId -> QueueResponse.builder()
                .requestId(requestId)
                .status("LOAD_BALANCED")
                .message("Request load balanced successfully")
                .build());
        
        return ResponseEntity.accepted().body(future);
    }
}

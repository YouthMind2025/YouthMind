package ai.youthmind.controller;

import ai.youthmind.dto.BatchRequest;
import ai.youthmind.dto.BatchResponse;
import ai.youthmind.service.BatchProcessingService;
import ai.youthmind.util.AuthenticationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for batch API operations
 */
@RestController
@RequestMapping("/v1/batch")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Batch Operations", description = "Batch processing APIs for high-volume operations")
public class BatchController {

    private final BatchProcessingService batchProcessingService;
    private final AuthenticationUtils authenticationUtils;

    @PostMapping("/process")
    @Operation(summary = "Process batch operations", description = "Process multiple operations in a single request")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<BatchResponse<?>>> processBatch(
            @Valid @RequestBody BatchRequest<?> request) {
        
        log.info("Processing batch request with ID: {} containing {} operations", 
            request.getBatchId(), request.getOperations().size());
        
        CompletableFuture<BatchResponse<?>> future = batchProcessingService.processBatchAsync(request).thenApply(response -> (BatchResponse<?>) response);
        
        return ResponseEntity.accepted().body(future);
    }

    @GetMapping("/status/{batchId}")
    @Operation(summary = "Get batch processing status", description = "Get the current status of a batch operation")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BatchResponse<?>> getBatchStatus(@PathVariable String batchId) {
        
        BatchResponse<?> status = batchProcessingService.getBatchStatus(batchId);
        
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/status")
    @Operation(summary = "Get all batch statuses", description = "Get status of all active batch operations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, BatchResponse<?>>> getAllBatchStatuses() {
        
        Map<String, BatchResponse<?>> statuses = batchProcessingService.getAllBatchStatuses();
        
        return ResponseEntity.ok(statuses);
    }

    @PostMapping("/posts/create")
    @Operation(summary = "Batch create posts", description = "Create multiple posts in a single batch operation")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<BatchResponse<?>> batchCreatePosts(
            @Valid @RequestBody BatchRequest<Object> request) {
        
        // Set operation type for all operations
        request.getOperations().forEach(op -> op.setOperationType("CREATE_POST"));
        
        log.info("Batch creating {} posts", request.getOperations().size());
        
        try {
            // Process synchronously to maintain security context
            BatchResponse<?> response = batchProcessingService.processBatchAsync(request).get();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing batch request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/posts/update")
    @Operation(summary = "Batch update posts", description = "Update multiple posts in a single batch operation")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<BatchResponse<?>>> batchUpdatePosts(
            @Valid @RequestBody BatchRequest<Object> request) {
        
        // Set operation type for all operations
        request.getOperations().forEach(op -> op.setOperationType("UPDATE_POST"));
        
        log.info("Batch updating {} posts", request.getOperations().size());
        
        CompletableFuture<BatchResponse<?>> future = batchProcessingService.processBatchAsync(request).thenApply(response -> (BatchResponse<?>) response);
        
        return ResponseEntity.accepted().body(future);
    }

    @PostMapping("/posts/delete")
    @Operation(summary = "Batch delete posts", description = "Delete multiple posts in a single batch operation")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<BatchResponse<?>>> batchDeletePosts(
            @Valid @RequestBody BatchRequest<Object> request) {
        
        // Set operation type for all operations
        request.getOperations().forEach(op -> op.setOperationType("DELETE_POST"));
        
        log.info("Batch deleting {} posts", request.getOperations().size());
        
        CompletableFuture<BatchResponse<?>> future = batchProcessingService.processBatchAsync(request).thenApply(response -> (BatchResponse<?>) response);
        
        return ResponseEntity.accepted().body(future);
    }

    @PostMapping("/moderation/process")
    @Operation(summary = "Batch moderation", description = "Process moderation for multiple posts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<BatchResponse<?>>> batchModeration(
            @Valid @RequestBody BatchRequest<Object> request) {
        
        // Set operation type for all operations
        request.getOperations().forEach(op -> op.setOperationType("BATCH_MODERATION"));
        
        log.info("Batch moderation for {} operations", request.getOperations().size());
        
        CompletableFuture<BatchResponse<?>> future = batchProcessingService.processBatchAsync(request).thenApply(response -> (BatchResponse<?>) response);
        
        return ResponseEntity.accepted().body(future);
    }
}

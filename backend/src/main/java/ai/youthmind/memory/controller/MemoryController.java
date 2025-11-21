
package ai.youthmind.memory.controller;

import ai.youthmind.memory.dto.ContextResponse;
import ai.youthmind.memory.dto.MemoryAppendRequest;
import ai.youthmind.memory.dto.PreferenceSetRequest;
import ai.youthmind.memory.entity.AiMemory;
import ai.youthmind.memory.entity.UserPreference;
import ai.youthmind.memory.service.MemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/memory")
@Tag(name = "Memory", description = "AI memory & preferences")
public class MemoryController {

    private final MemoryService service;

    public MemoryController(MemoryService service) {
        this.service = service;
    }

    @PostMapping("/append")
    @Operation(summary = "Append a memory item")
    public ResponseEntity<AiMemory> append(@Valid @RequestBody MemoryAppendRequest req) {
        return ResponseEntity.ok(service.append(req.getUserId(), req.getLabel(), req.getContent()));
    }

    @PostMapping("/preference")
    @Operation(summary = "Set a user preference (key/value)")
    public ResponseEntity<UserPreference> setPref(@Valid @RequestBody PreferenceSetRequest req) {
        return ResponseEntity.ok(service.setPref(req.getUserId(), req.getKey(), req.getValue()));
    }

    @GetMapping("/context/{userId}")
    @Operation(summary = "Get conversation context (recent memories + preferences)")
    public ResponseEntity<ContextResponse> context(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.context(userId));
    }
}

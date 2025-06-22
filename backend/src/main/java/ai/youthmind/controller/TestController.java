package ai.youthmind.controller;

import ai.youthmind.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Test Endpoints", description = "Test and health check endpoints for development and monitoring")
public class TestController {

    @Autowired
    private RedisService redisService;

    @GetMapping("/hello")
    @Operation(
        summary = "Hello endpoint",
        description = "Simple greeting endpoint to verify the API is running"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully returned greeting message")
    })
    public String hello() {
        return "Hello, YouthMind!";
    }

    @GetMapping("/health")
    @Operation(
        summary = "Health check",
        description = "Health check endpoint to verify the backend service is running"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Backend is healthy and running")
    })
    public String health() {
        return "YouthMind Backend is running successfully!";
    }

    @GetMapping("/redis")
    @Operation(
        summary = "Redis test",
        description = "Test Redis connectivity by setting and retrieving a test value"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Redis test completed successfully"),
        @ApiResponse(responseCode = "500", description = "Redis connection failed or test failed")
    })
    public String testRedis() {
        String key = "youthmind:test:greeting";
        String value = "Hello from Redis!";
        try {
            redisService.setValue(key, value, 1, TimeUnit.MINUTES);
            String retrievedValue = (String) redisService.getValue(key);
            if (value.equals(retrievedValue)) {
                return "OK: Successfully set and retrieved a value from Redis. Got: '" + retrievedValue + "'";
            } else {
                return "FAIL: Retrieved a different value from Redis. Expected: '" + value + "', Got: '" + retrievedValue + "'";
            }
        } catch (Exception e) {
            return "FAIL: An exception occurred while interacting with Redis: " + e.getMessage();
        }
    }
} 
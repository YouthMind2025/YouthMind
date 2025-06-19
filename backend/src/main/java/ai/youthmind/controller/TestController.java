package ai.youthmind.controller;

import ai.youthmind.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private RedisService redisService;

    @GetMapping("/hello")
    public String hello() {
        return "Hello, YouthMind!";
    }

    @GetMapping("/health")
    public String health() {
        return "YouthMind Backend is running successfully!";
    }

    @GetMapping("/redis")
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
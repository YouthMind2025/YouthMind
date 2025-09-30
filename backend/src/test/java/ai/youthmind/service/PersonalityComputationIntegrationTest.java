package ai.youthmind.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration test to verify personality computation services compile and can be instantiated
 */
@SpringBootTest
@ActiveProfiles("test")
class PersonalityComputationIntegrationTest {
    
    @Test
    void testServicesCanBeInstantiated() {
        // This test verifies that all the personality computation services can be instantiated
        // and their dependencies are properly configured
        
        // Test that the services exist and can be loaded
        assertDoesNotThrow(() -> {
            // This will fail if there are any compilation or dependency injection issues
            Class.forName("ai.youthmind.service.PersonalityComputationService");
            Class.forName("ai.youthmind.service.ActivePersonalityCalculator");
            Class.forName("ai.youthmind.service.SubconsciousPersonalityCalculator");
            Class.forName("ai.youthmind.service.PersonalityFusionService");
            Class.forName("ai.youthmind.service.PersonalityScheduler");
            Class.forName("ai.youthmind.controller.PersonalityComputationController");
        });
    }
    
    @Test
    void testServiceConstants() {
        // Test that the algorithm constants are properly defined
        assertEquals(0.7, PersonalityComputationService.ACTIVE_PERSONALITY_WEIGHT);
        assertEquals(0.3, PersonalityComputationService.SUBCONSCIOUS_PERSONALITY_WEIGHT);
        assertEquals(10, PersonalityComputationService.TOP_K_RESONANCE);
        assertEquals(24, PersonalityComputationService.PERSONALITY_COMPUTATION_INTERVAL_HOURS);
    }
}

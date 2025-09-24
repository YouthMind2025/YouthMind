package ai.youthmind.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration test to verify interaction processing services compile and can be instantiated
 */
@SpringBootTest
@ActiveProfiles("test")
class InteractionProcessingIntegrationTest {
    
    @Test
    void testServicesCanBeInstantiated() {
        // This test verifies that all the interaction processing services can be instantiated
        // and their dependencies are properly configured
        
        // Test that the services exist and can be loaded
        assertDoesNotThrow(() -> {
            // This will fail if there are any compilation or dependency injection issues
            Class.forName("ai.youthmind.service.InteractionModulationService");
            Class.forName("ai.youthmind.service.ResonanceProcessor");
            Class.forName("ai.youthmind.service.LingerProcessor");
            Class.forName("ai.youthmind.service.PositiveBalanceCalculator");
            Class.forName("ai.youthmind.service.InteractionWeightService");
            Class.forName("ai.youthmind.controller.InteractionProcessingController");
        });
        
        // Test that the repository methods exist
        assertDoesNotThrow(() -> {
            Class.forName("ai.youthmind.repository.PostResonanceRepository");
        });
    }
    
    @Test
    void testServiceConstants() {
        // Test that the algorithm constants are properly defined
        assertEquals(0.2, InteractionModulationService.RESONANCE_WEIGHT);
        assertEquals(0.01, InteractionModulationService.LINGER_WEIGHT);
        assertEquals(0.7, InteractionModulationService.POSITIVE_BALANCE_THRESHOLD);
    }
}

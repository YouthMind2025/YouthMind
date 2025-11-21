package ai.youthmind.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration test to verify enhanced AI analysis services compile and can be instantiated
 */
@SpringBootTest
@ActiveProfiles("test")
class EnhancedAIAnalysisIntegrationTest {
    
    @Test
    void testServicesCanBeInstantiated() {
        // This test verifies that all the enhanced AI analysis services can be instantiated
        // and their dependencies are properly configured
        
        // Test that the services exist and can be loaded
        assertDoesNotThrow(() -> {
            // This will fail if there are any compilation or dependency injection issues
            Class.forName("ai.youthmind.service.PsychologicalVectorGenerator");
            Class.forName("ai.youthmind.service.BuddhistFactorAnalyzer");
            Class.forName("ai.youthmind.service.IntensityPredictor");
            Class.forName("ai.youthmind.service.StateVectorProcessor");
            Class.forName("ai.youthmind.controller.EnhancedAIAnalysisController");
        });
    }
    
    @Test
    void testServiceConstants() {
        // Test that the algorithm constants are properly defined
        assertEquals(43, PsychologicalVectorGenerator.PSYCHOLOGICAL_VECTOR_DIMENSIONS);
        assertEquals(0.7, IntensityPredictor.HIGH_INTENSITY_THRESHOLD);
        assertEquals(0.4, IntensityPredictor.MODERATE_INTENSITY_THRESHOLD);
        assertEquals(0.2, IntensityPredictor.LOW_INTENSITY_THRESHOLD);
        assertEquals(43, StateVectorProcessor.VECTOR_DIMENSIONS);
    }
}

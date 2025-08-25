package ai.youthmind.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

@DisplayName("PostViewStats Tests")
public class PostViewStatsTest {
    
    private final UUID testPostId = UUID.randomUUID();
    
    @Test
    @DisplayName("Should create PostViewStats with all fields")
    void shouldCreatePostViewStatsWithAllFields() {
        PostViewStats stats = new PostViewStats(testPostId, 100L, 25L, 25);
        
        assertNotNull(stats);
        assertEquals(testPostId, stats.getPostId());
        assertEquals(100L, stats.getTotalViews());
        assertEquals(25L, stats.getRecentViews());
        assertEquals(25, stats.getUniqueViewers());
    }
    
    @Test
    @DisplayName("Should calculate view growth rate correctly")
    void shouldCalculateViewGrowthRateCorrectly() {
        // 25 recent views out of 100 total = 25%
        PostViewStats stats = new PostViewStats(testPostId, 100L, 25L, 25);
        assertEquals(25.0, stats.getViewGrowthRate());
        
        // 10 recent views out of 50 total = 20%
        PostViewStats stats2 = new PostViewStats(testPostId, 50L, 10L, 10);
        assertEquals(20.0, stats2.getViewGrowthRate());
        
        // 5 recent views out of 20 total = 25%
        PostViewStats stats3 = new PostViewStats(testPostId, 20L, 5L, 5);
        assertEquals(25.0, stats3.getViewGrowthRate());
    }
    
    @Test
    @DisplayName("Should handle zero total views in growth rate calculation")
    void shouldHandleZeroTotalViewsInGrowthRateCalculation() {
        PostViewStats stats = new PostViewStats(testPostId, 0L, 10L, 10);
        assertEquals(0.0, stats.getViewGrowthRate());
    }
    
    @Test
    @DisplayName("Should handle zero recent views in growth rate calculation")
    void shouldHandleZeroRecentViewsInGrowthRateCalculation() {
        PostViewStats stats = new PostViewStats(testPostId, 100L, 0L, 0);
        assertEquals(0.0, stats.getViewGrowthRate());
    }
    
    @Test
    @DisplayName("Should identify high engagement correctly")
    void shouldIdentifyHighEngagementCorrectly() {
        // 15% engagement (high)
        PostViewStats highEngagement = new PostViewStats(testPostId, 100L, 15L, 15);
        assertTrue(highEngagement.hasHighEngagement());
        
        // 25% engagement (very high)
        PostViewStats veryHighEngagement = new PostViewStats(testPostId, 100L, 25L, 25);
        assertTrue(veryHighEngagement.hasHighEngagement());
        
        // 5% engagement (not high)
        PostViewStats lowEngagement = new PostViewStats(testPostId, 100L, 5L, 5);
        assertFalse(lowEngagement.hasHighEngagement());
        
        // 0% engagement (not high)
        PostViewStats noEngagement = new PostViewStats(testPostId, 100L, 0L, 0);
        assertFalse(noEngagement.hasHighEngagement());
    }
    
    @Test
    @DisplayName("Should return correct engagement level descriptions")
    void shouldReturnCorrectEngagementLevelDescriptions() {
        // Very High: > 20%
        PostViewStats veryHigh = new PostViewStats(testPostId, 100L, 25L, 25);
        assertEquals("Very High", veryHigh.getEngagementLevel());
        
        // High: 10-20%
        PostViewStats high = new PostViewStats(testPostId, 100L, 15L, 15);
        assertEquals("High", high.getEngagementLevel());
        
        // Moderate: 5-10%
        PostViewStats moderate = new PostViewStats(testPostId, 100L, 7L, 7);
        assertEquals("Moderate", moderate.getEngagementLevel());
        
        // Low: 1-5%
        PostViewStats low = new PostViewStats(testPostId, 100L, 3L, 3);
        assertEquals("Low", low.getEngagementLevel());
        
        // Very Low: 0-1%
        PostViewStats veryLow = new PostViewStats(testPostId, 100L, 0L, 0);
        assertEquals("Very Low", veryLow.getEngagementLevel());
        
        // Edge case: exactly 1%
        PostViewStats exactlyOne = new PostViewStats(testPostId, 100L, 1L, 1);
        assertEquals("Low", exactlyOne.getEngagementLevel());
        
        // Edge case: exactly 5%
        PostViewStats exactlyFive = new PostViewStats(testPostId, 100L, 5L, 5);
        assertEquals("Moderate", exactlyFive.getEngagementLevel());
        
        // Edge case: exactly 10%
        PostViewStats exactlyTen = new PostViewStats(testPostId, 100L, 10L, 10);
        assertEquals("High", exactlyTen.getEngagementLevel());
        
        // Edge case: exactly 20%
        PostViewStats exactlyTwenty = new PostViewStats(testPostId, 100L, 20L, 20);
        assertEquals("Very High", exactlyTwenty.getEngagementLevel());
    }
    
    @Test
    @DisplayName("Should handle edge cases in engagement calculations")
    void shouldHandleEdgeCasesInEngagementCalculations() {
        // Very small numbers
        PostViewStats smallNumbers = new PostViewStats(testPostId, 1L, 1L, 1);
        assertEquals(100.0, smallNumbers.getViewGrowthRate());
        assertTrue(smallNumbers.hasHighEngagement());
        assertEquals("Very High", smallNumbers.getEngagementLevel());
        
        // Large numbers
        PostViewStats largeNumbers = new PostViewStats(testPostId, 1000000L, 50000L, 50000);
        assertEquals(5.0, largeNumbers.getViewGrowthRate());
        assertFalse(largeNumbers.hasHighEngagement());
        assertEquals("Moderate", largeNumbers.getEngagementLevel());
        
        // Equal numbers
        PostViewStats equalNumbers = new PostViewStats(testPostId, 100L, 100L, 100);
        assertEquals(100.0, equalNumbers.getViewGrowthRate());
        assertTrue(equalNumbers.hasHighEngagement());
        assertEquals("Very High", equalNumbers.getEngagementLevel());
    }
    
    @Test
    @DisplayName("Should handle decimal precision in growth rate")
    void shouldHandleDecimalPrecisionInGrowthRate() {
        // 33.33...% (1/3)
        PostViewStats oneThird = new PostViewStats(testPostId, 3L, 1L, 1);
        assertEquals(33.33333333333333, oneThird.getViewGrowthRate(), 0.00000000000001);
        
        // 66.66...% (2/3)
        PostViewStats twoThirds = new PostViewStats(testPostId, 3L, 2L, 2);
        assertEquals(66.66666666666666, twoThirds.getViewGrowthRate(), 0.00000000000001);
    }
    
    @Test
    @DisplayName("Should provide consistent engagement level mapping")
    void shouldProvideConsistentEngagementLevelMapping() {
        // Test that the same percentage always maps to the same level
        for (int total = 100; total <= 1000; total += 100) {
            for (int recent = 0; recent <= total; recent += 10) {
                PostViewStats stats = new PostViewStats(testPostId, (long) total, (long) recent, recent);
                double rate = stats.getViewGrowthRate();
                
                // Verify that the same rate always produces the same level
                String expectedLevel = getExpectedLevel(rate);
                assertEquals(expectedLevel, stats.getEngagementLevel(), 
                    "Rate: " + rate + "%, Total: " + total + ", Recent: " + recent);
            }
        }
    }
    
    private String getExpectedLevel(double rate) {
        if (rate > 20.0) return "Very High";
        if (rate > 10.0) return "High";
        if (rate > 5.0) return "Moderate";
        if (rate > 1.0) return "Low";
        return "Very Low";
    }
} 
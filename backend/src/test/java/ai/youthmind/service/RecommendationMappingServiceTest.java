package ai.youthmind.service;

import ai.youthmind.entity.RecommendationMappingRule;
import ai.youthmind.repository.RecommendationMappingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RecommendationMappingService
 * Tests core functionality of recommendation mapping rule management
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RecommendationMappingServiceTest {

    @Autowired
    private RecommendationMappingService recommendationMappingService;

    @Autowired
    private RecommendationMappingRuleRepository recommendationMappingRuleRepository;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        recommendationMappingRuleRepository.deleteAll();
    }

    @Test
    void testSeedDefaultRulesCreatesAll20Rules() {
        // Act
        int rulesCreated = recommendationMappingService.seedDefaultRules();

        // Assert
        assertEquals(20, rulesCreated, "Should create all 20 default rules");
        
        // Verify all rules exist in database
        long totalRules = recommendationMappingRuleRepository.count();
        assertEquals(20, totalRules, "Should have exactly 20 rules in database");
        
        // Verify some specific rules exist
        assertTrue(recommendationMappingRuleRepository.existsByEmotionCombination("sad"));
        assertTrue(recommendationMappingRuleRepository.existsByEmotionCombination("angry"));
        assertTrue(recommendationMappingRuleRepository.existsByEmotionCombination("angry,sad"));
        assertTrue(recommendationMappingRuleRepository.existsByEmotionCombination("fear,sad"));
        
        // Verify default distribution is applied
        Optional<RecommendationMappingRule> sadRule = recommendationMappingRuleRepository.findByEmotionCombination("sad");
        assertTrue(sadRule.isPresent());
        assertEquals(20, sadRule.get().getSadPercentage());
        assertEquals(5, sadRule.get().getFearPercentage());
        assertEquals(20, sadRule.get().getAngryPercentage());
        assertEquals(20, sadRule.get().getHappyPercentage());
        assertEquals(20, sadRule.get().getConflictedPercentage());
        assertEquals(15, sadRule.get().getSurprisedPercentage());
        assertEquals(100, sadRule.get().getTotalPercentage());
    }

    @Test
    void testSeedDefaultRulesIsIdempotent() {
        // Act - Run seedDefaultRules twice
        int firstRun = recommendationMappingService.seedDefaultRules();
        int secondRun = recommendationMappingService.seedDefaultRules();

        // Assert
        assertEquals(20, firstRun, "First run should create 20 rules");
        assertEquals(0, secondRun, "Second run should create 0 rules (idempotent)");
        
        // Verify total count is still 20 (no duplicates)
        long totalRules = recommendationMappingRuleRepository.count();
        assertEquals(20, totalRules, "Should still have exactly 20 rules after second run");
        
        // Verify all rules are still valid
        assertTrue(recommendationMappingService.areAllRulesSeeded());
    }

    @Test
    void testGenerateCombinationKeySortsEmotionsCorrectly() {
        // Test case 1: ["sad", "angry", "fear"] → "angry,fear,sad"
        List<String> emotions1 = Arrays.asList("sad", "angry", "fear");
        String result1 = recommendationMappingService.generateCombinationKey(emotions1);
        assertEquals("angry,fear,sad", result1);
        
        // Test case 2: ["happy", "conflicted", "surprised"] → "conflicted,happy,surprised"
        List<String> emotions2 = Arrays.asList("happy", "conflicted", "surprised");
        String result2 = recommendationMappingService.generateCombinationKey(emotions2);
        assertEquals("conflicted,happy,surprised", result2);
        
        // Test case 3: Single emotion
        List<String> emotions3 = Arrays.asList("sad");
        String result3 = recommendationMappingService.generateCombinationKey(emotions3);
        assertEquals("sad", result3);
        
        // Test case 4: Already sorted
        List<String> emotions4 = Arrays.asList("angry", "fear", "sad");
        String result4 = recommendationMappingService.generateCombinationKey(emotions4);
        assertEquals("angry,fear,sad", result4);
        
        // Test case 5: Empty list
        List<String> emotions5 = Arrays.asList();
        String result5 = recommendationMappingService.generateCombinationKey(emotions5);
        assertEquals("", result5);
        
        // Test case 6: Null list
        String result6 = recommendationMappingService.generateCombinationKey(null);
        assertEquals("", result6);
    }

    @Test
    void testGetDistributionForEmotionsReturnsCustomRuleWhenItExists() {
        // Arrange - Don't seed default rules, create only the custom rule we need
        RecommendationMappingRule customRule = new RecommendationMappingRule();
        customRule.setEmotionCombination("angry,fear,sad"); // Note: sorted order
        customRule.setSadPercentage(30);
        customRule.setFearPercentage(10);
        customRule.setAngryPercentage(30);
        customRule.setHappyPercentage(10);
        customRule.setConflictedPercentage(10);
        customRule.setSurprisedPercentage(10);
        recommendationMappingRuleRepository.save(customRule);
        
        // Act
        List<String> topThree = Arrays.asList("sad", "angry", "fear");
        Map<String, Integer> distribution = recommendationMappingService.getDistributionForEmotions(topThree);
        
        // Assert
        assertNotNull(distribution);
        assertEquals(6, distribution.size(), "Should have 6 emotions in distribution");
        assertEquals(30, distribution.get("sad"));
        assertEquals(10, distribution.get("fear"));
        assertEquals(30, distribution.get("angry"));
        assertEquals(10, distribution.get("happy"));
        assertEquals(10, distribution.get("conflicted"));
        assertEquals(10, distribution.get("surprised"));
        
        // Verify total is 100
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(100, total);
    }

    @Test
    void testGetDistributionForEmotionsReturnsDefaultDistributionWhenRuleDoesNotExist() {
        // Arrange - Don't seed any rules, so no custom rules exist
        
        // Act
        List<String> topThree = Arrays.asList("sad", "angry", "fear");
        Map<String, Integer> distribution = recommendationMappingService.getDistributionForEmotions(topThree);
        
        // Assert - Should return default distribution
        assertNotNull(distribution);
        assertEquals(6, distribution.size(), "Should have 6 emotions in distribution");
        assertEquals(20, distribution.get("sad"));      // Default: 20%
        assertEquals(5, distribution.get("fear"));      // Default: 5%
        assertEquals(20, distribution.get("angry"));    // Default: 20%
        assertEquals(20, distribution.get("happy"));    // Default: 20%
        assertEquals(20, distribution.get("conflicted")); // Default: 20%
        assertEquals(15, distribution.get("surprised")); // Default: 15%
        
        // Verify total is 100
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(100, total);
    }

    @Test
    void testFindRuleByEmotionCombinationReturnsEmptyOptionalWhenNotFound() {
        // Arrange - No rules in database
        
        // Act
        Optional<RecommendationMappingRule> result = recommendationMappingService.findRuleByEmotionCombination("nonexistent,combination");
        
        // Assert
        assertFalse(result.isPresent(), "Should return empty Optional when rule not found");
        
        // Test with null input
        Optional<RecommendationMappingRule> resultNull = recommendationMappingService.findRuleByEmotionCombination(null);
        assertFalse(resultNull.isPresent(), "Should return empty Optional for null input");
        
        // Test with empty input
        Optional<RecommendationMappingRule> resultEmpty = recommendationMappingService.findRuleByEmotionCombination("");
        assertFalse(resultEmpty.isPresent(), "Should return empty Optional for empty input");
    }

    @Test
    void testGetDistributionForEmotionsWithNullInput() {
        // Act
        Map<String, Integer> distribution = recommendationMappingService.getDistributionForEmotions(null);
        
        // Assert - Should return default distribution
        assertNotNull(distribution);
        assertEquals(6, distribution.size());
        assertEquals(20, distribution.get("sad"));
        assertEquals(5, distribution.get("fear"));
        assertEquals(20, distribution.get("angry"));
        assertEquals(20, distribution.get("happy"));
        assertEquals(20, distribution.get("conflicted"));
        assertEquals(15, distribution.get("surprised"));
    }

    @Test
    void testGetDistributionForEmotionsWithEmptyInput() {
        // Act
        Map<String, Integer> distribution = recommendationMappingService.getDistributionForEmotions(Arrays.asList());
        
        // Assert - Should return default distribution
        assertNotNull(distribution);
        assertEquals(6, distribution.size());
        assertEquals(20, distribution.get("sad"));
        assertEquals(5, distribution.get("fear"));
        assertEquals(20, distribution.get("angry"));
        assertEquals(20, distribution.get("happy"));
        assertEquals(20, distribution.get("conflicted"));
        assertEquals(15, distribution.get("surprised"));
    }

    @Test
    void testAreAllRulesSeeded() {
        // Initially should be false (no rules)
        assertFalse(recommendationMappingService.areAllRulesSeeded());
        
        // After seeding, should be true
        recommendationMappingService.seedDefaultRules();
        assertTrue(recommendationMappingService.areAllRulesSeeded());
    }

    @Test
    void testGetTotalCombinationCount() {
        // Act
        int totalCount = recommendationMappingService.getTotalCombinationCount();
        
        // Assert
        assertEquals(20, totalCount, "Should return 20 total combinations");
    }
}

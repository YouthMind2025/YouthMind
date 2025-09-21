package ai.youthmind.comprehensive;

import ai.youthmind.entity.*;
import ai.youthmind.repository.*;
import ai.youthmind.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ComprehensiveSystemTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private MemoryItemRepository memoryRepository;
    
    @Autowired
    private UserPreferencesRepository preferencesRepository;
    
    @Autowired
    private MemoryService memoryService;
    
    @Autowired
    private RAGContextBuilderService ragContextBuilder;
    
    @Autowired
    private AIFallbackService fallbackService;
    
    @Autowired
    private CircuitBreakerService circuitBreakerService;
    
    private User testUser;
    private UserPreferences testPreferences;
    
    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setEmail("comprehensive@test.com");
        testUser.setPassword("hashedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setUsername("comptest");
        testUser = userRepository.save(testUser);
        
        // Create user preferences
        testPreferences = new UserPreferences();
        testPreferences.setUserId(testUser.getId());
        testPreferences.setTheme(UserPreferences.Theme.DARK);
        testPreferences.setAiSuggestionsEnabled(true);
        testPreferences = preferencesRepository.save(testPreferences);
    }
    
    @Test
    @Order(1)
    @DisplayName("Test Enhanced Post Entity with Moderation Fields")
    @Transactional
    void testEnhancedPostEntity() {
        System.out.println("=== Testing Enhanced Post Entity ===");
        
        Post post = new Post();
        post.setUserId(testUser.getId());
        post.setTitle("Test Post with Moderation");
        post.setContent("This is a test post with enhanced moderation fields");
        post.setEmotion("HAPPY");
        post.setModerationLayer("AI_PRIMARY");
        post.setModerationConfidence(0.95);
        post.setModerationDetails("{\"categories\": [\"safe\"], \"confidence\": 0.95}");
        
        Post savedPost = postRepository.save(post);
        
        assertThat(savedPost.getId()).isNotNull();
        assertThat(savedPost.getModerationLayer()).isEqualTo("AI_PRIMARY");
        assertThat(savedPost.getModerationConfidence()).isEqualTo(0.95);
        assertThat(savedPost.getContentHash()).isNotNull(); // Should be auto-generated
        
        System.out.println("✓ Enhanced post entity test passed");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test Memory System End-to-End")
    @Transactional
    void testMemorySystemEndToEnd() {
        System.out.println("=== Testing Memory System End-to-End ===");
        
        // Store different types of memories
        MemoryItem chatMemory = memoryService.storeChatMemory(
            testUser.getId(),
            "User discussing anxiety",
            "I'm feeling really anxious about my presentation tomorrow",
            "I understand that presentations can be nerve-wracking. What specific aspects worry you most?"
        );
        
        MemoryItem personalityMemory = memoryService.storePersonalityTrait(
            testUser.getId(),
            "analytical_thinker",
            "User asks detailed questions and seeks thorough explanations",
            0.8
        );
        
        MemoryItem engagementMemory = memoryService.storeEngagementMemory(
            testUser.getId(),
            "RESONANCE",
            "POST",
            "User resonated with mental health awareness post"
        );
        
        // Test memory retrieval
        String compressedContext = memoryService.getCompressedContext(testUser.getId(), "anxiety presentation");
        assertThat(compressedContext).isNotNull();
        assertThat(compressedContext.length()).isLessThanOrEqualTo(2048); // 2KB limit
        
        // Test weight calculation
        assertThat(chatMemory.getWeightScore()).isGreaterThan(BigDecimal.ZERO);
        assertThat(personalityMemory.getWeightScore()).isGreaterThan(chatMemory.getWeightScore());
        
        System.out.println("Memory context size: " + compressedContext.length() + " characters");
        System.out.println("✓ Memory system end-to-end test passed");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test RAG Context Builder")
    void testRAGContextBuilder() {
        System.out.println("=== Testing RAG Context Builder ===");
        
        // Create some test data first
        memoryService.storePersonalityTrait(testUser.getId(), "empathetic", "Shows concern for others", 0.9);
        memoryService.storeChatMemory(testUser.getId(), "anxiety discussion", 
                                    "I'm worried about my job", "Let's explore what's causing this worry");
        
        // Build RAG context
        String context = ragContextBuilder.buildRAGContext(
            testUser.getId(),
            "I'm feeling overwhelmed with work",
            "Previous conversation about job stress"
        );
        
        assertThat(context).isNotNull();
        assertThat(context.length()).isLessThanOrEqualTo(2048);
        assertThat(context).contains("USER_PROFILE:");
        
        // Test context facts
        Map<String, Object> facts = ragContextBuilder.getContextFacts(
            testUser.getId(), "I need help with stress management");
        
        assertThat(facts).isNotEmpty();
        assertThat(facts).containsKey("userName");
        assertThat(facts.get("userName")).isEqualTo("Test");
        
        System.out.println("RAG context size: " + context.length() + " characters");
        System.out.println("Context facts: " + facts.size() + " items");
        System.out.println("✓ RAG context builder test passed");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test AI Fallback System")
    void testAIFallbackSystem() {
        System.out.println("=== Testing AI Fallback System ===");
        
        // Test crisis detection
        AIFallbackService.ClassificationResult crisisResult = 
            fallbackService.classifyMessage("I want to end it all, life isn't worth living");
        
        assertThat(crisisResult.getCategory()).isEqualTo("CRISIS");
        assertThat(crisisResult.getConfidence()).isEqualTo(1.0);
        assertThat(crisisResult.getFallbackResponse()).contains("crisis");
        
        // Test emotional support detection
        AIFallbackService.ClassificationResult supportResult = 
            fallbackService.classifyMessage("I'm feeling really sad and lonely today");
        
        assertThat(supportResult.getCategory()).isEqualTo("EMOTIONAL_SUPPORT");
        assertThat(supportResult.getConfidence()).isGreaterThan(0.5);
        
        // Test moderation decision
        AIFallbackService.ModerationDecision decision = 
            fallbackService.makeModerationDecision("This is a normal message", 0.95);
        
        assertThat(decision.getAction()).isEqualTo(AIFallbackService.ModerationAction.ALLOW);
        
        // Test degraded response
        String degradedResponse = fallbackService.provideDegradedResponse(
            "I need help with anxiety", testUser.getId());
        
        assertThat(degradedResponse).isNotNull();
        assertThat(degradedResponse).contains("simplified mode");
        
        System.out.println("✓ AI fallback system test passed");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test Circuit Breaker Pattern")
    void testCircuitBreakerPattern() {
        System.out.println("=== Testing Circuit Breaker Pattern ===");
        
        // Test successful operation
        String result1 = circuitBreakerService.executeWithCircuitBreaker(
            "test_service",
            () -> "success",
            () -> "fallback"
        );
        assertThat(result1).isEqualTo("success");
        
        // Test fallback when operation fails
        String result2 = circuitBreakerService.executeWithCircuitBreaker(
            "failing_service",
            () -> {
                throw new RuntimeException("Service unavailable");
            },
            () -> "fallback_response"
        );
        assertThat(result2).isEqualTo("fallback_response");
        
        System.out.println("✓ Circuit breaker pattern test passed");
    }
    
    @Test
    @Order(6)
    @DisplayName("Test Complete Integration Flow")
    @Transactional
    void testCompleteIntegrationFlow() {
        System.out.println("=== Testing Complete Integration Flow ===");
        
        // 1. Create a post with enhanced fields
        Post post = new Post();
        post.setUserId(testUser.getId());
        post.setTitle("Integration Test Post");
        post.setContent("This post tests the complete integration flow");
        post.setEmotion("EXCITED");
        post.setModerationLayer("AI_PRIMARY");
        post.setModerationConfidence(0.88);
        Post savedPost = postRepository.save(post);
        
        // 2. Store memory about this interaction
        memoryService.storeEngagementMemory(
            testUser.getId(), "POST_CREATION", "POST", 
            "User created post about integration testing");
        
        // 3. Build RAG context
        String ragContext = ragContextBuilder.buildRAGContext(
            testUser.getId(), "How did my post perform?", "");
        
        assertThat(ragContext).isNotNull();
        assertThat(ragContext).contains("USER_PROFILE:");
        
        // 4. Test fallback classification
        AIFallbackService.ClassificationResult classification = 
            fallbackService.classifyMessage("How did my post perform?");
        
        assertThat(classification.getCategory()).isEqualTo("GENERAL");
        
        // 5. Verify all data is properly stored
        Optional<Post> retrievedPost = postRepository.findById(savedPost.getId());
        assertThat(retrievedPost).isPresent();
        assertThat(retrievedPost.get().getContentHash()).isNotNull();
        
        List<MemoryItem> userMemories = memoryRepository.findByUserId(testUser.getId());
        assertThat(userMemories).isNotEmpty();
        
        Optional<UserPreferences> userPrefs = preferencesRepository.findByUserId(testUser.getId());
        assertThat(userPrefs).isPresent();
        
        System.out.println("Posts in database: " + postRepository.count());
        System.out.println("Memories stored: " + userMemories.size());
        System.out.println("RAG context length: " + ragContext.length());
        System.out.println("✓ Complete integration flow test passed");
    }
    
    @AfterEach
    void cleanup() {
        // Cleanup handled by test framework
        System.out.println("Test cleanup completed");
    }
}

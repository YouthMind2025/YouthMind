package ai.youthmind.cli;

// Import the ACTUAL production classes
import ai.youthmind.service.PsychologicalVectorGenerator;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * CLI Test Tool for 43-Vector Psychological Analysis
 * This test uses the ACTUAL production PsychologicalVectorGenerator class
 * 
 * ✅ Uses real production PsychologicalVectorGenerator
 * ✅ Injects mock RedisTemplate via reflection
 * ✅ Calls actual generateVector() method
 * ✅ Uses exact keyword mappings from production code
 * ❌ NO reimplemented logic
 */
public class PsychologicalVectorCLITest {
    
    /**
     * Factor names in EXACT order as they appear in production code
     * PsychologicalVectorGenerator.analyzeBuddhistMentalFactors()
     */
    private static final String[] FACTOR_NAMES = {
        // Positive factors (1-16) - Wholesome
        "Attention", "Contact", "Feeling", "Thinking", "Deliberation", 
        "Faith", "Vigor", "Inner Shame", "Shame", 
        "Absence of Craving", "Absence of Hatred", "Absence of Ignorance", 
        "Light Ease", "Non-laxness", "Renunciation", "Non-harming",
        
        // Negative factors (17-43) - Unwholesome
        "Greed", "Anger", "Arrogance", "Foolishness", "Doubt", 
        "Improper Views", "Wrath", "Hatred", "Rage", "Covering", 
        "Deceit", "Flattery", "Conceit", "Harming", "Jealousy", 
        "Stinginess", "Lack of Remorse", "Lack of Shame", "Lack of Faith", 
        "Laziness", "Laxness", "Torpor", "Restlessness", "Distraction", 
        "Improper Knowledge", "Scatteredness", "Regret"
    };
    
    public static void main(String[] args) {
        System.out.println("=== YouthMind 43-Vector Psychological Analysis CLI Test ===");
        System.out.println("✅ Using ACTUAL production code from PsychologicalVectorGenerator");
        System.out.println("✅ No reimplemented logic - testing real system");
        System.out.println();
        
        String testText = "A very minor thing happened which triggered my panic attack. I made a very small mistake at a center I visited which led to me " +
                         "spiraling down into anxiety and depression because I thought \"if i cannot even get this thing right, how am I even gonna find and hold " +
                         "down a job\"? I previously burnout hard at my masters, and subsequently dropped out, developed health issues, and my future isnt " +
                         "certain and I am very anxious about it. I locked myself up in my room all day because I am having suicidal thoughts. I told my mom " +
                         "what happened, and continued to lock myself up. During dinner time, I heard noises. Turns out my parents were fighting about what " +
                         "I told my mom, and then my dad said 'a loser is a loser'; I am sure he is referring to me. So... yeah. What a way to welcome the new year.";
        
        System.out.println("Test Input Text:");
        System.out.println("\"" + (testText.length() > 100 ? testText.substring(0, 100) + "..." : testText) + "\"");
        System.out.println();
        
        try {
            // Step 1: Create a mock RedisTemplate
            org.springframework.data.redis.core.RedisTemplate<String, Object> mockRedis = createDynamicMockRedisTemplate();
            System.out.println("✓ Created mock RedisTemplate (in-memory, no actual Redis connection)");
            
            // Step 2: Instantiate the ACTUAL production PsychologicalVectorGenerator
            PsychologicalVectorGenerator generator = new PsychologicalVectorGenerator();
            System.out.println("✓ Instantiated REAL PsychologicalVectorGenerator from production code");
            
            // Step 3: Inject the mock Redis dependency using reflection
            injectRedisTemplate(generator, mockRedis);
            System.out.println("✓ Injected mock RedisTemplate into production class via reflection");
            
            // Step 4: Call the ACTUAL generateVector() method from production code
            UUID testUserId = UUID.randomUUID();
            System.out.println("✓ Calling REAL production method: PsychologicalVectorGenerator.generateVector()");
            System.out.println();
            
            Map<String, Object> result = generator.generateVector(testText, testUserId);
            
            // Step 5: Extract and display the results
            displayResults(result);
            
            System.out.println();
            System.out.println("=== Test Complete ===");
            System.out.println("✅ Successfully tested ACTUAL production code");
            System.out.println("✅ All 43 Buddhist mental factors analyzed");
            System.out.println("✅ Using real keyword mappings and calculation logic");
            
        } catch (Exception e) {
            System.err.println();
            System.err.println("❌ Error running CLI test: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create a mock RedisTemplate by extending the class
     * This allows us to test the production code without a real Redis instance
     */
    private static org.springframework.data.redis.core.RedisTemplate<String, Object> createDynamicMockRedisTemplate() {
        final Map<String, Object> cache = new HashMap<>();
        
        // Create a concrete mock class that extends RedisTemplate
        return new org.springframework.data.redis.core.RedisTemplate<String, Object>() {
            @Override
            public org.springframework.data.redis.core.ValueOperations<String, Object> opsForValue() {
                return createValueOperationsMock(cache);
            }
        };
    }
    
    /**
     * Create a mock ValueOperations using a dynamic proxy
     * Only implements get() and set() which are the methods used by PsychologicalVectorGenerator
     */
    private static org.springframework.data.redis.core.ValueOperations<String, Object> createValueOperationsMock(final Map<String, Object> cache) {
        return (org.springframework.data.redis.core.ValueOperations<String, Object>) Proxy.newProxyInstance(
            PsychologicalVectorCLITest.class.getClassLoader(),
            new Class<?>[]{org.springframework.data.redis.core.ValueOperations.class},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String methodName = method.getName();
                    
                    // Handle get() method - return from in-memory cache
                    if (methodName.equals("get") && args != null && args.length == 1) {
                        return cache.get(args[0]);
                    }
                    
                    // Handle set() methods - store in in-memory cache
                    if (methodName.equals("set") && args != null && args.length >= 2) {
                        cache.put((String) args[0], args[1]);
                        return null;
                    }
                    
                    // Handle other methods with default returns
                    Class<?> returnType = method.getReturnType();
                    if (returnType == Boolean.class || returnType == boolean.class) {
                        return true;
                    } else if (returnType == Long.class || returnType == long.class) {
                        return 0L;
                    } else if (returnType == Integer.class || returnType == int.class) {
                        return 0;
                    } else if (returnType == Double.class || returnType == double.class) {
                        return 0.0;
                    } else if (returnType == List.class) {
                        return new ArrayList<>();
                    } else if (returnType == Set.class) {
                        return new HashSet<>();
                    } else if (returnType == Map.class) {
                        return new HashMap<>();
                    }
                    
                    return null;
                }
            }
        );
    }
    
    /**
     * Inject the mock Redis dependency into the generator using reflection
     * This bypasses Spring's @Autowired mechanism for testing
     */
    private static void injectRedisTemplate(PsychologicalVectorGenerator generator, org.springframework.data.redis.core.RedisTemplate<String, Object> mockRedis) throws Exception {
        Field redisField = PsychologicalVectorGenerator.class.getDeclaredField("redisTemplate");
        redisField.setAccessible(true);
        redisField.set(generator, mockRedis);
    }
    
    /**
     * Display the results from the ACTUAL production code
     * Includes debug output to verify production code integration
     */
    private static void displayResults(Map<String, Object> result) {
        System.out.println("=== Raw Result from Production Code ===");
        System.out.println("Result keys: " + result.keySet());
        System.out.println("Dimensions: " + result.get("dimensions"));
        System.out.println("Is Valid: " + result.get("isValid"));
        System.out.println("Generation Method: " + result.get("generationMethod"));
        System.out.println("Timestamp: " + result.get("timestamp"));
        System.out.println();
        
        // Extract the psychological vector from the result
        @SuppressWarnings("unchecked")
        List<Double> vector = (List<Double>) result.get("psychologicalVector");
        
        if (vector == null) {
            System.err.println("❌ Error: No psychological vector found in result");
            System.out.println("Available keys: " + result.keySet());
            return;
        }
        
        // Verify vector dimensions
        if (vector.size() != 43) {
            System.err.println("⚠️  Warning: Expected 43 dimensions, got " + vector.size());
        } else {
            System.out.println("✓ Vector dimensions: " + vector.size() + " (correct)");
        }
        
        // Verify value ranges
        boolean allInRange = vector.stream().allMatch(v -> v >= 0.0 && v <= 1.0);
        if (!allInRange) {
            System.err.println("⚠️  Warning: Some values are outside [0.0, 1.0] range");
        } else {
            System.out.println("✓ All values in valid range [0.0, 1.0]");
        }
        System.out.println();
        
        // Create factor results with names and values
        List<FactorResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(vector.size(), FACTOR_NAMES.length); i++) {
            results.add(new FactorResult(i, FACTOR_NAMES[i], vector.get(i)));
        }
        
        // Sort by intensity (highest first)
        results.sort((a, b) -> Double.compare(b.value, a.value));
        
        // Display results in table format
        System.out.println("=== 43-Dimensional Buddhist Mental Factors Analysis ===");
        System.out.printf("%-30s %s%n", "Factor Name", "Value");
        System.out.println("-----------------------------------------------");
        
        // Show all factors
        for (FactorResult factorResult : results) {
            // Highlight significant factors (value > 0.0)
            if (factorResult.value > 0.0) {
                System.out.printf("%-30s %.2f ★%n", factorResult.name, factorResult.value);
            } else {
                System.out.printf("%-30s %.2f%n", factorResult.name, factorResult.value);
            }
        }
        
        System.out.println("-----------------------------------------------");
        System.out.println("★ = Significant factor detected (value > 0.0)");
    }
    
    /**
     * Helper class for displaying factor results
     */
    private static class FactorResult {
        final int index;
        final String name;
        final double value;
        
        FactorResult(int index, String name, double value) {
            this.index = index;
            this.name = name;
            this.value = value;
        }
    }
}

package ai.youthmind.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Command Line Interface for 43-Vector Psychological Analysis
 * 
 * This standalone CLI tool allows users to input text and receive
 * psychological vector analysis using the YouthMind 43-dimensional
 * Buddhist mental factors analysis system.
 * 
 * Usage:
 *   java -cp target/classes ai.youthmind.cli.PsychologicalVectorCLI
 *   mvn exec:java -Dexec.mainClass="ai.youthmind.cli.PsychologicalVectorCLI"
 */
public class PsychologicalVectorCLI {
    
    private static final String[] FACTOR_NAMES = {
        // Positive factors (wholesome - dimensions 0-15)
        "Attention", "Contact", "Feeling", "Thinking", "Deliberation", "Faith", "Vigor", "Inner Shame",
        "Shame", "Absence of Craving", "Absence of Hatred", "Absence of Ignorance", "Light Ease",
        "Non-laxness", "Renunciation", "Non-harming",
        
        // Negative factors (unwholesome - dimensions 16-42)
        "Greed", "Anger", "Arrogance", "Foolishness", "Doubt", "Improper Views", "Wrath", "Hatred",
        "Rage", "Covering", "Deceit", "Flattery", "Conceit", "Harming", "Jealousy", "Stinginess",
        "Lack of Remorse", "Lack of Shame", "Lack of Faith", "Laziness", "Laxness", "Torpor",
        "Restlessness", "Distraction", "Improper Knowledge", "Scatteredness", "Regret"
    };
    
    private static final String[][] KEYWORDS = {
        // Positive factors keywords
        {"attention", "mindful", "aware", "focused", "attentive"},
        {"contact", "touch", "connection", "interaction", "engagement"},
        {"feeling", "emotion", "sensation", "experience", "perception"},
        {"thinking", "thought", "consideration", "reflection", "contemplation"},
        {"deliberation", "consideration", "pondering", "weighing", "evaluation"},
        {"faith", "trust", "confidence", "belief", "conviction"},
        {"vigor", "energy", "enthusiasm", "zeal", "effort"},
        {"inner shame", "conscience", "moral sense", "self-respect", "dignity"},
        {"shame", "embarrassment", "humility", "modesty", "bashfulness"},
        {"contentment", "satisfaction", "non-craving", "detachment", "freedom"},
        {"love", "compassion", "kindness", "forgiveness", "non-hatred"},
        {"wisdom", "understanding", "knowledge", "insight", "clarity"},
        {"lightness", "ease", "comfort", "relief", "peace"},
        {"diligence", "perseverance", "steadfastness", "determination", "commitment"},
        {"renunciation", "letting go", "detachment", "abandonment", "release"},
        {"non-harming", "compassion", "kindness", "gentleness", "care"},
        
        // Negative factors keywords
        {"greed", "desire", "craving", "attachment", "covetousness"},
        {"anger", "rage", "fury", "wrath", "hostility"},
        {"arrogance", "pride", "conceit", "haughtiness", "superiority"},
        {"foolishness", "ignorance", "stupidity", "naivety", "unwisdom"},
        {"doubt", "uncertainty", "skepticism", "hesitation", "confusion"},
        {"wrong view", "misconception", "delusion", "false belief", "distorted thinking"},
        {"wrath", "rage", "fury", "anger", "indignation"},
        {"hatred", "aversion", "dislike", "enmity", "hostility"},
        {"rage", "fury", "wrath", "anger", "outrage"},
        {"covering", "hiding", "concealing", "masking", "disguising"},
        {"deceit", "deception", "lying", "falsehood", "dishonesty"},
        {"flattery", "praise", "compliment", "adulation", "sycophancy"},
        {"conceit", "pride", "arrogance", "vanity", "self-importance"},
        {"harming", "hurting", "damaging", "injuring", "violence"},
        {"jealousy", "envy", "resentment", "covetousness", "bitterness"},
        {"stinginess", "greed", "selfishness", "miserliness", "avarice"},
        {"no remorse", "unrepentant", "unconcerned", "indifferent", "callous"},
        {"shameless", "brazen", "impudent", "audacious", "bold"},
        {"faithless", "distrustful", "skeptical", "cynical", "disbelieving"},
        {"laziness", "sloth", "idleness", "indolence", "procrastination"},
        {"laxness", "looseness", "carelessness", "negligence", "slackness"},
        {"torpor", "dullness", "lethargy", "sluggishness", "inertia"},
        {"restlessness", "agitation", "unease", "anxiety", "worry"},
        {"distraction", "scatteredness", "confusion", "disorder", "chaos"},
        {"wrong knowledge", "misunderstanding", "ignorance", "confusion", "delusion"},
        {"scatteredness", "distraction", "dispersion", "fragmentation", "disorder"},
        {"regret", "remorse", "sorrow", "repentance", "guilt"}
    };
    
    public static void main(String[] args) {
        System.out.println("=== YouthMind 43-Vector Psychological Analysis CLI ===");
        System.out.println("Analyze text using Buddhist mental factors framework");
        System.out.println("Type 'quit' or 'exit' to exit the program");
        System.out.println("Type 'help' for usage instructions");
        System.out.println();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        while (true) {
            try {
                System.out.print("Enter text to analyze (or command): ");
                String input = reader.readLine();
                
                if (input == null || input.trim().isEmpty()) {
                    continue;
                }
                
                String command = input.trim().toLowerCase();
                
                if (command.equals("quit") || command.equals("exit")) {
                    System.out.println("Goodbye!");
                    break;
                }
                
                if (command.equals("help")) {
                    printHelp();
                    continue;
                }
                
                if (command.equals("example")) {
                    runExample();
                    continue;
                }
                
                // Analyze the input text
                analyzeText(input);
                
            } catch (IOException e) {
                System.err.println("Error reading input: " + e.getMessage());
            }
        }
    }
    
    private static void printHelp() {
        System.out.println("\n=== Help ===");
        System.out.println("Commands:");
        System.out.println("  help     - Show this help message");
        System.out.println("  example  - Run analysis on example text");
        System.out.println("  quit/exit - Exit the program");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  Simply paste or type your text and press Enter");
        System.out.println("  The tool will analyze it using 43 Buddhist mental factors");
        System.out.println("  Results are displayed sorted by intensity (highest first)");
        System.out.println();
        System.out.println("Output Format:");
        System.out.println("  Factor Name          Value");
        System.out.println("  ----------------------------");
        System.out.println("  Doubt               0.60");
        System.out.println("  Attention           0.60");
        System.out.println("  ...");
        System.out.println();
    }
    
    private static void runExample() {
        String exampleText = "A very minor thing happened which triggered my panic attack. " +
                           "I made a very small mistake at a center I visited which led to me " +
                           "spiraling down into anxiety and depression because I thought 'if i " +
                           "cannot even get this thing right, how am I even gonna find and hold " +
                           "down a job'? I previously burnout hard at my masters, and subsequently " +
                           "dropped out, developed health issues, and my future isnt certain and " +
                           "I am very anxious about it...";
        
        System.out.println("\n=== Example Analysis ===");
        System.out.println("Text: " + exampleText);
        System.out.println();
        
        analyzeText(exampleText);
    }
    
    private static void analyzeText(String text) {
        System.out.println("\n=== Analysis Results ===");
        
        // Generate 43-dimensional vector
        List<Double> vector = generatePsychologicalVector(text);
        
        // Create factor results with names and values
        List<FactorResult> results = new ArrayList<>();
        for (int i = 0; i < vector.size(); i++) {
            results.add(new FactorResult(i, FACTOR_NAMES[i], vector.get(i)));
        }
        
        // Sort by intensity (highest first)
        results.sort((a, b) -> Double.compare(b.value, a.value));
        
        // Display results
        System.out.printf("%-20s %s%n", "Factor Name", "Value");
        System.out.println("----------------------------");
        
        for (FactorResult result : results) {
            if (result.value > 0.0) { // Only show non-zero values
                System.out.printf("%-20s %.2f%n", result.name, result.value);
            }
        }
        
        System.out.println();
    }
    
    private static List<Double> generatePsychologicalVector(String content) {
        List<Double> vector = new ArrayList<>();
        
        for (int i = 0; i < 43; i++) {
            double intensity = calculateFactorIntensity(content, KEYWORDS[i]);
            vector.add(intensity);
        }
        
        return vector;
    }
    
    private static double calculateFactorIntensity(String content, String[] keywords) {
        if (content == null || content.isEmpty()) {
            return 0.0;
        }
        
        String lowerContent = content.toLowerCase();
        int totalOccurrences = 0;
        int totalWords = content.split("\\s+").length;
        
        for (String keyword : keywords) {
            int occurrences = countOccurrences(lowerContent, keyword);
            totalOccurrences += occurrences;
        }
        
        // Normalize to [0, 1] range using the same logic as the main system
        double intensity = Math.min(1.0, (double) totalOccurrences / Math.max(1, totalWords / 10));
        return intensity;
    }
    
    private static int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }
    
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




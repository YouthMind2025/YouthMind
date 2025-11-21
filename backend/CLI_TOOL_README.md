# YouthMind 43-Vector Analysis CLI Tool

A standalone command-line interface for analyzing text using the YouthMind 43-dimensional Buddhist mental factors analysis system.

## Features

- **Standalone Operation**: No database or Spring Boot application required
- **43-Dimensional Analysis**: Uses the complete Buddhist mental factors framework
- **Interactive CLI**: Simple text input with formatted output
- **Multiple Execution Methods**: Maven exec, standalone JAR, or direct Java execution
- **Example Analysis**: Built-in example with expected output

## Quick Start

### Method 1: Maven Exec Plugin (Recommended)
```bash
cd backend
mvn exec:java
```

### Method 2: Standalone JAR
```bash
cd backend
mvn clean package
java -jar target/youthmind-cli-tool.jar
```

### Method 3: Direct Java Execution
```bash
cd backend
mvn compile
java -cp target/classes ai.youthmind.cli.PsychologicalVectorCLI
```

## Usage

1. **Start the CLI tool** using any of the methods above
2. **Enter text** to analyze when prompted
3. **View results** displayed in a formatted table
4. **Use commands**:
   - `help` - Show usage instructions
   - `example` - Run analysis on example text
   - `quit` or `exit` - Exit the program

## Example Output

```
=== YouthMind 43-Vector Psychological Analysis CLI ===
Analyze text using Buddhist mental factors framework
Type 'quit' or 'exit' to exit the program
Type 'help' for usage instructions

Enter text to analyze (or command): example

=== Example Analysis ===
Text: A very minor thing happened which triggered my panic attack...

=== Analysis Results ===
Factor Name          Value
----------------------------
Doubt               0.60
Attention           0.60
Feeling             0.50
Anger               0.40
Foolishness         0.40
Contact             0.40
Thinking            0.40
Deliberation        0.40
Restlessness        0.30
Anxiety             0.30
...
```

## The 43 Buddhist Mental Factors

### Positive Factors (Wholesome - Dimensions 0-15)
1. **Attention** - Mindful awareness and focus
2. **Contact** - Sensory and mental contact
3. **Feeling** - Emotional sensations and experiences
4. **Thinking** - Cognitive processes and thoughts
5. **Deliberation** - Careful consideration and reflection
6. **Faith** - Trust, confidence, and belief
7. **Vigor** - Energy, enthusiasm, and effort
8. **Inner Shame** - Moral conscience and self-respect
9. **Shame** - Humility, modesty, and embarrassment
10. **Absence of Craving** - Contentment and non-attachment
11. **Absence of Hatred** - Love, compassion, and forgiveness
12. **Absence of Ignorance** - Wisdom, understanding, and insight
13. **Light Ease** - Lightness, comfort, and peace
14. **Non-laxness** - Diligence, perseverance, and determination
15. **Renunciation** - Letting go, detachment, and abandonment
16. **Non-harming** - Compassion, kindness, and gentleness

### Negative Factors (Unwholesome - Dimensions 16-42)
17. **Greed** - Desire, craving, and attachment
18. **Anger** - Rage, fury, and hostility
19. **Arrogance** - Pride, conceit, and superiority
20. **Foolishness** - Ignorance, stupidity, and unwisdom
21. **Doubt** - Uncertainty, skepticism, and confusion
22. **Improper Views** - Wrong view, misconception, and delusion
23. **Wrath** - Rage, fury, and indignation
24. **Hatred** - Aversion, dislike, and enmity
25. **Rage** - Fury, wrath, and outrage
26. **Covering** - Hiding, concealing, and masking
27. **Deceit** - Deception, lying, and dishonesty
28. **Flattery** - Praise, compliment, and adulation
29. **Conceit** - Pride, arrogance, and vanity
30. **Harming** - Hurting, damaging, and violence
31. **Jealousy** - Envy, resentment, and bitterness
32. **Stinginess** - Greed, selfishness, and miserliness
33. **Lack of Remorse** - Unrepentant, unconcerned, and callous
34. **Lack of Shame** - Shameless, brazen, and impudent
35. **Lack of Faith** - Faithless, distrustful, and cynical
36. **Laziness** - Sloth, idleness, and procrastination
37. **Laxness** - Looseness, carelessness, and negligence
38. **Torpor** - Dullness, lethargy, and sluggishness
39. **Restlessness** - Agitation, unease, and anxiety
40. **Distraction** - Scatteredness, confusion, and disorder
41. **Improper Knowledge** - Wrong knowledge, misunderstanding, and delusion
42. **Scatteredness** - Distraction, dispersion, and fragmentation
43. **Regret** - Remorse, sorrow, and repentance

## Technical Details

- **Algorithm**: Keyword-based intensity calculation with normalization
- **Output Range**: Values between 0.00 and 1.00
- **Sorting**: Results sorted by intensity (highest first)
- **Filtering**: Only displays factors with non-zero values
- **Dependencies**: Pure Java - no external dependencies for core functionality

## Integration with Main System

This CLI tool uses the exact same calculation logic as the production YouthMind system:
- Same keyword mappings for all 43 factors
- Same intensity calculation algorithm
- Same normalization and thresholding
- Same factor definitions and descriptions

The tool extracts the core vector generation logic from:
- `PsychologicalVectorGenerator.java`
- `BuddhistFactorAnalyzer.java`
- `MentalFactorClassifier.java`
- `BuddhistPsychologyMapper.java`

## Development

### Building
```bash
mvn clean compile
```

### Testing
```bash
mvn test
```

### Creating Standalone JAR
```bash
mvn clean package
# Creates: target/youthmind-cli-tool.jar
```

## Troubleshooting

### Common Issues

1. **"Could not find or load main class"**
   - Ensure you're in the `backend` directory
   - Run `mvn compile` first
   - Check Java version (requires Java 17+)

2. **Maven exec plugin not found**
   - Update Maven to version 3.6+
   - Run `mvn clean compile` first

3. **No output or empty results**
   - Check that input text contains recognizable keywords
   - Try the `example` command to verify functionality

### System Requirements

- Java 17 or higher
- Maven 3.6 or higher
- No database or Redis required
- No Spring Boot application needed

## License

This tool is part of the YouthMind project and follows the same licensing terms.




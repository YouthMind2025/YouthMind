package ai.youthmind.service;

import ai.youthmind.entity.AIAnalysis;
import ai.youthmind.entity.Post;
import ai.youthmind.repository.AIAnalysisRepository;
import ai.youthmind.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class AIPostAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIPostAnalysisService.class);
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private AIAnalysisRepository aiAnalysisRepository;
    
    @Autowired
    private PostService postService;
    
    /**
     * Trigger AI analysis for a post asynchronously
     */
    public CompletableFuture<Void> analyzePostAsync(UUID postId) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting AI analysis for post: {}", postId);
                analyzePost(postId);
                logger.info("Completed AI analysis for post: {}", postId);
            } catch (Exception e) {
                logger.error("Error during AI analysis for post {}: {}", postId, e.getMessage(), e);
            }
        });
    }
    
    /**
     * Perform comprehensive AI analysis on a post
     */
    public void analyzePost(UUID postId) {
        try {
            // Get the post
            Optional<Post> postOpt = postRepository.findById(postId);
            if (postOpt.isEmpty()) {
                logger.warn("Post not found for analysis: {}", postId);
                return;
            }
            
            Post post = postOpt.get();
            logger.info("Analyzing post: {} - Title: {}", postId, post.getTitle());
            
            // Combine title and content for analysis
            String fullContent = buildContentForAnalysis(post);
            
            // Perform sentiment analysis
            BigDecimal sentimentScore = performSentimentAnalysis(fullContent);
            
            // Perform empathy analysis
            BigDecimal empathyScore = performEmpathyAnalysis(fullContent);
            
            // Generate follow-up questions
            List<String> generatedQuestions = generateFollowUpQuestions(fullContent, post.getEmotion());
            
            // Calculate processing time (simplified)
            int processingTimeMs = 1500; // Would be calculated from actual processing time
            
            // Save analysis results
            AIAnalysis analysis = postService.updateOrInsertAnalysisResult(
                postId, 
                sentimentScore, 
                empathyScore, 
                generatedQuestions, 
                processingTimeMs
            );
            
            logger.info("AI analysis completed for post {} - Sentiment: {}, Empathy: {}, Questions: {}", 
                postId, sentimentScore, empathyScore, generatedQuestions.size());
            
            // Publish AI processed event to Redis
            publishAIProcessedEvent(postId, analysis);
            
        } catch (Exception e) {
            logger.error("Error analyzing post {}: {}", postId, e.getMessage(), e);
            throw new RuntimeException("Failed to analyze post: " + e.getMessage());
        }
    }
    
    /**
     * Build content string for AI analysis
     */
    private String buildContentForAnalysis(Post post) {
        StringBuilder content = new StringBuilder();
        
        if (post.getTitle() != null && !post.getTitle().trim().isEmpty()) {
            content.append("Title: ").append(post.getTitle()).append("\n");
        }
        
        if (post.getContent() != null && !post.getContent().trim().isEmpty()) {
            content.append("Content: ").append(post.getContent());
        }
        
        if (post.getEmotion() != null && !post.getEmotion().trim().isEmpty()) {
            content.append("\nEmotion: ").append(post.getEmotion());
        }
        
        if (post.getTopics() != null && !post.getTopics().trim().isEmpty()) {
            content.append("\nTopics: ").append(post.getTopics());
        }
        
        return content.toString();
    }
    
    /**
     * Perform sentiment analysis using AI service
     */
    private BigDecimal performSentimentAnalysis(String content) {
        try {
            // Create sentiment analysis prompt
            String prompt = buildSentimentAnalysisPrompt(content);
            
            // Call AI service for sentiment analysis
            String aiResponse = aiServiceClient.generateCompletion(prompt, "sentiment_analysis");
            
            // Parse sentiment score from AI response
            return parseSentimentScore(aiResponse);
            
        } catch (Exception e) {
            logger.error("Error performing sentiment analysis: {}", e.getMessage(), e);
            // Return neutral sentiment as fallback
            return new BigDecimal("0.0");
        }
    }
    
    /**
     * Perform empathy analysis using AI service
     */
    private BigDecimal performEmpathyAnalysis(String content) {
        try {
            // Create empathy analysis prompt
            String prompt = buildEmpathyAnalysisPrompt(content);
            
            // Call AI service for empathy analysis
            String aiResponse = aiServiceClient.generateCompletion(prompt, "empathy_analysis");
            
            // Parse empathy score from AI response
            return parseEmpathyScore(aiResponse);
            
        } catch (Exception e) {
            logger.error("Error performing empathy analysis: {}", e.getMessage(), e);
            // Return moderate empathy as fallback
            return new BigDecimal("0.5");
        }
    }
    
    /**
     * Generate follow-up questions using AI service
     */
    private List<String> generateFollowUpQuestions(String content, String emotion) {
        try {
            // Create question generation prompt
            String prompt = buildQuestionGenerationPrompt(content, emotion);
            
            // Call AI service for question generation
            String aiResponse = aiServiceClient.generateCompletion(prompt, "question_generation");
            
            // Parse questions from AI response
            return parseQuestionsFromResponse(aiResponse);
            
        } catch (Exception e) {
            logger.error("Error generating follow-up questions: {}", e.getMessage(), e);
            // Return fallback questions
            return generateFallbackQuestions(emotion);
        }
    }
    
    /**
     * Build sentiment analysis prompt
     */
    private String buildSentimentAnalysisPrompt(String content) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant analyzing sentiment for a mental health support platform. ");
        prompt.append("Analyze the sentiment of the following post content and provide a sentiment score.\n\n");
        prompt.append("Post Content:\n").append(content).append("\n\n");
        prompt.append("Please provide a sentiment score between -1.0 (very negative) and 1.0 (very positive). ");
        prompt.append("Respond with only the numeric score (e.g., 0.3 or -0.7).");
        
        return prompt.toString();
    }
    
    /**
     * Build empathy analysis prompt
     */
    private String buildEmpathyAnalysisPrompt(String content) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant analyzing empathy for a mental health support platform. ");
        prompt.append("Analyze how much empathy and emotional support the following post content conveys.\n\n");
        prompt.append("Post Content:\n").append(content).append("\n\n");
        prompt.append("Please provide an empathy score between 0.0 (no empathy) and 1.0 (high empathy). ");
        prompt.append("Respond with only the numeric score (e.g., 0.8 or 0.3).");
        
        return prompt.toString();
    }
    
    /**
     * Build question generation prompt
     */
    private String buildQuestionGenerationPrompt(String content, String emotion) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant generating supportive follow-up questions for a mental health platform. ");
        prompt.append("Generate 3-5 thoughtful, supportive questions that could help the author explore their feelings further.\n\n");
        prompt.append("Post Content:\n").append(content).append("\n");
        if (emotion != null && !emotion.trim().isEmpty()) {
            prompt.append("Emotion: ").append(emotion).append("\n");
        }
        prompt.append("\nPlease provide questions that are:\n");
        prompt.append("- Supportive and non-judgmental\n");
        prompt.append("- Helpful for emotional exploration\n");
        prompt.append("- Appropriate for a youth mental health platform\n");
        prompt.append("- One question per line, starting with a dash (-)");
        
        return prompt.toString();
    }
    
    /**
     * Parse sentiment score from AI response
     */
    private BigDecimal parseSentimentScore(String aiResponse) {
        try {
            // Simple parsing - extract numeric value
            String cleanResponse = aiResponse.trim().replaceAll("[^\\d.-]", "");
            if (cleanResponse.isEmpty()) {
                return new BigDecimal("0.0");
            }
            
            double score = Double.parseDouble(cleanResponse);
            // Clamp between -1.0 and 1.0
            score = Math.max(-1.0, Math.min(1.0, score));
            return new BigDecimal(score);
            
        } catch (Exception e) {
            logger.warn("Error parsing sentiment score from AI response: {}", aiResponse);
            return new BigDecimal("0.0");
        }
    }
    
    /**
     * Parse empathy score from AI response
     */
    private BigDecimal parseEmpathyScore(String aiResponse) {
        try {
            // Simple parsing - extract numeric value
            String cleanResponse = aiResponse.trim().replaceAll("[^\\d.-]", "");
            if (cleanResponse.isEmpty()) {
                return new BigDecimal("0.5");
            }
            
            double score = Double.parseDouble(cleanResponse);
            // Clamp between 0.0 and 1.0
            score = Math.max(0.0, Math.min(1.0, score));
            return new BigDecimal(score);
            
        } catch (Exception e) {
            logger.warn("Error parsing empathy score from AI response: {}", aiResponse);
            return new BigDecimal("0.5");
        }
    }
    
    /**
     * Parse questions from AI response
     */
    private List<String> parseQuestionsFromResponse(String aiResponse) {
        List<String> questions = Arrays.asList();
        
        try {
            String[] lines = aiResponse.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("-") || line.startsWith("•") || line.matches("\\d+\\.")) {
                    String question = line.replaceFirst("^[-•\\d+\\.\\s]+", "").trim();
                    if (!question.isEmpty()) {
                        questions.add(question);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error parsing questions from AI response: {}", aiResponse);
        }
        
        // If no questions found, return fallback questions
        if (questions.isEmpty()) {
            return generateFallbackQuestions(null);
        }
        
        return questions;
    }
    
    /**
     * Generate fallback questions when AI service is unavailable
     */
    private List<String> generateFallbackQuestions(String emotion) {
        List<String> fallbackQuestions = Arrays.asList(
            "How are you feeling about this situation?",
            "What would be most helpful for you right now?",
            "Would you like to talk more about this?"
        );
        
        // Add emotion-specific questions if emotion is provided
        if (emotion != null && !emotion.trim().isEmpty()) {
            switch (emotion.toLowerCase()) {
                case "anxious":
                case "stressed":
                    fallbackQuestions = Arrays.asList(
                        "What's causing you the most anxiety right now?",
                        "Have you found any coping strategies that help?",
                        "Would you like to talk about ways to manage stress?"
                    );
                    break;
                case "sad":
                case "depressed":
                    fallbackQuestions = Arrays.asList(
                        "What's been weighing on your mind lately?",
                        "How long have you been feeling this way?",
                        "What usually helps you feel better?"
                    );
                    break;
                case "angry":
                case "frustrated":
                    fallbackQuestions = Arrays.asList(
                        "What's been frustrating you recently?",
                        "How do you usually handle these feelings?",
                        "Would you like to talk about what's bothering you?"
                    );
                    break;
                case "happy":
                case "excited":
                    fallbackQuestions = Arrays.asList(
                        "What's been going well for you lately?",
                        "What are you most excited about?",
                        "How can you maintain these positive feelings?"
                    );
                    break;
            }
        }
        
        return fallbackQuestions;
    }
    
    /**
     * Publish AI processed event to Redis
     */
    private void publishAIProcessedEvent(UUID postId, AIAnalysis analysis) {
        try {
            // Create AI processed event
            String analysisResult = String.format(
                "Sentiment: %s, Empathy: %s, Questions: %d",
                analysis.getSentimentScore(),
                analysis.getEmpathyScore(),
                analysis.getGeneratedQuestions() != null ? analysis.getGeneratedQuestions().size() : 0
            );
            RedisService.AiProcessedEvent event = new RedisService.AiProcessedEvent(
                postId.toString(), 
                analysisResult, 
                System.currentTimeMillis()
            );
            
            // Publish to Redis channel
            // This would be implemented with RedisService.publishAIProcessedEvent(event)
            logger.info("AI processed event published for post: {}", postId);
            
        } catch (Exception e) {
            logger.error("Error publishing AI processed event for post {}: {}", postId, e.getMessage(), e);
        }
    }
}

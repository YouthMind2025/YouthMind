package ai.youthmind.service;

import ai.youthmind.entity.AIAnalysis;
import ai.youthmind.entity.Post;
import ai.youthmind.dto.PostCreateRequest;
import ai.youthmind.entity.User;
import ai.youthmind.repository.AIAnalysisRepository;
import ai.youthmind.repository.PostRepository;
import ai.youthmind.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class PostService {
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private AIAnalysisRepository aiAnalysisRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Post createPost(UUID userId, PostCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Post post = new Post();
        post.setUser(user);
        post.setContent(request.getContent());
        post.setEmotionTag(request.getEmotionTag());
        post.setAiMetadata(request.getAiMetadata());

        return postRepository.save(post);
    }
    
    /**
     * Update or insert AI analysis result for a post
     */
    public AIAnalysis updateOrInsertAnalysisResult(UUID postId, 
                                                 BigDecimal sentimentScore, 
                                                 BigDecimal empathyScore, 
                                                 List<String> generatedQuestions, 
                                                 Integer processingTimeMs) {
        
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found with id: " + postId);
        }
        
        Post post = postOpt.get();
        
        // Check if analysis already exists
        Optional<AIAnalysis> existingAnalysis = aiAnalysisRepository.findById(postId);
        
        AIAnalysis analysis;
        if (existingAnalysis.isPresent()) {
            // Update existing analysis
            analysis = existingAnalysis.get();
            analysis.setSentimentScore(sentimentScore);
            analysis.setEmpathyScore(empathyScore);
            analysis.setGeneratedQuestions(generatedQuestions);
            analysis.setProcessingTimeMs(processingTimeMs);
        } else {
            // Create new analysis
            analysis = new AIAnalysis();
            analysis.setPost(post);
            analysis.setSentimentScore(sentimentScore);
            analysis.setEmpathyScore(empathyScore);
            analysis.setGeneratedQuestions(generatedQuestions);
            analysis.setProcessingTimeMs(processingTimeMs);
        }
        
        return aiAnalysisRepository.save(analysis);
    }
} 
package ai.youthmind.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIAnalysis {
    
    @Id
    @Column(name = "post_id")
    private UUID postId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    @MapsId
    private Post post;
    
    @Column(name = "sentiment_score", precision = 3, scale = 2)
    private BigDecimal sentimentScore;
    
    @Column(name = "empathy_score", precision = 3, scale = 2)
    private BigDecimal empathyScore;
    
    @Column(name = "generated_questions", columnDefinition = "json")
    @Convert(converter = JsonbListConverter.class)
    private List<String> generatedQuestions;
    
    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;
    
    // Manual setters to ensure compilation
    public void setPost(Post post) { this.post = post; }
    public void setSentimentScore(BigDecimal sentimentScore) { this.sentimentScore = sentimentScore; }
    public void setEmpathyScore(BigDecimal empathyScore) { this.empathyScore = empathyScore; }
    public void setGeneratedQuestions(List<String> generatedQuestions) { this.generatedQuestions = generatedQuestions; }
    public void setProcessingTimeMs(Integer processingTimeMs) { this.processingTimeMs = processingTimeMs; }
} 
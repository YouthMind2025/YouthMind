package ai.youthmind.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Builder class for PostDto to ensure proper construction and validation.
 * 
 * This builder pattern provides:
 * - Type-safe construction
 * - Required field validation
 * - Default value handling
 * - Fluent API for easy use
 * - Future extensibility
 */
public class PostDtoBuilder {
    
    // Core iOS-aligned fields
    private UUID id;
    private int resonance = 0;
    private int view = 0;
    private int collect = 0;
    private String title;
    private String content;
    private String preImg = "";
    private boolean isresonanced = false;
    private boolean isCollected = false;
    private String emotion;
    private LocalDateTime date;
    
    // Backend enhancement fields
    private UUID userId;
    private String topics = "";
    private String aiMetadata = "";
    private boolean flagged = false;
    private String flagReason = "";
    private LocalDateTime flaggedAt;
    private UserDto author;
    private int commentsCount = 0;
    
    /**
     * Set the post ID
     */
    public PostDtoBuilder id(UUID id) {
        this.id = id;
        return this;
    }
    
    /**
     * Set the post ID from string
     */
    public PostDtoBuilder id(String id) {
        if (id != null && !id.trim().isEmpty()) {
            try {
                this.id = UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                // Log warning but continue building
                System.err.println("Invalid UUID format: " + id);
            }
        }
        return this;
    }
    
    /**
     * Set the resonance (likes) count
     */
    public PostDtoBuilder resonance(int resonance) {
        this.resonance = Math.max(0, resonance);
        return this;
    }
    
    /**
     * Set the view count
     */
    public PostDtoBuilder view(int view) {
        this.view = Math.max(0, view);
        return this;
    }
    
    /**
     * Set the collect (saves) count
     */
    public PostDtoBuilder collect(int collect) {
        this.collect = Math.max(0, collect);
        return this;
    }
    
    /**
     * Set the post title
     */
    public PostDtoBuilder title(String title) {
        this.title = title;
        return this;
    }
    
    /**
     * Set the post content
     */
    public PostDtoBuilder content(String content) {
        this.content = content;
        return this;
    }
    
    /**
     * Set the preview image URL
     */
    public PostDtoBuilder preImg(String preImg) {
        this.preImg = preImg != null ? preImg : "";
        return this;
    }
    
    /**
     * Set whether the current user has resonated (liked)
     */
    public PostDtoBuilder isresonanced(boolean isresonanced) {
        this.isresonanced = isresonanced;
        return this;
    }
    
    /**
     * Set whether the current user has collected (saved)
     */
    public PostDtoBuilder isCollected(boolean isCollected) {
        this.isCollected = isCollected;
        return this;
    }
    
    /**
     * Set the emotion tag
     */
    public PostDtoBuilder emotion(String emotion) {
        this.emotion = emotion;
        return this;
    }
    
    /**
     * Set the creation date
     */
    public PostDtoBuilder date(LocalDateTime date) {
        this.date = date;
        return this;
    }
    
    /**
     * Set the creation date to now
     */
    public PostDtoBuilder dateNow() {
        this.date = LocalDateTime.now();
        return this;
    }
    
    /**
     * Set the user ID
     */
    public PostDtoBuilder userId(UUID userId) {
        this.userId = userId;
        return this;
    }
    
    /**
     * Set the topics/tags
     */
    public PostDtoBuilder topics(String topics) {
        this.topics = topics != null ? topics : "";
        return this;
    }
    
    /**
     * Set the AI metadata
     */
    public PostDtoBuilder aiMetadata(String aiMetadata) {
        this.aiMetadata = aiMetadata != null ? aiMetadata : "";
        return this;
    }
    
    /**
     * Set the flagged status
     */
    public PostDtoBuilder flagged(boolean flagged) {
        this.flagged = flagged;
        return this;
    }
    
    /**
     * Set the flag reason
     */
    public PostDtoBuilder flagReason(String flagReason) {
        this.flagReason = flagReason != null ? flagReason : "";
        return this;
    }
    
    /**
     * Set the flagged timestamp
     */
    public PostDtoBuilder flaggedAt(LocalDateTime flaggedAt) {
        this.flaggedAt = flaggedAt;
        return this;
    }
    
    /**
     * Set the author information
     */
    public PostDtoBuilder author(UserDto author) {
        this.author = author;
        return this;
    }
    
    /**
     * Set the comments count
     */
    public PostDtoBuilder commentsCount(int commentsCount) {
        this.commentsCount = Math.max(0, commentsCount);
        return this;
    }
    
    /**
     * Build a PostDto with validation
     * @return Validated PostDto
     * @throws IllegalStateException if required fields are missing
     */
    public PostDto build() {
        validateRequiredFields();
        return new PostDto(
            id, resonance, view, collect, title, content, preImg,
            isresonanced, isCollected, emotion, date, userId, topics,
            aiMetadata, flagged, flagReason, flaggedAt, author, commentsCount
        );
    }
    
    /**
     * Build a PostDto without validation (for internal use)
     * @return PostDto
     */
    public PostDto buildUnsafe() {
        return new PostDto(
            id, resonance, view, collect, title, content, preImg,
            isresonanced, isCollected, emotion, date, userId, topics,
            aiMetadata, flagged, flagReason, flaggedAt, author, commentsCount
        );
    }
    
    /**
     * Validate that all required fields are present
     * @throws IllegalStateException if validation fails
     */
    private void validateRequiredFields() {
        if (id == null) {
            throw new IllegalStateException("Post ID is required");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalStateException("Post title is required");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalStateException("Post content is required");
        }
        if (emotion == null || emotion.trim().isEmpty()) {
            throw new IllegalStateException("Post emotion is required");
        }
        if (date == null) {
            throw new IllegalStateException("Post creation date is required");
        }
    }
    
    /**
     * Create a builder with default values
     * @return PostDtoBuilder instance
     */
    public static PostDtoBuilder create() {
        return new PostDtoBuilder();
    }
    
    /**
     * Create a builder from an existing PostDto
     * @param postDto Existing PostDto
     * @return PostDtoBuilder instance
     */
    public static PostDtoBuilder from(PostDto postDto) {
        PostDtoBuilder builder = new PostDtoBuilder();
        if (postDto != null) {
            builder.id(postDto.getId())
                   .resonance(postDto.getResonance())
                   .view(postDto.getView())
                   .collect(postDto.getCollect())
                   .title(postDto.getTitle())
                   .content(postDto.getContent())
                   .preImg(postDto.getPreImg())
                   .isresonanced(postDto.isIsresonanced())
                   .isCollected(postDto.isCollected())
                   .emotion(postDto.getEmotion())
                   .date(postDto.getDate())
                   .userId(postDto.getUserId())
                   .topics(postDto.getTopics())
                   .aiMetadata(postDto.getAiMetadata())
                   .flagged(postDto.isFlagged())
                   .flagReason(postDto.getFlagReason())
                   .flaggedAt(postDto.getFlaggedAt())
                   .author(postDto.getAuthor())
                   .commentsCount(postDto.getCommentsCount());
        }
        return builder;
    }
} 
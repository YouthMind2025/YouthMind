package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Article Data Transfer Object for website integration.
 * 
 * This DTO aligns with the existing article structure used by the website
 * while providing backend capabilities for article management.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Article data transfer object for content management")
public class ArticleDto {
    
    @Schema(description = "Unique article identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "Article title", example = "The Future of AI in Education")
    private String title;
    
    @Schema(description = "Article content in HTML format", example = "<h1>Introduction</h1><p>This article discusses...</p>")
    private String content;
    
    @Schema(description = "Article introduction/summary", example = "Exploring how AI is transforming education...")
    private String intro;
    
    @Schema(description = "Cover image URL or base64 data", example = "cover1.jpg")
    private String coverImage;
    
    @Schema(description = "Article publication status", example = "true")
    private boolean published;
    
    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last updated timestamp", example = "2024-01-15T11:00:00")
    private LocalDateTime updatedAt;
    
    // Website compatibility methods
    
    /**
     * Get the date formatted for website display
     * @return Formatted date string (YYYY format for compatibility)
     */
    public String getDate() {
        if (createdAt != null) {
            return createdAt.format(DateTimeFormatter.ofPattern("yyyy"));
        }
        return "2024";
    }
    
    /**
     * Get the file name for backward compatibility
     * @return Generated file name based on title
     */
    public String getFile() {
        if (title != null) {
            return title.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", " ").trim() + ".docx";
        }
        return "article.docx";
    }
    
    /**
     * Get cover image name for website compatibility
     * @return Cover image filename or default
     */
    public String getCover() {
        return coverImage != null ? coverImage : "default-cover.jpg";
    }
    
    /**
     * Check if the article has all required fields
     * @return True if all required fields are present
     */
    public boolean hasRequiredFields() {
        return title != null && !title.trim().isEmpty() && 
               content != null && !content.trim().isEmpty();
    }
    
    /**
     * Get a short summary for display
     * @return Truncated intro or content
     */
    public String getSummary() {
        if (intro != null && !intro.trim().isEmpty()) {
            return intro.length() > 150 ? intro.substring(0, 150) + "..." : intro;
        }
        if (content != null) {
            // Strip HTML tags for summary
            String plainText = content.replaceAll("<[^>]*>", "");
            return plainText.length() > 150 ? plainText.substring(0, 150) + "..." : plainText;
        }
        return "";
    }
    
    /**
     * Explicit getter for published field to resolve Lombok issues
     * @return True if article is published
     */
    public boolean isPublished() {
        return published;
    }
}

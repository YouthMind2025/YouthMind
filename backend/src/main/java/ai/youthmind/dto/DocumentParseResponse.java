package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for document parsing results
 */
@Data
@NoArgsConstructor
@Schema(description = "Response containing parsed document content")
public class DocumentParseResponse {
    
    // Constructor without images for backward compatibility
    public DocumentParseResponse(String title, String content, String intro, String filename, long fileSize) {
        this.title = title;
        this.content = content;
        this.intro = intro;
        this.filename = filename;
        this.fileSize = fileSize;
        this.images = List.of(); // Empty list
        this.imageMetadata = Map.of(); // Empty map
    }
    
    // Constructor with images
    public DocumentParseResponse(String title, String content, String intro, String filename, long fileSize, 
                               List<String> images, Map<String, Map<String, Object>> imageMetadata) {
        this.title = title;
        this.content = content;
        this.intro = intro;
        this.filename = filename;
        this.fileSize = fileSize;
        this.images = images != null ? images : List.of();
        this.imageMetadata = imageMetadata != null ? imageMetadata : Map.of();
    }
    
    @Schema(description = "Extracted title from document", example = "Article Title")
    private String title;
    
    @Schema(description = "Extracted content in HTML format", example = "<p>Article content...</p>")
    private String content;
    
    @Schema(description = "Extracted introduction/summary", example = "Brief introduction...")
    private String intro;
    
    @Schema(description = "Original filename", example = "document.docx")
    private String filename;
    
    @Schema(description = "File size in bytes", example = "12345")
    private long fileSize;
    
    @Schema(description = "Extracted images as base64 data URLs", example = "[\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...\"]")
    private List<String> images;
    
    @Schema(description = "Image metadata (filename, size, type)", example = "{\"image1.png\": {\"size\": 1024, \"type\": \"png\"}}")
    private Map<String, Map<String, Object>> imageMetadata;
}

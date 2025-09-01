package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for document parsing results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing parsed document content")
public class DocumentParseResponse {
    
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
}

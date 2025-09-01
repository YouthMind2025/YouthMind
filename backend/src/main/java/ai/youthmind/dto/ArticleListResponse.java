package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for article lists with pagination
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing a list of articles with pagination info")
public class ArticleListResponse {
    
    @Schema(description = "List of articles")
    private List<ArticleDto> articles;
    
    @Schema(description = "Pagination information")
    private PaginationDto pagination;
    
    @Schema(description = "Total number of articles", example = "25")
    private long totalElements;
    
    @Schema(description = "Whether this is the last page", example = "false")
    private boolean last;
}

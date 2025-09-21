package ai.youthmind.dto;

import ai.youthmind.entity.CommentReport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for reporting a comment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentReportRequest {
    
    @NotNull(message = "Report reason is required")
    private CommentReport.ReportReason reportReason;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    /**
     * Validate the request
     */
    public boolean isValid() {
        return reportReason != null;
    }
}

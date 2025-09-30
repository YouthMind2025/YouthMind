package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContentTypeStats {
    private Long total;
    private Long pending;
    private Long approved;
    private Long rejected;
}

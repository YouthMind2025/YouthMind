package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftUpdateRequest {
    private String title;
    private String content;
    private String emotion;
    private List<String> topics;
}

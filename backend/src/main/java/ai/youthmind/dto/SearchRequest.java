package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    private String q;
    private String emotion;
    private String sort;
    private int page = 1;
    private int limit = 10;
}

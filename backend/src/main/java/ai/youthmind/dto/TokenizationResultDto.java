package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenizationResultDto {
    private int[] tokenIds;
    private int[] attentionMask;
    private List<String> tokens;
    private String originalText;
    private String processedText;
    private boolean truncated;
    private int sequenceLength;

    public void setTokenIds(int[] tokenIds) {
        this.tokenIds = tokenIds;
        this.sequenceLength = tokenIds != null ? tokenIds.length : 0;
    }
}

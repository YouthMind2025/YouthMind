package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenizerDto {
    private Map<String, Integer> vocab;
    private Map<String, Object> config;
}

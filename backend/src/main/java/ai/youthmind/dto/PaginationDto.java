package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDto {
    private int page;
    private int limit;
    private int total;
    private boolean hasNext;
    private boolean hasPrevious;
    private int totalPages;
    private int totalItems;
}

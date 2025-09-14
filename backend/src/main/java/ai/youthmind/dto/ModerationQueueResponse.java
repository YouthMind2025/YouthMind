package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModerationQueueResponse {
    List<ModerationQueueItem> items;
    Long totalItems;
    Integer totalPages;
    Integer currentPage;
    Integer pageSize;
}

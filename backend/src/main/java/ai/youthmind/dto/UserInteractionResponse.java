package ai.youthmind.dto;

import java.util.List;

/**
 * Response DTO for user interactions
 */
public class UserInteractionResponse {
    private List<UserInteractionItem> interactions;
    private int totalCount;
    private int currentPage;
    private int totalPages;
    private boolean hasNext;

    // Constructors
    public UserInteractionResponse() {}

    public UserInteractionResponse(List<UserInteractionItem> interactions, int totalCount, int currentPage, int totalPages, boolean hasNext) {
        this.interactions = interactions;
        this.totalCount = totalCount;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
    }

    // Getters and Setters
    public List<UserInteractionItem> getInteractions() {
        return interactions;
    }

    public void setInteractions(List<UserInteractionItem> interactions) {
        this.interactions = interactions;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
}

package ai.youthmind.dto;

import java.util.List;

/**
 * Response DTO for user shared posts
 */
public class UserSharedResponse {
    private List<UserSharedItem> sharedItems;
    private int totalCount;
    private int currentPage;
    private int totalPages;
    private boolean hasNext;

    // Constructors
    public UserSharedResponse() {}

    public UserSharedResponse(List<UserSharedItem> sharedItems, int totalCount, int currentPage, int totalPages, boolean hasNext) {
        this.sharedItems = sharedItems;
        this.totalCount = totalCount;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
    }

    // Getters and Setters
    public List<UserSharedItem> getSharedItems() {
        return sharedItems;
    }

    public void setSharedItems(List<UserSharedItem> sharedItems) {
        this.sharedItems = sharedItems;
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

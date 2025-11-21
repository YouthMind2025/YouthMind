package ai.youthmind.dto;

import java.util.List;

/**
 * Response DTO for user notifications
 */
public class NotificationResponse {
    private List<NotificationItem> notifications;
    private int totalCount;
    private int currentPage;
    private int totalPages;
    private boolean hasNext;

    // Constructors
    public NotificationResponse() {}

    public NotificationResponse(List<NotificationItem> notifications, int totalCount, int currentPage, int totalPages, boolean hasNext) {
        this.notifications = notifications;
        this.totalCount = totalCount;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
    }

    // Getters and Setters
    public List<NotificationItem> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<NotificationItem> notifications) {
        this.notifications = notifications;
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

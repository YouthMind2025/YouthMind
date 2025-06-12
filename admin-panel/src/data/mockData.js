
//export const flagedmockData = [
//    "flagged_posts": [
//        {
//            "post_id": "post_12345",
//            "flag_reason": "potential_crisis",
//            "flagged_at": "2024-01-15T10:30:00Z",
//            "ai_confidence": 0.95,
//            "content_preview": "I can't take this anymore...",
//            "status": "pending_review"
//        }
//    ],
//    "total_pending": 3
//];

export const flagedmockData = {
    "flagged_posts": [
        {
            "post_id": "post_12345",
            "flag_reason": "potential_crisis",
            "flagged_at": "2024-01-15T10:30:00Z",
            "ai_confidence": 0.95,
            "content_preview": "I can't take this anymore...",
            "status": "pending_review"
        }
    ],
    "total_pending": 3
};

export const systemmonitoringmockData = {
        "status": "healthy",
        "services": {
          "database": "up",
          "redis": "up",
          "ai_service": "up"
        },
        "metrics": {
          "active_users": 1247,
          "posts_today": 89,
          "ai_response_time_avg": "450ms"
        }
}
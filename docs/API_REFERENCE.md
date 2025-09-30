# API Reference Documentation
## YouthMind Backend & AI Services

*Complete API specification for YouthMind/SaySomething application services*

---

## Base URLs

| **Service** | **Environment** | **Base URL** | **Port** |
|-------------|-----------------|--------------|----------|
| Backend API | Development | `http://localhost:8080` | 8080 |
| Backend API | Production | `https://api.youthmind.dev` | 443 |
| AI Services | Development | `http://localhost:8001` | 8001 |
| AI Services | Production | `https://ai.youthmind.dev` | 443 |
| Admin Panel | Development | `http://localhost:3000` | 3000 |
| Admin Panel | Production | `https://admin.youthmind.dev` | 443 |

---

## Authentication & Security

### **Anonymous JWT Tokens**
All API requests require a JWT token for anonymous user identification:

```http
Authorization: Bearer <jwt_token>
```

### **Token Generation**
```http
POST /auth/anonymous
Content-Type: application/json

{
  "device_uuid": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 2592000,
  "user_id": "anonymous_550e8400"
}
```

---

## Backend API Endpoints

### **Posts Management**

#### Create New Post
```http
POST /api/v1/posts
Content-Type: application/json
Authorization: Bearer <token>

{
  "content": "Feeling overwhelmed with school lately...",
  "emotion_tags": ["stress", "anxiety"],
  "is_anonymous": true
}
```

**Response:**
```json
{
  "id": "post_12345",
  "content": "Feeling overwhelmed with school lately...",
  "emotion_tags": ["stress", "anxiety"],
  "created_at": "2024-01-15T10:30:00Z",
  "author_id": "anonymous_550e8400",
  "understanding_count": 0,
  "ai_response_pending": true
}
```

#### Retrieve Feed
```http
GET /api/v1/feed?page=0&size=20&sort_by=recent
Authorization: Bearer <token>
```

**Response:**
```json
{
  "posts": [
    {
      "id": "post_12345",
      "content": "Feeling overwhelmed with school lately...",
      "emotion_tags": ["stress", "anxiety"],
      "created_at": "2024-01-15T10:30:00Z",
      "understanding_count": 5,
      "ai_summary": "The author is experiencing academic stress...",
      "has_user_understood": false
    }
  ],
  "pagination": {
    "current_page": 0,
    "total_pages": 25,
    "total_elements": 487,
    "has_next": true
  }
}
```

#### Get Post Details
```http
GET /api/v1/posts/{post_id}
Authorization: Bearer <token>
```

**Response:**
```json
{
  "id": "post_12345",
  "content": "Feeling overwhelmed with school lately...",
  "emotion_tags": ["stress", "anxiety"],
  "created_at": "2024-01-15T10:30:00Z",
  "understanding_count": 5,
  "ai_response": {
    "summary": "The author is experiencing academic stress...",
    "suggested_questions": [
      "What specific aspects of school are causing the most stress?",
      "Have you tried any stress management techniques before?"
    ],
    "empathy_score": 0.85
  },
  "understanding_users": ["anonymous_user1", "anonymous_user2"]
}
```

#### Express Understanding
```http
POST /api/v1/posts/{post_id}/understand
Authorization: Bearer <token>
```

**Response:**
```json
{
  "success": true,
  "new_understanding_count": 6,
  "user_already_understood": false
}
```

---

### **User Interaction**

#### Get User's Posts History
```http
GET /api/v1/users/me/posts?page=0&size=10
Authorization: Bearer <token>
```

**Response:**
```json
{
  "posts": [
    {
      "id": "post_12345",
      "content": "Feeling overwhelmed with school lately...",
      "created_at": "2024-01-15T10:30:00Z",
      "understanding_count": 5,
      "ai_response_received": true
    }
  ],
  "emotional_timeline": [
    {
      "date": "2024-01-15",
      "dominant_emotion": "stress",
      "sentiment_score": -0.3
    }
  ]
}
```

#### Save Draft Post
```http
POST /api/v1/users/me/drafts
Content-Type: application/json
Authorization: Bearer <token>

{
  "content": "Work in progress post...",
  "emotion_tags": ["contemplation"]
}
```

**Response:**
```json
{
  "draft_id": "draft_789",
  "saved_at": "2024-01-15T10:30:00Z",
  "content": "Work in progress post...",
  "emotion_tags": ["contemplation"]
}
```

---

## AI Services API

### **Question Generation**

#### Generate Personalized Questions
```http
POST /ai/v1/questions/generate
Content-Type: application/json

{
  "post_content": "Feeling overwhelmed with school lately...",
  "user_context": {
    "previous_emotions": ["stress", "anxiety"],
    "engagement_level": "medium"
  }
}
```

**Response:**
```json
{
  "questions": [
    {
      "text": "What specific aspects of school are causing the most stress?",
      "category": "exploration",
      "relevance_score": 0.92
    },
    {
      "text": "Have you found any healthy ways to cope with academic pressure?",
      "category": "coping_strategies",
      "relevance_score": 0.88
    }
  ],
  "generation_time_ms": 245
}
```

### **Emotional Analysis**

#### Analyze Post Sentiment
```http
POST /ai/v1/analyze/sentiment
Content-Type: application/json

{
  "text": "Feeling overwhelmed with school lately...",
  "include_emotions": true,
  "include_empathy_score": true
}
```

**Response:**
```json
{
  "sentiment": {
    "polarity": -0.3,
    "subjectivity": 0.7,
    "classification": "negative"
  },
  "emotions": [
    {
      "label": "stress",
      "confidence": 0.89
    },
    {
      "label": "anxiety",
      "confidence": 0.76
    }
  ],
  "empathy_score": 0.85,
  "crisis_indicators": {
    "level": "low",
    "requires_intervention": false
  }
}
```

### **Content Summarization**

#### Generate Post Summary
```http
POST /ai/v1/summarize
Content-Type: application/json

{
  "text": "Feeling overwhelmed with school lately...",
  "summary_length": "short",
  "focus": "emotional_state"
}
```

**Response:**
```json
{
  "summary": "The author is experiencing academic stress and feeling overwhelmed by school responsibilities.",
  "key_themes": ["academic_pressure", "emotional_overwhelm"],
  "confidence_score": 0.91,
  "word_count": 15
}
```

---

## Administrative API

### **Content Moderation**

#### Get Flagged Posts
```http
GET /admin/v1/posts/flagged?status=pending
Authorization: Bearer <admin_token>
```

**Response:**
```json
{
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
}
```

#### Moderate Post
```http
PUT /admin/v1/posts/{post_id}/moderate
Content-Type: application/json
Authorization: Bearer <admin_token>

{
  "action": "approve",
  "moderator_notes": "False positive - regular stress expression",
  "follow_up_required": false
}
```

### **System Monitoring**

#### Get System Health
```http
GET /admin/v1/system/health
Authorization: Bearer <admin_token>
```

**Response:**
```json
{
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
```

---

## Error Handling

### **Standard Error Response**
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Post content cannot be empty",
    "details": {
      "field": "content",
      "rejected_value": ""
    },
    "timestamp": "2024-01-15T10:30:00Z"
  }
}
```

### **Common Error Codes**

| **HTTP Status** | **Error Code** | **Description** |
|-----------------|----------------|-----------------|
| 400 | `VALIDATION_ERROR` | Request validation failed |
| 401 | `UNAUTHORIZED` | Invalid or missing authentication token |
| 403 | `FORBIDDEN` | Insufficient permissions |
| 404 | `NOT_FOUND` | Resource not found |
| 429 | `RATE_LIMITED` | Too many requests |
| 500 | `INTERNAL_ERROR` | Server error |
| 503 | `SERVICE_UNAVAILABLE` | AI service temporarily unavailable |

---

## Rate Limiting

### **API Rate Limits**

| **Endpoint Category** | **Limit** | **Window** |
|-----------------------|-----------|------------|
| Post Creation | 10 requests | 15 minutes |
| Feed Retrieval | 100 requests | 5 minutes |
| Understanding Votes | 50 requests | 5 minutes |
| AI Services | 20 requests | 10 minutes |

### **Rate Limit Headers**
```http
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1642252800
```

---

## WebSocket Connections

### **Real-time Updates**
```javascript
// Connect to real-time updates
const ws = new WebSocket('wss://api.youthmind.dev/ws/updates');

ws.onmessage = function(event) {
  const update = JSON.parse(event.data);
  if (update.type === 'new_understanding') {
    // Handle real-time understanding count update
    updatePostUnderstanding(update.post_id, update.new_count);
  }
};
```

---

## SDK Examples

### **iOS Swift Example**
```swift
import Foundation

class YouthMindAPI {
    private let baseURL = "https://api.youthmind.dev"
    private var authToken: String?
    
    func createPost(content: String, emotionTags: [String]) async throws -> Post {
        let url = URL(string: "\(baseURL)/api/v1/posts")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(authToken!)", forHTTPHeaderField: "Authorization")
        
        let body = PostCreateRequest(content: content, emotionTags: emotionTags, isAnonymous: true)
        request.httpBody = try JSONEncoder().encode(body)
        
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(Post.self, from: data)
    }
}
```

### **JavaScript Example**
```javascript
class YouthMindClient {
  constructor(baseURL = 'https://api.youthmind.dev', token = null) {
    this.baseURL = baseURL;
    this.token = token;
  }
  
  async createPost(content, emotionTags) {
    const response = await fetch(`${this.baseURL}/api/v1/posts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.token}`
      },
      body: JSON.stringify({
        content,
        emotion_tags: emotionTags,
        is_anonymous: true
      })
    });
    
    return response.json();
  }
}
```

---

*This API reference provides comprehensive documentation for integrating with YouthMind's backend services and AI capabilities.* 
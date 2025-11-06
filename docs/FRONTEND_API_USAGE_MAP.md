# Frontend API Usage Mapping

**Document Version**: 1.0  
**Last Updated**: 2024  
**Purpose**: Map which API endpoints are used by which frontend applications

---

## Overview

This document maps all API endpoint usage across the YouthMind frontend applications (iOS app and Admin panel) to help identify actively used vs. unused endpoints.

### Applications

1. **iOS App** (`ios-app/SaySomething/`) - Swift/SwiftUI mobile application
2. **Admin Panel** (`admin-panel/src/`) - React TypeScript web application
3. **Website** (`Youthmind-website/`) - Public-facing website

---

## iOS App API Usage

### API Client Files
- `ios-app/SaySomething/Services/AuthAPI.swift`
- `ios-app/SaySomething/Services/PostAPI.swift`
- `ios-app/SaySomething/Services/FeedAPI.swift`
- `ios-app/SaySomething/Services/ConversationAPI.swift`
- `ios-app/SaySomething/Services/AIChatAPI.swift`
- `ios-app/SaySomething/Services/RVectorAPI.swift`

### Authentication Endpoints

| Backend Endpoint | iOS Method | File | Usage |
|-----------------|------------|------|-------|
| `POST /v1/auth/register` | `AuthAPI.register()` | AuthAPI.swift | User registration with OTP |
| `POST /v1/auth/login` | `AuthAPI.login()` | AuthAPI.swift | User login |
| `POST /v1/auth/send-otp` | Not directly called | - | Called via registration flow |

**Note**: iOS app does not use:
- `/v1/auth/refresh-token` (seems to rely on long-lived tokens)
- `/v1/auth/logout` (local logout only)
- `/v1/auth/admin/*` (admin endpoints)

---

### Post & Feed Endpoints

| Backend Endpoint | iOS Method | File | Usage |
|-----------------|------------|------|-------|
| `GET /v1/posts` | `PostAPI.fetchPosts()` | PostAPI.swift | Get all posts |
| `POST /v1/posts/create` | `PostAPI.createPost()` | PostAPI.swift | Create new post |
| `PUT /v1/posts/{id}/resonance` | `PostAPI.toggleResonance()` | PostAPI.swift | Like/unlike post |
| `PUT /v1/posts/{id}/collect` | `PostAPI.toggleCollection()` | PostAPI.swift | Save/unsave post |
| `POST /v1/posts/{id}/view` | `PostAPI.trackPostView()` | PostAPI.swift | Track post view |
| `GET /v1/posts/search` | `PostAPI.searchPosts()` | PostAPI.swift | Search posts |
| `GET /v1/posts/search/advanced` | `PostAPI.advancedSearch()` | PostAPI.swift | Advanced search |
| `GET /v1/feed/blocks` | `FeedAPI.fetchFeedBlock()` | FeedAPI.swift | Fetch feed blocks |
| `POST /v1/feed/scroll` | `FeedAPI.notifyScroll()` | FeedAPI.swift | Notify scroll event |
| `GET /v1/feed/status` | `FeedAPI.getFeedStatus()` | FeedAPI.swift | Get feed status |

**Not Used by iOS**:
- `GET /v1/posts/feed` (uses `/v1/feed/blocks` instead)
- `GET /v1/posts/by-user/{userId}` (likely in user profile, not yet implemented)
- `PUT /v1/posts/{id}` (post editing not implemented)
- `DELETE /v1/posts/{id}` (post deletion not implemented)
- `POST /v1/posts/{id}/view/duration` (not tracking duration yet)
- `POST /v1/posts/{id}/view/start` (deprecated)
- `POST /v1/posts/{id}/view/end` (deprecated)
- `GET /v1/posts/{id}/view-stats` (analytics not shown in app)
- `POST /v1/posts/{id}/analyze-psychological` (internal only)
- `GET /v1/posts/{id}/psychological-metadata` (internal only)

---

### Comment Endpoints

**Status**: Not yet implemented in iOS app

All comment endpoints are defined in backend but iOS app doesn't have comment feature implemented yet:
- `GET /v1/posts/{postId}/comments`
- `POST /v1/posts/{postId}/comments`
- `POST /v1/posts/comments/{commentId}/replies`
- `DELETE /v1/posts/comments/{commentId}`
- `PUT /v1/posts/comments/{commentId}/like`
- `POST /v1/posts/comments/{commentId}/report`

**Recommendation**: Implement comment feature in iOS or remove these endpoints if not planned.

---

### User Profile Endpoints

**Status**: Partially implemented

| Backend Endpoint | iOS Usage | Status |
|-----------------|-----------|--------|
| `GET /v1/user/profile` | Not found | Not implemented |
| `PUT /v1/user/profile` | Not found | Not implemented |
| `GET /v1/user/posts` | Not found | Not implemented |
| `GET /v1/user/liked-posts` | Not found | Not implemented |
| `GET /v1/user/saved-posts` | Not found | Not implemented |
| `GET /v1/user/statistics` | Not found | Not implemented |
| `POST /v1/user/avatar` | Not found | Not implemented |
| `DELETE /v1/user/account` | Not found | Not implemented |

**Recommendation**: Either implement user profile in iOS or these endpoints remain unused.

---

### Search & Discovery Endpoints

| Backend Endpoint | iOS Method | File | Usage |
|-----------------|------------|------|-------|
| `GET /v1/search/posts` | `PostAPI.searchPosts()` | PostAPI.swift | Basic search |
| `GET /v1/search/posts/advanced` | `PostAPI.advancedSearch()` | PostAPI.swift | Advanced search |

**Not Used**:
- `GET /v1/search/posts/emotion/{emotion}` (emotion filtering not in UI)
- `GET /v1/trending` (trending not implemented)
- `GET /v1/leaderboard` (leaderboard not implemented)
- `GET /v1/suggestions` (AI suggestions not implemented)
- `GET /v1/trending-topics` (trending topics not implemented)

---

### Notification Endpoints

**Status**: Not implemented in iOS app

All notification endpoints are unused by iOS:
- `GET /v1/notifications`
- `POST /v1/notifications/{notificationId}/read`
- `POST /v1/notifications/read-all`

**Recommendation**: Implement notifications in iOS or remove these endpoints.

---

### Conversation Endpoints

**Status**: Implemented

| Backend Endpoint | iOS Method | File | Usage |
|-----------------|------------|------|-------|
| `GET /v1/conversations` | `ConversationAPI.getConversations()` | ConversationAPI.swift | Get conversations |
| `POST /v1/conversations` | `ConversationAPI.createConversation()` | ConversationAPI.swift | Create conversation |
| `GET /v1/conversations/{id}` | `ConversationAPI.getConversation()` | ConversationAPI.swift | Get single conversation |
| `POST /v1/conversations/{id}/messages` | `ConversationAPI.sendMessage()` | ConversationAPI.swift | Send message |
| `DELETE /v1/conversations/{id}` | `ConversationAPI.deleteConversation()` | ConversationAPI.swift | Delete conversation |

**Note**: Conversation feature is actively used.

---

### AI Chat Endpoints

**Status**: Implemented

| Backend Endpoint | iOS Method | File | Usage |
|-----------------|------------|------|-------|
| `POST /v1/ai-chat/send-message` | `AIChatAPI.sendMessage()` | AIChatAPI.swift | Send AI chat message |

**Not Used**:
- `GET /v1/ai-chat/personality-profile` (not shown in UI)
- `PUT /v1/ai-chat/personality-profile` (not editable in UI)
- `POST /v1/ai-chat/test-safety-filter` (testing endpoint)

---

### Draft Endpoints

**Status**: Not implemented in iOS app

All draft endpoints are unused:
- `GET /v1/drafts`
- `POST /v1/drafts`
- `PUT /v1/drafts/{id}`
- `DELETE /v1/drafts/{id}`
- `POST /v1/drafts/{id}/publish`

**Recommendation**: Implement drafts feature in iOS or remove these endpoints.

---

### R-Vector Endpoints

**Status**: Partially implemented

| Backend Endpoint | iOS Method | File | Usage |
|-----------------|------------|------|-------|
| `POST /v1/posts/{id}/view/duration` | `RVectorAPI.updateRVector()` | RVectorAPI.swift | Update R-vector |

**Note**: iOS tracks post view duration which updates R-vector on backend.

---

## Admin Panel API Usage

### API Client File
- `admin-panel/src/services/api.ts`

### Authentication Endpoints

| Backend Endpoint | Admin Method | Usage |
|-----------------|--------------|-------|
| `POST /v1/auth/admin/login` | `authApi.adminLogin()` | Admin login |
| `GET /v1/auth/test` | `authApi.testAuth()` | Health check |

---

### User Management Endpoints

| Backend Endpoint | Admin Method | Usage |
|-----------------|--------------|-------|
| `GET /api/users` | `usersApi.getAllUsers()` | Get all users |
| `GET /api/users/{id}` | `usersApi.getUserById()` | Get user by ID |
| `GET /api/users/email/{email}` | `usersApi.getUserByEmail()` | Get user by email |
| `DELETE /api/users/{id}` | `usersApi.deleteUser()` | Delete user |

---

### Post Management Endpoints

| Backend Endpoint | Admin Method | Usage |
|-----------------|--------------|-------|
| `GET /v1/posts` | `postsApi.getPosts()` | Get posts with pagination |
| `GET /v1/posts/{id}` | `postsApi.getPost()` | Get single post |
| `POST /v1/posts/create` | `postsApi.createPost()` | Create post (admin) |
| `DELETE /admin/v1/posts/delete/{postId}` | `postsApi.deletePost()` | Delete any post |
| `GET /admin/v1/posts/flagged` | `postsApi.getFlaggedPosts()` | Get flagged posts |
| `POST /admin/v1/posts/flag/{postId}` | `postsApi.flagPost()` | Flag post |
| `POST /admin/v1/posts/unflag/{postId}` | `postsApi.unflagPost()` | Unflag post |

---

### Article Management Endpoints

| Backend Endpoint | Admin Method | Usage |
|-----------------|--------------|-------|
| `GET /v1/articles` | `articlesApi.getPublishedArticles()` | Get published articles |
| `GET /v1/articles/admin` | `articlesApi.getArticles()` | Get all articles (admin) |
| `GET /v1/articles/view/{id}` | `articlesApi.getArticle()` | Get single article |
| `POST /v1/articles/create` | `articlesApi.createArticle()` | Create article |
| `PUT /v1/articles/edit/{id}` | `articlesApi.updateArticle()` | Update article |
| `DELETE /v1/articles/delete/{id}` | `articlesApi.deleteArticle()` | Delete article |
| `POST /v1/articles/manage/{id}/toggle-publish` | `articlesApi.togglePublishStatus()` | Toggle publish |
| `GET /v1/articles/search` | `articlesApi.searchArticles()` | Search articles |
| `POST /v1/articles/upload-docx` | `articlesApi.uploadDocx()` | Upload Word doc |

---

### Psychological Analysis Endpoints

| Backend Endpoint | Admin Method | Usage |
|-----------------|--------------|-------|
| `GET /api/ai/psychology/state/{userId}` | `psychologicalApi.getPsychologicalState()` | Get psychological state |
| `POST /api/ai/psychology/chat/{userId}` | `psychologicalApi.generatePsychologicalResponse()` | Generate response |
| `POST /api/ai/psychology/analyze/{userId}` | `psychologicalApi.analyzeMessage()` | Analyze message |
| `GET /api/ai/psychology/health` | `psychologicalApi.getHealth()` | Health check |
| `GET /api/ai/psychology/batch/statistics` | `psychologicalApi.getBatchStatistics()` | Batch stats |
| `GET /api/ai/psychology/cache/statistics` | `psychologicalApi.getCacheStatistics()` | Cache stats |
| `DELETE /api/ai/psychology/cache/{userId}` | `psychologicalApi.invalidateUserCache()` | Invalidate cache |
| `POST /api/ai/psychology/cache/warmup/{userId}` | `psychologicalApi.warmUpCache()` | Warm up cache |

---

### System Health Endpoints

| Backend Endpoint | Admin Method | Usage |
|-----------------|--------------|-------|
| `GET /actuator/health` | `systemApi.getHealth()` | System health |
| `GET /actuator/health/database` | `systemApi.getDatabaseHealth()` | Database health |
| `GET /actuator/health/redis` | `systemApi.getRedisHealth()` | Redis health |
| `GET /actuator/health/liveness` | `systemApi.getLivenessHealth()` | Liveness probe |
| `GET /actuator/health/readiness` | `systemApi.getReadinessHealth()` | Readiness probe |

---

### R-Vector Management Endpoints

| Backend Endpoint | Admin Method | Usage |
|-----------------|--------------|-------|
| `GET /v1/recommendation/rvector` | `rVectorApi.getRVector()` | Get R vector |
| `GET /v1/recommendation/rvector/history` | `rVectorApi.getRVectorHistory()` | Get R vector history |
| `POST /v1/recommendation/rvector/reset` | `rVectorApi.resetRVector()` | Reset R vector |

---

### Recommendation Analytics Endpoints

| Backend Endpoint | Admin Method | Usage |
|-----------------|--------------|-------|
| `GET /v1/analytics/recommendation/performance` | `recommendationApi.getPerformanceMetrics()` | Performance metrics |
| `GET /v1/analytics/recommendation/emotion-distribution` | `recommendationApi.getEmotionDistribution()` | Emotion distribution |

---

### Admin-Specific Management

| Backend Endpoint | Admin Method | Usage |
|-----------------|--------------|-------|
| `PUT /admin/v1/users/{userId}/recommendation-toggle` | `adminApi.updateUserRecommendationToggle()` | Toggle user recommendation |
| `GET /admin/v1/users/{userId}/recommendation-status` | `adminApi.getUserRecommendationStatus()` | Get recommendation status |

---

### Mapping Rules Management

| Backend Endpoint | Admin Method | Usage |
|-----------------|--------------|-------|
| `GET /admin/v1/recommendationmapping/rules` | `mappingRulesApi.getAllRules()` | Get all mapping rules |
| `GET /admin/v1/recommendationmapping/rules/{id}` | `mappingRulesApi.getRule()` | Get single rule |
| `PUT /admin/v1/recommendationmapping/rules/{id}` | `mappingRulesApi.updateRule()` | Update rule |

---

### Queue Management

| Backend Endpoint | Admin Method | Usage |
|-----------------|--------------|-------|
| `GET /v1/queue/statistics` | `queueApi.getStatistics()` | Get queue statistics |

---

## Website API Usage

### Articles Endpoints

| Backend Endpoint | Website Usage |
|-----------------|---------------|
| `GET /v1/articles` | Display published articles |
| `GET /v1/articles/view/{id}` | Display article content |
| `GET /v1/articles/search` | Search articles |

---

## Summary Statistics

### iOS App
- **Total Backend Endpoints**: ~450
- **Endpoints Used by iOS**: ~30 (7%)
- **Not Implemented Features**: Comments, Notifications, User Profile, Drafts

### Admin Panel
- **Total Backend Endpoints**: ~450
- **Endpoints Used by Admin**: ~50 (11%)
- **Primary Use Cases**: User management, post moderation, article management, analytics

### Website
- **Total Backend Endpoints**: ~450
- **Endpoints Used by Website**: ~3 (< 1%)
- **Primary Use Cases**: Display articles

---

## Unused Endpoint Analysis

### Categories of Unused Endpoints

1. **Test & Debug (~50 endpoints)** - Not used by any frontend
   - All `/api/test/*` endpoints
   - DeepSeek test controllers
   - Moderation test controllers
   - Enhanced AI analysis test endpoints

2. **Operations & Deployment (~40 endpoints)** - Not used by any frontend
   - Deployment controllers
   - Auto-scaling controllers
   - Load balancing controllers
   - Feature flag controllers

3. **Performance & Monitoring (~30 endpoints)** - Not used by any frontend
   - Multi-level cache controllers
   - AI service connection pool
   - Batch processing controllers
   - Model training controllers

4. **Specialized Features (~80 endpoints)** - Not used by any frontend
   - User consent management (not implemented)
   - Data sharing (not implemented)
   - Interests management (not implemented)
   - Advanced personality computation (not used)
   - Vector database direct access (internal only)

5. **Feature Gaps (~50 endpoints)** - Backend ready, frontend not implemented
   - Comments system (iOS)
   - Notifications (iOS)
   - User profile management (iOS)
   - Drafts (iOS)
   - Trending/Leaderboard (iOS)

---

## Frontend Implementation Gaps

### iOS App Missing Features

1. **Comments** - Backend fully implemented, iOS missing
   - Impact: Users cannot comment on posts
   - Effort: Medium (2-3 weeks)
   - Priority: High

2. **Notifications** - Backend fully implemented, iOS missing
   - Impact: Users miss important updates
   - Effort: Medium (2-3 weeks)
   - Priority: High

3. **User Profile** - Backend fully implemented, iOS missing
   - Impact: Users cannot edit profile, view stats
   - Effort: Large (3-4 weeks)
   - Priority: Medium

4. **Drafts** - Backend fully implemented, iOS missing
   - Impact: Users cannot save post drafts
   - Effort: Small (1 week)
   - Priority: Low

5. **Discovery Features** - Backend partially implemented, iOS missing
   - Trending posts/topics
   - Leaderboards
   - AI suggestions
   - Impact: Reduced user engagement
   - Effort: Medium (2-3 weeks)
   - Priority: Medium

---

## Recommendations

### For iOS App

1. **Implement High-Priority Features**
   - Comments system (highest user value)
   - Push notifications
   - Basic user profile editing

2. **Remove/Don't Implement**
   - Complex analytics endpoints (admin only)
   - Deployment management
   - Advanced cache management

### For Admin Panel

1. **Add Missing Features**
   - User warning/ban management
   - Comment moderation
   - Better analytics visualizations

2. **Focus On**
   - Content moderation tools
   - User management
   - System monitoring

### For Backend

1. **Remove Unused Endpoints**
   - All test controllers from production
   - Unused specialized features (consent, data sharing, interests)
   - Operations endpoints (move to separate admin service)

2. **Consolidate Redundant**
   - Multiple moderation controllers
   - Duplicate admin endpoints
   - Multiple AI chat controllers

3. **Improve What's Used**
   - Add better error messages for iOS endpoints
   - Optimize frequently-called endpoints
   - Add more comprehensive logging

---

**Document Version**: 1.0  
**Next Review**: After implementing iOS feature gaps  
**Related Documents**: `API_ENDPOINT_INVENTORY.md`, `API_OPTIMIZATION_RECOMMENDATIONS.md`



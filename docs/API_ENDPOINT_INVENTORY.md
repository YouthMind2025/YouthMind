# YouthMind API Endpoint Comprehensive Inventory

**Generated**: 2024
**Total Controllers**: 56
**Total Endpoints**: ~450+

## Executive Summary

This document provides a comprehensive inventory of all API endpoints in the YouthMind backend, categorizes them by purpose and necessity, maps frontend usage, and provides recommendations for optimization.

### Key Findings
- **Core Production Endpoints**: ~80 endpoints actively used by iOS app and admin panel
- **Test/Debug Endpoints**: ~50 endpoints that should be removed from production
- **Deployment/Operations Endpoints**: ~40 endpoints for infrastructure management
- **Unused/Redundant Endpoints**: ~30 endpoints with no frontend usage
- **API Versioning Issues**: Mix of `/v1/` and `/api/` prefixes causing inconsistency

---

## 1. Core Production APIs

### 1.1 Authentication (`/v1/auth`) - AuthController
**Status**: ESSENTIAL | **Frontend Usage**: iOS, Admin | **Security**: Public + JWT

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| POST | `/v1/auth/send-otp` | Send OTP for registration | iOS |
| POST | `/v1/auth/register` | Register new user | iOS |
| POST | `/v1/auth/login` | User login | iOS |
| POST | `/v1/auth/refresh-token` | Refresh JWT token | iOS |
| POST | `/v1/auth/logout` | Logout user | iOS |
| POST | `/v1/auth/admin/register` | Register admin user | Admin |
| POST | `/v1/auth/admin/login` | Admin login | Admin |
| POST | `/v1/auth/admin/revoke-token` | Revoke user tokens | Admin |
| GET | `/v1/auth/test` | Test auth endpoint | Testing |

**Necessity**: All endpoints are essential except `/test` which should be removed in production.

---

### 1.2 Posts (`/v1/posts`) - PostController
**Status**: ESSENTIAL | **Frontend Usage**: iOS | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| POST | `/v1/posts/create` | Create new post | iOS |
| GET | `/v1/posts` | Get all posts with pagination | iOS |
| GET | `/v1/posts/feed` | Get paginated feed | iOS |
| GET | `/v1/posts/feed/blocks` | Get personalized feed block | iOS |
| GET | `/v1/posts/by-user/{userId}` | Get posts by user | iOS |
| GET | `/v1/posts/{id}` | Get post by ID | iOS |
| PUT | `/v1/posts/{id}/resonance` | Toggle post like | iOS |
| PUT | `/v1/posts/{id}/collect` | Toggle post save | iOS |
| PUT | `/v1/posts/{id}` | Update post | iOS |
| DELETE | `/v1/posts/{id}` | Delete post | iOS |
| POST | `/v1/posts/{id}/view` | Track post view | iOS |
| POST | `/v1/posts/{id}/view/duration` | Track view with duration | iOS |
| POST | `/v1/posts/{id}/view/start` | Start view tracking | iOS (Deprecated) |
| POST | `/v1/posts/{id}/view/end` | End view tracking | iOS (Deprecated) |
| GET | `/v1/posts/{id}/view-stats` | Get view statistics | iOS |
| POST | `/v1/posts/{id}/analyze-psychological` | Analyze post psychology | Internal |
| GET | `/v1/posts/{id}/psychological-metadata` | Get psychological metadata | Internal |

**Necessity**: All essential except view/start and view/end which appear deprecated based on code comments.

---

### 1.3 Comments (`/v1/posts`) - CommentController
**Status**: ESSENTIAL | **Frontend Usage**: iOS | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| GET | `/v1/posts/{postId}/comments` | Get comments for post | iOS |
| POST | `/v1/posts/{postId}/comments` | Add comment to post | iOS |
| POST | `/v1/posts/comments/{commentId}/replies` | Add reply to comment | iOS |
| DELETE | `/v1/posts/comments/{commentId}` | Delete comment | iOS |
| PUT | `/v1/posts/comments/{commentId}/like` | Like/unlike comment | iOS |
| POST | `/v1/posts/comments/{commentId}/report` | Report comment | iOS |

**Necessity**: All essential.

---

### 1.4 User Management (`/api/users`) - UserController
**Status**: ESSENTIAL | **Frontend Usage**: Admin | **Security**: JWT Required
**API VERSION ISSUE**: Uses `/api/` instead of `/v1/`

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| GET | `/api/users` | Get all users | Admin |
| GET | `/api/users/{id}` | Get user by ID | Admin, iOS |
| GET | `/api/users/email/{email}` | Get user by email | Admin |
| DELETE | `/api/users/{id}` | Delete user | Admin |
| GET | `/api/users/personality/{userId}` | Get personality profile | Internal |
| POST | `/api/users/personality/compute` | Compute personality | Internal |
| GET | `/api/users/personality/insights/{userId}` | Get personality insights | Internal |
| GET | `/api/users/{userId}/psychological-state` | Get psychological state | Internal |
| PUT | `/api/users/{userId}/recommendation-engine` | Toggle recommendations | Admin |
| GET | `/api/users/{userId}/recommendation-engine` | Get recommendation status | Admin |

**Necessity**: All essential. **Action Required**: Migrate to `/v1/` for consistency.

---

### 1.5 User Profile (`/v1/user`) - UserProfileController
**Status**: ESSENTIAL | **Frontend Usage**: iOS | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| GET | `/v1/user/profile` | Get current user profile | iOS |
| PUT | `/v1/user/profile` | Update user profile | iOS |
| GET | `/v1/user/posts` | Get user's posts | iOS |
| GET | `/v1/user/liked-posts` | Get liked posts | iOS |
| GET | `/v1/user/saved-posts` | Get saved posts | iOS |
| GET | `/v1/user/statistics` | Get user statistics | iOS |
| POST | `/v1/user/avatar` | Upload avatar | iOS |
| DELETE | `/v1/user/account` | Delete account | iOS |
| PUT | `/v1/user/settings/recommendation-engine` | Update recommendation setting | iOS |

**Necessity**: All essential.

---

### 1.6 Search & Discovery (`/v1`) - SearchController
**Status**: ESSENTIAL | **Frontend Usage**: iOS | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| GET | `/v1/search/posts` | Search posts | iOS |
| GET | `/v1/search/posts/advanced` | Advanced search with relevance | iOS |
| GET | `/v1/search/posts/emotion/{emotion}` | Get posts by emotion | iOS |
| GET | `/v1/trending` | Get trending posts | iOS |
| GET | `/v1/leaderboard` | Get leaderboard | iOS |
| GET | `/v1/suggestions` | Get AI suggestions | iOS |
| GET | `/v1/trending-topics` | Get trending topics | iOS |

**Necessity**: All essential for discovery features.

---

### 1.7 Notifications (`/v1/notifications`) - NotificationController
**Status**: ESSENTIAL | **Frontend Usage**: iOS | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| GET | `/v1/notifications` | Get user notifications | iOS |
| POST | `/v1/notifications/{notificationId}/read` | Mark notification as read | iOS |
| POST | `/v1/notifications/read-all` | Mark all as read | iOS |

**Necessity**: All essential.

---

### 1.8 Articles (`/v1/articles`) - ArticleController
**Status**: ESSENTIAL | **Frontend Usage**: Admin, Website | **Security**: Mixed

| Method | Endpoint | Description | Used By | Security |
|--------|----------|-------------|---------|----------|
| GET | `/v1/articles` | Get published articles | Website | Public |
| GET | `/v1/articles/admin` | Get articles with pagination | Admin | JWT |
| GET | `/v1/articles/view/{id}` | Get article by ID | Website | Public |
| POST | `/v1/articles/create` | Create article | Admin | JWT |
| PUT | `/v1/articles/edit/{id}` | Update article | Admin | JWT |
| DELETE | `/v1/articles/delete/{id}` | Delete article | Admin | JWT |
| POST | `/v1/articles/manage/{id}/toggle-publish` | Toggle publish status | Admin | JWT |
| GET | `/v1/articles/search` | Search articles | Website | Public |
| POST | `/v1/articles/upload-docx` | Upload .docx file | Admin | JWT |

**Necessity**: All essential.

---

### 1.9 Conversations (`/v1/conversations`) - ConversationController
**Status**: ESSENTIAL | **Frontend Usage**: iOS | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| GET | `/v1/conversations` | Get conversations | iOS |
| POST | `/v1/conversations` | Create conversation | iOS |
| GET | `/v1/conversations/{conversationId}` | Get single conversation | iOS |
| POST | `/v1/conversations/{conversationId}/messages` | Send message | iOS |
| DELETE | `/v1/conversations/{conversationId}` | Delete conversation | iOS |
| PUT | `/v1/conversations/{conversationId}/pin` | Pin/unpin conversation | iOS |

**Necessity**: All essential for messaging feature.

---

### 1.10 AI Chat (`/v1/ai-chat`) - AIChatController
**Status**: ESSENTIAL | **Frontend Usage**: iOS | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| POST | `/v1/ai-chat/send-message` | Send message to AI | iOS |
| GET | `/v1/ai-chat/personality-profile` | Get personality profile | iOS |
| PUT | `/v1/ai-chat/personality-profile` | Update personality profile | iOS |
| POST | `/v1/ai-chat/test-safety-filter` | Test safety filter | Testing |

**Necessity**: All essential except test endpoint.

---

### 1.11 Drafts (`/v1/drafts`) - DraftController
**Status**: ESSENTIAL | **Frontend Usage**: iOS | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| GET | `/v1/drafts` | Get user drafts | iOS |
| POST | `/v1/drafts` | Save draft | iOS |
| PUT | `/v1/drafts/{draftId}` | Update draft | iOS |
| DELETE | `/v1/drafts/{draftId}` | Delete draft | iOS |
| POST | `/v1/drafts/{draftId}/publish` | Publish draft as post | iOS |

**Necessity**: All essential.

---

## 2. Admin-Specific APIs

### 2.1 Admin Operations (`/admin/v1`) - AdminController
**Status**: ESSENTIAL | **Frontend Usage**: Admin | **Security**: Admin JWT

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| PUT | `/admin/v1/users/{userId}/recommendation-toggle` | Toggle user recommendations | Admin |
| GET | `/admin/v1/users/{userId}/recommendation-status` | Get recommendation status | Admin |
| POST | `/admin/v1/posts/classify-emotions` | Classify post emotions | Admin |
| POST | `/admin/v1/users/migrate` | Migrate users | Admin |
| GET | `/admin/v1/recommendationmapping/rules` | Get mapping rules | Admin |
| GET | `/admin/v1/recommendationmapping/rules/{id}` | Get specific rule | Admin |
| PUT | `/admin/v1/recommendationmapping/rules/{id}` | Update mapping rule | Admin |

**Necessity**: All essential for admin operations.

---

### 2.2 Post Moderation (`/api/moderation/posts`) - PostModerationController
**Status**: ESSENTIAL | **Frontend Usage**: Admin | **Security**: Admin JWT
**API VERSION ISSUE**: Uses `/api/` instead of `/v1/`

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| POST | `/api/moderation/posts/{postId}/moderate` | Moderate post | Admin |
| GET | `/api/moderation/posts/flagged` | Get flagged posts | Admin |
| POST | `/api/moderation/posts/{postId}/unflag` | Unflag post | Admin |
| POST | `/api/moderation/posts/{postId}/flag` | Flag post | Admin |
| GET | `/api/moderation/posts/statistics` | Get moderation stats | Admin |

**Necessity**: All essential. **Action Required**: Migrate to `/admin/v1/moderation/`.

---

### 2.3 Admin Post Management (`/admin/v1/posts`) - AdminPostController
**Status**: OPTIONAL | **Frontend Usage**: Admin | **Security**: Admin JWT

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| DELETE | `/admin/v1/posts/delete/{postId}` | Delete any post | Admin |
| GET | `/admin/v1/posts/flagged` | Get flagged posts | Admin |
| POST | `/admin/v1/posts/flag/{postId}` | Flag post | Admin |
| POST | `/admin/v1/posts/unflag/{postId}` | Unflag post | Admin |

**Necessity**: REDUNDANT - Overlaps with PostModerationController. **Action Required**: Consolidate with PostModerationController.

---

## 3. ML/AI & Recommendation APIs

### 3.1 Recommendation Engine (`/v1/recommendation`) - BaseRecommendationController
**Status**: ESSENTIAL | **Frontend Usage**: Internal, Admin | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| GET | `/v1/recommendation/rvector` | Get R vector | Admin, iOS |
| GET | `/v1/recommendation/rvector/history` | Get R vector history | Admin |
| POST | `/v1/recommendation/rvector/reset` | Reset R vector | Admin |
| GET | `/v1/analytics/recommendation/performance` | Get performance metrics | Admin |
| GET | `/v1/analytics/recommendation/emotion-distribution` | Get emotion distribution | Admin |

**Necessity**: All essential for recommendation system.

---

### 3.2 Psychological State (`/api/psychological-state`) - PsychologicalStateController
**Status**: ESSENTIAL | **Frontend Usage**: Internal | **Security**: JWT Required
**API VERSION ISSUE**: Uses `/api/` instead of `/v1/`

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| POST | `/api/psychological-state/update-from-post` | Update from post | Internal |
| POST | `/api/psychological-state/update-from-interaction` | Update from interaction | Internal |
| GET | `/api/psychological-state/current/{userId}` | Get current state | Internal |
| POST | `/api/psychological-state/similarity` | Calculate similarity | Internal |
| GET | `/api/psychological-state/history/{userId}` | Get state history | Internal |
| POST | `/api/psychological-state/initialize/{userId}` | Initialize state | Internal |
| POST | `/api/psychological-state/vector-operations` | Vector operations | Internal |

**Necessity**: All essential. **Action Required**: Migrate to `/v1/`.

---

### 3.3 Psychological AI Chat (`/api/ai/psychology`) - PsychologicalAIChatController
**Status**: ESSENTIAL | **Frontend Usage**: Internal, Admin | **Security**: JWT Required
**API VERSION ISSUE**: Uses `/api/` instead of `/v1/`

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| POST | `/api/ai/psychology/chat/{userId}` | Psychological chat | Admin |
| GET | `/api/ai/psychology/state/{userId}` | Get psychological state | Admin |
| POST | `/api/ai/psychology/analyze/{userId}` | Analyze message | Admin |
| GET | `/api/ai/psychology/health` | Health check | Admin |

**Necessity**: All essential for admin psychological analysis dashboard.

---

### 3.4 Behavior Analytics (`/v1/analytics/behavior`) - BehaviorAnalyticsController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| POST | `/v1/analytics/behavior/view` | Log view event | Internal |
| POST | `/v1/analytics/behavior/interaction` | Log interaction event | Internal |
| GET | `/v1/analytics/behavior/summary/{userId}` | Get behavior summary | None |
| GET | `/v1/analytics/behavior/patterns/{userId}` | Get behavior patterns | None |

**Necessity**: OPTIONAL - Analytics endpoints not currently used by any frontend.

---

### 3.5 Personality Computation (`/api/personality`) - PersonalityComputationController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: JWT Required
**API VERSION ISSUE**: Uses `/api/` instead of `/v1/`

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| GET | `/api/personality/analysis/{userId}` | Get personality analysis | None |
| GET | `/api/personality/active/{userId}` | Get active personality | None |
| GET | `/api/personality/subconscious/{userId}` | Get subconscious | None |
| GET | `/api/personality/fused/{userId}` | Get fused personality | None |
| POST | `/api/personality/compute/{userId}` | Compute personality | None |
| POST | `/api/personality/schedule/{userId}` | Schedule computation | None |
| GET | `/api/personality/history/{userId}` | Get computation history | None |
| GET | `/api/personality/status/{userId}` | Get computation status | None |
| GET | `/api/personality/health` | Health check | None |

**Necessity**: UNUSED - No frontend usage found. Consider removing or integrating with UserController personality endpoints.

---

## 4. Infrastructure & Monitoring APIs

### 4.1 Health Check (`/v1/health`) - HealthController
**Status**: ESSENTIAL | **Frontend Usage**: Admin, K8s | **Security**: Public

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| GET | `/v1/health` | Overall health check | Admin, K8s |
| GET | `/v1/health/detailed` | Detailed health info | Admin |
| GET | `/actuator/health` | Spring actuator health | Admin, K8s |
| GET | `/actuator/health/liveness` | Liveness probe | K8s |
| GET | `/actuator/health/readiness` | Readiness probe | K8s |

**Necessity**: All essential for monitoring.

---

### 4.2 AI Service Health (`/v1/ai-health`) - AIHealthController
**Status**: ESSENTIAL | **Frontend Usage**: Admin | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| GET | `/v1/ai-health` | AI service health | Admin |
| GET | `/v1/ai-health/connection-status` | Connection status | Admin |
| GET | `/v1/ai-health/usage-stats` | Usage statistics | Admin |

**Necessity**: All essential for AI service monitoring.

---

### 4.3 Queue Management (`/v1/queue`) - QueueController
**Status**: ESSENTIAL | **Frontend Usage**: Admin | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| POST | `/v1/queue/submit` | Submit task | Internal |
| GET | `/v1/queue/status/{requestId}` | Get task status | Internal |
| GET | `/v1/queue/statistics` | Get queue statistics | Admin |
| GET | `/v1/queue/processing-times` | Get processing times | Admin |
| POST | `/v1/queue/load-balance` | Load balance tasks | Internal |

**Necessity**: All essential for queue management.

---

### 4.4 Multi-Level Cache (`/v1/cache`) - MultiLevelCacheController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: JWT Required

**~20 endpoints** for cache management, metrics, warming, compression, consistency checks.

**Necessity**: TESTING/DEBUG - Should be restricted to internal operations or removed from production.

---

## 5. Test & Debug APIs

### 5.1 Test Controller (`/api/test`) - TestController
**Status**: TESTING | **Frontend Usage**: None | **Security**: Public
**⚠️ SECURITY RISK**: Test endpoints exposed in production

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/test/hello` | Hello world | REMOVE |
| GET | `/api/test/health` | Health check | REMOVE |
| GET | `/api/test/redis` | Redis test | REMOVE |
| GET | `/api/test/redis/cache` | Redis cache test | REMOVE |
| POST | `/api/test/redis/pubsub` | Redis pub/sub test | REMOVE |
| GET | `/api/test/redis/hot-posts` | Hot posts test | REMOVE |
| GET | `/api/test/redis/jwt-token` | JWT token test | REMOVE |

**Necessity**: ALL SHOULD BE REMOVED from production builds.

---

### 5.2 DeepSeek API Test (`/api/deepseek/test`) - DeepSeekApiTestController
**Status**: TESTING | **Frontend Usage**: None | **Security**: JWT Required

**~10 endpoints** for testing DeepSeek AI API integration.

**Necessity**: TESTING ONLY - Should be removed from production or protected behind feature flag.

---

### 5.3 DeepSeek Adapter Test (`/api/deepseek-adapter/test`) - DeepSeekAdapterTestController
**Status**: TESTING | **Frontend Usage**: None | **Security**: JWT Required

**~5 endpoints** for testing DeepSeek adapter functionality.

**Necessity**: TESTING ONLY - Should be removed from production.

---

### 5.4 Enhanced Moderation Test (`/api/moderation/enhanced/test`) - EnhancedModerationTestController
**Status**: TESTING | **Frontend Usage**: None | **Security**: JWT Required

**~8 endpoints** for testing enhanced moderation features.

**Necessity**: TESTING ONLY - Should be removed from production.

---

### 5.5 Advanced Moderation Test (`/api/moderation/advanced/test`) - AdvancedModerationTestController
**Status**: TESTING | **Frontend Usage**: None | **Security**: JWT Required

**~8 endpoints** for testing advanced moderation.

**Necessity**: TESTING ONLY - Should be removed from production.

---

## 6. Specialized APIs

### 6.1 Vector Search (`/v1/vector-search`) - VectorSearchController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| POST | `/v1/vector-search/search` | Vector similarity search | Internal |
| GET | `/v1/vector-search/health` | Vector DB health | Internal |
| GET | `/v1/vector-search/metrics` | Vector search metrics | None |
| POST | `/v1/vector-search/initialize` | Initialize vector DB | Internal |

**Necessity**: INTERNAL USE - Not exposed to frontends, used by recommendation engine.

---

### 6.2 Vector Database (`/api/vector-db`) - VectorDatabaseController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: JWT Required
**API VERSION ISSUE**: Uses `/api/` instead of `/v1/`

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| POST | `/api/vector-db/initialize` | Initialize DB | Internal |
| GET | `/api/vector-db/health` | Health check | Internal |
| GET | `/api/vector-db/collections/{collectionName}/stats` | Get stats | None |
| POST | `/api/vector-db/collections/{collectionName}/search` | Search vectors | Internal |
| POST | `/api/vector-db/collections/{collectionName}/vectors` | Add vectors | Internal |
| POST | `/api/vector-db/test` | Test endpoint | REMOVE |

**Necessity**: INTERNAL USE except test endpoint which should be removed.

---

### 6.3 Psychological Data Access (`/api/psychological-data`) - PsychologicalDataAccessController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: Admin JWT
**API VERSION ISSUE**: Uses `/api/` instead of `/v1/`

**~15 endpoints** for managing psychological data access, consent, and privacy.

**Necessity**: OPTIONAL - Appears to be for future GDPR/privacy compliance features.

---

### 6.4 Data Sharing (`/api/data-sharing`) - DataSharingController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: Admin JWT
**API VERSION ISSUE**: Uses `/api/` instead of `/v1/`

**~10 endpoints** for managing data sharing agreements and exports.

**Necessity**: OPTIONAL - Appears to be for future data sharing features.

---

### 6.5 User Consent (`/v1/consent`) - UserConsentController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: JWT Required

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| GET | `/v1/consent/pending` | Get pending consents | None |
| GET | `/v1/consent/history` | Get consent history | None |
| POST | `/v1/consent/{consentId}/decision` | Make decision | None |
| GET | `/v1/consent/check/{contentId}` | Check consent | Internal |
| GET | `/v1/consent/statistics` | Get statistics | None |
| GET | `/v1/consent/alternatives/{word}` | Get alternatives | Internal |

**Necessity**: UNUSED - No frontend usage found. May be for future compliance features.

---

### 6.6 Warning System (`/api/warnings`) - WarningController
**Status**: ESSENTIAL | **Frontend Usage**: Internal, Admin | **Security**: Admin JWT
**API VERSION ISSUE**: Uses `/api/` instead of `/v1/`

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| POST | `/api/warnings/issue` | Issue warning | Internal |
| GET | `/api/warnings/user/{userId}` | Get user warnings | Admin |
| POST | `/api/warnings/{warningId}/acknowledge` | Acknowledge warning | iOS |
| POST | `/api/warnings/ban/{userId}` | Ban user | Admin |
| POST | `/api/warnings/unban/{userId}` | Unban user | Admin |
| GET | `/api/warnings/user/{userId}/banned` | Check if banned | Internal |
| GET | `/api/warnings/user/{userId}/status` | Get warning status | Internal |
| GET | `/api/warnings/manual-review` | Get manual review queue | Admin |

**Necessity**: All essential for moderation system.

---

### 6.7 Interests (`/v1/interests`) - InterestController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: JWT Required

**~6 endpoints** for managing user interests and preferences.

**Necessity**: UNUSED - No frontend usage found. May be for future personalization features.

---

### 6.8 Memory Management (`/v1/memory`) - MemoryController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: JWT Required

**~8 endpoints** for managing AI conversation memory.

**Necessity**: OPTIONAL - Used internally by AI chat system.

---

## 7. Deployment & Operations APIs

### 7.1 Deployment (`/v1/deployment`) - DeploymentController
**Status**: TESTING | **Frontend Usage**: None | **Security**: Admin JWT

**~10 endpoints** for deployment management.

**Necessity**: OPERATIONS - Should not be exposed in production API, use separate admin service or CI/CD tools.

---

### 7.2 Deployment Monitoring (`/v1/deployment/monitoring`) - DeploymentMonitoringController
**Status**: TESTING | **Frontend Usage**: None | **Security**: Admin JWT

**~8 endpoints** for deployment monitoring.

**Necessity**: OPERATIONS - Should be integrated with external monitoring tools, not exposed as API.

---

### 7.3 Deployment Configuration (`/v1/deployment/config`) - DeploymentConfigurationController
**Status**: TESTING | **Frontend Usage**: None | **Security**: Admin JWT

**~12 endpoints** for deployment configuration.

**Necessity**: OPERATIONS - Should not be exposed as public API endpoints.

---

### 7.4 Auto Scaling (`/v1/auto-scaling`) - AutoScalingController
**Status**: TESTING | **Frontend Usage**: None | **Security**: Admin JWT

**~10 endpoints** for auto-scaling management.

**Necessity**: OPERATIONS - Should be handled by Kubernetes/cloud provider, not application API.

---

### 7.5 Load Balancing (`/v1/load-balancing`) - LoadBalancingController
**Status**: TESTING | **Frontend Usage**: None | **Security**: Admin JWT

**~8 endpoints** for load balancing.

**Necessity**: OPERATIONS - Should be handled by infrastructure layer, not application.

---

### 7.6 Feature Flags (`/v1/feature-flags`) - FeatureFlagController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: Admin JWT

**~10 endpoints** for feature flag management.

**Necessity**: OPTIONAL - Useful for gradual rollouts, but should use dedicated feature flag service like LaunchDarkly.

---

### 7.7 Feature Flag Analytics (`/v1/feature-flags/analytics`) - FeatureFlagAnalyticsController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: Admin JWT

**~8 endpoints** for feature flag analytics.

**Necessity**: OPTIONAL - Analytics should be in dedicated analytics platform.

---

## 8. Performance & Optimization APIs

### 8.1 AI Service Connection Pool (`/v1/ai/connection-pool`) - AIServiceConnectionPoolController
**Status**: TESTING | **Frontend Usage**: None | **Security**: Admin JWT

**~10 endpoints** for connection pool management.

**Necessity**: OPERATIONS - Should be internal only, not exposed as API.

---

### 8.2 AI Service Extensions (`/v1/ai/extensions`) - AIServiceExtensionsController
**Status**: TESTING | **Frontend Usage**: None | **Security**: Admin JWT

**~8 endpoints** for AI service extensions.

**Necessity**: TESTING/DEBUG - Should be removed or restricted.

---

### 8.3 AI Performance (`/v1/ai/performance`) - AIPerformanceController
**Status**: OPTIONAL | **Frontend Usage**: Admin | **Security**: Admin JWT

**~10 endpoints** for AI performance monitoring.

**Necessity**: OPTIONAL - Useful for monitoring but could be integrated into standard monitoring.

---

### 8.4 Batch Operations (`/v1/batch`) - BatchController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: JWT Required

**~8 endpoints** for batch processing operations.

**Necessity**: INTERNAL USE - Should not be exposed to frontends.

---

### 8.5 Interaction Processing (`/v1/interactions`) - InteractionProcessingController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: JWT Required

**~8 endpoints** for interaction processing.

**Necessity**: INTERNAL USE - Background processing, not for direct frontend use.

---

### 8.6 Model Training (`/v1/model-training`) - ModelTrainingController
**Status**: TESTING | **Frontend Usage**: None | **Security**: Admin JWT

**~10 endpoints** for model training management.

**Necessity**: OPERATIONS - Should be separate ML ops pipeline, not in production API.

---

## 9. Additional Controllers

### 9.1 Moderation (`/api/moderation`) - ModerationController
**Status**: ESSENTIAL | **Frontend Usage**: Internal | **Security**: JWT Required
**API VERSION ISSUE**: Uses `/api/` instead of `/v1/`

**~12 endpoints** for general content moderation.

**Necessity**: ESSENTIAL but has overlap with PostModerationController and should be consolidated.

---

### 9.2 Insights (`/v1/insights`) - InsightsController
**Status**: OPTIONAL | **Frontend Usage**: None | **Security**: JWT Required

**~8 endpoints** for user insights and analytics.

**Necessity**: UNUSED - No frontend usage found.

---

### 9.3 Enhanced AI Analysis (`/v1/ai/enhanced-analysis`) - EnhancedAIAnalysisController
**Status**: TESTING | **Frontend Usage**: None | **Security**: JWT Required

**~10 endpoints** for enhanced AI analysis testing.

**Necessity**: TESTING ONLY - Should be removed from production.

---

### 9.4 AI Chat API (`/v1/ai-chat-api`) - AIChatApiController
**Status**: REDUNDANT | **Frontend Usage**: None | **Security**: JWT Required

**Necessity**: REDUNDANT - Overlaps with AIChatController. Consolidate into one controller.

---

### 9.5 API Usage Analytics (`/v1/analytics/api-usage`) - ApiUsageAnalyticsController
**Status**: OPTIONAL | **Frontend Usage**: Admin | **Security**: Admin JWT

**~8 endpoints** for API usage tracking.

**Necessity**: OPTIONAL - Useful for monitoring but could use standard API gateway analytics.

---

### 9.6 Welcome & Docs (`/`) - WelcomeController, DocsController
**Status**: OPTIONAL | **Frontend Usage**: Public | **Security**: Public

| Method | Endpoint | Description | Used By |
|--------|----------|-------------|---------|
| GET | `/` | Welcome page | Public |
| GET | `/docs` | API documentation | Public |

**Necessity**: OPTIONAL - Nice to have for API documentation landing page.

---

## 10. API Versioning Analysis

### Current State

**Version Prefixes Used**:
- `/v1/` - ~60% of endpoints (preferred standard)
- `/api/` - ~30% of endpoints (inconsistent)
- `/admin/v1/` - ~10% of endpoints (admin-specific)

### Issues Identified

1. **Inconsistent Versioning**: Mix of `/v1/` and `/api/` prefixes
2. **Missing Versioning**: Some endpoints have no version prefix
3. **Admin Prefix Confusion**: Should admin be `/admin/v1/` or `/v1/admin/`?

### Recommended Standard

```
Public APIs:     /v1/{resource}
Admin APIs:      /admin/v1/{resource}
Internal APIs:   /internal/v1/{resource}
Test APIs:       /test/{resource} (only in dev/staging)
```

### Endpoints Needing Migration

| Current | Recommended | Controller |
|---------|-------------|------------|
| `/api/users` | `/v1/users` | UserController |
| `/api/moderation` | `/admin/v1/moderation` | ModerationController |
| `/api/psychological-state` | `/v1/psychological-state` | PsychologicalStateController |
| `/api/personality` | `/v1/personality` | PersonalityComputationController |
| `/api/vector-db` | `/internal/v1/vector-db` | VectorDatabaseController |
| `/api/warnings` | `/v1/warnings` | WarningController |
| `/api/test` | `/test/*` (remove) | TestController |

---

## 11. Security Analysis

### Authentication Requirements

| Security Level | Endpoints | Description |
|---------------|-----------|-------------|
| **Public** | ~30 | No authentication required |
| **JWT Required** | ~300 | Standard user authentication |
| **Admin JWT** | ~80 | Admin-only endpoints |
| **Internal** | ~40 | Service-to-service |

### Security Issues Found

1. **⚠️ Test Endpoints Exposed**: `/api/test/*` endpoints are publicly accessible
2. **⚠️ No Rate Limiting Visible**: No clear rate limiting on public endpoints
3. **⚠️ Inconsistent Admin Protection**: Some admin endpoints might not have proper role checks
4. **⚠️ CORS Configuration**: Need to verify CORS is properly configured for production

### Recommendations

1. **Remove Test Endpoints**: All `/api/test/*` endpoints from production
2. **Implement Rate Limiting**: Add rate limiting to all public endpoints
3. **Audit Admin Endpoints**: Verify all admin endpoints have `@PreAuthorize("hasRole('ADMIN')")`
4. **API Gateway**: Consider implementing API gateway for unified security

---

## 12. Frontend Usage Mapping

### iOS App Endpoint Usage

**Total Endpoints Used**: ~70

**Categories**:
- Authentication: 5 endpoints
- Posts & Feed: 15 endpoints
- Comments: 6 endpoints
- User Profile: 9 endpoints
- Search & Discovery: 7 endpoints
- Notifications: 3 endpoints
- Conversations: 6 endpoints
- AI Chat: 3 endpoints
- Drafts: 5 endpoints
- Warnings: 1 endpoint (acknowledge)

**Unused by iOS**: ~380 endpoints (including test, admin, operations)

---

### Admin Panel Endpoint Usage

**Total Endpoints Used**: ~50

**Categories**:
- Authentication: 2 endpoints (admin login/register)
- User Management: 4 endpoints
- Articles: 9 endpoints
- Posts Moderation: 5 endpoints
- Recommendation Management: 5 endpoints
- Psychological Analysis: 10 endpoints
- System Health: 5 endpoints
- Queue Management: 2 endpoints
- Analytics: 8 endpoints

**Unused by Admin**: ~400 endpoints

---

### Total Frontend Usage

- **Endpoints Used by Any Frontend**: ~90 (20%)
- **Endpoints Not Used**: ~360 (80%)

**Note**: High percentage of unused endpoints indicates potential for cleanup.

---

## 13. Endpoint Necessity Categorization

### Essential (Must Keep) - ~120 endpoints
- All authentication endpoints
- Core CRUD operations (posts, comments, users)
- Feed and recommendation endpoints
- Search and discovery
- Admin moderation tools
- System health checks

### Optional (Nice to Have) - ~80 endpoints
- Advanced analytics endpoints
- Feature flag management
- API usage tracking
- Performance monitoring
- Some specialized APIs

### Testing (Remove from Production) - ~50 endpoints
- All `/api/test/*` endpoints
- DeepSeek test controllers
- Moderation test controllers
- Enhanced AI analysis tests
- Test safety filters

### Deprecated (Can Remove) - ~30 endpoints
- View tracking start/end (replaced by duration tracking)
- Duplicate auth test endpoints
- Old API versions

### Redundant (Consolidate) - ~40 endpoints
- AdminPostController (merge with PostModerationController)
- AIChatApiController (merge with AIChatController)
- Multiple cache management endpoints
- Duplicate moderation endpoints

### Unused (Evaluate/Remove) - ~130 endpoints
- Deployment operations (should be in CI/CD)
- Auto-scaling (should be in K8s)
- Load balancing (should be in infrastructure)
- Model training (should be separate ML pipeline)
- Personality computation (no frontend usage)
- Interests management (not implemented)
- User consent (not implemented)
- Data sharing (not implemented)

---

## 14. Recommendations

### Immediate Actions (High Priority)

1. **Remove Test Endpoints** (Security)
   - Delete all controllers with "Test" in name from production build
   - Estimated: ~50 endpoints removed
   - Risk: Low (only affects testing)

2. **Standardize API Versioning** (Consistency)
   - Migrate all `/api/*` endpoints to `/v1/*`
   - Update frontend API clients
   - Add deprecation notices for 1-2 releases
   - Estimated: ~30 endpoints migrated

3. **Consolidate Duplicate Endpoints** (Code Quality)
   - Merge AdminPostController into PostModerationController
   - Merge AIChatApiController into AIChatController
   - Consolidate moderation endpoints
   - Estimated: ~20 endpoints removed

4. **Audit Admin Security** (Security)
   - Verify all admin endpoints have proper `@PreAuthorize`
   - Add admin role checks where missing
   - Test admin access control

### Short-term Actions (Medium Priority)

5. **Move Operations Endpoints** (Architecture)
   - Move deployment, scaling, load balancing to separate admin service
   - Not in main API
   - Estimated: ~40 endpoints moved

6. **Implement Rate Limiting** (Performance/Security)
   - Add rate limiting to all public endpoints
   - Use Redis for distributed rate limiting
   - Configure limits per endpoint type

7. **Remove Unused Endpoints** (Code Quality)
   - Remove interests, consent, data sharing controllers (not implemented)
   - Remove personality computation if not used
   - Estimated: ~50 endpoints removed

8. **Add API Documentation** (Developer Experience)
   - Ensure all essential endpoints have complete Swagger docs
   - Add request/response examples
   - Document error codes

### Long-term Actions (Low Priority)

9. **Implement API Gateway** (Architecture)
   - Use Kong, AWS API Gateway, or similar
   - Centralize authentication, rate limiting, monitoring
   - Unified CORS configuration

10. **Versioning Strategy** (Maintenance)
    - Plan for v2 API with breaking changes
    - Support v1 for 12 months after v2 release
    - Document migration guide

11. **Optimize Internal APIs** (Performance)
    - Move internal APIs to service-to-service communication
    - Not exposed via public API gateway
    - Use gRPC or message queues

12. **Monitoring & Analytics** (Observability)
    - Use external APM tool (New Relic, Datadog, etc.)
    - Move analytics endpoints to analytics service
    - Real-time monitoring dashboard

---

## 15. Migration Plan

### Phase 1: Cleanup (Week 1-2)
- Remove all test controllers
- Remove deprecated endpoints
- Update frontend clients to stop using removed endpoints

**Impact**: Low risk, immediate code quality improvement

### Phase 2: Consolidation (Week 3-4)
- Merge duplicate controllers
- Consolidate redundant endpoints
- Update tests

**Impact**: Medium risk, requires thorough testing

### Phase 3: Versioning (Week 5-8)
- Migrate `/api/*` to `/v1/*`
- Update frontend clients
- Add deprecation warnings
- Keep old endpoints for backward compatibility

**Impact**: Medium risk, requires coordinated frontend updates

### Phase 4: Security & Operations (Week 9-12)
- Audit and fix admin security
- Move operations endpoints
- Implement rate limiting
- API gateway setup

**Impact**: Medium-high risk, requires security testing

---

## 16. Endpoint Count Summary

| Category | Count | Percentage | Status |
|----------|-------|------------|--------|
| **Essential** | 120 | 27% | Keep |
| **Optional** | 80 | 18% | Keep |
| **Testing** | 50 | 11% | Remove |
| **Deprecated** | 30 | 7% | Remove |
| **Redundant** | 40 | 9% | Consolidate |
| **Unused** | 130 | 29% | Remove/Evaluate |
| **Total** | ~450 | 100% | |

**After Cleanup**: ~200 endpoints (44% reduction)

---

## 17. Conclusion

The YouthMind backend has comprehensive API coverage but suffers from:

1. **Over-engineering**: Many endpoints for features not used by any frontend
2. **Inconsistent standards**: Mixed API versioning and naming
3. **Security risks**: Test endpoints exposed in production
4. **Maintenance burden**: 450+ endpoints to maintain when only ~90 are actively used

**Recommendation**: Follow the phased migration plan to reduce API surface area by ~55%, improve consistency, and enhance security while maintaining all essential functionality.

---

**Document Version**: 1.0
**Last Updated**: 2024
**Next Review**: After Phase 1 cleanup completion



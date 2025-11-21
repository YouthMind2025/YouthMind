# Swagger UI Testing Guide - YouthMind API

**Purpose**: Step-by-step guide to test all core API endpoints in Swagger UI  
**Time Required**: 2-3 hours for complete testing  
**Prerequisites**: Backend running on `localhost:8080`

---

## 🚀 Getting Started

### Step 1: Start the Backend

```bash
cd backend
mvn spring-boot:run
```

Wait until you see: `Started YouthmindBackendApplication`

### Step 2: Access Swagger UI

Open your browser and navigate to:
```
http://localhost:8080/swagger-ui.html
```

You should see the interactive API documentation with all endpoints organized by tags.

---

## 🔐 Authentication Setup

Most endpoints require JWT authentication. You'll need to get a token first.

### Option A: Register a New Test User

1. **Find the Authentication section** in Swagger UI
2. **Expand `POST /v1/auth/send-otp`**
   - Click "Try it out"
   - Enter test email: `test@example.com`
   - Click "Execute"
   - Note the OTP from your email (if configured) or check logs

3. **Expand `POST /v1/auth/register`**
   - Click "Try it out"
   - Enter:
     ```json
     {
       "email": "test@example.com",
       "password": "Test123!",
       "username": "testuser",
       "otp": "123456"
     }
     ```
   - Click "Execute"
   - Copy the `token` from the response

### Option B: Login with Existing User

1. **Expand `POST /v1/auth/login`**
   - Click "Try it out"
   - Enter:
     ```json
     {
       "email": "existing@example.com",
       "password": "yourpassword"
     }
     ```
   - Click "Execute"
   - Copy the `token` from the response

### Step 3: Authorize Swagger UI

1. **Click the "Authorize" button** at the top right (🔓 icon)
2. **Enter your token** in the format: `Bearer YOUR_TOKEN_HERE`
   - Example: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
3. **Click "Authorize"** then **"Close"**
4. The lock icons (🔒) should now be closed

---

## 📋 Organized Testing Plan

Test in this order to build up the necessary data for later tests.

---

## 1️⃣ Authentication Testing (5 minutes)

### Tests to Run:

#### Test 1.1: Health Check (No Auth Required)
- **Endpoint**: `GET /v1/health` or `GET /actuator/health`
- **Expected**: Status 200, `"status": "UP"`
- **Purpose**: Verify backend is running

#### Test 1.2: Send OTP
- **Endpoint**: `POST /v1/auth/send-otp`
- **Body**: 
  ```json
  {
    "email": "newuser@test.com"
  }
  ```
- **Expected**: Status 200, OTP sent message
- **Note**: Check console logs if email not configured

#### Test 1.3: Register New User
- **Endpoint**: `POST /v1/auth/register`
- **Body**:
  ```json
  {
    "email": "newuser@test.com",
    "password": "Test123!",
    "username": "newtestuser",
    "otp": "123456"
  }
  ```
- **Expected**: Status 201, user ID and token returned
- **Save**: Copy the `token` and `userId`

#### Test 1.4: Login
- **Endpoint**: `POST /v1/auth/login`
- **Body**:
  ```json
  {
    "email": "newuser@test.com",
    "password": "Test123!"
  }
  ```
- **Expected**: Status 200, token returned

#### Test 1.5: Refresh Token
- **Endpoint**: `POST /v1/auth/refresh-token`
- **Header**: Authorization: Bearer YOUR_TOKEN
- **Expected**: Status 200, new token returned

#### Test 1.6: Logout
- **Endpoint**: `POST /v1/auth/logout`
- **Header**: Authorization: Bearer YOUR_TOKEN
- **Expected**: Status 200, logout successful

**✅ Checklist:**
- [ ] Can send OTP
- [ ] Can register new user
- [ ] Can login with credentials
- [ ] Can refresh token
- [ ] Can logout
- [ ] Receive valid JWT tokens

---

## 2️⃣ User Profile Testing (10 minutes)

### Tests to Run:

#### Test 2.1: Get Current User Profile
- **Endpoint**: `GET /v1/user/profile`
- **Auth**: Required (use your token)
- **Expected**: Status 200, your user profile data
- **Note**: Should show empty bio, no avatar initially

#### Test 2.2: Update User Profile
- **Endpoint**: `PUT /v1/user/profile`
- **Body**:
  ```json
  {
    "username": "updated_username",
    "bio": "This is my test bio"
  }
  ```
- **Expected**: Status 200, updated profile returned

#### Test 2.3: Get User Statistics
- **Endpoint**: `GET /v1/user/statistics`
- **Expected**: Status 200, statistics about your posts, likes, etc.

#### Test 2.4: Get User by ID
- **Endpoint**: `GET /api/users/{id}` (Note: uses /api/ not /v1/)
- **Path Parameter**: Enter your userId
- **Expected**: Status 200, user data returned

**✅ Checklist:**
- [ ] Can view own profile
- [ ] Can update profile (username, bio)
- [ ] Can view user statistics
- [ ] API versioning inconsistency noted (/api/users vs /v1/user)

---

## 3️⃣ Post Creation & Management (15 minutes)

### Tests to Run:

#### Test 3.1: Create a Post
- **Endpoint**: `POST /v1/posts/create`
- **Body**:
  ```json
  {
    "title": "My First Test Post",
    "content": "This is test content for my post. I'm testing the API!",
    "emotion": "happy",
    "preImg": "https://example.com/image.jpg"
  }
  ```
- **Expected**: Status 201, post created with ID
- **Save**: Copy the `postId` from response

#### Test 3.2: Get All Posts
- **Endpoint**: `GET /v1/posts`
- **Parameters**: 
  - page: 0
  - size: 20
- **Expected**: Status 200, list of posts including yours

#### Test 3.3: Get Post by ID
- **Endpoint**: `GET /v1/posts/{id}`
- **Path Parameter**: Your postId
- **Expected**: Status 200, detailed post data

#### Test 3.4: Get Posts by User
- **Endpoint**: `GET /v1/posts/by-user/{userId}`
- **Path Parameter**: Your userId
- **Expected**: Status 200, list of your posts

#### Test 3.5: Update Your Post
- **Endpoint**: `PUT /v1/posts/{id}`
- **Path Parameter**: Your postId
- **Body**:
  ```json
  {
    "title": "Updated Test Post",
    "content": "This is updated content",
    "emotion": "excited"
  }
  ```
- **Expected**: Status 200, post updated

#### Test 3.6: Track Post View
- **Endpoint**: `POST /v1/posts/{id}/view`
- **Path Parameter**: Your postId
- **Expected**: Status 200, view count incremented

#### Test 3.7: Get Post View Stats
- **Endpoint**: `GET /v1/posts/{id}/view-stats`
- **Path Parameter**: Your postId
- **Expected**: Status 200, view statistics

**✅ Checklist:**
- [ ] Can create posts
- [ ] Can retrieve all posts
- [ ] Can get specific post by ID
- [ ] Can get posts by user
- [ ] Can update own posts
- [ ] View tracking works
- [ ] Can get view statistics

---

## 4️⃣ Post Engagement Testing (10 minutes)

### Tests to Run:

#### Test 4.1: Like a Post (Resonance)
- **Endpoint**: `PUT /v1/posts/{id}/resonance`
- **Path Parameter**: Your postId
- **Expected**: Status 200, `isresonanced: true`, resonance count +1

#### Test 4.2: Unlike the Post
- **Endpoint**: `PUT /v1/posts/{id}/resonance` (same endpoint)
- **Path Parameter**: Your postId
- **Expected**: Status 200, `isresonanced: false`, resonance count -1

#### Test 4.3: Save a Post (Collection)
- **Endpoint**: `PUT /v1/posts/{id}/collect`
- **Path Parameter**: Your postId
- **Expected**: Status 200, `isCollected: true`, collect count +1

#### Test 4.4: Unsave the Post
- **Endpoint**: `PUT /v1/posts/{id}/collect` (same endpoint)
- **Path Parameter**: Your postId
- **Expected**: Status 200, `isCollected: false`, collect count -1

#### Test 4.5: Get Liked Posts
- **Endpoint**: `GET /v1/user/liked-posts`
- **Parameters**: page: 1, limit: 20
- **Expected**: Status 200, list of posts you liked

#### Test 4.6: Get Saved Posts
- **Endpoint**: `GET /v1/user/saved-posts`
- **Parameters**: page: 1, limit: 20
- **Expected**: Status 200, list of posts you saved

**✅ Checklist:**
- [ ] Can like/unlike posts
- [ ] Resonance count updates correctly
- [ ] Can save/unsave posts
- [ ] Collection count updates correctly
- [ ] Can view liked posts list
- [ ] Can view saved posts list

---

## 5️⃣ Feed & Discovery Testing (15 minutes)

### Tests to Run:

#### Test 5.1: Get Regular Feed
- **Endpoint**: `GET /v1/posts/feed`
- **Parameters**:
  - page: 0
  - size: 20
  - sortBy: newest
- **Expected**: Status 200, paginated feed response

#### Test 5.2: Get Feed with Emotion Filter
- **Endpoint**: `GET /v1/posts/feed`
- **Parameters**:
  - page: 0
  - size: 20
  - sortBy: newest
  - emotion: happy
- **Expected**: Status 200, only "happy" emotion posts

#### Test 5.3: Get Personalized Feed Blocks
- **Endpoint**: `GET /v1/posts/feed/blocks`
- **Parameters**:
  - blockNumber: 1
  - pageSize: 20
  - prefetch: false
- **Expected**: Status 200, feed block with recommendation data

#### Test 5.4: Search Posts (Basic)
- **Endpoint**: `GET /v1/search/posts`
- **Parameters**:
  - q: test
  - page: 1
  - limit: 20
  - sort: relevance
- **Expected**: Status 200, search results

#### Test 5.5: Advanced Search
- **Endpoint**: `GET /v1/search/posts/advanced`
- **Parameters**:
  - q: test
  - page: 0
  - size: 20
- **Expected**: Status 200, relevance-scored results

#### Test 5.6: Get Trending Posts
- **Endpoint**: `GET /v1/trending`
- **Parameters**:
  - timeframe: week
  - limit: 10
- **Expected**: Status 200, trending posts

#### Test 5.7: Get Leaderboard
- **Endpoint**: `GET /v1/leaderboard`
- **Parameters**:
  - metric: resonance
  - timeframe: week
  - limit: 10
- **Expected**: Status 200, top posts by metric

**✅ Checklist:**
- [ ] Regular feed works
- [ ] Can filter by emotion
- [ ] Personalized feed blocks work
- [ ] Basic search returns results
- [ ] Advanced search works
- [ ] Trending posts display
- [ ] Leaderboard calculates correctly

---

## 6️⃣ Comments Testing (15 minutes)

### Tests to Run:

#### Test 6.1: Get Comments for Post
- **Endpoint**: `GET /v1/posts/{postId}/comments`
- **Path Parameter**: Your postId
- **Parameters**: page: 1, limit: 20
- **Expected**: Status 200, empty list initially

#### Test 6.2: Add Comment to Post
- **Endpoint**: `POST /v1/posts/{postId}/comments`
- **Path Parameter**: Your postId
- **Body**:
  ```json
  {
    "content": "This is my first test comment!"
  }
  ```
- **Expected**: Status 201, comment created
- **Save**: Copy the `commentId`

#### Test 6.3: Add Reply to Comment
- **Endpoint**: `POST /v1/posts/comments/{commentId}/replies`
- **Path Parameter**: Your commentId
- **Body**:
  ```json
  {
    "content": "This is a reply to the comment"
  }
  ```
- **Expected**: Status 201, reply created
- **Save**: Copy the reply `commentId`

#### Test 6.4: Like a Comment
- **Endpoint**: `PUT /v1/posts/comments/{commentId}/like`
- **Path Parameter**: Your commentId
- **Body**:
  ```json
  {
    "action": "like"
  }
  ```
- **Expected**: Status 200, comment liked

#### Test 6.5: Unlike Comment
- **Endpoint**: `PUT /v1/posts/comments/{commentId}/like`
- **Body**:
  ```json
  {
    "action": "unlike"
  }
  ```
- **Expected**: Status 200, like removed

#### Test 6.6: Report Comment
- **Endpoint**: `POST /v1/posts/comments/{commentId}/report`
- **Path Parameter**: Your commentId
- **Body**:
  ```json
  {
    "reason": "TEST_SPAM",
    "description": "Testing the report functionality"
  }
  ```
- **Expected**: Status 201, comment reported

#### Test 6.7: Delete Comment
- **Endpoint**: `DELETE /v1/posts/comments/{commentId}`
- **Path Parameter**: Your commentId
- **Expected**: Status 200, comment deleted

**✅ Checklist:**
- [ ] Can view comments on post
- [ ] Can add comment to post
- [ ] Can add reply to comment
- [ ] Can like/unlike comments
- [ ] Comment counts update correctly
- [ ] Can report comments
- [ ] Can delete own comments
- [ ] Moderation system triggers (check logs)

---

## 7️⃣ Conversations & AI Chat (10 minutes)

### Tests to Run:

#### Test 7.1: Get All Conversations
- **Endpoint**: `GET /v1/conversations`
- **Parameters**: page: 1, limit: 20
- **Expected**: Status 200, empty list initially

#### Test 7.2: Create Conversation
- **Endpoint**: `POST /v1/conversations`
- **Body**:
  ```json
  {
    "title": "Test Conversation"
  }
  ```
- **Expected**: Status 201, conversation created
- **Save**: Copy the `conversationId`

#### Test 7.3: Get Single Conversation
- **Endpoint**: `GET /v1/conversations/{conversationId}`
- **Path Parameter**: Your conversationId
- **Expected**: Status 200, conversation details

#### Test 7.4: Send Message in Conversation
- **Endpoint**: `POST /v1/conversations/{conversationId}/messages`
- **Path Parameter**: Your conversationId
- **Body**:
  ```json
  {
    "content": "Hello, this is a test message!"
  }
  ```
- **Expected**: Status 201, message sent

#### Test 7.5: Send Message to AI Chat
- **Endpoint**: `POST /v1/ai-chat/send-message`
- **Body**:
  ```json
  {
    "message": "Hello AI, how are you?",
    "conversationId": null
  }
  ```
- **Expected**: Status 200, AI response
- **Note**: Check if AI service is configured

#### Test 7.6: Delete Conversation
- **Endpoint**: `DELETE /v1/conversations/{conversationId}`
- **Path Parameter**: Your conversationId
- **Expected**: Status 200, conversation deleted

**✅ Checklist:**
- [ ] Can create conversations
- [ ] Can view conversations list
- [ ] Can send messages in conversation
- [ ] Can delete conversations
- [ ] AI chat responds (if configured)

---

## 8️⃣ Drafts Testing (10 minutes)

### Tests to Run:

#### Test 8.1: Get All Drafts
- **Endpoint**: `GET /v1/drafts`
- **Parameters**: page: 1, limit: 20
- **Expected**: Status 200, empty list initially

#### Test 8.2: Save Draft
- **Endpoint**: `POST /v1/drafts`
- **Body**:
  ```json
  {
    "title": "Draft Post Title",
    "content": "This is draft content that I'll publish later",
    "emotion": "thoughtful",
    "topics": ["testing", "api"]
  }
  ```
- **Expected**: Status 201, draft saved
- **Save**: Copy the `draftId`

#### Test 8.3: Update Draft
- **Endpoint**: `PUT /v1/drafts/{draftId}`
- **Path Parameter**: Your draftId
- **Body**:
  ```json
  {
    "title": "Updated Draft Title",
    "content": "Updated draft content"
  }
  ```
- **Expected**: Status 200, draft updated

#### Test 8.4: Get Single Draft
- **Endpoint**: `GET /v1/drafts/{draftId}`
- **Path Parameter**: Your draftId
- **Expected**: Status 200, draft details

#### Test 8.5: Publish Draft
- **Endpoint**: `POST /v1/drafts/{draftId}/publish`
- **Path Parameter**: Your draftId
- **Expected**: Status 200, draft published as post
- **Save**: Copy the new `postId`

#### Test 8.6: Delete Draft
- **Endpoint**: `DELETE /v1/drafts/{draftId}`
- **Path Parameter**: Your draftId
- **Expected**: Status 200, draft deleted

**✅ Checklist:**
- [ ] Can create drafts
- [ ] Can update drafts
- [ ] Can retrieve drafts
- [ ] Can publish drafts as posts
- [ ] Can delete drafts

---

## 9️⃣ Notifications Testing (5 minutes)

### Tests to Run:

#### Test 9.1: Get Notifications
- **Endpoint**: `GET /v1/notifications`
- **Parameters**: page: 1, limit: 20
- **Expected**: Status 200, notifications list (may be empty)

#### Test 9.2: Mark Notification as Read
- **Endpoint**: `POST /v1/notifications/{notificationId}/read`
- **Path Parameter**: A notification ID (if any exist)
- **Expected**: Status 200, notification marked read

#### Test 9.3: Mark All as Read
- **Endpoint**: `POST /v1/notifications/read-all`
- **Expected**: Status 200, all notifications marked read

**✅ Checklist:**
- [ ] Can retrieve notifications
- [ ] Can mark individual as read
- [ ] Can mark all as read
- [ ] Notifications generate from actions (like, comment, etc.)

---

## 🔟 Articles Testing (10 minutes)

### Tests to Run:

#### Test 10.1: Get Published Articles (Public)
- **Endpoint**: `GET /v1/articles`
- **Auth**: Not required
- **Expected**: Status 200, list of published articles

#### Test 10.2: Get Single Article (Public)
- **Endpoint**: `GET /v1/articles/view/{id}`
- **Path Parameter**: An article ID
- **Expected**: Status 200, article content

#### Test 10.3: Search Articles
- **Endpoint**: `GET /v1/articles/search`
- **Parameters**:
  - query: test
  - page: 0
  - limit: 20
- **Expected**: Status 200, search results

### Admin-Only Tests (Need Admin Token):

#### Test 10.4: Get All Articles (Admin)
- **Endpoint**: `GET /v1/articles/admin`
- **Auth**: Admin JWT required
- **Parameters**: page: 0, limit: 20, publishedOnly: false
- **Expected**: Status 200, all articles including unpublished

#### Test 10.5: Create Article (Admin)
- **Endpoint**: `POST /v1/articles/create`
- **Auth**: Admin JWT required
- **Body**:
  ```json
  {
    "title": "Test Article",
    "content": "<p>This is test article content</p>",
    "intro": "A brief introduction",
    "coverImage": "https://example.com/image.jpg",
    "published": false
  }
  ```
- **Expected**: Status 201, article created
- **Save**: Copy `articleId`

#### Test 10.6: Update Article (Admin)
- **Endpoint**: `PUT /v1/articles/edit/{id}`
- **Path Parameter**: Your articleId
- **Body**: Similar to create but with updated content
- **Expected**: Status 200, article updated

#### Test 10.7: Toggle Publish Status (Admin)
- **Endpoint**: `POST /v1/articles/manage/{id}/toggle-publish`
- **Path Parameter**: Your articleId
- **Expected**: Status 200, publish status toggled

**✅ Checklist:**
- [ ] Public can view published articles
- [ ] Article search works
- [ ] Admin can create articles
- [ ] Admin can update articles
- [ ] Admin can toggle publish status
- [ ] Unpublished articles not visible to public

---

## 1️⃣1️⃣ Admin Testing (15 minutes)

**Note**: You'll need admin credentials for these tests.

### Get Admin Token:

1. **Register Admin** (if not exists):
   - Endpoint: `POST /v1/auth/admin/register`
   - Body:
     ```json
     {
       "email": "admin@test.com",
       "password": "Admin123!",
       "username": "testadmin",
       "firstName": "Test",
       "lastName": "Admin",
       "adminSecretKey": "YOUR_ADMIN_SECRET_KEY"
     }
     ```
   - **Note**: Get the secret key from your backend configuration

2. **Or Login as Admin**:
   - Endpoint: `POST /v1/auth/admin/login`
   - Body:
     ```json
     {
       "email": "admin@test.com",
       "password": "Admin123!"
     }
     ```

3. **Re-authorize Swagger** with admin token

### Admin Tests:

#### Test 11.1: Get All Users
- **Endpoint**: `GET /api/users` (Note: /api/ not /v1/)
- **Expected**: Status 200, list of all users

#### Test 11.2: Delete User
- **Endpoint**: `DELETE /api/users/{id}`
- **Path Parameter**: A test user ID
- **Expected**: Status 204, user deleted

#### Test 11.3: Get Flagged Posts
- **Endpoint**: `GET /admin/v1/posts/flagged`
- **Expected**: Status 200, list of flagged posts

#### Test 11.4: Flag a Post
- **Endpoint**: `POST /admin/v1/posts/flag/{postId}`
- **Path Parameter**: A post ID
- **Body**:
  ```json
  {
    "reason": "Testing moderation"
  }
  ```
- **Expected**: Status 200, post flagged

#### Test 11.5: Unflag a Post
- **Endpoint**: `POST /admin/v1/posts/unflag/{postId}`
- **Path Parameter**: The flagged post ID
- **Expected**: Status 200, post unflagged

#### Test 11.6: Delete Any Post (Admin)
- **Endpoint**: `DELETE /admin/v1/posts/delete/{postId}`
- **Path Parameter**: Any post ID
- **Expected**: Status 200, post deleted (bypasses ownership check)

#### Test 11.7: Get Recommendation Stats
- **Endpoint**: `GET /admin/v1/users/{userId}/recommendation-status`
- **Path Parameter**: A user ID
- **Expected**: Status 200, recommendation engine status

#### Test 11.8: Toggle User Recommendations
- **Endpoint**: `PUT /admin/v1/users/{userId}/recommendation-toggle`
- **Path Parameter**: A user ID
- **Body**:
  ```json
  {
    "enabled": false
  }
  ```
- **Expected**: Status 200, recommendation setting updated

**✅ Checklist:**
- [ ] Admin can view all users
- [ ] Admin can delete users
- [ ] Admin can flag/unflag posts
- [ ] Admin can delete any post
- [ ] Admin can manage user recommendation settings
- [ ] Admin endpoints properly secured (try with regular user token - should fail)

---

## 1️⃣2️⃣ System Health & Monitoring (5 minutes)

### Tests to Run:

#### Test 12.1: Basic Health Check
- **Endpoint**: `GET /v1/health`
- **Auth**: Not required
- **Expected**: Status 200, health status

#### Test 12.2: Actuator Health
- **Endpoint**: `GET /actuator/health`
- **Auth**: Not required
- **Expected**: Status 200, detailed health info

#### Test 12.3: Liveness Probe
- **Endpoint**: `GET /actuator/health/liveness`
- **Expected**: Status 200, liveness status

#### Test 12.4: Readiness Probe
- **Endpoint**: `GET /actuator/health/readiness`
- **Expected**: Status 200, readiness status

#### Test 12.5: Queue Statistics
- **Endpoint**: `GET /v1/queue/statistics`
- **Expected**: Status 200, queue metrics

**✅ Checklist:**
- [ ] Health endpoints respond
- [ ] Actuator endpoints working
- [ ] K8s probes configured correctly
- [ ] Queue system operational

---

## 📝 Testing Notes Template

As you test, document your findings:

```markdown
## Endpoint: [Method] [Path]
- **Status Code**: [Actual vs Expected]
- **Response Time**: [ms]
- **Works?**: ✅ / ❌
- **Issues Found**: 
  - [List any problems]
- **Notes**:
  - [Any observations]
```

---

## 🚨 Common Issues & Solutions

### Issue: 401 Unauthorized
**Solution**: 
- Verify you clicked "Authorize" button
- Check token format: `Bearer YOUR_TOKEN`
- Token might be expired (get new one)

### Issue: 403 Forbidden
**Solution**:
- Endpoint requires admin role
- Register/login as admin
- Use admin token in Authorization

### Issue: 404 Not Found
**Solution**:
- Check endpoint path is correct
- Verify ID exists (postId, userId, etc.)
- Check if resource was deleted

### Issue: 400 Bad Request
**Solution**:
- Check JSON format is valid
- Verify all required fields included
- Check field types match (string, number, etc.)

### Issue: 500 Internal Server Error
**Solution**:
- Check backend console logs
- Verify database is running
- Check Redis is running (if required)
- Look for stack traces in logs

---

## 📊 Testing Checklist Summary

After completing all tests, you should know:

### What Works ✅
- [ ] Which endpoints are functional
- [ ] Which features are complete
- [ ] Response times are acceptable
- [ ] Authentication works properly
- [ ] Data persists correctly

### What Doesn't Work ❌
- [ ] Endpoints returning errors
- [ ] Missing features
- [ ] Broken integrations
- [ ] Security issues
- [ ] Performance problems

### What's Unnecessary 🗑️
- [ ] Endpoints never called by frontends
- [ ] Duplicate functionality
- [ ] Test endpoints in production
- [ ] Unimplemented features

---

## 📈 Next Steps After Testing

1. **Document Results**: Create a spreadsheet with all tested endpoints and their status

2. **Compare with Frontend Usage**: Cross-reference your test results with the `FRONTEND_API_USAGE_MAP.md`

3. **Update Recommendations**: Based on your testing, adjust the action plan priorities

4. **Fix Critical Issues**: Address any broken endpoints used by frontends

5. **Remove Confirmed Unused**: Delete endpoints that don't work and aren't used

---

## 💡 Pro Tips

1. **Use Postman Alternative**: Export OpenAPI spec and import to Postman for easier testing
   ```bash
   http://localhost:8080/api-docs
   ```

2. **Save Useful Requests**: Bookmark or save requests you'll use frequently

3. **Test Error Cases**: Try invalid data to see error handling

4. **Check Logs**: Watch backend console while testing to see what's happening

5. **Performance**: Note which endpoints are slow (>1 second)

6. **Database**: Check database after tests to verify data persistence

---

## 🎯 Focus Areas

Based on your analysis document, prioritize testing:

### High Priority (Used by iOS)
1. Authentication endpoints
2. Post CRUD operations
3. Feed endpoints
4. Search functionality
5. AI chat

### Medium Priority (Used by Admin)
1. User management
2. Post moderation
3. Articles management
4. Analytics endpoints

### Low Priority (Unused)
1. Test controllers
2. Deployment endpoints
3. Unused features (consent, interests)

---

**Time Estimate**: 
- Quick Test (Core Features Only): 1 hour
- Comprehensive Test (All Above): 2-3 hours
- With Documentation: 3-4 hours

Start with authentication and core features, then expand to other areas as time permits!


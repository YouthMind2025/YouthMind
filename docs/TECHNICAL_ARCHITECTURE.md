# YouthMind: High-Level Technical Architecture & Technology Stack

*A clear, concise overview of the technology choices and component interactions for building YouthMind*

Below is a comprehensive guide focusing on which technologies to use, how each part fits together, and who on the team is responsible. The UI designs and team composition are already finalized—this guide focuses on the technical implementation strategy.

---

## 1. Core Components & Their Responsibilities

### iOS Mobile App (SwiftUI)

**Purpose**: Provides the user interface for anonymous posting, feed browsing, AI-driven sentiment insights, emotion museum, avatar/cover creation, and notifications.

**Technology**: Swift 5.5+ with SwiftUI (MVVM architecture, Combine for reactive flows).

**Team**: Dawei, Dexter, Youssef.

**Key Features**:
- Anonymous user session management
- Real-time feed with infinite scroll
- Post creation with emotion selection
- Avatar/cover design tools
- Emotion museum visualization
- Push notifications integration

---

### Backend API (Spring Boot)

**Purpose**: Exposes REST endpoints for user sessions, posts (create/read/delete), comments/resonances, drafts (optional), and admin moderation. Orchestrates communication with the AI service.

**Technology**: Java 17+ with Spring Boot 3.x (Spring Security for JWT authentication, Spring Data JPA for PostgreSQL).

**Team**: Mustafo (API architecture), Junayd (system design/PM), Ahmed (database), Harley (caching & security).

**Key Features**:
- RESTful API with OpenAPI documentation
- JWT-based authentication
- PostgreSQL data persistence
- Redis caching integration
- AI service orchestration
- Admin endpoints for content moderation

---

### AI Microservice (FastAPI)

**Purpose**: Hosts all machine-learning capabilities—sentiment analysis, follow-up question generation, emotion classification, and post summarization.

**Technology**: Python 3.9+ with FastAPI (HuggingFace Transformers / PyTorch), containerized via Docker.

**Team**: Andy (models/NLP), Kevin (service architecture & deployment).

**Key Features**:
- Real-time sentiment analysis
- Emotion-based question generation
- Content summarization
- Empathy scoring algorithms
- Model versioning and A/B testing support

---

### Admin Web Dashboard (React SPA)

**Purpose**: Allows moderators to view flagged content, delete inappropriate posts, and manage basic metrics and user accounts.

**Technology**: React (JavaScript/TypeScript) with a lightweight UI library (e.g., Tailwind CSS or Material-UI).

**Team**: Alex (full-stack).

**Key Features**:
- Content moderation interface
- Real-time statistics dashboard
- User management tools
- Flagged post review system
- Admin authentication and authorization

---

### Database & Caching

**PostgreSQL**: Stores all relational data (users, posts, comments, resonances, AI metadata, notifications, drafts).

**Redis**: Acts as a cache for feed paging and search results, and as a fast counter store for resonance counts or "hot posts."

**Team**: Ahmed (schema design & optimization), Harley (Redis configuration & caching strategy).

**Key Features**:
- JSONB columns for flexible data storage
- Full-text search capabilities
- Optimized indexing strategy
- Redis TTL-based caching
- Pub/Sub for real-time notifications

---

### Infrastructure & DevOps

**Docker Compose**: For local development (PostgreSQL, Redis, backend, AI service, admin dashboard).

**Kubernetes** (or managed container service): For staging/production—deploy backend and AI microservices with autoscaling, TLS termination via an Ingress.

**CI/CD (GitHub Actions)**: Automate builds/tests for mobile, backend, AI, and admin code; push Docker images; deploy to staging/production.

**Team**: Junayd (pipeline coordination), Kevin (AI service deployments), Ahmed & Harley (database & infrastructure), Alex (admin deployment scripts).

---

## 2. High-Level Data Flow

### User Session & Identity

1. **Anonymous Browsing**: New users can immediately view the feed and browse posts without any registration via `GET /posts/feed` (no authentication required)
2. **Registration Required for Interaction**: To create posts, add comments, give resonances, or access personal features, users must register with email or phone via `POST /auth/register`
3. **Anonymous Registration**: 
   - Backend generates a UUID for the user and stores email/phone for authentication only
   - No personally identifiable information is ever displayed or associated with posts
   - User receives a JWT token for all subsequent authenticated interactions
4. **Session Management**: iOS app stores the JWT in iOS Keychain for authenticated features while allowing anonymous browsing

### Creating a New Post

1. **Authentication Check**: Ensure user is registered and has valid JWT token; redirect to registration if not authenticated
2. **User Input**: User composes title, content, selects emotion tag, and optionally picks avatar/cover
3. **API Call**: iOS calls `POST /posts/create` on backend, including JWT and post data
4. **Backend Processing**:
   - Validates JWT (Spring Security) and confirms user registration
   - Persists basic Post record to PostgreSQL with anonymous user UUID (no PII exposure)
   - Calls AI microservice (`POST /ai/sentiment`, `POST /ai/questions`) to compute:
     - Sentiment score
     - Empathy score
     - Follow-up questions
   - Stores AI metadata in separate AI-data table
   - Sends back combined PostDto to iOS client
5. **Cache Update**: Redis invalidates or updates cached feed pages so new post appears at top

### Browsing the Feed

1. **Request**: iOS app requests `GET /posts/feed?page=X&size=Y` (including JWT)
2. **Backend Processing**:
   - Checks Redis cache for `feed:X:Y`
   - If cache miss: queries PostgreSQL for recent posts, assembles PostDto objects (including AI fields), stores result in Redis with 5–10 minute TTL
   - Returns paginated PostDto array and hasNext flag
3. **Display**: iOS displays posts in SwiftUI List or LazyVStack, coloring each card's border based on emotion field

### Emotion Museum (User History)

1. **Request**: iOS "Emotion Museum" screen calls `GET /posts/user/{userId}` to fetch all posts by that user
2. **Backend**: Retrieves all posts for userId (sorted ascending by creation date) and includes emotion tags
3. **Visualization**: iOS renders in timeline or puzzle view, grouping/counting by emotion type

### Resonances & Comments

1. **Resonance**: iOS calls `POST /resonances/create` with `{ postId, userId }`
2. **Backend Processing**:
   - Creates Resonance record in PostgreSQL
   - Increments Post.resonanceCount
   - Optionally publishes Redis message (Pub/Sub) for notifications
   - Returns updated resonance count to iOS
3. **Comments**: iOS calls `POST /comments/create`, backend stores comment and triggers notification to post author

### Flagging & Admin Moderation

1. **User Flagging**: iOS user flags post via `POST /posts/{postId}/flag`
2. **Backend Processing**:
   - Marks post's flagged field as true in PostgreSQL
   - Sends system notification to moderators (adds Notification entry)
3. **Admin Review**: Admin Dashboard (React) calls `GET /admin/posts?flagged=true` to list flagged posts
4. **Admin Actions**: Administrators log in via `POST /admin/login`, receive admin JWT, can delete flagged posts via `DELETE /admin/posts/{postId}`

### Notifications

1. **Event Triggers**: Backend creates Notification record when events occur (new resonance, comment, admin actions)
2. **Data Structure**: `{ recipientId, type, message, createdAt, read: false }`
3. **Client Retrieval**: iOS calls `GET /notifications?userId={userId}` to fetch unread notifications
4. **Display**: iOS shows badge and separate "Messages" screen grouping notifications by type
5. **Mark as Read**: Future feature via `POST /notifications/mark-read`

---

## 3. Technology Stack Summary

| Component | Framework / Language | Key Libraries / Tools | Team Lead(s) |
|-----------|---------------------|----------------------|--------------|
| **iOS Mobile App** | Swift 5.5+ / SwiftUI | Combine, CoreData, URLSession, Firebase Crashlytics | Dawei, Dexter, Youssef |
| **Backend API** | Java 17+ / Spring Boot | Spring Security (JWT), Spring Data JPA (PostgreSQL), Spring Cache (Redis), OpenAPI/Swagger | Mustafo, Junayd, Ahmed, Harley |
| **AI Microservice** | Python 3.9+ / FastAPI | HuggingFace Transformers, PyTorch (or TensorFlow), Uvicorn, Docker | Andy, Kevin |
| **Admin Dashboard** | React (JavaScript/TS) | React Router, Axios, Tailwind CSS (or Material-UI), Jest | Alex |
| **Database** | PostgreSQL 14+ | Hibernate ORM (JPA), JSONB columns, Full-Text Search (GIN) | Ahmed |
| **Caching** | Redis 7+ | Spring Cache abstraction, TTL-based feed & search cache, Pub/Sub | Harley |
| **Containerization** | Docker & Kubernetes | Docker Compose (local), Kubernetes manifests & Ingress, Helm (optional) | Junayd, Kevin, Ahmed, Harley |
| **CI/CD** | GitHub Actions | Maven (Java), pytest (Python), npm (React), xcodebuild (iOS) | Junayd |
| **Monitoring & Logs** | Prometheus & Grafana | Spring Actuator `/actuator/prometheus`, FastAPI metrics, Grafana dashboards, ELK/EFK (optional) | Ahmed, Harley, Kevin |

---

## 4. Team Responsibilities & Onboarding Checklist

Use the following checklist to ensure every team member knows their responsibilities and can start coding immediately:

### Dawei / Dexter / Youssef (iOS Team)

**Setup & Configuration**:
- [ ] Verify that the UI designs (SwiftUI views) match the Figma/Sketch prototypes
- [ ] Clone the `mobile/` folder, open `YouthMindApp.xcodeproj`
- [ ] Update the `baseURL` constants to point to the local backend (`http://localhost:8080`)
- [ ] Configure Firebase Crashlytics with `GoogleService-Info.plist`

**Architecture Implementation**:
- [ ] Confirm that Combine-based view models are set up to call the correct endpoints:
  - `FeedViewModel` → `GET /posts/feed` (anonymous access)
  - `AuthViewModel` → `POST /auth/register` (email/phone registration)
  - `PostViewModel` → `POST /posts/create` (authenticated)
  - `NotificationViewModel` → `GET /notifications` (authenticated)
  - `EmotionMuseumViewModel` → `GET /posts/user/{userId}` (authenticated)
- [ ] Ensure CoreData stack is configured for local drafts (optional) and caching of read notifications
- [ ] Implement `SessionManager` for JWT token management in iOS Keychain
- [ ] Set up `APIClient` with proper error handling and retry logic
- [ ] Create registration flow with email/phone input validation and OTP verification

**Key Deliverables**:
- Anonymous feed browsing (no authentication required)
- Email/phone registration flow with OTP verification
- Anonymous session creation and JWT management
- Post creation with emotion selection and AI integration (authenticated users only)
- Feed browsing with infinite scroll and pull-to-refresh
- Avatar creation and cover design tools (authenticated users only)
- Emotion museum with timeline and puzzle views (authenticated users only)
- Push notifications for resonances and comments (authenticated users only)

---

### Mustafo / Junayd / Ahmed / Harley (Backend Team)

**Environment Setup**:
- [ ] Clone the `backend/` folder; set up a local PostgreSQL instance (or Docker Compose)
- [ ] Update `application.yml` with local DB credentials, Redis host (`redis:6379`), and AI service URL (`http://localhost:8001`)
- [ ] Run `mvn clean package` → `java -jar target/youthmind-backend.jar`
- [ ] Confirm that the OpenAPI/Swagger UI is accessible at `http://localhost:8080/swagger-ui.html`

**Core Implementation**:
- [ ] Implement the core controllers using DTOs for request/response:
  - `AuthController` → Email/phone registration with OTP verification
  - `PostController` → CRUD operations with AI integration (authenticated only)
  - `CommentController` → Comment creation and retrieval (authenticated only)
  - `ResonanceController` → Like/unlike functionality (authenticated only)
  - `NotificationController` → User notification management (authenticated only)
  - `AdminController` → Content moderation endpoints
- [ ] Configure Spring Security to allow anonymous access to feed browsing and require JWT validation for all interactive endpoints
- [ ] Define JPA entities and repositories:
  - `User` (UUID, email/phone hash, registration timestamp), `Post`, `Comment`, `Resonance`, `AIData`, `Notification`, `Draft`
- [ ] Implement OTP verification service with email/SMS integration (Twilio or AWS SNS)

**Caching & Performance**:
- [ ] Set up Redis caching for `/posts/feed` (anonymous access), with a 5-minute TTL
- [ ] Implement cache invalidation strategies for new post creation
- [ ] Configure connection pooling and query optimization
- [ ] Add Spring Actuator health checks and metrics

**Security Implementation**:
- [ ] JWT token generation and validation for registered users
- [ ] Email/phone verification with time-limited OTP codes
- [ ] Role-based access control for admin endpoints
- [ ] Rate limiting for registration attempts and anonymous API calls
- [ ] Hash storage of email/phone numbers for privacy protection

---

### Andy / Kevin (AI Team)

**Environment Setup**:
- [ ] Clone the `ai/` folder; create a Python virtual environment
- [ ] Install dependencies from `requirements.txt`
- [ ] Verify that sentiment analysis, question generation, and emotion classification models are present under `ai/models/`
- [ ] If models are missing, download or fine-tune the necessary HuggingFace checkpoints

**Service Implementation**:
- [ ] Start the FastAPI service: `uvicorn app:app --reload --port 8001`
- [ ] Manually test endpoints via curl or Postman:
  - `POST http://localhost:8001/ai/sentiment` with `{ "text": "I feel sad today" }`
  - `POST http://localhost:8001/ai/questions` with `{ "text": "I am stressed about exams" }`
- [ ] Ensure returned JSON matches the expected schema:
  - `{ "sentiment_score": 0.2, "empathy_score": 0.8 }`
  - `{ "questions": ["What pressures do you feel?", "How can you manage stress?"] }`

**Model Integration**:
- [ ] Implement real-time sentiment analysis using `cardiffnlp/twitter-roberta-base-sentiment-latest`
- [ ] Create emotion classification pipeline with `j-hartmann/emotion-english-distilroberta-base`
- [ ] Build rule-based question generation system with emotion-specific templates
- [ ] Add empathy scoring algorithm based on emotion analysis

**Deployment Preparation**:
- [ ] Containerize the service with a Dockerfile for easy deployment
- [ ] Add health check endpoints for monitoring
- [ ] Implement error handling and fallback responses
- [ ] Set up model versioning for A/B testing

---

### Alex (Admin Dashboard)

**Project Setup**:
- [ ] Clone the `admin/` folder; run `npm install` → `npm start`
- [ ] Update the Axios base URL to `http://localhost:8080` (backend)
- [ ] Ensure the "Flagged Posts" page fetches from `/admin/posts?flagged=true`

**Authentication Implementation**:
- [ ] Implement a simple login flow (MVP: accept a hard-coded admin credential) to obtain an admin JWT
- [ ] Set up token storage in `localStorage` with automatic refresh
- [ ] Add route protection to redirect to login if no token is present

**Core Features**:
- [ ] Create a basic dashboard that lists flagged posts and allows deletion via `DELETE /admin/posts/{postId}`
- [ ] Implement real-time statistics display:
  - Total posts count
  - Flagged posts count
  - Active users count
  - Today's posts count
- [ ] Add post flagging/unflagging functionality
- [ ] Create responsive design with Tailwind CSS or Material-UI

**UI Components**:
- [ ] Admin login page with error handling
- [ ] Post management interface with filtering options
- [ ] Statistics dashboard with charts and metrics
- [ ] Content moderation workflow
- [ ] User management tools (future feature)

---

### DevOps / Infrastructure Team

**Local Development Environment**:
- [ ] In the top-level `infrastructure/docker-compose.yml`, verify services: `postgres`, `redis`, `backend`, `ai-service`, `admin-ui`
- [ ] Run `docker-compose up --build` from the `infrastructure/` folder to spin up all services
- [ ] Validate that all services are reachable:
  - `http://localhost:5432` (PostgreSQL) is reachable
  - `http://localhost:6379` (Redis) is up
  - `http://localhost:8080` (Backend) is serving the API
  - `http://localhost:8001` (AI) serves AI endpoints
  - `http://localhost:3000` (Admin) loads the React dashboard

**Production Infrastructure**:
- [ ] Prepare Kubernetes manifests (under `infrastructure/k8s/`) for staging/production:
  - `deployment-backend.yaml`, `service-backend.yaml`
  - `deployment-ai.yaml`, `service-ai.yaml`
  - `configmap.yaml` for environment variables
  - `secret.yaml` for sensitive data (JWT secrets, DB passwords)
- [ ] Set up an Ingress resource for domain routing:
  - `youthmind.com` → Backend API
  - `ai.youthmind.com` → AI Service
  - `admin.youthmind.com` → Admin Dashboard
- [ ] Configure TLS certificates with Let's Encrypt or cert-manager

**CI/CD Pipeline**:
- [ ] Create GitHub Actions workflows for each service:
  - iOS: Build and test with Xcode
  - Backend: Maven build, test, and Docker image creation
  - AI: Python testing and Docker image creation
  - Admin: npm build and deployment
- [ ] Set up automated deployments to staging environment
- [ ] Configure manual approval gates for production deployment

**Monitoring & Logging**:
- [ ] Deploy Prometheus and Grafana for metrics collection
- [ ] Set up centralized logging with ELK stack (optional)
- [ ] Configure alerting for service health and performance
- [ ] Implement backup strategies for PostgreSQL

---

## 5. Putting It All Together

### Data Flow Architecture
- **iOS ⟷ Backend ⟷ AI Microservice**: Seamless communication between mobile app, API gateway, and ML services
- **PostgreSQL**: Long-term storage for users, posts, comments, and metadata
- **Redis**: High-performance caching for feeds, search results, and real-time counters

### Authentication Strategy
- **Anonymous Browsing**: Unrestricted access to view posts and browse feed without any registration
- **Email/Phone Registration**: Required for all interactive features (posting, commenting, resonating, notifications)
- **Anonymous Identity**: Email/phone used only for authentication - no PII displayed or linked to posts  
- **JWT Tokens**: Secure authentication for registered users with role-based access for admin functions
- **OTP Verification**: Time-limited verification codes for secure registration process

### Caching Implementation
- **Redis Cache**: Feed pages and search results with intelligent TTL policies
- **Real-time Counters**: Resonance counts, user engagement metrics
- **Cache Invalidation**: Smart cache updates on new content creation

### Deployment Strategy
- **Development**: Docker Compose for consistent local environment across all teams
- **Staging**: Kubernetes cluster with staging data for integration testing
- **Production**: Managed container service with autoscaling, load balancing, and monitoring
- **CI/CD**: GitHub Actions for automated testing, building, and deployment

### Team Coordination
- **Daily Standups**: Cross-team alignment on integration points and blockers
- **API Contract**: OpenAPI specification as the source of truth for all integrations
- **Testing Strategy**: Unit tests, integration tests, and end-to-end testing across all components
- **Documentation**: Living documentation updated with each release cycle

---

*This architecture provides a scalable, maintainable foundation for YouthMind's anonymous youth mental health platform, with clear separation of concerns and team responsibilities.* 
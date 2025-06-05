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

1. **First Launch**: iOS app requests a new anonymous session from `POST /auth/session`
2. **Backend Processing**: 
   - Generates a UUID for the user
   - Issues a short-lived JWT token
3. **Client Storage**: Mobile app stores the JWT in iOS Keychain (handled by IdentityManager) for all subsequent calls

### Creating a New Post

1. **User Input**: User composes title, content, selects emotion tag, and optionally picks avatar/cover
2. **API Call**: iOS calls `POST /posts/create` on backend, including JWT and post data
3. **Backend Processing**:
   - Validates JWT (Spring Security)
   - Persists basic Post record to PostgreSQL
   - Calls AI microservice (`POST /ai/sentiment`, `POST /ai/questions`) to compute:
     - Sentiment score
     - Empathy score
     - Follow-up questions
   - Stores AI metadata in separate AI-data table
   - Sends back combined PostDto to iOS client
4. **Cache Update**: Redis invalidates or updates cached feed pages so new post appears at top

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

## 4. Security & Authentication

### JWT Authentication Flow
- **Anonymous Sessions**: UUID-based user identification with JWT tokens
- **Token Management**: Short-lived access tokens with refresh token rotation
- **Role-Based Access**: Admin endpoints protected with role-based JWT claims
- **API Security**: All endpoints secured with Spring Security filters

### Data Protection
- **Encryption**: TLS/HTTPS for all API communications
- **Data Anonymization**: No personally identifiable information stored
- **Content Moderation**: AI-powered content filtering with human review
- **Privacy by Design**: Minimal data collection and retention policies

---

## 5. Performance & Scalability

### Caching Strategy
- **Redis Cache**: Feed pagination, search results, hot posts
- **TTL Policy**: 5-10 minute cache expiration for dynamic content
- **Cache Invalidation**: Smart invalidation on new post creation

### Database Optimization
- **Indexing**: Optimized indexes for common queries (user_id, created_at, emotion)
- **Connection Pooling**: HikariCP for efficient database connections
- **Query Optimization**: JPA query optimization and N+1 prevention

### Horizontal Scaling
- **Microservice Architecture**: Independent scaling of AI and backend services
- **Load Balancing**: Kubernetes ingress with automatic load distribution
- **Database Scaling**: Read replicas for query-heavy operations

---

## 6. Monitoring & Observability

### Application Metrics
- **Performance**: Response times, throughput, error rates
- **Business Metrics**: Post creation rates, user engagement, sentiment trends
- **AI Model Performance**: Prediction accuracy, processing latency

### Health Checks
- **Service Health**: Spring Actuator health endpoints
- **Database**: Connection pool status and query performance
- **External Dependencies**: AI service availability and response times

### Alerting
- **Error Thresholds**: Automated alerts for error rate spikes
- **Performance Degradation**: Response time and resource utilization alerts
- **Business Metrics**: Unusual patterns in user behavior or content

---

## 7. Development & Deployment

### Local Development
```bash
# Start all services with Docker Compose
docker-compose up -d

# Services available at:
# - Backend API: http://localhost:8080
# - AI Service: http://localhost:8001  
# - Admin Dashboard: http://localhost:3000
# - PostgreSQL: localhost:5432
# - Redis: localhost:6379
```

### CI/CD Pipeline
1. **Code Commit**: Trigger on push to main branch
2. **Build & Test**: Parallel builds for all services
3. **Security Scanning**: Dependency and container security checks
4. **Deploy to Staging**: Automated deployment to staging environment
5. **Integration Tests**: End-to-end testing in staging
6. **Production Deployment**: Manual approval gate for production

### Environment Management
- **Development**: Local Docker Compose setup
- **Staging**: Kubernetes cluster with staging data
- **Production**: Production Kubernetes with monitoring and backup

---

*This architecture provides a scalable, maintainable foundation for YouthMind's anonymous youth mental health platform, with clear separation of concerns and team responsibilities.* 
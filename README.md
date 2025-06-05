# YouthMind (SaySomething App)

**YouthMind** is a next-generation mobile application designed to revolutionize youth mental health support through cutting-edge AI technology, anonymous community engagement, and personalized emotional intelligence.

## 🎯 Project Overview

Our platform creates a safe space where young people can express themselves anonymously, receive AI-powered emotional support, and connect with peers facing similar challenges. The app combines advanced sentiment analysis, personalized content generation, and community-driven understanding to provide comprehensive mental health assistance.

### Core Mission
- Provide accessible mental health support for youth through anonymous interaction
- Foster judgment-free community discussions with privacy-first design
- Leverage AI to deliver personalized emotional guidance and question generation
- Create measurable impact through data-driven insights and empathy scoring

## 🌟 Key Features

### Anonymous Browsing & Secure Registration
- **Open Feed Access**: Users can browse posts without any registration
- **Privacy-First Registration**: Email/phone registration with OTP verification for interactions
- **Anonymous Identity**: No personally identifiable information linked to posts
- **Secure Authentication**: JWT-based session management with anonymous user UUIDs

### AI-Powered Emotional Support
- **Real-Time Sentiment Analysis**: Advanced emotion detection using HuggingFace Transformers
- **Personalized Question Generation**: Context-aware conversation starters based on emotional state
- **Empathy Scoring**: AI-driven understanding measurement and response quality assessment
- **Smart Content Moderation**: Automated flagging with human review workflow

### Community Engagement
- **Interactive Feed**: Infinite scroll with emotion-based post categorization
- **Resonance System**: Community-driven empathy and understanding indicators
- **Anonymous Commenting**: Safe peer-to-peer support and discussion
- **Content Flagging**: Community-powered moderation for safe spaces

### Personal Wellness Journey
- **Emotion Museum**: Personal emotional timeline with puzzle and chart visualizations
- **Avatar & Cover Creation**: Personalized visual identity for anonymous expression
- **Progress Tracking**: Long-term emotional wellness trend analysis
- **Draft Management**: Private space for content creation and reflection

## 🏗️ System Architecture

### Technology Stack
- **📱 Mobile Frontend**: Swift 5.5+ with SwiftUI, Combine for reactive programming
- **⚙️ Backend API**: Java 17+ with Spring Boot 3.x, Spring Security for JWT authentication
- **🤖 AI Microservice**: Python 3.9+ with FastAPI, HuggingFace Transformers for ML models
- **🌐 Admin Dashboard**: React with TypeScript, Tailwind CSS for responsive design
- **💾 Data Storage**: PostgreSQL 14+ with JSONB columns, Redis 7+ for caching
- **🐳 Infrastructure**: Docker & Kubernetes, GitHub Actions for CI/CD

### 1. iOS Client (SwiftUI)

**Features**: Anonymous browsing, user registration/login, post creation, resonance ("like") system, commenting, emotion museum, avatar/cover design, push notifications.

**Backend Interactions**:

- **Anonymous Browsing**: Direct `GET /v1/posts/feed` to retrieve cached/database post lists
- **Registration/Login**: `POST /v1/auth/register` (email/phone → OTP verification → generate anonymous UUID and JWT), JWT stored in Keychain
- **Post Creation**: `POST /v1/posts/create` (with JWT, title, content, emotion tags, optional avatar/cover). Response includes AI-computed sentiment scores and question suggestions
- **Resonance/Comments**: Call `POST /v1/resonances/create` and `POST /v1/comments/create` respectively, backend writes to database, updates counters, generates notifications
- **Emotion Museum**: `GET /v1/posts/user/{userId}` to retrieve all user posts in chronological order, client renders timeline or puzzle view
- **Notifications**: `GET /v1/notifications?userId={}` to pull unread messages, mark as read with `POST /v1/notifications/mark-read`

### 2. Admin Dashboard (React SPA)

**Features**: Admin login, view "flagged" reports list, delete violating posts, view basic statistics (total posts, daily new posts, active users).

**Backend Interactions**:

- **Login**: `POST /v1/admin/login` → returns admin JWT
- **Fetch Review Queue**: `GET /v1/admin/posts?flagged=true` to display all posts marked as "flagged"
- **Delete**: `DELETE /v1/admin/posts/{postId}` to delete corresponding database records and synchronize cache cleanup

### 3. Backend Core (Spring Boot)

**Role**: Central business logic hub responsible for authentication, routing, data access, AI service calls, caching strategies, security validation.

**Main Modules**:

**Authentication**: Spring Security + JWT
- Anonymous endpoints (like `/v1/posts/feed`) require no JWT; other endpoints must provide valid JWT
- Admin endpoints require additional role validation

**User Module (AuthController)**:
- `POST /v1/auth/register`: Generate UUID, send OTP, verify and store hashed email/phone, issue JWT
- `POST /v1/auth/login` (optional): Allow existing accounts to login for new JWT

**Post Module (PostController)**:
- `GET /v1/posts/feed`: First check Redis cache `feed:{page}:{size}`; on cache miss, query PostgreSQL, assemble DTO, write back to Redis (TTL 5 minutes)
- `POST /v1/posts/create`: Validate JWT, store in database → call AI service (`/v1/ai/sentiment`, `/v1/ai/questions`) → return PostDto with AI fields → update Redis cache (insert new post or clear corresponding pages)
- `GET /v1/posts/user/{userId}`: Query all posts for user (sorted by time), return with emotion tags

**Comment/Resonance Module**:
- `POST /v1/comments/create`: Validate JWT, write to comment table, insert into PostgreSQL → trigger notification to post author
- `POST /v1/resonances/create`: Validate JWT, write to resonance table, increment post `resonance_count` → trigger notification to post author → update Redis cache "hot posts" score or directly delete cache pages

**Reporting/Moderation Module**:
- `POST /v1/posts/{postId}/flag`: Validate JWT, update post `flagged = true` → write notification to admin
- Admin calls `/v1/admin/posts?flagged=true`, `DELETE /v1/admin/posts/{postId}`

**Notification Module (NotificationController)**:
- All events (resonance, comments, reports) insert a row in database `notifications` table `{recipientId, type, message, timestamp}`
- `GET /v1/notifications?userId=`: Pull unread messages
- `POST /v1/notifications/mark-read`: Update `read=true`

**Caching Strategy**:
- **Redis Cache**: `feed:{page}:{size}`, TTL 5 minutes
- **Trending Rankings**: Periodically or real-time update `resonance_count:{postId}`, can use ZSET (sorted sets)
- **User Sessions**: For temporary storage of OTP verification codes or short-term session states
- **Pub/Sub**: Broadcast messages to other services (like push servers) for notifications or log analysis

**AI Integration**:
- REST requests from backend to AI microservice using Spring RestTemplate/Feign
- If AI service timeout (e.g., no response within 2 seconds), backend can still allow post creation success, leaving AI fields empty for later completion

### 4. AI Microservice (FastAPI)

**Role**: Machine learning model service running independently. Main functions: sentiment analysis, follow-up question generation, emotion classification and article summarization, empathy score calculation.

**Endpoints**:
- `POST /v1/ai/sentiment` → Input `{ text: "..." }`, return `{ sentiment_score, empathy_score }`
- `POST /v1/ai/questions` → Input `{ text: "..." }`, return `["Question1", "Question2", ...]`
- `POST /v1/ai/emotion` → Input `{ text: "..." }`, return `{ emotion: "sad" }`
- `POST /v1/ai/summarize` → Input `{ text: "..." }`, return `{ summary: "..." }`

**Runtime**:
- **Local**: `uvicorn app:app --reload --port 8001`
- **Production**: Docker containers with auto-scaling

**Model Management**:
- Using HuggingFace or self-trained models, loading different versions by scenario, supporting simple A/B testing

### 5. Data Storage & Caching

**PostgreSQL (Primary Database)**:
- **Users Table**: `{ id: UUID, phone_hash/email_hash, registered_at, ... }`
- **Posts Table**: `{ id: UUID, user_id, title, content, emotion_tag, ai_summary, empathy_score, resonance_count, flagged, created_at }`
- **Comments Table**: `{ id, post_id, user_id, content, created_at }`
- **Resonances Table**: `{ id, post_id, user_id, created_at }`
- **AI Data Table**: Store sentiment analysis results and question lists for future statistical analysis
- **Notifications Table**: `{ id, recipient_id, type, message, created_at, read }`
- **Drafts Table** (optional): User's unpublished local saves
- **Indexes & Full-Text Search**: GIN indexes on title/content for full-text search, indexes on high-frequency query fields (like `created_at`)

**Redis (Cache Layer)**:
- **Cache Post Lists**: `feed:{page}:{size}`, TTL 5 minutes
- **Trending Rankings**: Periodically or real-time update `resonance_count:{postId}`, can use ZSET (sorted sets)
- **User Sessions**: For temporary storage of OTP verification codes or short-term session states
- **Pub/Sub**: Broadcast messages to other services (like push servers) for notifications or log analysis

**Object Storage (S3/GCS)**:
- Store user-uploaded avatars, cover images, and potential future rich text files
- Backend only stores paths/URLs in PostgreSQL, actual large files stored in S3/R2/MinIO

### 6. Containerization & Deployment (DevOps)

**Local Development (Docker Compose)**:
- One command to start locally: PostgreSQL, Redis, Backend, AI service, Admin panel
- Developers can quickly debug and ensure environment consistency

**Testing/Staging (Kubernetes Staging)**:
- Package components as images: `youthmind-backend:latest`, `youthmind-ai:latest`, `youthmind-admin:latest`
- Use Deployment + Service + Ingress for automated deployment on test cluster
- Integrate CI/CD: Code push to main branch automatically triggers build, test, push images, Staging environment "auto" updates

**Production (Kubernetes/Managed Containers)**:
- Nearly identical to Staging configuration, but with production-grade resources and real domain names
- Set up auto-scaling (HPA), automatically add replicas when CPU or request volume exceeds thresholds
- TLS managed uniformly by Ingress Controller (like cert-manager + Let's Encrypt)

**CI/CD (GitHub Actions)**:
- **iOS**: macOS Runner automatically executes xcodebuild test and packaging
- **Backend**: Ubuntu Runner executes `mvn test` → build Jar → create Docker image
- **AI Service**: Ubuntu Runner executes `pytest` → create Docker image
- **Admin**: Ubuntu Runner executes `npm test` → `npm build` → create Docker image
- **Deployment Jobs**: After pushing images to container registry, trigger Kubernetes rolling updates

**Monitoring & Alerts**:
- Backend and AI services have built-in Prometheus metrics endpoints (`/actuator/prometheus`, `/metrics`)
- Grafana dashboards display: API latency, error rates, AI inference time, database connections, Redis cache hit ratio
- Alertmanager: When conditions like "Backend 5xx error rate > 1%" or "AI inference P95 > 500ms" are met, send urgent notifications to on-call team

### 7. End-to-End Data Flow (Key Steps)

**Anonymous User Opens App**:
```
User → GET /v1/posts/feed → Backend queries Redis cache → Returns post list to App → User sees feed
```

**User Registration and Login**:
```
App → POST /v1/auth/register + OTP → Backend verifies code, stores in database, generates JWT → App stores in Keychain
```

**User Creates Post**:
```
App "Create Post" interface fills title, content, selects emotion tags → POST /v1/posts/create (with JWT)

Backend:
1. Validate JWT, write to posts table (with user UUID, content, tags)
2. Synchronously call AI: /v1/ai/sentiment + /v1/ai/questions → return sentiment scores, follow-up question list
3. Write AI data table, update Redis cache corresponding pages
4. Return complete PostDto to App (with AI analysis results)
```

**User Resonance (Like)**:
```
App clicks "Resonance" → POST /v1/resonances/create (with JWT + postId)

Backend:
1. Write to resonance table, resonance_count++
2. Send notification to post author (write Notification table)
3. Update Redis trending scores (or directly clear cache pages)
4. Return latest resonance_count to App
```

**View Emotion Museum**:
```
App → GET /v1/posts/user/{userId} → Backend queries all user posts chronologically → Returns to App → Display in timeline/puzzle view with emotion tags and AI prompts
```

**Report and Admin Review**:
```
App clicks "Report" → POST /v1/posts/{postId}/flag → Backend updates post flagged=true → Write notification to admin
Admin dashboard pulls GET /v1/admin/posts?flagged=true → Display review queue → Select delete DELETE /v1/admin/posts/{postId} → Backend deletes from database, clears cache
```

**Push Notifications**:
```
New resonance or comment → Backend writes Notification table
If user online, App can pull notifications via polling or WebSocket/Push (APNs):
GET /v1/notifications?userId={} → Returns unread message list
App displays red dot, popup alerts, etc.
```

### Service Architecture Diagram
```
                                   🌐 INTERNET
                                       │
                              ┌────────┴────────┐
                              │  Load Balancer  │
                              │   (nginx/ALB)   │
                              └────────┬────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
                    ▼                  ▼                  ▼
         ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
         │   iOS App       │ │  Admin Panel    │ │  Web Interface  │
         │   (SwiftUI)     │ │    (React)      │ │   (Future)      │
         │                 │ │                 │ │                 │
         │ • Anonymous     │ │ • Admin Login   │ │ • Public View   │
         │ • Registration  │ │ • Moderation    │ │ • Statistics    │
         │ • Posts/Feed    │ │ • Statistics    │ │ • Reports       │
         │ • Resonance     │ │ • User Mgmt     │ │                 │
         │ • Comments      │ │ • Content Mgmt  │ │                 │
         │ • Notifications │ │ • System Health │ │                 │
         │ • Emotion Museum│ │                 │ │                 │
         └─────────┬───────┘ └─────────┬───────┘ └─────────┬───────┘
                   │                   │                   │
                   └───────────────────┼───────────────────┘
                                       │
                            HTTPS/REST API (JWT Auth)
                                       │
                              ┌────────▼────────┐
                              │  Backend API    │
                              │ (Spring Boot)   │
                              │                 │
                              │ ┌─────────────┐ │
                              │ │   Auth      │ │ ── JWT + OTP
                              │ │ Controller  │ │
                              │ └─────────────┘ │
                              │ ┌─────────────┐ │
                              │ │   Posts     │ │ ── CRUD + AI
                              │ │ Controller  │ │
                              │ └─────────────┘ │
                              │ ┌─────────────┐ │
                              │ │ Comments &  │ │ ── Interactions
                              │ │ Resonances  │ │
                              │ └─────────────┘ │
                              │ ┌─────────────┐ │
                              │ │Notifications│ │ ── Real-time
                              │ │ Controller  │ │
                              │ └─────────────┘ │
                              │ ┌─────────────┐ │
                              │ │   Admin     │ │ ── Moderation
                              │ │ Controller  │ │
                              │ └─────────────┘ │
                              └────────┬────────┘
                                       │
                         ┌─────────────┼─────────────┐
                         │             │             │
                         ▼             ▼             ▼
               ┌─────────────────┐ ┌──────────┐ ┌─────────────────┐
               │  AI Services    │ │PostgreSQL│ │  Redis Cache    │
               │   (FastAPI)     │ │ Database │ │    Cluster      │
               │                 │ │          │ │                 │
               │ ┌─────────────┐ │ │ Users    │ │ • Feed Cache    │
               │ │ Sentiment   │ │ │ Posts    │ │ • Session Store │
               │ │ Analysis    │ │ │ Comments │ │ • Hot Rankings  │
               │ └─────────────┘ │ │ Resonance│ │ • Rate Limiting │
               │ ┌─────────────┐ │ │ AI Data  │ │ • Pub/Sub       │
               │ │ Question    │ │ │ Notifs   │ │ • Temp Storage  │
               │ │Generation   │ │ │ Drafts   │ │                 │
               │ └─────────────┘ │ │ Sessions │ │                 │
               │ ┌─────────────┐ │ │          │ │ TTL Policies:   │
               │ │ Emotion     │ │ │Indexes:  │ │ • feed:5min     │
               │ │ Detection   │ │ │• GIN FTS │ │ • users:24h     │
               │ └─────────────┘ │ │• created │ │ • hot:1h        │
               │ ┌─────────────┐ │ │• user_id │ │                 │
               │ │ Content     │ │ │• flagged │ │                 │
               │ │Moderation   │ │ │          │ │                 │
               │ └─────────────┘ │ │          │ │                 │
               └─────────────────┘ └──────────┘ └─────────────────┘
                         │                              │
                         │                              │
                    ┌────▼──────┐                ┌─────▼─────┐
                    │Model Cache│                │ Message   │
                    │& Artifacts│                │   Queue   │
                    │           │                │(Future)   │
                    │• HF Models│                │• Push     │
                    │• Vectors  │                │• Email    │
                    │• Pipelines│                │• SMS      │
                    └───────────┘                └───────────┘

                              ┌─────────────────┐
                              │  Object Storage │
                              │     (S3/GCS)    │
                              │                 │
                              │ • User Avatars  │
                              │ • Cover Images  │
                              │ • Backups       │
                              │ • Static Assets │
                              │ • Log Archives  │
                              └─────────────────┘

       ┌──────────────────────── MONITORING & OBSERVABILITY ────────────────────────┐
       │                                                                             │
       │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐   │
       │  │ Prometheus  │  │  Grafana    │  │     ELK     │  │   Alert         │   │
       │  │   Metrics   │  │ Dashboards  │  │    Stack    │  │   Manager       │   │
       │  │             │  │             │  │             │  │                 │   │
       │  │• API Latency│  │• System     │  │• App Logs   │  │• Error Rate >1% │   │
       │  │• Error Rate │  │  Health     │  │• Access Log │  │• Latency >500ms │   │
       │  │• Cache Hit  │  │• User       │  │• Error Log  │  │• DB Conn >80%   │   │
       │  │• DB Perf    │  │  Analytics  │  │• Audit Log  │  │• Cache Miss >50%│   │
       │  │• AI Metrics │  │• AI Model   │  │             │  │                 │   │
       │  │             │  │  Performance│  │             │  │                 │   │
       │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────┘   │
       └──────────────────────────────────────────────────────────────────────────┘

Data Flow Examples:
┌─ Anonymous Browse ─┐    ┌─ User Post ─┐         ┌─ Admin Moderation ─┐
│1. GET /posts/feed  │    │1. POST      │         │1. GET flagged      │
│2. Redis check     │    │2. JWT verify│         │2. Review content   │
│3. DB if cache miss│    │3. AI process│         │3. DELETE post      │
│4. Cache result    │    │4. Store DB  │         │4. Clear cache      │
│5. Return to app   │    │5. Update    │         │5. Audit log       │
└───────────────────┘    └─────────────┘         └───────────────────┘
```

### 8. Operations & Security Focus

**Privacy Protection**:
- User real identity only used for SMS/email verification, stored as hash values, posts always recorded with random anonymous IDs
- All transmission via HTTPS, encrypted JWT tokens, database stores no plaintext email/phone
- GDPR compliance with data deletion endpoints

**Performance & Scaling**:
- High concurrent reads (browsing) use Redis cache, writes update database and cache in parallel
- Backend and AI services horizontally scalable, deployed on Kubernetes clusters with automatic load balancing
- Continuous monitoring: Prometheus captures key metrics (latency, error rates, resource usage), Grafana real-time visualization
- Pre-set alerts (error rates, Redis cache hit rate anomalies), timely operations notifications

**Backup & Recovery**:
- PostgreSQL daily full backup + incremental WAL async upload to object storage (S3)
- Redis uses master-slave replication or managed clusters to ensure hot data persistence
- Regular recovery drills ensure service restoration within 30 minutes in case of disasters

## 🚀 Quick Start

### Prerequisites
- Xcode 15+ (for iOS development)
- Java 17+ and Maven (for backend)
- Python 3.9+ (for AI services)
- Node.js 18+ (for admin dashboard)
- Docker and Docker Compose
- PostgreSQL 14+ and Redis 7+

### Environment Variables Setup
All services use standardized `YOUTHMIND_` prefix:
```bash
# Database Configuration
export YOUTHMIND_DB_HOST=localhost
export YOUTHMIND_DB_PORT=5432
export YOUTHMIND_DB_NAME=youthmind
export YOUTHMIND_DB_USER=your_db_user
export YOUTHMIND_DB_PASSWORD=your_db_password

# Redis Configuration
export YOUTHMIND_REDIS_HOST=localhost
export YOUTHMIND_REDIS_PORT=6379

# Service URLs
export YOUTHMIND_AI_SERVICE_URL=http://localhost:8001
export YOUTHMIND_BACKEND_URL=http://localhost:8080

# Security
export YOUTHMIND_JWT_SECRET=your_256_bit_secret
export YOUTHMIND_TWILIO_API_KEY=your_twilio_key
```

### Local Development Setup

1. **Clone Repository**
   ```bash
   git clone https://github.com/Kevin0304-li/YouthMind.git
   cd YouthMind
   ```

2. **Start Infrastructure Services**
   ```bash
   docker-compose up -d postgres redis
   ```

3. **Backend Setup (Port 8080)**
   ```bash
   cd backend
   mvn clean package
   java -jar target/youthmind-backend.jar
   # Swagger UI: http://localhost:8080/swagger-ui.html
   ```

4. **AI Services Setup (Port 8001)**
   ```bash
   cd ai-services
   pip install -r requirements.txt
   uvicorn app:app --reload --port 8001
   ```

5. **Admin Dashboard Setup (Port 3000)**
   ```bash
   cd admin
   npm install
   npm start
   ```

6. **iOS App Setup**
   ```bash
   cd mobile
   # Open YouthMindApp.xcodeproj in Xcode
   # Update baseURL to http://localhost:8080
   # Build and run on simulator
   ```

## 👥 Team Structure & Coordination

### Development Teams
- **iOS Team**: Dawei, Dexter, Youssef (SwiftUI, Core Data, UI/UX)
- **Backend Team**: Mustafo, Ahmed, Harley, Junayd (Spring Boot, PostgreSQL, Redis)
- **AI Team**: Kevin (AI Lead), Andy (AI Developer - UI/UX specialist)
- **Admin Dashboard**: Alex (React, TypeScript, Content Moderation)
- **Project Manager**: Junayd (Coordination, Notion workspace management)

### Coordination Framework
- **📅 Daily Development Standups**: Development teams coordinate progress and blockers
- **🤖 AI Coordination**: Kevin, Andy, Junayd sync every 2-3 days for AI integration
- **📊 Task Management**: [YouthMind Notion Workspace](https://www.notion.so/2078796c838d80d39393cb8cfd5f963c?v=2078796c838d806ba152000c2de604d9)

**📚 For detailed team roles and responsibilities**: See [Team Structure Documentation](./docs/TEAM_STRUCTURE.md)  
**📋 For meeting protocols and communication flow**: See [Team Coordination Guide](./docs/TEAM_COORDINATION.md)

## 📚 Documentation

### Core Documentation
- **[📋 Team Coordination](./docs/TEAM_COORDINATION.md)** - Meeting structure and communication protocols
- **[🏗️ Technical Architecture](./docs/TECHNICAL_ARCHITECTURE.md)** - System design and production operations
- **[👥 Team Structure](./docs/TEAM_STRUCTURE.md)** - Detailed role assignments and responsibilities
- **[📡 API Reference](./docs/API_REFERENCE.md)** - Complete API endpoints and schemas
- **[🚀 Deployment Guide](./docs/DEPLOYMENT.md)** - Production deployment and infrastructure

### Key Production Standards
- **Environment Variables**: All services use standardized `YOUTHMIND_` prefix
- **API Versioning**: `/v1/` prefixed endpoints with deprecation policy
- **Rate Limiting**: Anonymous (100 req/5min) and authenticated (10 posts/hour) limits
- **Monitoring**: Prometheus metrics, Grafana dashboards, ELK logging
- **Security**: GDPR compliance, audit logging, automated backup strategy

## 🔐 Security & Privacy

### Data Protection
- **Anonymous Identity**: SHA256 hashed email/phone storage only
- **No PII Recovery**: Impossible to retrieve personal information from database
- **GDPR Compliance**: Right to erasure endpoint with data retention policies
- **Content Moderation**: AI-powered flagging with human review workflow

### Authentication Flow
- **Anonymous Browsing**: Open access to view posts without registration
- **Registration Required**: Email/phone + OTP verification for interactions
- **JWT Security**: Short-lived tokens with role-based access control
- **Rate Limiting**: Abuse prevention with Redis-backed sliding windows

## 🚀 API Overview

### Core Endpoints (v1)
```
# Public Access
GET  /v1/posts/feed                 # Anonymous feed browsing

# Authentication  
POST /v1/auth/register             # Email/phone registration with OTP

# User Actions (JWT Required)
POST /v1/posts/create              # Create post with AI analysis
POST /v1/comments/create           # Add comment to post  
POST /v1/resonances/create         # Express understanding/empathy
GET  /v1/posts/user/{userId}       # Personal emotion museum

# Admin Actions
GET  /v1/admin/posts?flagged=true  # Moderation queue
DELETE /v1/admin/posts/{postId}    # Remove violating content
```

**📡 For complete API documentation with schemas**: See [API Reference](./docs/API_REFERENCE.md)

## 🤝 Contributing

We welcome contributions from developers passionate about mental health technology. Our development follows strict standards for safety, privacy, and quality.

### Key Principles
- **Mental Health Safety**: All AI outputs reviewed for appropriateness and safety
- **Privacy by Design**: No personal data collection beyond authentication requirements
- **Quality Assurance**: Comprehensive testing and code review requirements
- **Documentation**: Living documentation updated with each feature

### Getting Started
1. **Review Documentation**: Start with [Team Coordination](./docs/TEAM_COORDINATION.md) and [Technical Architecture](./docs/TECHNICAL_ARCHITECTURE.md)
2. **Set up Local Environment**: Follow the Quick Start guide above
3. **Join Team Workspace**: Access the [Notion workspace](https://www.notion.so/2078796c838d80d39393cb8cfd5f963c?v=2078796c838d806ba152000c2de604d9) for task assignments
4. **Contact Project Manager**: Reach out to [Junayd](mailto:mohammad.junayd@proton.me) for team integration

**📋 For detailed development workflows and standards**: See [Team Coordination Guide](./docs/TEAM_COORDINATION.md)

## 📊 Analytics & Monitoring

### Key Performance Indicators
- **Feed API Performance**: P95 latency < 200ms
- **AI Processing**: Sentiment analysis < 500ms  
- **User Engagement**: Post creation and resonance rates
- **Mental Health Impact**: Emotional wellness trend analysis

### Production Infrastructure Status
- ✅ Standardized environment variables with `YOUTHMIND_` prefix
- ✅ Kubernetes secrets for sensitive data management
- ✅ Prometheus metrics with Grafana dashboards
- ✅ ELK stack for centralized logging and monitoring
- ✅ Automated database backup and disaster recovery

**🚀 For complete deployment procedures**: See [Deployment Guide](./docs/DEPLOYMENT.md)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.

## 🆘 Support & Contact

### Project Communication
- **Technical Issues**: Create an issue in this repository with detailed information
- **Project Manager**: [mohammad.junayd@proton.me](mailto:mohammad.junayd@proton.me)
- **Team Coordination**: [Schedule a meeting](mailto:mohammad.junayd@proton.me?subject=Team%20Meeting%20Request)

### Emergency Response
- **Production Issues**: Contact on-call rotation (< 15 minutes response)
- **Security Concerns**: Immediate escalation to project manager
- **Mental Health Content**: Follow content moderation and crisis intervention protocols

**📋 For detailed contact protocols**: See [Team Coordination Guide](./docs/TEAM_COORDINATION.md)

---

**Built with ❤️ by a dedicated team focused on youth mental health innovation through privacy-first technology**

*Last Updated: 2024 - This README reflects the current state of documentation across the entire YouthMind repository*

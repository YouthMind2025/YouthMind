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

### Service Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   iOS App       │───▶│  Backend API    │───▶│  AI Services    │
│   (SwiftUI)     │    │  (Spring Boot)  │    │  (FastAPI)      │
│ Anonymous +     │    │ JWT Auth +      │    │ Sentiment +     │
│ Registration    │    │ Rate Limiting   │    │ Questions       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       ▼                       │
         │              ┌─────────────────┐              │
         │              │   PostgreSQL    │              │
         │              │ Users + Posts + │              │
         │              │   AI Metadata   │              │
         │              └─────────────────┘              │
         │                       │                       │
         │                       ▼                       │
    ┌────▼──────┐       ┌─────────────────┐              │
    │ React     │       │  Redis Cache    │◀─────────────┘
    │ Admin     │       │ Feed + Search + │
    │ Dashboard │       │  Counters       │
    └───────────┘       └─────────────────┘
```

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

### Meeting Structure
- **📅 Daily Development Standups**: All teams except AI (if there are updates)
- **🤖 AI Coordination**: Kevin, Andy, Junayd every 2-3 days
- **📊 Task Management**: [YouthMind Notion Workspace](https://www.notion.so/2078796c838d80d39393cb8cfd5f963c?v=2078796c838d806ba152000c2de604d9)
- **📧 Meeting Coordination**: [mohammad.junayd@proton.me](mailto:mohammad.junayd@proton.me?subject=Team%20Meeting%20Request)

### Team Responsibilities
- **Kevin**: AI models, strategic solutions across all teams (advisory role)
- **Andy**: AI development, UI/UX guidance, app function questions
- **Others**: Direct implementation of their respective components

## 📚 Documentation

### Core Documentation
- **[📋 Team Coordination](./docs/TEAM_COORDINATION.md)** - Meeting structure and communication protocols
- **[🏗️ Technical Architecture](./docs/TECHNICAL_ARCHITECTURE.md)** - System design and production operations
- **[👥 Team Structure](./docs/TEAM_STRUCTURE.md)** - Detailed role assignments and responsibilities
- **[📡 API Reference](./docs/API_REFERENCE.md)** - Complete API endpoints and schemas
- **[🚀 Deployment Guide](./docs/DEPLOYMENT.md)** - Production deployment and infrastructure

### Production Operations
- **Environment & Secrets Management**: Kubernetes secrets and GitHub Actions
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

### Public Endpoints (v1)
```
GET  /v1/posts/feed                 # Anonymous feed browsing
POST /v1/auth/register             # Email/phone registration with OTP
POST /v1/posts/create              # Create post (authenticated)
GET  /v1/posts/user/{userId}       # Emotion museum (authenticated)
POST /v1/comments/create           # Add comment (authenticated)
POST /v1/resonances/create         # Add resonance (authenticated)
```

### Error Response Format
```json
{
  "error": {
    "code": "INVALID_AUTH",
    "message": "JWT token is missing or invalid.",
    "details": { "field": "authorizationHeader" }
  }
}
```

## 🤝 Contributing

We welcome contributions from developers passionate about mental health technology. Please read our [Contributing Guidelines](./CONTRIBUTING.md) for development standards and safety considerations.

### Development Standards
- **Mental Health Safety**: All AI outputs reviewed for appropriateness
- **Privacy by Design**: No personal data collection beyond authentication
- **Testing Requirements**: Unit, integration, and E2E test coverage
- **Code Quality**: ESLint, SwiftLint, and SonarQube compliance
- **Documentation**: Living documentation updated with each feature

### Getting Started
1. Review the [Team Coordination](./docs/TEAM_COORDINATION.md) document
2. Set up your local development environment
3. Join the [Notion workspace](https://www.notion.so/2078796c838d80d39393cb8cfd5f963c?v=2078796c838d806ba152000c2de604d9) for task assignments
4. Contact [Junayd](mailto:mohammad.junayd@proton.me) for team integration

## 📊 Analytics & Monitoring

### Key Performance Indicators
- **Feed API Performance**: P95 latency < 200ms
- **AI Processing**: Sentiment analysis < 500ms
- **User Engagement**: Post creation and resonance rates
- **Mental Health Impact**: Emotional wellness trend analysis

### Production Checklist
- ✅ Environment variables standardized with `YOUTHMIND_` prefix
- ✅ Kubernetes secrets configured for sensitive data
- ✅ Prometheus metrics and Grafana dashboards active
- ✅ ELK stack for centralized logging and monitoring
- ✅ Database backup and disaster recovery tested

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.

## 🆘 Support & Contact

### Technical Support
- **Issues**: Create an issue in this repository with detailed information
- **Project Manager**: [mohammad.junayd@proton.me](mailto:mohammad.junayd@proton.me)
- **Documentation**: Navigate between docs using cross-references in each file

### Emergency Protocols
- **Production Issues**: Contact on-call rotation (< 15 minutes response)
- **Security Concerns**: Immediate escalation to project manager
- **Mental Health Content**: Follow content moderation and crisis intervention protocols

---

**Built with ❤️ by a dedicated team focused on youth mental health innovation through privacy-first technology**

*Last Updated: 2024 - This README reflects the current state of documentation across the entire YouthMind repository*

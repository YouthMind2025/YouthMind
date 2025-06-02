# YouthMind (SaySomething App)

**YouthMind** is a next-generation mobile application designed to revolutionize youth mental health support through cutting-edge AI technology, anonymous community engagement, and personalized emotional intelligence.

## 🎯 Project Overview

Our platform creates a safe space where young people can express themselves anonymously, receive AI-powered emotional support, and connect with peers facing similar challenges. The app combines advanced sentiment analysis, personalized content generation, and community-driven understanding to provide comprehensive mental health assistance.

### Core Mission
- Provide accessible mental health support for youth
- Foster anonymous, judgment-free community discussions
- Leverage AI to deliver personalized emotional guidance
- Create measurable impact through data-driven insights

## 🌟 Key Features

### Anonymous Expression
- **Secure Identity System**: UUID-based anonymous posting
- **Safe Space Creation**: No personal data collection
- **Community Voting**: Understanding-based post ranking

### AI-Powered Support
- **Personalized Questions**: Context-aware conversation starters
- **Emotional Analysis**: Real-time sentiment and emotion detection
- **Smart Responses**: Tailored feedback and suggestions
- **Crisis Detection**: Automated support escalation when needed

### Community Engagement
- **Feed Algorithm**: Smart content curation based on relevance
- **Peer Understanding**: Community-driven empathy scoring
- **Discussion Threads**: Meaningful conversation facilitation

### Wellness Tracking
- **Emotion Museum**: Personal emotional journey visualization
- **Progress Insights**: Long-term mental health trend analysis
- **Achievement System**: Positive reinforcement mechanisms

## 🏗️ System Architecture

### Technology Stack
- **Mobile Frontend**: SwiftUI (iOS), Clean Architecture
- **Backend Services**: Java Spring Boot (RESTful APIs)
- **AI/ML Pipeline**: Python FastAPI, HuggingFace Transformers
- **Data Storage**: PostgreSQL (primary), Redis (caching)
- **Infrastructure**: Docker, Nginx, AWS/Cloud deployment

### Service Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   iOS App       │───▶│  Backend API    │───▶│  AI Services    │
│   (SwiftUI)     │    │  (Spring Boot)  │    │  (FastAPI)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       ▼                       │
         │              ┌─────────────────┐              │
         │              │   PostgreSQL    │              │
         │              │   Database      │              │
         │              └─────────────────┘              │
         │                       │                       │
         │                       ▼                       │
         │              ┌─────────────────┐              │
         └──────────────│  Redis Cache    │◀─────────────┘
                        │  (Hot Data)     │
                        └─────────────────┘
```

## 🚀 Quick Start

### Prerequisites
- Xcode 15+ (for iOS development)
- Java 17+ and Maven (for backend)
- Python 3.9+ (for AI services)
- Docker and Docker Compose
- PostgreSQL and Redis

### Local Development Setup

1. **Clone Repository**
   ```bash
   git clone https://github.com/Kevin0304-li/YouthMind.git
   cd YouthMind
   ```

2. **Backend Setup**
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```

3. **AI Services Setup**
   ```bash
   cd ai-services
   pip install -r requirements.txt
   uvicorn main:app --reload
   ```

4. **iOS App Setup**
   ```bash
   cd ios-app
   # Open YouthMind.xcodeproj in Xcode
   # Build and run on simulator
   ```

5. **Database Setup**
   ```bash
   docker-compose up -d postgres redis
   ```

## 📚 Documentation

- [**Development Workflow**](./docs/DEVELOPMENT_WORKFLOW.md) - Detailed 6-week development plan
- [**API Documentation**](./docs/API_REFERENCE.md) - Complete API endpoints and schemas
- [**Team Structure**](./docs/TEAM_STRUCTURE.md) - Roles and responsibilities
- [**Deployment Guide**](./docs/DEPLOYMENT.md) - Production deployment instructions

## 🤝 Contributing

We welcome contributions from developers passionate about mental health technology. Please read our [Contributing Guidelines](./CONTRIBUTING.md) before submitting pull requests.

### Development Standards
- Follow established coding conventions for each platform
- Write comprehensive unit and integration tests
- Ensure all AI outputs are safe and appropriate
- Maintain data privacy and security standards

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.

## 🆘 Support

For technical support or questions:
- Create an issue in this repository
- Email: support@youthmind.dev
- Documentation: [docs.youthmind.dev](https://docs.youthmind.dev)

---

**Built with ❤️ by a team dedicated to youth mental health innovation**

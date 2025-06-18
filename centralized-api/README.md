# YouthMind Centralized API

**YouthMind** is a next-generation mobile application designed to revolutionize youth mental health support through cutting-edge AI technology, anonymous community engagement, and personalized emotional intelligence.

## 🎯 Project Overview

Our platform creates a safe space where young people can express themselves anonymously, receive AI-powered emotional support, and connect with peers facing similar challenges. The app combines advanced sentiment analysis, personalized content generation, and community-driven understanding to provide comprehensive mental health assistance.

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

## 📱 iOS App Features

### Authentication & Security
- **LoginService**: Handles email/OTP registration with `/v1/auth/register` endpoint
- **TokenManager**: Secure JWT storage using iOS Keychain
- **NetworkService**: Robust API communication with error handling
- **LoginViewModel**: Reactive login state management with validation

### UI/UX Improvements
- **Modern Login Interface**: Clean, accessible design with validation feedback
- **Real-time Validation**: Email format and OTP length validation
- **Error Handling**: User-friendly error messages and loading states
- **Success Feedback**: Visual confirmation of successful login
- **Responsive Design**: Adaptive layout for different screen sizes

### Testing
- **Unit Tests**: Comprehensive test coverage for LoginService and LoginViewModel
- **Mock Services**: Isolated testing with mock network services
- **Error Scenarios**: Testing for network errors, invalid responses, and token expiration

## 🚀 Getting Started

### Prerequisites
- Xcode 15.0+
- iOS 17.0+
- Swift 5.5+

### Installation
1. Clone the repository
2. Open `Saysomething.xcodeproj` in Xcode
3. Build and run the project

### Testing
Run the test suite with `Cmd+U` in Xcode or:
```bash
xcodebuild test -scheme Saysomething -destination 'platform=iOS Simulator,name=iPhone 15'
```

## 📁 Project Structure

```
centralized-api/
├── Saysomething/
│   ├── Saysomething/
│   │   ├── Services/
│   │   │   ├── NetworkService.swift      # API communication
│   │   │   ├── LoginService.swift        # Authentication
│   │   │   └── TokenManager.swift        # Secure token storage
│   │   ├── ViewModels/
│   │   │   └── HomeViewModels.swift      # Login & Home view models
│   │   ├── Views/
│   │   │   ├── LoginView.swift           # Login interface
│   │   │   ├── HomeView.swift            # Main app interface
│   │   │   └── ...                       # Other views
│   │   ├── SaysomethingApp.swift         # App entry point
│   │   └── ContentView.swift             # Default content view
│   └── SaysomethingTests/
│       ├── LoginServiceTests.swift       # Login service tests
│       ├── NetworkServiceTests.swift     # Network service tests
│       └── SaysomethingTests.swift       # General tests
├── backend/                              # Spring Boot backend
├── docs/                                 # Documentation
└── README.md                             # This file
```

## 🔧 Configuration

### API Endpoints
- **Base URL**: `https://api.example.com` (configurable in NetworkService)
- **Registration**: `POST /v1/auth/register`
- **Authentication**: JWT Bearer token in Authorization header

### Environment Variables
- `JWT_SECRET`: Secret key for JWT signing
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `REDIS_HOST`: Redis server host
- `REDIS_PORT`: Redis server port

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

- **Technical Issues**: Create an issue in this repository with detailed information
- **Feature Requests**: Use the GitHub Issues feature
- **Documentation**: Check the `/docs` folder for detailed technical documentation

---

*Last Updated: 2024 - This README reflects the current state of the centralized-api implementation with all UI improvements, login flow, and comprehensive testing.* 
# API Usage Guide

## Overview

This project implements complete API client functionality with JWT authentication, error handling, and state management.

## Main Features

### 1. Authentication API

#### Registration Endpoint
- **URL**: `/v1/auth/register`
- **Method**: POST
- **Request Body**:
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```
- **Response**:
```json
{
  "data": {
    "userId": "user_id",
    "token": "jwt_token",
    "expiresAt": "2024-01-01T00:00:00Z",
    "message": "Registration successful"
  }
}
```

#### Login Endpoint
- **URL**: `/v1/auth/login`
- **Method**: POST
- **Request Body**:
```json
{
  "email": "user@example.com",
  "password": "password"
}
```

### 2. Posts API

#### Create Post
- **URL**: `/v1/posts/create`
- **Method**: POST
- **JWT Required**: Yes
- **Request Body**:
```json
{
  "title": "Post Title",
  "content": "Post Content"
}
```

#### Get Posts List
- **URL**: `/v1/posts`
- **Method**: GET
- **JWT Required**: Yes

## JWT Injection Mechanism

### Automatic JWT Injection
The system automatically injects JWT tokens into requests that require authentication:

```swift
// Requests requiring JWT automatically add Authorization header
if let token = TokenManager.shared.getToken() {
    headers["Authorization"] = "Bearer \(token)"
}
```

### JWTInjectable Protocol
Use protocol to control which requests need JWT:

```swift
protocol JWTInjectable {
    var shouldInjectJWT: Bool { get }
}

// Registration request - no JWT needed
extension RegisterRequest: JWTInjectable {
    var shouldInjectJWT: Bool { false }
}

// Create post request - JWT required
extension CreatePostRequest: JWTInjectable {
    var shouldInjectJWT: Bool { true }
}
```

## Error Handling

### API Error Types
```swift
enum APIError: Error, LocalizedError {
    case invalidURL
    case networkError(Error)
    case invalidResponse
    case invalidStatusCode(Int)
    case decodingError(Error)
    case timeout
    case unauthorized
    case forbidden
    case notFound
    case serverError
}
```

### Automatic Token Error Handling
When receiving 401 unauthorized errors, the system automatically:
1. Clears locally stored tokens
2. Throws unauthorized error
3. Displays appropriate error messages in UI

## UI State Management

### Success State
- Display green success messages
- Auto-clear after 3 seconds
- Auto-reset forms

### Error State
- Display red error messages
- Show specific error descriptions
- Support English error messages

### Loading State
- Display loading indicators
- Disable submit buttons
- Prevent duplicate submissions

## Usage Examples

### 1. Register User
```swift
let viewModel = LoginViewModel()
viewModel.email = "user@example.com"
viewModel.otp = "123456"
viewModel.register()
```

### 2. Create Post
```swift
let viewModel = HomeViewModel()
viewModel.createPost(title: "My Post", content: "Post content")
```

### 3. Get Posts List
```swift
let viewModel = HomeViewModel()
viewModel.fetchPosts()
```

## Token Management

### Token Storage
- Secure storage using Keychain
- Automatic token validity verification
- Support for token expiration checking

### Token Operations
```swift
// Save token
TokenManager.shared.saveToken("jwt_token")

// Get token
let token = TokenManager.shared.getToken()

// Check token validity
let isValid = TokenManager.shared.isTokenValid()

// Clear token
TokenManager.shared.clearToken()
```

## Testing Features

The app provides a complete API testing interface including:
- Token status checking
- Registration/login testing
- Create post testing
- Get posts list testing
- Real-time status display

## Notes

1. **Network Configuration**: Ensure backend server runs on `http://localhost:8080`
2. **CORS Settings**: Backend needs appropriate CORS policy configuration
3. **Token Security**: JWT tokens stored in Keychain for security
4. **Error Handling**: All API calls have complete error handling mechanisms
5. **State Management**: Uses Combine for reactive state management

## Development Debugging

### View Token Status
Real-time token status and validity can be viewed in the API test interface.

### Error Debugging
All errors are displayed in UI with detailed error descriptions.

### Network Debugging
Use Xcode's network debugging tools to view API requests and responses. 
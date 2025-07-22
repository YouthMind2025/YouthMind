# Firebase Crashlytics Setup Guide

This guide will help you set up Firebase Crashlytics for real-time crash tracking in your iOS app.

## Prerequisites

- Xcode 14.0 or later
- iOS 15.0 or later
- Firebase account

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project" or "Add project"
3. Enter your project name (e.g., "Saysomething")
4. Choose whether to enable Google Analytics (recommended)
5. Click "Create project"

## Step 2: Add iOS App to Firebase

1. In your Firebase project console, click the iOS icon (+ Add app)
2. Enter your iOS bundle ID (e.g., `com.yourcompany.Saysomething`)
3. Enter app nickname (optional)
4. Click "Register app"
5. Download the `GoogleService-Info.plist` file

## Step 3: Configure Your Xcode Project

### 3.1 Add Firebase SDK via Swift Package Manager

1. In Xcode, go to File → Add Package Dependencies
2. Enter the Firebase iOS SDK URL: `https://github.com/firebase/firebase-ios-sdk.git`
3. Select the following packages:
   - FirebaseCrashlytics
   - FirebaseAnalytics
4. Click "Add Package"

### 3.2 Add GoogleService-Info.plist

1. Drag the downloaded `GoogleService-Info.plist` into your Xcode project
2. Make sure "Copy items if needed" is checked
3. Add to your main app target

### 3.3 Update Bundle Identifier

1. In Xcode, select your project
2. Select your app target
3. In the "General" tab, update the Bundle Identifier to match what you registered in Firebase

## Step 4: Configure Build Settings

### 4.1 Add Run Script Phase

1. In Xcode, select your app target
2. Go to "Build Phases" tab
3. Click "+" → "New Run Script Phase"
4. Add this script:

```bash
"${PODS_ROOT}/FirebaseCrashlytics/run"
```

**Note:** If using Swift Package Manager instead of CocoaPods, use this script:

```bash
"${BUILD_DIR%/Build/*}/SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/run"
```

### 4.2 Configure Debug Information Format

1. In your app target's build settings, search for "Debug Information Format"
2. Set it to "DWARF with dSYM File" for both Debug and Release configurations

## Step 5: Test the Integration

### 5.1 Build and Run

1. Build and run your app on a device or simulator
2. Check the console for Firebase initialization messages

### 5.2 Test Crash Reporting

1. Use the `CrashTestView` in your app to test various crash scenarios
2. Or add this code to trigger a test crash:

```swift
import FirebaseCrashlytics

// Test crash
Crashlytics.crashlytics().crash()
```

### 5.3 Verify in Firebase Console

1. Go to your Firebase Console
2. Navigate to Crashlytics section
3. You should see crash reports within a few minutes

## Step 6: Add Crashlytics Logging to Key Flows

The following files have been updated with Crashlytics logging:

### NetworkService.swift
- Logs all API requests
- Records network errors with context
- Tracks retry attempts

### FirebaseService.swift
- Centralized Firebase configuration
- Helper methods for logging events and errors
- User identification and custom properties

### CrashTestView.swift
- Test interface for crash scenarios
- Demonstrates various Crashlytics features

## Step 7: Customize Logging

### Add Custom Logs

```swift
import FirebaseCrashlytics

// Log custom events
Crashlytics.crashlytics().log("User completed onboarding")

// Log errors with context
Crashlytics.crashlytics().record(error: error, userInfo: [
    "screen": "LoginView",
    "action": "login_attempt"
])

// Set user properties
Crashlytics.crashlytics().setCustomValue("premium", forKey: "user_type")

// Set user ID
Crashlytics.crashlytics().setUserID("user123")
```

### Add to Key User Flows

Consider adding logging to:
- User authentication (login/logout)
- Critical app features
- Error-prone areas
- Performance bottlenecks

## Troubleshooting

### Common Issues

1. **No crashes appearing in console:**
   - Ensure you're testing on a device (not simulator)
   - Check that the run script phase is added correctly
   - Verify `GoogleService-Info.plist` is included in the app bundle

2. **Build errors:**
   - Clean build folder (Cmd+Shift+K)
   - Delete derived data
   - Reinstall Firebase packages

3. **Symbolication issues:**
   - Ensure dSYM files are uploaded
   - Check that the correct bundle identifier is used

### Debug Mode

For development, you can enable debug mode:

```swift
#if DEBUG
Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
#endif
```

## Best Practices

1. **Don't log sensitive information** (passwords, tokens, etc.)
2. **Use meaningful log messages** that help with debugging
3. **Add context to errors** (screen, user action, etc.)
4. **Test crash reporting** in both debug and release builds
5. **Monitor crash reports** regularly in the Firebase console

## Next Steps

1. Set up crash alert notifications in Firebase Console
2. Configure crash report filtering and grouping
3. Set up custom dashboards for crash analytics
4. Integrate with your CI/CD pipeline for automated symbol uploads

## Support

- [Firebase Crashlytics Documentation](https://firebase.google.com/docs/crashlytics)
- [Firebase iOS SDK GitHub](https://github.com/firebase/firebase-ios-sdk)
- [Firebase Support](https://firebase.google.com/support)
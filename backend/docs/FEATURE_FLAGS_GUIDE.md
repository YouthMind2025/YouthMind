# YouthMind Feature Flag Management Guide

## Overview

This guide provides comprehensive instructions for managing feature flags in YouthMind Psychological Analysis Services. Feature flags enable gradual rollout of new features, A/B testing, and safe deployment practices.

## Table of Contents

1. [Introduction](#introduction)
2. [Feature Flag Concepts](#feature-flag-concepts)
3. [Getting Started](#getting-started)
4. [Feature Flag Management](#feature-flag-management)
5. [Gradual Rollout](#gradual-rollout)
6. [A/B Testing](#ab-testing)
7. [Analytics and Monitoring](#analytics-and-monitoring)
8. [Integration with Psychological Analysis](#integration-with-psychological-analysis)
9. [Best Practices](#best-practices)
10. [Troubleshooting](#troubleshooting)
11. [API Reference](#api-reference)

## Introduction

Feature flags (also known as feature toggles) are a powerful technique that allows you to:
- **Gradually roll out new features** to a subset of users
- **A/B test different versions** of features
- **Quickly disable features** if issues arise
- **Enable features for specific user groups** (beta users, premium users, etc.)
- **Reduce deployment risk** by decoupling feature release from code deployment

## Feature Flag Concepts

### Core Concepts

- **Feature Flag**: A boolean or configuration value that controls whether a feature is enabled
- **Targeting Rules**: Conditions that determine which users see a feature
- **Gradual Rollout**: Slowly increasing the percentage of users who see a feature
- **A/B Testing**: Comparing two versions of a feature to determine which performs better
- **Analytics**: Tracking how users interact with features

### Feature Flag Types

1. **Release Flags**: Control the release of new features
2. **Experiment Flags**: Enable A/B testing
3. **Permission Flags**: Control access to premium features
4. **Operational Flags**: Control system behavior (circuit breakers, etc.)

## Getting Started

### Prerequisites

- YouthMind backend running
- Redis server available
- Access to feature flag management endpoints

### Initial Setup

1. **Initialize Feature Flag Manager**
   ```bash
   curl -X POST http://localhost:8080/v1/feature-flags/initialize
   ```

2. **Check Available Feature Flags**
   ```bash
   curl http://localhost:8080/v1/feature-flags
   ```

3. **Verify Health Status**
   ```bash
   curl http://localhost:8080/v1/feature-flags/health
   ```

## Feature Flag Management

### Creating Feature Flags

**Create a new feature flag:**
```bash
curl -X POST http://localhost:8080/v1/feature-flags/my_new_feature \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my_new_feature",
    "description": "Enhanced psychological analysis algorithm",
    "enabled": false,
    "targetingRules": [
      {
        "type": "PERCENTAGE",
        "value": 0.0
      }
    ],
    "gradualRolloutEnabled": true,
    "rolloutPercentage": 0.0,
    "abTestEnabled": false
  }'
```

### Updating Feature Flag Configuration

**Update feature flag settings:**
```bash
curl -X PUT http://localhost:8080/v1/feature-flags/my_new_feature/config \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my_new_feature",
    "description": "Enhanced psychological analysis algorithm",
    "enabled": true,
    "targetingRules": [
      {
        "type": "PERCENTAGE",
        "value": 25.0
      }
    ],
    "gradualRolloutEnabled": true,
    "rolloutPercentage": 25.0,
    "abTestEnabled": false
  }'
```

### Toggling Feature Flags

**Enable/disable a feature flag:**
```bash
# Enable
curl -X POST http://localhost:8080/v1/feature-flags/my_new_feature/toggle?enabled=true

# Disable
curl -X POST http://localhost:8080/v1/feature-flags/my_new_feature/toggle?enabled=false
```

### Emergency Disable

**Emergency disable a feature flag:**
```bash
curl -X POST http://localhost:8080/v1/feature-flags/my_new_feature/emergency-disable?reason=critical_bug_found
```

## Gradual Rollout

### Percentage-Based Rollout

Gradually increase the percentage of users who see a feature:

1. **Start with 0%**
   ```bash
   curl -X PUT http://localhost:8080/v1/feature-flags/my_new_feature/config \
     -H "Content-Type: application/json" \
     -d '{
       "rolloutPercentage": 0.0,
       "gradualRolloutEnabled": true
     }'
   ```

2. **Increase to 10%**
   ```bash
   curl -X PUT http://localhost:8080/v1/feature-flags/my_new_feature/config \
     -H "Content-Type: application/json" \
     -d '{
       "rolloutPercentage": 10.0,
       "gradualRolloutEnabled": true
     }'
   ```

3. **Continue increasing** (25%, 50%, 75%, 100%)

### User Group Targeting

Target specific user groups:

```bash
curl -X PUT http://localhost:8080/v1/feature-flags/my_new_feature/config \
  -H "Content-Type: application/json" \
  -d '{
    "targetingRules": [
      {
        "type": "USER_GROUP",
        "userGroup": "beta_users"
      }
    ]
  }'
```

### Custom Attribute Targeting

Target users based on custom attributes:

```bash
curl -X PUT http://localhost:8080/v1/feature-flags/my_new_feature/config \
  -H "Content-Type: application/json" \
  -d '{
    "targetingRules": [
      {
        "type": "CUSTOM_ATTRIBUTE",
        "attributeName": "subscription_tier",
        "attributeValue": "premium"
      }
    ]
  }'
```

## A/B Testing

### Starting an A/B Test

**Create an A/B test:**
```bash
curl -X POST http://localhost:8080/v1/feature-flags/my_new_feature/ab-test \
  -H "Content-Type: application/json" \
  -d '{
    "name": "psychological_analysis_ab_test",
    "description": "Test enhanced vs standard psychological analysis",
    "variantAPercentage": 50.0,
    "variantBPercentage": 50.0,
    "successMetric": "conversion_rate",
    "minimumSampleSize": 1000,
    "significanceLevel": 0.95
  }'
```

### Monitoring A/B Test Results

**Get A/B test results:**
```bash
curl http://localhost:8080/v1/feature-flags/my_new_feature/ab-test/results
```

**Response example:**
```json
{
  "flagName": "my_new_feature",
  "status": "RUNNING",
  "variantAUsers": 500,
  "variantBUsers": 500,
  "variantASuccesses": 45,
  "variantBSuccesses": 52,
  "variantASuccessRate": 0.09,
  "variantBSuccessRate": 0.104,
  "statisticalSignificance": 0.85,
  "winningVariant": "variant_b",
  "timestamp": "2024-01-15T10:30:00"
}
```

## Analytics and Monitoring

### Recording Usage

**Record feature flag usage:**
```bash
curl -X POST http://localhost:8080/v1/feature-flags/analytics/usage \
  -d "flagName=my_new_feature&userId=user123&enabled=true"
```

### Recording Conversions

**Record conversion events:**
```bash
curl -X POST http://localhost:8080/v1/feature-flags/analytics/conversion \
  -d "flagName=my_new_feature&userId=user123&conversionType=purchase&value=29.99"
```

### Viewing Analytics

**Get feature flag analytics:**
```bash
curl http://localhost:8080/v1/feature-flags/analytics/my_new_feature
```

**Get analytics dashboard:**
```bash
curl http://localhost:8080/v1/feature-flags/analytics/dashboard
```

## Integration with Psychological Analysis

### Using Feature Flags in Code

The `FeatureFlagIntegrationService` provides methods to integrate feature flags with psychological analysis services:

```java
@Autowired
private FeatureFlagIntegrationService featureFlagIntegrationService;

// Execute psychological analysis with feature flags
CompletableFuture<Object> result = featureFlagIntegrationService
    .executePsychologicalAnalysisWithFeatureFlags(userId, content, context);

// Execute state update with feature flags
CompletableFuture<Object> result = featureFlagIntegrationService
    .executeStateUpdateWithFeatureFlags(userId, postContent, postId, context);

// Execute personality analysis with feature flags
CompletableFuture<Object> result = featureFlagIntegrationService
    .executePersonalityAnalysisWithFeatureFlags(userId, content, context);

// Execute mood analysis with feature flags
CompletableFuture<Object> result = featureFlagIntegrationService
    .executeMoodAnalysisWithFeatureFlags(userId, content, context);
```

### Checking Feature Flag Status

**Get feature flag status for a user:**
```bash
curl "http://localhost:8080/v1/feature-flags/my_new_feature/enabled?userId=user123"
```

**Response:**
```json
{
  "flagName": "my_new_feature",
  "userId": "user123",
  "enabled": true,
  "timestamp": "2024-01-15T10:30:00"
}
```

## Best Practices

### 1. Feature Flag Naming

- Use descriptive names: `psychological_analysis_v2` instead of `feature_1`
- Include version numbers for iterative features
- Use consistent naming conventions

### 2. Gradual Rollout Strategy

1. **Start Small**: Begin with 0-5% of users
2. **Monitor Closely**: Watch for errors and performance issues
3. **Increase Gradually**: 5% → 10% → 25% → 50% → 75% → 100%
4. **Have Rollback Plan**: Be ready to disable quickly

### 3. A/B Testing Best Practices

- **Set Clear Success Metrics**: Define what success looks like
- **Ensure Statistical Significance**: Wait for sufficient sample size
- **Test One Variable at a Time**: Avoid confounding variables
- **Monitor Both Variants**: Watch for negative impacts

### 4. Cleanup

- **Remove Old Flags**: Clean up flags that are no longer needed
- **Document Decisions**: Record why flags were enabled/disabled
- **Monitor Flag Usage**: Track which flags are actually being used

### 5. Security

- **Access Control**: Limit who can modify feature flags
- **Audit Logging**: Track all flag changes
- **Emergency Procedures**: Have clear emergency disable procedures

## Troubleshooting

### Common Issues

1. **Feature Flag Not Working**
   ```bash
   # Check if flag exists
   curl http://localhost:8080/v1/feature-flags/my_new_feature/config
   
   # Check if flag is enabled
   curl "http://localhost:8080/v1/feature-flags/my_new_feature/enabled?userId=user123"
   ```

2. **User Not Seeing Feature**
   ```bash
   # Check targeting rules
   curl http://localhost:8080/v1/feature-flags/my_new_feature/config
   
   # Verify user context
   curl "http://localhost:8080/v1/feature-flags/my_new_feature/enabled?userId=user123&context={\"userGroup\":\"beta_users\"}"
   ```

3. **A/B Test Not Working**
   ```bash
   # Check A/B test status
   curl http://localhost:8080/v1/feature-flags/my_new_feature/ab-test/results
   
   # Verify A/B test configuration
   curl http://localhost:8080/v1/feature-flags/my_new_feature/config
   ```

### Debugging Commands

**Check feature flag events:**
```bash
curl http://localhost:8080/v1/feature-flags/my_new_feature/events?limit=10
```

**Get user behavior analytics:**
```bash
curl http://localhost:8080/v1/feature-flags/analytics/user/user123
```

**Check system health:**
```bash
curl http://localhost:8080/v1/feature-flags/health
```

## API Reference

### Feature Flag Management

**Base URL:** `http://localhost:8080/v1/feature-flags`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/{flagName}/enabled` | Check if feature flag is enabled for user |
| GET | `/{flagName}/config` | Get feature flag configuration |
| PUT | `/{flagName}/config` | Update feature flag configuration |
| POST | `/{flagName}` | Create new feature flag |
| POST | `/{flagName}/toggle` | Toggle feature flag (enable/disable) |
| POST | `/{flagName}/ab-test` | Start A/B test for feature flag |
| POST | `/{flagName}/emergency-disable` | Emergency disable feature flag |
| GET | `/{flagName}/events` | Get feature flag events |
| GET | | Get all feature flags |

### Analytics

**Base URL:** `http://localhost:8080/v1/feature-flags/analytics`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/usage` | Record feature flag usage |
| POST | `/conversion` | Record conversion event |
| GET | `/{flagName}` | Get feature flag analytics |
| GET | `/{flagName}/ab-test` | Get A/B test analytics |
| GET | `/user/{userId}` | Get user behavior analytics |
| GET | `/dashboard` | Get analytics dashboard |

### Request/Response Examples

**Create Feature Flag Request:**
```json
{
  "name": "psychological_analysis_v2",
  "description": "Enhanced psychological analysis with improved algorithms",
  "enabled": false,
  "targetingRules": [
    {
      "type": "PERCENTAGE",
      "value": 0.0
    }
  ],
  "gradualRolloutEnabled": true,
  "rolloutPercentage": 0.0,
  "abTestEnabled": false
}
```

**Feature Flag Status Response:**
```json
{
  "flagName": "psychological_analysis_v2",
  "userId": "user123",
  "enabled": true,
  "timestamp": "2024-01-15T10:30:00"
}
```

**Analytics Response:**
```json
{
  "flagName": "psychological_analysis_v2",
  "totalRequests": 1000,
  "enabledRequests": 250,
  "enableRate": 0.25,
  "totalConversions": 45,
  "conversionRate": 0.045,
  "totalConversionValue": 1349.55,
  "averageConversionValue": 29.99,
  "uniqueUsers": 800,
  "timestamp": "2024-01-15T10:30:00"
}
```

## Support

For feature flag issues and questions:

1. **Check the logs** for error messages
2. **Review the troubleshooting section** above
3. **Contact the development team** for assistance
4. **Create an issue** in the project repository

## Changelog

### Version 1.0.0
- Initial feature flag management system
- Gradual rollout capabilities
- A/B testing framework
- Analytics and monitoring
- Integration with psychological analysis services
- Emergency disable functionality
- Comprehensive API and documentation

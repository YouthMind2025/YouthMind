# CORS Production Configuration

This document outlines the comprehensive CORS (Cross-Origin Resource Sharing) configuration implemented for the YouthMind Backend API across different environments.

## Overview

The CORS configuration provides environment-specific settings to ensure secure cross-origin requests while maintaining functionality across development, staging, and production environments.

**CRITICAL SECURITY NOTE:** This configuration has been hardened to prevent the critical security vulnerability of allowing wildcard origins (`*`) with credentials enabled. This vulnerability allows external domains to make authenticated requests to your API. All configurations now use explicit origin allowlists.

## Environment-Specific Configurations

### Development Environment (`dev`)

**Profile:** `dev`  
**Configuration:** `application-dev.yaml` and `CorsConfig.java`

- **Origins:** Explicit localhost origins only:
  - `http://localhost:3000`
  - `http://localhost:3001`
  - `http://127.0.0.1:3000`
  - `http://127.0.0.1:3001`
- **Methods:** All HTTP methods (GET, POST, PUT, DELETE, OPTIONS, PATCH)
- **Headers:** All headers (`*`)
- **Credentials:** Enabled
- **Max Age:** 3600 seconds (1 hour)

**Use Case:** Local development with explicit localhost origins for security.

**Security:** Uses explicit origins instead of wildcard to prevent external domains from making authenticated requests, even in development.

### Staging Environment (`staging`)

**Profile:** `staging`  
**Configuration:** `application-staging.yaml`

- **Origins:** 
  - `https://staging.youthmind.ai`
  - `https://staging-api.youthmind.ai`
  - `http://localhost:3000` (for local testing)
  - `http://localhost:3001` (for local testing)
- **Methods:** GET, POST, PUT, DELETE, OPTIONS, PATCH
- **Headers:** Specific headers only (Authorization, Content-Type, etc.)
- **Credentials:** Enabled
- **Max Age:** 1800 seconds (30 minutes)

**Use Case:** Pre-production testing with controlled access.

### Production Environment (`prod`)

**Profile:** `prod`  
**Configuration:** `application-prod.yaml`

- **Origins:** 
  - `https://youthmind.ai`
  - `https://www.youthmind.ai`
  - `https://api.youthmind.ai`
- **Methods:** GET, POST, PUT, DELETE, OPTIONS (restricted)
- **Headers:** Specific production headers only
- **Credentials:** Enabled
- **Max Age:** 600 seconds (10 minutes)

**Use Case:** Live production environment with strict security.

## Environment Variables

### Required Environment Variables

```bash
# CORS Configuration
ALLOWED_ORIGINS=https://youthmind.ai,https://www.youthmind.ai,https://api.youthmind.ai
ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
ALLOWED_HEADERS=Authorization,Content-Type,X-Requested-With,Accept,Origin,Access-Control-Request-Method,Access-Control-Request-Headers,X-API-Key,X-Client-Version
EXPOSED_HEADERS=Authorization,Content-Type,X-Total-Count,X-Rate-Limit-Remaining,X-Rate-Limit-Reset,X-Request-ID
ALLOW_CREDENTIALS=true
MAX_AGE=600
```

### Optional Environment Variables

```bash
# Additional CORS settings
CORS_DEBUG=true  # Enable CORS debugging logs
CORS_LOG_LEVEL=INFO  # CORS logging level
```

## Security Headers Implementation

### Content Security Policy (CSP)

The production configuration includes a strict CSP header:

```
Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' https://api.deepseek.com; frame-ancestors 'none'; base-uri 'self'; form-action 'self'
```

### Additional Security Headers

- **X-Content-Type-Options:** `nosniff`
- **X-Frame-Options:** `DENY`
- **X-XSS-Protection:** `1; mode=block`
- **Strict-Transport-Security:** `max-age=31536000; includeSubDomains; preload`
- **Referrer-Policy:** `strict-origin-when-cross-origin`
- **Permissions-Policy:** `camera=(), microphone=(), geolocation=(), payment=()`

## Configuration Classes

### CorsConfig.java

The main CORS configuration class that provides environment-specific CORS settings:

- `devCorsConfigurationSource()` - Development configuration with explicit localhost origins
- `stagingCorsConfigurationSource()` - Staging configuration with restricted origins
- `prodCorsConfigurationSource()` - Production configuration with strict domain allowlist
- `defaultCorsConfigurationSource()` - Fallback configuration that requires explicit origins when credentials are enabled
- `validateCorsConfiguration()` - Security validation that prevents wildcard origins with credentials

**Security Features:**
- Automatic validation of CORS configuration at bean creation time
- Throws `IllegalStateException` if wildcard origins are detected with credentials enabled
- Clear error messages explaining security violations
- Prevents application startup with insecure CORS configuration

### SecurityConfig.java

Updated to use the new CORS configuration and include security headers:

- Integrates with `CorsConfig` via `@Qualifier`
- Implements comprehensive security headers
- Configures CSP and other security measures

## Testing

### Staging Environment Testing

Run the staging CORS tests:

```bash
# Windows
./test-cors-staging.bat

# Linux/Mac
./test-cors-staging.sh
```

### Production Environment Testing

Run the production CORS tests:

```bash
# Windows
./test-cors-production.bat

# Linux/Mac
./test-cors-production.sh
```

### Manual Testing

Test CORS configuration manually:

```bash
# Test preflight request
curl -X OPTIONS \
  -H "Origin: https://youthmind.ai" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type,Authorization" \
  -v http://localhost:8080/api/v1/auth/login

# Test actual request
curl -X GET \
  -H "Origin: https://youthmind.ai" \
  -H "Authorization: Bearer your-token" \
  -v http://localhost:8080/api/v1/posts
```

## Deployment Considerations

### Environment Setup

1. **Set the active profile:**
   ```bash
   export SPRING_PROFILES_ACTIVE=prod
   ```

2. **Configure environment variables:**
   ```bash
   export ALLOWED_ORIGINS="https://youthmind.ai,https://www.youthmind.ai,https://api.youthmind.ai"
   ```

3. **Verify configuration:**
   ```bash
   # Check CORS configuration
   curl -I -H "Origin: https://youthmind.ai" https://api.youthmind.ai/v1/posts
   ```

### Monitoring

Monitor CORS-related issues through:

- Application logs (CORS debug information)
- Security headers validation
- Preflight request success rates
- Origin validation failures

### Troubleshooting

Common CORS issues and solutions:

1. **CORS errors in browser console:**
   - Verify the origin is in the allowed list
   - Check if credentials are properly configured
   - Ensure preflight requests are handled correctly

2. **Security header warnings:**
   - Verify CSP configuration matches your frontend requirements
   - Check if all required headers are present
   - Validate HSTS configuration

3. **Environment-specific issues:**
   - Ensure the correct profile is active
   - Verify environment variables are set correctly
   - Check configuration file loading order

## Best Practices

1. **NEVER use wildcard origins (`*`) with credentials enabled** - This applies to ALL environments, including development. The application enforces this at startup.

2. **Always specify exact domains/origins** - Use explicit allowlists in all environments (development, staging, production).

3. **Use HTTPS in production environments** - All production origins should use HTTPS.

4. **Regularly audit allowed origins** - Review and remove unused origins periodically.

5. **Monitor CORS-related security events** - Track origin validation failures and preflight request patterns.

6. **Test CORS configuration in staging before production deployment** - Verify all frontend applications work with restricted origins.

7. **Keep CORS configuration in version control** - Use `app.cors.*` properties in YAML files, not `spring.web.cors.*`.

8. **Remove controller-level `@CrossOrigin` annotations** - Let the global `CorsConfig` handle all CORS configuration consistently.

9. **Single source of truth** - Use only `CorsConfig.java` for CORS configuration. Do not use conflicting YAML properties.

10. **Fail fast on invalid configuration** - The application validates CORS configuration at startup and will not start with insecure settings.

## Security Considerations

### Critical Security Rules

1. **NEVER use wildcard origins (`*`) with credentials enabled** - This is a critical security vulnerability that allows external domains to make authenticated requests to your API. The application will throw an `IllegalStateException` at startup if this configuration is detected.

2. **Always use explicit origin allowlists** - Even in development, specify exact origins (e.g., `http://localhost:3000`) instead of wildcards.

3. **Origin Validation:** Only allow trusted domains - Regularly audit and review allowed origins.

4. **Credential Handling:** When `allowCredentials: true`, you MUST specify explicit origins. Wildcard origins are automatically rejected.

5. **Header Restrictions:** Limit allowed headers to necessary ones to reduce attack surface.

6. **Method Restrictions:** Only allow required HTTP methods.

7. **Regular Audits:** Periodically review and update CORS settings.

8. **Monitoring:** Implement logging and monitoring for CORS violations.

### Automatic Security Validation

The `CorsConfig.java` class includes automatic validation that:
- Prevents wildcard origins (`*` or `**`) when credentials are enabled
- Throws `IllegalStateException` at application startup if invalid configuration is detected
- Provides clear error messages explaining the security issue

This ensures that even if configuration is misconfigured, the application will fail fast with a clear security error rather than starting with a vulnerable configuration.

## Security Fixes Applied (2024)

### Critical Security Vulnerability Fixed

The application previously allowed wildcard origins (`*`) with credentials enabled, which is a critical security vulnerability. This has been fixed:

1. **Development Profile:** Changed from wildcard (`*`) to explicit localhost origins:
   - `http://localhost:3000`
   - `http://localhost:3001`
   - `http://127.0.0.1:3000`
   - `http://127.0.0.1:3001`

2. **Controller-Level Overrides Removed:** Removed `@CrossOrigin(origins = "*")` annotations from 8 controllers to ensure consistent global CORS configuration.

3. **Configuration Cleanup:** Removed conflicting `spring.web.cors.*` properties from YAML files that were not being used.

4. **Automatic Validation:** Added `validateCorsConfiguration()` method that prevents wildcard origins with credentials at application startup.

5. **Default Fallback Security:** Updated default fallback configuration to require explicit origins when credentials are enabled.

### Migration from Wildcard CORS

If you need to add additional origins:

1. **Identify all frontend domains** that need access
2. **Add explicit origins** to the appropriate profile configuration in `CorsConfig.java` or via `app.cors.allowed-origins` property
3. **Test in staging environment** with restricted origins
4. **Update frontend applications** to use correct origins
5. **Deploy to production** with specific domain allowlist
6. **Monitor for any CORS errors** and adjust as needed

**Note:** The application will now refuse to start if wildcard origins are configured with credentials enabled, ensuring this vulnerability cannot be accidentally reintroduced.

## Support

For CORS-related issues or questions:

1. Check the application logs for CORS debug information
2. Verify environment configuration
3. Test with the provided test scripts
4. Review this documentation for troubleshooting steps
5. Contact the development team for assistance

---

**Last Updated:** $(date)  
**Version:** 1.0.0  
**Maintainer:** YouthMind Development Team

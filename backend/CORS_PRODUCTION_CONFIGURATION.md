# CORS Production Configuration

This document outlines the comprehensive CORS (Cross-Origin Resource Sharing) configuration implemented for the YouthMind Backend API across different environments.

## Overview

The CORS configuration provides environment-specific settings to ensure secure cross-origin requests while maintaining functionality across development, staging, and production environments.

## Environment-Specific Configurations

### Development Environment (`dev`)

**Profile:** `dev`  
**Configuration:** `application-dev.yaml`

- **Origins:** Wildcard (`*`) - allows all origins for development flexibility
- **Methods:** All HTTP methods (GET, POST, PUT, DELETE, OPTIONS, PATCH)
- **Headers:** All headers (`*`)
- **Credentials:** Enabled
- **Max Age:** 3600 seconds (1 hour)

**Use Case:** Local development with various frontend frameworks and tools.

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

- `devCorsConfigurationSource()` - Development configuration
- `stagingCorsConfigurationSource()` - Staging configuration  
- `prodCorsConfigurationSource()` - Production configuration
- `defaultCorsConfigurationSource()` - Fallback configuration

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

1. **Never use wildcard origins in production**
2. **Always specify exact domains in production**
3. **Use HTTPS in production environments**
4. **Regularly audit allowed origins**
5. **Monitor CORS-related security events**
6. **Test CORS configuration in staging before production deployment**
7. **Keep CORS configuration in version control**
8. **Document any CORS exceptions or special cases**

## Security Considerations

1. **Origin Validation:** Only allow trusted domains
2. **Credential Handling:** Be cautious with `allowCredentials: true`
3. **Header Restrictions:** Limit allowed headers to necessary ones
4. **Method Restrictions:** Only allow required HTTP methods
5. **Regular Audits:** Periodically review and update CORS settings
6. **Monitoring:** Implement logging and monitoring for CORS violations

## Migration from Wildcard CORS

If migrating from wildcard CORS configuration:

1. **Identify all frontend domains** that need access
2. **Test in staging environment** with restricted origins
3. **Update frontend applications** to use correct origins
4. **Deploy to production** with specific domain allowlist
5. **Monitor for any CORS errors** and adjust as needed

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

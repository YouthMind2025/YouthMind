# YouthMind Backend - Production Environment Setup Guide 🚀

## 🔐 Environment Variables Configuration

### Required Environment Variables

The following environment variables **MUST** be configured for production deployment:

#### **Critical Security Variables**

```bash
# Admin Secret Key - REQUIRED for admin functionality
ADMIN_SECRET_KEY=your-secure-production-admin-secret-key-here

# JWT Secret Key - REQUIRED for authentication
JWT_SECRET=your-secure-jwt-secret-key-here

# Database Credentials - REQUIRED for data persistence
DB_USERNAME=youthmind_user
DB_PASSWORD=your-secure-database-password
DB_HOST=your-database-host
DB_PORT=5432
DB_NAME=youthmind_db

# Redis Credentials - REQUIRED for caching and sessions
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your-secure-redis-password
```

#### **Optional Configuration Variables**

```bash
# File Upload Configuration
UPLOAD_DIR=/app/uploads
AVATAR_MAX_SIZE=2097152
AVATAR_ALLOWED_TYPES=image/jpeg,image/png,image/gif,image/webp

# AI Service Configuration
YOUTHMIND_DEEPSEEK_API_KEY=your-deepseek-api-key
YOUTHMIND_DEEPSEEK_API_URL=https://api.deepseek.com/v1
YOUTHMIND_AI_ENABLED=true

# JWT Configuration
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
```

## 🛡️ Security Best Practices

### Admin Secret Key Security

**CRITICAL SECURITY REQUIREMENTS:**

1. **Never use default values in production**
   - Default: `YOUTHMIND_ADMIN_2024` (development only)
   - Production: Must be a secure, unique, randomly generated key

2. **Key Generation Guidelines**
   ```bash
   # Generate a secure admin secret key
   openssl rand -base64 32
   # or
   python3 -c "import secrets; print(secrets.token_urlsafe(32))"
   ```

3. **Key Management**
   - Store in secure environment variable management system
   - Never commit to version control
   - Rotate regularly (recommended: every 90 days)
   - Use different keys for different environments

4. **Access Control**
   - Limit access to production admin secret to authorized personnel only
   - Use secure secret management tools (AWS Secrets Manager, HashiCorp Vault, etc.)

### JWT Secret Key Security

1. **Generate a strong JWT secret**
   ```bash
   # Generate a secure JWT secret (512-bit)
   openssl rand -hex 64
   ```

2. **Key Requirements**
   - Minimum 256 bits (32 characters)
   - Use cryptographically secure random generation
   - Different from admin secret key
   - Rotate periodically

## 🚀 Deployment Steps

### 1. Pre-Deployment Checklist

- [ ] All required environment variables configured
- [ ] Database is accessible and migrations applied
- [ ] Redis is accessible and configured
- [ ] Admin secret key is secure and unique
- [ ] JWT secret key is secure and unique
- [ ] SSL/TLS certificates configured (if applicable)
- [ ] Firewall rules configured
- [ ] Monitoring and logging configured

### 2. Environment-Specific Configuration

#### Development Environment
```bash
# .env.development
ADMIN_SECRET_KEY=YOUTHMIND_ADMIN_2024  # Default allowed for dev
JWT_SECRET=dev-jwt-secret-key
DB_USERNAME=youthmind_user
DB_PASSWORD=dev_password
REDIS_HOST=localhost
REDIS_PORT=6379
```

#### Staging Environment
```bash
# .env.staging
ADMIN_SECRET_KEY=staging-admin-secret-key-here
JWT_SECRET=staging-jwt-secret-key-here
DB_USERNAME=youthmind_user
DB_PASSWORD=staging_secure_password
REDIS_HOST=staging-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=staging_redis_password
```

#### Production Environment
```bash
# .env.production
ADMIN_SECRET_KEY=production-admin-secret-key-here
JWT_SECRET=production-jwt-secret-key-here
DB_USERNAME=youthmind_user
DB_PASSWORD=production_secure_password
DB_HOST=production-db-host
REDIS_HOST=production-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=production_redis_password
```

### 3. Docker Deployment

#### Dockerfile Environment Variables
```dockerfile
# Dockerfile
ENV ADMIN_SECRET_KEY=""
ENV JWT_SECRET=""
ENV DB_USERNAME=""
ENV DB_PASSWORD=""
ENV REDIS_HOST=""
ENV REDIS_PASSWORD=""
```

#### Docker Compose Example
```yaml
# docker-compose.yml
version: '3.8'
services:
  youthmind-backend:
    image: youthmind-backend:latest
    environment:
      - ADMIN_SECRET_KEY=${ADMIN_SECRET_KEY}
      - JWT_SECRET=${JWT_SECRET}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - REDIS_HOST=${REDIS_HOST}
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    ports:
      - "8080:8080"
```

### 4. Kubernetes Deployment

#### Secret Configuration
```yaml
# admin-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: youthmind-secrets
type: Opaque
data:
  admin-secret-key: <base64-encoded-admin-secret>
  jwt-secret: <base64-encoded-jwt-secret>
  db-password: <base64-encoded-db-password>
  redis-password: <base64-encoded-redis-password>
```

#### Deployment Configuration
```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: youthmind-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: youthmind-backend
  template:
    metadata:
      labels:
        app: youthmind-backend
    spec:
      containers:
      - name: youthmind-backend
        image: youthmind-backend:latest
        env:
        - name: ADMIN_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: youthmind-secrets
              key: admin-secret-key
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: youthmind-secrets
              key: jwt-secret
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: youthmind-secrets
              key: db-password
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: youthmind-secrets
              key: redis-password
        ports:
        - containerPort: 8080
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
```

## 🔍 Validation and Testing

### 1. Environment Variable Validation

The application includes built-in validation that will:
- ✅ Check if `ADMIN_SECRET_KEY` is configured
- ✅ Validate admin secret is not using default value in production
- ✅ Fail startup if critical environment variables are missing
- ✅ Log security warnings for development configurations

### 2. Health Check Validation

```bash
# Test admin secret validation
curl -X GET http://localhost:8080/actuator/health

# Test admin registration (should work with correct secret)
curl -X POST http://localhost:8080/v1/auth/admin/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@youthmind.ai",
    "password": "secure_password",
    "firstName": "Admin",
    "lastName": "User",
    "username": "admin",
    "adminSecretKey": "your-admin-secret-key"
  }'
```

### 3. Security Validation

```bash
# Verify admin secret is not exposed in logs
grep -i "admin.*secret" /var/log/youthmind-backend.log

# Verify environment variables are not exposed
curl -X GET http://localhost:8080/actuator/env | grep -i secret
```

## 🚨 Troubleshooting

### Common Issues

1. **Application fails to start with "ADMIN_SECRET_KEY must be configured"**
   - Solution: Set the `ADMIN_SECRET_KEY` environment variable

2. **Admin registration fails with "Invalid admin secret key"**
   - Solution: Verify the `ADMIN_SECRET_KEY` matches the value sent in the request

3. **Security warning about default admin secret in production**
   - Solution: Generate and set a secure admin secret key

4. **Environment variables not loading**
   - Solution: Check environment variable syntax and ensure they're properly exported

### Log Analysis

```bash
# Check for admin secret validation messages
grep "Admin secret key validation" /var/log/youthmind-backend.log

# Check for security warnings
grep "security.*warning\|admin.*secret" /var/log/youthmind-backend.log

# Check for startup validation
grep "AdminSecretValidator" /var/log/youthmind-backend.log
```

## 📋 Maintenance

### Regular Tasks

1. **Monthly Security Review**
   - Rotate admin secret key
   - Rotate JWT secret key
   - Review access logs
   - Update security documentation

2. **Environment Variable Audit**
   - Verify all secrets are properly configured
   - Check for any hardcoded secrets
   - Validate secret management practices

3. **Monitoring**
   - Monitor admin registration attempts
   - Track failed authentication attempts
   - Alert on security-related log entries

---

## 🎯 Quick Reference

### Essential Commands

```bash
# Generate secure admin secret
openssl rand -base64 32

# Generate secure JWT secret
openssl rand -hex 64

# Test admin registration
curl -X POST http://localhost:8080/v1/auth/admin/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"password","firstName":"Admin","lastName":"User","username":"admin","adminSecretKey":"YOUR_SECRET_KEY"}'

# Check health status
curl http://localhost:8080/actuator/health
```

### Contact Information

- **Backend Architect**: Mustafo
- **DevOps Engineer**: Kevin
- **Lead Developer**: Ahmed

---

🚀 **Production deployment ready with secure admin secret management!** 🚀

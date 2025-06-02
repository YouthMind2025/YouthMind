# Production Deployment Guide
## YouthMind Infrastructure & Release Management

*Complete guide for deploying YouthMind to production environments*

---

## Overview

This deployment guide covers the complete process of setting up YouthMind's production infrastructure, from initial server configuration to ongoing monitoring and maintenance. The architecture supports high availability, scalability, and robust security for handling sensitive mental health data.

### Production Architecture
```
Internet
    ↓
Load Balancer (AWS ALB)
    ↓
Nginx Reverse Proxy
    ↓
┌─────────────────┬─────────────────┬─────────────────┐
│   Backend API   │   AI Services   │   Admin Panel   │
│  (Spring Boot)  │    (FastAPI)    │    (React)      │
└─────────────────┴─────────────────┴─────────────────┘
    ↓                       ↓                       ↓
┌─────────────────┬─────────────────┬─────────────────┐
│   PostgreSQL    │     Redis       │   File Storage  │
│   (Primary DB)  │    (Cache)      │     (AWS S3)    │
└─────────────────┴─────────────────┴─────────────────┘
```

---

## Infrastructure Requirements

### **Minimum Production Specifications**

#### Application Servers
- **Backend API**: 2 vCPUs, 4GB RAM, 50GB SSD
- **AI Services**: 4 vCPUs, 8GB RAM, 100GB SSD (GPU recommended)
- **Admin Panel**: 1 vCPU, 2GB RAM, 20GB SSD

#### Database & Cache
- **PostgreSQL**: 2 vCPUs, 8GB RAM, 200GB SSD, automated backups
- **Redis**: 2 vCPUs, 4GB RAM, 50GB SSD

#### Load Balancer & CDN
- Application Load Balancer with SSL termination
- CloudFront CDN for static assets
- Route 53 for DNS management

### **Recommended Production Configuration**

#### AWS Infrastructure
```yaml
# infrastructure.yml
VPC: 10.0.0.0/16
Subnets:
  - Public: 10.0.1.0/24 (Load Balancer)
  - Private App: 10.0.2.0/24 (Applications)
  - Private DB: 10.0.3.0/24 (Databases)

Security Groups:
  - ALB: Allow 80,443 from 0.0.0.0/0
  - Apps: Allow 8080,8001,3000 from ALB
  - DB: Allow 5432,6379 from Apps
```

---

## Environment Setup

### **1. Domain & SSL Configuration**

#### Domain Setup
```bash
# Configure DNS records
youthmind.dev               A      <load-balancer-ip>
api.youthmind.dev          CNAME  youthmind.dev
ai.youthmind.dev           CNAME  youthmind.dev
admin.youthmind.dev        CNAME  youthmind.dev
```

#### SSL Certificate (Let's Encrypt)
```bash
# Install Certbot
sudo apt update
sudo apt install certbot python3-certbot-nginx

# Generate certificates
sudo certbot --nginx -d youthmind.dev \
  -d api.youthmind.dev \
  -d ai.youthmind.dev \
  -d admin.youthmind.dev

# Auto-renewal
sudo crontab -e
0 12 * * * /usr/bin/certbot renew --quiet
```

### **2. Server Preparation**

#### System Dependencies
```bash
#!/bin/bash
# server-setup.sh

# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker & Docker Compose
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Install Nginx
sudo apt install nginx -y

# Install monitoring tools
sudo apt install htop iotop netstat-nat -y
```

#### Java & Python Setup
```bash
# Install Java 17
sudo apt install openjdk-17-jdk -y
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc

# Install Python 3.9+
sudo apt install python3.9 python3.9-venv python3-pip -y
```

---

## Application Deployment

### **3. Database Setup**

#### PostgreSQL Configuration
```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: youthmind_prod
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
    restart: unless-stopped
    
  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
```

#### Database Migration
```bash
# Create production database schema
psql -h localhost -U ${DB_USER} -d youthmind_prod -f schema.sql

# Apply migrations
cd backend
mvn liquibase:update -Dliquibase.url=jdbc:postgresql://localhost:5432/youthmind_prod
```

### **4. Backend Service Deployment**

#### Application Configuration
```properties
# application-prod.properties
server.port=8080
spring.profiles.active=prod

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/youthmind_prod
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# Redis
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=${REDIS_PASSWORD}

# Security
jwt.secret=${JWT_SECRET}
jwt.expiration=2592000

# AI Service
ai.service.url=http://localhost:8001
ai.service.timeout=30s

# Monitoring
management.endpoints.web.exposure.include=health,metrics,prometheus
management.endpoint.health.show-details=always
```

#### Service Deployment Script
```bash
#!/bin/bash
# deploy-backend.sh

# Build application
cd backend
mvn clean package -DskipTests

# Stop existing service
sudo systemctl stop youthmind-backend

# Deploy new version
sudo cp target/youthmind-backend-*.jar /opt/youthmind/backend.jar

# Start service
sudo systemctl start youthmind-backend
sudo systemctl enable youthmind-backend

# Verify deployment
sleep 10
curl -f http://localhost:8080/actuator/health || exit 1
```

#### Systemd Service Configuration
```ini
# /etc/systemd/system/youthmind-backend.service
[Unit]
Description=YouthMind Backend Service
After=network.target

[Service]
Type=simple
User=youthmind
ExecStart=/usr/bin/java -jar /opt/youthmind/backend.jar
Restart=always
RestartSec=10
Environment=SPRING_PROFILES_ACTIVE=prod
EnvironmentFile=/opt/youthmind/backend.env

[Install]
WantedBy=multi-user.target
```

### **5. AI Services Deployment**

#### Docker Configuration
```dockerfile
# Dockerfile.ai-services
FROM python:3.9-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Expose port
EXPOSE 8001

# Health check
HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8001/health || exit 1

# Run application
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
```

#### Production AI Service Config
```yaml
# ai-services/config/production.yml
model:
  sentiment_model: "cardiffnlp/twitter-roberta-base-sentiment-latest"
  emotion_model: "j-hartmann/emotion-english-distilroberta-base"
  question_generation_model: "valhalla/t5-small-qg-hl"
  
cache:
  enabled: true
  ttl: 3600
  
security:
  api_key_required: true
  rate_limit:
    requests_per_minute: 60
    
logging:
  level: INFO
  format: json
  
monitoring:
  metrics_enabled: true
  health_check_interval: 30
```

### **6. Nginx Configuration**

#### Production Nginx Config
```nginx
# /etc/nginx/sites-available/youthmind
upstream backend {
    server localhost:8080;
}

upstream ai_service {
    server localhost:8001;
}

upstream admin_panel {
    server localhost:3000;
}

# Rate limiting
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=ai:10m rate=5r/s;

server {
    listen 80;
    server_name youthmind.dev api.youthmind.dev ai.youthmind.dev admin.youthmind.dev;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.youthmind.dev;
    
    ssl_certificate /etc/letsencrypt/live/youthmind.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/youthmind.dev/privkey.pem;
    
    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains";
    
    location / {
        limit_req zone=api burst=20 nodelay;
        
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }
}

server {
    listen 443 ssl http2;
    server_name ai.youthmind.dev;
    
    ssl_certificate /etc/letsencrypt/live/youthmind.dev/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/youthmind.dev/privkey.pem;
    
    location / {
        limit_req zone=ai burst=10 nodelay;
        
        proxy_pass http://ai_service;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # AI-specific timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;
    }
}
```

---

## Security Configuration

### **7. Application Security**

#### Environment Variables
```bash
# /opt/youthmind/backend.env
DB_USER=youthmind_user
DB_PASSWORD=<secure-random-password>
REDIS_PASSWORD=<secure-random-password>
JWT_SECRET=<256-bit-random-key>
AI_SERVICE_API_KEY=<secure-api-key>
ENCRYPTION_KEY=<encryption-key>
```

#### Database Security
```sql
-- Create restricted database user
CREATE USER youthmind_app WITH PASSWORD '<secure-password>';
GRANT CONNECT ON DATABASE youthmind_prod TO youthmind_app;
GRANT USAGE ON SCHEMA public TO youthmind_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO youthmind_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO youthmind_app;

-- Configure PostgreSQL security
-- /etc/postgresql/14/main/postgresql.conf
ssl = on
ssl_cert_file = '/etc/ssl/certs/ssl-cert-snakeoil.pem'
ssl_key_file = '/etc/ssl/private/ssl-cert-snakeoil.key'
```

#### Firewall Configuration
```bash
# UFW firewall setup
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow ssh
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

### **8. Monitoring & Logging**

#### Prometheus Configuration
```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'youthmind-backend'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    
  - job_name: 'youthmind-ai'
    static_configs:
      - targets: ['localhost:8001']
    metrics_path: '/metrics'
```

#### Grafana Dashboard
```json
{
  "dashboard": {
    "title": "YouthMind Production Monitoring",
    "panels": [
      {
        "title": "API Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "http_request_duration_seconds_bucket{job=\"youthmind-backend\"}"
          }
        ]
      },
      {
        "title": "AI Service Latency",
        "type": "graph",
        "targets": [
          {
            "expr": "ai_inference_duration_seconds{job=\"youthmind-ai\"}"
          }
        ]
      }
    ]
  }
}
```

#### Log Management
```yaml
# docker-compose.logging.yml
version: '3.8'
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:7.14.0
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    volumes:
      - es_data:/usr/share/elasticsearch/data
      
  kibana:
    image: docker.elastic.co/kibana/kibana:7.14.0
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
    ports:
      - "5601:5601"
      
  logstash:
    image: docker.elastic.co/logstash/logstash:7.14.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
```

---

## Deployment Automation

### **9. CI/CD Pipeline**

#### GitHub Actions Workflow
```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Tests
        run: |
          cd backend && mvn test
          cd ../ai-services && python -m pytest
          
  deploy:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - name: Deploy to Production
        uses: appleboy/ssh-action@v0.1.5
        with:
          host: ${{ secrets.PROD_HOST }}
          username: ${{ secrets.PROD_USER }}
          key: ${{ secrets.PROD_SSH_KEY }}
          script: |
            cd /opt/youthmind
            git pull origin main
            ./deploy.sh
```

#### Blue-Green Deployment Script
```bash
#!/bin/bash
# blue-green-deploy.sh

BLUE_PORT=8080
GREEN_PORT=8081
HEALTH_CHECK_URL="http://localhost:${GREEN_PORT}/actuator/health"

echo "Starting blue-green deployment..."

# Build new version
cd backend
mvn clean package -DskipTests

# Deploy to green environment
java -jar target/youthmind-backend-*.jar --server.port=${GREEN_PORT} &
GREEN_PID=$!

# Health check
sleep 30
if curl -f ${HEALTH_CHECK_URL}; then
    echo "Green environment healthy, switching traffic..."
    
    # Update Nginx to point to green
    sed -i "s/localhost:${BLUE_PORT}/localhost:${GREEN_PORT}/g" /etc/nginx/sites-available/youthmind
    nginx -s reload
    
    # Stop blue environment
    pkill -f "server.port=${BLUE_PORT}"
    
    echo "Deployment successful!"
else
    echo "Green environment unhealthy, rolling back..."
    kill ${GREEN_PID}
    exit 1
fi
```

---

## Maintenance & Operations

### **10. Backup Strategy**

#### Database Backups
```bash
#!/bin/bash
# backup-db.sh

BACKUP_DIR="/opt/youthmind/backups"
DATE=$(date +%Y%m%d_%H%M%S)

# PostgreSQL backup
pg_dump -h localhost -U ${DB_USER} youthmind_prod | gzip > ${BACKUP_DIR}/postgres_${DATE}.sql.gz

# Redis backup
redis-cli --rdb ${BACKUP_DIR}/redis_${DATE}.rdb

# Upload to S3
aws s3 cp ${BACKUP_DIR}/ s3://youthmind-backups/ --recursive --exclude "*" --include "*${DATE}*"

# Cleanup old backups (keep 30 days)
find ${BACKUP_DIR}/ -name "*.gz" -mtime +30 -delete
find ${BACKUP_DIR}/ -name "*.rdb" -mtime +30 -delete
```

#### Automated Backup Schedule
```bash
# Add to crontab
0 2 * * * /opt/youthmind/scripts/backup-db.sh
0 6 * * 0 /opt/youthmind/scripts/backup-full-system.sh
```

### **11. Health Monitoring**

#### Application Health Checks
```bash
#!/bin/bash
# health-check.sh

SERVICES=("api.youthmind.dev" "ai.youthmind.dev")
SLACK_WEBHOOK="${SLACK_WEBHOOK_URL}"

for service in "${SERVICES[@]}"; do
    if ! curl -f https://${service}/health; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"🚨 ${service} is down!\"}" \
            ${SLACK_WEBHOOK}
    fi
done
```

#### Performance Monitoring
```sql
-- Database performance queries
SELECT 
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats
WHERE schemaname = 'public'
ORDER BY n_distinct DESC;

-- Slow query monitoring
SELECT 
    query,
    calls,
    total_time,
    mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

---

## Disaster Recovery

### **12. Recovery Procedures**

#### Database Recovery
```bash
# Restore from backup
gunzip -c /opt/youthmind/backups/postgres_YYYYMMDD_HHMMSS.sql.gz | psql -h localhost -U ${DB_USER} youthmind_prod

# Redis recovery
redis-cli --rdb /opt/youthmind/backups/redis_YYYYMMDD_HHMMSS.rdb
```

#### Complete System Recovery
```bash
#!/bin/bash
# disaster-recovery.sh

echo "Starting disaster recovery process..."

# 1. Restore database
./restore-database.sh

# 2. Redeploy applications
./deploy.sh

# 3. Verify all services
./health-check.sh

# 4. Update DNS if needed
./update-dns.sh

echo "Recovery complete. Verify all systems manually."
```

---

## Security Compliance

### **13. Data Protection**

#### GDPR Compliance
- Anonymous user system (no personal data collection)
- Data retention policies (automatic cleanup after 2 years)
- Right to deletion (complete post removal)
- Encryption at rest and in transit

#### Security Auditing
```bash
# Regular security scans
nmap -sS -O localhost
nikto -h https://api.youthmind.dev
sqlmap -u "https://api.youthmind.dev/api/v1/posts" --batch --banner
```

---

*This deployment guide ensures a robust, secure, and scalable production environment for YouthMind's mental health platform.* 
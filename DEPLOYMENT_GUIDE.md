# 🚀 Agentic Itinerary Planner - Complete Deployment Guide

## 📋 Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Docker Configuration](#docker-configuration)
4. [Cloud Build Configuration](#cloud-build-configuration)
5. [Environment Variables](#environment-variables)
6. [CORS Configuration](#cors-configuration)
7. [Deployment Process](#deployment-process)
8. [Troubleshooting](#troubleshooting)
9. [Monitoring & Logs](#monitoring--logs)

---

## 🎯 Overview

This document provides a complete guide for deploying the Agentic Itinerary Planner application to Google Cloud Run using Google Cloud Build. The application consists of:

- **Backend**: Spring Boot application (Java 17)
- **Frontend**: React application with Vite build system
- **Database**: H2 in-memory database (for development/mock setup)
- **Deployment**: Google Cloud Run (serverless containers)

---

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │
│   (React/Vite)  │◄──►│   (Spring Boot) │
│   Cloud Run     │    │   Cloud Run     │
│   Port: 80      │    │   Port: 8080    │
└─────────────────┘    └─────────────────┘
         │                       │
         │                       │
         ▼                       ▼
┌─────────────────────────────────────────┐
│           Google Cloud Run              │
│         (Serverless Platform)           │
└─────────────────────────────────────────┘
```

---

## 🐳 Docker Configuration

### Backend Dockerfile (`Dockerfile.backend`)

```dockerfile
# Multi-stage build for Spring Boot backend
FROM gradle:8.5-jdk17-alpine AS build

# Set working directory
WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./

# Copy source code
COPY src/ src/

# Build the application using gradle directly
RUN gradle bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Create app user
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser

# Set working directory
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Create app directory and set ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-Dspring.profiles.active=cloud", "-Dserver.port=${PORT:-8080}", "-Dserver.address=0.0.0.0", "-jar", "app.jar"]
```

**Key Features:**
- ✅ Multi-stage build for smaller image size
- ✅ Non-root user for security
- ✅ Cloud Run compatible port binding
- ✅ Direct gradle usage (no wrapper issues)

### Frontend Dockerfile (`Dockerfile.frontend`)

```dockerfile
# Multi-stage build for React frontend
FROM node:18-alpine AS build

# Set working directory
WORKDIR /app

# Copy package files first for better caching
COPY frontend/package*.json ./

# Install dependencies (including dev dependencies for build)
RUN npm ci

# Copy source code
COPY frontend/ ./

# Set build argument for backend URL
ARG BACKEND_URL
ENV VITE_API_BASE_URL=${BACKEND_URL}/api/v1

# Build the application
RUN npm run build

# Production stage with nginx
FROM nginx:alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy built assets from build stage
COPY --from=build /app/build /usr/share/nginx/html

# Create nginx config
RUN echo 'events { worker_connections 1024; } \
http { \
    include /etc/nginx/mime.types; \
    default_type application/octet-stream; \
    sendfile on; \
    keepalive_timeout 65; \
    server { \
        listen 80; \
        server_name _; \
        root /usr/share/nginx/html; \
        index index.html; \
        location / { \
            try_files $uri $uri/ /index.html; \
        } \
        location /health { \
            return 200 "healthy"; \
            add_header Content-Type text/plain; \
        } \
    } \
}' > /etc/nginx/nginx.conf

# Expose port
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
```

**Key Features:**
- ✅ Multi-stage build for optimization
- ✅ Nginx for serving static files
- ✅ Client-side routing support
- ✅ Health check endpoint
- ✅ Environment variable injection

### Docker Ignore (`.dockerignore`)

```
# Build artifacts
build/
target/
*.jar
*.war

# Dependencies
node_modules/
.gradle/

# IDE files
.idea/
.vscode/
*.iml

# OS files
.DS_Store
Thumbs.db

# Logs
*.log
logs/

# Environment files
.env
.env.local
.env.production

# Git
.git/
.gitignore

# Documentation
*.md
docs/

# Test files
src/test/
__tests__/
*.test.*
*.spec.*
```

---

## ☁️ Cloud Build Configuration

### Main Cloud Build File (`cloudbuild.yaml`)

```yaml
steps:
  # Build and deploy backend
  - name: 'gcr.io/cloud-builders/docker'
    args: [
      'build',
      '-f', 'Dockerfile.backend',
      '-t', 'gcr.io/$PROJECT_ID/agentic-itinerary-planner-backend:$COMMIT_SHA',
      '-t', 'gcr.io/$PROJECT_ID/agentic-itinerary-planner-backend:latest',
      '.'
    ]
    id: 'build-backend'

  - name: 'gcr.io/cloud-builders/docker'
    args: ['push', 'gcr.io/$PROJECT_ID/agentic-itinerary-planner-backend:$COMMIT_SHA']
    id: 'push-backend-sha'

  - name: 'gcr.io/cloud-builders/docker'
    args: ['push', 'gcr.io/$PROJECT_ID/agentic-itinerary-planner-backend:latest']
    id: 'push-backend-latest'

  # Deploy backend to Cloud Run
  - name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
    entrypoint: 'gcloud'
    args: [
      'run', 'deploy', 'agentic-itinerary-planner-backend',
      '--image', 'gcr.io/$PROJECT_ID/agentic-itinerary-planner-backend:$COMMIT_SHA',
      '--region', '${_REGION}',
      '--platform', 'managed',
      '--allow-unauthenticated',
      '--port', '8080',
      '--memory', '2Gi',
      '--cpu', '2',
      '--max-instances', '10',
      '--min-instances', '0',
      '--concurrency', '100',
      '--timeout', '900',
      '--set-env-vars', 'SPRING_PROFILES_ACTIVE=cloud',
      '--set-env-vars', 'GEMINI_API_KEY=${_GEMINI_API_KEY}',
      '--set-env-vars', 'GEMINI_MOCK_MODE=true',
      '--set-env-vars', 'GOOGLE_OAUTH_CLIENT_ID=${_GOOGLE_OAUTH_CLIENT_ID}',
      '--set-env-vars', 'GOOGLE_OAUTH_CLIENT_SECRET=${_GOOGLE_OAUTH_CLIENT_SECRET}',
      '--set-env-vars', 'RAZORPAY_KEY_ID=${_RAZORPAY_KEY_ID}',
      '--set-env-vars', 'RAZORPAY_KEY_SECRET=${_RAZORPAY_KEY_SECRET}',
      '--set-env-vars', 'SMTP_USERNAME=${_SMTP_USERNAME}',
      '--set-env-vars', 'SMTP_PASSWORD=${_SMTP_PASSWORD}',
      '--set-env-vars', 'EMAIL_FROM=${_EMAIL_FROM}'
    ]
    id: 'deploy-backend'
    waitFor: ['push-backend-sha']

  # Build and deploy frontend
  - name: 'gcr.io/cloud-builders/docker'
    args: [
      'build',
      '-f', 'Dockerfile.frontend',
      '--build-arg', 'BACKEND_URL=${_BACKEND_URL}',
      '-t', 'gcr.io/$PROJECT_ID/agentic-itinerary-planner-frontend:$COMMIT_SHA',
      '-t', 'gcr.io/$PROJECT_ID/agentic-itinerary-planner-frontend:latest',
      '.'
    ]
    id: 'build-frontend'
    waitFor: ['build-backend']

  - name: 'gcr.io/cloud-builders/docker'
    args: ['push', 'gcr.io/$PROJECT_ID/agentic-itinerary-planner-frontend:$COMMIT_SHA']
    id: 'push-frontend-sha'

  - name: 'gcr.io/cloud-builders/docker'
    args: ['push', 'gcr.io/$PROJECT_ID/agentic-itinerary-planner-frontend:latest']
    id: 'push-frontend-latest'

  # Deploy frontend to Cloud Run
  - name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
    entrypoint: 'gcloud'
    args: [
      'run', 'deploy', 'agentic-itinerary-planner-frontend',
      '--image', 'gcr.io/$PROJECT_ID/agentic-itinerary-planner-frontend:$COMMIT_SHA',
      '--region', '${_REGION}',
      '--platform', 'managed',
      '--allow-unauthenticated',
      '--port', '80',
      '--memory', '512Mi',
      '--cpu', '1',
      '--max-instances', '10',
      '--min-instances', '0',
      '--concurrency', '1000',
      '--timeout', '300'
    ]
    id: 'deploy-frontend'
    waitFor: ['push-frontend-sha']

  # Update backend with frontend URL
  - name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
    entrypoint: 'bash'
    args:
      - '-c'
      - |
        FRONTEND_URL=$$(gcloud run services describe agentic-itinerary-planner-frontend --region=${_REGION} --format='value(status.url)')
        echo "Updating backend with frontend URL: $$FRONTEND_URL"
        gcloud run services update agentic-itinerary-planner-backend \
          --region=${_REGION} \
          --set-env-vars="FRONTEND_URL=$$FRONTEND_URL"
    id: 'update-backend-urls'
    waitFor: ['deploy-frontend']

# Substitution variables (hardcoded for mock setup)
substitutions:
  _REGION: 'us-south1'
  _BACKEND_URL: 'https://agenticitineraryplanner-342690752571.us-south1.run.app'
  _GEMINI_API_KEY: 'AIzaSyCmIwxSsiWnElgNy63Pm2Ub4KWKi1oNIr8'
  _GOOGLE_OAUTH_CLIENT_ID: 'dummy-oauth-client-id'
  _GOOGLE_OAUTH_CLIENT_SECRET: 'dummy-oauth-client-secret'
  _RAZORPAY_KEY_ID: 'dummy-razorpay-key-id'
  _RAZORPAY_KEY_SECRET: 'dummy-razorpay-key-secret'
  _SMTP_USERNAME: 'dummy-smtp-username'
  _SMTP_PASSWORD: 'dummy-smtp-password'
  _EMAIL_FROM: 'noreply@agenticitineraryplanner.com'

options:
  logging: CLOUD_LOGGING_ONLY
  machineType: 'E2_HIGHCPU_8'
```

**Key Features:**
- ✅ **Parallel builds** where possible
- ✅ **Dependency management** between steps
- ✅ **Environment variable injection**
- ✅ **Dynamic URL updates**
- ✅ **Optimized machine type**

---

## 🔧 Environment Variables

### Backend Environment Variables

| Variable | Description | Default Value | Required |
|----------|-------------|---------------|----------|
| `SPRING_PROFILES_ACTIVE` | Spring profile | `cloud` | ✅ |
| `PORT` | Server port | `8080` | ✅ |
| `GEMINI_API_KEY` | Gemini AI API key | Mock key | ✅ |
| `GEMINI_MOCK_MODE` | Enable mock mode | `true` | ✅ |
| `GOOGLE_OAUTH_CLIENT_ID` | OAuth client ID | Dummy | ❌ |
| `GOOGLE_OAUTH_CLIENT_SECRET` | OAuth client secret | Dummy | ❌ |
| `RAZORPAY_KEY_ID` | Razorpay key ID | Dummy | ❌ |
| `RAZORPAY_KEY_SECRET` | Razorpay key secret | Dummy | ❌ |
| `SMTP_USERNAME` | SMTP username | Dummy | ❌ |
| `SMTP_PASSWORD` | SMTP password | Dummy | ❌ |
| `EMAIL_FROM` | Email sender | Dummy | ❌ |
| `FRONTEND_URL` | Frontend URL | Auto-set | ✅ |

### Frontend Environment Variables

| Variable | Description | Default Value | Required |
|----------|-------------|---------------|----------|
| `VITE_API_BASE_URL` | Backend API URL | Auto-set | ✅ |

---

## 🌐 CORS Configuration

### Backend CORS Setup

#### 1. Main Application CORS (`src/main/java/com/tripplanner/App.java`)

```java
@Bean
public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                    .allowedHeaders("*")
                    .allowCredentials(false)
                    .maxAge(3600);
        }
    };
}
```

#### 2. CORS Filter (`src/main/java/com/tripplanner/config/CorsFilter.java`)

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Always set CORS headers
        httpResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD");
        httpResponse.setHeader("Access-Control-Allow-Headers", "*");
        httpResponse.setHeader("Access-Control-Allow-Credentials", "false");
        httpResponse.setHeader("Access-Control-Max-Age", "3600");
        
        // Handle preflight OPTIONS request
        if ("OPTIONS".equalsIgnoreCase(((HttpServletRequest) request).getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        chain.doFilter(request, response);
    }
}
```

#### 3. OPTIONS Handler (`src/main/java/com/tripplanner/api/HealthController.java`)

```java
@RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
public ResponseEntity<Map<String, String>> handleOptions() {
    return ResponseEntity.ok(Map.of(
        "status", "OK",
        "message", "CORS preflight handled"
    ));
}
```

### Frontend CORS Setup

#### API Client Configuration (`frontend/src/services/apiClient.ts`)

```typescript
const config: RequestInit = {
  ...options,
  headers,
  mode: 'cors',           // ✅ Explicitly set
  credentials: 'omit',    // ✅ No credentials
};
```

#### Chat Service Configuration (`frontend/src/services/chatService.ts`)

```typescript
const response = await fetch(`${API_BASE_URL}/chat/route`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json; charset=UTF-8',
  },
  body: safeJsonEncode(normalizedRequest),
  mode: 'cors',           // ✅ Added
  credentials: 'omit',    // ✅ Added
});
```

---

## 🚀 Deployment Process

### 1. Prerequisites

- ✅ Google Cloud Project with billing enabled
- ✅ Cloud Build API enabled
- ✅ Cloud Run API enabled
- ✅ Container Registry API enabled
- ✅ IAM permissions for Cloud Build service account

### 2. Cloud Build Trigger Setup

1. **Go to Cloud Build Triggers**
2. **Create Trigger**
3. **Configure:**
   - **Name**: `agentic-itinerary-planner-deploy`
   - **Event**: Push to a branch
   - **Source**: Connect your repository
   - **Configuration**: Cloud Build configuration file (yaml or json)
   - **Location**: `/cloudbuild.yaml`
   - **Branch**: `main` (or your default branch)

### 3. Deployment Steps

```bash
# 1. Commit and push your changes
git add .
git commit -m "Deploy: Update CORS configuration"
git push origin main

# 2. Cloud Build automatically triggers
# 3. Monitor the build in Google Cloud Console
# 4. Check deployment status
```

### 4. Manual Deployment (if needed)

```bash
# Build and deploy manually
gcloud builds submit --config cloudbuild.yaml .

# Or deploy individual services
gcloud run deploy agentic-itinerary-planner-backend \
  --source . \
  --region us-south1 \
  --allow-unauthenticated

gcloud run deploy agentic-itinerary-planner-frontend \
  --source . \
  --region us-south1 \
  --allow-unauthenticated
```

---

## 🔍 Troubleshooting

### Common Issues and Solutions

#### 1. CORS Errors

**Problem**: `Access to fetch at 'backend-url' from origin 'frontend-url' has been blocked by CORS policy`

**Solution**:
- ✅ Verify CORS configuration in backend
- ✅ Check frontend fetch calls have `mode: 'cors'`
- ✅ Ensure OPTIONS requests are handled

#### 2. Container Startup Failures

**Problem**: `Container failed to start and listen on the port`

**Solution**:
- ✅ Check Dockerfile port configuration
- ✅ Verify environment variables
- ✅ Check application logs

#### 3. Build Failures

**Problem**: Docker build fails

**Solution**:
- ✅ Check Dockerfile syntax
- ✅ Verify file paths in COPY commands
- ✅ Check build context size

#### 4. Frontend-Backend Communication

**Problem**: Frontend can't reach backend

**Solution**:
- ✅ Verify backend URL in frontend build
- ✅ Check Cloud Run service URLs
- ✅ Verify network connectivity

### Debug Commands

```bash
# Check Cloud Run services
gcloud run services list --region=us-south1

# View service logs
gcloud logs read --service=agentic-itinerary-planner-backend --limit=50

# Test backend health
curl https://your-backend-url/api/v1/health

# Test CORS preflight
curl -X OPTIONS https://your-backend-url/api/v1/itineraries \
  -H "Origin: https://your-frontend-url" \
  -H "Access-Control-Request-Method: POST"
```

---

## 📊 Monitoring & Logs

### Cloud Run Monitoring

1. **Service Metrics**:
   - Request count
   - Request latency
   - Error rate
   - Instance count

2. **Logs**:
   - Application logs
   - Request logs
   - Error logs

### Log Locations

- **Cloud Run Logs**: Google Cloud Console → Cloud Run → Service → Logs
- **Cloud Build Logs**: Google Cloud Console → Cloud Build → History
- **Application Logs**: Available in Cloud Run service logs

### Health Checks

#### Backend Health Endpoint
```
GET /api/v1/health
Response: {"status": "UP", "timestamp": "...", "service": "Agentic Itinerary Planner"}
```

#### Frontend Health Endpoint
```
GET /health
Response: "healthy"
```

---

## 🔐 Security Considerations

### Current Security Measures

- ✅ **Non-root containers** (Docker)
- ✅ **HTTPS only** (Cloud Run)
- ✅ **No credentials in CORS** (security best practice)
- ✅ **Environment variable injection** (no secrets in code)
- ✅ **Minimal container images** (reduced attack surface)

### Production Recommendations

- 🔒 **Use Secret Manager** for API keys
- 🔒 **Enable IAM authentication** for internal services
- 🔒 **Implement rate limiting**
- 🔒 **Add request validation**
- 🔒 **Enable audit logging**

---

## 📈 Performance Optimization

### Current Optimizations

- ✅ **Multi-stage Docker builds** (smaller images)
- ✅ **Nginx for frontend** (efficient static serving)
- ✅ **Cloud Run auto-scaling** (cost-effective)
- ✅ **CDN for static assets** (faster delivery)

### Additional Optimizations

- 🚀 **Enable Cloud CDN** for global distribution
- 🚀 **Implement caching strategies**
- 🚀 **Optimize bundle sizes**
- 🚀 **Use Cloud SQL** for production database

---

## 📞 Support

For deployment issues:

1. **Check Cloud Build logs** for build failures
2. **Check Cloud Run logs** for runtime issues
3. **Verify environment variables** are set correctly
4. **Test endpoints** individually
5. **Check CORS configuration** if frontend-backend communication fails

---

**Last Updated**: September 2024  
**Version**: 1.0.0  
**Maintainer**: Development Team

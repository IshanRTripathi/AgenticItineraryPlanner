# Deployment Guide

## 🚀 Production Deployment

### Prerequisites
- Node.js 18+ installed
- Access to hosting platform (Vercel, Netlify, AWS, etc.)
- Backend API URL

### Environment Setup

1. **Create production environment file**
```bash
cp .env.example .env.production
```

2. **Configure environment variables**
```env
VITE_API_BASE_URL=https://api.yourdomain.com/api
VITE_ENABLE_ANALYTICS=true
VITE_ENABLE_MOCK_DATA=false
VITE_ENV=production
```

### Build Process

```bash
# Install dependencies
npm ci

# Run production build
npm run build

# Test production build locally
npm run preview
```

The build output will be in the `dist/` directory.

---

## 📦 Deployment Options

### Option 1: Vercel (Recommended)

**Automatic Deployment:**
1. Push code to GitHub/GitLab/Bitbucket
2. Import project in Vercel dashboard
3. Configure environment variables
4. Deploy automatically on push

**Manual Deployment:**
```bash
npm install -g vercel
vercel --prod
```

**Configuration:**
- Build Command: `npm run build`
- Output Directory: `dist`
- Install Command: `npm install`

### Option 2: Netlify

**Automatic Deployment:**
1. Connect repository to Netlify
2. Configure build settings:
   - Build command: `npm run build`
   - Publish directory: `dist`
3. Add environment variables
4. Deploy

**Manual Deployment:**
```bash
npm install -g netlify-cli
netlify deploy --prod --dir=dist
```

### Option 3: AWS S3 + CloudFront

**Steps:**
1. Build the project: `npm run build`
2. Create S3 bucket with static website hosting
3. Upload `dist/` contents to S3
4. Create CloudFront distribution
5. Configure custom domain (optional)

**AWS CLI Commands:**
```bash
# Build
npm run build

# Sync to S3
aws s3 sync dist/ s3://your-bucket-name --delete

# Invalidate CloudFront cache
aws cloudfront create-invalidation --distribution-id YOUR_DIST_ID --paths "/*"
```

### Option 4: Docker

**Dockerfile:**
```dockerfile
# Build stage
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Production stage
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**nginx.conf:**
```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

**Build and run:**
```bash
docker build -t easytrip-frontend .
docker run -p 80:80 easytrip-frontend
```

---

## 🔧 Configuration

### API Integration

Update `VITE_API_BASE_URL` to point to your backend:
- Development: `http://localhost:8080/api`
- Staging: `https://staging-api.yourdomain.com/api`
- Production: `https://api.yourdomain.com/api`

### WebSocket Configuration

The WebSocket URL is automatically derived from the API URL:
- HTTP → WS
- HTTPS → WSS

Example: `https://api.yourdomain.com/api` → `wss://api.yourdomain.com/api`

### Feature Flags

```env
# Enable/disable analytics
VITE_ENABLE_ANALYTICS=true

# Use mock data (development only)
VITE_ENABLE_MOCK_DATA=false
```

---

## 🔒 Security Checklist

- [ ] Environment variables configured correctly
- [ ] API endpoints use HTTPS in production
- [ ] CORS configured on backend
- [ ] Content Security Policy headers set
- [ ] Rate limiting enabled on API
- [ ] Authentication tokens stored securely
- [ ] No sensitive data in client-side code
- [ ] Dependencies updated and audited

---

## 📊 Performance Optimization

### Pre-deployment Checklist

- [ ] Run production build: `npm run build`
- [ ] Check bundle size (should be ~280KB)
- [ ] Test on slow 3G network
- [ ] Verify lazy loading works
- [ ] Check Lighthouse score (target: 90+)
- [ ] Test on mobile devices
- [ ] Verify all images are optimized

### CDN Configuration

For optimal performance, serve static assets via CDN:
1. Upload `dist/assets/` to CDN
2. Update asset URLs in build config
3. Set appropriate cache headers

---

## 🧪 Pre-deployment Testing

```bash
# Build for production
npm run build

# Run production preview
npm run preview

# Test checklist:
# - All routes work correctly
# - Forms submit properly
# - Images load correctly
# - Animations are smooth
# - Mobile menu works
# - Error boundaries catch errors
# - Loading states display
```

---

## 📈 Monitoring

### Recommended Tools

- **Error Tracking**: Sentry, Rollbar
- **Analytics**: Google Analytics, Mixpanel
- **Performance**: Lighthouse CI, WebPageTest
- **Uptime**: Pingdom, UptimeRobot

### Health Check Endpoint

Create a simple health check:
```typescript
// Add to your backend
GET /api/health
Response: { status: "ok", timestamp: "..." }
```

---

## 🔄 CI/CD Pipeline

### GitHub Actions Example

```yaml
name: Deploy to Production

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          
      - name: Install dependencies
        run: npm ci
        
      - name: Build
        run: npm run build
        env:
          VITE_API_BASE_URL: ${{ secrets.API_BASE_URL }}
          
      - name: Deploy to Vercel
        uses: amondnet/vercel-action@v20
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.ORG_ID }}
          vercel-project-id: ${{ secrets.PROJECT_ID }}
          vercel-args: '--prod'
```

---

## 🆘 Troubleshooting

### Build Fails

```bash
# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm install
npm run build
```

### Routes Don't Work After Deployment

Configure your hosting platform for SPA routing:
- **Vercel**: Automatic
- **Netlify**: Add `_redirects` file: `/* /index.html 200`
- **S3**: Configure error document to `index.html`

### Environment Variables Not Working

- Ensure variables start with `VITE_`
- Rebuild after changing variables
- Check variable names match exactly

### WebSocket Connection Fails

- Verify WebSocket endpoint is accessible
- Check CORS configuration
- Ensure WSS is used with HTTPS

---

## 📞 Support

For deployment issues:
1. Check build logs
2. Verify environment variables
3. Test locally with `npm run preview`
4. Review hosting platform documentation
5. Check backend API connectivity

---

**Last Updated**: January 2025

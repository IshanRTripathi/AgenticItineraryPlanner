# Agentic Itinerary Planner

A comprehensive travel planning platform powered by AI agents that creates personalized itineraries with real-time updates, booking capabilities, and sharing features.

## 🏗️ Architecture

**Backend**: Java 17 + Spring Boot 3.x + Firestore + OpenRouter/Gemini AI + Razorpay  
**Frontend**: React + TypeScript + Vite + Radix UI + Tailwind CSS  
**Real-time**: WebSocket + Server-Sent Events (SSE) for agent progress updates  
**Database**: Google Cloud Firestore  
**Payments**: Razorpay integration  
**AI**: OpenRouter (primary) + Google Gemini (fallback) with resilient client  

## 🚀 Features

### Core Features
- **AI-Powered Itinerary Generation**: Multi-agent system creates personalized travel plans
- **Real-time Agent Updates**: Live progress tracking via SSE
- **Google Authentication**: Secure login with Google OAuth
- **Payment Integration**: Razorpay for booking payments
- **PDF Export**: Generate beautiful itinerary PDFs
- **Email Sharing**: Share itineraries via email
- **Public Sharing**: Share itineraries with public links

### AI Agents
- **Planner Agent**: Main orchestrator for itinerary generation using LLM
- **Editor Agent**: Handles user-driven modifications and summarization
- **Enrichment Agent**: Validates and enriches itineraries with place details
- **Booking Agent**: Manages booking operations across multiple providers
- **Places Agent**: Discovers locations and local insights

### Tools
- **Packing List Generator**: AI-generated packing recommendations
- **Photo Spots**: Discover best photography locations
- **Must-Try Foods**: Local cuisine recommendations
- **Cost Estimator**: Detailed budget breakdowns

## 🛠️ Setup & Installation

### Prerequisites
- Java 17 or higher
- Node.js 18+ and npm
- Google Cloud Project with Firestore enabled
- Razorpay account (for payments)
- Google AI Studio API key (for Gemini)

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd AgenticItineraryPlanner
   ```

2. **Configure environment variables**
   Create a `.env` file or set environment variables:
   ```bash
   # Google Cloud & AI
   GOOGLE_OAUTH_CLIENT_ID=your-google-oauth-client-id
   GOOGLE_OAUTH_CLIENT_SECRET=your-google-oauth-client-secret
   GEMINI_API_KEY=your-gemini-api-key
   OPENROUTER_API_KEY=your-openrouter-api-key
   AI_PROVIDER=openrouter
   GCP_PROJECT_ID=your-gcp-project-id
   GOOGLE_APPLICATION_CREDENTIALS=path/to/service-account.json

   # Razorpay
   RAZORPAY_KEY_ID=your-razorpay-key-id
   RAZORPAY_KEY_SECRET=your-razorpay-key-secret
   RAZORPAY_WEBHOOK_SECRET=your-webhook-secret

   # Email (SMTP)
   SMTP_HOST=smtp.gmail.com
   SMTP_PORT=587
   SMTP_USERNAME=your-email@gmail.com
   SMTP_PASSWORD=your-app-password
   EMAIL_FROM=noreply@yourdomain.com

   # Application
   APP_BASE_URL=http://localhost:8080
   ```

3. **Build and run the backend**
   ```bash
   ./gradlew build
   ./gradlew bootRun
   ```

   The backend will start on `http://localhost:8080/api/v1`

### Frontend Setup

1. **Navigate to frontend directory**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Create environment file**
   Create `frontend/.env`:
   ```bash
   VITE_API_BASE_URL=http://localhost:8080/api/v1
   VITE_GOOGLE_CLIENT_ID=your-google-oauth-client-id
   ```

4. **Start the frontend**
   ```bash
   npm run dev
   ```

   The frontend will start on `http://localhost:3000`

## 📡 API Endpoints

### Itineraries
- `POST /itineraries` - Create new itinerary
- `GET /itineraries/{id}` - Get itinerary
- `GET /itineraries/{id}/public` - Get public itinerary
- `POST /itineraries/{id}:revise` - Revise itinerary
- `POST /itineraries/{id}:extend` - Extend itinerary
- `POST /itineraries/{id}:save` - Save itinerary

### Real-time Updates
- `GET /agents/stream?itineraryId={id}` - SSE stream for agent events

### Tools
- `POST /packing-list` - Generate packing list
- `POST /photo-spots` - Get photo spots
- `POST /must-try-foods` - Get food recommendations
- `POST /cost-estimator` - Generate cost estimate

### Booking & Payments
- `POST /payments/razorpay/order` - Create payment order
- `POST /payments/razorpay/webhook` - Payment webhook
- `POST /providers/{vertical}/{provider}:book` - Execute booking
- `GET /bookings/{id}` - Get booking details

### Export
- `GET /itineraries/{id}/pdf` - Generate PDF
- `POST /email/send` - Send email

### Authentication
- `POST /auth/google` - Google OAuth authentication

## 🧪 Testing

Run backend tests:
```bash
./gradlew test
```

Run frontend tests:
```bash
cd frontend
npm test
```

## 🏗️ Development

### Backend Development
- Follow the package structure: `com.tripplanner.{service,data,agents,controller,config,dto,exception}`
- All services are conditionally loaded based on available dependencies
- Use the `@ConditionalOnBean` annotations for optional features
- Agent events are broadcasted via WebSocket and SSE for real-time updates
- New architecture includes canonical place registry, durable task system, and conflict resolution

### Frontend Development
- React components in `frontend/src/components/`
- API client in `frontend/src/services/apiClient.ts`
- Type definitions in `frontend/src/types/`
- UI components in `frontend/src/components/ui/`

### Adding New Agents
1. Extend `BaseAgent` class
2. Implement `executeInternal` method
3. Define request/response DTOs
4. Add to `AgentOrchestrator`
5. Update frontend `AGENT_TASKS`

## 🚀 Deployment

### Backend (Google Cloud Run)
1. Build container:
   ```bash
   ./gradlew bootBuildImage
   ```

2. Deploy to Cloud Run:
   ```bash
   gcloud run deploy agentic-itinerary-planner \
     --image gcr.io/PROJECT_ID/agentic-itinerary-planner \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated
   ```

### Frontend (Vercel/Netlify)
1. Build frontend:
   ```bash
   cd frontend
   npm run build
   ```

2. Deploy the `build/` directory to your hosting platform

## 📊 Monitoring

- Health checks: `/actuator/health`
- Metrics: `/actuator/metrics`
- Application info: `/actuator/info`

## 🔧 Configuration

All configuration is externalized via environment variables. See `application.yml` for all available options.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License.

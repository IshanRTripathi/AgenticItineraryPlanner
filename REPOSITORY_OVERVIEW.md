# Agentic Itinerary Planner - Comprehensive Repository Overview

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Backend Analysis](#backend-analysis)
4. [Frontend Analysis](#frontend-analysis)
5. [Data Models & Types](#data-models--types)
6. [API Documentation](#api-documentation)
7. [Features & Functionality](#features--functionality)
8. [Development Setup](#development-setup)
9. [Deployment](#deployment)
10. [Current Status & Roadmap](#current-status--roadmap)

---

## Project Overview

**Agentic Itinerary Planner** is a comprehensive AI-powered travel planning platform that creates personalized itineraries using multiple AI agents working in parallel. The system combines modern web technologies with intelligent automation to deliver a seamless travel planning experience.

### Key Characteristics
- **AI-Driven**: Uses Google Gemini AI for intelligent itinerary generation
- **Multi-Agent System**: Orchestrates multiple specialized agents for different aspects of travel planning
- **Real-Time Updates**: Server-Sent Events (SSE) for live progress tracking
- **Full-Stack**: Complete backend and frontend implementation
- **Modern Tech Stack**: Java 17 + Spring Boot 3.x + React + TypeScript
- **Production-Ready**: Includes authentication, payments, PDF generation, and email sharing

---

## Architecture

### High-Level Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Backend       │    │   External      │
│   (React/TS)    │◄──►│   (Spring Boot) │◄──►│   Services      │
│                 │    │                 │    │                 │
│ • React Router  │    │ • REST API      │    │ • Google Gemini │
│ • Zustand Store │    │ • SSE Streams   │    │ • Razorpay      │
│ • Radix UI      │    │ • H2 Database   │    │ • SMTP Email    │
│ • Tailwind CSS  │    │ • JPA/Hibernate │    │ • PDF Gen       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Technology Stack

#### Backend
- **Framework**: Spring Boot 3.5.5
- **Language**: Java 17
- **Database**: H2 (in-memory, with JPA/Hibernate)
- **AI Integration**: Google Gemini 1.5 Pro
- **Payments**: Razorpay Java SDK
- **PDF Generation**: OpenHTMLtoPDF
- **Email**: Spring Boot Mail
- **Security**: Spring Security (currently disabled for development)
- **Build Tool**: Gradle

#### Frontend
- **Framework**: React 18.3.1
- **Language**: TypeScript
- **Build Tool**: Vite 6.3.5
- **UI Library**: Radix UI components
- **Styling**: Tailwind CSS
- **State Management**: Zustand
- **Data Fetching**: TanStack React Query
- **Routing**: React Router DOM
- **Internationalization**: i18next
- **Icons**: Lucide React

---

## Backend Analysis

### Project Structure
```
src/main/java/com/tripplanner/
├── api/                    # REST Controllers
│   ├── dto/               # Data Transfer Objects
│   ├── AgentController.java
│   ├── ItinerariesController.java
│   ├── BookingController.java
│   └── ToolsController.java
├── config/                # Configuration classes
├── data/                  # Data layer
│   ├── entity/           # JPA entities
│   └── repo/             # Repository interfaces
├── providers/             # External service providers
├── service/               # Business logic
│   ├── agents/           # AI agent implementations
│   ├── AgentEventBus.java
│   ├── GeminiClient.java
│   └── ItineraryService.java
└── App.java              # Main application class
```

### Core Components

#### 1. Controllers
- **ItinerariesController**: CRUD operations for itineraries
- **AgentController**: SSE streams for real-time agent progress
- **BookingController**: Payment and booking management
- **ToolsController**: Travel planning tools (packing lists, photo spots, etc.)

#### 2. AI Agent System
- **AgentOrchestrator**: Coordinates multiple agents
- **PlannerAgent**: Main itinerary generation agent
- **PlacesAgent**: Location and attraction discovery
- **BaseAgent**: Abstract base class for all agents

#### 3. Data Layer
- **Itinerary Entity**: Complex JPA entity with nested classes
- **Repository Pattern**: Spring Data JPA repositories
- **DTOs**: Request/Response data transfer objects

#### 4. Services
- **ItineraryService**: Core business logic
- **GeminiClient**: AI integration service
- **RazorpayService**: Payment processing
- **PdfService**: PDF generation
- **AgentEventBus**: SSE event management

### Key Features

#### Real-Time Agent Updates
- Server-Sent Events (SSE) for live progress tracking
- Agent status broadcasting to connected clients
- Progress percentage and status updates

#### AI Integration
- Google Gemini 1.5 Pro for intelligent content generation
- Configurable AI parameters (temperature, max tokens)
- Mock mode for development/testing

#### Database Design
- Complex entity relationships with nested classes
- Support for activities, accommodations, transportation, meals
- Location and pricing information
- User and booking management

---

## Frontend Analysis

### Project Structure
```
frontend/src/
├── components/            # React components
│   ├── agents/           # Agent progress components
│   ├── booking/          # Payment and booking UI
│   ├── travel-planner/   # Main planner interface
│   ├── trip-wizard/      # Trip creation wizard
│   ├── ui/               # Reusable UI components
│   └── shared/           # Shared components
├── services/             # API and data services
├── state/                # State management
│   ├── store/           # Zustand store
│   ├── query/           # React Query hooks
│   └── slices/          # Store slices
├── types/                # TypeScript type definitions
├── utils/                # Utility functions
└── i18n/                 # Internationalization
```

### Core Components

#### 1. Application Flow
- **LandingPage**: Marketing and entry point
- **SimplifiedTripWizard**: Trip creation with preferences
- **AgentProgressModal**: Real-time agent progress display
- **TravelPlanner**: Main itinerary management interface
- **TripDashboard**: User's trip collection

#### 2. State Management
- **Zustand Store**: Global application state
- **React Query**: Server state management
- **Local Storage**: Persistent state with Zustand persist

#### 3. UI Components
- **Radix UI**: Accessible component primitives
- **Tailwind CSS**: Utility-first styling
- **Custom Components**: Travel-specific UI elements

#### 4. Data Flow
- **API Client**: Centralized HTTP client
- **Data Transformer**: Backend-to-frontend data conversion
- **Type Safety**: Comprehensive TypeScript definitions

### Key Features

#### Trip Creation Wizard
- Multi-step form with validation
- Preference sliders (music mixer style)
- Traveler management
- Budget planning
- Date selection with calendar

#### Real-Time Progress Tracking
- SSE connection management
- Agent status visualization
- Progress bars and status indicators
- Automatic completion detection

#### Travel Planner Interface
- Resizable panels
- Multiple view modes (map, workflow)
- Day-by-day itinerary display
- Component management (activities, hotels, etc.)

---

## Data Models & Types

### Backend Data Models

#### Core Entities
```java
// Main itinerary entity with nested classes
@Entity
public class Itinerary {
    private Long id;
    private String userId;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Party party;                    // Embedded
    private String budgetTier;
    private List<String> interests;
    private List<String> constraints;
    private List<ItineraryDay> days;        // One-to-many
    private String status;
    // ... other fields
}

// Nested entity classes
public static class ItineraryDay {
    private List<Activity> activities;
    private Accommodation accommodation;
    private List<Transportation> transportation;
    private List<Meal> meals;
    // ... other fields
}
```

#### DTOs
- **CreateItineraryReq**: Trip creation request
- **ItineraryDto**: Complete itinerary response
- **PartyDto**: Traveler information
- **ActivityDto, AccommodationDto, etc.**: Component DTOs

### Frontend Data Models

#### Core Types
```typescript
interface TripData {
  id: string;
  startLocation: TripLocation;
  endLocation: TripLocation;
  dates: { start: string; end: string };
  travelers: Traveler[];
  budget: BudgetInfo;
  preferences: TravelPreferences;
  settings: TripSettings;
  itinerary?: TripItinerary;
  status: 'draft' | 'planning' | 'booked' | 'completed';
  // ... other fields
}

interface TripItinerary {
  days: DayPlan[];
  totalCost: number;
  highlights: string[];
  themes: string[];
  // ... other fields
}
```

#### Component Types
- **TripComponent**: Unified component interface
- **DayPlan**: Daily itinerary structure
- **TravelPreferences**: User preference sliders
- **AgentTask**: AI agent definitions

### Data Transformation
- **DataTransformer**: Converts backend DTOs to frontend types
- **Mock Data Generation**: Fallback data for development
- **Type Safety**: Comprehensive TypeScript coverage

---

## API Documentation

### Endpoints Overview

#### Itinerary Management
```
POST   /api/v1/itineraries              # Create itinerary
GET    /api/v1/itineraries              # List all itineraries
GET    /api/v1/itineraries/{id}         # Get specific itinerary
DELETE /api/v1/itineraries/{id}         # Delete itinerary
```

#### Real-Time Updates
```
GET    /api/v1/agents/stream            # SSE stream for agent events
GET    /api/v1/agents/events/{id}       # Alternative SSE endpoint
GET    /api/v1/agents/{id}/status       # Polling-based status check
```

#### Travel Tools
```
POST   /api/v1/packing-list             # Generate packing list
POST   /api/v1/photo-spots              # Get photo spots
POST   /api/v1/must-try-foods           # Get food recommendations
POST   /api/v1/cost-estimator           # Generate cost estimate
```

#### Booking & Payments
```
POST   /api/v1/payments/razorpay/order  # Create payment order
POST   /api/v1/payments/razorpay/webhook # Payment webhook
POST   /api/v1/providers/{v}/{p}:book   # Execute booking
```

#### Health & Testing
```
GET    /api/v1/health                   # Health check
GET    /api/v1/test                     # Test endpoint
GET    /api/v1/ping                     # Ping endpoint
POST   /api/v1/echo                     # Echo endpoint
```

### Request/Response Examples

#### Create Itinerary Request
```json
{
  "destination": "Barcelona, Spain",
  "startDate": "2025-06-01",
  "endDate": "2025-06-04",
  "party": {
    "adults": 2,
    "children": 1,
    "infants": 0,
    "rooms": 1
  },
  "budgetTier": "mid-range",
  "interests": ["culture", "architecture", "food"],
  "constraints": ["familyFriendly", "wheelchairAccessible"],
  "language": "en"
}
```

#### SSE Agent Event
```
event: agent-event
data: {
  "kind": "planner",
  "status": "running",
  "progress": 45,
  "message": "Generating activities...",
  "itineraryId": "3"
}
```

---

## Features & Functionality

### Core Features

#### 1. AI-Powered Itinerary Generation
- **Multi-Agent System**: Specialized agents for different aspects
- **Real-Time Progress**: Live updates via Server-Sent Events
- **Intelligent Planning**: Google Gemini AI integration
- **Personalization**: User preferences and constraints

#### 2. Trip Creation Wizard
- **Interactive Form**: Multi-step trip configuration
- **Preference Sliders**: Music mixer-style preference selection
- **Traveler Management**: Add/edit traveler information
- **Budget Planning**: Visual budget breakdown
- **Date Selection**: Calendar-based date picker

#### 3. Travel Planner Interface
- **Resizable Panels**: Flexible layout management
- **Multiple Views**: Map view and workflow builder
- **Day-by-Day Planning**: Detailed daily itineraries
- **Component Management**: Activities, hotels, transportation
- **Real-Time Editing**: Live itinerary modifications

#### 4. Agent Progress Tracking
- **Live Updates**: Real-time agent status
- **Progress Visualization**: Progress bars and status indicators
- **Agent Details**: Individual agent progress tracking
- **Completion Detection**: Automatic flow progression

#### 5. Booking & Payments
- **Razorpay Integration**: Payment processing
- **Booking Management**: Track and manage bookings
- **Confirmation System**: Booking confirmation flow
- **Cost Estimation**: Detailed cost breakdowns

#### 6. Sharing & Export
- **PDF Generation**: Beautiful itinerary PDFs
- **Email Sharing**: Send itineraries via email
- **Public Sharing**: Share with public links
- **Social Integration**: Share to social platforms

### Advanced Features

#### 1. Internationalization
- **Multi-Language Support**: i18next integration
- **Locale Management**: Language selector component
- **Localized Content**: Translated UI elements

#### 2. State Management
- **Persistent Storage**: LocalStorage integration
- **Server State**: React Query for API data
- **Real-Time Updates**: SSE integration in store

#### 3. Error Handling
- **Error Boundaries**: React error boundaries
- **API Error Handling**: Comprehensive error management
- **Fallback Data**: Mock data for development

#### 4. Development Tools
- **API Testing**: Built-in API test endpoints
- **Debug Functions**: Store debugging utilities
- **Hot Reloading**: Vite development server

---

## Development Setup

### Prerequisites
- Java 17 or higher
- Node.js 18+ and npm
- Google Cloud Project with Gemini API access
- Razorpay account (for payments)
- SMTP email service (for notifications)

### Backend Setup
```bash
# Clone repository
git clone <repository-url>
cd AgenticItineraryPlanner

# Configure environment variables
export GEMINI_API_KEY=your-gemini-api-key
export RAZORPAY_KEY_ID=your-razorpay-key-id
export RAZORPAY_KEY_SECRET=your-razorpay-key-secret
export SMTP_USERNAME=your-email@gmail.com
export SMTP_PASSWORD=your-app-password

# Build and run
./gradlew build
./gradlew bootRun
```

### Frontend Setup
```bash
# Navigate to frontend
cd frontend

# Install dependencies
npm install

# Create environment file
echo "VITE_API_BASE_URL=http://localhost:8080/api/v1" > .env

# Start development server
npm run dev
```

### Configuration
- **Backend**: `src/main/resources/application.yml`
- **Frontend**: `frontend/.env`
- **Database**: H2 in-memory (development)
- **Security**: Disabled for development

---

## Deployment

### Backend Deployment (Google Cloud Run)
```bash
# Build container
./gradlew bootBuildImage

# Deploy to Cloud Run
gcloud run deploy agentic-itinerary-planner \
  --image gcr.io/PROJECT_ID/agentic-itinerary-planner \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

### Frontend Deployment (Vercel/Netlify)
```bash
# Build frontend
cd frontend
npm run build

# Deploy build/ directory to hosting platform
```

### Environment Variables
- **Production**: Set via hosting platform
- **Database**: Configure production database
- **Security**: Enable Spring Security
- **Monitoring**: Configure logging and metrics

---

## Current Status & Roadmap

### Current Implementation Status

#### ✅ Completed Features
- **Backend API**: Complete REST API with all endpoints
- **AI Integration**: Google Gemini integration working
- **Frontend UI**: Complete React application with all screens
- **Real-Time Updates**: SSE implementation functional
- **Data Models**: Comprehensive entity and DTO structure
- **Payment Integration**: Razorpay integration ready
- **PDF Generation**: PDF export functionality
- **Email Sharing**: SMTP email integration

#### 🚧 In Progress
- **Agent Orchestration**: Multi-agent system implementation
- **Database Persistence**: H2 to production database migration
- **Authentication**: Google OAuth integration
- **Production Deployment**: Cloud deployment configuration

#### 📋 Planned Features
- **Mobile App**: React Native mobile application
- **Advanced AI**: More sophisticated agent coordination
- **Social Features**: User profiles and trip sharing
- **Analytics**: Usage analytics and insights
- **Multi-Language**: Full internationalization
- **Offline Support**: Progressive Web App features

### Technical Debt & Improvements

#### Backend
- **Database Migration**: Move from H2 to PostgreSQL/MySQL
- **Security**: Implement proper authentication and authorization
- **Testing**: Add comprehensive unit and integration tests
- **Monitoring**: Add application monitoring and logging
- **Caching**: Implement Redis caching for performance

#### Frontend
- **Performance**: Optimize bundle size and loading
- **Accessibility**: Improve accessibility compliance
- **Testing**: Add unit and integration tests
- **Error Handling**: Enhance error boundaries and recovery
- **Offline Support**: Add service worker for offline functionality

### Roadmap Priorities

#### Phase 1: Core Stability (Current)
- Complete agent orchestration
- Implement authentication
- Deploy to production
- Add comprehensive testing

#### Phase 2: Enhanced Features
- Advanced AI capabilities
- Social features
- Mobile application
- Analytics dashboard

#### Phase 3: Scale & Optimize
- Performance optimization
- Advanced caching
- Multi-region deployment
- Enterprise features

---

## Conclusion

The Agentic Itinerary Planner represents a sophisticated, full-stack travel planning application with modern architecture and comprehensive feature set. The project demonstrates:

- **Advanced AI Integration**: Multi-agent system with real-time updates
- **Modern Tech Stack**: Latest versions of Spring Boot, React, and TypeScript
- **Production-Ready Features**: Payments, PDF generation, email sharing
- **Comprehensive Data Models**: Complex entity relationships and type safety
- **Real-Time Communication**: Server-Sent Events for live updates
- **User Experience**: Intuitive wizard and planner interfaces

The codebase is well-structured, follows modern development practices, and provides a solid foundation for a production travel planning platform. The combination of AI-powered automation with user-friendly interfaces creates a compelling solution for modern travelers.

---

*This document provides a comprehensive overview of the Agentic Itinerary Planner repository as of the current analysis. For specific implementation details, refer to the individual source files and API documentation.*

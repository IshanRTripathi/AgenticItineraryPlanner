# Agentic Itinerary Planner - Project Blueprint & File Structure

## 📁 Project Overview
**Full-Stack AI-Powered Travel Planning Platform**
- **Backend**: Java 17 + Spring Boot 3.x + Firestore + Gemini AI
- **Frontend**: React 18 + TypeScript + Vite + Radix UI + Tailwind CSS
- **Architecture**: Multi-agent AI system with real-time SSE updates

---

## 🌳 Complete File Tree Structure

### **Level 0: Project Root**
```
AgenticItineraryPlanner/
├── 📄 backend_technical_design.md
├── 📄 build.gradle
├── 📄 gradlew
├── 📄 gradlew.bat
├── 📄 HELP.md
├── 📄 README.md
├── 📄 REPOSITORY_OVERVIEW.md
├── 📄 Roadmap.md
├── 📄 settings.gradle
├── 📄 swagger-api-documentation.yaml
├── 📁 bin/ (Build artifacts)
├── 📁 build/ (Compiled output)
├── 📁 frontend/ (React application)
├── 📁 gradle/ (Gradle wrapper)
├── 📁 logs/ (Application logs)
├── 📁 responses/ (AI response cache)
└── 📁 src/ (Java source code)
```

### **Level 1: Backend Source Structure**
```
src/
├── 📁 main/
│   ├── 📁 java/com/tripplanner/ (Java source code)
│   └── 📁 resources/ (Configuration files)
└── 📁 test/
    └── 📁 resources/ (Test configuration)
```

### **Level 2: Java Package Structure**
```
src/main/java/com/tripplanner/
├── 📄 App.java (Main application class)
├── 📁 api/ (REST Controllers)
├── 📁 config/ (Configuration classes)
├── 📁 data/ (Data layer)
├── 📁 providers/ (External service providers)
└── 📁 service/ (Business logic)
```

### **Level 3: API Layer (Controllers & DTOs)**
```
src/main/java/com/tripplanner/api/
├── 📄 AgentController.java (SSE agent events)
├── 📄 BookingController.java (Payment & booking)
├── 📄 DocumentationController.java (API docs)
├── 📄 ExportController.java (PDF/email export)
├── 📄 GlobalExceptionHandler.java (Error handling)
├── 📄 HealthController.java (Health checks)
├── 📄 ItinerariesController.java (Main CRUD operations)
├── 📄 TestController.java (Testing endpoints)
├── 📄 ToolsController.java (Utility endpoints)
└── 📁 dto/ (Data Transfer Objects)
    ├── 📄 AccommodationDto.java
    ├── 📄 ActivityDto.java
    ├── 📄 AgentEvent.java
    ├── 📄 CreateItineraryReq.java
    ├── 📄 EnhancedCreateItineraryReq.java
    ├── 📄 ExtendReq.java
    ├── 📄 ExtendRequest.java
    ├── 📄 ItineraryDayDto.java
    ├── 📄 ItineraryDto.java
    ├── 📄 LocationDto.java
    ├── 📄 MasterTripData.java
    ├── 📄 MealDto.java
    ├── 📄 PartyDto.java
    ├── 📄 PriceDto.java
    ├── 📄 ReviseReq.java
    ├── 📄 ReviseRequest.java
    ├── 📄 ReviseRes.java
    ├── 📄 ShareResponse.java
    ├── 📄 TransportationDto.java
    └── 📄 UpdateItineraryReq.java
```

### **Level 3: Data Layer**
```
src/main/java/com/tripplanner/data/
├── 📁 entity/ (JPA Entities)
│   ├── 📄 Booking.java
│   ├── 📄 CostEstimate.java
│   ├── 📄 Itinerary.java
│   ├── 📄 PackingList.java
│   ├── 📄 ProviderConfig.java
│   └── 📄 User.java
└── 📁 repo/ (Repository interfaces)
    ├── 📄 BookingRepository.java
    ├── 📄 ItineraryRepository.java
    └── 📄 UserRepository.java
```

### **Level 3: Service Layer**
```
src/main/java/com/tripplanner/service/
├── 📄 AgentEventBus.java (SSE event management)
├── 📄 BookingService.java (Payment processing)
├── 📄 GeminiClient.java (AI integration)
├── 📄 ItineraryService.java (Core business logic)
├── 📄 MasterTripDataService.java (Trip data management)
├── 📄 PdfService.java (PDF generation)
├── 📄 RazorpayService.java (Payment gateway)
├── 📄 ToolsService.java (Utility services)
└── 📁 agents/ (AI Agent implementations)
    ├── 📄 AgentCompletionEvent.java
    ├── 📄 AgentOrchestrator.java (Main orchestrator)
    ├── 📄 BaseAgent.java (Abstract base class)
    ├── 📄 PlacesAgent.java (Places discovery - UNUSED)
    └── 📄 PlannerAgent.java (Main planner - USES MOCK DATA)
```

### **Level 3: Configuration & Providers**
```
src/main/java/com/tripplanner/
├── 📁 config/
│   └── 📄 CorsConfig.java (CORS configuration)
└── 📁 providers/
    ├── 📄 HotelSearchProvider.java (Hotel search interface)
    └── 📁 impl/
        └── 📄 MockHotelProvider.java (Mock implementation)
```

### **Level 3: Resources**
```
src/main/resources/
├── 📄 application.yml (Main configuration)
├── 📄 swagger-api-documentation.yaml (API documentation)
├── 📁 static/ (Static web assets)
│   ├── 📄 swagger-ui.html
│   └── 📁 swagger-ui/
│       └── 📄 index.html
└── 📁 templates/ (Email templates)
```

---

## 🎨 Frontend Structure

### **Level 1: Frontend Root**
```
frontend/
├── 📄 index.html (Entry point)
├── 📄 package.json (Dependencies)
├── 📄 package-lock.json (Lock file)
├── 📄 README.md (Frontend docs)
├── 📄 vite.config.ts (Build configuration)
├── 📁 build/ (Production build)
├── 📁 node_modules/ (Dependencies)
└── 📁 src/ (Source code)
```

### **Level 2: Frontend Source**
```
frontend/src/
├── 📄 App.tsx (Main app component)
├── 📄 main.tsx (React entry point)
├── 📄 index.css (Global styles)
├── 📁 assets/ (Static assets)
├── 📁 components/ (React components)
├── 📁 data/ (Static data)
├── 📁 guidelines/ (Development guidelines)
├── 📁 i18n/ (Internationalization)
├── 📁 services/ (API services)
├── 📁 state/ (State management)
├── 📁 styles/ (Styling)
├── 📁 types/ (TypeScript types)
└── 📁 utils/ (Utility functions)
```

### **Level 3: Components Structure**
```
frontend/src/components/
├── 📄 LandingPage.tsx (Homepage)
├── 📄 TravelPlanner.tsx (Main planner component)
├── 📄 WorkflowBuilder.tsx (Workflow management)
├── 📁 agents/ (AI Agent UI)
│   ├── 📄 AgentOrchestrator.tsx (Agent progress UI)
│   ├── 📄 AgentProgressModal.tsx (Progress modal)
│   ├── 📄 AgentResultsPanel.tsx (Results display)
│   └── 📄 GeneratingPlan.tsx (Generation UI)
├── 📁 booking/ (Booking system)
│   ├── 📄 BookingConfirmation.tsx
│   ├── 📄 Checkout.tsx
│   ├── 📄 CostAndCart.tsx
│   └── 📄 HotelBookingSystem.tsx
├── 📁 dialogs/ (Modal dialogs)
│   └── 📄 AddDestinationDialog.tsx
├── 📁 figma/ (Design system)
│   └── 📄 ImageWithFallback.tsx
├── 📁 shared/ (Shared components)
│   └── 📄 LanguageSelector.tsx
├── 📁 travel-planner/ (Main planner components)
│   ├── 📁 layout/ (Layout components)
│   │   ├── 📄 NavigationSidebar.tsx
│   │   ├── 📄 ResizablePanel.tsx
│   │   └── 📄 TopNavigation.tsx
│   ├── 📁 shared/ (Shared planner components)
│   │   ├── 📄 ErrorBoundary.tsx
│   │   ├── 📄 LoadingSpinner.tsx
│   │   ├── 📄 ToolsPanels.tsx
│   │   ├── 📄 TransportConnector.tsx
│   │   ├── 📄 TransportPopup.tsx
│   │   └── 📄 types.ts
│   └── 📁 views/ (Different planner views)
│       ├── 📄 BudgetView.tsx
│       ├── 📄 CollectionView.tsx
│       ├── 📄 DayByDayView.tsx
│       ├── 📄 DestinationsManager.tsx
│       ├── 📄 DiscoverView.tsx
│       ├── 📄 DocumentsView.tsx
│       ├── 📄 PackingListView.tsx
│       ├── 📄 TransportPlanner.tsx
│       └── 📄 TripOverviewView.tsx
├── 📁 trip-management/ (Trip management)
│   ├── 📄 EditMode.tsx
│   ├── 📄 ItineraryOverview.tsx
│   ├── 📄 ShareView.tsx
│   └── 📄 TripDashboard.tsx
├── 📁 trip-wizard/ (Trip creation wizard)
│   └── 📄 SimplifiedTripWizard.tsx
├── 📁 ui/ (UI component library - 48 components)
│   ├── 📄 accordion.tsx
│   ├── 📄 alert-dialog.tsx
│   ├── 📄 alert.tsx
│   ├── 📄 aspect-ratio.tsx
│   ├── 📄 avatar.tsx
│   ├── 📄 badge.tsx
│   ├── 📄 breadcrumb.tsx
│   ├── 📄 button.tsx
│   ├── 📄 calendar.tsx
│   ├── 📄 card.tsx
│   ├── 📄 carousel.tsx
│   ├── 📄 chart.tsx
│   ├── 📄 checkbox.tsx
│   ├── 📄 collapsible.tsx
│   ├── 📄 command.tsx
│   ├── 📄 context-menu.tsx
│   ├── 📄 dialog.tsx
│   ├── 📄 drawer.tsx
│   ├── 📄 dropdown-menu.tsx
│   ├── 📄 form.tsx
│   ├── 📄 hover-card.tsx
│   ├── 📄 input-otp.tsx
│   ├── 📄 input.tsx
│   ├── 📄 label.tsx
│   ├── 📄 menubar.tsx
│   ├── 📄 navigation-menu.tsx
│   ├── 📄 pagination.tsx
│   ├── 📄 popover.tsx
│   ├── 📄 progress.tsx
│   ├── 📄 radio-group.tsx
│   ├── 📄 resizable.tsx
│   ├── 📄 scroll-area.tsx
│   ├── 📄 select.tsx
│   ├── 📄 separator.tsx
│   ├── 📄 sheet.tsx
│   ├── 📄 sidebar.tsx
│   ├── 📄 skeleton.tsx
│   ├── 📄 slider.tsx
│   ├── 📄 sonner.tsx
│   ├── 📄 switch.tsx
│   ├── 📄 table.tsx
│   ├── 📄 tabs.tsx
│   ├── 📄 textarea.tsx
│   ├── 📄 toggle-group.tsx
│   ├── 📄 toggle.tsx
│   ├── 📄 tooltip.tsx
│   ├── 📄 use-mobile.ts
│   └── 📄 utils.ts
└── 📁 workflow/ (Workflow management)
    ├── 📄 WorkflowNode.tsx
    └── 📄 WorkflowUtils.ts
```

### **Level 3: Frontend Services & State**
```
frontend/src/
├── 📁 services/ (API integration)
│   ├── 📄 apiClient.ts (HTTP client)
│   └── 📄 dataTransformer.ts (Data transformation)
├── 📁 state/ (State management)
│   ├── 📁 hooks/ (Custom hooks)
│   │   └── 📄 index.ts
│   ├── 📁 query/ (React Query)
│   │   ├── 📄 client.ts
│   │   └── 📄 hooks.ts
│   ├── 📁 slices/ (Redux slices)
│   │   └── 📄 types.ts
│   ├── 📁 store/ (Store configuration)
│   │   ├── 📄 appState.ts
│   │   ├── 📄 index.ts
│   │   └── 📄 useAppStore.ts
│   └── 📄 README.md
├── 📁 i18n/ (Internationalization)
│   ├── 📄 index.ts
│   ├── 📁 locales/ (Language files)
│   │   ├── 📄 bn.json (Bengali)
│   │   ├── 📄 en.json (English)
│   │   ├── 📄 hi.json (Hindi)
│   │   └── 📄 te.json (Telugu)
│   └── 📄 README.md
├── 📁 data/ (Static data)
│   └── 📄 destinations.ts
├── 📁 types/ (TypeScript definitions)
│   └── 📄 TripData.ts
└── 📁 styles/ (Styling)
    └── 📄 globals.css
```

---

## 📊 File Count Summary by Level

### **Backend (Java)**
- **Level 0**: 8 files (root config files)
- **Level 1**: 2 directories (main, test)
- **Level 2**: 5 packages (api, config, data, providers, service)
- **Level 3**: 55+ Java files
  - Controllers: 9 files
  - DTOs: 20 files
  - Entities: 6 files
  - Repositories: 3 files
  - Services: 8 files
  - Agents: 5 files
  - Config: 1 file
  - Providers: 2 files

### **Frontend (React/TypeScript)**
- **Level 0**: 5 files (root config files)
- **Level 1**: 2 directories (src, build)
- **Level 2**: 8 directories (components, services, state, etc.)
- **Level 3**: 100+ TypeScript/TSX files
  - Components: 80+ files
  - Services: 2 files
  - State: 6 files
  - Types: 1 file
  - Utils: Multiple files
  - i18n: 5 files

### **Configuration & Build**
- **Gradle**: 3 files (build.gradle, settings.gradle, wrapper)
- **Resources**: 4 files (YAML configs, HTML templates)
- **Documentation**: 4 files (README, Roadmap, Technical Design, Overview)

---

## 🔧 Key Architecture Patterns

### **Backend Architecture**
```
Controller Layer (REST API)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Entity Layer (Data Models)
```

### **Frontend Architecture**
```
App Component
    ↓
Feature Components (Travel Planner, Booking, etc.)
    ↓
UI Components (Radix UI + Custom)
    ↓
Services (API Client, State Management)
```

### **Agent System Architecture**
```
AgentOrchestrator (Main Coordinator)
    ↓
BaseAgent (Abstract Base)
    ↓
PlannerAgent (Main - Uses Mock Data)
PlacesAgent (Unused - Has Real AI)
```

---

## 📈 Development Status by Component

### **✅ Fully Implemented**
- Backend API structure
- Frontend UI components
- State management
- Internationalization
- PDF generation
- Payment integration

### **⚠️ Partially Implemented**
- AI Agent system (PlannerAgent uses mock data)
- Real-time SSE (infrastructure ready, limited usage)

### **❌ Not Implemented**
- PlacesAgent integration (exists but unused)
- Real AI integration in main flow
- Multiple agent orchestration

---

## 🎯 File Organization Principles

1. **Separation of Concerns**: Clear separation between API, business logic, and data layers
2. **Feature-Based Organization**: Frontend components organized by feature
3. **Reusable Components**: UI library with 48+ reusable components
4. **Type Safety**: Comprehensive TypeScript definitions
5. **Internationalization**: Multi-language support (4 languages)
6. **State Management**: Multiple state management approaches (Zustand, React Query)

This blueprint provides a complete overview of the project structure, making it easy to navigate and understand the codebase organization.

# Java Backend Technical Design — MVP Trip Planner (UI + LLM Agents)

**Stack**
Java 17+, Spring Boot 3.x, REST + SSE, Firestore (server SDK), Google GenAI SDK (Gemini), Google Sign‑In (ID token verification), Razorpay (payments), Google Maps (frontend), HTML→PDF via OpenHTMLtoPDF, SMTP via Jakarta Mail.
**Base URL:** `/api/v1`
**Non‑goals:** pick specific travel providers (adapters are generic), pricing/subscriptions.

---

## 1) Responsibilities & Module Map

* **API Layer (Controllers)** — stateless REST endpoints + an SSE stream for agent events.
* **Auth/Security** — verify Google ID tokens; session + CSRF for browser POSTs.
* **Itinerary Engine** — orchestration entrypoint; delegates to agents; persists canonical itinerary JSON.
* **Agents** — Planner, Places, Route/Time, Hotel, Flights, Activities, Ground (bus/train), PT, Food, Photo, Packing, Cost.
* **Provider Adapters** — abstraction over external flight/hotel/activity/bus/train/PT providers. Each adapter implements a common SPI.
* **Payments** — Razorpay order creation, webhook verify, booking state machine.
* **Docs/Exports** — HTML→PDF generator; transactional Email sender.
* **Data Access** — Firestore repositories + DTO mappers.

### Package layout

```
com.tripplanner
 ├─ api        // controllers (REST + SSE)
 ├─ security   // token verification, csrf, rate‑limit
 ├─ service    // itinerary, revise, extend, hotels, booking, pdf, email
 ├─ agents     // orchestrator + individual agents
 ├─ providers  // vertical + provider adapters (SPI + impls)
 ├─ data       // Firestore entities & repositories
 └─ util       // idempotency, currency, time, geo helpers
```

---

## 2) Contracts That Match the UI

### 2.1 Itineraries

* **Create** `POST /itineraries` → `{id, summary, map, days[]}`
* **Get** `GET /itineraries/{id}`
* **Revise** `POST /itineraries/{id}:revise` → `{diff, full}`
* **Extend** `POST /itineraries/{id}:extend` → updated itinerary
* **Save** `POST /itineraries/{id}:save` → `204`
* **Share (public DTO)** `GET /itineraries/{id}/public`

### 2.2 Hotels / Search (per‑day)

* **Search** `POST /itineraries/{id}/hotels:query` → `{options[]}`

### 2.3 Booking (Mode B)

* **Create Razorpay order** `POST /payments/razorpay/order` → `{orderId, amount, currency}`
* **Webhook** `POST /payments/razorpay/webhook` → `200`
* **Execute booking** `POST /providers/{vertical}/{provider}:book` → `{bookingId, status, providerConfirmationId}`
* **Get booking** `GET /bookings/{bookingId}`

### 2.4 Tools

* **Packing** `POST /packing-list` → `{items[]}`
* **Photo spots** `POST /photo-spots` → `{spots[]}`
* **Must‑try foods** `POST /must-try-foods` → `{items[]}`
* **Cost estimator** `POST /cost-estimator` → `{currency, totals, perDay}`

### 2.5 Auth / Export

* **Google Sign‑In** `POST /auth/google` (exchange & verify ID token → server session)
* **Email send** `POST /email/send` → `202`
* **PDF** `GET /itineraries/{id}/pdf` (binary)

### 2.6 Agent Events (SSE)

* **Stream** `GET /agents/stream?itineraryId=…` → `text/event-stream` with `AgentEvent` JSON.

**`AgentEvent`**

```json
{
  "agentId": "uuid",
  "kind": "planner|places|route|hotels|flights|activities|bus|train|pt|food|photo|packing|cost|orchestrator",
  "status": "queued|running|succeeded|failed",
  "progress": 0,
  "message": "string",
  "step": "string",
  "updatedAt": "ISO-8601",
  "itineraryId": "it_xxx"
}
```

---

## 3) Data Model (Firestore)

Collections (minimal MVP):

* `users/{userId}` → `{ email, name, createdAt }`
* `itineraries/{itineraryId}` → `{ userId, destination, dates, party, budgetTier, interests[], language, summary, map, days[] }`
* `bookings/{bookingId}` → `{ userId, itineraryId, item{type,provider,token}, price{amount,currency}, razorpay{orderId,paymentId,signature}, provider{confirmationId,status} }`
* `providerConfigs/{id}` → `{ type, enabled, priority, authMeta, capabilities }`
* `packingLists/{id}`, `costEstimates/{id}`, `assets/{id}` (PDF)

Indexes: by `userId` + `updatedAt` on itineraries & bookings. TTL on ephemeral documents (estimates).

---

## 4) Auth & Security

* **ID token verification**: Backend verifies Google ID tokens and creates a server session. Reject tokens with wrong audience/issuer/expiry.
* **Headers**: All API POST/PUT carry anti‑CSRF token (cookie + header).
* **Idempotency**: `Idempotency-Key` for booking‑related POSTs.
* **Secrets**: Razorpay keys, SMTP creds, Gemini key in Secret Manager.

---

## 5) LLM Agent Architecture (Gemini)

### 5.1 Orchestrator flow (create itinerary)

1. Normalize input → persist a draft itinerary doc.
2. **Planner Agent** invokes Gemini with **structured output** schema for itinerary JSON.
3. **Places** resolves geo (server‑side lookup if needed) and merges lat/lng + tags into the plan.
4. **Route/Time** enriches segments with mode + duration.
5. **Hotel/Food/Photo/Packing/Cost** agents run in parallel as needed; results attached to draft.
6. Persist canonical JSON → emit `succeeded` event.

### 5.2 Function‑calling & structured output

* Use Gemini **function calling (tools)** for cases where the model must produce parameters that our code executes (e.g., a place search or provider query).
* Use **structured output** (response schema) for itinerary JSON and diffs to guarantee machine‑readable responses.

### 5.3 Agent communication

* Agents communicate via in‑process service calls. Each agent emits `AgentEvent` to the SSE hub. The Orchestrator aggregates completion and sets overall status.

---

## 6) Controllers — Code skeletons

### 6.1 SSE — Agent stream

```java
@GetMapping(path = "/agents/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream(@RequestParam String itineraryId) {
  SseEmitter emitter = new SseEmitter(0L); // no timeout; manage lifecycle
  agentEventBus.register(itineraryId, emitter);
  emitter.onCompletion(() -> agentEventBus.unregister(itineraryId, emitter));
  emitter.onTimeout(() -> agentEventBus.unregister(itineraryId, emitter));
  return emitter;
}
```

### 6.2 Create itinerary → orchestrator

```java
@PostMapping("/itineraries")
public ItineraryDto create(@RequestBody CreateItineraryReq req, Principal user) {
  return itineraryService.create(req, user);
}
```

### 6.3 Revise / Extend

```java
@PostMapping("/itineraries/{id}:revise")
public ReviseRes revise(@PathVariable String id, @RequestBody ReviseReq r) {
  return reviseService.revise(id, r);
}

@PostMapping("/itineraries/{id}:extend")
public ItineraryDto extend(@PathVariable String id, @RequestBody ExtendReq r) {
  return extendService.extend(id, r.days());
}
```

---

## 7) Gemini usage (Agents) — Code outline

### 7.1 Structured output (Planner)

* Define a JSON schema for `{summary, map, days[]}`.
* Call Gemini with system prompt + user inputs + `responseSchema`.
* Parse the JSON; persist to Firestore; emit events.

### 7.2 Function calling (tools)

* Define function declarations for provider searches (input shapes only).
* Let Gemini produce the tool call and parameters; execute the HTTP call in Java; supply function response back; let Gemini produce the final structured result.

*(Concrete API calls are executed by our code; the model only proposes parameters.)*

---

## 8) Firestore Access — Patterns

* Use the Java server client to create a privileged environment.
* Repositories wrap CRUD with DTO mapping and validation.
* Batched writes for multi‑document transactions (e.g., create itinerary + initial events).

---

## 9) Payments — Razorpay (Mode B)

### 9.1 Order + Checkout

* Server creates a Razorpay **Order** for the amount/currency and returns `orderId` to FE.
* FE opens Razorpay Checkout with the `orderId`.

### 9.2 Webhook → booking pipeline

* Verify webhook signature; read payment + order IDs.
* Execute provider booking with stored token; update `bookings/{}`; emit events.
* On provider failure after payment, mark as failed and follow refund policy (business rule).

---

## 10) PDF & Email

* **PDF**: Generate from an HTML template (itinerary view) using OpenHTMLtoPDF; stream as binary.
* **Email**: Use Jakarta Mail (SMTP) for transactional email (share itinerary, receipts). Configure SMTP host/port/credentials.

---

## 11) Deployment (Google Cloud)

* Containerize Spring Boot; deploy to **Cloud Run**. Use Cloud Buildpacks or Dockerfile.
* Store secrets in Secret Manager; configure service env vars; set min instances if needed to reduce cold starts.

---

## 12) Provider Adapter SPI

### SPI Interfaces

```java
public interface HotelSearchProvider {
  List<HotelOption> search(HotelQuery q);
  BookingResult book(BookingRequest r);
}

public interface ActivitySearchProvider { /* search(...) */ }
public interface FlightSearchProvider { /* search(...) */ }
public interface GroundSearchProvider { /* search(...) */ }
public interface PublicTransportProvider { /* search(...) */ }
```

### Normalized DTOs

* `HotelQuery { lat,lng,dateWindow,filters,budgetTier }` → `HotelOption { provider,name,lat,lng,checkIn,checkOut,price,currency,token }`
* Similar for Flights/Activities/Ground/PT.

**Selection policy:** configurable priority list per vertical; failover on provider errors.

---

## 13) Booking State Machine

States: `INIT → PAYMENT_ORDERED → PAYMENT_CONFIRMED → BOOKING_IN_PROGRESS → CONFIRMED | FAILED`
Events: payment webhook, provider booked/failed, timeout handler.
Persistence: stored in `bookings/{}` with timestamps.
SSE: emit per‑transition `AgentEvent` (kind=`hotels|flights|activities` etc.).

---

## 14) Error Model & Idempotency

* Error body: `{ error:{ code, message, hint } }`
* Idempotency for POSTs via `Idempotency-Key` header (dedupe cache by key + route + body hash).
* Retries: exponential backoff for provider calls; circuit breaker per adapter.

---

## 15) Non‑functional

* **Performance**: keep cold start small; cache model prompts; batch Firestore reads.
* **Security**: validate all inputs; sanitize HTML (PDF/email); least‑privilege IAM for Firestore.
* **Internationalization**: server returns English text; numeric/currency formats in payloads.

---

## 16) Acceptance Criteria (Backend)

* All endpoints behave per contracts; schema validation on inputs/outputs.
* Agent SSE stream emits `queued→running→terminal` for each agent with well‑formed JSON.
* Razorpay order creation + webhook verification + booking flow works end‑to‑end.
* Firestore documents present and queryable by `userId`.
* PDF renders with correct itinerary; Email sends with SMTP test account.

---

## 17) Build Checklist

* Add dependencies: Spring Web, Spring Validation, Spring Security, Firestore Java client, Google GenAI (Gemini), Razorpay Java SDK, Jakarta Mail, OpenHTMLtoPDF.
* Create controllers and DTOs; wire services; implement Orchestrator + Agents; add SSE hub.
* Implement at least one provider adapter per vertical (search‑only for MVP where booking isn’t needed).
* Configure Google token verification, Razorpay keys, SMTP, Firestore credentials.
* Add Dockerfile/Buildpack settings; Cloud Run service with env vars; Secret Manager bindings.

---

## 18) Spring Boot Application Blueprint (concrete, UI-aligned)

This extends the earlier Java design with **Spring Boot 3.x** specifics, exact endpoint surface, config skeletons, and code scaffolds the UI can call immediately.

### 18.1 Project layout (Gradle/Maven agnostic)

```
src/main/java/com/tripplanner
 ├─ App.java                          // @SpringBootApplication
 ├─ api/                              // REST + SSE controllers
 │   ├─ ItinerariesController.java
 │   ├─ ToolsController.java          // packing, photo, foods, cost
 │   ├─ BookingController.java        // payments + provider booking
 │   ├─ AuthController.java
 │   ├─ ExportController.java         // PDF
 │   └─ AgentController.java          // SSE stream
 ├─ security/
 │   ├─ SecurityConfig.java           // SecurityFilterChain + CORS + CSRF
 │   ├─ GoogleIdTokenAuthFilter.java  // verifies Google ID tokens
 │   └─ IdempotencyFilter.java        // once-per-request filter
 ├─ service/                          // business/agents
 │   ├─ ItineraryService.java
 │   ├─ ReviseService.java
 │   ├─ ExtendService.java
 │   ├─ HotelsService.java
 │   ├─ BookingService.java
 │   ├─ PdfService.java
 │   ├─ EmailService.java
 │   ├─ AgentEventBus.java
 │   └─ agents/ (PlannerAgent, PlaceAgent, RouteAgent, etc.)
 ├─ providers/                        // adapter SPI + impls
 │   ├─ HotelSearchProvider.java
 │   ├─ FlightSearchProvider.java
 │   ├─ ActivitySearchProvider.java
 │   ├─ GroundSearchProvider.java
 │   ├─ PublicTransportProvider.java
 │   └─ impl/ (first provider per vertical, added sequentially)
 ├─ data/
 │   ├─ FirestoreConfig.java          // beans + templates
 │   ├─ entity/ (Itinerary, Booking, …)
 │   └─ repo/ (ItineraryRepo, BookingRepo, …)
 └─ util/ (currency, geo, time, json)
```

### 18.2 Dependencies (pom.xml excerpt)

```xml
<dependencies>
  <!-- Spring -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>

  <!-- Firestore server SDK -->
  <dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-firestore</artifactId>
    <version>RELEASE</version>
  </dependency>

  <!-- Razorpay Java SDK -->
  <dependency>
    <groupId>com.razorpay</groupId>
    <artifactId>razorpay-java</artifactId>
    <version>RELEASE</version>
  </dependency>

  <!-- Email + PDF -->
  <dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>jakarta.mail</artifactId>
    <version>RELEASE</version>
  </dependency>
  <dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-pdfbox</artifactId>
    <version>RELEASE</version>
  </dependency>

  <!-- JSON (if needed) -->
  <dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
  </dependency>
</dependencies>
```

### 18.3 application.yml (skeleton)

```yaml
server:
  port: 8080
  forward-headers-strategy: framework

spring:
  jackson:
    serialization:
      WRITE_DATES_AS_TIMESTAMPS: false
  main:
    allow-bean-definition-overriding: true

security:
  cors:
    allowed-origins: ["https://your-frontend.example", "http://localhost:3000"]
    allowed-methods: [GET, POST, PUT, DELETE]
    allowed-headers: ["*"]
    allow-credentials: true

google:
  oauth:
    client-id: "<your-web-client-id>"

firestore:
  project-id: "<gcp-project>"
  credentials: "<path-or-use-default>"

razorpay:
  key-id: "<key-id>"
  key-secret: "<key-secret>"
  webhook-secret: "<webhook-secret>"

email:
  smtp:
    host: "smtp.example.com"
    port: 587
    username: "<user>"
    password: "<pass>"
    from: "noreply@example.com"
```

### 18.4 Main app & config

```java
@SpringBootApplication
public class App { public static void main(String[] args) { SpringApplication.run(App.class, args); } }
```

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
      .authorizeHttpRequests(reg -> reg
         .requestMatchers(HttpMethod.GET, "/api/v1/itineraries/*/public").permitAll()
         .requestMatchers(HttpMethod.POST, "/api/v1/auth/google").permitAll()
         .anyRequest().authenticated())
      .addFilterBefore(new GoogleIdTokenAuthFilter(), UsernamePasswordAuthenticationFilter.class)
      .cors(withDefaults());
    return http.build();
  }
}
```

### 18.5 Google ID token verification filter (sketch)

```java
public class GoogleIdTokenAuthFilter extends OncePerRequestFilter {
  @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain fc)
      throws IOException, ServletException {
    // Expect ID token in Authorization: Bearer <id_token> or custom header; verify audience & issuer.
    // On success, set Authentication in SecurityContext.
    fc.doFilter(req, res);
  }
}
```

### 18.6 Idempotency filter (header: Idempotency-Key)

```java
public class IdempotencyFilter extends OncePerRequestFilter {
  @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain fc)
      throws IOException, ServletException {
    // For POST booking/payment endpoints: read Idempotency-Key; lookup Firestore cache; short-circuit if seen.
    fc.doFilter(req, res);
  }
}
```

---

## 19) Endpoint Surface (final, UI-ready)

Base: `/api/v1`
Auth: Google Sign‑In required except where noted.

### 19.1 Itineraries

* **POST** `/itineraries` → 200

  * Req: `{ destination, startDate, endDate, party, budgetTier, interests[], constraints[], language:"en" }`
  * Res: `{ id, summary, map, days[] }`
* **GET** `/itineraries/{id}` → 200

  * Res: `{ id, summary, map, days[] }`
* **POST** `/itineraries/{id}:revise` → 200

  * Req: `{ instructions, focusDay? }`
  * Res: `{ id, diff:{added[],removed[],updated[]}, full }`
* **POST** `/itineraries/{id}:extend` → 200

  * Req: `{ days }`
  * Res: updated itinerary
* **POST** `/itineraries/{id}:save` → 204
* **GET** `/itineraries/{id}/public` → 200 (no auth)

### 19.2 Agents (SSE)

* **GET** `/agents/stream?itineraryId=…` → `text/event-stream`

  * Data: `AgentEvent` JSON frames `{ agentId, kind, status, progress?, message?, step?, updatedAt, itineraryId }`

### 19.3 Hotels / Search

* **POST** `/itineraries/{id}/hotels:query` → 200

  * Req: `{ day, window:{from,to}, location:{lat,lng}, filters:{stars[],budgetTier} }`
  * Res: `{ options:[{provider,name,address,lat,lng,checkIn,checkOut,price,currency,token}] }`

### 19.4 Booking (Mode B)

* **POST** `/payments/razorpay/order` → 200

  * Req: `{ itemType, itineraryId, amount, currency, meta }`
  * Res: `{ orderId, amount, currency }`
* **POST** `/payments/razorpay/webhook` → 200 (no auth; signature verified)
* **POST** `/providers/{vertical}/{provider}:book` → 200

  * Req: `{ payment:{orderId,paymentId,signature}, item:{token}, itineraryId }`
  * Res: `{ bookingId, status, providerConfirmationId }`
* **GET** `/bookings/{bookingId}` → 200

### 19.5 Tools

* **POST** `/packing-list` → 200 `{ items:[{name,qty,group}] }`
* **POST** `/photo-spots` → 200 `{ spots:[{name,lat,lng,tips?}] }`
* **POST** `/must-try-foods` → 200 `{ items:[{name,desc,venues?}] }`
* **POST** `/cost-estimator` → 200 `{ currency, totals:{transport,lodging,food,activities,misc}, perDay }`

### 19.6 Auth, Export

* **POST** `/auth/google` → 200 `{ session:"ok" }` (public)
* **POST** `/email/send` → 202
* **GET** `/itineraries/{id}/pdf` → 200 (application/pdf)

**Error body (all endpoints):** `{ "error": { "code":"...", "message":"...", "hint":"..." } }`

---

## 20) Controller scaffolds (selected)

```java
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ItinerariesController {
  private final ItineraryService itineraries;
  private final ReviseService revise;
  private final ExtendService extend;

  @PostMapping("/itineraries")
  public ItineraryDto create(@Valid @RequestBody CreateItineraryReq req, Principal user) {
    return itineraries.create(req, user);
  }

  @GetMapping("/itineraries/{id}")
  public ItineraryDto get(@PathVariable String id) { return itineraries.get(id); }

  @PostMapping("/itineraries/{id}:revise")
  public ReviseRes revise(@PathVariable String id, @Valid @RequestBody ReviseReq req) {
    return revise.revise(id, req);
  }

  @PostMapping("/itineraries/{id}:extend")
  public ItineraryDto extend(@PathVariable String id, @Valid @RequestBody ExtendReq req) {
    return extend.extend(id, req.days());
  }

  @PostMapping("/itineraries/{id}:save")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void save(@PathVariable String id, Principal user) { itineraries.save(id, user); }
}
```

```java
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AgentController {
  private final AgentEventBus bus;
  @GetMapping(path = "/agents/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(@RequestParam String itineraryId) {
    SseEmitter e = new SseEmitter(0L);
    bus.register(itineraryId, e);
    e.onCompletion(() -> bus.unregister(itineraryId, e));
    e.onTimeout(() -> bus.unregister(itineraryId, e));
    return e;
  }
}
```

```java
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {
  private final BookingService booking;

  @PostMapping("/payments/razorpay/order")
  public RazorpayOrderRes order(@Valid @RequestBody RazorpayOrderReq req) { return booking.createOrder(req); }

  @PostMapping("/payments/razorpay/webhook")
  public ResponseEntity<Void> webhook(HttpServletRequest request, @RequestBody String body) {
    booking.handleWebhook(request, body);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/providers/{vertical}/{provider}:book")
  public BookingRes book(@PathVariable String vertical, @PathVariable String provider, @Valid @RequestBody ProviderBookReq req) {
    return booking.executeBooking(vertical, provider, req);
  }

  @GetMapping("/bookings/{bookingId}")
  public BookingRes get(@PathVariable String bookingId) { return booking.get(bookingId); }
}
```

---

## 21) DTOs with Validation (samples)

```java
public record CreateItineraryReq(
  @NotBlank String destination,
  @NotBlank String startDate,
  @NotBlank String endDate,
  @Valid Party party,
  @NotBlank String budgetTier,
  List<String> interests,
  List<String> constraints,
  @NotBlank String language
) {}

public record ReviseReq(@NotBlank String instructions, Integer focusDay) {}
public record ExtendReq(@Min(1) @Max(30) int days) {}

public record HotelQueryReq(
  @NotNull Integer day,
  @Valid Window window,
  @Valid LatLng location,
  @Valid Filters filters
) {}
```

---

## 22) Services & Agents — wiring

* `ItineraryService.create` → persist draft → emit `queued` for agents → call `PlannerAgent.generate` (Gemini structured output) → `Place/Route` enrich → `Hotel/Food/Photo/Packing/Cost` run as needed → update Firestore → emit `succeeded`.
* `ReviseService.revise` → call `PlannerAgent.revise` (Gemini diff schema) → merge → emit events.
* `BookingService` → Razorpay order → webhook verify → provider booking via adapter → state update + events.

---

## 23) Firestore access (example)

```java
@Component
@RequiredArgsConstructor
public class ItineraryRepo {
  private final Firestore firestore;
  public Itinerary save(Itinerary it) throws Exception {
    firestore.collection("itineraries").document(it.id()).set(it).get();
    return it;
  }
  public Optional<Itinerary> find(String id) throws Exception {
    var snap = firestore.collection("itineraries").document(id).get().get();
    return Optional.ofNullable(snap.toObject(Itinerary.class));
  }
}
```

---

## 24) SSE Event Bus (in‑memory registry)

```java
@Component
public class AgentEventBus {
  private final ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
  public void register(String itineraryId, SseEmitter e) { emitters.computeIfAbsent(itineraryId, k -> new CopyOnWriteArrayList<>()).add(e); }
  public void unregister(String itineraryId, SseEmitter e) { emitters.getOrDefault(itineraryId, new CopyOnWriteArrayList<>()).remove(e); }
  public void publish(String itineraryId, AgentEvent evt) {
    var list = emitters.getOrDefault(itineraryId, new CopyOnWriteArrayList<>());
    for (var e : list) try { e.send(SseEmitter.event().data(evt)); } catch (Exception ex) { e.complete(); }
  }
}
```

---

## 25) Razorpay integration (order + webhook)

```java
@Service
@RequiredArgsConstructor
public class BookingService {
  private final RazorpayClient client; // configured with keyId/secret
  public RazorpayOrderRes createOrder(RazorpayOrderReq req) {
    // build JSON opts per Razorpay SDK, create order, return id/amount/currency
    return new RazorpayOrderRes(/*...*/);
  }
  public void handleWebhook(HttpServletRequest req, String body) {
    // verify signature using webhook-secret; on success → move to BOOKING_IN_PROGRESS and call provider adapter
  }
}
```

---

## 26) Gemini client usage (planner agent outline)

```java
@Component
public class PlannerAgent {
  public ItineraryDto generate(CreateItineraryReq req) {
    // Call Gemini with a response schema for itinerary JSON; parse; return DTO
    return /* parsed output */;
  }
}
```

---

## 27) PDF & Email hooks

```java
@Service
public class PdfService {
  public byte[] render(ItineraryDto it) { /* build HTML, OpenHTMLtoPDF render to byte[] */ return new byte[0]; }
}

@Service
public class EmailService {
  public void send(String to, String subject, String html, byte[] attachmentPdf) { /* Jakarta Mail SMTP */ }
}
```

---

## 28) CORS, JSON, errors

* **CORS**: whitelist the FE origins in `application.yml` and apply via `CorsConfigurationSource` bean if preferred.
* **JSON**: `ObjectMapper` module config centralised; ISO‑8601 dates.
* **Errors**: `@ControllerAdvice` → map exceptions to `{error:{code,message,hint}}` with proper HTTP status.

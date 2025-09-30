# Unified Agent System Design & PRD (Updated)

This document integrates insights from the attached Reddit guidance, the enriched agent requirements, and the current system setup. It ensures **0 assumptions** and establishes a **clear, end-to-end design** for agents, data models, and chat orchestration.

---

## 1. Core Principles

* **Single Source of Truth** → `masterItinerary.json` stored in **Firebase** at path:

  ```
  users/{userId}/itineraries/{itineraryId}
  ```
* **Agents Modify JSON Only** → All agents read/update the same JSON structure. No duplication.
* **Logical Node IDs** → Each entity must have **short, consistent, logical IDs**:

  * Example: `d1_transport_r1` → Day 1, transport, revision 1
  * Example: `d2_meal_r3` → Day 2, meal, revision 3
* **Revisions Supported** → Each change produces a **revision log** stored under:

  ```
  users/{userId}/itineraries/{itineraryId}/revisions/{revisionId}
  ```
* **Chat is First-Class** → All changes from agents or users through chat are logged with action types: `ADD`, `EDIT`, `DELETE`, `PROPOSE`.
* **Minimal Scope** → Avoid too many tools/agents; focus on a few high-value ones (see below).

---

## 2. Agents (Minimal, Unified)

### **1. Enrichment Agent (Places & Context)**

* **Purpose:** Enhances itinerary with:

  * Google Places API data (photos, reviews, hours, pricing)
  * Must-try foods, nearby attractions, photo spots
* **Input:** Node ID(s) + user context (day/time slot)
* **Output:** Updates node with enriched details.

### **2. Booking & Payment Agent**

* **Purpose:** Handles **all booking flows** (flights, hotels, trains, activities) and payment processing.
* **Input:** Node ID(s) + booking request
* **Output:** Booking status + transaction link (mocked with Razorpay for now)
* **Special:** Consolidates payments for multiple bookings.

### **3. Editor Agent (Chat-driven Edits)**

* **Purpose:** Applies modifications requested via chat.
* **Capabilities:**

  * Add/delete/edit nodes
  * Shift timings when user oversleeps or a place is closed
  * Adjust day structure dynamically
* **Input:** Chat request + context (dayId, nodeId, itinerary summary)
* **Output:** Updated JSON (new revision logged)

### **4. Orchestrator Agent (Router)**

* **Purpose:** Routes chat requests to the right agent(s).
* **Implementation:** LLM-based classifier (Qwen2.5-7b or Gemini)
* **Flow:**

  * Parse intent → Assign responsible agent(s)
  * Maintain context of **day, time, place** to reduce ambiguity
  * Return **top 3 candidates** for ambiguous nodes (FE renders as quick-pick chips)

---

## 3. Data Model (Day & Node Structure)

### **Itinerary JSON Root**

```json
{
  "itineraryId": "uuid",
  "userId": "user123",
  "summary": {
    "title": "Taipei Family Trip",
    "startDate": "2025-09-20",
    "endDate": "2025-09-25",
    "totalDays": 6,
    "lastRevisionId": "rev_004"
  },
  "days": [
    {
      "dayId": "d1",
      "date": "2025-09-20",
      "nodes": [
        {
          "nodeId": "d1_m1_r1", // day1, meal1, revision1
          "type": "meal",
          "title": "Breakfast at The Westin Taipei",
          "location": "Taipei, Taiwan",
          "time": "08:00",
          "duration": "2h",
          "cost": 1500,
          "currency": "INR",
          "status": "confirmed",
          "source": "EnrichmentAgent",
          "metadata": {
            "reviews": 4.8,
            "openingHours": "06:00-10:00",
            "googlePlaceId": "xyz123"
          }
        }
      ]
    }
  ],
  "revisions": [
    {
      "revisionId": "r1",
      "timestamp": "2025-09-15T12:34:56Z",
      "changes": [
        {
          "action": "EDIT",
          "nodeId": "d1_m2_r1",
          "field": "time",
          "oldValue": "07:30",
          "newValue": "08:00"
        }
      ],
      "agent": "EditorAgent"
    }
  ],
  "chat": [
    {
      "chatId": "c123",
      "timestamp": "2025-09-15T12:30:00Z",
      "message": "Move breakfast to 8 AM",
      "agentAction": "EditorAgent",
      "status": "applied",
      "linkedRevision": "r1"
    }
  ]
}
```

---

## 4. Chat Integration

* **FE → BE**: Always send with:

  * `message`
  * `selectedDayId` (if user is inside Day 2 tab, etc.)
  * `selectedNodeId` (if specific)
* **BE → FE**:

  * Return structured action (ADD/EDIT/DELETE/PROPOSE)
  * Return updated JSON snapshot (revision applied)
  * If ambiguous → top 3 node candidates

---

## 5. Revision & History

* Every agent action creates a **revision record**.
* Users can **revert to any revision**.
* FE shows a timeline slider of revisions.

---

## 6. Critical Directions (from expert Guidance)

1. **Start Small:** Implement EditorAgent end-to-end first.
2. **Interface Simplicity:** FE must always render from JSON, no side DB joins.
3. **Short-Term Memory:** Only store last few messages in chat context. Use JSON for persistent memory.
4. **Iterative Dev:** Test one loop at a time → Chat → Orchestrator → Agent → JSON → FE render.
5. **Scope Control:** Do not add packing/photo agents until MVP is stable.
6. itineraries can contain request for large number of days, agents should be able to render plan for days in parts. if planner agent gets request to plan for 14 days, the context threshold will reach, instead agents can get summary of previous days and accordingly plan for upcoming without duplications
7. A summariser logic that helps summarise data for less token use but 100% context on the user request and data present with the user
---

## 7. todo

* ✅ Finalize unified JSON schema (above)
* ✅ Implement EditorAgent with chat → revision → UI sync
* ✅ Add Orchestrator routing logic that should adapt to new agents added
* Assign priority to agents such that if multiple arerunning, one should have precedence to avoid deadlocks
* 🔜 Add EnrichmentAgent (Places API) that hits places api and gets place info, time to be spent there, etc
* 🔜 Add BookingAgent that aggregates data fromm different api providers like booking, expedia, etc and gets booking related data like hotels in day1 area, suggest by ranking using reasoning like reviews, ratings, etc. gather payment info from their api (read their docs on how to use their api)
* 🔜 Enable revisions/revert in UI

---

Here's a logically **restructured and streamlined instruction guide** based on both images, organized into **clear phases** to guide someone step-by-step through building their first AI agent.

---

# 🚀 How to Build Your AI Agent – A Practical Guide

### 🔁 Core Principle:

**Loop: Model → Tool → Result → Model**
This feedback loop is the **heartbeat of every AI agent.**

---

## 🧭 PHASE 1: Define the Problem Clearly

### ✅ Step 1: Choose a Very Specific Task

Avoid general-purpose agents in the beginning. Focus on one **clear, narrow job**. Examples:

* Book a doctor’s appointment from a hospital website.
* Monitor job boards and send matching listings.
* Summarize unread emails in your inbox.

> 🎯 **Tip:** The smaller and more well-defined the task, the easier it is to build, debug, and succeed.

---

## 🤖 PHASE 2: Set Up the Core AI Engine

### ✅ Step 2: Choose a Base Language Model (LLM)

Use a pre-trained model that handles structured outputs and reasoning. we're using gemini/openrouter models

---

## 🌐 PHASE 3: Connect to the Outside World

### ✅ Step 3: Decide the Agent’s Interfaces (Tools)

Agents need tools to act. Think of:

* **Web Scraping/Browsing:** firecrawl APIs
* **Email APIs:** Gmail API(less priority)
* **Calendar APIs:** Google Calendar
* **File Ops:** Read/write files, parse PDFs, save outputs


---

## 🛠️ PHASE 4: Build the Workflow Skeleton

### ✅ Step 4: Wire the Basics

Start with a simple control loop. Your basic pipeline:

1. **Input:** User gives a task or goal.
2. **Model Call:** Feed task + instructions into the LLM.
3. **Action Selection:** Let the model decide the next step.
4. **Tool Use:** If needed, call a tool/API (scrape, query, execute).
5. **Feedback Loop:** Feed result back into model.
6. **Repeat Until:** The user gets the final output or the task is done.

> 🧠 Loop through: model → tool → result → model — until done.

---

## 🎯 PHASE 6: Manage Scope & Memory

### ✅ Step 5: Add summary/Memory When Needed

Start simple:

* Use short-term context (last few messages) or from the summarizer that we are planning to create.
* For cross-run memory, use JSON files or a small database.

### ✅ Step 6: Avoid Scope Creep

Don’t try to build a “universal agent” from day one.

> 🎯 A **single, reliable agent** that can book an appointment or manage emails is far more valuable than a buggy general-purpose one.

---



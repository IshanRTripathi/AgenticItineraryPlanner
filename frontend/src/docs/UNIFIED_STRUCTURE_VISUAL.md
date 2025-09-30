# Unified Itinerary Structure - Visual Representation

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    UnifiedItinerary                            │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │ Trip Overview│  │ Global Data │  │Change History│            │
│  │             │  │             │  │             │            │
│  │ • destination│  │ • settings  │  │ • version 1 │            │
│  │ • dates     │  │ • weather   │  │ • version 2 │            │
│  │ • budget    │  │ • budget    │  │ • version 3 │            │
│  │ • party     │  │ • packing   │  │ • ...       │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Days Array                              │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   Day 1     │  │   Day 2     │  │   Day 3     │            │
│  │             │  │             │  │             │            │
│  │ • location  │  │ • location  │  │ • location  │            │
│  │ • components│  │ • components│  │ • components│            │
│  │ • agentData │  │ • agentData │  │ • agentData │            │
│  │ • workflow  │  │ • workflow  │  │ • workflow  │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────┘
```

## Day Structure Detail

```
┌─────────────────────────────────────────────────────────────────┐
│                        UnifiedDay                              │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │ Components  │  │ Agent Data  │  │ Workflow    │            │
│  │             │  │             │  │             │            │
│  │ ┌─────────┐ │  │ ┌─────────┐ │  │ ┌─────────┐ │            │
│  │ │Component│ │  │ │Location │ │  │ │ Nodes   │ │            │
│  │ │   1     │ │  │ │ Agent   │ │  │ │         │ │            │
│  │ └─────────┘ │  │ └─────────┘ │  │ └─────────┘ │            │
│  │ ┌─────────┐ │  │ ┌─────────┐ │  │ ┌─────────┐ │            │
│  │ │Component│ │  │ │ Photos  │ │  │ │ Edges   │ │            │
│  │ │   2     │ │  │ │ Agent   │ │  │ │         │ │            │
│  │ └─────────┘ │  │ └─────────┘ │  │ └─────────┘ │            │
│  │ ┌─────────┐ │  │ ┌─────────┐ │  │ ┌─────────┐ │            │
│  │ │Component│ │  │ │ Booking │ │  │ │ Layout  │ │            │
│  │ │   3     │ │  │ │ Agent   │ │  │ │         │ │            │
│  │ └─────────┘ │  │ └─────────┘ │  │ └─────────┘ │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────┘
```

## Component Structure Detail

```
┌─────────────────────────────────────────────────────────────────┐
│                    UnifiedComponent                            │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │ Core Data   │  │ Agent Data  │  │ Workflow    │            │
│  │             │  │             │  │             │            │
│  │ • id        │  │ ┌─────────┐ │  │ • nodeId    │            │
│  │ • type      │  │ │Location │ │  │ • position  │            │
│  │ • title     │  │ │ Data    │ │  │ • status    │            │
│  │ • timing    │  │ └─────────┘ │  │ • connections│           │
│  │ • location  │  │ ┌─────────┐ │  └─────────────┘            │
│  │ • cost      │  │ │ Photos  │ │                             │
│  │ • validation│  │ │ Data    │ │                             │
│  │ • version   │  │ └─────────┘ │                             │
│  └─────────────┘  │ ┌─────────┐ │                             │
│                   │ │ Booking │ │                             │
│                   │ │ Data    │ │                             │
│                   │ └─────────┘ │                             │
│                   │ ┌─────────┐ │                             │
│                   │ │Transport│ │                             │
│                   │ │ Data    │ │                             │
│                   │ └─────────┘ │                             │
│                   └─────────────┘                             │
└─────────────────────────────────────────────────────────────────┘
```

## Agent Data Sections

```
┌─────────────────────────────────────────────────────────────────┐
│                    Agent Data Sections                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                Location Agent Data                      │   │
│  │                                                         │   │
│  │ • coordinates: { lat, lng }                            │   │
│  │ • address: "123 Main St, City"                         │   │
│  │ • placeId: "ChIJ..."                                   │   │
│  │ • distances: { fromPrevious, toNext, fromStart }       │   │
│  │ • mapData: { zoomLevel, markers, routes }              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                 Photos Agent Data                       │   │
│  │                                                         │   │
│  │ • images: [{ id, url, alt, source, tags }]             │   │
│  │ • photoSpots: [{ name, coordinates, bestTime, tips }]  │   │
│  │ • instagramSpots: [{ name, hashtags, coordinates }]    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                Booking Agent Data                       │   │
│  │                                                         │   │
│  │ • status: "confirmed" | "pending" | "cancelled"        │   │
│  │ • confirmationNumber: "ABC123"                          │   │
│  │ • provider: "Booking.com"                               │   │
│  │ • bookingUrl: "https://..."                             │   │
│  │ • price: 150.00                                         │   │
│  │ • currency: "USD"                                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │               Transport Agent Data                      │   │
│  │                                                         │   │
│  │ • mode: "taxi" | "walk" | "flight" | "train"           │   │
│  │ • distance: 5.2 (km)                                    │   │
│  │ • duration: 15 (minutes)                                │   │
│  │ • cost: 25.00                                           │   │
│  │ • provider: "Uber"                                      │   │
│  │ • bookingRequired: true                                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow Diagram

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   User      │    │   Agent     │    │   System    │
│  Action     │    │ Processing  │    │  Storage    │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Update      │───▶│ Process     │───▶│ Save with   │
│ Component   │    │ with Agents │    │ Versioning  │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Day-by-Day  │    │ Workflow    │    │ Change      │
│ View Update │    │ View Update │    │ History     │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           ▼
                 ┌─────────────┐
                 │ Perfect     │
                 │ Sync        │
                 └─────────────┘
```

## Agent Processing Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    Agent Processing Flow                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Component Update Request                                       │
│           │                                                     │
│           ▼                                                     │
│  ┌─────────────────┐                                           │
│  │ Agent Registry  │                                           │
│  │                 │                                           │
│  │ • Location Agent│                                           │
│  │ • Photos Agent  │                                           │
│  │ • Booking Agent │                                           │
│  │ • Transport Agent│                                          │
│  └─────────────────┘                                           │
│           │                                                     │
│           ▼                                                     │
│  ┌─────────────────┐                                           │
│  │ Filter Relevant │                                           │
│  │ Agents          │                                           │
│  │                 │                                           │
│  │ Based on:       │                                           │
│  │ • Component Type│                                           │
│  │ • Capabilities  │                                           │
│  │ • Data Sections │                                           │
│  └─────────────────┘                                           │
│           │                                                     │
│           ▼                                                     │
│  ┌─────────────────┐                                           │
│  │ Process with    │                                           │
│  │ Each Agent      │                                           │
│  │                 │                                           │
│  │ 1. Location     │                                           │
│  │    Agent        │                                           │
│  │ 2. Photos       │                                           │
│  │    Agent        │                                           │
│  │ 3. Booking      │                                           │
│  │    Agent        │                                           │
│  │ 4. Transport    │                                           │
│  │    Agent        │                                           │
│  └─────────────────┘                                           │
│           │                                                     │
│           ▼                                                     │
│  ┌─────────────────┐                                           │
│  │ Merge Results   │                                           │
│  │                 │                                           │
│  │ • Update Agent  │                                           │
│  │   Data Sections │                                           │
│  │ • Create Change │                                           │
│  │   Record        │                                           │
│  │ • Increment     │                                           │
│  │   Version       │                                           │
│  └─────────────────┘                                           │
│           │                                                     │
│           ▼                                                     │
│  ┌─────────────────┐                                           │
│  │ Update Views    │                                           │
│  │                 │                                           │
│  │ • Day-by-Day    │                                           │
│  │ • Workflow      │                                           │
│  │ • Map           │                                           │
│  └─────────────────┘                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Versioning System

```
┌─────────────────────────────────────────────────────────────────┐
│                    Versioning System                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Version 1 (Initial)                                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ • Created itinerary                                    │   │
│  │ • Added 3 days                                         │   │
│  │ • Added 12 components                                  │   │
│  │ • Modified by: "planner-agent"                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Version 2 (Location Updates)                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ • Updated coordinates for 5 components                 │   │
│  │ • Added map markers                                     │   │
│  │ • Calculated distances                                  │   │
│  │ • Modified by: "location-agent"                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Version 3 (Photo Integration)                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ • Added image URLs for 8 components                    │   │
│  │ • Generated photo spots                                 │   │
│  │ • Added Instagram locations                             │   │
│  │ • Modified by: "photos-agent"                           │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Version 4 (Booking Updates)                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ • Confirmed 3 hotel bookings                           │   │
│  │ • Updated flight details                                │   │
│  │ • Added confirmation numbers                            │   │
│  │ • Modified by: "booking-agent"                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Version 5 (User Edits)                                        │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ • Changed restaurant time                               │   │
│  │ • Added personal notes                                  │   │
│  │ • Modified by: "user"                                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                Rollback Capability                      │   │
│  │                                                         │   │
│  │ • Can rollback to any previous version                  │   │
│  │ • Preserves all change history                          │   │
│  │ • Atomic operations                                     │   │
│  │ • No data loss                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Benefits Visualization

```
┌─────────────────────────────────────────────────────────────────┐
│                    Benefits Overview                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ Perfect Sync    │  │ Agent Efficiency│  │ Versioning      │ │
│  │                 │  │                 │  │                 │ │
│  │ • Day-by-Day    │  │ • Dedicated     │  │ • Change        │ │
│  │   ↔ Workflow    │  │   Data Sections │  │   History       │ │
│  │ • Real-time     │  │ • Independent   │  │ • Rollback      │ │
│  │   Updates       │  │   Processing    │  │   Capability    │ │
│  │ • No Conflicts  │  │ • Easy to Add   │  │ • Audit Trail   │ │
│  └─────────────────┘  │   New Agents    │  └─────────────────┘ │
│                       └─────────────────┘                     │
│                                                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ Performance     │  │ Extensibility   │  │ Data Integrity  │ │
│  │                 │  │                 │  │                 │ │
│  │ • Single Source │  │ • New Agent     │  │ • Validation    │ │
│  │   of Truth      │  │   Types         │  │   at Every      │ │
│  │ • Reduced       │  │ • New Data      │  │   Step          │ │
│  │   Duplication   │  │   Sections      │  │ • Atomic        │ │
│  │ • Efficient     │  │ • Backward      │  │   Operations    │ │
│  │   Access        │  │   Compatible    │  │ • No Data Loss  │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

This visual representation shows how the unified structure provides a clean, efficient, and extensible foundation for managing itinerary data across all views and agents.


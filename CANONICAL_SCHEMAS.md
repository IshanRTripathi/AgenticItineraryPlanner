# Canonical Data Schemas - Agentic Itinerary Planner

## Overview
This document defines the single source of truth for all data structures used across frontend, backend, and LLM integration.

## 1. NormalizedItinerary Schema

### Backend DTO (Java)
```java
public class NormalizedItinerary {
    @JsonProperty("itineraryId") private String itineraryId;
    @JsonProperty("version") private Integer version;
    @JsonProperty("summary") private String summary;
    @JsonProperty("currency") private String currency;
    @JsonProperty("themes") private List<String> themes;
    @JsonProperty("days") private List<NormalizedDay> days;
    @JsonProperty("settings") private ItinerarySettings settings;
    @JsonProperty("agents") private Map<String, AgentStatus> agents;
}
```

### Frontend Type (TypeScript)
```typescript
export interface NormalizedItinerary {
  itineraryId: string;
  version: number;
  summary: string;
  currency: string;
  themes: string[];
  days: NormalizedDay[];
  settings: ItinerarySettings;
  agents: Record<string, AgentStatus>;
}
```

### LLM Request Schema (JSON Schema)
```json
{
  "type": "object",
  "properties": {
    "itineraryId": {"type": "string"},
    "version": {"type": "integer"},
    "summary": {"type": "string"},
    "currency": {"type": "string"},
    "themes": {"type": "array", "items": {"type": "string"}},
    "days": {
      "type": "array",
      "items": {"$ref": "#/definitions/NormalizedDay"}
    },
    "settings": {"$ref": "#/definitions/ItinerarySettings"},
    "agents": {"type": "object"}
  },
  "required": ["itineraryId", "version", "summary", "currency", "themes", "days"]
}
```

## 2. NormalizedDay Schema

### Backend DTO (Java)
```java
public class NormalizedDay {
    @JsonProperty("dayNumber") private Integer dayNumber;
    @JsonProperty("date") private String date; // CHANGED: String instead of LocalDate
    @JsonProperty("location") private String location;
    @JsonProperty("nodes") private List<NormalizedNode> nodes;
    @JsonProperty("edges") private List<Edge> edges;
    @JsonProperty("intensity") private Pacing intensity;
    @JsonProperty("timeWindow") private TimeWindow timeWindow;
    @JsonProperty("totals") private DayTotals totals;
    @JsonProperty("warnings") private List<String> warnings;
    @JsonProperty("notes") private String notes;
}
```

### Frontend Type (TypeScript)
```typescript
export interface NormalizedDay {
  dayNumber: number;
  date: string; // ISO date string
  location: string;
  nodes: NormalizedNode[];
  edges: Edge[];
  intensity?: Pacing;
  timeWindow?: TimeWindow;
  totals?: DayTotals;
  warnings?: string[];
  notes?: string;
}
```

## 3. NormalizedNode Schema

### Backend DTO (Java)
```java
public class NormalizedNode {
    @JsonProperty("id") private String id;
    @JsonProperty("type") private String type; // "attraction", "meal", "accommodation", "transport"
    @JsonProperty("title") private String title;
    @JsonProperty("location") private NodeLocation location;
    @JsonProperty("timing") private NodeTiming timing;
    @JsonProperty("cost") private NodeCost cost;
    @JsonProperty("details") private NodeDetails details;
    @JsonProperty("labels") private List<String> labels;
    @JsonProperty("tips") private NodeTips tips;
    @JsonProperty("links") private NodeLinks links;
    @JsonProperty("locked") private Boolean locked;
    @JsonProperty("bookingRef") private String bookingRef;
}
```

### Frontend Type (TypeScript)
```typescript
export interface NormalizedNode {
  id: string;
  type: 'attraction' | 'meal' | 'accommodation' | 'transport';
  title: string;
  location?: NodeLocation;
  timing?: NodeTiming;
  cost?: NodeCost;
  details?: NodeDetails;
  labels?: string[];
  tips?: NodeTips;
  links?: NodeLinks;
  locked?: boolean;
  bookingRef?: string;
}
```

## 4. NodeCost Schema

### Backend DTO (Java)
```java
public class NodeCost {
    @JsonProperty("amount") private Double amount;
    @JsonProperty("currency") private String currency;
    @JsonProperty("per") private String per; // "person", "group", "night", etc.
}
```

### Frontend Type (TypeScript)
```typescript
export interface NodeCost {
  amount: number;
  currency: string;
  per: string; // "person", "group", "night", etc.
}
```

## 5. ChangeSet Schema

### Backend DTO (Java)
```java
public class ChangeSet {
    @JsonProperty("scope") private String scope; // "trip" or "day"
    @JsonProperty("day") private Integer day;
    @JsonProperty("ops") private List<ChangeOperation> ops;
    @JsonProperty("preferences") private ChangePreferences preferences;
}
```

### Frontend Type (TypeScript)
```typescript
export interface ChangeSet {
  scope: string; // "trip" | "day"
  day?: number;
  ops: ChangeOperation[];
  preferences?: ChangePreferences;
}
```

## 6. ChangeOperation Schema

### Backend DTO (Java)
```java
public class ChangeOperation {
    @JsonProperty("op") private String op; // "move", "insert", "delete"
    @JsonProperty("id") private String id;
    @JsonProperty("after") private String after;
    @JsonProperty("node") private NormalizedNode node;
    @JsonProperty("startTime") private String startTime;
    @JsonProperty("endTime") private String endTime;
}
```

### Frontend Type (TypeScript)
```typescript
export interface ChangeOperation {
  op: string; // "move" | "insert" | "delete"
  id?: string;
  after?: string;
  node?: NormalizedNode;
  startTime?: string;
  endTime?: string;
}
```

## 7. LLM Integration Schemas

### Itinerary Generation Request Schema
```json
{
  "type": "object",
  "properties": {
    "itineraryId": {"type": "string"},
    "version": {"type": "integer", "default": 1},
    "summary": {"type": "string"},
    "currency": {"type": "string"},
    "themes": {"type": "array", "items": {"type": "string"}},
    "days": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "dayNumber": {"type": "integer"},
          "date": {"type": "string", "format": "date"},
          "location": {"type": "string"},
          "nodes": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "id": {"type": "string"},
                "type": {"type": "string", "enum": ["attraction", "meal", "accommodation", "transport"]},
                "title": {"type": "string"},
                "location": {
                  "type": "object",
                  "properties": {
                    "name": {"type": "string"},
                    "address": {"type": "string"},
                    "coordinates": {
                      "type": "object",
                      "properties": {
                        "lat": {"type": "number"},
                        "lng": {"type": "number"}
                      }
                    }
                  }
                },
                "timing": {
                  "type": "object",
                  "properties": {
                    "startTime": {"type": "string"},
                    "endTime": {"type": "string"},
                    "durationMin": {"type": "integer"}
                  }
                },
                "cost": {
                  "type": "object",
                  "properties": {
                    "amount": {"type": "number"},
                    "currency": {"type": "string"},
                    "per": {"type": "string"}
                  }
                },
                "details": {
                  "type": "object",
                  "properties": {
                    "rating": {"type": "number"},
                    "category": {"type": "string"},
                    "tags": {"type": "array", "items": {"type": "string"}}
                  }
                },
                "labels": {"type": "array", "items": {"type": "string"}},
                "tips": {
                  "type": "object",
                  "properties": {
                    "bestTime": {"type": "array", "items": {"type": "string"}},
                    "travel": {"type": "array", "items": {"type": "string"}},
                    "warnings": {"type": "array", "items": {"type": "string"}}
                  }
                },
                "links": {
                  "type": "object",
                  "properties": {
                    "booking": {"type": "string"},
                    "website": {"type": "string"},
                    "phone": {"type": "string"}
                  }
                },
                "locked": {"type": "boolean"},
                "bookingRef": {"type": "string"}
              },
              "required": ["id", "type", "title"]
            }
          },
          "edges": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "from": {"type": "string"},
                "to": {"type": "string"},
                "transitInfo": {
                  "type": "object",
                  "properties": {
                    "mode": {"type": "string"},
                    "durationMin": {"type": "integer"},
                    "provider": {"type": "string"},
                    "bookingUrl": {"type": "string"}
                  }
                }
              }
            }
          }
        },
        "required": ["dayNumber", "date", "location", "nodes"]
      }
    }
  },
  "required": ["itineraryId", "version", "summary", "currency", "themes", "days"]
}
```

### ChangeSet Generation Request Schema
```json
{
  "type": "object",
  "properties": {
    "scope": {"type": "string", "enum": ["trip", "day"]},
    "day": {"type": "integer"},
    "ops": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "op": {"type": "string", "enum": ["move", "insert", "delete"]},
          "id": {"type": "string"},
          "after": {"type": "string"},
          "node": {"$ref": "#/definitions/NormalizedNode"},
          "startTime": {"type": "string"},
          "endTime": {"type": "string"}
        },
        "required": ["op"]
      }
    },
    "preferences": {
      "type": "object",
      "properties": {
        "userFirst": {"type": "boolean"},
        "autoApply": {"type": "boolean"},
        "respectLocks": {"type": "boolean"}
      }
    }
  },
  "required": ["scope", "ops"]
}
```

## 8. API Response Schemas

### Itinerary List Response
```json
{
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "id": {"type": "string"},
      "summary": {"type": "string"},
      "destination": {"type": "string"},
      "startDate": {"type": "string"},
      "endDate": {"type": "string"},
      "durationDays": {"type": "integer"},
      "status": {"type": "string"},
      "createdAt": {"type": "string"},
      "updatedAt": {"type": "string"}
    }
  }
}
```

### Itinerary Detail Response
```json
{
  "$ref": "#/definitions/NormalizedItinerary"
}
```

## 9. Data Transformation Rules

### Backend to Frontend
1. **Date Conversion**: `LocalDate` → `string` (ISO format)
2. **Field Mapping**: All field names must match exactly
3. **Type Conversion**: `Integer` → `number`, `Double` → `number`
4. **Null Handling**: Optional fields use `?` in TypeScript

### Frontend to Backend
1. **Date Conversion**: `string` → `LocalDate`
2. **Validation**: Use Jakarta validation annotations
3. **Type Safety**: Ensure type compatibility

### LLM Integration
1. **Schema Compliance**: All LLM responses must match canonical schemas
2. **Field Validation**: Validate all required fields are present
3. **Type Conversion**: Convert LLM strings to appropriate types

## 10. Migration Checklist

- [ ] Update `NormalizedDay.date` from `LocalDate` to `String`
- [ ] Update LLM schema to use `nodes`/`edges` instead of `activities`/`meals`/etc.
- [ ] Ensure `NodeCost.per` field is used consistently
- [ ] Update all API endpoints to return canonical schemas
- [ ] Add validation tests for schema compliance
- [ ] Update frontend types to match backend DTOs exactly
- [ ] Test end-to-end data flow with real LLM calls

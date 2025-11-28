# Phase 2: Place Search - Quick Summary

**Date:** November 28, 2025  
**Status:** 🚀 Ready to Implement  
**Estimated Time:** 2-3 days

---

## 🎯 WHAT WE'RE BUILDING

**User Experience:**
```
User: "Show me museums in Interlaken"
AI: [Shows 3 museum suggestions with photos, ratings, prices]
User: [Clicks "Select" on Jungfrau Museum]
AI: "✅ Added Jungfrau Museum to Day 2 at 2pm"
```

---

## 🔄 SMART CACHING

### **Cache Key:**
```
place-search:museum:interlaken, switzerland:46.6863,7.8632:museum
```

### **How It Works:**
1. First request: API call → Validate (30km filter) → Cache 3 results
2. Same request: Cache hit → Return instantly (no API call)
3. Different location: New cache entry

### **Benefits:**
- ✅ Instant responses for repeated queries
- ✅ Saves Google Places API costs (90%+ reduction)
- ✅ Only caches validated results (within 30km)
- ✅ 7-day TTL (place data doesn't change often)

---

## 🎨 UI DESIGN

### **Always 3 Cards Vertically Stacked:**

```
┌─────────────────────┐
│ ① [Photo]          │
│   Museum Name       │
│   ⭐ 4.7 (2.5K)    │
│   📍 Address        │
│   💰 $15  ⏰ 9-6pm │
│   [Select]          │
└─────────────────────┘

┌─────────────────────┐
│ ② [Photo]          │
│   Museum Name       │
│   ⭐ 4.5 (1.2K)    │
│   📍 Address        │
│   💰 $10  ⏰ 10-5pm│
│   [Select]          │
└─────────────────────┘

┌─────────────────────┐
│ ③ [Photo]          │
│   Museum Name       │
│   ⭐ 4.3 (890)     │
│   📍 Address        │
│   💰 $25  ⏰ 9-7pm │
│   [Select]          │
└─────────────────────┘
```

---

## 📋 IMPLEMENTATION STEPS

### **Backend (4-6 hours):**
1. ✅ Create `PlaceSuggestion.java` DTO (30 min)
2. ✅ Add `searchPlaces()` with caching to GooglePlacesService (2 hours)
3. ✅ Add `placeSuggestions` to ChatResponse (5 min)
4. ✅ Create `/api/v1/tools/search-places` endpoint (30 min)
5. ✅ Create request/response DTOs (30 min)

### **Frontend (4-6 hours):**
6. ✅ Create `PlaceSuggestionCard.tsx` component (2-3 hours)
7. ✅ Integrate in `ChatMessage.tsx` (1 hour)
8. ✅ Update `ChatTypes.ts` (5 min)
9. ✅ Test end-to-end (1-2 hours)

---

## ✅ SUCCESS CRITERIA

### **Backend:**
- [ ] `/api/v1/tools/search-places` returns 3 suggestions
- [ ] Each suggestion has: photo, name, rating, address, cost
- [ ] Cache works: Same query returns cached results instantly
- [ ] Only validated results (within 30km) are cached

### **Frontend:**
- [ ] 3 cards displayed vertically stacked
- [ ] Each card shows photo, name, rating, address, cost, hours
- [ ] "Select" button adds place to itinerary
- [ ] Hover effects work
- [ ] Responsive on mobile

### **End-to-End:**
- [ ] User asks for museums → Gets 3 suggestions
- [ ] User selects one → Place added to itinerary
- [ ] Same query again → Instant response (cached)

---

## 🚀 START HERE

**Full Implementation Guide:** `PHASE2_PLACE_SEARCH_IMPLEMENTATION.md`

**Quick Start:**
1. Read the full implementation guide
2. Start with Step 1: Create PlaceSuggestion DTO
3. Follow steps 2-4 for backend
4. Test backend with Postman/curl
5. Move to frontend (steps 5-7)
6. Test end-to-end

---

**Estimated Completion:** 2-3 days  
**API Cost Savings:** 90%+ (with caching)  
**User Experience:** 10x better (photos, ratings, instant responses)

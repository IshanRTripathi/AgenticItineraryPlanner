# Budget Handling Update - Relative & Context-Aware

## Problem
The system was exposing specific budget amounts (e.g., "1000.0 - 2500.0 per person") in logs and prompts, which:
- Lacks context for different destinations (1000 USD in Thailand vs Switzerland)
- Doesn't account for local cost of living
- Makes budget tiers meaningless if specific amounts override them

## Solution
Updated budget handling to be **relative and destination-aware**:

### Before
```
User's budget range: 1000.0 - 2500.0 per person
```

### After
```
Budget preference: medium
IMPORTANT: Interpret budget tier relative to Dubai, UAE:
- 'medium' means comfortable mid-range options for this destination
- Balance between cost and comfort with good value
- Mix of popular attractions, decent restaurants, efficient transport
All cost estimates should be PER PERSON in local currency.
Adjust recommendations based on local cost of living and tourism standards.
```

## Budget Tier Interpretations

### Budget Tier
- Cost-conscious choices appropriate for the destination
- Affordable but quality accommodations
- Local restaurants, public transport
- Free/low-cost activities, local experiences

### Medium Tier
- Comfortable mid-range options
- Balance between cost and comfort
- Popular attractions, decent restaurants
- Efficient transport options

### Luxury Tier
- Premium experiences appropriate for the destination
- High-end accommodations, fine dining
- Private transport, VIP experiences
- Exclusive activities, premium services

## Key Benefits

1. **Context-Aware**: "Luxury" in Bali means something different than "luxury" in Dubai
2. **Flexible**: LLM can interpret based on local standards
3. **Privacy**: No specific budget amounts exposed in logs
4. **Realistic**: Recommendations match local cost of living

## Files Modified
- `src/main/java/com/tripplanner/agents/SkeletonPlannerAgent.java`

## Example Scenarios

### Budget Tier in Thailand
- Budget: Hostels, street food, local buses ($20-30/day)
- Medium: 3-star hotels, local restaurants, taxis ($50-80/day)
- Luxury: 5-star resorts, fine dining, private drivers ($200+/day)

### Budget Tier in Switzerland
- Budget: Budget hotels, casual dining, public transport ($100-150/day)
- Medium: 3-star hotels, good restaurants, trains ($200-300/day)
- Luxury: 5-star hotels, Michelin dining, private transport ($500+/day)

Same tier, different absolute costs - but appropriate for each destination!

# Looker Studio Dashboard - Field Guide & Significance
**Complete field mapping for analytics dashboard based on scheduled queries**

---

## 📊 Dashboard Structure Overview

### Recommended Dashboard Pages:
1. **Executive Summary** - High-level KPIs and trends
2. **User Growth & Engagement** - User acquisition and activity
3. **Conversion Funnels** - User journey and drop-offs
4. **LLM Cost Analysis** - AI infrastructure costs
5. **Agent Performance** - AI agent reliability and speed
6. **Alerts & Monitoring** - Real-time health checks

---

## 1️⃣ EXECUTIVE SUMMARY PAGE

### Data Source: `daily_metrics`

### Key Metrics (Scorecards)

| Field | Display Name | Significance | Alert Threshold |
|-------|--------------|--------------|-----------------|
| `dau` | Daily Active Users | Core growth metric - shows platform adoption | < 10 (low), > 100 (good) |
| `new_signups` | New Signups | User acquisition rate - marketing effectiveness | < 5 (concern), > 20 (excellent) |
| `trips_created` | Trips Created | Core product usage - primary value delivery | < 5 (low), > 50 (high) |
| `revenue` | Daily Revenue | Business viability - monetization success | < $10 (concern), > $100 (good) |
| `trip_completion_rate` | Trip Completion Rate (%) | Product quality - how many started trips finish | < 50% (poor), > 80% (excellent) |
| `booking_conversion_rate` | Booking Conversion (%) | Monetization efficiency | < 10% (poor), > 30% (excellent) |

### Time Series Charts

**Chart 1: User Growth Trend**
- **X-Axis:** `date` (last 30 days)
- **Y-Axis:** `dau`, `new_signups`
- **Significance:** Shows growth trajectory and user retention
- **Insight:** Upward trend = healthy growth, flat = stagnation

**Chart 2: Trip Creation Funnel**
- **X-Axis:** `date`
- **Y-Axis:** `trip_wizard_starts`, `trips_created`, `trip_creation_failures`
- **Significance:** Shows product adoption and quality issues
- **Insight:** High failures = UX problems or technical issues

**Chart 3: Revenue & Bookings**
- **X-Axis:** `date`
- **Y-Axis:** `revenue`, `bookings_completed`
- **Significance:** Business health and monetization
- **Insight:** Revenue per booking = pricing effectiveness

### Calculated Fields to Create

```sql
-- Average Revenue Per User (ARPU)
revenue / dau

-- Trip Success Rate
(trips_created / trip_wizard_starts) * 100

-- Engagement Rate
(trip_wizard_starts / dau) * 100
```

---

## 2️⃣ USER GROWTH & ENGAGEMENT PAGE

### Data Source: `user_engagement_daily`

### Key Metrics (Scorecards)

| Field | Display Name | Significance | What It Tells You |
|-------|--------------|--------------|-------------------|
| `dau` | Daily Active Users | User base size | Platform reach and retention |
| `total_sessions` | Total Sessions | Usage frequency | How often users return |
| `avg_events_per_session` | Events Per Session | Session depth | User engagement quality |
| `avg_page_views_per_user` | Pages Per User | Content consumption | User exploration behavior |
| `engagement_score` | Engagement Score | Overall activity | Weighted metric of valuable actions |

### Engagement Breakdown (Bar Chart)

**Chart: Feature Usage**
- **Dimensions:** Feature type
- **Metrics:**
  - `users_viewing_activities` - Activity exploration
  - `users_expanding_days` - Itinerary interaction
  - `users_using_chat` - AI assistant usage
  - `users_searching` - Search feature adoption
  - `users_exporting_pdf` - Export feature usage
  - `users_creating_links` - Sharing feature usage

**Significance:** Shows which features drive engagement and which are underutilized

### Session Quality (Time Series)

**Chart: Session Metrics Over Time**
- **X-Axis:** `date`
- **Y-Axis:** `avg_events_per_session`, `sessions_with_page_views`
- **Significance:** Higher values = more engaged users
- **Alert:** Drop in avg_events_per_session indicates UX issues

### Calculated Fields

```sql
-- Session Quality Score
(avg_events_per_session * sessions_with_page_views) / 10

-- Feature Adoption Rate
(users_using_chat / dau) * 100

-- Power User Percentage
(users_exporting_pdf / dau) * 100
```

---

## 3️⃣ CONVERSION FUNNELS PAGE

### Data Source: `funnel_metrics`

### Trip Creation Funnel (Funnel Chart)

**Stages:**
1. `funnel_trip_wizard_started` - Wizard Started (100%)
2. `funnel_trip_initiated` - Trip Initiated
3. `funnel_trip_completed` - Trip Completed

**Conversion Rates:**
- `trip_wizard_to_initiation_rate` - Wizard → Initiation
- `trip_initiation_to_completion_rate` - Initiation → Completion
- `trip_overall_conversion_rate` - Wizard → Completion

**Significance:**
- **Drop at Stage 1→2:** Wizard UX issues, unclear value prop
- **Drop at Stage 2→3:** AI generation problems, long wait times
- **Overall rate < 50%:** Major product issues

### Booking Funnel (Funnel Chart)

**Stages:**
1. `funnel_booking_initiated` - Booking Started (100%)
2. `funnel_booking_completed` - Booking Completed

**Conversion Rate:**
- `booking_conversion_rate` - Booking success rate

**Significance:**
- **Low rate (<30%):** Booking flow friction, payment issues
- **High rate (>70%):** Smooth checkout experience

### Payment Funnel (Funnel Chart)

**Stages:**
1. `funnel_payment_initiated` - Payment Started (100%)
2. `funnel_payment_completed` - Payment Completed

**Conversion Rate:**
- `payment_conversion_rate` - Payment success rate

**Significance:**
- **Low rate (<80%):** Payment gateway issues, trust problems
- **High rate (>95%):** Reliable payment processing

### Export Success (Scorecard)

**Metrics:**
- `funnel_export_initiated` - Export attempts
- `funnel_export_completed` - Successful exports
- `export_success_rate` - Success percentage

**Significance:**
- **Low rate (<90%):** PDF generation issues, server problems
- **High rate (>98%):** Reliable export feature

### Calculated Fields

```sql
-- Funnel Drop-off Rate
100 - trip_overall_conversion_rate

-- Booking Intent Rate
(funnel_booking_initiated / funnel_trip_completed) * 100

-- Revenue Per Completed Trip
revenue / funnel_trip_completed
```

---

## 4️⃣ LLM COST ANALYSIS PAGE

### Data Source: `llm_costs_daily` + `llm_costs_daily_summary`

### Cost Overview (Scorecards)

| Field | Display Name | Significance | Alert Threshold |
|-------|--------------|--------------|-----------------|
| `total_cost_usd` | Daily LLM Cost | Infrastructure spend | > $15/day (warning), > $50/day (critical) |
| `total_tokens` | Total Tokens | API usage volume | Track for rate limits |
| `request_count` | API Requests | Number of LLM calls | High count = high cost risk |
| `avg_cost_per_request` | Cost Per Request | Efficiency metric | > $0.10 (expensive) |
| `avg_tokens_per_request` | Tokens Per Request | Prompt efficiency | > 5000 (optimize prompts) |

### Cost Breakdown by Provider (Pie Chart)

**Dimensions:** `provider` (gemini, openrouter, etc.)
**Metric:** `total_cost_usd`

**Significance:** Shows which AI provider is most cost-effective

### Cost by Agent Type (Stacked Bar Chart)

**Dimensions:** `agent_type` (trip_planner, activity_search, etc.)
**Metric:** `total_cost_usd`
**Breakdown:** `provider`

**Significance:**
- Identifies expensive agents
- Helps prioritize optimization efforts
- Shows which features drive costs

### Cost by Model (Table)

**Columns:**
- `provider` - AI provider
- `model` - Specific model used
- `total_cost_usd` - Total cost
- `request_count` - Number of requests
- `avg_cost_per_request` - Cost efficiency
- `total_tokens` - Token consumption

**Significance:**
- Compare model pricing
- Identify opportunities to switch to cheaper models
- Track token efficiency

### Monthly Projection (from `llm_costs_monthly_projection`)

**Metrics:**
- `current_cost` - Month-to-date spend
- `projected_monthly_cost` - Estimated month-end cost
- `avg_daily_cost` - Average daily spend
- `days_remaining` - Days left in month

**Significance:**
- Budget forecasting
- Early warning for overspend
- Helps plan infrastructure scaling

### Time Series: Cost Trends

**Chart: Daily Cost Breakdown**
- **X-Axis:** `date`
- **Y-Axis:** `total_cost_usd`
- **Breakdown:** `agent_type`

**Significance:** Shows cost trends and spikes

### Calculated Fields

```sql
-- Cost Per Trip
total_cost_usd / trips_created

-- Cost Per User
total_cost_usd / dau

-- Token Efficiency
total_tokens / request_count

-- Provider Cost Comparison
(gemini_cost_usd / openrouter_cost_usd) * 100
```

---

## 5️⃣ AGENT PERFORMANCE PAGE

### Data Source: `agent_performance_daily`

### Reliability Metrics (Scorecards)

| Field | Display Name | Significance | Alert Threshold |
|-------|--------------|--------------|-----------------|
| `success_rate` | Agent Success Rate (%) | Reliability | < 90% (critical), > 95% (good) |
| `executions_started` | Total Executions | Usage volume | Track for capacity planning |
| `executions_completed` | Successful Runs | Completed tasks | Should match started |
| `executions_failed` | Failed Runs | Error count | > 10% (investigate) |

### Performance Metrics (Scorecards)

| Field | Display Name | Significance | Alert Threshold |
|-------|--------------|--------------|-----------------|
| `avg_duration_ms` | Average Duration | Typical response time | > 5000ms (slow) |
| `p50_duration_ms` | Median Duration | Typical user experience | > 3000ms (slow) |
| `p95_duration_ms` | 95th Percentile | Worst-case for most users | > 10000ms (poor) |
| `p99_duration_ms` | 99th Percentile | Worst-case scenarios | > 20000ms (timeout risk) |

### Agent Comparison (Table)

**Columns:**
- `agent_type` - Agent name
- `success_rate` - Reliability %
- `avg_duration_ms` - Speed
- `executions_started` - Usage
- `executions_failed` - Errors

**Sort by:** `success_rate` (ascending) to see problem agents first

**Significance:**
- Identify unreliable agents
- Find performance bottlenecks
- Prioritize optimization work

### Performance Distribution (Histogram)

**Chart: Response Time Distribution**
- **X-Axis:** Duration buckets (0-1s, 1-3s, 3-5s, 5-10s, 10s+)
- **Y-Axis:** Count of executions
- **Breakdown:** `agent_type`

**Significance:** Shows if most requests are fast or slow

### Time Series: Agent Health

**Chart: Success Rate Over Time**
- **X-Axis:** `date`
- **Y-Axis:** `success_rate`
- **Breakdown:** `agent_type`

**Significance:** Detect degradation trends

### Calculated Fields

```sql
-- Failure Rate
100 - success_rate

-- Timeout Risk Score
(p99_duration_ms / 30000) * 100

-- Agent Efficiency Score
(success_rate * 100) / avg_duration_ms

-- Error Volume
executions_failed / executions_started
```

---

## 6️⃣ ALERTS & MONITORING PAGE

### Data Source: Alert views (`alert_*`)

### Critical Alerts (Scorecards with Conditional Formatting)

**LLM Daily Cost Alert**
- **Source:** `alert_llm_daily_cost`
- **Field:** `cost`
- **Severity:** `severity` (NORMAL/WARNING/CRITICAL)
- **Color:** Green < $5, Yellow $5-15, Red > $15

**Monthly Cost Projection Alert**
- **Source:** `alert_llm_monthly_projection`
- **Field:** `projected_cost`
- **Severity:** `severity`
- **Color:** Green < $100, Yellow $100-200, Red > $200

**Agent Failure Rate Alert**
- **Source:** `alert_agent_failure_rates`
- **Fields:** `agent_type`, `failure_rate`
- **Severity:** `severity`
- **Color:** Green < 5%, Yellow 5-10%, Red > 10%

**Booking Failure Alert**
- **Source:** `alert_booking_failure_rate`
- **Field:** `failure_rate_percent`
- **Severity:** `severity`
- **Color:** Green < 10%, Yellow 10-20%, Red > 20%

### Alert History (Table)

**Columns:**
- `check_time` - When alert fired
- `alert_type` - What triggered
- `value` - Metric value
- `severity` - Alert level
- `details` - Additional context

**Significance:** Track alert patterns and response times

---

## 📈 RECOMMENDED DASHBOARD FILTERS

### Global Filters (Apply to all pages)

1. **Date Range**
   - Default: Last 30 days
   - Options: Last 7 days, Last 30 days, Last 90 days, Custom

2. **Comparison Period**
   - Compare to: Previous period, Same period last year

### Page-Specific Filters

**LLM Cost Page:**
- `provider` - Filter by AI provider
- `agent_type` - Filter by agent
- `model` - Filter by model

**Agent Performance Page:**
- `agent_type` - Focus on specific agents

---

## 🎨 VISUALIZATION BEST PRACTICES

### Color Coding

**Success Metrics (higher is better):**
- 🟢 Green: > 80%
- 🟡 Yellow: 50-80%
- 🔴 Red: < 50%

**Cost Metrics (lower is better):**
- 🟢 Green: Within budget
- 🟡 Yellow: Approaching limit
- 🔴 Red: Over budget

**Performance Metrics:**
- 🟢 Green: < 3 seconds
- 🟡 Yellow: 3-10 seconds
- 🔴 Red: > 10 seconds

### Chart Types by Use Case

- **Trends:** Line charts
- **Comparisons:** Bar charts
- **Proportions:** Pie/Donut charts
- **Funnels:** Funnel charts
- **Distributions:** Histograms
- **Correlations:** Scatter plots
- **Details:** Tables with conditional formatting

---

## 🚨 KEY BUSINESS INSIGHTS TO TRACK

### Growth Health
- DAU growth rate > 10% week-over-week
- New signups > returning users (growth phase)
- Trip completion rate > 70%

### Product Quality
- Agent success rate > 95%
- Export success rate > 98%
- Average response time < 5 seconds

### Business Viability
- Daily LLM cost < $15
- Cost per trip < $0.50
- Booking conversion > 20%

### User Satisfaction
- Engagement score trending up
- Feature adoption > 30%
- Session depth > 10 events

---

## 📊 SAMPLE CALCULATED METRICS

### Business Metrics
```sql
-- Customer Acquisition Cost (if you track marketing spend)
marketing_spend / new_signups

-- Lifetime Value Proxy
(revenue / dau) * 365

-- Viral Coefficient
users_creating_links / dau
```

### Efficiency Metrics
```sql
-- AI Cost Efficiency
total_cost_usd / trips_created

-- Agent Utilization
executions_started / dau

-- Feature ROI
(trips_created / users_using_chat) * 100
```

### Quality Metrics
```sql
-- Overall Platform Health Score
(success_rate + trip_completion_rate + export_success_rate) / 3

-- User Satisfaction Proxy
(engagement_score / dau) * (trip_completion_rate / 100)
```

---

## 🎯 DASHBOARD GOALS BY STAKEHOLDER

### For CEO/Executives
- **Focus:** DAU, revenue, growth rate, LLM costs
- **Key Question:** "Are we growing profitably?"

### For Product Managers
- **Focus:** Conversion rates, feature usage, engagement score
- **Key Question:** "What features drive value?"

### For Engineering
- **Focus:** Agent performance, success rates, response times
- **Key Question:** "Is the system reliable and fast?"

### For Finance
- **Focus:** LLM costs, cost per trip, monthly projections
- **Key Question:** "Are we within budget?"

### For Marketing
- **Focus:** New signups, DAU, booking conversion
- **Key Question:** "Is our acquisition strategy working?"

---

**This dashboard design provides complete visibility into your AI-powered trip planning platform's health, growth, and efficiency.**

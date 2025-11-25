# Looker Studio - Single Page Dashboard Design
**Complete analytics overview on one scrollable page**

---

## 🎯 Dashboard Layout Philosophy

**Single-page design with vertical sections:**
- Quick overview at the top (most critical metrics)
- Detailed sections below (scroll for deeper insights)
- Each section is self-contained
- Color-coded for quick scanning

**Page Width:** Full width (1920px recommended)  
**Sections:** 6 main sections, vertically stacked

---

## 📐 SECTION 1: EXECUTIVE SUMMARY (Top of Page)
**Height:** 300px | **Background:** Light blue (#E8F4F8)

### Layout: 4 columns

#### Column 1: User Metrics (Scorecards)
**Data Source:** `daily_metrics`

```
┌─────────────────────┐
│  Daily Active Users │
│       [dau]         │
│    ↑ +15% vs prev   │
└─────────────────────┘

┌─────────────────────┐
│   New Signups       │
│  [new_signups]      │
│    ↑ +8% vs prev    │
└─────────────────────┘
```

#### Column 2: Product Metrics (Scorecards)
**Data Source:** `daily_metrics`

```
┌─────────────────────┐
│  Trips Created      │
│  [trips_created]    │
│    ↑ +12% vs prev   │
└─────────────────────┘

┌─────────────────────┐
│ Trip Completion %   │
│[trip_completion_rate]│
│   🟢 85% (Good)     │
└─────────────────────┘
```

#### Column 3: Revenue Metrics (Scorecards)
**Data Source:** `daily_metrics`

```
┌─────────────────────┐
│  Daily Revenue      │
│    [revenue]        │
│    ↑ +20% vs prev   │
└─────────────────────┘

┌─────────────────────┐
│ Booking Conv. %     │
│[booking_conversion_rate]│
│   🟡 22% (Fair)     │
└─────────────────────┘
```

#### Column 4: Cost Metrics (Scorecards)
**Data Source:** `llm_costs_daily_summary`

```
┌─────────────────────┐
│  Daily LLM Cost     │
│ [total_cost_usd]    │
│   🟢 $8.50 (Good)   │
└─────────────────────┘

┌─────────────────────┐
│ Monthly Projection  │
│[projected_monthly_cost]│
│   🟡 $180 (Watch)   │
└─────────────────────┘
```

**Conditional Formatting:**
- 🟢 Green: Metrics within target
- 🟡 Yellow: Approaching threshold
- 🔴 Red: Exceeds threshold

---

## 📊 SECTION 2: GROWTH TRENDS (Below Summary)
**Height:** 400px | **Background:** White

### Layout: 2 columns

#### Left Column (60%): Time Series Chart
**Chart Type:** Line chart with multiple series  
**Data Source:** `daily_metrics`

```
Title: "30-Day Growth Trends"

X-Axis: date (last 30 days)
Y-Axis (Left): dau, new_signups
Y-Axis (Right): trips_created

Series:
- Daily Active Users (Blue line)
- New Signups (Green line)
- Trips Created (Orange line)

Show: Trend lines, data labels on hover
```

**Significance:** Shows growth trajectory and correlation between user acquisition and product usage

#### Right Column (40%): Key Ratios (Scorecards + Sparklines)
**Data Source:** `daily_metrics`

```
┌─────────────────────────────┐
│ Engagement Rate             │
│ (trip_wizard_starts / dau)  │
│        45%                  │
│ [7-day sparkline]           │
└─────────────────────────────┘

┌─────────────────────────────┐
│ ARPU (Revenue / DAU)        │
│        $2.50                │
│ [7-day sparkline]           │
└─────────────────────────────┘

┌─────────────────────────────┐
│ Cost Per Trip               │
│ (llm_cost / trips_created)  │
│        $0.35                │
│ [7-day sparkline]           │
└─────────────────────────────┘
```

---

## 🎯 SECTION 3: CONVERSION FUNNELS
**Height:** 500px | **Background:** Light gray (#F5F5F5)

### Layout: 3 columns (equal width)

#### Column 1: Trip Creation Funnel
**Chart Type:** Funnel chart  
**Data Source:** `funnel_metrics`

```
Title: "Trip Creation Journey"

Stages:
1. Wizard Started     [funnel_trip_wizard_started]     100%
   ↓ 75% conversion
2. Trip Initiated     [funnel_trip_initiated]          75%
   ↓ 85% conversion
3. Trip Completed     [funnel_trip_completed]          64%

Overall Conversion: [trip_overall_conversion_rate] 64%

Color: Green gradient (darker = later stage)
```

**Below funnel: Conversion rates table**
```
┌────────────────────────────────────┐
│ Wizard → Initiation:  75% 🟢       │
│ Initiation → Completion: 85% 🟢    │
│ Overall: 64% 🟡                    │
└────────────────────────────────────┘
```

#### Column 2: Booking & Payment Funnel
**Chart Type:** Funnel chart  
**Data Source:** `funnel_metrics`

```
Title: "Monetization Funnel"

Booking Flow:
1. Booking Initiated  [funnel_booking_initiated]      100%
   ↓ [booking_conversion_rate]
2. Booking Completed  [funnel_booking_completed]      28%

Payment Flow:
1. Payment Initiated  [funnel_payment_initiated]      100%
   ↓ [payment_conversion_rate]
2. Payment Completed  [funnel_payment_completed]      92%

Color: Blue gradient
```

#### Column 3: Feature Success Rates
**Chart Type:** Horizontal bar chart  
**Data Source:** `funnel_metrics`

```
Title: "Feature Success Rates"

Bars:
- Export Success    [export_success_rate]      98% 🟢
- Payment Success   [payment_conversion_rate]  92% 🟢
- Booking Success   [booking_conversion_rate]  28% 🔴
- Trip Completion   [trip_overall_conversion_rate] 64% 🟡

Color: Green (>80%), Yellow (50-80%), Red (<50%)
Target line at 80%
```

---

## 👥 SECTION 4: USER ENGAGEMENT BREAKDOWN
**Height:** 400px | **Background:** White

### Layout: 2 columns

#### Left Column (50%): Feature Usage
**Chart Type:** Stacked bar chart  
**Data Source:** `user_engagement_daily`

```
Title: "Daily Feature Adoption"

X-Axis: date (last 14 days)
Y-Axis: Number of users

Stacked bars:
- Viewing Activities    [users_viewing_activities]
- Expanding Days        [users_expanding_days]
- Using Chat           [users_using_chat]
- Searching            [users_searching]
- Exporting PDF        [users_exporting_pdf]
- Creating Links       [users_creating_links]

Show: Percentage of DAU on hover
```

#### Right Column (50%): Engagement Quality
**Top: Scorecard**
```
┌─────────────────────────────────────┐
│     Engagement Score                │
│   [engagement_score]                │
│         2,450                       │
│   ↑ +18% vs previous week           │
└─────────────────────────────────────┘
```

**Bottom: Table**
**Data Source:** `user_engagement_daily`

```
Title: "Session Quality Metrics"

┌──────────────────────────┬─────────┬──────────┐
│ Metric                   │ Today   │ 7d Avg   │
├──────────────────────────┼─────────┼──────────┤
│ Total Sessions           │   450   │   420    │
│ Avg Events/Session       │   12.5  │   11.8   │
│ Avg Pages/User           │   8.2   │   7.9    │
│ Sessions w/ Page Views   │   0.95  │   0.93   │
└──────────────────────────┴─────────┴──────────┘

Color: Green if today > 7d avg, else gray
```

---

## 💰 SECTION 5: LLM COST ANALYSIS
**Height:** 500px | **Background:** Light yellow (#FFF9E6)

### Layout: 3 rows

#### Row 1: Cost Overview (4 scorecards, horizontal)
**Data Source:** `llm_costs_daily_summary`

```
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Daily Cost   │ │ Total Tokens │ │ API Requests │ │ Cost/Request │
│   $8.50      │ │   1.2M       │ │    2,450     │ │   $0.0035    │
│ 🟢 Under $15 │ │              │ │              │ │ 🟢 Efficient │
└──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
```

#### Row 2: Cost Breakdown (2 columns)

**Left (60%): Cost by Agent Type**
**Chart Type:** Stacked area chart  
**Data Source:** `llm_costs_daily`

```
Title: "Daily Cost Breakdown by Agent"

X-Axis: date (last 30 days)
Y-Axis: total_cost_usd

Stacked areas (by agent_type):
- trip_planner
- activity_search
- cost_estimator
- enrichment_agent
- editor_agent

Show: Hover for exact costs
```

**Right (40%): Cost by Provider**
**Chart Type:** Donut chart  
**Data Source:** `llm_costs_daily`

```
Title: "Cost Distribution by Provider"

Segments (by provider):
- Gemini: 65% ($5.53)
- OpenRouter: 35% ($2.97)

Center: Total $8.50

Show: Percentage and dollar amount
```

#### Row 3: Detailed Cost Table
**Chart Type:** Table with conditional formatting  
**Data Source:** `llm_costs_daily`

```
Title: "Cost Details by Agent & Model"

┌─────────────┬──────────┬────────────┬──────────┬──────────┬─────────────┐
│ Agent Type  │ Provider │ Model      │ Requests │ Tokens   │ Cost (USD)  │
├─────────────┼──────────┼────────────┼──────────┼──────────┼─────────────┤
│ trip_planner│ gemini   │ gemini-pro │   850    │  450K    │ $3.20 🔴    │
│ activity_..  │ gemini   │ gemini-pro │   620    │  280K    │ $1.85 🟡    │
│ cost_estim.. │ openrouter│ claude-3  │   380    │  190K    │ $1.50 🟡    │
│ enrichment.. │ gemini   │ gemini-pro │   420    │  210K    │ $1.35 🟢    │
│ editor_agent│ openrouter│ gpt-4     │   180    │   95K    │ $0.60 🟢    │
└─────────────┴──────────┴────────────┴──────────┴──────────┴─────────────┘

Sort by: Cost (descending)
Color: Red (>$3), Yellow ($1-3), Green (<$1)
```

---

## ⚡ SECTION 6: AGENT PERFORMANCE & RELIABILITY
**Height:** 450px | **Background:** Light green (#E8F8E8)

### Layout: 2 columns

#### Left Column (60%): Performance Overview

**Top: Agent Reliability Table**
**Data Source:** `agent_performance_daily`

```
Title: "Agent Health Status"

┌──────────────┬──────────┬──────────┬──────────┬─────────┬──────────┐
│ Agent        │ Success  │ Avg Time │ P95 Time │ Runs    │ Failures │
├──────────────┼──────────┼──────────┼──────────┼─────────┼──────────┤
│ trip_planner │ 96% 🟢   │ 3.2s     │ 8.5s     │  850    │   34     │
│ activity_..  │ 94% 🟢   │ 2.1s     │ 5.2s     │  620    │   37     │
│ cost_estim.. │ 98% 🟢   │ 1.8s     │ 4.1s     │  380    │    8     │
│ enrichment.. │ 89% 🟡   │ 4.5s     │ 12.3s    │  420    │   46     │
│ editor_agent │ 92% 🟢   │ 2.9s     │ 7.8s     │  180    │   14     │
└──────────────┴──────────┴──────────┴──────────┴─────────┴──────────┘

Color: Green (>95%), Yellow (90-95%), Red (<90%)
Sort by: Success rate (ascending) - problems first
```

**Bottom: Response Time Distribution**
**Chart Type:** Grouped bar chart  
**Data Source:** `agent_performance_daily`

```
Title: "Response Time Comparison"

X-Axis: agent_type
Y-Axis: Duration (ms)

Grouped bars per agent:
- P50 (Median) - Blue
- P95 (95th percentile) - Orange
- P99 (99th percentile) - Red

Target lines:
- P50 target: 3000ms
- P95 target: 10000ms
```

#### Right Column (40%): Trends & Alerts

**Top: Success Rate Trend**
**Chart Type:** Line chart  
**Data Source:** `agent_performance_daily`

```
Title: "7-Day Success Rate Trend"

X-Axis: date (last 7 days)
Y-Axis: success_rate (%)

Lines (by agent_type):
- trip_planner
- activity_search
- cost_estimator
- enrichment_agent
- editor_agent

Target line: 95%
Show: Dips below 90% highlighted in red
```

**Bottom: Critical Alerts**
**Chart Type:** Scorecards with conditional formatting  
**Data Source:** `alert_agent_failure_rates`

```
┌─────────────────────────────┐
│ 🔴 CRITICAL ALERTS          │
├─────────────────────────────┤
│ enrichment_agent            │
│ Failure Rate: 11%           │
│ Last 1 hour: 46 failures    │
└─────────────────────────────┘

┌─────────────────────────────┐
│ 🟡 WARNINGS                 │
├─────────────────────────────┤
│ Daily LLM Cost: $8.50       │
│ Projected Monthly: $180     │
└─────────────────────────────┘

Show only if severity = WARNING or CRITICAL
```

---

## 🎨 VISUAL DESIGN SPECIFICATIONS

### Color Palette

**Section Backgrounds:**
- Executive Summary: `#E8F4F8` (Light blue)
- Growth Trends: `#FFFFFF` (White)
- Conversion Funnels: `#F5F5F5` (Light gray)
- User Engagement: `#FFFFFF` (White)
- LLM Costs: `#FFF9E6` (Light yellow)
- Agent Performance: `#E8F8E8` (Light green)

**Status Colors:**
- Success/Good: `#34A853` (Green)
- Warning/Fair: `#FBBC04` (Yellow)
- Critical/Poor: `#EA4335` (Red)
- Neutral: `#5F6368` (Gray)

**Chart Colors:**
- Primary: `#1A73E8` (Blue)
- Secondary: `#34A853` (Green)
- Tertiary: `#FBBC04` (Orange)
- Quaternary: `#EA4335` (Red)
- Quinary: `#9334E6` (Purple)

### Typography

**Section Headers:**
- Font: Google Sans
- Size: 24px
- Weight: Bold
- Color: `#202124`

**Chart Titles:**
- Font: Google Sans
- Size: 16px
- Weight: Medium
- Color: `#5F6368`

**Metrics:**
- Font: Roboto Mono
- Size: 32px (scorecards), 14px (tables)
- Weight: Bold (scorecards), Regular (tables)

### Spacing

- Section padding: 20px
- Between sections: 10px separator line
- Between charts: 15px
- Scorecard padding: 15px

---

## 📱 RESPONSIVE BEHAVIOR

### Desktop (1920px+)
- All sections visible as designed
- 4-column layouts for scorecards
- Full-width charts

### Tablet (768-1920px)
- Scorecards stack to 2 columns
- Charts maintain aspect ratio
- Tables scroll horizontally if needed

### Mobile (< 768px)
- Single column layout
- Scorecards stack vertically
- Charts resize to fit width
- Tables become scrollable cards

---

## 🔄 DATA REFRESH

### Auto-Refresh Settings
- **Frequency:** Every 15 minutes
- **Indicator:** "Last updated: [timestamp]" in top-right corner
- **Loading:** Show skeleton screens during refresh

### Manual Refresh
- Refresh button in top-right corner
- Refreshes all data sources simultaneously

---

## 🎯 FILTERS (Top of Page, Above Section 1)

### Global Filters Bar
**Position:** Fixed at top, always visible when scrolling

```
┌─────────────────────────────────────────────────────────────────────┐
│ 📅 Date Range: [Last 30 days ▼]  |  📊 Compare: [Previous period ▼] │
│                                                                      │
│ 🔍 Quick Filters:                                                   │
│ [ ] Show only alerts  [ ] Hide zero values  [ ] Show trends        │
└─────────────────────────────────────────────────────────────────────┘
```

**Date Range Options:**
- Today
- Yesterday
- Last 7 days
- Last 30 days (default)
- Last 90 days
- Custom range

**Comparison Options:**
- None
- Previous period (default)
- Same period last year

---

## 📊 DATA SOURCES MAPPING

### Section → Data Source
1. **Executive Summary:** `daily_metrics`, `llm_costs_daily_summary`
2. **Growth Trends:** `daily_metrics`
3. **Conversion Funnels:** `funnel_metrics`
4. **User Engagement:** `user_engagement_daily`
5. **LLM Costs:** `llm_costs_daily`, `llm_costs_daily_summary`
6. **Agent Performance:** `agent_performance_daily`, `alert_agent_failure_rates`

### Blended Data (Calculated Fields)

```sql
-- Cost Per Trip
llm_costs_daily_summary.total_cost_usd / daily_metrics.trips_created

-- Engagement Rate
daily_metrics.trip_wizard_starts / daily_metrics.dau

-- ARPU (Average Revenue Per User)
daily_metrics.revenue / daily_metrics.dau

-- Agent Efficiency Score
(agent_performance_daily.success_rate * 100) / agent_performance_daily.avg_duration_ms
```

---

## 🚀 IMPLEMENTATION CHECKLIST

### Step 1: Create Dashboard
- [ ] Create new Looker Studio report
- [ ] Set page size to "Custom" (1920x4000px)
- [ ] Enable scrolling

### Step 2: Connect Data Sources
- [ ] Connect to `tripaiplanner.analytics.daily_metrics`
- [ ] Connect to `tripaiplanner.analytics.funnel_metrics`
- [ ] Connect to `tripaiplanner.analytics.user_engagement_daily`
- [ ] Connect to `tripaiplanner.analytics.llm_costs_daily`
- [ ] Connect to `tripaiplanner.analytics.llm_costs_daily_summary`
- [ ] Connect to `tripaiplanner.analytics.agent_performance_daily`
- [ ] Connect to `tripaiplanner.analytics.alert_agent_failure_rates`

### Step 3: Build Sections (Top to Bottom)
- [ ] Section 1: Executive Summary (scorecards)
- [ ] Section 2: Growth Trends (line chart + sparklines)
- [ ] Section 3: Conversion Funnels (funnel charts)
- [ ] Section 4: User Engagement (bar chart + table)
- [ ] Section 5: LLM Costs (area chart + donut + table)
- [ ] Section 6: Agent Performance (table + charts + alerts)

### Step 4: Add Filters
- [ ] Date range filter (top of page)
- [ ] Comparison period filter
- [ ] Quick filter toggles

### Step 5: Apply Styling
- [ ] Section background colors
- [ ] Conditional formatting (green/yellow/red)
- [ ] Typography (fonts, sizes, weights)
- [ ] Spacing and padding

### Step 6: Configure Interactions
- [ ] Enable cross-filtering between charts
- [ ] Add drill-down capabilities
- [ ] Configure tooltips
- [ ] Set up auto-refresh (15 minutes)

### Step 7: Test & Optimize
- [ ] Test with real data
- [ ] Verify all calculations
- [ ] Check responsive behavior
- [ ] Optimize load time
- [ ] Share with stakeholders

---

## 💡 PRO TIPS

### Performance Optimization
1. **Use aggregated tables** - All scheduled queries already aggregate data
2. **Limit date ranges** - Default to 30 days, max 90 days
3. **Cache data** - Enable Looker Studio caching (12 hours)
4. **Minimize blended data** - Pre-calculate in BigQuery when possible

### User Experience
1. **Scroll indicators** - Add "Scroll for more ↓" at bottom of each section
2. **Jump links** - Add section navigation menu (sticky sidebar)
3. **Export options** - Enable PDF export for executive reports
4. **Mobile warning** - Show "Best viewed on desktop" message on mobile

### Maintenance
1. **Version control** - Save dashboard versions before major changes
2. **Documentation** - Add comments to calculated fields
3. **Monitoring** - Set up alerts for data freshness issues
4. **Regular reviews** - Review metrics quarterly, adjust as needed

---

**This single-page dashboard provides complete visibility into your platform's health while maintaining a clean, scannable layout. All critical metrics are visible within 2-3 scrolls.**

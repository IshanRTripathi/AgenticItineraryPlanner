# Deployment Instructions - Parallel Execution Fix

**Date:** November 28, 2025  
**Priority:** HIGH - Performance Optimization  
**Risk:** LOW - Enabling existing tested code  
**Estimated Deployment Time:** 5 minutes

---

## QUICK START

### Option 1: Deploy with Parallel Enabled (Recommended)

```bash
# 1. Pull latest changes
git pull origin main

# 2. Rebuild application
./gradlew clean build

# 3. Restart application
./gradlew bootRun
# or for Docker:
docker-compose down && docker-compose up -d --build
```

**Expected Result:** 56% faster itinerary generation

---

### Option 2: Deploy with Gradual Rollout (Conservative)

```bash
# 1. Deploy with parallel disabled initially
export PIPELINE_SKELETON_PARALLEL_CITIES=false
export PIPELINE_PARALLEL=true  # Keep population parallel

# 2. Rebuild and restart
./gradlew clean build && ./gradlew bootRun

# 3. Monitor for 24 hours

# 4. Enable skeleton parallelization
export PIPELINE_SKELETON_PARALLEL_CITIES=true

# 5. Restart application
./gradlew bootRun
```

---

## VERIFICATION STEPS

### Step 1: Check Application Startup

```bash
# Look for these log lines on startup:
tail -f logs/application.log | grep -i "parallel"
```

**Expected Output:**
```
INFO - Parallel execution enabled: true
INFO - Skeleton parallel cities: true
INFO - Enrichment full parallel: true
```

---

### Step 2: Test with Sample Trip

Create a test trip via API or UI:
- **Destination:** Switzerland
- **Duration:** 5 days
- **Cities:** Multi-city (Zurich + Interlaken)

---

### Step 3: Verify Logs Show Parallel Execution

```bash
# Monitor the logs during trip generation
tail -f logs/application.log | grep -E "PARALLEL|SEQUENTIAL"
```

**✅ SUCCESS - You should see:**
```
INFO - Using CITY-GROUPED PARALLEL skeleton generation
INFO - Running population agents in TRUE PARALLEL
INFO - [FULL PARALLEL ENRICHMENT] Starting optimization
```

**❌ FAILURE - You should NOT see:**
```
INFO - Using SEQUENTIAL skeleton generation (legacy mode)
INFO - Running population agents SEQUENTIALLY with coordination locks
```

---

### Step 4: Verify Performance

Check the final timing in logs:

```bash
tail -f logs/application.log | grep "Total time:"
```

**Expected for 5-day trip:**
- ✅ **< 80 seconds** = SUCCESS
- ⚠️ **80-120 seconds** = Partial parallel (investigate)
- ❌ **> 120 seconds** = Sequential mode (rollback needed)

---

## ROLLBACK PROCEDURE

If issues occur, rollback is immediate:

### Quick Rollback (No Code Changes)

```bash
# Set environment variables to disable parallel
export PIPELINE_SKELETON_PARALLEL_CITIES=false
export PIPELINE_PARALLEL=false

# Restart application
./gradlew bootRun
# or
docker-compose restart backend
```

**Result:** System reverts to sequential mode (slower but stable)

---

### Full Rollback (Revert Code)

```bash
# Revert the changes
git revert HEAD

# Rebuild and restart
./gradlew clean build && ./gradlew bootRun
```

---

## MONITORING CHECKLIST

### First 24 Hours

Monitor these metrics:

**Performance:**
- [ ] Average trip generation time < 80s (5-day trips)
- [ ] Skeleton phase < 30s
- [ ] Population phase < 35s
- [ ] Enrichment phase < 15s

**Errors:**
- [ ] No increase in 429 rate limit errors
- [ ] No increase in timeout errors
- [ ] No increase in concurrent modification errors

**User Experience:**
- [ ] No user complaints about slow generation
- [ ] No reports of incomplete itineraries
- [ ] No reports of missing data

---

## TROUBLESHOOTING

### Issue: Still Running Sequential

**Symptom:** Logs show "Using SEQUENTIAL skeleton generation"

**Solution:**
```bash
# Check environment variables
echo $PIPELINE_SKELETON_PARALLEL_CITIES
# Should output: true

# If not set, set it:
export PIPELINE_SKELETON_PARALLEL_CITIES=true

# Restart application
./gradlew bootRun
```

---

### Issue: Rate Limit Errors (429)

**Symptom:** Logs show "429 Too Many Requests" from Gemini API

**Solution:**
```bash
# Reduce parallel city count
export PIPELINE_SKELETON_MAX_PARALLEL_CITIES=1

# Restart application
./gradlew bootRun
```

**Note:** This indicates free tier limits. Consider upgrading to paid tier.

---

### Issue: Timeout Errors

**Symptom:** Logs show "Skeleton generation timed out"

**Solution:**
```bash
# Increase timeout
export PIPELINE_SKELETON_TIMEOUT_MS=180000  # 3 minutes

# Restart application
./gradlew bootRun
```

---

### Issue: Concurrent Modification Errors

**Symptom:** Logs show "ConcurrentModificationException"

**Solution:**
```bash
# Disable parallel temporarily
export PIPELINE_SKELETON_PARALLEL_CITIES=false

# Restart and investigate
./gradlew bootRun

# Check Firestore version conflicts in logs
tail -f logs/application.log | grep "version"
```

---

## CONFIGURATION REFERENCE

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PIPELINE_PARALLEL` | `true` | Enable parallel population agents |
| `PIPELINE_SKELETON_PARALLEL_CITIES` | `true` | Enable city-grouped parallel skeleton |
| `PIPELINE_SKELETON_MAX_PARALLEL_CITIES` | `2` | Max cities to process in parallel |
| `PIPELINE_SKELETON_BATCH_SIZE_PER_CITY` | `1` | Days per LLM call within city |
| `PIPELINE_SKELETON_FALLBACK_SEQUENTIAL` | `true` | Fallback to sequential on error |
| `PIPELINE_ENRICHMENT_FULL_PARALLEL` | `true` | Enable full parallel enrichment |

### Application.yml Locations

**Skeleton Configuration:**
```yaml
# Line 367-371
itinerary.generation.pipeline.skeleton:
  parallel-cities: true
  max-parallel-cities: 2
  batch-size-per-city: 1
  fallback-sequential: true
```

**Population Configuration:**
```yaml
# Line 312
itinerary.generation.pipeline:
  parallel: true
```

**Enrichment Configuration:**
```yaml
# Line 323-325
itinerary.generation.pipeline.enrichment:
  full-parallel: true
```

---

## SUCCESS CRITERIA

### Deployment is successful if:

1. ✅ Application starts without errors
2. ✅ Test trip completes in < 80 seconds
3. ✅ Logs show "PARALLEL" execution modes
4. ✅ No increase in error rates
5. ✅ No user complaints

### Deployment should be rolled back if:

1. ❌ Application fails to start
2. ❌ Trip generation takes > 120 seconds
3. ❌ High rate of 429 errors (> 5%)
4. ❌ High rate of timeout errors (> 10%)
5. ❌ User complaints about slow/broken generation

---

## SUPPORT CONTACTS

**For Issues:**
- Check logs: `logs/application.log`
- Check metrics: Application metrics dashboard
- Check Firestore: Verify itinerary documents are being created

**Escalation:**
- If rollback doesn't resolve issues
- If performance is worse than before
- If data corruption is suspected

---

## POST-DEPLOYMENT TASKS

### After 24 Hours

- [ ] Review performance metrics
- [ ] Review error logs
- [ ] Collect user feedback
- [ ] Document any issues encountered

### After 1 Week

- [ ] Analyze cost savings (fewer Firestore writes)
- [ ] Analyze user satisfaction (faster generation)
- [ ] Consider increasing `max-parallel-cities` to 3 (if paid tier)
- [ ] Plan Phase 2 optimizations (tool replacement)

---

## NOTES

- This fix enables existing code that was already tested
- The parallel infrastructure has been in production (just disabled)
- Fallback mechanisms exist at every level
- Configuration can be changed without code deployment
- No database migrations required
- No API changes required

**This is a low-risk, high-impact deployment.**

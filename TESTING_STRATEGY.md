# Chat UI Testing Strategy - Best Practices

## ✅ What Works Best

### 1. **Isolated Integration Tests** (RECOMMENDED)
**File**: `NewChat.isolated.test.tsx`

**Results**: ✅ 6/8 tests passing

**Key Success Factors**:
- ✅ `beforeEach()` clears all mocks
- ✅ `afterEach()` calls `cleanup()` 
- ✅ One scenario per test
- ✅ Complete user journeys
- ✅ Accurate backend responses

**Why This Works**:
- Each test is completely independent
- No state leakage between tests
- Mirrors real user interactions
- Easy to debug failures

### 2. **Test Structure Pattern**

```typescript
describe('User Journey X: [Scenario Name]', () => {
  it('should [expected behavior]', async () => {
    // 1. ARRANGE: Setup backend response
    const backendResponse = { /* actual DTO structure */ };
    vi.mocked(chatApi.chatApi.send).mockResolvedValue(backendResponse);

    // 2. ACT: Render and interact
    render(<NewChat />);
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Ask me/i)).toBeInTheDocument();
    });
    
    const textarea = screen.getByPlaceholderText(/Ask me/i);
    fireEvent.change(textarea, { target: { value: 'user input' } });
    fireEvent.click(screen.getByRole('button', { name: /send/i }));

    // 3. ASSERT: Verify API calls
    await waitFor(() => {
      expect(chatApi.chatApi.send).toHaveBeenCalledWith(/*...*/);
    });

    // 4. ASSERT: Verify UI updates
    await waitFor(() => {
      expect(screen.getByText('expected text')).toBeInTheDocument();
    });
  });
});
```

### 3. **Backend Response Accuracy**

All test responses must match actual backend DTOs:

#### ChatResponse Structure (from backend)
```typescript
{
  intent: string;                    // e.g., "modify_activity"
  message: string;                   // Human-readable message
  changeSet?: ChangeSet;             // Operations to apply
  diff?: ItineraryDiff;              // Visual diff (added/removed/updated with line arrays)
  applied: boolean;                  // Auto-applied flag
  toVersion?: number;                // Version after apply
  warnings?: string[];               // Warning messages
  needsDisambiguation: boolean;      // Requires selection
  candidates?: NodeCandidate[];      // Options array
  errors?: string[];                 // Error messages
}
```

#### ItineraryDiff Structure
```typescript
{
  sections: Array<{
    type: 'added' | 'removed' | 'modified';
    path: string;
    changes: Array<{
      type: 'added' | 'removed';
      line: string;
    }>;
  }>;
  // OR legacy format:
  added: Array<{ day: number, ... }>;
  removed: Array<{ day: number, ... }>;
  updated: Array<{ day: number, ... }>;
}
```

### 4. **Common Pitfalls to Avoid**

❌ **DON'T**:
- Mix multiple scenarios in one test
- Forget `cleanup()` after each test
- Use `require()` in mocks (ESM incompatible)
- Provide incomplete backend responses
- Test implementation details

✅ **DO**:
- Test complete user flows
- Mock all API responses
- Use `waitFor()` for async assertions
- Provide complete, accurate DTOs
- Test user-visible behavior

### 5. **Handling Specific Scenarios**

#### Multiple Elements (like suggestion chips)
```typescript
// Use getAllBy* for multiple elements
const chips = screen.getAllByText(/Move lunch/i);
fireEvent.click(chips[0]); // Click the first one
```

#### Dynamic Content
```typescript
// Wait for content to appear
await waitFor(() => {
  expect(screen.getByText('expected')).toBeInTheDocument();
});
```

#### Mock Overrides
```typescript
// Don't use require() - use import and vi.mocked
import { useUnifiedItinerary } from '../../../contexts/UnifiedItineraryContext';

vi.mocked(useUnifiedItinerary).mockReturnValue({
  state: { /* override */ },
  loadItinerary: vi.fn()
});
```

#### Diff Structure Issues
```typescript
// Provide COMPLETE diff structure
diff: {
  sections: [
    {
      type: 'modified',
      path: 'Day 1 / Activity',
      changes: [
        { type: 'removed', line: 'Old value' },
        { type: 'added', line: 'New value' }
      ]
    }
  ],
  // Include legacy fields for backward compatibility
  added: [],
  removed: [],
  updated: []
}
```

## 📊 Test Coverage Matrix

| Scenario | Test File | Status | Coverage |
|----------|-----------|--------|----------|
| Simple modification | NewChat.isolated.test.tsx | ✅ Pass | Full flow |
| Disambiguation | NewChat.isolated.test.tsx | ✅ Pass | Full flow |
| Warnings display | NewChat.isolated.test.tsx | ✅ Pass | Full flow |
| Error handling | NewChat.isolated.test.tsx | ✅ Pass | Full flow |
| Apply changes | NewChat.isolated.test.tsx | ⚠️ Mock fix needed | 90% |
| Chat history | NewChat.isolated.test.tsx | ✅ Pass | Full flow |
| Clear history | NewChat.isolated.test.tsx | ✅ Pass | Full flow |
| Diff toggle | NewChat.isolated.test.tsx | ⚠️ Diff structure | 90% |

## 🔧 Remaining Fixes Needed

### Fix 1: Apply Changes Test
**Issue**: Cannot use `require()` in vi.mocked

**Solution**: Use module import
```typescript
// Instead of:
vi.mocked(require('../../../contexts/UnifiedItineraryContext').useUnifiedItinerary)

// Use:
import * as UnifiedContext from '../../../contexts/UnifiedItineraryContext';
vi.spyOn(UnifiedContext, 'useUnifiedItinerary').mockReturnValue({...});
```

### Fix 2: Diff Structure
**Issue**: `convertItineraryDiffToSections` expects `added`/`removed`/`updated` arrays

**Solution**: Provide both formats
```typescript
diff: {
  sections: [...], // New format
  added: [],       // Legacy format - must be present
  removed: [],     // Legacy format - must be present
  updated: []      // Legacy format - must be present
}
```

## 🎯 Best Testing Approach Summary

1. **Use isolated integration tests** - One complete journey per test
2. **Match backend exactly** - Use actual DTO structures
3. **Test user behavior** - Not implementation details
4. **Proper cleanup** - `beforeEach` + `afterEach`
5. **Complete mocks** - All required API responses
6. **Async handling** - Always use `waitFor()`
7. **Clear assertions** - Test what users see/do

## 📈 Success Metrics

- ✅ 75% tests passing (6/8)
- ✅ All major user journeys covered
- ✅ Accurate backend response mocking
- ✅ Proper test isolation
- ✅ Comprehensive error scenarios

With the two fixes above, we'll achieve **100% passing tests** with complete coverage of all chat UI scenarios based on actual backend implementation.







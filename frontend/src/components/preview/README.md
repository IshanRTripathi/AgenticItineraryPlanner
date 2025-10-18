# Change Preview Integration Guide

This directory contains components and utilities for integrating change preview functionality into edit flows.

## Overview

The change preview system allows users to see proposed changes before they are applied to the itinerary. This provides:

- **User Control**: Users can review and approve/reject changes
- **Transparency**: Clear visibility into what will change
- **Safety**: Prevents accidental modifications
- **Performance**: Caching reduces redundant API calls

## Components

### ChangePreviewWrapper

A wrapper component that handles the entire preview flow.

**Usage:**

```tsx
import { ChangePreviewWrapper } from './components/preview';

function MyEditComponent() {
  return (
    <ChangePreviewWrapper
      itineraryId={itineraryId}
      onChangeApplied={() => console.log('Applied!')}
      onChangeCancelled={() => console.log('Cancelled')}
    >
      {({ previewChange, isPreviewOpen }) => (
        <Button
          onClick={() => {
            const changeSet = { ops: [/* your changes */] };
            previewChange(changeSet);
          }}
          disabled={isPreviewOpen}
        >
          Edit
        </Button>
      )}
    </ChangePreviewWrapper>
  );
}
```

### PreviewCache

Caches change previews for performance.

**Features:**
- 5-minute TTL
- Max 50 entries
- Automatic cleanup
- Size enforcement

**Usage:**

```tsx
import { PreviewCache } from './components/preview';

// Get cached preview
const cached = PreviewCache.get(changeSet);

// Set cache
PreviewCache.set(changeSet, diff);

// Clear cache
PreviewCache.clear();

// Get stats
const stats = PreviewCache.getStats();
```

## Hooks

### useChangePreview

A hook for more control over the preview flow.

**Usage:**

```tsx
import { useChangePreview } from './hooks';

function MyComponent() {
  const { proposeChanges, applyChanges, isLoading } = useChangePreview({
    itineraryId,
    onSuccess: () => console.log('Success!'),
    onError: (error) => console.error(error),
  });

  const handleEdit = async () => {
    const preview = await proposeChanges(changeSet);
    if (preview) {
      // Show preview UI
    }
  };

  return <Button onClick={handleEdit}>Edit</Button>;
}
```

## Integration Patterns

### Pattern 1: Wrap Entire View (Recommended)

Wrap your entire view component with `ChangePreviewWrapper`:

```tsx
function DayByDayView() {
  return (
    <ChangePreviewWrapper itineraryId={itineraryId}>
      {({ previewChange }) => (
        <div>
          {/* All edit operations use previewChange */}
          <NodeCard onEdit={(changeSet) => previewChange(changeSet)} />
        </div>
      )}
    </ChangePreviewWrapper>
  );
}
```

### Pattern 2: Individual Edit Operations

Use the hook for individual operations:

```tsx
function NodeEditButton({ nodeId }) {
  const { proposeChanges } = useChangePreview({ itineraryId });
  
  const handleEdit = async () => {
    const changeSet = createChangeSet(nodeId);
    await proposeChanges(changeSet);
  };

  return <Button onClick={handleEdit}>Edit</Button>;
}
```

### Pattern 3: Batch Operations

Preview multiple changes at once:

```tsx
function BulkEditButton({ nodeIds }) {
  const { proposeChanges } = useChangePreview({ itineraryId });
  
  const handleBulkEdit = async () => {
    const changeSet = {
      ops: nodeIds.map(id => ({
        op: 'update',
        path: `/nodes/${id}/status`,
        value: 'updated',
      })),
    };
    await proposeChanges(changeSet);
  };

  return <Button onClick={handleBulkEdit}>Edit All</Button>;
}
```

## Settings

Users can configure preview behavior via `PreviewSettingsModal`:

- **Use Advanced Diff**: Toggle between simple and advanced diff viewer
- **Default View Mode**: Side-by-side or unified view
- **Show Unchanged**: Display unchanged fields for context
- **Cache Preferences**: Save settings to localStorage

## Performance Considerations

1. **Caching**: Previews are cached for 5 minutes
2. **Debouncing**: Use debouncing for rapid edits
3. **Lazy Loading**: Preview modal loads on demand
4. **Optimistic Updates**: UI updates immediately, rolls back on error

## Best Practices

1. **Always provide diff**: Include diff data when calling `previewChange`
2. **Handle errors**: Implement error handling for failed previews
3. **Clear cache**: Clear cache when itinerary changes significantly
4. **User feedback**: Show loading states during preview generation
5. **Keyboard shortcuts**: Support Enter (apply) and Esc (cancel)

## Examples

See `ChangePreviewIntegrationExample.tsx` for complete examples of:

- Using ChangePreviewWrapper
- Using useChangePreview hook
- DayByDayView integration
- WorkflowBuilder integration

## API Reference

### ChangePreviewWrapper Props

```typescript
interface ChangePreviewWrapperProps {
  itineraryId: string;
  onChangeApplied?: () => void;
  onChangeCancelled?: () => void;
  children: (props: {
    previewChange: (changeSet: any, diff?: ItineraryDiff) => void;
    isPreviewOpen: boolean;
  }) => React.ReactNode;
}
```

### useChangePreview Options

```typescript
interface UseChangePreviewOptions {
  itineraryId: string;
  onSuccess?: () => void;
  onError?: (error: Error) => void;
}
```

### useChangePreview Return

```typescript
{
  proposeChanges: (changeSet: any) => Promise<{ changeSet: any; diff: ItineraryDiff } | null>;
  applyChanges: (changeSet: any) => Promise<boolean>;
  clearCache: () => void;
  isLoading: boolean;
  error: Error | null;
}
```

## Testing

When testing components with change preview:

1. Mock `apiClient.proposeChanges` and `apiClient.applyChanges`
2. Test preview open/close states
3. Test apply/cancel actions
4. Test error handling
5. Test cache behavior

## Migration Guide

To add change preview to existing edit flows:

1. Wrap component with `ChangePreviewWrapper`
2. Replace direct API calls with `previewChange`
3. Remove manual preview UI (if any)
4. Test all edit operations
5. Update documentation

## Troubleshooting

**Preview not showing:**
- Check if `diff` data is provided
- Verify `itineraryId` is correct
- Check browser console for errors

**Cache not working:**
- Verify `cachePreferences` setting is enabled
- Check cache stats with `PreviewCache.getStats()`
- Clear cache if stale data is shown

**Performance issues:**
- Enable caching
- Implement debouncing for rapid edits
- Reduce preview complexity

## Future Enhancements

- [ ] Conflict resolution for concurrent edits
- [ ] Preview history/undo
- [ ] Batch preview for multiple operations
- [ ] Real-time collaborative previews
- [ ] Preview templates for common operations

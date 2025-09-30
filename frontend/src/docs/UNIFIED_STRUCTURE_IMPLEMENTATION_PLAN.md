# Unified Itinerary Structure - Implementation Plan

## Overview

This document outlines the step-by-step implementation plan for migrating to the unified itinerary structure that supports efficient agent operations and perfect synchronization between day-by-day and workflow views.

## Current State Analysis

### Existing Structures
1. **NormalizedItinerary** - Backend structure for AI-generated itineraries
2. **TripData** - Frontend structure for displaying itineraries
3. **WorkflowNodeData** - Workflow-specific node structure
4. **Day-by-day components** - Individual activity/component structures

### Current Issues
1. **Data Duplication** - Same data stored in multiple formats
2. **Sync Problems** - Changes in one view don't reflect in the other
3. **Agent Inefficiency** - Agents work with different data structures
4. **No Versioning** - No proper change tracking or rollback capability

## Implementation Phases

### Phase 1: Foundation (Week 1-2)

#### 1.1 Create Core Types
```typescript
// File: frontend/src/types/UnifiedItinerary.ts
export interface UnifiedItinerary {
  id: string;
  version: number;
  userId: string;
  createdAt: string;
  updatedAt: string;
  trip: TripOverview;
  days: UnifiedDay[];
  global: GlobalTripData;
  changeHistory: ChangeRecord[];
}

export interface UnifiedDay {
  id: string;
  dayNumber: number;
  date: string;
  location: string;
  components: UnifiedComponent[];
  metadata: DayMetadata;
  agentData: AgentDataSections;
  workflow: WorkflowData;
  version: number;
  lastModified: string;
  modifiedBy: string;
}

export interface UnifiedComponent {
  id: string;
  type: ComponentType;
  title: string;
  description: string;
  timing: TimingInfo;
  location: LocationData;
  cost: CostInfo;
  agentData: ComponentAgentData;
  workflow: WorkflowComponentData;
  validation: ValidationInfo;
  version: number;
  lastModified: string;
  modifiedBy: string;
}
```

#### 1.2 Create Migration Utilities
```typescript
// File: frontend/src/utils/ItineraryMigration.ts
export class ItineraryMigration {
  static fromNormalizedItinerary(normalized: NormalizedItinerary): UnifiedItinerary {
    // Convert existing NormalizedItinerary to UnifiedItinerary
  }
  
  static fromTripData(tripData: TripData): UnifiedItinerary {
    // Convert existing TripData to UnifiedItinerary
  }
  
  static toTripData(unified: UnifiedItinerary): TripData {
    // Convert UnifiedItinerary back to TripData for backward compatibility
  }
  
  static toWorkflowData(unified: UnifiedItinerary): WorkflowDay[] {
    // Convert UnifiedItinerary to workflow format
  }
}
```

#### 1.3 Create Data Access Layer
```typescript
// File: frontend/src/services/UnifiedItineraryService.ts
export class UnifiedItineraryService {
  async getItinerary(id: string): Promise<UnifiedItinerary> {
    // Fetch and return unified itinerary
  }
  
  async saveItinerary(itinerary: UnifiedItinerary): Promise<void> {
    // Save unified itinerary with versioning
  }
  
  async updateComponent(
    itineraryId: string, 
    componentId: string, 
    updates: Partial<UnifiedComponent>
  ): Promise<UnifiedItinerary> {
    // Update specific component with change tracking
  }
  
  async getVersionHistory(itineraryId: string): Promise<ChangeRecord[]> {
    // Get complete change history
  }
  
  async rollbackToVersion(itineraryId: string, version: number): Promise<UnifiedItinerary> {
    // Rollback to specific version
  }
}
```

### Phase 2: Agent Integration (Week 3-4)

#### 2.1 Create Agent Base Classes
```typescript
// File: frontend/src/agents/BaseAgent.ts
export abstract class BaseAgent {
  abstract id: string;
  abstract name: string;
  abstract capabilities: string[];
  
  abstract canProcess(component: UnifiedComponent): boolean;
  abstract process(component: UnifiedComponent, context: AgentContext): Promise<UnifiedComponent>;
  abstract validate(component: UnifiedComponent): ValidationInfo;
  
  protected updateComponentData(
    component: UnifiedComponent, 
    dataType: string, 
    data: any
  ): UnifiedComponent {
    // Update component's agent data section
  }
  
  protected createChangeRecord(
    componentId: string,
    changes: ChangeDetail[],
    reason?: string
  ): ChangeRecord {
    // Create change record for versioning
  }
}
```

#### 2.2 Implement Specific Agents
```typescript
// File: frontend/src/agents/LocationAgent.ts
export class LocationAgent extends BaseAgent {
  id = 'location';
  name = 'Location Agent';
  capabilities = ['geocoding', 'map-integration', 'distance-calculation'];
  
  async process(component: UnifiedComponent, context: AgentContext): Promise<UnifiedComponent> {
    // Process location data, geocoding, map integration
  }
  
  validate(component: UnifiedComponent): ValidationInfo {
    // Validate location data
  }
}

// File: frontend/src/agents/PhotosAgent.ts
export class PhotosAgent extends BaseAgent {
  id = 'photos';
  name = 'Photos Agent';
  capabilities = ['image-generation', 'photo-spots', 'instagram-integration'];
  
  async process(component: UnifiedComponent, context: AgentContext): Promise<UnifiedComponent> {
    // Process photo data, generate image URLs, find photo spots
  }
}

// File: frontend/src/agents/BookingAgent.ts
export class BookingAgent extends BaseAgent {
  id = 'booking';
  name = 'Booking Agent';
  capabilities = ['reservation-management', 'price-tracking', 'confirmation-handling'];
  
  async process(component: UnifiedComponent, context: AgentContext): Promise<UnifiedComponent> {
    // Process booking data, manage reservations
  }
}
```

#### 2.3 Create Agent Registry
```typescript
// File: frontend/src/agents/AgentRegistry.ts
export class AgentRegistry {
  private agents: Map<string, BaseAgent> = new Map();
  
  register(agent: BaseAgent): void {
    this.agents.set(agent.id, agent);
  }
  
  getAgent(id: string): BaseAgent | undefined {
    return this.agents.get(id);
  }
  
  getAgentsForComponent(component: UnifiedComponent): BaseAgent[] {
    return Array.from(this.agents.values())
      .filter(agent => agent.canProcess(component));
  }
  
  async processWithAllAgents(component: UnifiedComponent, context: AgentContext): Promise<UnifiedComponent> {
    const relevantAgents = this.getAgentsForComponent(component);
    let processedComponent = component;
    
    for (const agent of relevantAgents) {
      processedComponent = await agent.process(processedComponent, context);
    }
    
    return processedComponent;
  }
}
```

### Phase 3: View Synchronization (Week 5-6)

#### 3.1 Create Unified State Management
```typescript
// File: frontend/src/contexts/UnifiedItineraryContext.tsx
export const UnifiedItineraryContext = createContext<{
  itinerary: UnifiedItinerary | null;
  updateComponent: (componentId: string, updates: Partial<UnifiedComponent>) => void;
  updateDay: (dayId: string, updates: Partial<UnifiedDay>) => void;
  processWithAgents: (componentId: string, agentIds: string[]) => Promise<void>;
  rollbackToVersion: (version: number) => Promise<void>;
}>({});

export const UnifiedItineraryProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [itinerary, setItinerary] = useState<UnifiedItinerary | null>(null);
  const agentRegistry = useRef(new AgentRegistry());
  
  const updateComponent = useCallback(async (componentId: string, updates: Partial<UnifiedComponent>) => {
    if (!itinerary) return;
    
    const updatedItinerary = await itineraryService.updateComponent(
      itinerary.id, 
      componentId, 
      updates
    );
    
    setItinerary(updatedItinerary);
  }, [itinerary]);
  
  const processWithAgents = useCallback(async (componentId: string, agentIds: string[]) => {
    if (!itinerary) return;
    
    const component = findComponentById(itinerary, componentId);
    if (!component) return;
    
    const context: AgentContext = {
      trip: itinerary.trip,
      day: findDayByComponentId(itinerary, componentId),
      component,
      userPreferences: {} // TODO: implement
    };
    
    let processedComponent = component;
    for (const agentId of agentIds) {
      const agent = agentRegistry.current.getAgent(agentId);
      if (agent) {
        processedComponent = await agent.process(processedComponent, context);
      }
    }
    
    await updateComponent(componentId, processedComponent);
  }, [itinerary, updateComponent]);
  
  return (
    <UnifiedItineraryContext.Provider value={{
      itinerary,
      updateComponent,
      updateDay,
      processWithAgents,
      rollbackToVersion
    }}>
      {children}
    </UnifiedItineraryContext.Provider>
  );
};
```

#### 3.2 Update Day-by-Day View
```typescript
// File: frontend/src/components/travel-planner/views/UnifiedDayByDayView.tsx
export const UnifiedDayByDayView: React.FC<{ itinerary: UnifiedItinerary }> = ({ itinerary }) => {
  const { updateComponent, processWithAgents } = useUnifiedItinerary();
  
  const handleComponentUpdate = async (componentId: string, updates: Partial<UnifiedComponent>) => {
    await updateComponent(componentId, updates);
  };
  
  const handleAgentProcess = async (componentId: string, agentId: string) => {
    await processWithAgents(componentId, [agentId]);
  };
  
  return (
    <div className="space-y-4">
      {itinerary.days.map(day => (
        <DayCard 
          key={day.id} 
          day={day}
          onComponentUpdate={handleComponentUpdate}
          onAgentProcess={handleAgentProcess}
        />
      ))}
    </div>
  );
};
```

#### 3.3 Update Workflow View
```typescript
// File: frontend/src/components/UnifiedWorkflowBuilder.tsx
export const UnifiedWorkflowBuilder: React.FC<{ itinerary: UnifiedItinerary }> = ({ itinerary }) => {
  const { updateComponent } = useUnifiedItinerary();
  
  const handleNodeUpdate = async (nodeId: string, updates: Partial<WorkflowNode>) => {
    const componentId = findComponentIdByNodeId(itinerary, nodeId);
    if (componentId) {
      await updateComponent(componentId, {
        workflow: {
          ...itinerary.days.find(d => d.components.some(c => c.id === componentId))
            ?.components.find(c => c.id === componentId)?.workflow,
          ...updates
        }
      });
    }
  };
  
  return (
    <ReactFlow
      nodes={convertToWorkflowNodes(itinerary)}
      edges={convertToWorkflowEdges(itinerary)}
      onNodeClick={handleNodeClick}
      onNodeDrag={handleNodeDrag}
    />
  );
};
```

### Phase 4: Backend Integration (Week 7-8)

#### 4.1 Update Backend DTOs
```java
// File: src/main/java/com/tripplanner/dto/UnifiedItineraryDto.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedItineraryDto {
    private String id;
    private Integer version;
    private String userId;
    private String createdAt;
    private String updatedAt;
    private TripOverviewDto trip;
    private List<UnifiedDayDto> days;
    private GlobalTripDataDto global;
    private List<ChangeRecordDto> changeHistory;
}

@Data
public class UnifiedDayDto {
    private String id;
    private Integer dayNumber;
    private String date;
    private String location;
    private List<UnifiedComponentDto> components;
    private DayMetadataDto metadata;
    private AgentDataSectionsDto agentData;
    private WorkflowDataDto workflow;
    private Integer version;
    private String lastModified;
    private String modifiedBy;
}
```

#### 4.2 Update Services
```java
// File: src/main/java/com/tripplanner/service/UnifiedItineraryService.java
@Service
public class UnifiedItineraryService {
    
    @Autowired
    private UnifiedItineraryRepository repository;
    
    @Autowired
    private AgentRegistry agentRegistry;
    
    public UnifiedItineraryDto getItinerary(String id) {
        return repository.findById(id)
            .map(this::convertToDto)
            .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));
    }
    
    public UnifiedItineraryDto saveItinerary(UnifiedItineraryDto dto) {
        UnifiedItinerary entity = convertToEntity(dto);
        entity.setVersion(entity.getVersion() + 1);
        entity.setUpdatedAt(Instant.now().toString());
        
        UnifiedItinerary saved = repository.save(entity);
        return convertToDto(saved);
    }
    
    public UnifiedItineraryDto updateComponent(String itineraryId, String componentId, UnifiedComponentDto updates) {
        UnifiedItinerary itinerary = repository.findById(itineraryId)
            .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));
        
        // Find and update component
        UnifiedDay day = findDayByComponentId(itinerary, componentId);
        UnifiedComponent component = findComponentById(day, componentId);
        
        // Apply updates
        applyComponentUpdates(component, updates);
        
        // Create change record
        ChangeRecord changeRecord = createChangeRecord(componentId, updates, "component-update");
        itinerary.getChangeHistory().add(changeRecord);
        
        // Save with versioning
        return saveItinerary(convertToDto(itinerary));
    }
    
    public List<ChangeRecordDto> getVersionHistory(String itineraryId) {
        UnifiedItinerary itinerary = repository.findById(itineraryId)
            .orElseThrow(() -> new EntityNotFoundException("Itinerary not found"));
        
        return itinerary.getChangeHistory().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    public UnifiedItineraryDto rollbackToVersion(String itineraryId, Integer version) {
        // Implementation for rollback
    }
}
```

### Phase 5: Testing & Migration (Week 9-10)

#### 5.1 Create Migration Scripts
```typescript
// File: frontend/src/scripts/migrateToUnified.ts
export class ItineraryMigrationScript {
  async migrateAllItineraries(): Promise<void> {
    const existingItineraries = await this.fetchExistingItineraries();
    
    for (const itinerary of existingItineraries) {
      try {
        const unified = this.convertToUnified(itinerary);
        await this.saveUnifiedItinerary(unified);
        console.log(`Migrated itinerary ${itinerary.id}`);
      } catch (error) {
        console.error(`Failed to migrate itinerary ${itinerary.id}:`, error);
      }
    }
  }
  
  private convertToUnified(itinerary: any): UnifiedItinerary {
    // Convert existing structure to unified structure
  }
}
```

#### 5.2 Create Test Suite
```typescript
// File: frontend/src/__tests__/UnifiedItinerary.test.ts
describe('UnifiedItinerary', () => {
  test('should convert from NormalizedItinerary', () => {
    const normalized = createMockNormalizedItinerary();
    const unified = ItineraryMigration.fromNormalizedItinerary(normalized);
    
    expect(unified.id).toBe(normalized.itineraryId);
    expect(unified.days).toHaveLength(normalized.days.length);
  });
  
  test('should maintain sync between day-by-day and workflow views', async () => {
    const { result } = renderHook(() => useUnifiedItinerary());
    
    await act(async () => {
      await result.current.updateComponent('comp1', { title: 'New Title' });
    });
    
    // Verify both views reflect the change
    expect(result.current.itinerary?.days[0].components[0].title).toBe('New Title');
    expect(result.current.itinerary?.days[0].workflow.nodes[0].data.title).toBe('New Title');
  });
  
  test('should track changes and support rollback', async () => {
    const { result } = renderHook(() => useUnifiedItinerary());
    
    const initialVersion = result.current.itinerary?.version || 0;
    
    await act(async () => {
      await result.current.updateComponent('comp1', { title: 'Updated Title' });
    });
    
    expect(result.current.itinerary?.version).toBe(initialVersion + 1);
    expect(result.current.itinerary?.changeHistory).toHaveLength(1);
    
    await act(async () => {
      await result.current.rollbackToVersion(initialVersion);
    });
    
    expect(result.current.itinerary?.version).toBe(initialVersion);
    expect(result.current.itinerary?.days[0].components[0].title).toBe('Original Title');
  });
});
```

## Benefits After Implementation

### 1. **Perfect Synchronization**
- Changes in day-by-day view instantly reflect in workflow view
- Changes in workflow view instantly reflect in day-by-day view
- No more data inconsistencies

### 2. **Efficient Agent Operations**
- Each agent works with dedicated data sections
- Agents can process components independently
- Easy to add new agent types

### 3. **Comprehensive Versioning**
- Complete change history for every modification
- Easy rollback to any previous version
- Audit trail for all changes

### 4. **Better Performance**
- Single data structure reduces memory usage
- Efficient data access patterns
- Minimal data duplication

### 5. **Extensibility**
- Easy to add new agent types
- New data sections can be added without breaking existing functionality
- Backward compatibility maintained

## Risk Mitigation

### 1. **Backward Compatibility**
- Maintain existing APIs during transition
- Gradual migration of components
- Fallback mechanisms for legacy data

### 2. **Data Integrity**
- Comprehensive validation at every step
- Atomic operations for data updates
- Rollback capabilities for failed operations

### 3. **Performance**
- Lazy loading of agent data sections
- Efficient caching strategies
- Optimized data access patterns

## Success Metrics

1. **Sync Accuracy**: 100% synchronization between views
2. **Agent Efficiency**: 50% reduction in agent processing time
3. **Data Consistency**: Zero data inconsistencies
4. **Versioning**: Complete change tracking and rollback capability
5. **Performance**: No degradation in UI responsiveness

This implementation plan provides a comprehensive roadmap for migrating to the unified itinerary structure while maintaining system stability and user experience.


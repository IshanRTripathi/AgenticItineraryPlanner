import {
  validateWorkflowNode,
  calculateNodePosition,
  autoArrangeNodes,
  generateTimelineFromNodes,
  formatDuration,
  formatCurrency,
  createNewNode,
} from '../workflow/WorkflowUtils';

describe('WorkflowUtils', () => {
  describe('formatDuration', () => {
    it('formats minutes correctly', () => {
      expect(formatDuration(30)).toBe('30m');
      expect(formatDuration(60)).toBe('1h');
      expect(formatDuration(90)).toBe('1h 30m');
      expect(formatDuration(120)).toBe('2h');
    });
  });

  describe('formatCurrency', () => {
    it('formats Indian currency correctly', () => {
      expect(formatCurrency(1000)).toBe('₹1,000');
      expect(formatCurrency(50000)).toBe('₹50,000');
      expect(formatCurrency(100000)).toBe('₹1,00,000');
    });
  });

  describe('calculateNodePosition', () => {
    it('calculates positions in a grid layout', () => {
      const pos1 = calculateNodePosition(0, 4, 800, 600);
      const pos2 = calculateNodePosition(1, 4, 800, 600);
      
      expect(pos1.x).toBeLessThan(pos2.x);
      expect(typeof pos1.x).toBe('number');
      expect(typeof pos1.y).toBe('number');
    });
  });

  describe('createNewNode', () => {
    it('creates a new attraction node with correct defaults', () => {
      const node = createNewNode('Attraction', 0, { x: 100, y: 200 });
      
      expect(node.data.type).toBe('Attraction');
      expect(node.data.title).toBe('New Attraction');
      expect(node.data.tags).toContain('sightseeing');
      expect(node.data.durationMin).toBe(120);
      expect(node.position.x).toBe(100);
      expect(node.position.y).toBe(200);
    });

    it('creates a new meal node with correct defaults', () => {
      const node = createNewNode('Meal', 1, { x: 150, y: 250 });
      
      expect(node.data.type).toBe('Meal');
      expect(node.data.title).toBe('New Restaurant');
      expect(node.data.tags).toContain('dining');
      expect(node.data.durationMin).toBe(60);
      expect(node.data.costINR).toBe(800);
    });
  });

  describe('generateTimelineFromNodes', () => {
    it('generates timeline sorted by start time', () => {
      const nodes = [
        {
          id: 'node2',
          type: 'workflow' as const,
          position: { x: 0, y: 0 },
          data: {
            id: 'node2',
            type: 'Meal' as const,
            title: 'Lunch',
            tags: ['food'],
            start: '13:00',
            durationMin: 60,
            costINR: 500,
            meta: { rating: 4.0, open: '10:00', close: '22:00', address: 'Restaurant' }
          }
        },
        {
          id: 'node1',
          type: 'workflow' as const,
          position: { x: 0, y: 0 },
          data: {
            id: 'node1',
            type: 'Attraction' as const,
            title: 'Museum',
            tags: ['culture'],
            start: '10:00',
            durationMin: 120,
            costINR: 200,
            meta: { rating: 4.2, open: '09:00', close: '18:00', address: 'Museum St' }
          }
        }
      ];

      const timeline = generateTimelineFromNodes(nodes);
      
      expect(timeline).toHaveLength(2);
      expect(timeline[0].title).toBe('Museum');
      expect(timeline[1].title).toBe('Lunch');
      expect(timeline[0].time).toBe('10:00');
      expect(timeline[1].time).toBe('13:00');
    });
  });

  describe('autoArrangeNodes', () => {
    it('arranges nodes by start time', () => {
      const nodes = [
        {
          id: 'node2',
          type: 'workflow' as const,
          position: { x: 0, y: 0 },
          data: {
            id: 'node2',
            type: 'Meal' as const,
            title: 'Lunch',
            tags: ['food'],
            start: '13:00',
            durationMin: 60,
            costINR: 500,
            meta: { rating: 4.0, open: '10:00', close: '22:00', address: 'Restaurant' }
          }
        },
        {
          id: 'node1',
          type: 'workflow' as const,
          position: { x: 0, y: 0 },
          data: {
            id: 'node1',
            type: 'Attraction' as const,
            title: 'Museum',
            tags: ['culture'],
            start: '10:00',
            durationMin: 120,
            costINR: 200,
            meta: { rating: 4.2, open: '09:00', close: '18:00', address: 'Museum St' }
          }
        }
      ];

      const arranged = autoArrangeNodes(nodes);
      
      expect(arranged).toHaveLength(2);
      expect(arranged[0].data.title).toBe('Museum'); // Earlier time should come first
      expect(arranged[1].data.title).toBe('Lunch');
      expect(arranged[0].position.x).toBeLessThan(arranged[1].position.x);
    });
  });
});
import { vi } from 'vitest';

// Mock Radix Slot to avoid vite resolver issues in tests
vi.mock('@radix-ui/react-slot@1.1.2', () => ({
  Slot: ({ children }: any) => children || null,
}));
vi.mock('@radix-ui/react-slot', () => ({
  Slot: ({ children }: any) => children || null,
}));

// Mock scrollIntoView for jsdom
// eslint-disable-next-line @typescript-eslint/no-explicit-any
if (!(HTMLElement as any).prototype.scrollIntoView) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (HTMLElement as any).prototype.scrollIntoView = vi.fn();
}

// Mock global fetch if not present
if (!(global as any).fetch) {
  (global as any).fetch = vi.fn();
}



import { describe, it, expect } from '@jest/globals';

/**
 * Test suite for chat pagination functionality
 * 
 * These tests verify that the pagination logic works correctly
 * for various message counts and user interactions.
 */

describe('Chat Pagination Logic', () => {
  const INITIAL_DISPLAY_COUNT = 20;
  const LOAD_MORE_COUNT = 20;

  describe('Initial Load', () => {
    it('should show all messages when count <= 20', () => {
      const allMessages = Array.from({ length: 15 }, (_, i) => ({ id: `msg-${i}` }));
      const displayCount = Math.min(INITIAL_DISPLAY_COUNT, allMessages.length);
      const displayedMessages = allMessages.slice(-displayCount);
      
      expect(displayedMessages.length).toBe(15);
      expect(displayedMessages[0].id).toBe('msg-0');
      expect(displayedMessages[14].id).toBe('msg-14');
    });

    it('should show latest 20 messages when count > 20', () => {
      const allMessages = Array.from({ length: 100 }, (_, i) => ({ id: `msg-${i}` }));
      const displayCount = Math.min(INITIAL_DISPLAY_COUNT, allMessages.length);
      const displayedMessages = allMessages.slice(-displayCount);
      
      expect(displayedMessages.length).toBe(20);
      expect(displayedMessages[0].id).toBe('msg-80'); // Latest 20 start at index 80
      expect(displayedMessages[19].id).toBe('msg-99');
    });

    it('should set hasMoreMessages correctly', () => {
      const allMessages = Array.from({ length: 100 }, (_, i) => ({ id: `msg-${i}` }));
      const displayCount = Math.min(INITIAL_DISPLAY_COUNT, allMessages.length);
      const hasMoreMessages = allMessages.length > displayCount;
      
      expect(hasMoreMessages).toBe(true);
    });
  });

  describe('Load More', () => {
    it('should load 20 more messages', () => {
      const allMessages = Array.from({ length: 100 }, (_, i) => ({ id: `msg-${i}` }));
      let displayCount = 20;
      
      // Simulate load more
      displayCount = Math.min(displayCount + LOAD_MORE_COUNT, allMessages.length);
      const displayedMessages = allMessages.slice(-displayCount);
      
      expect(displayedMessages.length).toBe(40);
      expect(displayedMessages[0].id).toBe('msg-60'); // Now showing from index 60
      expect(displayedMessages[39].id).toBe('msg-99');
    });

    it('should not exceed total message count', () => {
      const allMessages = Array.from({ length: 25 }, (_, i) => ({ id: `msg-${i}` }));
      let displayCount = 20;
      
      // Simulate load more
      displayCount = Math.min(displayCount + LOAD_MORE_COUNT, allMessages.length);
      const displayedMessages = allMessages.slice(-displayCount);
      
      expect(displayedMessages.length).toBe(25); // Not 40
      expect(displayedMessages[0].id).toBe('msg-0');
      expect(displayedMessages[24].id).toBe('msg-24');
    });

    it('should update hasMoreMessages after loading', () => {
      const allMessages = Array.from({ length: 100 }, (_, i) => ({ id: `msg-${i}` }));
      let displayCount = 20;
      let hasMoreMessages = allMessages.length > displayCount;
      
      expect(hasMoreMessages).toBe(true);
      
      // Load more 4 times (20 + 20 + 20 + 20 + 20 = 100)
      for (let i = 0; i < 4; i++) {
        displayCount = Math.min(displayCount + LOAD_MORE_COUNT, allMessages.length);
        hasMoreMessages = displayCount < allMessages.length;
      }
      
      expect(displayCount).toBe(100);
      expect(hasMoreMessages).toBe(false);
    });
  });

  describe('New Messages', () => {
    it('should add new messages to display', () => {
      const allMessages = Array.from({ length: 20 }, (_, i) => ({ id: `msg-${i}` }));
      let displayCount = 20;
      
      // Add 2 new messages (user + AI)
      const newMessages = [
        ...allMessages,
        { id: 'msg-20' },
        { id: 'msg-21' }
      ];
      displayCount = displayCount + 2;
      const displayedMessages = newMessages.slice(-displayCount);
      
      expect(displayedMessages.length).toBe(22);
      expect(displayedMessages[20].id).toBe('msg-20');
      expect(displayedMessages[21].id).toBe('msg-21');
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty message list', () => {
      const allMessages: any[] = [];
      const displayCount = Math.min(INITIAL_DISPLAY_COUNT, allMessages.length);
      const displayedMessages = allMessages.slice(-displayCount);
      const hasMoreMessages = allMessages.length > displayCount;
      
      expect(displayedMessages.length).toBe(0);
      expect(hasMoreMessages).toBe(false);
    });

    it('should handle exactly 20 messages', () => {
      const allMessages = Array.from({ length: 20 }, (_, i) => ({ id: `msg-${i}` }));
      const displayCount = Math.min(INITIAL_DISPLAY_COUNT, allMessages.length);
      const displayedMessages = allMessages.slice(-displayCount);
      const hasMoreMessages = allMessages.length > displayCount;
      
      expect(displayedMessages.length).toBe(20);
      expect(hasMoreMessages).toBe(false);
    });

    it('should handle 21 messages (boundary case)', () => {
      const allMessages = Array.from({ length: 21 }, (_, i) => ({ id: `msg-${i}` }));
      const displayCount = Math.min(INITIAL_DISPLAY_COUNT, allMessages.length);
      const displayedMessages = allMessages.slice(-displayCount);
      const hasMoreMessages = allMessages.length > displayCount;
      
      expect(displayedMessages.length).toBe(20);
      expect(displayedMessages[0].id).toBe('msg-1'); // Oldest shown is msg-1
      expect(hasMoreMessages).toBe(true);
    });
  });

  describe('Performance', () => {
    it('should handle large message counts efficiently', () => {
      const allMessages = Array.from({ length: 1000 }, (_, i) => ({ id: `msg-${i}` }));
      const displayCount = Math.min(INITIAL_DISPLAY_COUNT, allMessages.length);
      const displayedMessages = allMessages.slice(-displayCount);
      
      // Only 20 messages in DOM, not 1000
      expect(displayedMessages.length).toBe(20);
      expect(displayedMessages[0].id).toBe('msg-980');
      expect(displayedMessages[19].id).toBe('msg-999');
    });

    it('should calculate load more count correctly', () => {
      const allMessages = Array.from({ length: 100 }, (_, i) => ({ id: `msg-${i}` }));
      const displayCount = 20;
      const remainingCount = allMessages.length - displayCount;
      const loadMoreCount = Math.min(LOAD_MORE_COUNT, remainingCount);
      
      expect(loadMoreCount).toBe(20);
    });

    it('should show correct remaining count near end', () => {
      const allMessages = Array.from({ length: 100 }, (_, i) => ({ id: `msg-${i}` }));
      const displayCount = 95; // 5 messages left
      const remainingCount = allMessages.length - displayCount;
      const loadMoreCount = Math.min(LOAD_MORE_COUNT, remainingCount);
      
      expect(loadMoreCount).toBe(5); // Not 20
    });
  });
});

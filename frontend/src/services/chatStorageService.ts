/**
 * Chat Storage Service
 * Handles storing and retrieving chat history from Firebase Firestore
 * Structure: users/{userId}/itineraries/{itineraryId}/chats/{chatId}
 */

import { 
  collection, 
  doc, 
  addDoc, 
  getDocs, 
  query, 
  orderBy, 
  limit, 
  serverTimestamp,
  Timestamp 
} from 'firebase/firestore';
import { db } from '../config/firebase';

export interface ChatMessage {
  id?: string;
  message: string;
  sender: 'user' | 'assistant';
  timestamp: Date;
  itineraryId?: string;
  metadata?: {
    nodeId?: string;
    dayNumber?: number;
    intent?: string;
    [key: string]: any;
  };
}

export interface ChatSession {
  id?: string;
  itineraryId: string;
  title: string;
  createdAt: Date;
  updatedAt: Date;
  messageCount: number;
  lastMessage?: string;
}

class ChatStorageService {
  private readonly USERS_COLLECTION = 'users';
  private readonly ITINERARIES_COLLECTION = 'itineraries';
  private readonly CHATS_COLLECTION = 'chats';

  /**
   * Save a chat message to Firebase
   */
  async saveMessage(userId: string, message: ChatMessage): Promise<string> {
    try {
      console.log('[ChatStorage] Saving message for user:', userId, 'itinerary:', message.itineraryId);
      
      if (!message.itineraryId) {
        throw new Error('Itinerary ID is required for chat messages');
      }
      
      const chatRef = collection(
        db, 
        this.USERS_COLLECTION, 
        userId, 
        this.ITINERARIES_COLLECTION,
        message.itineraryId,
        this.CHATS_COLLECTION
      );
      
      const docRef = await addDoc(chatRef, {
        ...message,
        timestamp: serverTimestamp(),
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      });
      
      console.log('[ChatStorage] Message saved with ID:', docRef.id);
      return docRef.id;
      
    } catch (error) {
      console.error('[ChatStorage] Failed to save message:', error);
      throw new Error('Failed to save chat message');
    }
  }

  /**
   * Get chat history for a user and specific itinerary
   */
  async getChatHistory(userId: string, itineraryId: string, limitCount: number = 50): Promise<ChatMessage[]> {
    try {
      console.log('[ChatStorage] Getting chat history for user:', userId, 'itinerary:', itineraryId);
      
      if (!itineraryId) {
        throw new Error('Itinerary ID is required for chat history');
      }
      
      const chatRef = collection(
        db, 
        this.USERS_COLLECTION, 
        userId, 
        this.ITINERARIES_COLLECTION,
        itineraryId,
        this.CHATS_COLLECTION
      );
      
      const q = query(
        chatRef,
        orderBy('timestamp', 'asc'),
        limit(limitCount)
      );
      
      const querySnapshot = await getDocs(q);
      const messages: ChatMessage[] = [];
      
      querySnapshot.forEach((doc) => {
        const data = doc.data();
        const message: ChatMessage = {
          id: doc.id,
          message: data.message,
          sender: data.sender,
          timestamp: data.timestamp?.toDate() || new Date(),
          itineraryId: itineraryId, // Use the provided itineraryId
          metadata: data.metadata
        };
        
        messages.push(message);
      });
      
      console.log('[ChatStorage] Retrieved', messages.length, 'messages');
      return messages;
      
    } catch (error) {
      console.error('[ChatStorage] Failed to get chat history:', error);
      throw new Error('Failed to retrieve chat history');
    }
  }

  /**
   * Get chat sessions for a user
   * Note: This method now requires knowing the itinerary IDs in advance
   * since chat messages are now organized under specific itineraries
   */
  async getChatSessions(userId: string, itineraryIds: string[]): Promise<ChatSession[]> {
    try {
      console.log('[ChatStorage] Getting chat sessions for user:', userId, 'itineraries:', itineraryIds);
      
      const sessions: ChatSession[] = [];
      
      // Get chat history for each itinerary
      for (const itineraryId of itineraryIds) {
        try {
          const messages = await this.getChatHistory(userId, itineraryId);
          
          if (messages.length > 0) {
            const lastMessage = messages[messages.length - 1];
            const firstMessage = messages[0];
            
            sessions.push({
              itineraryId,
              title: `Chat for Itinerary ${itineraryId.slice(-8)}`,
              createdAt: firstMessage.timestamp,
              updatedAt: lastMessage.timestamp,
              messageCount: messages.length,
              lastMessage: lastMessage.message
            });
          }
        } catch (error) {
          console.warn('[ChatStorage] Failed to get chat history for itinerary:', itineraryId, error);
          // Continue with other itineraries
        }
      }
      
      // Sort by most recent activity
      sessions.sort((a, b) => b.updatedAt.getTime() - a.updatedAt.getTime());
      
      console.log('[ChatStorage] Retrieved', sessions.length, 'chat sessions');
      return sessions;
      
    } catch (error) {
      console.error('[ChatStorage] Failed to get chat sessions:', error);
      throw new Error('Failed to retrieve chat sessions');
    }
  }

  /**
   * Clear chat history for a specific itinerary
   */
  async clearChatHistory(userId: string, itineraryId: string): Promise<void> {
    try {
      console.log('[ChatStorage] Clearing chat history for user:', userId, 'itinerary:', itineraryId);
      
      // Note: This is a simplified implementation
      // In a real app, you might want to use batch deletes or soft deletes
      const messages = await this.getChatHistory(userId, itineraryId);
      
      // For now, we'll just log the action
      // In a full implementation, you would delete the documents
      console.log('[ChatStorage] Would delete', messages.length, 'messages');
      
    } catch (error) {
      console.error('[ChatStorage] Failed to clear chat history:', error);
      throw new Error('Failed to clear chat history');
    }
  }

  /**
   * Get message count for a user and specific itinerary
   */
  async getMessageCount(userId: string, itineraryId: string): Promise<number> {
    try {
      const messages = await this.getChatHistory(userId, itineraryId);
      return messages.length;
    } catch (error) {
      console.error('[ChatStorage] Failed to get message count:', error);
      return 0;
    }
  }
}

// Export singleton instance
export const chatStorageService = new ChatStorageService();
export default chatStorageService;

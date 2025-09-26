/**
 * Chat Storage Service
 * Handles storing and retrieving chat history from Firebase Firestore
 * Structure: users/{userId}/chat/{chatId}
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
  private readonly CHAT_COLLECTION = 'chat';

  /**
   * Save a chat message to Firebase
   */
  async saveMessage(userId: string, message: ChatMessage): Promise<string> {
    try {
      console.log('[ChatStorage] Saving message for user:', userId);
      
      const chatRef = collection(
        db, 
        this.USERS_COLLECTION, 
        userId, 
        this.CHAT_COLLECTION
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
   * Get chat history for a user
   */
  async getChatHistory(userId: string, itineraryId?: string, limitCount: number = 50): Promise<ChatMessage[]> {
    try {
      console.log('[ChatStorage] Getting chat history for user:', userId, 'itinerary:', itineraryId);
      
      const chatRef = collection(
        db, 
        this.USERS_COLLECTION, 
        userId, 
        this.CHAT_COLLECTION
      );
      
      let q = query(
        chatRef,
        orderBy('timestamp', 'desc'),
        limit(limitCount)
      );
      
      // If itineraryId is provided, filter by it
      if (itineraryId) {
        q = query(
          chatRef,
          orderBy('itineraryId'),
          orderBy('timestamp', 'desc'),
          limit(limitCount)
        );
      }
      
      const querySnapshot = await getDocs(q);
      const messages: ChatMessage[] = [];
      
      querySnapshot.forEach((doc) => {
        const data = doc.data();
        const message: ChatMessage = {
          id: doc.id,
          message: data.message,
          sender: data.sender,
          timestamp: data.timestamp?.toDate() || new Date(),
          itineraryId: data.itineraryId,
          metadata: data.metadata
        };
        
        // Filter by itineraryId if specified
        if (!itineraryId || message.itineraryId === itineraryId) {
          messages.push(message);
        }
      });
      
      // Sort by timestamp ascending (oldest first)
      messages.sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime());
      
      console.log('[ChatStorage] Retrieved', messages.length, 'messages');
      return messages;
      
    } catch (error) {
      console.error('[ChatStorage] Failed to get chat history:', error);
      throw new Error('Failed to retrieve chat history');
    }
  }

  /**
   * Get chat sessions for a user
   */
  async getChatSessions(userId: string): Promise<ChatSession[]> {
    try {
      console.log('[ChatStorage] Getting chat sessions for user:', userId);
      
      // Get all messages and group by itineraryId
      const messages = await this.getChatHistory(userId);
      const sessionMap = new Map<string, ChatSession>();
      
      messages.forEach((message) => {
        if (!message.itineraryId) return;
        
        const itineraryId = message.itineraryId;
        
        if (!sessionMap.has(itineraryId)) {
          sessionMap.set(itineraryId, {
            itineraryId,
            title: `Chat for Itinerary ${itineraryId.slice(-8)}`,
            createdAt: message.timestamp,
            updatedAt: message.timestamp,
            messageCount: 0,
            lastMessage: message.message
          });
        }
        
        const session = sessionMap.get(itineraryId)!;
        session.messageCount++;
        
        // Update with latest message
        if (message.timestamp > session.updatedAt) {
          session.updatedAt = message.timestamp;
          session.lastMessage = message.message;
        }
      });
      
      const sessions = Array.from(sessionMap.values());
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
   * Get message count for a user
   */
  async getMessageCount(userId: string, itineraryId?: string): Promise<number> {
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

import { 
  collection, 
  doc, 
  getDoc, 
  getDocs, 
  addDoc, 
  updateDoc, 
  deleteDoc, 
  query, 
  orderBy, 
  where,
  Timestamp 
} from 'firebase/firestore';
import { db } from '../config/firebase';

export interface FirestoreItinerary {
  id: string;
  version: number;
  json: string;
  updatedAt: Timestamp;
}

export interface NormalizedItinerary {
  itineraryId: string;
  version: number;
  // Add other properties as needed
  [key: string]: any;
}

class FirebaseService {
  private readonly COLLECTION_NAME = 'itineraries';

  /**
   * Get an itinerary by ID
   */
  async getItinerary(id: string): Promise<NormalizedItinerary | null> {
    try {
      const docRef = doc(db, this.COLLECTION_NAME, id);
      const docSnap = await getDoc(docRef);
      
      if (docSnap.exists()) {
        const data = docSnap.data() as FirestoreItinerary;
        return JSON.parse(data.json);
      }
      return null;
    } catch (error) {
      console.error('Error getting itinerary:', error);
      throw error;
    }
  }

  /**
   * Get all itineraries ordered by updated timestamp
   */
  async getAllItineraries(): Promise<NormalizedItinerary[]> {
    try {
      const q = query(
        collection(db, this.COLLECTION_NAME),
        orderBy('updatedAt', 'desc')
      );
      const querySnapshot = await getDocs(q);
      
      return querySnapshot.docs.map(doc => {
        const data = doc.data() as FirestoreItinerary;
        return JSON.parse(data.json);
      });
    } catch (error) {
      console.error('Error getting all itineraries:', error);
      throw error;
    }
  }

  /**
   * Save an itinerary
   */
  async saveItinerary(itinerary: NormalizedItinerary): Promise<string> {
    try {
      const firestoreItinerary: Omit<FirestoreItinerary, 'id'> = {
        version: itinerary.version,
        json: JSON.stringify(itinerary),
        updatedAt: Timestamp.now()
      };

      const docRef = await addDoc(collection(db, this.COLLECTION_NAME), firestoreItinerary);
      return docRef.id;
    } catch (error) {
      console.error('Error saving itinerary:', error);
      throw error;
    }
  }

  /**
   * Update an existing itinerary
   */
  async updateItinerary(id: string, itinerary: NormalizedItinerary): Promise<void> {
    try {
      const docRef = doc(db, this.COLLECTION_NAME, id);
      await updateDoc(docRef, {
        version: itinerary.version,
        json: JSON.stringify(itinerary),
        updatedAt: Timestamp.now()
      });
    } catch (error) {
      console.error('Error updating itinerary:', error);
      throw error;
    }
  }

  /**
   * Delete an itinerary
   */
  async deleteItinerary(id: string): Promise<void> {
    try {
      const docRef = doc(db, this.COLLECTION_NAME, id);
      await deleteDoc(docRef);
    } catch (error) {
      console.error('Error deleting itinerary:', error);
      throw error;
    }
  }

  /**
   * Get itineraries updated after a specific timestamp
   */
  async getItinerariesUpdatedAfter(timestamp: Date): Promise<NormalizedItinerary[]> {
    try {
      const q = query(
        collection(db, this.COLLECTION_NAME),
        where('updatedAt', '>', Timestamp.fromDate(timestamp)),
        orderBy('updatedAt', 'desc')
      );
      const querySnapshot = await getDocs(q);
      
      return querySnapshot.docs.map(doc => {
        const data = doc.data() as FirestoreItinerary;
        return JSON.parse(data.json);
      });
    } catch (error) {
      console.error('Error getting itineraries updated after timestamp:', error);
      throw error;
    }
  }
}

export const firebaseService = new FirebaseService();


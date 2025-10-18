package com.tripplanner.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.tripplanner.data.entity.FirestoreItinerary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class FirestoreDatabaseService implements DatabaseService {

    private static final String COLLECTION_ITINERARIES = "itineraries";
    private static final String SUBCOLLECTION_REVISIONS = "revisions";

    private final Firestore firestore;

    public FirestoreDatabaseService(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public FirestoreItinerary save(FirestoreItinerary itinerary) {
        itinerary.updateTimestamp();
        Map<String, Object> data = toMap(itinerary);
        DocumentReference docRef = firestore.collection(COLLECTION_ITINERARIES).document(itinerary.getId());
        try {
            docRef.set(data).get();
            return itinerary;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save itinerary", e);
        }
    }

    @Override
    public Optional<FirestoreItinerary> findById(String id) {
        try {
            DocumentSnapshot snapshot = firestore.collection(COLLECTION_ITINERARIES).document(id).get().get();
            if (!snapshot.exists()) return Optional.empty();
            return Optional.of(fromSnapshot(snapshot));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to find itinerary", e);
        }
    }

    @Override
    public boolean existsById(String id) {
        try {
            DocumentSnapshot snapshot = firestore.collection(COLLECTION_ITINERARIES).document(id).get().get();
            return snapshot.exists();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to check existence", e);
        }
    }

    @Override
    public List<FirestoreItinerary> findAllOrderByUpdatedAtDesc() {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_ITINERARIES)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<FirestoreItinerary> result = new ArrayList<>();
            for (DocumentSnapshot doc : documents) {
                result.add(fromSnapshot(doc));
            }
            return result;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to list itineraries", e);
        }
    }

    @Override
    public List<FirestoreItinerary> findByUpdatedAtAfter(Instant timestamp) {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_ITINERARIES)
                    .whereGreaterThan("updatedAt", Timestamp.ofTimeSecondsAndNanos(timestamp.getEpochSecond(), timestamp.getNano()))
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<FirestoreItinerary> result = new ArrayList<>();
            for (DocumentSnapshot doc : documents) {
                result.add(fromSnapshot(doc));
            }
            return result;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to query itineraries", e);
        }
    }

    @Override
    public void deleteById(String id) {
        try {
            firestore.collection(COLLECTION_ITINERARIES).document(id).delete().get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to delete itinerary", e);
        }
    }

    @Override
    public void saveRevision(String itineraryId, FirestoreItinerary revision) {
        revision.updateTimestamp();
        
        // Use new document method for flexible storage
        String path = COLLECTION_ITINERARIES + "/" + itineraryId + "/" + SUBCOLLECTION_REVISIONS + "/" + revision.getVersion();
        
        try {
            // Create revision data with additional metadata
            Map<String, Object> revisionData = new HashMap<>();
            revisionData.put("id", revision.getId());
            revisionData.put("version", revision.getVersion());
            revisionData.put("json", revision.getJson());
            revisionData.put("updatedAt", Timestamp.ofTimeSecondsAndNanos(
                revision.getUpdatedAt().getEpochSecond(), 
                revision.getUpdatedAt().getNano()
            ));
            
            // Save using the new flexible document method
            DocumentReference docRef = buildDocumentReference(path.split("/"));
            docRef.set(revisionData).get();
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save revision", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save revision", e);
        }
    }

    @Override
    public Optional<FirestoreItinerary> findRevisionByItineraryIdAndVersion(String itineraryId, Integer version) {
        try {
            DocumentSnapshot snapshot = firestore.collection(COLLECTION_ITINERARIES)
                    .document(itineraryId)
                    .collection(SUBCOLLECTION_REVISIONS)
                    .document(String.valueOf(version))
                    .get().get();
            if (!snapshot.exists()) return Optional.empty();
            return Optional.of(fromSnapshot(snapshot));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to load revision", e);
        }
    }

    @Override
    public String getDatabaseType() {
        return "firestore";
    }

    /**
     * Save a document at the specified path with JSON content.
     * Supports flexible document storage for revision support.
     */
    public void saveDocument(String path, String json) {
        try {
            // Parse the path to get collection and document references
            String[] pathParts = path.split("/");
            if (pathParts.length < 2 || pathParts.length % 2 != 0) {
                throw new IllegalArgumentException("Invalid path format. Expected: collection/document or collection/document/subcollection/subdocument");
            }

            DocumentReference docRef = buildDocumentReference(pathParts);
            
            // Create document data with JSON content and timestamp
            Map<String, Object> data = new HashMap<>();
            data.put("json", json);
            data.put("updatedAt", Timestamp.now());
            
            docRef.set(data).get();
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save document at path: " + path, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save document at path: " + path, e);
        }
    }

    /**
     * Get documents from a collection path.
     * Returns a list of document data as maps.
     */
    public List<Map<String, Object>> getDocuments(String path) {
        try {
            // Parse the path to get collection reference
            String[] pathParts = path.split("/");
            if (pathParts.length % 2 == 0) {
                throw new IllegalArgumentException("Invalid collection path format. Expected odd number of path parts for collection");
            }

            CollectionReference collectionRef = buildCollectionReference(pathParts);
            
            ApiFuture<QuerySnapshot> future = collectionRef.orderBy("updatedAt", Query.Direction.DESCENDING).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (DocumentSnapshot doc : documents) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", doc.getId());
                data.put("json", doc.getString("json"));
                data.put("updatedAt", doc.getTimestamp("updatedAt"));
                // Include all other fields from the document
                data.putAll(doc.getData());
                result.add(data);
            }
            
            return result;
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to get documents from path: " + path, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get documents from path: " + path, e);
        }
    }

    /**
     * Get a single document from the specified path.
     * Returns the document data as a map, or empty optional if not found.
     */
    public Optional<Map<String, Object>> getDocument(String path) {
        try {
            // Parse the path to get document reference
            String[] pathParts = path.split("/");
            if (pathParts.length < 2 || pathParts.length % 2 != 0) {
                throw new IllegalArgumentException("Invalid document path format. Expected: collection/document or collection/document/subcollection/subdocument");
            }

            DocumentReference docRef = buildDocumentReference(pathParts);
            DocumentSnapshot snapshot = docRef.get().get();
            
            if (!snapshot.exists()) {
                return Optional.empty();
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", snapshot.getId());
            data.put("json", snapshot.getString("json"));
            data.put("updatedAt", snapshot.getTimestamp("updatedAt"));
            // Include all other fields from the document
            data.putAll(snapshot.getData());
            
            return Optional.of(data);
            
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to get document from path: " + path, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get document from path: " + path, e);
        }
    }

    /**
     * Build a DocumentReference from path parts.
     */
    private DocumentReference buildDocumentReference(String[] pathParts) {
        DocumentReference docRef = firestore.collection(pathParts[0]).document(pathParts[1]);
        
        // Handle nested collections/documents
        for (int i = 2; i < pathParts.length; i += 2) {
            if (i + 1 < pathParts.length) {
                docRef = docRef.collection(pathParts[i]).document(pathParts[i + 1]);
            }
        }
        
        return docRef;
    }

    /**
     * Build a CollectionReference from path parts.
     */
    private CollectionReference buildCollectionReference(String[] pathParts) {
        CollectionReference collectionRef = firestore.collection(pathParts[0]);
        
        // Handle nested collections
        for (int i = 1; i < pathParts.length; i += 2) {
            if (i + 1 < pathParts.length) {
                collectionRef = collectionRef.document(pathParts[i]).collection(pathParts[i + 1]);
            }
        }
        
        return collectionRef;
    }

    private static Map<String, Object> toMap(FirestoreItinerary it) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", it.getId());
        map.put("version", it.getVersion());
        map.put("json", it.getJson());
        map.put("updatedAt", Timestamp.ofTimeSecondsAndNanos(it.getUpdatedAt().getEpochSecond(), it.getUpdatedAt().getNano()));
        return map;
    }

    private static FirestoreItinerary fromSnapshot(DocumentSnapshot doc) {
        String id = doc.contains("id") ? doc.getString("id") : doc.getId();
        Long versionLong = doc.getLong("version");
        Integer version = versionLong == null ? null : versionLong.intValue();
        String json = doc.getString("json");
        Timestamp ts = doc.contains("updatedAt") ? doc.getTimestamp("updatedAt") : Timestamp.now();
        Instant updatedAt = Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
        return new FirestoreItinerary(id, version, json, updatedAt);
    }
}



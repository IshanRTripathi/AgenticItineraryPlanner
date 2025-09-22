package com.tripplanner.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.tripplanner.data.entity.FirestoreItinerary;
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
        Map<String, Object> data = toMap(revision);
        try {
            firestore.collection(COLLECTION_ITINERARIES)
                    .document(itineraryId)
                    .collection(SUBCOLLECTION_REVISIONS)
                    .document(String.valueOf(revision.getVersion()))
                    .set(data)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
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



package com.tripplanner.service;

import com.tripplanner.data.entity.FirestoreItinerary;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Abstraction over the persistence layer for itineraries.
 * This implementation is expected to be backed by Firestore.
 */
public interface DatabaseService {

    FirestoreItinerary save(FirestoreItinerary itinerary);

    Optional<FirestoreItinerary> findById(String id);

    boolean existsById(String id);

    List<FirestoreItinerary> findAllOrderByUpdatedAtDesc();

    List<FirestoreItinerary> findByUpdatedAtAfter(Instant timestamp);

    void deleteById(String id);

    void saveRevision(String itineraryId, FirestoreItinerary revision);

    Optional<FirestoreItinerary> findRevisionByItineraryIdAndVersion(String itineraryId, Integer version);

    String getDatabaseType();
}



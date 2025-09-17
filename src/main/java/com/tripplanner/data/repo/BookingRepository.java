package com.tripplanner.data.repo;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.tripplanner.data.entity.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Repository for Booking entity operations with Firestore.
 */
@Repository
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(Firestore.class)
public class BookingRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingRepository.class);
    private static final String COLLECTION_NAME = "bookings";
    
    private final Firestore firestore;
    
    public BookingRepository(Firestore firestore) {
        this.firestore = firestore;
    }
    
    /**
     * Save a booking to Firestore.
     */
    public Booking save(Booking booking) throws ExecutionException, InterruptedException {
        logger.debug("Saving booking: {}", booking.getId());
        
        if (booking.getId() == null) {
            // Generate ID for new booking
            booking.setId(firestore.collection(COLLECTION_NAME).document().getId());
        }
        
        booking.updateTimestamp();
        
        firestore.collection(COLLECTION_NAME)
                .document(booking.getId())
                .set(booking)
                .get();
        
        logger.debug("Booking saved successfully: {}", booking.getId());
        return booking;
    }
    
    /**
     * Find booking by ID.
     */
    public Optional<Booking> findById(String id) throws ExecutionException, InterruptedException {
        logger.debug("Finding booking by ID: {}", id);
        
        DocumentSnapshot document = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get()
                .get();
        
        if (document.exists()) {
            Booking booking = document.toObject(Booking.class);
            if (booking != null) {
                booking.setId(document.getId());
            }
            logger.debug("Booking found: {}", id);
            return Optional.ofNullable(booking);
        }
        
        logger.debug("Booking not found: {}", id);
        return Optional.empty();
    }
    
    /**
     * Find bookings by user ID, ordered by creation timestamp (most recent first).
     */
    public List<Booking> findByUserId(String userId) throws ExecutionException, InterruptedException {
        return findByUserId(userId, 50); // Default limit
    }
    
    /**
     * Find bookings by user ID with limit.
     */
    public List<Booking> findByUserId(String userId, int limit) throws ExecutionException, InterruptedException {
        logger.debug("Finding bookings for user: {}, limit: {}", userId, limit);
        
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .get();
        
        List<Booking> bookings = querySnapshot.getDocuments().stream()
                .map(doc -> {
                    Booking booking = doc.toObject(Booking.class);
                    if (booking != null) {
                        booking.setId(doc.getId());
                    }
                    return booking;
                })
                .collect(Collectors.toList());
        
        logger.debug("Found {} bookings for user: {}", bookings.size(), userId);
        return bookings;
    }
    
    /**
     * Find bookings by itinerary ID.
     */
    public List<Booking> findByItineraryId(String itineraryId) throws ExecutionException, InterruptedException {
        logger.debug("Finding bookings for itinerary: {}", itineraryId);
        
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("itineraryId", itineraryId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .get();
        
        List<Booking> bookings = querySnapshot.getDocuments().stream()
                .map(doc -> {
                    Booking booking = doc.toObject(Booking.class);
                    if (booking != null) {
                        booking.setId(doc.getId());
                    }
                    return booking;
                })
                .collect(Collectors.toList());
        
        logger.debug("Found {} bookings for itinerary: {}", bookings.size(), itineraryId);
        return bookings;
    }
    
    /**
     * Find booking by Razorpay order ID.
     */
    public Optional<Booking> findByRazorpayOrderId(String orderId) throws ExecutionException, InterruptedException {
        logger.debug("Finding booking by Razorpay order ID: {}", orderId);
        
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("razorpay.orderId", orderId)
                .limit(1)
                .get()
                .get();
        
        if (!querySnapshot.isEmpty()) {
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            Booking booking = document.toObject(Booking.class);
            if (booking != null) {
                booking.setId(document.getId());
            }
            logger.debug("Booking found by Razorpay order ID: {}", orderId);
            return Optional.ofNullable(booking);
        }
        
        logger.debug("Booking not found by Razorpay order ID: {}", orderId);
        return Optional.empty();
    }
    
    /**
     * Find booking by Razorpay payment ID.
     */
    public Optional<Booking> findByRazorpayPaymentId(String paymentId) throws ExecutionException, InterruptedException {
        logger.debug("Finding booking by Razorpay payment ID: {}", paymentId);
        
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("razorpay.paymentId", paymentId)
                .limit(1)
                .get()
                .get();
        
        if (!querySnapshot.isEmpty()) {
            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
            Booking booking = document.toObject(Booking.class);
            if (booking != null) {
                booking.setId(document.getId());
            }
            logger.debug("Booking found by Razorpay payment ID: {}", paymentId);
            return Optional.ofNullable(booking);
        }
        
        logger.debug("Booking not found by Razorpay payment ID: {}", paymentId);
        return Optional.empty();
    }
    
    /**
     * Update booking status.
     */
    public void updateStatus(String id, Booking.BookingStatus status) throws ExecutionException, InterruptedException {
        logger.debug("Updating status for booking: {} to {}", id, status);
        
        firestore.collection(COLLECTION_NAME)
                .document(id)
                .update("status", status, "updatedAt", Instant.now())
                .get();
        
        logger.debug("Status updated for booking: {}", id);
    }
    
    /**
     * Update booking with Razorpay payment details.
     */
    public void updateRazorpayDetails(String id, Booking.RazorpayDetails razorpayDetails) throws ExecutionException, InterruptedException {
        logger.debug("Updating Razorpay details for booking: {}", id);
        
        firestore.collection(COLLECTION_NAME)
                .document(id)
                .update("razorpay", razorpayDetails, "updatedAt", Instant.now())
                .get();
        
        logger.debug("Razorpay details updated for booking: {}", id);
    }
    
    /**
     * Update booking with provider confirmation details.
     */
    public void updateProviderDetails(String id, Booking.ProviderDetails providerDetails) throws ExecutionException, InterruptedException {
        logger.debug("Updating provider details for booking: {}", id);
        
        firestore.collection(COLLECTION_NAME)
                .document(id)
                .update("provider", providerDetails, "updatedAt", Instant.now())
                .get();
        
        logger.debug("Provider details updated for booking: {}", id);
    }
    
    /**
     * Find bookings by status.
     */
    public List<Booking> findByStatus(Booking.BookingStatus status) throws ExecutionException, InterruptedException {
        logger.debug("Finding bookings with status: {}", status);
        
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .get();
        
        return querySnapshot.getDocuments().stream()
                .map(doc -> {
                    Booking booking = doc.toObject(Booking.class);
                    if (booking != null) {
                        booking.setId(doc.getId());
                    }
                    return booking;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Find bookings that need processing (stuck in intermediate states).
     */
    public List<Booking> findStaleBookings(Instant olderThan) throws ExecutionException, InterruptedException {
        logger.debug("Finding stale bookings older than: {}", olderThan);
        
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereIn("status", List.of(
                        Booking.BookingStatus.PAYMENT_CONFIRMED,
                        Booking.BookingStatus.BOOKING_IN_PROGRESS
                ))
                .whereLessThan("updatedAt", olderThan)
                .get()
                .get();
        
        return querySnapshot.getDocuments().stream()
                .map(doc -> {
                    Booking booking = doc.toObject(Booking.class);
                    if (booking != null) {
                        booking.setId(doc.getId());
                    }
                    return booking;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Delete booking by ID.
     */
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        logger.debug("Deleting booking: {}", id);
        
        firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete()
                .get();
        
        logger.debug("Booking deleted: {}", id);
    }
    
    /**
     * Check if booking exists by ID.
     */
    public boolean existsById(String id) throws ExecutionException, InterruptedException {
        DocumentSnapshot document = firestore.collection(COLLECTION_NAME)
                .document(id)
                .get()
                .get();
        
        return document.exists();
    }
    
    /**
     * Count bookings by user ID.
     */
    public long countByUserId(String userId) throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .get()
                .get();
        
        return querySnapshot.size();
    }
    
    /**
     * Count bookings by status.
     */
    public long countByStatus(Booking.BookingStatus status) throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("status", status)
                .get()
                .get();
        
        return querySnapshot.size();
    }
}

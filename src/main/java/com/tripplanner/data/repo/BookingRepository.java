package com.tripplanner.data.repo;

import com.tripplanner.data.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Booking entity operations with Firestore.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    /**
     * Find bookings by user ID.
     */
    List<Booking> findByUserId(String userId);
    
    /**
     * Find bookings by itinerary ID.
     */
    List<Booking> findByItineraryId(String itineraryId);
    
    /**
     * Find booking by payment ID.
     */
    @Query("SELECT b FROM Booking b WHERE b.razorpay.paymentId = :paymentId")
    Optional<Booking> findByPaymentId(@Param("paymentId") String paymentId);

    /**
     * Find booking by Razorpay order ID.
     */
    @Query("SELECT b FROM Booking b WHERE b.razorpay.orderId = :orderId")
    Optional<Booking> findByRazorpayOrderId(@Param("orderId") String orderId);
    
    /**
     * Find bookings by status.
     */
    List<Booking> findByStatus(Booking.BookingStatus status);
    
    /**
     * Find booking by booking ID.
     */
    Optional<Booking> findByBookingId(String bookingId);
}

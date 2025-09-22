package com.tripplanner.data.repo;

import com.tripplanner.data.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Itinerary entity operations with JPA.
 */
@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    
    /**
     * Find itineraries by user ID, ordered by updated timestamp (most recent first).
     */
    List<Itinerary> findByUserIdOrderByUpdatedAtDesc(String userId);
    
    /**
     * Find itineraries by user ID with limit.
     */
    @Query("SELECT i FROM Itinerary i WHERE i.userId = :userId ORDER BY i.updatedAt DESC")
    List<Itinerary> findByUserIdWithLimit(@Param("userId") String userId, org.springframework.data.domain.Pageable pageable);
    
    /**
     * Find itineraries by status.
     */
    List<Itinerary> findByStatus(String status);
    
    /**
     * Find public itinerary by share token.
     */
    Optional<Itinerary> findByShareToken(String shareToken);
    
    /**
     * Find itineraries by user ID and status.
     */
    List<Itinerary> findByUserIdAndStatus(String userId, String status);
    
    /**
     * Find itinerary by ID with eagerly loaded days.
     */
    @Query("SELECT i FROM Itinerary i LEFT JOIN FETCH i.days WHERE i.id = :id")
    Optional<Itinerary> findByIdWithDays(@Param("id") Long id);
}
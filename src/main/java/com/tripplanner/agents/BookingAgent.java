package com.tripplanner.agents;

import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * BookingAgent for handling hotel, flight, and activity bookings.
 * Extends BaseAgent to provide complete booking flow with payment processing.
 */
@Component
public class BookingAgent extends BaseAgent {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingAgent.class);
    
    private final BookingComService bookingComService;
    private final ExpediaService expediaService;
    private final RazorpayService razorpayService;
    private final ItineraryJsonService itineraryJsonService;
    
    public BookingAgent(AgentEventBus eventBus,
                       BookingComService bookingComService,
                       ExpediaService expediaService,
                       RazorpayService razorpayService,
                       ItineraryJsonService itineraryJsonService) {
        super(eventBus, AgentEvent.AgentKind.BOOKING);
        this.bookingComService = bookingComService;
        this.expediaService = expediaService;
        this.razorpayService = razorpayService;
        this.itineraryJsonService = itineraryJsonService;
    }
    
    @Override
    public AgentCapabilities getCapabilities() {
        AgentCapabilities capabilities = new AgentCapabilities();
        
        // Single clear task type: book
        capabilities.addSupportedTask("book");
        
        // Set priority (lower = higher priority)
        capabilities.setPriority(30); // Medium priority for bookings
        
        // Configuration
        capabilities.setChatEnabled(true); // Handle chat requests
        capabilities.setConfigurationValue("requiresPayment", true);
        capabilities.setConfigurationValue("handlesReservations", true);
        capabilities.setConfigurationValue("scopeType", "booking_only");
        
        return capabilities;
    }
    
    @Override
    public boolean canHandle(String taskType, Object taskContext) {
        // BookingAgent handles "book" task type only
        return super.canHandle(taskType);
    }
    
    @Override
    protected <T> T executeInternal(String itineraryId, AgentRequest<T> request) {
        logger.info("BookingAgent executing for itinerary: {}", itineraryId);
        
        try {
            // Extract BookingRequest from Map or use directly
            BookingRequest bookingRequest;
            Object data = request.getData();
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) data;
                bookingRequest = (BookingRequest) dataMap.get("bookingRequest");
            } else if (data instanceof BookingRequest) {
                bookingRequest = (BookingRequest) data;
            } else {
                bookingRequest = null;
            }
            
            if (bookingRequest == null) {
                throw new IllegalArgumentException("BookingAgent requires BookingRequest data");
            }
            
            emitProgress(itineraryId, 10, "Processing booking request", "validate");
            
            // Validate booking request
            validateBookingRequest(bookingRequest);
            
            emitProgress(itineraryId, 20, "Determining booking type", "route");
            
            // Switch on booking type and call appropriate booking method
            BookingResult result;
            switch (bookingRequest.getBookingType().toLowerCase()) {
                case "hotel":
                    result = bookHotel(bookingRequest);
                    break;
                case "flight":
                    result = bookFlight(bookingRequest);
                    break;
                case "activity":
                    result = bookActivity(bookingRequest);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported booking type: " + bookingRequest.getBookingType());
            }
            
            emitProgress(itineraryId, 90, "Updating itinerary with booking details", "update");
            
            // Update itinerary with booking reference if successful
            if (result.isSuccessful()) {
                updateItineraryWithBooking(itineraryId, result);
            }
            
            emitProgress(itineraryId, 100, "Booking process complete", "complete");
            
            logger.info("BookingAgent completed for itinerary: {} with status: {}", itineraryId, result.getStatus());
            
            // Return BookingResult cast to generic type T
            @SuppressWarnings("unchecked")
            T typedResult = (T) result;
            return typedResult;
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid booking request for itinerary {}: {}", itineraryId, e.getMessage());
            BookingResult errorResult = createErrorResult("VALIDATION_ERROR", e.getMessage());
            @SuppressWarnings("unchecked")
            T typedResult = (T) errorResult;
            return typedResult;
        } catch (Exception e) {
            logger.error("BookingAgent execution failed for itinerary {}: {}", itineraryId, e.getMessage(), e);
            BookingResult errorResult = createErrorResult("BOOKING_ERROR", "Booking failed: " + e.getMessage());
            @SuppressWarnings("unchecked")
            T typedResult = (T) errorResult;
            return typedResult;
        }
    }
    
    @Override
    protected String getAgentName() {
        return "BookingAgent";
    }
    
    /**
     * Book a hotel using BookingComService.
     */
    private BookingResult bookHotel(BookingRequest request) {
        logger.info("Booking hotel for location: {}", request.getLocation());
        
        try {
            emitProgress(null, 30, "Searching for hotels", "search");
            
            // Search hotels using BookingComService
            HotelSearchResponse searchResponse = bookingComService.searchHotels(request);
            
            if (!searchResponse.isSuccessful() || !searchResponse.hasHotels()) {
                return createErrorResult("NO_HOTELS_FOUND", "No hotels found for the specified criteria");
            }
            
            emitProgress(null, 50, "Selecting best hotel", "select");
            
            // Rank and select best hotel
            Hotel selectedHotel = rankAndSelectHotel(searchResponse.getHotels(), request);
            
            emitProgress(null, 60, "Processing payment", "payment");
            
            // Process payment using RazorpayService
            PaymentRequest paymentRequest = createHotelPaymentRequest(selectedHotel, request);
            PaymentOrder paymentOrder = razorpayService.createPaymentOrder(paymentRequest);
            
            // Simulate payment verification (in real scenario, this would come from frontend)
            PaymentVerification verification = simulatePaymentVerification(paymentOrder);
            PaymentResult paymentResult = razorpayService.verifyPayment(verification);
            
            if (!paymentResult.isSuccessful()) {
                return createErrorResult("PAYMENT_FAILED", "Payment failed: " + paymentResult.getErrorMessage());
            }
            
            emitProgress(null, 80, "Confirming hotel booking", "confirm");
            
            // Confirm booking using BookingComService
            BookingConfirmation confirmation = bookingComService.confirmBooking(selectedHotel, paymentResult);
            
            if (!confirmation.isSuccessful()) {
                // Payment succeeded but booking failed - need to refund
                logger.warn("Booking failed after successful payment, initiating refund");
                RefundRequest refundRequest = new RefundRequest();
                refundRequest.setPaymentId(paymentResult.getPaymentId());
                refundRequest.setAmount(paymentResult.getAmount());
                refundRequest.setReason("Booking confirmation failed");
                razorpayService.processRefund(refundRequest);
                return createErrorResult("BOOKING_FAILED", "Booking failed: " + confirmation.getErrorMessage());
            }
            
            // Create successful booking result
            BookingResult result = new BookingResult("hotel", "SUCCESS");
            result.setConfirmationNumber(confirmation.getConfirmationNumber());
            result.setBookingReference(confirmation.getBookingReference());
            result.setPaymentId(paymentResult.getPaymentId());
            result.setTotalAmount(paymentResult.getAmount());
            result.setCurrency(paymentResult.getCurrency());
            result.setProvider("Booking.com");
            result.setCancellationPolicy(selectedHotel.getCancellationPolicy());
            
            // Add hotel details
            result.addBookingDetail("hotelName", selectedHotel.getName());
            result.addBookingDetail("hotelAddress", selectedHotel.getAddress());
            result.addBookingDetail("checkInDate", request.getCheckInDate().toString());
            result.addBookingDetail("checkOutDate", request.getCheckOutDate().toString());
            result.addBookingDetail("guests", request.getGuests());
            result.addBookingDetail("rooms", request.getRooms());
            
            // Add contact info
            result.addContactInfo("email", confirmation.getContactEmail());
            result.addContactInfo("phone", confirmation.getContactPhone());
            
            logger.info("Hotel booking successful: {}", result.getBookingReference());
            return result;
            
        } catch (Exception e) {
            logger.error("Hotel booking failed", e);
            return createErrorResult("HOTEL_BOOKING_ERROR", "Hotel booking failed: " + e.getMessage());
        }
    }
    
    /**
     * Book a flight using ExpediaService.
     */
    private BookingResult bookFlight(BookingRequest request) {
        logger.info("Booking flight from {} to {}", request.getOrigin(), request.getDestination());
        
        try {
            emitProgress(null, 30, "Searching for flights", "search");
            
            // Search flights using ExpediaService
            FlightSearchResponse searchResponse = expediaService.searchFlights(request);
            
            if (!searchResponse.isSuccessful() || !searchResponse.hasFlights()) {
                return createErrorResult("NO_FLIGHTS_FOUND", "No flights found for the specified criteria");
            }
            
            emitProgress(null, 50, "Selecting best flight", "select");
            
            // Select best flight (first in ranked list)
            Flight selectedFlight = searchResponse.getFlights().get(0);
            
            emitProgress(null, 60, "Processing payment", "payment");
            
            // Process payment using RazorpayService
            PaymentRequest paymentRequest = createFlightPaymentRequest(selectedFlight, request);
            PaymentOrder paymentOrder = razorpayService.createPaymentOrder(paymentRequest);
            
            // Simulate payment verification
            PaymentVerification verification = simulatePaymentVerification(paymentOrder);
            PaymentResult paymentResult = razorpayService.verifyPayment(verification);
            
            if (!paymentResult.isSuccessful()) {
                return createErrorResult("PAYMENT_FAILED", "Payment failed: " + paymentResult.getErrorMessage());
            }
            
            emitProgress(null, 80, "Confirming flight booking", "confirm");
            
            // Confirm flight booking using ExpediaService
            BookingConfirmation confirmation = expediaService.confirmFlightBooking(selectedFlight, paymentResult);
            
            if (!confirmation.isSuccessful()) {
                // Payment succeeded but booking failed - need to refund
                logger.warn("Flight booking failed after successful payment, initiating refund");
                RefundRequest refundRequest = new RefundRequest();
                refundRequest.setPaymentId(paymentResult.getPaymentId());
                refundRequest.setAmount(paymentResult.getAmount());
                refundRequest.setReason("Flight booking confirmation failed");
                razorpayService.processRefund(refundRequest);
                return createErrorResult("BOOKING_FAILED", "Flight booking failed: " + confirmation.getErrorMessage());
            }
            
            // Create successful booking result
            BookingResult result = new BookingResult("flight", "SUCCESS");
            result.setConfirmationNumber(confirmation.getConfirmationNumber());
            result.setBookingReference(confirmation.getBookingReference());
            result.setPaymentId(paymentResult.getPaymentId());
            result.setTotalAmount(paymentResult.getAmount());
            result.setCurrency(paymentResult.getCurrency());
            result.setProvider("Expedia");
            
            // Add flight details
            result.addBookingDetail("airline", selectedFlight.getAirline());
            result.addBookingDetail("flightNumber", selectedFlight.getFlightNumber());
            result.addBookingDetail("origin", selectedFlight.getOrigin());
            result.addBookingDetail("destination", selectedFlight.getDestination());
            result.addBookingDetail("departureTime", selectedFlight.getDepartureTime().toString());
            result.addBookingDetail("arrivalTime", selectedFlight.getArrivalTime().toString());
            result.addBookingDetail("passengers", request.getPassengers());
            result.addBookingDetail("cabinClass", selectedFlight.getCabinClass());
            
            logger.info("Flight booking successful: {}", result.getBookingReference());
            return result;
            
        } catch (Exception e) {
            logger.error("Flight booking failed", e);
            return createErrorResult("FLIGHT_BOOKING_ERROR", "Flight booking failed: " + e.getMessage());
        }
    }
    
    /**
     * Book an activity using ExpediaService.
     */
    private BookingResult bookActivity(BookingRequest request) {
        logger.info("Booking activity in {} for type: {}", request.getLocation(), request.getActivityType());
        
        try {
            emitProgress(null, 30, "Searching for activities", "search");
            
            // Search activities using ExpediaService
            ActivitySearchResponse searchResponse = expediaService.searchActivities(request);
            
            if (!searchResponse.isSuccessful() || !searchResponse.hasActivities()) {
                return createErrorResult("NO_ACTIVITIES_FOUND", "No activities found for the specified criteria");
            }
            
            emitProgress(null, 50, "Selecting best activity", "select");
            
            // Select best activity (first in sorted list)
            Activity selectedActivity = searchResponse.getActivities().get(0);
            
            emitProgress(null, 60, "Processing payment", "payment");
            
            // Process payment using RazorpayService
            PaymentRequest paymentRequest = createActivityPaymentRequest(selectedActivity, request);
            PaymentOrder paymentOrder = razorpayService.createPaymentOrder(paymentRequest);
            
            // Simulate payment verification
            PaymentVerification verification = simulatePaymentVerification(paymentOrder);
            PaymentResult paymentResult = razorpayService.verifyPayment(verification);
            
            if (!paymentResult.isSuccessful()) {
                return createErrorResult("PAYMENT_FAILED", "Payment failed: " + paymentResult.getErrorMessage());
            }
            
            // Create successful booking result (simplified - no actual activity booking confirmation API)
            BookingResult result = new BookingResult("activity", "SUCCESS");
            result.setConfirmationNumber("AC" + System.currentTimeMillis());
            result.setBookingReference(generateActivityReference());
            result.setPaymentId(paymentResult.getPaymentId());
            result.setTotalAmount(paymentResult.getAmount());
            result.setCurrency(paymentResult.getCurrency());
            result.setProvider("Expedia");
            
            // Add activity details
            result.addBookingDetail("activityName", selectedActivity.getName());
            result.addBookingDetail("location", selectedActivity.getLocation());
            result.addBookingDetail("duration", selectedActivity.getDuration());
            result.addBookingDetail("difficulty", selectedActivity.getDifficulty());
            result.addBookingDetail("type", selectedActivity.getType());
            result.addBookingDetail("date", request.getDepartureDate().toString());
            result.addBookingDetail("participants", request.getPassengers());
            
            logger.info("Activity booking successful: {}", result.getBookingReference());
            return result;
            
        } catch (Exception e) {
            logger.error("Activity booking failed", e);
            return createErrorResult("ACTIVITY_BOOKING_ERROR", "Activity booking failed: " + e.getMessage());
        }
    }
    
    /**
     * Rank and select the best hotel based on criteria.
     */
    private Hotel rankAndSelectHotel(List<Hotel> hotels, BookingRequest request) {
        // Use the ranking from BookingComService or apply additional criteria
        Hotel bestHotel = hotels.get(0); // Already ranked by BookingComService
        
        // Apply budget constraint if specified
        if (request.getBudget() != null) {
            for (Hotel hotel : hotels) {
                if (hotel.getTotalPrice() != null && hotel.getTotalPrice() <= request.getBudget()) {
                    bestHotel = hotel;
                    break;
                }
            }
        }
        
        // Apply preference filters
        if (request.getPreferences() != null) {
            for (Hotel hotel : hotels) {
                if (matchesPreferences(hotel, request.getPreferences())) {
                    bestHotel = hotel;
                    break;
                }
            }
        }
        
        logger.info("Selected hotel: {} with price: {} {}", 
                   bestHotel.getName(), bestHotel.getTotalPrice(), bestHotel.getCurrency());
        return bestHotel;
    }
    
    /**
     * Check if hotel matches user preferences.
     */
    private boolean matchesPreferences(Hotel hotel, java.util.Map<String, Object> preferences) {
        // Check minimum rating preference
        if (preferences.containsKey("minRating")) {
            Double minRating = (Double) preferences.get("minRating");
            if (hotel.getRating() == null || hotel.getRating() < minRating) {
                return false;
            }
        }
        
        // Check amenities preference
        if (preferences.containsKey("amenities")) {
            @SuppressWarnings("unchecked")
            List<String> requiredAmenities = (List<String>) preferences.get("amenities");
            if (hotel.getAmenities() == null || !hotel.getAmenities().containsAll(requiredAmenities)) {
                return false;
            }
        }
        
        // Check location preference
        if (preferences.containsKey("location")) {
            String preferredLocation = (String) preferences.get("location");
            if (hotel.getAddress() == null || !hotel.getAddress().toLowerCase().contains(preferredLocation.toLowerCase())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Create payment request for hotel booking.
     */
    private PaymentRequest createHotelPaymentRequest(Hotel hotel, BookingRequest request) {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(hotel.getTotalPrice());
        paymentRequest.setCurrency(hotel.getCurrency());
        paymentRequest.setBookingType("hotel");
        paymentRequest.setUserId(request.getUserId());
        paymentRequest.setItineraryId(request.getItineraryId());
        paymentRequest.setDescription("Hotel booking: " + hotel.getName());
        return paymentRequest;
    }
    
    /**
     * Create payment request for flight booking.
     */
    private PaymentRequest createFlightPaymentRequest(Flight flight, BookingRequest request) {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(flight.getTotalPrice());
        paymentRequest.setCurrency(flight.getCurrency());
        paymentRequest.setBookingType("flight");
        paymentRequest.setUserId(request.getUserId());
        paymentRequest.setItineraryId(request.getItineraryId());
        paymentRequest.setDescription("Flight booking: " + flight.getFlightNumber());
        return paymentRequest;
    }
    
    /**
     * Create payment request for activity booking.
     */
    private PaymentRequest createActivityPaymentRequest(Activity activity, BookingRequest request) {
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(activity.getPrice());
        paymentRequest.setCurrency(activity.getCurrency());
        paymentRequest.setBookingType("activity");
        paymentRequest.setUserId(request.getUserId());
        paymentRequest.setItineraryId(request.getItineraryId());
        paymentRequest.setDescription("Activity booking: " + activity.getName());
        return paymentRequest;
    }
    
    /**
     * Simulate payment verification for demo purposes.
     * In real implementation, this would come from frontend after user completes payment.
     */
    private PaymentVerification simulatePaymentVerification(PaymentOrder paymentOrder) {
        PaymentVerification verification = new PaymentVerification();
        verification.setOrderId(paymentOrder.getOrderId());
        verification.setPaymentId("pay_" + UUID.randomUUID().toString().replace("-", ""));
        verification.setSignature("simulated_signature_" + System.currentTimeMillis());
        return verification;
    }
    
    /**
     * Update itinerary with booking details using flexible agent data structure.
     */
    private void updateItineraryWithBooking(String itineraryId, BookingResult result) {
        try {
            // Create flexible booking data structure
            java.util.Map<String, Object> bookingData = new java.util.HashMap<>();
            bookingData.put("confirmationNumber", result.getConfirmationNumber());
            bookingData.put("bookingReference", result.getBookingReference());
            bookingData.put("bookingType", result.getBookingType());
            bookingData.put("status", result.getStatus());
            bookingData.put("paymentId", result.getPaymentId());
            bookingData.put("totalAmount", result.getTotalAmount());
            bookingData.put("currency", result.getCurrency());
            bookingData.put("provider", result.getProvider());
            bookingData.put("timestamp", System.currentTimeMillis());
            bookingData.put("bookedAt", LocalDateTime.now().toString());
            
            // Add booking details if available
            if (result.getBookingDetails() != null && !result.getBookingDetails().isEmpty()) {
                bookingData.put("details", result.getBookingDetails());
            }
            
            // Add contact info if available
            if (result.getContactInfo() != null && !result.getContactInfo().isEmpty()) {
                bookingData.put("contactInfo", result.getContactInfo());
            }
            
            // Add cancellation policy if available
            if (result.getCancellationPolicy() != null) {
                bookingData.put("cancellationPolicy", result.getCancellationPolicy());
            }
            
            // Store using the new flexible agent data service
            itineraryJsonService.updateAgentData(itineraryId, "booking", bookingData);
            
            logger.info("Updated itinerary {} with flexible booking data: {}", itineraryId, result.getBookingReference());
            
        } catch (Exception e) {
            logger.error("Failed to update itinerary {} with booking details", itineraryId, e);
            // Don't throw exception as booking was successful
        }
    }
    
    /**
     * Validate booking request parameters.
     */
    private void validateBookingRequest(BookingRequest request) {
        if (request.getBookingType() == null || request.getBookingType().trim().isEmpty()) {
            throw new IllegalArgumentException("Booking type is required");
        }
        
        if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        
        // Validate based on booking type
        switch (request.getBookingType().toLowerCase()) {
            case "hotel":
                validateHotelRequest(request);
                break;
            case "flight":
                validateFlightRequest(request);
                break;
            case "activity":
                validateActivityRequest(request);
                break;
            default:
                throw new IllegalArgumentException("Unsupported booking type: " + request.getBookingType());
        }
    }
    
    /**
     * Validate hotel booking request.
     */
    private void validateHotelRequest(BookingRequest request) {
        if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Location is required for hotel booking");
        }
        
        if (request.getCheckInDate() == null) {
            throw new IllegalArgumentException("Check-in date is required for hotel booking");
        }
        
        if (request.getCheckOutDate() == null) {
            throw new IllegalArgumentException("Check-out date is required for hotel booking");
        }
        
        if (request.getCheckInDate().isAfter(request.getCheckOutDate())) {
            throw new IllegalArgumentException("Check-in date must be before check-out date");
        }
        
        if (request.getGuests() == null || request.getGuests() < 1) {
            throw new IllegalArgumentException("Number of guests must be at least 1");
        }
        
        if (request.getRooms() == null || request.getRooms() < 1) {
            throw new IllegalArgumentException("Number of rooms must be at least 1");
        }
    }
    
    /**
     * Validate flight booking request.
     */
    private void validateFlightRequest(BookingRequest request) {
        if (request.getOrigin() == null || request.getOrigin().trim().isEmpty()) {
            throw new IllegalArgumentException("Origin is required for flight booking");
        }
        
        if (request.getDestination() == null || request.getDestination().trim().isEmpty()) {
            throw new IllegalArgumentException("Destination is required for flight booking");
        }
        
        if (request.getOrigin().equals(request.getDestination())) {
            throw new IllegalArgumentException("Origin and destination cannot be the same");
        }
        
        if (request.getDepartureDate() == null) {
            throw new IllegalArgumentException("Departure date is required for flight booking");
        }
        
        if (request.getDepartureDate().isBefore(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Departure date cannot be in the past");
        }
        
        if (request.getPassengers() == null || request.getPassengers() < 1) {
            throw new IllegalArgumentException("Number of passengers must be at least 1");
        }
        
        if (request.getPassengers() > 9) {
            throw new IllegalArgumentException("Number of passengers cannot exceed 9");
        }
    }
    
    /**
     * Validate activity booking request.
     */
    private void validateActivityRequest(BookingRequest request) {
        if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
            throw new IllegalArgumentException("Location is required for activity booking");
        }
        
        if (request.getDepartureDate() == null) {
            throw new IllegalArgumentException("Date is required for activity booking");
        }
        
        if (request.getDepartureDate().isBefore(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Activity date cannot be in the past");
        }
        
        if (request.getPassengers() == null || request.getPassengers() < 1) {
            throw new IllegalArgumentException("Number of participants must be at least 1");
        }
    }
    
    /**
     * Create error result for failed bookings.
     */
    private BookingResult createErrorResult(String errorCode, String errorMessage) {
        BookingResult result = new BookingResult("unknown", "FAILED");
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }
    
    /**
     * Generate unique flight booking reference.
     */
    private String generateFlightReference() {
        return "FL" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Generate unique activity booking reference.
     */
    private String generateActivityReference() {
        return "AC" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
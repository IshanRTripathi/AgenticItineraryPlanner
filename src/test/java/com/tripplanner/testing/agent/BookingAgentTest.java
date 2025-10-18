package com.tripplanner.testing.agent;

import com.tripplanner.agents.BookingAgent;
import com.tripplanner.agents.BaseAgent;
import com.tripplanner.dto.*;
import com.tripplanner.service.*;
import com.tripplanner.testing.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Atomic tests for BookingAgent with mocked external services.
 */
class BookingAgentTest extends BaseServiceTest {
    
    @Mock
    private AgentEventBus mockEventBus;
    
    @Mock
    private BookingComService mockBookingComService;
    
    @Mock
    private ExpediaService mockExpediaService;
    
    @Mock
    private RazorpayService mockRazorpayService;
    
    @Mock
    private ItineraryJsonService mockItineraryJsonService;
    
    private BookingAgent bookingAgent;
    
    @BeforeEach
    protected void setUp() {
        super.setUp();
        bookingAgent = new BookingAgent(
            mockEventBus,
            mockBookingComService,
            mockExpediaService,
            mockRazorpayService,
            mockItineraryJsonService
        );
    }
    
    @Override
    protected void setupSpecificMocks() {
        // Setup event bus mock with lenient stubbing
        lenient().doNothing().when(mockEventBus).publish(anyString(), any(AgentEvent.class));
        
        // Setup other service mocks with lenient stubbing
        // Note: These will be mocked specifically in individual tests as needed
    }
    
    @Test
    @DisplayName("Should get booking agent capabilities")
    void shouldGetBookingAgentCapabilities() {
        // When
        AgentCapabilities capabilities = bookingAgent.getCapabilities();
        
        // Then
        assertThat(capabilities).isNotNull();
        assertThat(capabilities.getSupportedTasks()).contains("book"); // Single task type
        assertThat(capabilities.getSupportedTasks()).hasSize(1); // Zero-overlap design
        assertThat(capabilities.getPriority()).isEqualTo(30); // Medium priority for bookings
        assertThat(capabilities.isChatEnabled()).isTrue(); // Chat-enabled
        assertThat(capabilities.getConfigurationValue("requiresPayment")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("handlesReservations")).isEqualTo(true);
        assertThat(capabilities.getConfigurationValue("scopeType")).isEqualTo("booking_only");
        
        logger.info("Get booking agent capabilities test passed");
    }
    
    @Test
    @DisplayName("Should handle booking task types")
    void shouldHandleBookingTaskTypes() {
        // When/Then
        assertThat(bookingAgent.canHandle("book")).isTrue(); // Only handles "book"
        assertThat(bookingAgent.canHandle("booking")).isFalse(); // No longer supported (zero-overlap)
        assertThat(bookingAgent.canHandle("reserve")).isFalse(); // No longer supported (zero-overlap)
        assertThat(bookingAgent.canHandle("payment")).isFalse(); // No longer supported (zero-overlap)
        assertThat(bookingAgent.canHandle("unsupported")).isFalse();
        
        logger.info("Handle booking task types test passed");
    }
    
    @Test
    @DisplayName("Should handle booking task with context")
    void shouldHandleBookingTaskWithContext() {
        // Given - context is no longer used for validation in simplified canHandle
        Map<String, Object> bookingContext = Map.of("operation", "book_hotel");
        Map<String, Object> nonBookingContext = Map.of("operation", "enrich_place");
        
        // When/Then - canHandle only checks taskType now, not context
        assertThat(bookingAgent.canHandle("book", bookingContext)).isTrue();
        assertThat(bookingAgent.canHandle("book", nonBookingContext)).isTrue(); // Still true, only taskType matters
        assertThat(bookingAgent.canHandle("enrich", bookingContext)).isFalse(); // Wrong taskType
        
        logger.info("Handle booking task with context test passed");
    }
    
    @Test
    @DisplayName("Should execute hotel booking successfully")
    void shouldExecuteHotelBookingSuccessfully() {
        // Given
        String itineraryId = "test-itinerary-001";
        BookingRequest bookingRequest = createHotelBookingRequest();
        BaseAgent.AgentRequest<BookingResult> request = new BaseAgent.AgentRequest<>(bookingRequest, BookingResult.class);
        
        // Setup mocks
        setupSuccessfulHotelBookingMocks();
        
        // When
        BookingResult result = bookingAgent.execute(itineraryId, request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getBookingType()).isEqualTo("hotel");
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getConfirmationNumber()).isNotNull();
        assertThat(result.getProvider()).isEqualTo("Booking.com");
        
        // Verify service interactions
        verify(mockBookingComService).searchHotels(bookingRequest);
        verify(mockRazorpayService).createPaymentOrder(any(PaymentRequest.class));
        verify(mockRazorpayService).verifyPayment(any(PaymentVerification.class));
        verify(mockBookingComService).confirmBooking(any(Hotel.class), any(PaymentResult.class));
        
        // Verify events were published
        verify(mockEventBus, atLeast(3)).publish(eq(itineraryId), any(AgentEvent.class));
        
        logger.info("Execute hotel booking successfully test passed");
    }
    
    @Test
    @DisplayName("Should execute flight booking successfully")
    void shouldExecuteFlightBookingSuccessfully() {
        // Given
        String itineraryId = "test-itinerary-001";
        BookingRequest bookingRequest = createFlightBookingRequest();
        BaseAgent.AgentRequest<BookingResult> request = new BaseAgent.AgentRequest<>(bookingRequest, BookingResult.class);
        
        // Setup mocks
        setupSuccessfulFlightBookingMocks();
        
        // When
        BookingResult result = bookingAgent.execute(itineraryId, request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getBookingType()).isEqualTo("flight");
        assertThat(result.isSuccessful()).isTrue();
        
        // Verify service interactions
        verify(mockExpediaService).searchFlights(bookingRequest);
        verify(mockRazorpayService).createPaymentOrder(any(PaymentRequest.class));
        
        logger.info("Execute flight booking successfully test passed");
    }
    
    @Test
    @DisplayName("Should handle invalid booking request")
    void shouldHandleInvalidBookingRequest() {
        // Given
        String itineraryId = "test-itinerary-001";
        String invalidData = "invalid_data";
        BaseAgent.AgentRequest<BookingResult> request = new BaseAgent.AgentRequest<>(invalidData, BookingResult.class);
        
        // When
        BookingResult result = bookingAgent.execute(itineraryId, request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(result.getErrorMessage()).contains("BookingAgent requires BookingRequest data");
        
        logger.info("Handle invalid booking request test passed");
    }
    
    @Test
    @DisplayName("Should handle unsupported booking type")
    void shouldHandleUnsupportedBookingType() {
        // Given
        String itineraryId = "test-itinerary-001";
        BookingRequest bookingRequest = createBookingRequest("unsupported");
        BaseAgent.AgentRequest<BookingResult> request = new BaseAgent.AgentRequest<>(bookingRequest, BookingResult.class);
        
        // When
        BookingResult result = bookingAgent.execute(itineraryId, request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(result.getErrorMessage()).contains("Unsupported booking type: unsupported");
        
        logger.info("Handle unsupported booking type test passed");
    }
    
    @Test
    @DisplayName("Should handle hotel search failure")
    void shouldHandleHotelSearchFailure() {
        // Given
        String itineraryId = "test-itinerary-001";
        BookingRequest bookingRequest = createHotelBookingRequest();
        BaseAgent.AgentRequest<BookingResult> request = new BaseAgent.AgentRequest<>(bookingRequest, BookingResult.class);
        
        // Setup mocks for failure
        HotelSearchResponse failedResponse = new HotelSearchResponse();
        failedResponse.setStatus("ERROR");
        when(mockBookingComService.searchHotels(bookingRequest)).thenReturn(failedResponse);
        
        // When
        BookingResult result = bookingAgent.execute(itineraryId, request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("NO_HOTELS_FOUND");
        
        verify(mockBookingComService).searchHotels(bookingRequest);
        
        logger.info("Handle hotel search failure test passed");
    }
    
    @Test
    @DisplayName("Should handle payment failure")
    void shouldHandlePaymentFailure() {
        // Given
        String itineraryId = "test-itinerary-001";
        BookingRequest bookingRequest = createHotelBookingRequest();
        BaseAgent.AgentRequest<BookingResult> request = new BaseAgent.AgentRequest<>(bookingRequest, BookingResult.class);
        
        // Setup mocks for payment failure
        setupHotelSearchMocks();
        setupPaymentFailureMocks();
        
        // When
        BookingResult result = bookingAgent.execute(itineraryId, request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("PAYMENT_FAILED");
        
        verify(mockRazorpayService).verifyPayment(any(PaymentVerification.class));
        
        logger.info("Handle payment failure test passed");
    }
    
    @Test
    @DisplayName("Should handle booking confirmation failure with refund")
    void shouldHandleBookingConfirmationFailureWithRefund() {
        // Given
        String itineraryId = "test-itinerary-001";
        BookingRequest bookingRequest = createHotelBookingRequest();
        BaseAgent.AgentRequest<BookingResult> request = new BaseAgent.AgentRequest<>(bookingRequest, BookingResult.class);
        
        // Setup mocks for booking confirmation failure
        setupHotelSearchMocks();
        setupSuccessfulPaymentMocks();
        setupBookingConfirmationFailureMocks();
        
        // When
        BookingResult result = bookingAgent.execute(itineraryId, request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("BOOKING_FAILED");
        
        // Verify refund was processed
        verify(mockRazorpayService).processRefund(any(RefundRequest.class));
        
        logger.info("Handle booking confirmation failure with refund test passed");
    }
    
    // Helper methods to create test data
    
    private BookingRequest createHotelBookingRequest() {
        return createBookingRequest("hotel");
    }
    
    private BookingRequest createFlightBookingRequest() {
        BookingRequest request = createBookingRequest("flight");
        request.setOrigin("New York");
        request.setDestination("Los Angeles");
        request.setDepartureDate(LocalDate.now().plusDays(30));
        request.setReturnDate(LocalDate.now().plusDays(35));
        request.setPassengers(2);
        return request;
    }
    
    private BookingRequest createBookingRequest(String bookingType) {
        BookingRequest request = new BookingRequest();
        request.setBookingType(bookingType);
        request.setLocation("Bali, Indonesia");
        request.setCheckInDate(LocalDate.now().plusDays(30));
        request.setCheckOutDate(LocalDate.now().plusDays(33));
        request.setGuests(2);
        request.setRooms(1);
        request.setBudget(500.0);
        request.setCurrency("USD");
        request.setUserId("test-user-123");
        request.setItineraryId("test-itinerary-001");
        return request;
    }
    
    // Helper methods to setup mocks
    
    private void setupSuccessfulHotelBookingMocks() {
        setupHotelSearchMocks();
        setupSuccessfulPaymentMocks();
        setupSuccessfulBookingConfirmationMocks();
    }
    
    private void setupSuccessfulFlightBookingMocks() {
        // Setup flight search mock
        FlightSearchResponse flightResponse = new FlightSearchResponse();
        flightResponse.setStatus("SUCCESS");
        
        Flight mockFlight = new Flight();
        mockFlight.setAirline("Mock Airlines");
        mockFlight.setFlightNumber("MA123");
        mockFlight.setPrice(450.0);
        mockFlight.setDepartureTime(LocalDateTime.now().plusDays(30));
        mockFlight.setArrivalTime(LocalDateTime.now().plusDays(30).plusHours(8));
        mockFlight.setOrigin("New York");
        mockFlight.setDestination("Los Angeles");
        flightResponse.setFlights(Arrays.asList(mockFlight));
        
        when(mockExpediaService.searchFlights(any(BookingRequest.class))).thenReturn(flightResponse);
        
        // Setup flight booking confirmation mock
        BookingConfirmation confirmation = new BookingConfirmation();
        confirmation.setStatus("CONFIRMED");
        confirmation.setConfirmationNumber("CONF-FLIGHT-123");
        confirmation.setBookingReference("REF-FLIGHT-789");
        confirmation.setContactEmail("passenger@airline.com");
        confirmation.setContactPhone("+1-800-FLY-NOW");
        when(mockExpediaService.confirmFlightBooking(any(Flight.class), any(PaymentResult.class))).thenReturn(confirmation);
        
        setupSuccessfulPaymentMocks();
    }
    
    private void setupHotelSearchMocks() {
        HotelSearchResponse hotelResponse = new HotelSearchResponse();
        hotelResponse.setStatus("SUCCESS");
        
        Hotel mockHotel = new Hotel();
        mockHotel.setName("Mock Luxury Resort");
        mockHotel.setAddress("123 Paradise Street, Bali");
        mockHotel.setRating(5.0);
        mockHotel.setPricePerNight(250.0);
        mockHotel.setCancellationPolicy("Free cancellation until 24 hours before check-in");
        hotelResponse.setHotels(Arrays.asList(mockHotel));
        
        when(mockBookingComService.searchHotels(any(BookingRequest.class))).thenReturn(hotelResponse);
    }
    
    private void setupSuccessfulPaymentMocks() {
        // Mock payment order creation
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setOrderId("order_123456");
        paymentOrder.setAmount(750.00); // 750.00 in currency units
        paymentOrder.setCurrency("INR");
        when(mockRazorpayService.createPaymentOrder(any(PaymentRequest.class))).thenReturn(paymentOrder);
        
        // Mock payment verification
        PaymentResult paymentResult = new PaymentResult();
        paymentResult.setStatus("SUCCESS");
        paymentResult.setPaymentId("pay_789012");
        paymentResult.setAmount(750.00);
        paymentResult.setCurrency("INR");
        when(mockRazorpayService.verifyPayment(any(PaymentVerification.class))).thenReturn(paymentResult);
    }
    
    private void setupPaymentFailureMocks() {
        // Mock payment order creation
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setOrderId("order_123456");
        when(mockRazorpayService.createPaymentOrder(any(PaymentRequest.class))).thenReturn(paymentOrder);
        
        // Mock payment verification failure
        PaymentResult paymentResult = new PaymentResult();
        paymentResult.setStatus("FAILED");
        paymentResult.setErrorMessage("Payment declined");
        when(mockRazorpayService.verifyPayment(any(PaymentVerification.class))).thenReturn(paymentResult);
    }
    
    private void setupSuccessfulBookingConfirmationMocks() {
        BookingConfirmation confirmation = new BookingConfirmation();
        confirmation.setStatus("CONFIRMED");
        confirmation.setConfirmationNumber("CONF-123456");
        confirmation.setBookingReference("REF-789012");
        confirmation.setContactEmail("guest@hotel.com");
        confirmation.setContactPhone("+62-361-123456");
        when(mockBookingComService.confirmBooking(any(Hotel.class), any(PaymentResult.class))).thenReturn(confirmation);
    }
    
    private void setupBookingConfirmationFailureMocks() {
        BookingConfirmation confirmation = new BookingConfirmation();
        confirmation.setStatus("FAILED");
        confirmation.setErrorMessage("Hotel fully booked");
        when(mockBookingComService.confirmBooking(any(Hotel.class), any(PaymentResult.class))).thenReturn(confirmation);
        
        // Mock refund processing
        RefundResult refundResult = new RefundResult();
        refundResult.setStatus("SUCCESS");
        refundResult.setRefundId("refund_123");
        when(mockRazorpayService.processRefund(any(RefundRequest.class))).thenReturn(refundResult);
    }
}
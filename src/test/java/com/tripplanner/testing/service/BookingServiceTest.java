package com.tripplanner.testing.service;

import com.tripplanner.controller.BookingController;
import com.tripplanner.data.entity.Booking;
import com.tripplanner.data.repo.BookingRepository;
import com.tripplanner.service.BookingService;
import com.tripplanner.service.RazorpayService;
import com.tripplanner.testing.BaseServiceTest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Atomic tests for BookingService with mocked payment and booking providers.
 */
class BookingServiceTest extends BaseServiceTest {
    
    @Mock
    private RazorpayService mockRazorpayService;
    
    @Mock
    private BookingRepository mockBookingRepository;
    
    @Mock
    private HttpServletRequest mockHttpRequest;
    
    private BookingService bookingService;
    
    @BeforeEach
    protected void setUp() {
        super.setUp();
        bookingService = new BookingService(mockRazorpayService, mockBookingRepository);
    }
    
    @Override
    protected void setupSpecificMocks() {
        // No additional mocks needed for BookingService tests
    }
    
    @Test
    @DisplayName("Should create Razorpay order successfully")
    void shouldCreateRazorpayOrderSuccessfully() {
        // Given
        BookingController.RazorpayOrderReq request = new BookingController.RazorpayOrderReq(
            "hotel",
            "it_bali_luxury_001",
            50000L, // 500.00 in paise
            "INR",
            Map.of("hotelId", "hotel-123", "roomType", "deluxe")
        );
        
        BookingController.RazorpayOrderRes expectedResponse = new BookingController.RazorpayOrderRes(
            "order_123456789",
            50000L,
            "INR",
            "receipt_001"
        );
        
        when(mockRazorpayService.createOrder(request)).thenReturn(expectedResponse);
        
        // When
        BookingController.RazorpayOrderRes result = bookingService.createRazorpayOrder(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo("order_123456789");
        assertThat(result.amount()).isEqualTo(50000L);
        assertThat(result.currency()).isEqualTo("INR");
        assertThat(result.receipt()).isEqualTo("receipt_001");
        
        verify(mockRazorpayService).createOrder(request);
        
        logger.info("Create Razorpay order test passed");
    }
    
    @Test
    @DisplayName("Should handle Razorpay order creation failure")
    void shouldHandleRazorpayOrderCreationFailure() {
        // Given
        BookingController.RazorpayOrderReq request = new BookingController.RazorpayOrderReq(
            "hotel",
            "it_bali_luxury_001",
            50000L,
            "INR",
            Map.of()
        );
        
        when(mockRazorpayService.createOrder(request))
                .thenThrow(new RuntimeException("Payment service unavailable"));
        
        // When/Then
        assertThatThrownBy(() -> bookingService.createRazorpayOrder(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment service unavailable");
        
        verify(mockRazorpayService).createOrder(request);
        
        logger.info("Razorpay order creation failure test passed");
    }
    
    @Test
    @DisplayName("Should handle Razorpay webhook successfully")
    void shouldHandleRazorpayWebhookSuccessfully() {
        // Given
        String webhookBody = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"id\":\"pay_123\"}}}";
        
        when(mockHttpRequest.getRequestURI()).thenReturn("/api/webhooks/razorpay");
        when(mockHttpRequest.getContentType()).thenReturn("application/json");
        when(mockHttpRequest.getContentLength()).thenReturn(webhookBody.length());
        when(mockHttpRequest.getHeaderNames()).thenReturn(java.util.Collections.enumeration(Arrays.asList("X-Razorpay-Signature")));
        when(mockHttpRequest.getHeader("X-Razorpay-Signature")).thenReturn("signature123");
        
        doNothing().when(mockRazorpayService).handleWebhook(mockHttpRequest, webhookBody);
        
        // When
        bookingService.handleRazorpayWebhook(mockHttpRequest, webhookBody);
        
        // Then
        verify(mockRazorpayService).handleWebhook(mockHttpRequest, webhookBody);
        
        logger.info("Razorpay webhook handling test passed");
    }
    
    @Test
    @DisplayName("Should handle webhook processing failure")
    void shouldHandleWebhookProcessingFailure() {
        // Given
        String webhookBody = "invalid webhook body";
        
        when(mockHttpRequest.getRequestURI()).thenReturn("/api/webhooks/razorpay");
        when(mockHttpRequest.getContentType()).thenReturn("application/json");
        when(mockHttpRequest.getContentLength()).thenReturn(webhookBody.length());
        when(mockHttpRequest.getHeaderNames()).thenReturn(java.util.Collections.enumeration(Arrays.asList()));
        
        doThrow(new RuntimeException("Invalid webhook signature"))
                .when(mockRazorpayService).handleWebhook(mockHttpRequest, webhookBody);
        
        // When/Then
        assertThatThrownBy(() -> bookingService.handleRazorpayWebhook(mockHttpRequest, webhookBody))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid webhook signature");
        
        verify(mockRazorpayService).handleWebhook(mockHttpRequest, webhookBody);
        
        logger.info("Webhook processing failure test passed");
    }
    
    @Test
    @DisplayName("Should execute provider booking successfully")
    void shouldExecuteProviderBookingSuccessfully() {
        // Given
        String userId = "user-123";
        String vertical = "hotel";
        String provider = "booking.com";
        
        BookingController.PaymentDetails paymentDetails = new BookingController.PaymentDetails(
            "order_123456789",
            "pay_987654321",
            "signature_abc123"
        );
        
        BookingController.ItemDetails itemDetails = new BookingController.ItemDetails(
            "hotel-token-123",
            Map.of("roomType", "deluxe", "checkIn", "2024-01-24")
        );
        
        BookingController.ProviderBookReq request = new BookingController.ProviderBookReq(
            paymentDetails,
            itemDetails,
            "it_bali_luxury_001"
        );
        
        Booking mockBooking = createMockBooking(userId, "order_123456789");
        String expectedBookingId = "BK_1234567890_TEST1234";
        mockBooking.setBookingId(expectedBookingId);
        
        when(mockBookingRepository.findByRazorpayOrderId("order_123456789"))
                .thenReturn(Optional.of(mockBooking));
        when(mockBookingRepository.save(any(Booking.class))).thenReturn(mockBooking);
        
        // When
        BookingController.BookingRes result = bookingService.executeProviderBooking(userId, vertical, provider, request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.bookingId()).isEqualTo(expectedBookingId);
        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.providerConfirmationId()).isNotNull();
        assertThat(result.providerConfirmationId()).startsWith("CONF-");
        assertThat(result.itineraryId()).isEqualTo("it_bali_luxury_001");
        assertThat(result.details()).containsEntry("provider", provider);
        assertThat(result.details()).containsEntry("vertical", vertical);
        
        verify(mockBookingRepository).findByRazorpayOrderId("order_123456789");
        verify(mockBookingRepository).save(argThat(booking -> 
            booking.getStatus() == Booking.BookingStatus.CONFIRMED &&
            booking.getProvider() != null &&
            booking.getProvider().getConfirmationId() != null
        ));
        
        logger.info("Execute provider booking test passed");
    }
    
    @Test
    @DisplayName("Should fail when booking not found for order")
    void shouldFailWhenBookingNotFoundForOrder() {
        // Given
        String userId = "user-123";
        String vertical = "hotel";
        String provider = "booking.com";
        
        BookingController.PaymentDetails paymentDetails = new BookingController.PaymentDetails(
            "non_existent_order",
            "pay_987654321",
            "signature_abc123"
        );
        
        BookingController.ItemDetails itemDetails = new BookingController.ItemDetails(
            "hotel-token-456",
            Map.of()
        );
        
        BookingController.ProviderBookReq request = new BookingController.ProviderBookReq(
            paymentDetails,
            itemDetails,
            "it_bali_luxury_001"
        );
        
        when(mockBookingRepository.findByRazorpayOrderId("non_existent_order"))
                .thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> bookingService.executeProviderBooking(userId, vertical, provider, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Booking not found for order: non_existent_order");
        
        verify(mockBookingRepository).findByRazorpayOrderId("non_existent_order");
        verifyNoMoreInteractions(mockBookingRepository);
        
        logger.info("Booking not found test passed");
    }
    
    @Test
    @DisplayName("Should fail when user does not own booking")
    void shouldFailWhenUserDoesNotOwnBooking() {
        // Given
        String userId = "user-123";
        String wrongUserId = "wrong-user-456";
        String vertical = "hotel";
        String provider = "booking.com";
        
        BookingController.PaymentDetails paymentDetails = new BookingController.PaymentDetails(
            "order_123456789",
            "pay_987654321",
            "signature_abc123"
        );
        
        BookingController.ItemDetails itemDetails = new BookingController.ItemDetails(
            "hotel-token-789",
            Map.of()
        );
        
        BookingController.ProviderBookReq request = new BookingController.ProviderBookReq(
            paymentDetails,
            itemDetails,
            "it_bali_luxury_001"
        );
        
        Booking mockBooking = createMockBooking(wrongUserId, "order_123456789"); // Different user
        
        when(mockBookingRepository.findByRazorpayOrderId("order_123456789"))
                .thenReturn(Optional.of(mockBooking));
        
        // When/Then
        assertThatThrownBy(() -> bookingService.executeProviderBooking(userId, vertical, provider, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User user-123 does not own booking 1");
        
        verify(mockBookingRepository).findByRazorpayOrderId("order_123456789");
        verifyNoMoreInteractions(mockBookingRepository);
        
        logger.info("User ownership validation test passed");
    }
    
    @Test
    @DisplayName("Should get booking by ID successfully")
    void shouldGetBookingByIdSuccessfully() {
        // Given
        String userId = "user-123";
        String bookingId = "BK_1234567890_ABCD1234";
        
        Booking mockBooking = createMockBookingWithProvider(userId, "order_123456789");
        mockBooking.setBookingId(bookingId);
        
        when(mockBookingRepository.findByBookingId(bookingId)).thenReturn(Optional.of(mockBooking));
        
        // When
        BookingController.BookingRes result = bookingService.getBooking(userId, bookingId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.bookingId()).isEqualTo(bookingId);
        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.providerConfirmationId()).isEqualTo("CONF-12345678");
        assertThat(result.itineraryId()).isEqualTo("it_bali_luxury_001");
        
        verify(mockBookingRepository).findByBookingId(bookingId);
        
        logger.info("Get booking by ID test passed");
    }
    
    @Test
    @DisplayName("Should fail to get booking when not found")
    void shouldFailToGetBookingWhenNotFound() {
        // Given
        String userId = "user-123";
        String bookingId = "BK_1234567890_ABCD1234";
        
        when(mockBookingRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> bookingService.getBooking(userId, bookingId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get booking");
        
        verify(mockBookingRepository).findByBookingId(bookingId);
        
        logger.info("Booking not found by ID test passed");
    }
    
    @Test
    @DisplayName("Should get user bookings successfully")
    void shouldGetUserBookingsSuccessfully() {
        // Given
        int page = 0;
        int size = 10;
        
        Booking booking1 = createMockBookingWithProvider("user-123", "order_1");
        Booking booking2 = createMockBookingWithProvider("user-456", "order_2");
        booking1.setBookingId("BK_1234567890_TEST0001");
        booking2.setBookingId("BK_1234567890_TEST0002");
        
        List<Booking> mockBookings = Arrays.asList(booking1, booking2);
        
        when(mockBookingRepository.findAll()).thenReturn(mockBookings);
        
        // When
        List<BookingController.BookingRes> result = bookingService.getUserBookings(page, size);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).bookingId()).isEqualTo("BK_1234567890_TEST0001");
        assertThat(result.get(0).status()).isEqualTo("CONFIRMED");
        assertThat(result.get(1).bookingId()).isEqualTo("BK_1234567890_TEST0002");
        assertThat(result.get(1).status()).isEqualTo("CONFIRMED");
        
        verify(mockBookingRepository).findAll();
        
        logger.info("Get user bookings test passed");
    }
    
    @Test
    @DisplayName("Should handle invalid booking ID format")
    void shouldHandleInvalidBookingIdFormat() {
        // Given
        String userId = "user-123";
        String invalidBookingId = "invalid-id";
        
        when(mockBookingRepository.findByBookingId(invalidBookingId)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> bookingService.getBooking(userId, invalidBookingId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to get booking");
        
        verify(mockBookingRepository).findByBookingId(invalidBookingId);
        
        logger.info("Invalid booking ID format test passed");
    }
    
    // Helper methods to create mock objects
    
    private Booking createMockBooking(String userId, String orderId) {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUserId(userId);
        booking.setItineraryId("it_bali_luxury_001");
        Booking.RazorpayDetails razorpayDetails = new Booking.RazorpayDetails();
        razorpayDetails.setOrderId(orderId);
        booking.setRazorpay(razorpayDetails);
        booking.setStatus(Booking.BookingStatus.INIT);
        booking.setCreatedAt(java.time.Instant.now());
        return booking;
    }
    
    private Booking createMockBookingWithProvider(String userId, String orderId) {
        Booking booking = createMockBooking(userId, orderId);
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        
        Booking.ProviderDetails providerDetails = new Booking.ProviderDetails();
        providerDetails.setConfirmationId("CONF-12345678");
        providerDetails.setStatus("confirmed");
        providerDetails.setBookingReference("REF-987654321");
        providerDetails.setContactInfoJson("{\"phone\":\"+1-555-PROVIDER\",\"email\":\"support@provider.com\"}");
        
        booking.setProvider(providerDetails);
        return booking;
    }
}
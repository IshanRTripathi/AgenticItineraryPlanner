package com.tripplanner.testing;

import com.tripplanner.service.*;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for mocking external services.
 * Provides mock implementations for all external API services.
 */
@TestConfiguration
public class MockExternalServicesConfiguration {
    
    /**
     * Mock Google Places Service for place search and details.
     */
    @Bean
    @Primary
    public GooglePlacesService mockGooglePlacesService() {
        return Mockito.mock(GooglePlacesService.class);
    }
    
    /**
     * Mock Razorpay Service for payment processing.
     */
    @Bean
    @Primary
    public RazorpayService mockRazorpayService() {
        return Mockito.mock(RazorpayService.class);
    }
    
    /**
     * Mock Expedia Service for flights and activities.
     */
    @Bean
    @Primary
    public ExpediaService mockExpediaService() {
        return Mockito.mock(ExpediaService.class);
    }
    
    /**
     * Mock Booking.com Service for hotel bookings.
     */
    @Bean
    @Primary
    public BookingComService mockBookingComService() {
        return Mockito.mock(BookingComService.class);
    }
    
    /**
     * Mock LLM Provider for AI responses.
     */
    @Bean
    @Primary
    public com.tripplanner.service.ai.AiClient mockAiClient() {
        return new MockLLMProvider();
    }
}
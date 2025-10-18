package com.tripplanner.testing.service;

import com.tripplanner.dto.*;
import com.tripplanner.service.GooglePlacesService;
import com.tripplanner.testing.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

/**
 * Atomic tests for GooglePlacesService with mocked external API calls.
 */
class GooglePlacesServiceTest extends BaseServiceTest {
    
    @Mock
    private RestTemplate mockRestTemplate;
    
    private GooglePlacesService googlePlacesService;
    
    @BeforeEach
    protected void setUp() {
        super.setUp();
        googlePlacesService = new GooglePlacesService(mockRestTemplate);
        
        // Set test configuration values using test properties
        ReflectionTestUtils.setField(googlePlacesService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(googlePlacesService, "dailyLimit", 1000);
        ReflectionTestUtils.setField(googlePlacesService, "rateLimitEnabled", false); // Disable for testing
    }
    
    @Override
    protected void setupSpecificMocks() {
        // No additional mocks needed for GooglePlacesService tests
    }
    
    @Test
    @DisplayName("Should get place details successfully")
    void shouldGetPlaceDetailsSuccessfully() {
        // Given
        String placeId = "ChIJoQ8Q6NNB0i0RkOYkS7EPkSQ";
        PlaceDetailsResponse mockResponse = createMockPlaceDetailsResponse();
        
        when(mockRestTemplate.getForEntity(anyString(), eq(PlaceDetailsResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        
        // When
        PlaceDetails result = googlePlacesService.getPlaceDetails(placeId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Place");
        assertThat(result.getFormattedAddress()).isEqualTo("123 Test Street, Test City");
        assertThat(result.getRating()).isEqualTo(4.5);
        assertThat(result.getPriceLevel()).isEqualTo(2);
        
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockRestTemplate).getForEntity(urlCaptor.capture(), eq(PlaceDetailsResponse.class));
        
        String capturedUrl = urlCaptor.getValue();
        assertThat(capturedUrl).contains("place_id=" + placeId);
        assertThat(capturedUrl).contains("key=test-api-key");
        assertThat(capturedUrl).contains("fields=");
        
        logger.info("Get place details test passed");
    }
    
    @Test
    @DisplayName("Should throw exception when place ID is null or empty")
    void shouldThrowExceptionWhenPlaceIdIsNullOrEmpty() {
        // When/Then - null place ID
        assertThatThrownBy(() -> googlePlacesService.getPlaceDetails(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Place ID cannot be null or empty");
        
        // When/Then - empty place ID
        assertThatThrownBy(() -> googlePlacesService.getPlaceDetails(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Place ID cannot be null or empty");
        
        // When/Then - whitespace place ID
        assertThatThrownBy(() -> googlePlacesService.getPlaceDetails("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Place ID cannot be null or empty");
        
        verifyNoInteractions(mockRestTemplate);
        
        logger.info("Place ID validation test passed");
    }
    
    @Test
    @DisplayName("Should handle API rate limit error")
    void shouldHandleApiRateLimitError() {
        // Given
        String placeId = "test-place-id";
        PlaceDetailsResponse rateLimitResponse = new PlaceDetailsResponse();
        rateLimitResponse.setStatus("OVER_QUERY_LIMIT");
        
        when(mockRestTemplate.getForEntity(anyString(), eq(PlaceDetailsResponse.class)))
                .thenReturn(new ResponseEntity<>(rateLimitResponse, HttpStatus.OK));
        
        // When/Then
        assertThatThrownBy(() -> googlePlacesService.getPlaceDetails(placeId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Google Places API rate limit exceeded");
        
        verify(mockRestTemplate).getForEntity(anyString(), eq(PlaceDetailsResponse.class));
        
        logger.info("API rate limit handling test passed");
    }
    
    @Test
    @DisplayName("Should handle place not found error")
    void shouldHandlePlaceNotFoundError() {
        // Given
        String placeId = "non-existent-place-id";
        PlaceDetailsResponse notFoundResponse = new PlaceDetailsResponse();
        notFoundResponse.setStatus("NOT_FOUND");
        
        when(mockRestTemplate.getForEntity(anyString(), eq(PlaceDetailsResponse.class)))
                .thenReturn(new ResponseEntity<>(notFoundResponse, HttpStatus.OK));
        
        // When/Then
        assertThatThrownBy(() -> googlePlacesService.getPlaceDetails(placeId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Place not found: " + placeId);
        
        verify(mockRestTemplate).getForEntity(anyString(), eq(PlaceDetailsResponse.class));
        
        logger.info("Place not found handling test passed");
    }
    
    @Test
    @DisplayName("Should retry on HTTP 429 Too Many Requests")
    void shouldRetryOnHttp429TooManyRequests() {
        // Given
        String placeId = "test-place-id";
        PlaceDetailsResponse successResponse = createMockPlaceDetailsResponse();
        
        // First call throws 429, second call succeeds
        when(mockRestTemplate.getForEntity(anyString(), eq(PlaceDetailsResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS))
                .thenReturn(new ResponseEntity<>(successResponse, HttpStatus.OK));
        
        // When
        PlaceDetails result = googlePlacesService.getPlaceDetails(placeId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Place");
        
        verify(mockRestTemplate, times(2)).getForEntity(anyString(), eq(PlaceDetailsResponse.class));
        
        logger.info("HTTP 429 retry test passed");
    }
    
    @Test
    @DisplayName("Should retry on server errors")
    void shouldRetryOnServerErrors() {
        // Given
        String placeId = "test-place-id";
        PlaceDetailsResponse successResponse = createMockPlaceDetailsResponse();
        
        // First call throws 500, second call succeeds
        when(mockRestTemplate.getForEntity(anyString(), eq(PlaceDetailsResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .thenReturn(new ResponseEntity<>(successResponse, HttpStatus.OK));
        
        // When
        PlaceDetails result = googlePlacesService.getPlaceDetails(placeId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Place");
        
        verify(mockRestTemplate, times(2)).getForEntity(anyString(), eq(PlaceDetailsResponse.class));
        
        logger.info("Server error retry test passed");
    }
    
    @Test
    @DisplayName("Should fail after max retries exceeded")
    void shouldFailAfterMaxRetriesExceeded() {
        // Given
        String placeId = "test-place-id";
        
        when(mockRestTemplate.getForEntity(anyString(), eq(PlaceDetailsResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
        
        // When/Then
        assertThatThrownBy(() -> googlePlacesService.getPlaceDetails(placeId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Server error after 5 attempts");
        
        verify(mockRestTemplate, times(5)).getForEntity(anyString(), eq(PlaceDetailsResponse.class));
        
        logger.info("Max retries exceeded test passed");
    }
    
    @Test
    @DisplayName("Should get place photos successfully")
    void shouldGetPlacePhotosSuccessfully() {
        // Given
        String placeId = "test-place-id";
        PlaceDetailsResponse mockResponse = createMockPlaceDetailsResponseWithPhotos();
        
        when(mockRestTemplate.getForEntity(anyString(), eq(PlaceDetailsResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        
        // When
        List<Photo> result = googlePlacesService.getPlacePhotos(placeId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPhotoReference()).isEqualTo("photo-ref-1");
        assertThat(result.get(1).getPhotoReference()).isEqualTo("photo-ref-2");
        
        verify(mockRestTemplate).getForEntity(anyString(), eq(PlaceDetailsResponse.class));
        
        logger.info("Get place photos test passed");
    }
    
    @Test
    @DisplayName("Should return empty list when no photos available")
    void shouldReturnEmptyListWhenNoPhotosAvailable() {
        // Given
        String placeId = "test-place-id";
        PlaceDetailsResponse mockResponse = createMockPlaceDetailsResponse(); // No photos
        
        when(mockRestTemplate.getForEntity(anyString(), eq(PlaceDetailsResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        
        // When
        List<Photo> result = googlePlacesService.getPlacePhotos(placeId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        
        verify(mockRestTemplate).getForEntity(anyString(), eq(PlaceDetailsResponse.class));
        
        logger.info("No photos available test passed");
    }
    
    @Test
    @DisplayName("Should get place reviews successfully")
    void shouldGetPlaceReviewsSuccessfully() {
        // Given
        String placeId = "test-place-id";
        PlaceDetailsResponse mockResponse = createMockPlaceDetailsResponseWithReviews();
        
        when(mockRestTemplate.getForEntity(anyString(), eq(PlaceDetailsResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        
        // When
        List<Review> result = googlePlacesService.getPlaceReviews(placeId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAuthorName()).isEqualTo("John Doe");
        assertThat(result.get(0).getRating()).isEqualTo(5);
        assertThat(result.get(1).getAuthorName()).isEqualTo("Jane Smith");
        assertThat(result.get(1).getRating()).isEqualTo(4);
        
        verify(mockRestTemplate).getForEntity(anyString(), eq(PlaceDetailsResponse.class));
        
        logger.info("Get place reviews test passed");
    }
    
    @Test
    @DisplayName("Should return empty list when photos request fails")
    void shouldReturnEmptyListWhenPhotosRequestFails() {
        // Given
        String placeId = "test-place-id";
        
        when(mockRestTemplate.getForEntity(anyString(), eq(PlaceDetailsResponse.class)))
                .thenThrow(new RuntimeException("API error"));
        
        // When
        List<Photo> result = googlePlacesService.getPlacePhotos(placeId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        
        verify(mockRestTemplate, atLeastOnce()).getForEntity(anyString(), eq(PlaceDetailsResponse.class));
        
        logger.info("Photos request failure handling test passed");
    }
    
    @Test
    @DisplayName("Should return empty list when reviews request fails")
    void shouldReturnEmptyListWhenReviewsRequestFails() {
        // Given
        String placeId = "test-place-id";
        
        when(mockRestTemplate.getForEntity(anyString(), eq(PlaceDetailsResponse.class)))
                .thenThrow(new RuntimeException("API error"));
        
        // When
        List<Review> result = googlePlacesService.getPlaceReviews(placeId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        
        verify(mockRestTemplate, atLeastOnce()).getForEntity(anyString(), eq(PlaceDetailsResponse.class));
        
        logger.info("Reviews request failure handling test passed");
    }
    
    @Test
    @DisplayName("Should handle non-retryable client errors")
    void shouldHandleNonRetryableClientErrors() {
        // Given
        String placeId = "test-place-id";
        
        when(mockRestTemplate.getForEntity(anyString(), eq(PlaceDetailsResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        
        // When/Then
        assertThatThrownBy(() -> googlePlacesService.getPlaceDetails(placeId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Google Places API client error");
        
        verify(mockRestTemplate, times(1)).getForEntity(anyString(), eq(PlaceDetailsResponse.class));
        
        logger.info("Non-retryable client error test passed");
    }
    
    // Helper methods to create mock responses
    
    private PlaceDetailsResponse createMockPlaceDetailsResponse() {
        PlaceDetailsResponse response = new PlaceDetailsResponse();
        response.setStatus("OK");
        
        PlaceDetails placeDetails = new PlaceDetails();
        placeDetails.setName("Test Place");
        placeDetails.setFormattedAddress("123 Test Street, Test City");
        placeDetails.setRating(4.5);
        placeDetails.setPriceLevel(2);
        placeDetails.setPlaceId("ChIJoQ8Q6NNB0i0RkOYkS7EPkSQ");
        
        // Set geometry
        PlaceDetails.Geometry geometry = new PlaceDetails.Geometry();
        PlaceDetails.Geometry.Location location = new PlaceDetails.Geometry.Location();
        location.setLat(-8.7467);
        location.setLng(115.1671);
        geometry.setLocation(location);
        placeDetails.setGeometry(geometry);
        
        // Set opening hours
        PlaceDetails.OpeningHours openingHours = new PlaceDetails.OpeningHours();
        openingHours.setOpenNow(true);
        placeDetails.setOpeningHours(openingHours);
        
        response.setResult(placeDetails);
        return response;
    }
    
    private PlaceDetailsResponse createMockPlaceDetailsResponseWithPhotos() {
        PlaceDetailsResponse response = createMockPlaceDetailsResponse();
        
        Photo photo1 = new Photo();
        photo1.setPhotoReference("photo-ref-1");
        photo1.setWidth(1024);
        photo1.setHeight(768);
        
        Photo photo2 = new Photo();
        photo2.setPhotoReference("photo-ref-2");
        photo2.setWidth(800);
        photo2.setHeight(600);
        
        response.getResult().setPhotos(Arrays.asList(photo1, photo2));
        return response;
    }
    
    private PlaceDetailsResponse createMockPlaceDetailsResponseWithReviews() {
        PlaceDetailsResponse response = createMockPlaceDetailsResponse();
        
        Review review1 = new Review();
        review1.setAuthorName("John Doe");
        review1.setRating(5);
        review1.setText("Excellent place!");
        review1.setTime(1640995200L); // Unix timestamp
        
        Review review2 = new Review();
        review2.setAuthorName("Jane Smith");
        review2.setRating(4);
        review2.setText("Very good experience");
        review2.setTime(1640908800L); // Unix timestamp
        
        response.getResult().setReviews(Arrays.asList(review1, review2));
        return response;
    }
}
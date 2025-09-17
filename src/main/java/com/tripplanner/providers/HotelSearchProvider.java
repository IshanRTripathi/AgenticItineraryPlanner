package com.tripplanner.providers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service Provider Interface for hotel search and booking.
 */
public interface HotelSearchProvider {
    
    /**
     * Search for hotels based on criteria.
     */
    List<HotelOption> search(HotelQuery query);
    
    /**
     * Book a hotel using the provider's booking token.
     */
    BookingResult book(BookingRequest request);
    
    /**
     * Get provider name.
     */
    String getProviderName();
    
    /**
     * Check if provider is available.
     */
    boolean isAvailable();
    
    /**
     * Hotel search query.
     */
    class HotelQuery {
        private double lat;
        private double lng;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private int adults;
        private int children;
        private int rooms;
        private String budgetTier;
        private Map<String, Object> filters;
        
        // Constructors
        public HotelQuery() {}
        
        public HotelQuery(double lat, double lng, LocalDate checkIn, LocalDate checkOut, 
                         int adults, int children, int rooms, String budgetTier) {
            this.lat = lat;
            this.lng = lng;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.adults = adults;
            this.children = children;
            this.rooms = rooms;
            this.budgetTier = budgetTier;
        }
        
        // Getters and Setters
        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLng() { return lng; }
        public void setLng(double lng) { this.lng = lng; }
        public LocalDate getCheckIn() { return checkIn; }
        public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
        public LocalDate getCheckOut() { return checkOut; }
        public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
        public int getAdults() { return adults; }
        public void setAdults(int adults) { this.adults = adults; }
        public int getChildren() { return children; }
        public void setChildren(int children) { this.children = children; }
        public int getRooms() { return rooms; }
        public void setRooms(int rooms) { this.rooms = rooms; }
        public String getBudgetTier() { return budgetTier; }
        public void setBudgetTier(String budgetTier) { this.budgetTier = budgetTier; }
        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }
    }
    
    /**
     * Hotel search result option.
     */
    class HotelOption {
        private String provider;
        private String name;
        private String address;
        private double lat;
        private double lng;
        private LocalDate checkIn;
        private LocalDate checkOut;
        private double price;
        private String currency;
        private String token; // Provider-specific booking token
        private double rating;
        private int reviewCount;
        private List<String> amenities;
        private String imageUrl;
        private String description;
        private Map<String, Object> metadata;
        
        // Constructors
        public HotelOption() {}
        
        // Getters and Setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLng() { return lng; }
        public void setLng(double lng) { this.lng = lng; }
        public LocalDate getCheckIn() { return checkIn; }
        public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
        public LocalDate getCheckOut() { return checkOut; }
        public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public double getRating() { return rating; }
        public void setRating(double rating) { this.rating = rating; }
        public int getReviewCount() { return reviewCount; }
        public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
        public List<String> getAmenities() { return amenities; }
        public void setAmenities(List<String> amenities) { this.amenities = amenities; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    /**
     * Booking request for hotel reservation.
     */
    class BookingRequest {
        private String token;
        private String itineraryId;
        private String guestName;
        private String guestEmail;
        private String guestPhone;
        private Map<String, Object> specialRequests;
        private Map<String, Object> paymentDetails;
        
        // Constructors
        public BookingRequest() {}
        
        public BookingRequest(String token, String itineraryId, String guestName, String guestEmail) {
            this.token = token;
            this.itineraryId = itineraryId;
            this.guestName = guestName;
            this.guestEmail = guestEmail;
        }
        
        // Getters and Setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getItineraryId() { return itineraryId; }
        public void setItineraryId(String itineraryId) { this.itineraryId = itineraryId; }
        public String getGuestName() { return guestName; }
        public void setGuestName(String guestName) { this.guestName = guestName; }
        public String getGuestEmail() { return guestEmail; }
        public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }
        public String getGuestPhone() { return guestPhone; }
        public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }
        public Map<String, Object> getSpecialRequests() { return specialRequests; }
        public void setSpecialRequests(Map<String, Object> specialRequests) { this.specialRequests = specialRequests; }
        public Map<String, Object> getPaymentDetails() { return paymentDetails; }
        public void setPaymentDetails(Map<String, Object> paymentDetails) { this.paymentDetails = paymentDetails; }
    }
    
    /**
     * Booking result from provider.
     */
    class BookingResult {
        private String bookingId;
        private String status;
        private String providerConfirmationId;
        private String cancellationPolicy;
        private Map<String, String> contactInfo;
        private String voucher;
        private Map<String, Object> additionalInfo;
        
        // Constructors
        public BookingResult() {}
        
        public BookingResult(String bookingId, String status, String providerConfirmationId) {
            this.bookingId = bookingId;
            this.status = status;
            this.providerConfirmationId = providerConfirmationId;
        }
        
        // Getters and Setters
        public String getBookingId() { return bookingId; }
        public void setBookingId(String bookingId) { this.bookingId = bookingId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getProviderConfirmationId() { return providerConfirmationId; }
        public void setProviderConfirmationId(String providerConfirmationId) { this.providerConfirmationId = providerConfirmationId; }
        public String getCancellationPolicy() { return cancellationPolicy; }
        public void setCancellationPolicy(String cancellationPolicy) { this.cancellationPolicy = cancellationPolicy; }
        public Map<String, String> getContactInfo() { return contactInfo; }
        public void setContactInfo(Map<String, String> contactInfo) { this.contactInfo = contactInfo; }
        public String getVoucher() { return voucher; }
        public void setVoucher(String voucher) { this.voucher = voucher; }
        public Map<String, Object> getAdditionalInfo() { return additionalInfo; }
        public void setAdditionalInfo(Map<String, Object> additionalInfo) { this.additionalInfo = additionalInfo; }
    }
}

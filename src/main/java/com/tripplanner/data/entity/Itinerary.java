package com.tripplanner.data.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Itinerary entity representing travel itineraries.
 * Legacy JPA entity - system now uses Firestore with NormalizedItinerary
 */
@Entity
@Table(name = "itineraries")
public class Itinerary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @NotBlank
    @Column(name = "destination", nullable = false)
    private String destination;
    
    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Valid
    @NotNull
    @Embedded
    private Party party;
    
    @NotBlank
    @Column(name = "budget_tier", nullable = false)
    private String budgetTier; // economy, mid-range, luxury
    
    @Size(max = 20)
    @ElementCollection
    @CollectionTable(name = "itinerary_interests", joinColumns = @JoinColumn(name = "itinerary_id"))
    @Column(name = "interest")
    private List<String> interests;
    
    @Size(max = 10)
    @ElementCollection
    @CollectionTable(name = "itinerary_constraints", joinColumns = @JoinColumn(name = "itinerary_id"))
    @Column(name = "constraint_text")
    private List<String> constraints;
    
    @Column(name = "language", length = 10)
    private String language = "en";
    
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;
    
    @Column(name = "map_data", columnDefinition = "TEXT")
    private String mapData; // Store as JSON string instead of Map
    
    @OneToMany(mappedBy = "itinerary", cascade = CascadeType.ALL)
    private List<ItineraryDay> days;
    
    @Column(name = "status", length = 50)
    private String status = "draft"; // draft, generating, completed, failed
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Column(name = "is_public")
    private boolean isPublic = false;
    
    @Column(name = "share_token", unique = true)
    private String shareToken;
    
    public Itinerary() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getDestination() {
        return destination;
    }
    
    public void setDestination(String destination) {
        this.destination = destination;
    }
    
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public Party getParty() {
        return party;
    }
    
    public void setParty(Party party) {
        this.party = party;
    }
    
    public String getBudgetTier() {
        return budgetTier;
    }
    
    public void setBudgetTier(String budgetTier) {
        this.budgetTier = budgetTier;
    }
    
    public List<String> getInterests() {
        return interests;
    }
    
    public void setInterests(List<String> interests) {
        this.interests = interests;
    }
    
    public List<String> getConstraints() {
        return constraints;
    }
    
    public void setConstraints(List<String> constraints) {
        this.constraints = constraints;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getMapData() {
        return mapData;
    }
    
    public void setMapData(String mapData) {
        this.mapData = mapData;
    }
    
    public List<ItineraryDay> getDays() {
        return days;
    }
    
    public void setDays(List<ItineraryDay> days) {
        this.days = days;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public boolean isPublic() {
        return isPublic;
    }
    
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
    
    public String getShareToken() {
        return shareToken;
    }
    
    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Itinerary itinerary = (Itinerary) o;
        return Objects.equals(id, itinerary.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Itinerary{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", destination='" + destination + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status='" + status + '\'' +
                '}';
    }
    
    // Nested classes
    @Embeddable
    public static class Party {
        @Column(name = "adults")
        private int adults = 1;
        
        @Column(name = "children")
        private int children = 0;
        
        @Column(name = "infants")
        private int infants = 0;
        
        @Column(name = "rooms")
        private int rooms = 1;
        
        public Party() {}
        
        public Party(int adults, int children, int infants, int rooms) {
            this.adults = adults;
            this.children = children;
            this.infants = infants;
            this.rooms = rooms;
        }
        
        // Getters and Setters
        public int getAdults() {
            return adults;
        }
        
        public void setAdults(int adults) {
            this.adults = adults;
        }
        
        public int getChildren() {
            return children;
        }
        
        public void setChildren(int children) {
            this.children = children;
        }
        
        public int getInfants() {
            return infants;
        }
        
        public void setInfants(int infants) {
            this.infants = infants;
        }
        
        public int getRooms() {
            return rooms;
        }
        
        public void setRooms(int rooms) {
            this.rooms = rooms;
        }
        
        public int getTotalGuests() {
            return adults + children + infants;
        }
    }
    
    @Entity
    @Table(name = "itinerary_days")
    public static class ItineraryDay {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @ManyToOne
        @JoinColumn(name = "itinerary_id")
        private Itinerary itinerary;
        
        @Column(name = "day_number")
        private int day;
        
        @Column(name = "date")
        private LocalDate date;
        
        @Column(name = "location")
        private String location;
        
        @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.ALL)
        private List<Activity> activities;
        
        @OneToOne(mappedBy = "itineraryDay", cascade = CascadeType.ALL)
        private Accommodation accommodation;
        
        @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.ALL)
        private List<Transportation> transportation;
        
        @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.ALL)
        private List<Meal> meals;
        
        @Column(name = "notes", columnDefinition = "TEXT")
        private String notes;
        
        public ItineraryDay() {}
        
        // Getters and Setters
        public int getDay() {
            return day;
        }
        
        public void setDay(int day) {
            this.day = day;
        }
        
        
        public LocalDate getDate() {
            return date;
        }
        
        public void setDate(LocalDate date) {
            this.date = date;
        }
        
        public String getLocation() {
            return location;
        }
        
        public void setLocation(String location) {
            this.location = location;
        }
        
        public List<Activity> getActivities() {
            return activities;
        }
        
        public void setActivities(List<Activity> activities) {
            this.activities = activities;
        }
        
        public Accommodation getAccommodation() {
            return accommodation;
        }
        
        public void setAccommodation(Accommodation accommodation) {
            this.accommodation = accommodation;
        }
        
        public List<Transportation> getTransportation() {
            return transportation;
        }
        
        public void setTransportation(List<Transportation> transportation) {
            this.transportation = transportation;
        }
        
        public List<Meal> getMeals() {
            return meals;
        }
        
        public void setMeals(List<Meal> meals) {
            this.meals = meals;
        }
        
        public String getNotes() {
            return notes;
        }
        
        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
    
    @Entity
    @Table(name = "activities")
    public static class Activity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @ManyToOne
        @JoinColumn(name = "itinerary_day_id")
        private ItineraryDay itineraryDay;
        
        @Column(name = "name")
        private String name;
        
        @Column(name = "description", columnDefinition = "TEXT")
        private String description;
        
        @OneToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "location_id")
        private Location location;
        
        @Column(name = "start_time")
        private String startTime;
        
        @Column(name = "end_time")
        private String endTime;
        
        @Column(name = "duration")
        private int duration; // in minutes
        
        @Column(name = "category")
        private String category;
        
        @OneToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "price_id")
        private Price price;
        
        @Column(name = "booking_required")
        private boolean bookingRequired = false;
        
        @Column(name = "booking_url")
        private String bookingUrl;
        
        @Column(name = "tips", columnDefinition = "TEXT")
        private String tips;
        
        public Activity() {}
        
        // Getters and Setters - abbreviated for brevity
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Location getLocation() { return location; }
        public void setLocation(Location location) { this.location = location; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Price getPrice() { return price; }
        public void setPrice(Price price) { this.price = price; }
        public boolean isBookingRequired() { return bookingRequired; }
        public void setBookingRequired(boolean bookingRequired) { this.bookingRequired = bookingRequired; }
        public String getBookingUrl() { return bookingUrl; }
        public void setBookingUrl(String bookingUrl) { this.bookingUrl = bookingUrl; }
        public String getTips() { return tips; }
        public void setTips(String tips) { this.tips = tips; }
    }
    
    @Entity
    @Table(name = "accommodations")
    public static class Accommodation {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @OneToOne
        @JoinColumn(name = "itinerary_day_id")
        private ItineraryDay itineraryDay;
        
        @Column(name = "name")
        private String name;
        
        @Column(name = "type")
        private String type; // hotel, hostel, apartment, etc.
        
        @OneToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "location_id")
        private Location location;
        
        @Column(name = "check_in")
        private LocalDate checkIn;
        
        @Column(name = "check_out")
        private LocalDate checkOut;
        
        @OneToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "price_id")
        private Price price;
        
        @Column(name = "rating")
        private double rating;
        
        @ElementCollection
        @CollectionTable(name = "accommodation_amenities", joinColumns = @JoinColumn(name = "accommodation_id"))
        @Column(name = "amenity")
        private List<String> amenities;
        
        @Column(name = "booking_url")
        private String bookingUrl;
        
        public Accommodation() {}
        
        // Getters and Setters - abbreviated for brevity
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Location getLocation() { return location; }
        public void setLocation(Location location) { this.location = location; }
        
        public LocalDate getCheckIn() { return checkIn; }
        public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
        public LocalDate getCheckOut() { return checkOut; }
        public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
        public Price getPrice() { return price; }
        public void setPrice(Price price) { this.price = price; }
        public double getRating() { return rating; }
        public void setRating(double rating) { this.rating = rating; }
        public List<String> getAmenities() { return amenities; }
        public void setAmenities(List<String> amenities) { this.amenities = amenities; }
        public String getBookingUrl() { return bookingUrl; }
        public void setBookingUrl(String bookingUrl) { this.bookingUrl = bookingUrl; }
    }
    
    @Entity
    @Table(name = "transportation")
    public static class Transportation {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @ManyToOne
        @JoinColumn(name = "itinerary_day_id")
        private ItineraryDay itineraryDay;
        
        @Column(name = "mode")
        private String mode; // flight, train, bus, car, taxi, walk
        
        @OneToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "from_location_id")
        private Location from;
        
        @OneToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "to_location_id")
        private Location to;
        
        @Column(name = "departure_time")
        private String departureTime;
        
        @Column(name = "arrival_time")
        private String arrivalTime;
        
        @Column(name = "duration")
        private int duration; // in minutes
        
        @OneToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "price_id")
        private Price price;
        
        @Column(name = "provider")
        private String provider;
        
        @Column(name = "booking_url")
        private String bookingUrl;
        
        @Column(name = "notes", columnDefinition = "TEXT")
        private String notes;
        
        public Transportation() {}
        
        // Getters and Setters - abbreviated for brevity
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public Location getFrom() { return from; }
        public void setFrom(Location from) { this.from = from; }
        public Location getTo() { return to; }
        public void setTo(Location to) { this.to = to; }
        public String getDepartureTime() { return departureTime; }
        public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }
        public String getArrivalTime() { return arrivalTime; }
        public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
        public Price getPrice() { return price; }
        public void setPrice(Price price) { this.price = price; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getBookingUrl() { return bookingUrl; }
        public void setBookingUrl(String bookingUrl) { this.bookingUrl = bookingUrl; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    @Entity
    @Table(name = "meals")
    public static class Meal {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @ManyToOne
        @JoinColumn(name = "itinerary_day_id")
        private ItineraryDay itineraryDay;
        
        @Column(name = "type")
        private String type; // breakfast, lunch, dinner, snack
        
        @Column(name = "name")
        private String name;
        
        @Column(name = "restaurant")
        private String restaurant;
        
        @OneToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "location_id")
        private Location location;
        
        @Column(name = "time")
        private String time;
        
        @OneToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "price_id")
        private Price price;
        
        @Column(name = "cuisine")
        private String cuisine;
        
        @Column(name = "notes", columnDefinition = "TEXT")
        private String notes;
        
        public Meal() {}
        
        // Getters and Setters - abbreviated for brevity
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRestaurant() { return restaurant; }
        public void setRestaurant(String restaurant) { this.restaurant = restaurant; }
        public Location getLocation() { return location; }
        public void setLocation(Location location) { this.location = location; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        public Price getPrice() { return price; }
        public void setPrice(Price price) { this.price = price; }
        public String getCuisine() { return cuisine; }
        public void setCuisine(String cuisine) { this.cuisine = cuisine; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    @Entity
    @Table(name = "locations")
    public static class Location {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @Column(name = "name")
        private String name;
        
        @Column(name = "address")
        private String address;
        
        @Column(name = "lat")
        private double lat;
        
        @Column(name = "lng")
        private double lng;
        
        @Column(name = "place_id")
        private String placeId;
        
        public Location() {}
        
        public Location(String name, double lat, double lng) {
            this.name = name;
            this.lat = lat;
            this.lng = lng;
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLng() { return lng; }
        public void setLng(double lng) { this.lng = lng; }
        public String getPlaceId() { return placeId; }
        public void setPlaceId(String placeId) { this.placeId = placeId; }
    }
    
    @Entity
    @Table(name = "prices")
    public static class Price {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @Column(name = "amountPerPerson")
        private double amountPerPerson;
        
        @Column(name = "currency", length = 3)
        private String currency;
        
        public Price() {}
        
        public Price(double amountPerPerson, String currency) {
            this.amountPerPerson = amountPerPerson;
            this.currency = currency;
        }
        
        // Getters and Setters
        public double getAmount() { return amountPerPerson; }
        public void setAmount(double amount) { this.amountPerPerson = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }
}


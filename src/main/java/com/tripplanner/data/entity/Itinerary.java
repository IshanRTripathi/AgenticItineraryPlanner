package com.tripplanner.data.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Itinerary entity representing travel itineraries.
 * Stored in Firestore collection: itineraries/{itineraryId}
 */
public class Itinerary {
    
    @DocumentId
    private String id;
    
    @NotBlank
    @PropertyName("userId")
    private String userId;
    
    @NotBlank
    @PropertyName("destination")
    private String destination;
    
    @NotNull
    @PropertyName("startDate")
    private LocalDate startDate;
    
    @NotNull
    @PropertyName("endDate")
    private LocalDate endDate;
    
    @Valid
    @NotNull
    @PropertyName("party")
    private Party party;
    
    @NotBlank
    @PropertyName("budgetTier")
    private String budgetTier; // economy, mid-range, luxury
    
    @Size(max = 20)
    @PropertyName("interests")
    private List<String> interests;
    
    @Size(max = 10)
    @PropertyName("constraints")
    private List<String> constraints;
    
    @PropertyName("language")
    private String language = "en";
    
    @PropertyName("summary")
    private String summary;
    
    @PropertyName("map")
    private Map<String, Object> map; // GeoJSON or similar map data
    
    @PropertyName("days")
    private List<ItineraryDay> days;
    
    @PropertyName("status")
    private String status = "draft"; // draft, generating, completed, failed
    
    @PropertyName("createdAt")
    private Instant createdAt;
    
    @PropertyName("updatedAt")
    private Instant updatedAt;
    
    @PropertyName("isPublic")
    private boolean isPublic = false;
    
    @PropertyName("shareToken")
    private String shareToken;
    
    public Itinerary() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
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
    
    public Map<String, Object> getMap() {
        return map;
    }
    
    public void setMap(Map<String, Object> map) {
        this.map = map;
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
    public static class Party {
        @PropertyName("adults")
        private int adults = 1;
        
        @PropertyName("children")
        private int children = 0;
        
        @PropertyName("infants")
        private int infants = 0;
        
        @PropertyName("rooms")
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
    
    public static class ItineraryDay {
        @PropertyName("day")
        private int day;
        
        @PropertyName("date")
        private LocalDate date;
        
        @PropertyName("location")
        private String location;
        
        @PropertyName("activities")
        private List<Activity> activities;
        
        @PropertyName("accommodation")
        private Accommodation accommodation;
        
        @PropertyName("transportation")
        private List<Transportation> transportation;
        
        @PropertyName("meals")
        private List<Meal> meals;
        
        @PropertyName("notes")
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
    
    public static class Activity {
        @PropertyName("name")
        private String name;
        
        @PropertyName("description")
        private String description;
        
        @PropertyName("location")
        private Location location;
        
        @PropertyName("startTime")
        private String startTime;
        
        @PropertyName("endTime")
        private String endTime;
        
        @PropertyName("duration")
        private String duration;
        
        @PropertyName("category")
        private String category;
        
        @PropertyName("price")
        private Price price;
        
        @PropertyName("bookingRequired")
        private boolean bookingRequired = false;
        
        @PropertyName("bookingUrl")
        private String bookingUrl;
        
        @PropertyName("tips")
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
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
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
    
    public static class Accommodation {
        @PropertyName("name")
        private String name;
        
        @PropertyName("type")
        private String type; // hotel, hostel, apartment, etc.
        
        @PropertyName("location")
        private Location location;
        
        @PropertyName("checkIn")
        private LocalDate checkIn;
        
        @PropertyName("checkOut")
        private LocalDate checkOut;
        
        @PropertyName("price")
        private Price price;
        
        @PropertyName("rating")
        private double rating;
        
        @PropertyName("amenities")
        private List<String> amenities;
        
        @PropertyName("bookingUrl")
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
    
    public static class Transportation {
        @PropertyName("mode")
        private String mode; // flight, train, bus, car, taxi, walk
        
        @PropertyName("from")
        private Location from;
        
        @PropertyName("to")
        private Location to;
        
        @PropertyName("departureTime")
        private String departureTime;
        
        @PropertyName("arrivalTime")
        private String arrivalTime;
        
        @PropertyName("duration")
        private String duration;
        
        @PropertyName("price")
        private Price price;
        
        @PropertyName("provider")
        private String provider;
        
        @PropertyName("bookingUrl")
        private String bookingUrl;
        
        @PropertyName("notes")
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
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
        public Price getPrice() { return price; }
        public void setPrice(Price price) { this.price = price; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getBookingUrl() { return bookingUrl; }
        public void setBookingUrl(String bookingUrl) { this.bookingUrl = bookingUrl; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    public static class Meal {
        @PropertyName("type")
        private String type; // breakfast, lunch, dinner, snack
        
        @PropertyName("name")
        private String name;
        
        @PropertyName("restaurant")
        private String restaurant;
        
        @PropertyName("location")
        private Location location;
        
        @PropertyName("time")
        private String time;
        
        @PropertyName("price")
        private Price price;
        
        @PropertyName("cuisine")
        private String cuisine;
        
        @PropertyName("notes")
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
    
    public static class Location {
        @PropertyName("name")
        private String name;
        
        @PropertyName("address")
        private String address;
        
        @PropertyName("lat")
        private double lat;
        
        @PropertyName("lng")
        private double lng;
        
        @PropertyName("placeId")
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
    
    public static class Price {
        @PropertyName("amount")
        private double amount;
        
        @PropertyName("currency")
        private String currency;
        
        @PropertyName("per")
        private String per; // person, group, night, etc.
        
        public Price() {}
        
        public Price(double amount, String currency) {
            this.amount = amount;
            this.currency = currency;
        }
        
        // Getters and Setters
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getPer() { return per; }
        public void setPer(String per) { this.per = per; }
    }
}


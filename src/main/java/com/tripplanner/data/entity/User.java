package com.tripplanner.data.entity;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.PropertyName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Objects;

/**
 * User entity representing authenticated users in the system.
 * Stored in Firestore collection: users/{userId}
 */
public class User {
    
    @DocumentId
    private String id;
    
    @NotBlank
    @Email
    @PropertyName("email")
    private String email;
    
    @NotBlank
    @PropertyName("name")
    private String name;
    
    @PropertyName("createdAt")
    private Instant createdAt;
    
    @PropertyName("updatedAt")
    private Instant updatedAt;
    
    @PropertyName("lastLoginAt")
    private Instant lastLoginAt;
    
    @PropertyName("profileImageUrl")
    private String profileImageUrl;
    
    @PropertyName("preferredLanguage")
    private String preferredLanguage = "en";
    
    @PropertyName("timezone")
    private String timezone = "UTC";
    
    public User() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public User(String email, String name) {
        this();
        this.email = email;
        this.name = name;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
    
    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    
    public String getPreferredLanguage() {
        return preferredLanguage;
    }
    
    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}


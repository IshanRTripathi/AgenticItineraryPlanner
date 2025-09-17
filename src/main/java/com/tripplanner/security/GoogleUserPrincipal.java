package com.tripplanner.security;

import java.security.Principal;
import java.util.Objects;

/**
 * Principal representing an authenticated Google user.
 */
public class GoogleUserPrincipal implements Principal {
    
    private final String userId;
    private final String email;
    private final String name;
    private final String pictureUrl;
    
    public GoogleUserPrincipal(String userId, String email, String name, String pictureUrl) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
    }
    
    @Override
    public String getName() {
        return email; // Use email as the principal name
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getDisplayName() {
        return name;
    }
    
    public String getPictureUrl() {
        return pictureUrl;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoogleUserPrincipal that = (GoogleUserPrincipal) o;
        return Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
    
    @Override
    public String toString() {
        return "GoogleUserPrincipal{" +
                "userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

package com.tripplanner.data.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirestoreItinerary {
    private String id;
    private Integer version;
    private String json;
    private Instant updatedAt;

    public FirestoreItinerary(String id, Integer version, String json) {
        this.id = id;
        this.version = version;
        this.json = json;
        this.updatedAt = Instant.now();
    }

    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
}



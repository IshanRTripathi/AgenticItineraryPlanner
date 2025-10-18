package com.tripplanner.dto.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.tripplanner.dto.NodeLinks.BookingInfo;

import java.io.IOException;

/**
 * Custom deserializer for BookingInfo that handles both string URLs and object formats.
 * 
 * Accepts:
 * - String: "https://example.com" -> converts to BookingInfo with details set to the URL
 * - Object: {"refNumber": "...", "status": "...", "details": "..."} -> standard deserialization
 */
public class BookingInfoDeserializer extends JsonDeserializer<BookingInfo> {
    
    @Override
    public BookingInfo deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        // If it's a string (URL), convert to BookingInfo object
        if (node.isTextual()) {
            String url = node.asText();
            BookingInfo bookingInfo = new BookingInfo();
            bookingInfo.setDetails(url);
            bookingInfo.setStatus("NOT_REQUIRED"); // Default status
            return bookingInfo;
        }
        
        // If it's an object, deserialize normally
        if (node.isObject()) {
            BookingInfo bookingInfo = new BookingInfo();
            
            if (node.has("refNumber")) {
                bookingInfo.setRefNumber(node.get("refNumber").asText());
            }
            
            if (node.has("status")) {
                bookingInfo.setStatus(node.get("status").asText());
            } else {
                bookingInfo.setStatus("NOT_REQUIRED"); // Default
            }
            
            if (node.has("details")) {
                bookingInfo.setDetails(node.get("details").asText());
            }
            
            return bookingInfo;
        }
        
        // If null or other type, return null
        return null;
    }
}

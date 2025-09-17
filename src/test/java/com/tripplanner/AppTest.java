package com.tripplanner;

import org.junit.jupiter.api.Test;

/**
 * Basic application test.
 */
class AppTest {
    
    @Test
    void applicationClassExists() {
        // Simple test to verify the application class exists and can be instantiated
        App app = new App();
        assert app != null;
    }
    
    @Test
    void mainMethodExists() {
        // Test that main method exists and can be called
        try {
            App.class.getDeclaredMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Main method not found", e);
        }
    }
}

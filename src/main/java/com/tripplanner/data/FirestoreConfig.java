package com.tripplanner.data;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Firestore configuration for Google Cloud Firestore client.
 */
@Configuration
public class FirestoreConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(FirestoreConfig.class);
    
    @Value("${firestore.project-id}")
    private String projectId;
    
    @Value("${firestore.credentials-path:}")
    private String credentialsPath;
    
    @Value("${firestore.database-id:(default)}")
    private String databaseId;
    
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        value = "firestore.project-id",
        matchIfMissing = false
    )
    public Firestore firestore() throws IOException {
        logger.info("Initializing Firestore with project: {}, database: {}", projectId, databaseId);
        
        FirestoreOptions.Builder optionsBuilder = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setDatabaseId(databaseId);
        
        // Use service account credentials if path is provided
        if (credentialsPath != null && !credentialsPath.isEmpty() && !credentialsPath.equals("")) {
            logger.info("Using service account credentials from: {}", credentialsPath);
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
            optionsBuilder.setCredentials(credentials);
        } else {
            logger.info("Using default credentials (Application Default Credentials)");
            // Will use Application Default Credentials (ADC)
            // This works in local development with gcloud auth application-default login
            // And in production with service account attached to the compute instance
        }
        
        Firestore firestore = optionsBuilder.build().getService();
        logger.info("Firestore initialized successfully");
        
        return firestore;
    }
}

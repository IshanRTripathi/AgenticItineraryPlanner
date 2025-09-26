package com.tripplanner.config;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirestoreConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirestoreConfig.class);

    @Value("${firestore.project-id:}")
    private String projectId;

    @Value("${firestore.use-emulator:false}")
    private boolean useEmulator;

    @Value("${firestore.emulator-host:localhost:8080}")
    private String emulatorHost;

    // Inline JSON credentials (optional)
    @Value("${firestore.credentials:}")
    private String credentialsJson;

    // Credentials file path (optional)
    @Value("${firestore.credentials-file:}")
    private String credentialsFilePath;

    @Bean
    public Firestore firestore() {
        try {
            FirestoreOptions.Builder builder = FirestoreOptions.getDefaultInstance().toBuilder();

            if (projectId != null && !projectId.isEmpty()) {
                builder.setProjectId(projectId);
            }

            if (useEmulator) {
                // Emulator host in format host:port
                String host = emulatorHost != null && !emulatorHost.isEmpty() ? emulatorHost : "localhost:8080";
                logger.info("Configuring Firestore emulator at {}", host);
                builder.setEmulatorHost(host);
            } else {
                Credentials creds = resolveCredentials();
                if (creds != null) {
                    builder.setCredentials(creds);
                    builder.setCredentialsProvider(FixedCredentialsProvider.create(creds));
                    logger.info("Configured Firestore credentials from {}",
                            credentialsJson != null && !credentialsJson.isEmpty() ? "inline JSON" :
                                    (credentialsFilePath != null && !credentialsFilePath.isEmpty() ? credentialsFilePath : "default provider"));
                } else {
                    logger.info("Using default application credentials for Firestore");
                }
            }

            FirestoreOptions options = builder.build();
            Firestore firestore = options.getService();
            logger.info("Firestore initialized for project: {} (emulator: {})",
                    options.getProjectId(), useEmulator);
            return firestore;
        } catch (Exception e) {
            logger.error("Failed to initialize Firestore", e);
            throw new RuntimeException("Failed to initialize Firestore", e);
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        try {
            // Initialize Firebase if not already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions.Builder builder = FirebaseOptions.builder();
                
                if (projectId != null && !projectId.isEmpty()) {
                    builder.setProjectId(projectId);
                }
                
                // Use the same credentials as Firestore
                GoogleCredentials credentials = resolveCredentials();
                if (credentials != null) {
                    builder.setCredentials(credentials);
                }
                
                FirebaseApp.initializeApp(builder.build());
                logger.info("Firebase initialized for project: {}", projectId);
            }
            
            return FirebaseAuth.getInstance();
        } catch (Exception e) {
            logger.error("Failed to initialize Firebase Auth", e);
            throw new RuntimeException("Failed to initialize Firebase Auth", e);
        }
    }

    private GoogleCredentials resolveCredentials() {
        try {
            // 1) Inline JSON takes precedence
            if (credentialsJson != null && !credentialsJson.isEmpty()) {
                return GoogleCredentials.fromStream(new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)));
            }

            // 2) Credentials file path property
            if (credentialsFilePath != null && !credentialsFilePath.isEmpty()) {
                File file = new File(credentialsFilePath);
                if (file.exists()) {
                    return GoogleCredentials.fromStream(new FileInputStream(file));
                }
            }

            // 3) GOOGLE_APPLICATION_CREDENTIALS env or default provider
            return GoogleCredentials.getApplicationDefault();
        } catch (Exception e) {
            logger.error("Failed to resolve Firestore credentials", e);
            return null;
        }
    }
}



package com.voltconnect.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Initialises the Firebase Admin SDK.
 *
 * Supports two modes:
 * 1. FIREBASE_CREDENTIALS_JSON env var — JSON content directly (for Railway/cloud)
 * 2. FIREBASE_CREDENTIALS_PATH env var — path to a local JSON file (for local dev)
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.project-id}")
    private String projectId;

    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    @Value("${firebase.credentials-json:}")
    private String credentialsJson;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase app already initialised — reusing existing instance");
            return FirebaseApp.getInstance();
        }

        log.info("Initialising Firebase Admin SDK for project: {}", projectId);

        InputStream serviceAccount;

        if (credentialsJson != null && !credentialsJson.isBlank()) {
            // Cloud mode: JSON content from env var
            log.info("Loading Firebase credentials from FIREBASE_CREDENTIALS_JSON env var");
            serviceAccount = new ByteArrayInputStream(
                    credentialsJson.getBytes(StandardCharsets.UTF_8));
        } else if (credentialsPath != null && !credentialsPath.isBlank()) {
            // Local mode: file path
            log.info("Loading Firebase credentials from file: {}", credentialsPath);
            serviceAccount = new FileInputStream(credentialsPath);
        } else {
            throw new IllegalStateException(
                    "Firebase credentials not configured. Set either " +
                    "FIREBASE_CREDENTIALS_JSON or FIREBASE_CREDENTIALS_PATH");
        }

        try (serviceAccount) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(projectId)
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialised successfully");
            return app;
        }
    }
}

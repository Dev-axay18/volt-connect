package com.voltconnect.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Initialises the Firebase Admin SDK using the service-account credentials file
 * whose path is supplied via the {@code FIREBASE_CREDENTIALS_PATH} environment variable.
 *
 * <p>The credentials file must be a valid Firebase service-account JSON downloaded from
 * the Firebase console. It must never be committed to source control.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.project-id}")
    private String projectId;

    @Value("${firebase.credentials-path}")
    private String credentialsPath;

    /**
     * Initialises {@link FirebaseApp} once at application startup.
     * If a default app is already initialised (e.g., in tests), this is a no-op.
     *
     * @return the initialised {@link FirebaseApp} instance
     * @throws IOException if the credentials file cannot be read
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase app already initialised — reusing existing instance");
            return FirebaseApp.getInstance();
        }

        log.info("Initialising Firebase Admin SDK for project: {}", projectId);

        try (InputStream serviceAccount = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(projectId)
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialised successfully");
            return app;
        } catch (IOException e) {
            log.error("Failed to initialise Firebase Admin SDK — credentials file not found at: {}",
                    credentialsPath);
            throw e;
        }
    }
}

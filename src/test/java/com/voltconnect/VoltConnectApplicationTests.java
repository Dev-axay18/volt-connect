package com.voltconnect;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test that verifies the Spring application context loads without errors.
 *
 * <p>Uses {@code @TestPropertySource} to supply the minimum required environment
 * variables so the context can start without real external services.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "SUPABASE_DB_URL=jdbc:postgresql://localhost:5432/voltconnect_test",
        "SUPABASE_DB_USERNAME=postgres",
        "SUPABASE_DB_PASSWORD=test",
        "FIREBASE_PROJECT_ID=test-project",
        "FIREBASE_CREDENTIALS_PATH=src/test/resources/firebase-test-credentials.json",
        "RAZORPAY_KEY_ID=rzp_test_key",
        "RAZORPAY_SECRET=test_secret",
        "RAZORPAY_WEBHOOK_SECRET=test_webhook_secret",
        "JWT_SECRET=test-jwt-secret-that-is-at-least-32-characters-long",
        "JWT_EXPIRY=3600000",
        "JWT_REFRESH_EXPIRY=2592000000",
        "REDIS_HOST=localhost",
        "REDIS_PORT=6379"
})
class VoltConnectApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts without errors.
        // Full integration tests are in the Week 6 test suite (Task 45).
    }
}

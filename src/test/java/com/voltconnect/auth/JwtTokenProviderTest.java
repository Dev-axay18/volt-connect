package com.voltconnect.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtTokenProvider}.
 *
 * Uses a fixed 32-byte (256-bit) base64-encoded secret so tests are
 * self-contained and do not require a running Spring context.
 *
 * Satisfies Requirements 1.2, 1.5, 1.6, 20.1, 20.2.
 */
class JwtTokenProviderTest {

    /** 32 ASCII chars → 256-bit key, base64-encoded for the provider constructor. */
    private static final String TEST_SECRET =
            Base64.getEncoder().encodeToString("volt-connect-test-secret-32chars!".getBytes());

    private static final long EXPIRY_MS         = 3_600_000L;  // 1 hour
    private static final long REFRESH_EXPIRY_MS = 86_400_000L; // 24 hours

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(TEST_SECRET, EXPIRY_MS, REFRESH_EXPIRY_MS);
    }

    // ── generateAccessToken ──────────────────────────────────────────────────

    @Test
    @DisplayName("generateAccessToken returns a token that is valid, has correct userId and roles")
    void generateAccessToken_returnsValidToken() {
        String userId = "user-123";
        List<String> roles = List.of("driver", "host");

        String token = provider.generateAccessToken(userId, roles);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.extractUserId(token)).isEqualTo(userId);
        assertThat(provider.extractRoles(token)).containsExactlyInAnyOrderElementsOf(roles);
    }

    // ── generateRefreshToken ─────────────────────────────────────────────────

    @Test
    @DisplayName("generateRefreshToken returns a valid token with type=refresh")
    void generateRefreshToken_returnsValidRefreshToken() {
        String userId = "user-456";

        String token = provider.generateRefreshToken(userId);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.extractUserId(token)).isEqualTo(userId);
        assertThat(provider.isRefreshToken(token)).isTrue();
        assertThat(provider.isAccessToken(token)).isFalse();
    }

    // ── validateToken ────────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken returns false for an expired token")
    void validateToken_withExpiredToken_returnsFalse() throws InterruptedException {
        // Create a provider with 1 ms expiry
        JwtTokenProvider shortLivedProvider =
                new JwtTokenProvider(TEST_SECRET, 1L, REFRESH_EXPIRY_MS);

        String token = shortLivedProvider.generateAccessToken("user-789", List.of("driver"));

        // Wait for the token to expire
        Thread.sleep(10);

        assertThat(shortLivedProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for a tampered token")
    void validateToken_withTamperedToken_returnsFalse() {
        String token = provider.generateAccessToken("user-abc", List.of("driver"));

        // Flip the last character to tamper with the signature
        char lastChar = token.charAt(token.length() - 1);
        char replacement = (lastChar == 'A') ? 'B' : 'A';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for null token")
    void validateToken_withNullToken_returnsFalse() {
        assertThat(provider.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for empty token")
    void validateToken_withEmptyToken_returnsFalse() {
        assertThat(provider.validateToken("")).isFalse();
        assertThat(provider.validateToken("   ")).isFalse();
    }

    // ── extractUserId ────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractUserId returns the correct subject from an access token")
    void extractUserId_returnsCorrectSubject() {
        String userId = "firebase-uid-xyz";
        String token = provider.generateAccessToken(userId, List.of("driver"));

        assertThat(provider.extractUserId(token)).isEqualTo(userId);
    }

    // ── extractRoles ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("extractRoles returns the correct roles list")
    void extractRoles_returnsCorrectRoles() {
        List<String> roles = List.of("driver", "host");
        String token = provider.generateAccessToken("user-roles-test", roles);

        assertThat(provider.extractRoles(token))
                .containsExactlyInAnyOrderElementsOf(roles);
    }

    // ── isAccessToken / isRefreshToken ───────────────────────────────────────

    @Test
    @DisplayName("isAccessToken returns true for an access token")
    void isAccessToken_returnsTrueForAccessToken() {
        String token = provider.generateAccessToken("user-type-test", List.of("driver"));

        assertThat(provider.isAccessToken(token)).isTrue();
        assertThat(provider.isRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("isRefreshToken returns true for a refresh token")
    void isRefreshToken_returnsTrueForRefreshToken() {
        String token = provider.generateRefreshToken("user-refresh-test");

        assertThat(provider.isRefreshToken(token)).isTrue();
        assertThat(provider.isAccessToken(token)).isFalse();
    }
}

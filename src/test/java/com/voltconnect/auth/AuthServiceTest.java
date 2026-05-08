package com.voltconnect.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.voltconnect.auth.dto.*;
import com.voltconnect.shared.exceptions.BadRequestException;
import com.voltconnect.shared.exceptions.ResourceNotFoundException;
import com.voltconnect.shared.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}.
 *
 * <p>Firebase Admin SDK, {@link UserRepository}, and {@link JwtTokenProvider}
 * are all mocked so these tests run without any external dependencies.
 *
 * <p>Satisfies Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.2, 2.3, 2.6.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static final String UID          = "firebase-uid-abc123";
    private static final String PHONE        = "+919876543210";
    private static final String ACCESS_TOKEN = "mock.access.token";
    private static final String REFRESH_TOKEN = "mock.refresh.token";

    private UserEntity existingUser;

    @BeforeEach
    void setUp() {
        existingUser = UserEntity.builder()
                .id(UID)
                .phone(PHONE)
                .name("Test User")
                .role(List.of("driver"))
                .isHostVerified(false)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build();
    }

    // ── verifyOtp — happy path ────────────────────────────────────────────────

    @Test
    @DisplayName("verifyOtp with valid Firebase token returns tokens and user for existing user")
    void verifyOtp_withValidToken_returnsTokensAndUser() throws FirebaseAuthException {
        // Arrange
        FirebaseToken mockToken = mockFirebaseToken(UID, PHONE);
        FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);
        when(mockFirebaseAuth.verifyIdToken(anyString())).thenReturn(mockToken);

        when(userRepository.findById(UID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(existingUser);
        when(jwtTokenProvider.generateAccessToken(UID, existingUser.getRole()))
                .thenReturn(ACCESS_TOKEN);
        when(jwtTokenProvider.generateRefreshToken(UID)).thenReturn(REFRESH_TOKEN);

        VerifyOtpRequest request = new VerifyOtpRequest("valid-firebase-token");

        try (MockedStatic<FirebaseAuth> firebaseAuthStatic = mockStatic(FirebaseAuth.class)) {
            firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);

            // Act
            VerifyOtpResponse response = authService.verifyOtp(request);

            // Assert
            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.user()).isNotNull();
            assertThat(response.user().id()).isEqualTo(UID);
            assertThat(response.user().phone()).isEqualTo(PHONE);
        }
    }

    @Test
    @DisplayName("verifyOtp creates new user with default driver role when user does not exist")
    void verifyOtp_withNewUser_createsUserWithDriverRole() throws FirebaseAuthException {
        // Arrange
        FirebaseToken mockToken = mockFirebaseToken(UID, PHONE);
        FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);
        when(mockFirebaseAuth.verifyIdToken(anyString())).thenReturn(mockToken);

        when(userRepository.findById(UID)).thenReturn(Optional.empty());

        UserEntity savedNewUser = UserEntity.builder()
                .id(UID)
                .phone(PHONE)
                .role(List.of("driver"))
                .isHostVerified(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedNewUser);
        when(jwtTokenProvider.generateAccessToken(eq(UID), eq(List.of("driver"))))
                .thenReturn(ACCESS_TOKEN);
        when(jwtTokenProvider.generateRefreshToken(UID)).thenReturn(REFRESH_TOKEN);

        VerifyOtpRequest request = new VerifyOtpRequest("valid-firebase-token");

        try (MockedStatic<FirebaseAuth> firebaseAuthStatic = mockStatic(FirebaseAuth.class)) {
            firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);

            // Act
            VerifyOtpResponse response = authService.verifyOtp(request);

            // Assert
            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.user().role()).containsExactly("driver");

            // Verify a new user was saved
            verify(userRepository).save(argThat(u ->
                    u.getId().equals(UID) &&
                    u.getPhone().equals(PHONE) &&
                    u.getRole().equals(List.of("driver"))
            ));
        }
    }

    // ── verifyOtp — invalid Firebase token ───────────────────────────────────

    @Test
    @DisplayName("verifyOtp with invalid Firebase token throws UnauthorizedException")
    void verifyOtp_withInvalidFirebaseToken_throwsUnauthorizedException() throws FirebaseAuthException {
        // Arrange
        FirebaseAuth mockFirebaseAuth = mock(FirebaseAuth.class);
        when(mockFirebaseAuth.verifyIdToken(anyString()))
                .thenThrow(new FirebaseAuthException(
                        com.google.firebase.ErrorCode.INVALID_ARGUMENT,
                        "Invalid token",
                        null, null, null));

        VerifyOtpRequest request = new VerifyOtpRequest("invalid-firebase-token");

        try (MockedStatic<FirebaseAuth> firebaseAuthStatic = mockStatic(FirebaseAuth.class)) {
            firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(mockFirebaseAuth);

            // Act & Assert
            assertThatThrownBy(() -> authService.verifyOtp(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid or expired Firebase token");
        }
    }

    // ── refreshToken — happy path ─────────────────────────────────────────────

    @Test
    @DisplayName("refreshToken with valid refresh token returns new access token")
    void refreshToken_withValidRefreshToken_returnsNewAccessToken() {
        // Arrange
        when(jwtTokenProvider.validateToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.extractUserId(REFRESH_TOKEN)).thenReturn(UID);
        when(userRepository.findById(UID)).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.generateAccessToken(UID, existingUser.getRole()))
                .thenReturn(ACCESS_TOKEN);

        RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);

        // Act
        RefreshTokenResponse response = authService.refreshToken(request);

        // Assert
        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
    }

    // ── refreshToken — expired / invalid token ────────────────────────────────

    @Test
    @DisplayName("refreshToken with expired token throws UnauthorizedException")
    void refreshToken_withExpiredToken_throwsUnauthorizedException() {
        // Arrange — validateToken returns false (expired)
        when(jwtTokenProvider.validateToken(REFRESH_TOKEN)).thenReturn(false);

        RefreshTokenRequest request = new RefreshTokenRequest(REFRESH_TOKEN);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("invalid or expired");
    }

    @Test
    @DisplayName("refreshToken with access token (wrong type) throws UnauthorizedException")
    void refreshToken_withAccessToken_throwsUnauthorizedException() {
        // Arrange — token is valid but is an access token, not a refresh token
        when(jwtTokenProvider.validateToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(ACCESS_TOKEN)).thenReturn(false);

        RefreshTokenRequest request = new RefreshTokenRequest(ACCESS_TOKEN);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not a refresh token");
    }

    // ── updateProfile — user not found ────────────────────────────────────────

    @Test
    @DisplayName("updateProfile with missing user throws ResourceNotFoundException")
    void updateProfile_withMissingUser_throwsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById("nonexistent-id")).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest(
                "New Name", null, null, null, null, null, null, null, null);

        // Act & Assert
        assertThatThrownBy(() -> authService.updateProfile("nonexistent-id", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateProfile — partial update ───────────────────────────────────────

    @Test
    @DisplayName("updateProfile with valid data updates only provided fields")
    void updateProfile_withValidData_updatesOnlyProvidedFields() {
        // Arrange — only name and email are provided; other fields should remain unchanged
        String originalPhone = existingUser.getPhone();
        List<String> originalRole = existingUser.getRole();

        when(userRepository.findById(UID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Updated Name",   // name — should be updated
                "new@email.com",  // email — should be updated
                null,             // avatarUrl — null, should NOT be changed
                null,             // vehicleType — null, should NOT be changed
                null,             // connectorType — null, should NOT be changed
                null,             // role — null, should NOT be changed
                null,             // bankAccountNumber — null, should NOT be changed
                null,             // bankIfsc — null, should NOT be changed
                null              // fcmToken — null, should NOT be changed
        );

        // Act
        UserDto result = authService.updateProfile(UID, request);

        // Assert — updated fields
        assertThat(result.name()).isEqualTo("Updated Name");
        assertThat(result.email()).isEqualTo("new@email.com");

        // Assert — unchanged fields (null fields in request must not overwrite)
        assertThat(result.phone()).isEqualTo(originalPhone);
        assertThat(result.role()).isEqualTo(originalRole);
    }

    @Test
    @DisplayName("updateProfile with blank name throws IllegalArgumentException")
    void updateProfile_withBlankName_throwsIllegalArgumentException() {
        // Arrange
        when(userRepository.findById(UID)).thenReturn(Optional.of(existingUser));

        UpdateProfileRequest request = new UpdateProfileRequest(
                "   ", null, null, null, null, null, null, null, null);

        // Act & Assert
        assertThatThrownBy(() -> authService.updateProfile(UID, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Name must not be blank");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Creates a mock {@link FirebaseToken} with the given UID and phone number.
     */
    private FirebaseToken mockFirebaseToken(String uid, String phone) {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn(uid);
        when(token.getClaims()).thenReturn(Map.of("phone_number", phone));
        return token;
    }
}

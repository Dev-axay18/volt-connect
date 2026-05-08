package com.voltconnect.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.voltconnect.auth.dto.*;
import com.voltconnect.shared.exceptions.BadRequestException;
import com.voltconnect.shared.exceptions.ResourceNotFoundException;
import com.voltconnect.shared.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Core authentication service for Volt-Connect.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Verify Firebase ID tokens and issue application JWTs ({@link #verifyOtp}).</li>
 *   <li>Issue new access tokens from a valid refresh token ({@link #refreshToken}).</li>
 *   <li>Partially update a user's profile ({@link #updateProfile}).</li>
 * </ul>
 *
 * <p>Satisfies Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.2, 2.3, 2.6.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "driver";

    private final UserRepository    userRepository;
    private final JwtTokenProvider  jwtTokenProvider;

    // ── verifyOtp ─────────────────────────────────────────────────────────────

    /**
     * Verifies a Firebase ID token, upserts the user in the database, and
     * returns a JWT access token + refresh token pair.
     *
     * <p>New users are created with {@code role = ['driver']} by default.
     * Existing users have their {@code updated_at} and {@code fcm_token}
     * refreshed on every login.
     *
     * @param request contains the Firebase ID token from the mobile client
     * @return access token, refresh token, and the user's profile
     * @throws UnauthorizedException if the Firebase token is invalid or expired
     */
    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        FirebaseToken decodedToken = verifyFirebaseToken(request.firebaseToken());

        String uid   = decodedToken.getUid();
        String phone = (String) decodedToken.getClaims().get("phone_number");

        UserEntity user = userRepository.findById(uid)
                .map(existing -> updateExistingUser(existing))
                .orElseGet(() -> createNewUser(uid, phone));

        String accessToken  = jwtTokenProvider.generateAccessToken(uid, user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(uid);

        log.info("User '{}' authenticated successfully", uid);
        return new VerifyOtpResponse(accessToken, refreshToken, UserDto.from(user));
    }

    // ── refreshToken ──────────────────────────────────────────────────────────

    /**
     * Validates a refresh token and issues a new access token.
     *
     * @param request contains the refresh token
     * @return a new access token
     * @throws UnauthorizedException if the refresh token is invalid, expired, or
     *                               is not a refresh-type token
     */
    @Transactional(readOnly = true)
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String token = request.refreshToken();

        if (!jwtTokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }
        if (!jwtTokenProvider.isRefreshToken(token)) {
            throw new UnauthorizedException("Provided token is not a refresh token");
        }

        String userId = jwtTokenProvider.extractUserId(token);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException(
                        "User associated with refresh token no longer exists"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, user.getRole());

        log.debug("Issued new access token for user '{}'", userId);
        return new RefreshTokenResponse(newAccessToken);
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    /**
     * Partially updates a user's profile. Only non-null fields in the request
     * are applied; null fields are left unchanged.
     *
     * @param userId  the authenticated user's ID
     * @param request the fields to update
     * @return the updated user profile as a {@link UserDto}
     * @throws ResourceNotFoundException if no user with {@code userId} exists
     * @throws IllegalArgumentException  if {@code name} is provided but blank
     */
    @Transactional
    public UserDto updateProfile(String userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Validate: name must not be blank if provided
        if (request.name() != null && request.name().isBlank()) {
            throw new BadRequestException("Name must not be blank");
        }

        // Apply only non-null fields (partial update)
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        if (request.vehicleType() != null) {
            user.setVehicleType(request.vehicleType());
        }
        if (request.connectorType() != null) {
            user.setConnectorType(request.connectorType());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.bankAccountNumber() != null) {
            user.setBankAccountNumber(request.bankAccountNumber());
        }
        if (request.bankIfsc() != null) {
            user.setBankIfsc(request.bankIfsc());
        }
        if (request.fcmToken() != null) {
            user.setFcmToken(request.fcmToken());
        }

        user.setUpdatedAt(Instant.now());
        UserEntity saved = userRepository.save(user);

        log.debug("Profile updated for user '{}'", userId);
        return UserDto.from(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Calls the Firebase Admin SDK to verify the ID token.
     *
     * @throws UnauthorizedException wrapping the {@link FirebaseAuthException} on failure
     */
    private FirebaseToken verifyFirebaseToken(String idToken) {
        try {
            return FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            log.warn("Firebase token verification failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired Firebase token", e);
        }
    }

    /** Refreshes {@code updated_at} on an existing user and saves. */
    private UserEntity updateExistingUser(UserEntity existing) {
        existing.setUpdatedAt(Instant.now());
        return userRepository.save(existing);
    }

    /** Creates a brand-new user with default {@code role = ['driver']}. */
    private UserEntity createNewUser(String uid, String phone) {
        Instant now = Instant.now();
        UserEntity newUser = UserEntity.builder()
                .id(uid)
                .phone(phone)
                .role(List.of(DEFAULT_ROLE))
                .isHostVerified(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return userRepository.save(newUser);
    }
}

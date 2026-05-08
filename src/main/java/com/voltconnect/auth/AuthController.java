package com.voltconnect.auth;

import com.voltconnect.auth.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication and profile management endpoints.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/v1/auth/verify-otp} — verify Firebase OTP token, issue JWT pair</li>
 *   <li>{@code POST /api/v1/auth/refresh}     — exchange refresh token for new access token</li>
 *   <li>{@code PUT  /api/v1/auth/profile}     — update authenticated user's profile (JWT required)</li>
 * </ul>
 *
 * <p>The {@code /verify-otp} and {@code /refresh} endpoints are public (no JWT required).
 * The {@code /profile} endpoint requires a valid JWT — the user ID is extracted from the
 * {@link org.springframework.security.core.Authentication} principal set by
 * {@link JwtAuthenticationFilter}.
 *
 * <p>Satisfies Requirements 1.2, 1.5, 2.2, 2.3, 2.6.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Verifies a Firebase ID token and returns a JWT access + refresh token pair.
     *
     * @param request the Firebase ID token from the mobile client
     * @return 200 OK with {@link VerifyOtpResponse}
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        VerifyOtpResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Exchanges a valid refresh token for a new access token.
     *
     * @param request the refresh token
     * @return 200 OK with {@link RefreshTokenResponse}
     */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially updates the authenticated user's profile.
     *
     * <p>The {@code userId} is extracted from the JWT principal set by
     * {@link JwtAuthenticationFilter} — the client does not supply it in the request.
     *
     * @param userId  the authenticated user's ID (from JWT)
     * @param request the profile fields to update (all optional)
     * @return 200 OK with the updated {@link UserDto}
     */
    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserDto updated = authService.updateProfile(userId, request);
        return ResponseEntity.ok(updated);
    }
}

package com.voltconnect.auth.dto;

/**
 * Response body for {@code POST /api/v1/auth/verify-otp}.
 *
 * <p>Returns the JWT access token, refresh token, and the user's profile
 * so the client can initialise its local state in a single round-trip.
 *
 * <p>Satisfies Requirements 1.2, 2.1.
 */
public record VerifyOtpResponse(
        String accessToken,
        String refreshToken,
        UserDto user
) {}

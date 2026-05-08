package com.voltconnect.auth.dto;

/**
 * Response body for {@code POST /api/v1/auth/refresh}.
 *
 * <p>Returns only the new access token; the refresh token is unchanged.
 *
 * <p>Satisfies Requirements 1.5, 1.6.
 */
public record RefreshTokenResponse(
        String accessToken
) {}

package com.voltconnect.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/auth/refresh}.
 *
 * <p>Satisfies Requirements 1.5, 1.6.
 */
public record RefreshTokenRequest(

        @NotBlank(message = "Refresh token must not be blank")
        String refreshToken
) {}

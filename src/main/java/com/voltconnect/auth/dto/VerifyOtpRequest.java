package com.voltconnect.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/auth/verify-otp}.
 *
 * <p>The client sends the Firebase ID token obtained after successful OTP
 * verification on the device. The backend verifies this token with the
 * Firebase Admin SDK and issues its own JWT pair.
 *
 * <p>Satisfies Requirements 1.2, 1.3.
 */
public record VerifyOtpRequest(

        @NotBlank(message = "Firebase token must not be blank")
        String firebaseToken
) {}

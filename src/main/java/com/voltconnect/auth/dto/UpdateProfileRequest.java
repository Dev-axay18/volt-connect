package com.voltconnect.auth.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for {@code PUT /api/v1/auth/profile}.
 *
 * <p>All fields are optional — only non-null values are applied (partial update).
 * If {@code name} is provided it must not be blank (enforced in the service layer
 * because {@code @NotBlank} would reject {@code null}, which is valid here).
 *
 * <p>Satisfies Requirements 2.2, 2.3, 2.4, 2.5, 2.6.
 */
public record UpdateProfileRequest(

        @Size(min = 1, message = "Name must not be blank when provided")
        String name,

        String email,

        String avatarUrl,

        List<String> vehicleType,

        List<String> connectorType,

        List<String> role,

        String bankAccountNumber,

        String bankIfsc,

        String fcmToken
) {}

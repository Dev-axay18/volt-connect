package com.voltconnect.auth.dto;

import com.voltconnect.auth.UserEntity;

import java.time.Instant;
import java.util.List;

/**
 * Public-facing representation of a {@link UserEntity}.
 *
 * <p>Sensitive fields (bank account number, bank IFSC) are intentionally
 * excluded from this DTO to avoid leaking financial data in API responses.
 *
 * <p>Satisfies Requirements 2.2, 2.6.
 */
public record UserDto(
        String id,
        String phone,
        String name,
        String email,
        String avatarUrl,
        List<String> role,
        List<String> vehicleType,
        List<String> connectorType,
        boolean isHostVerified,
        String fcmToken,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Maps a {@link UserEntity} to a {@link UserDto}, omitting sensitive fields.
     *
     * @param entity the entity to map
     * @return the corresponding DTO
     */
    public static UserDto from(UserEntity entity) {
        return new UserDto(
                entity.getId(),
                entity.getPhone(),
                entity.getName(),
                entity.getEmail(),
                entity.getAvatarUrl(),
                entity.getRole(),
                entity.getVehicleType(),
                entity.getConnectorType(),
                entity.isHostVerified(),
                entity.getFcmToken(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

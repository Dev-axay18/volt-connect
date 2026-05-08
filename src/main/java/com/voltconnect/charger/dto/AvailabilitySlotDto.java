package com.voltconnect.charger.dto;

import com.voltconnect.charger.AvailabilitySlotEntity;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Response DTO for an availability slot.
 *
 * <p>Satisfies Requirements 11.1, 11.2, 11.3.
 */
public record AvailabilitySlotDto(

        UUID id,
        UUID chargerId,
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean isActive
) {

    /**
     * Creates an {@link AvailabilitySlotDto} from an {@link AvailabilitySlotEntity}.
     */
    public static AvailabilitySlotDto from(AvailabilitySlotEntity entity) {
        return new AvailabilitySlotDto(
                entity.getId(),
                entity.getChargerId(),
                entity.getDayOfWeek(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.isActive()
        );
    }
}

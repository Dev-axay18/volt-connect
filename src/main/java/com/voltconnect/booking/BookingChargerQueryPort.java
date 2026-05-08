package com.voltconnect.booking;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface used by {@link BookingService} to query charger data without
 * creating a direct dependency on the charger package.
 *
 * <p>The charger package provides the implementation of this interface via
 * {@link DefaultBookingChargerQueryPort}.
 *
 * <p>Satisfies Requirements 6.1, 6.2.
 */
public interface BookingChargerQueryPort {

    /**
     * Returns charger information needed for booking validation and pricing.
     *
     * @param chargerId the charger UUID
     * @return an Optional containing charger info, or empty if not found
     */
    Optional<ChargerInfo> findChargerById(UUID chargerId);

    /**
     * Immutable value object carrying the charger fields needed by the booking service.
     */
    record ChargerInfo(
        UUID id,
        UUID hostId,
        BigDecimal pricePerHour,
        String status,
        boolean isAvailable
    ) {}
}

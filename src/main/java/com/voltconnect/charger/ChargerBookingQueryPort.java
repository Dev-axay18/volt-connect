package com.voltconnect.charger;

import java.util.List;
import java.util.UUID;

/**
 * Port interface used by {@link ChargerService} to query booking data without
 * creating a direct dependency on the booking package.
 *
 * <p>The booking package provides the implementation of this interface.
 * A default no-op implementation is provided here so the charger module
 * compiles and works independently until the booking module is implemented.
 *
 * <p>Satisfies Requirements 13.5.
 */
public interface ChargerBookingQueryPort {

    /**
     * Returns the IDs of all active or upcoming confirmed bookings for the
     * given charger. Used to prevent deletion of chargers with live bookings.
     *
     * @param chargerId the charger UUID
     * @return list of conflicting booking IDs (empty if none)
     */
    List<UUID> findActiveBookingIdsForCharger(UUID chargerId);
}

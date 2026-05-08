package com.voltconnect.charger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AvailabilitySlotEntity}.
 *
 * <p>Satisfies Requirements 11.1, 11.2, 11.3.
 */
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlotEntity, UUID> {

    /**
     * Returns all active slots for the given charger.
     */
    List<AvailabilitySlotEntity> findByChargerIdAndIsActiveTrue(UUID chargerId);

    /**
     * Returns all slots (active and inactive) for the given charger on a specific day.
     * Used for overlap detection when creating or updating slots.
     */
    List<AvailabilitySlotEntity> findByChargerIdAndDayOfWeek(UUID chargerId, int dayOfWeek);
}

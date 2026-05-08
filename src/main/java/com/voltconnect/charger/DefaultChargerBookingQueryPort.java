package com.voltconnect.charger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Default no-op implementation of {@link ChargerBookingQueryPort}.
 *
 * <p>This bean is active only when no other implementation of
 * {@link ChargerBookingQueryPort} is registered (i.e., before the booking
 * module is implemented). It always returns an empty list, meaning charger
 * deletion will succeed without booking conflict checks.
 *
 * <p>Once the booking module provides a real implementation, this bean will
 * be automatically replaced via {@link ConditionalOnMissingBean}.
 */
@Component
@ConditionalOnMissingBean(ChargerBookingQueryPort.class)
public class DefaultChargerBookingQueryPort implements ChargerBookingQueryPort {

    @Override
    public List<UUID> findActiveBookingIdsForCharger(UUID chargerId) {
        // No booking module yet — no conflicts
        return Collections.emptyList();
    }
}

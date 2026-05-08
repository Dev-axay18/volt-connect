package com.voltconnect.booking;

import com.voltconnect.charger.ChargerBookingQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implements the {@link ChargerBookingQueryPort} interface defined in the charger package.
 *
 * <p>This component lives in the booking package and provides the real booking-aware
 * implementation that replaces the no-op {@code DefaultChargerBookingQueryPort} once
 * the booking module is active.
 *
 * <p>Spring will prefer this bean over the {@code @ConditionalOnMissingBean} default
 * because this bean is unconditionally registered.
 *
 * <p>Satisfies Requirements 13.5.
 */
@Component
@RequiredArgsConstructor
public class BookingQueryPortImpl implements ChargerBookingQueryPort {

    private final BookingRepository bookingRepository;

    @Override
    public List<UUID> findActiveBookingIdsForCharger(UUID chargerId) {
        return bookingRepository.findActiveBookingIdsForCharger(chargerId, Instant.now());
    }
}
